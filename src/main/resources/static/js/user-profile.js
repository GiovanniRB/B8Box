// user-profile.js
const AUTH_TOKEN_KEY = 'b8box_token';
const USERNAME_KEY = 'b8box_username';
const API_BASE = '/api';

const token = localStorage.getItem(AUTH_TOKEN_KEY);
const myStoredUsername = localStorage.getItem(USERNAME_KEY);
if (!token) {
    window.location.href = '/';
}

function authHeaders() {
    return { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' };
}

function getParam(name) {
    return new URLSearchParams(window.location.search).get(name);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

const profileUserId = getParam('id');
let myId = null;
// Promessa do loadMe(): as outras cargas começam junto com ela e só a
// esperam na hora de desenhar (quando precisam saber o meu id).
let meReady = Promise.resolve();
let isOwnProfile = false;
let isFollowing = false;

document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('user-name').innerText =
        (myStoredUsername && !myStoredUsername.includes('@')) ? myStoredUsername : '';

    if (!profileUserId) {
        document.getElementById('profile-username').innerText = 'Usuário não encontrado';
        return;
    }

    makeStatClickable('stat-followers', 'followers');
    makeStatClickable('stat-following', 'following');

    // Tudo em paralelo: antes cada pedido esperava o anterior terminar.
    meReady = loadMe();
    const profileDone = loadProfile();
    loadRatings();
    loadPlaylists();
    B8.bindLikeButtons(document.getElementById('ratings-list'), authHeaders);
    B8.bindCommentButtons(document.getElementById('ratings-list'), authHeaders);
    await profileDone;

    // vindo do dashboard (?list=followers|following): já abre a lista
    const list = getParam('list');
    if (list === 'followers' || list === 'following') openFollowList(list);
});

// ============================================================
// EU (nome do topo + meu id)
// ============================================================
async function loadMe() {
    try {
        const res = await fetch(`${API_BASE}/users/me`, { headers: authHeaders() });
        if (!res.ok) return;
        const me = await res.json();
        myId = me.id ?? null;
        if (me.username) {
            localStorage.setItem(USERNAME_KEY, me.username);
            document.getElementById('user-name').innerText = me.username;
        }
    } catch (err) {
        console.error(err);
    }
}

