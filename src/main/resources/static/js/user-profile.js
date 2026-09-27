// user-profile.js
const AUTH_TOKEN_KEY = 'b8box_token';
const USERNAME_KEY = 'b8box_username';
const API_BASE = '/api';

const token = localStorage.getItem(AUTH_TOKEN_KEY);
const myUsername = localStorage.getItem(USERNAME_KEY);
if (!token) {
    window.location.href = '/';
}

function authHeaders() {
    return { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' };
}

function getParam(name) {
    return new URLSearchParams(window.location.search).get(name);
}

const profileUserId = getParam('id');
let isFollowing = false;

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('nav-user-name').innerText = myUsername || 'Usuário';

    if (!profileUserId) {
        document.getElementById('profile-username').innerText = 'Usuário não encontrado';
        return;
    }

    loadProfile();
    loadPlaylists();

    document.getElementById('btn-follow').addEventListener('click', toggleFollow);
    document.getElementById('logout-btn').addEventListener('click', () => {
        localStorage.removeItem(AUTH_TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        window.location.href = '/';
    });
});

async function loadProfile() {
    try {
        const res = await fetch(`${API_BASE}/users/${profileUserId}/public`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar perfil');
        const profile = await res.json();

        document.getElementById('profile-username').innerText = '@' + profile.username;
        document.getElementById('profile-avatar').innerText = ''; // mantém o ícone padrão
        if (profile.createdAt) {
            const date = new Date(profile.createdAt);
            document.getElementById('profile-since').innerText = 'Na B8Box desde ' + date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
        }

        document.getElementById('stat-followers').innerText = profile.followersCount;
        document.getElementById('stat-following').innerText = profile.followingCount;
        document.getElementById('stat-albums').innerText = profile.albumsCount;
        document.getElementById('stat-playlists').innerText = profile.publicPlaylistsCount;

        isFollowing = profile.followingByMe;
        updateFollowButton();
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
        updateFollowButton();
    } catch (err) {
        console.error(err);
        alert('Não foi possível atualizar. Tente de novo.');
    }
    btn.disabled = false;
}

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
                        <h6 class="mb-0">${p.title}</h6>
                    </div>
                    <p class="text-muted small mb-0">${p.description || ''}</p>
                </a>
            </div>
        `).join('');
    } catch (err) {
        console.error(err);
        grid.innerHTML = '<div class="col-12 text-center text-muted py-4">Não foi possível carregar as playlists.</div>';
    }
}