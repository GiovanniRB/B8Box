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
let isOwnProfile = false;
let isFollowing = false;

document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('nav-user-name').innerText =
        (myStoredUsername && !myStoredUsername.includes('@')) ? myStoredUsername : '';

    document.getElementById('logout-btn').addEventListener('click', () => {
        localStorage.removeItem(AUTH_TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        window.location.href = '/';
    });

    if (!profileUserId) {
        document.getElementById('profile-username').innerText = 'Usuário não encontrado';
        return;
    }

    makeStatClickable('stat-followers', 'followers');
    makeStatClickable('stat-following', 'following');

    await loadMe(); // precisa do meu id pra saber se este perfil é o meu
    await loadProfile();
    loadPlaylists();

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
            document.getElementById('nav-user-name').innerText = me.username;
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

        isOwnProfile = myId != null && String(myId) === String(profileUserId);

        document.getElementById('profile-username').innerText = '@' + profile.username;
        // CORRIGIDO: antes isso fazia innerText = '' e apagava o ícone do
        // avatar; agora mostra a inicial do nome.
        document.getElementById('profile-avatar').textContent = (profile.username || '?').charAt(0).toUpperCase();

        if (profile.createdAt) {
            const date = new Date(profile.createdAt);
            document.getElementById('profile-since').innerText =
                'Na B8Box desde ' + date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
        }

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

    const avatar = document.createElement('a');
    avatar.href = `user-profile.html?id=${u.id}`;
    avatar.className = 'text-white text-decoration-none';
    avatar.style.cssText = 'width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; ' +
        'justify-content: center; font-weight: 700; flex-shrink: 0; ' +
        'background: linear-gradient(135deg, var(--purple-1), var(--purple-2));';
    avatar.textContent = (u.username || '?').charAt(0).toUpperCase();

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
            <div class="col-md-4">
                <a href="playlist-detail.html?id=${p.id}" class="card p-3 d-block text-decoration-none text-white h-100">
                    <div class="d-flex align-items-center gap-2 mb-2">
                        <i class="bi bi-list-ul" style="color: var(--purple-2); font-size: 1.3rem;"></i>
                        <h6 class="mb-0">${escapeHtml(p.title)}</h6>
                    </div>
                    <p class="text-muted small mb-0">${escapeHtml(p.description || '')}</p>
                </a>
            </div>
        `).join('');
    } catch (err) {
        console.error(err);
        grid.innerHTML = '<div class="col-12 text-center text-muted py-4">Não foi possível carregar as playlists.</div>';
    }
}