// ============================================================
// PERFIL
// ============================================================
async function loadProfile() {
    try {
        const res = await fetch(`${API_BASE}/users/${profileUserId}/public`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar perfil');
        const profile = await res.json();
        await meReady; // precisa do meu id pra saber se este perfil é o meu

        isOwnProfile = myId != null && String(myId) === String(profileUserId);

        document.getElementById('profile-username').innerText = '@' + profile.username;
        // foto de perfil ou, sem foto, a inicial do nome
        document.getElementById('profile-avatar').innerHTML =
            B8.avatarHtml(profile.avatarUrl, profile.username, 'b8-avatar-fill');

        if (profile.createdAt) {
            const date = new Date(profile.createdAt);
            document.getElementById('profile-since').innerText =
                'Na B8Box desde ' + date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
        }

        // Bio e gêneros favoritos (texto via textContent: nada vira HTML)
        const bioEl = document.getElementById('profile-bio');
        bioEl.textContent = profile.bio || '';
        bioEl.classList.toggle('d-none', !profile.bio);
        B8.renderGenreChips(document.getElementById('profile-genres'), profile.favoriteGenres || []);

        document.getElementById('stat-followers').innerText = profile.followersCount;
        document.getElementById('stat-following').innerText = profile.followingCount;
        document.getElementById('stat-albums').innerText = profile.albumsCount;
        document.getElementById('stat-playlists').innerText = profile.publicPlaylistsCount;

        const btn = document.getElementById('btn-follow');
        if (isOwnProfile) {
            // não dá pra seguir a si mesmo: o botão vira atalho pro perfil editável
            btn.className = 'btn-pill-outline';
            btn.innerHTML = '<i class="bi bi-pencil"></i> Editar perfil';
            btn.onclick = () => { window.location.href = '/profile.html'; };
        } else {
            isFollowing = profile.followingByMe;
            btn.onclick = toggleFollow;
            updateFollowButton();
        }
    } catch (err) {
        console.error(err);
        document.getElementById('profile-username').innerText = 'Erro ao carregar perfil';
    }
}

function updateFollowButton() {
    const btn = document.getElementById('btn-follow');
    if (isFollowing) {
        btn.innerHTML = '<i class="bi bi-person-check-fill"></i> Seguindo';
        btn.classList.remove('btn-primary');
        btn.classList.add('btn-pill-outline');
    } else {
        btn.innerHTML = '<i class="bi bi-person-plus"></i> Seguir';
        btn.classList.add('btn-primary');
        btn.classList.remove('btn-pill-outline');
    }
}

async function toggleFollow() {
    const btn = document.getElementById('btn-follow');
    btn.disabled = true;
    try {
        const method = isFollowing ? 'DELETE' : 'POST';
        const res = await fetch(`${API_BASE}/follows/${profileUserId}`, { method, headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao atualizar');
        const status = await res.json();

        isFollowing = status.isFollowing;
        document.getElementById('stat-followers').innerText = status.followersCount;
        document.getElementById('stat-following').innerText = status.followingCount;
        updateFollowButton();
    } catch (err) {
        console.error(err);
        alert('Não foi possível atualizar. Tente de novo.');
    }
    btn.disabled = false;
}

// Recarrega só os contadores (depois de seguir/deixar de seguir dentro da lista)
async function refreshCounts() {
    try {
        const res = await fetch(`${API_BASE}/follows/status/${profileUserId}`, { headers: authHeaders() });
        if (!res.ok) return;
        const st = await res.json();
        document.getElementById('stat-followers').innerText = st.followersCount;
        document.getElementById('stat-following').innerText = st.followingCount;
        if (!isOwnProfile) {
            isFollowing = st.isFollowing;
            updateFollowButton();
        }
    } catch (err) {
        console.error(err);
    }
}

// ============================================================
// LISTA DE SEGUIDORES / SEGUINDO (modal)
// ============================================================
function makeStatClickable(statId, type) {
    const el = document.getElementById(statId);
    const block = el && el.closest('.stat-mini');
    if (!block) return;
    block.style.cursor = 'pointer';
    block.setAttribute('role', 'button');
    block.setAttribute('tabindex', '0');
    block.title = type === 'followers' ? 'Ver seguidores' : 'Ver quem segue';
    block.addEventListener('click', () => openFollowList(type));
    block.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            openFollowList(type);
        }
    });
}

function ensureFollowModal() {
    if (document.getElementById('followListModal')) return;
    document.body.insertAdjacentHTML('beforeend', `
        <div class="modal fade" id="followListModal" tabindex="-1">
            <div class="modal-dialog modal-dialog-scrollable">
                <div class="modal-content">
                    <div class="modal-header" style="border-bottom: 1px solid var(--border-soft);">
                        <h5 class="modal-title" id="follow-list-title">Seguidores</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body" id="follow-list-body"></div>
                </div>
            </div>
        </div>
    `);
}

async function openFollowList(type) {
    ensureFollowModal();
    const titleEl = document.getElementById('follow-list-title');
    const bodyEl = document.getElementById('follow-list-body');

    titleEl.textContent = type === 'followers' ? 'Seguidores' : 'Seguindo';
    bodyEl.innerHTML = '<div class="text-center text-muted py-4">Carregando...</div>';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('followListModal')).show();

    try {
        const res = await fetch(`${API_BASE}/follows/${profileUserId}/${type}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar a lista');
        const users = await res.json();

        bodyEl.textContent = '';
        if (!users.length) {
            const empty = document.createElement('div');
            empty.className = 'text-center text-muted py-4';
            empty.textContent = type === 'followers'
                ? 'Ninguém segue este perfil ainda.'
                : 'Este perfil ainda não segue ninguém.';
            bodyEl.appendChild(empty);
            return;
        }

        users.forEach(u => bodyEl.appendChild(buildUserRow(u)));
    } catch (err) {
        console.error(err);
        bodyEl.innerHTML = '<div class="text-center text-muted py-4">Não foi possível carregar a lista.</div>';
    }
}

// Uma linha da lista: avatar + @nome (link pro perfil) + botão Seguir/Seguindo
// (não aparece na linha que é o próprio usuário logado). Montada via DOM
// com textContent, então nenhum nome vira HTML.
function buildUserRow(u) {
    const row = document.createElement('div');
    row.className = 'd-flex align-items-center gap-3 py-2';

    // avatarHtml escapa a URL e o nome, então o innerHTML aqui é seguro
    const avatar = document.createElement('a');
    avatar.href = `user-profile.html?id=${u.id}`;
    avatar.className = 'text-white text-decoration-none flex-shrink-0';
    avatar.innerHTML = B8.avatarHtml(u.avatarUrl, u.username, 'b8-avatar-sm');

    const name = document.createElement('a');
    name.href = `user-profile.html?id=${u.id}`;
    name.className = 'fw-semibold text-white text-decoration-none flex-grow-1 text-truncate';
    name.textContent = '@' + u.username;

    row.appendChild(avatar);
    row.appendChild(name);

    if (!u.isMe) {
        const btn = document.createElement('button');
        btn.type = 'button';
        styleRowButton(btn, u.followingByMe);
        btn.addEventListener('click', () => toggleRowFollow(u, btn));
        row.appendChild(btn);
    }
    return row;
}

function styleRowButton(btn, following) {
    btn.className = following ? 'btn-pill-outline btn-sm' : 'btn btn-primary btn-sm';
    btn.textContent = following ? 'Seguindo' : 'Seguir';
}

async function toggleRowFollow(u, btn) {
    btn.disabled = true;
    try {
        const method = u.followingByMe ? 'DELETE' : 'POST';
        const res = await fetch(`${API_BASE}/follows/${u.id}`, { method, headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao atualizar');
        const st = await res.json();
        u.followingByMe = st.isFollowing;
        styleRowButton(btn, u.followingByMe);
        refreshCounts(); // meus contadores podem ter mudado
    } catch (err) {
        console.error(err);
        alert('Não foi possível atualizar. Tente de novo.');
    }
    btn.disabled = false;
}

// ============================================================
// AVALIAÇÕES RECENTES (mesmo cartão do feed)
// ============================================================
async function loadRatings() {
    const list = document.getElementById('ratings-list');
    try {
        const res = await fetch(`${API_BASE}/users/${profileUserId}/ratings`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar as avaliações');
        const items = await res.json();
        await meReady; // meu id: não oferecer "curtir" nas minhas avaliações

        if (!items.length) {
            list.innerHTML = '<div class="text-center text-muted py-4">Nenhuma avaliação ainda.</div>';
            return;
        }
        list.innerHTML = items.map(it => B8.renderFeedItem(it, myId)).join('');
    } catch (err) {
        console.error(err);
        list.innerHTML = '<div class="text-center text-muted py-4">Não foi possível carregar as avaliações.</div>';
    }
}

// ============================================================
// PLAYLISTS PÚBLICAS
// ============================================================
async function loadPlaylists() {
    const grid = document.getElementById('playlists-grid');
    try {
        const res = await fetch(`${API_BASE}/playlists/user/${profileUserId}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar playlists');
        const playlists = await res.json();

        if (!playlists.length) {
            grid.innerHTML = '<div class="col-12 text-center text-muted py-4">Nenhuma playlist pública ainda.</div>';
            return;
        }

        grid.innerHTML = playlists.map(p => `
            <div class="col-6 col-md-4 col-lg-3">${B8.playlistCardHtml(p, false)}</div>
        `).join('');
    } catch (err) {
        console.error(err);
        grid.innerHTML = '<div class="col-12 text-center text-muted py-4">Não foi possível carregar as playlists.</div>';
    }
}