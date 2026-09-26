// playlist.js
const AUTH_TOKEN_KEY = 'b8box_token';
const USERNAME_KEY = 'b8box_username';
const API_BASE = '/api';

const token = localStorage.getItem(AUTH_TOKEN_KEY);
const username = localStorage.getItem(USERNAME_KEY);
if (!token) {
    window.location.href = '/';
}

function authHeaders() {
    return { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' };
}

function getParam(name) {
    return new URLSearchParams(window.location.search).get(name);
}

function formatDuration(seconds) {
    if (seconds === null || seconds === undefined) return '';
    const min = Math.floor(seconds / 60);
    const sec = String(seconds % 60).padStart(2, '0');
    return `${min}:${sec}`;
}

const playlistId = getParam('id');
let currentPlaylist = null;

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('user-name').innerText = username || 'Usuário';

    if (!playlistId) {
        document.getElementById('playlist-title').innerText = 'Playlist não encontrada';
        return;
    }

    loadPlaylist();
    loadTracks();

    document.getElementById('btn-edit-playlist').addEventListener('click', showEditForm);
    document.getElementById('btn-cancel-edit').addEventListener('click', hideEditForm);
    document.getElementById('btn-save-playlist').addEventListener('click', savePlaylist);
    document.getElementById('btn-delete-playlist').addEventListener('click', deletePlaylist);

    document.getElementById('logout-btn').addEventListener('click', () => {
        localStorage.removeItem(AUTH_TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        window.location.href = '/';
    });
});

async function loadPlaylist() {
    try {
        const res = await fetch(`${API_BASE}/playlists/${playlistId}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar playlist');
        const playlist = await res.json();
        currentPlaylist = playlist;
        renderPlaylistHeader(playlist);
    } catch (err) {
        console.error(err);
        document.getElementById('playlist-title').innerText = 'Erro ao carregar playlist';
    }
}

function renderPlaylistHeader(playlist) {
    document.getElementById('playlist-title').innerText = playlist.title || 'Sem título';
    document.getElementById('playlist-description').innerText = playlist.description || '';
    const badge = document.getElementById('playlist-visibility-badge');
    badge.innerHTML = playlist.isPublic
        ? '<i class="bi bi-globe2"></i> Pública'
        : '<i class="bi bi-lock-fill"></i> Privada';
}

async function loadTracks() {
    const container = document.getElementById('track-list');
    try {
        const res = await fetch(`${API_BASE}/playlists/${playlistId}/musics`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar faixas');
        const musics = await res.json();

        if (!musics.length) {
            container.innerHTML = '<div class="text-center text-muted py-4">Nenhuma faixa nessa playlist ainda. Adicione faixas a partir da página de um álbum.</div>';
            return;
        }

        container.innerHTML = musics.map((m, i) => `
            <div class="track-row">
                <span class="track-num">${i + 1}</span>
                <span class="track-name">${m.title}</span>
                <span class="track-duration">${formatDuration(m.duration)}</span>
                <button class="btn btn-sm btn-outline-danger ms-3" onclick="removeTrack(${m.id})" title="Remover da playlist">
                    <i class="bi bi-x-lg"></i>
                </button>
            </div>
        `).join('');
    } catch (err) {
        console.error(err);
        container.innerHTML = '<div class="text-center text-muted py-4">Não foi possível carregar as faixas.</div>';
    }
}

async function removeTrack(musicId) {
    if (!confirm('Remover essa faixa da playlist?')) return;
    try {
        const res = await fetch(`${API_BASE}/playlists/${playlistId}/musics/${musicId}`, {
            method: 'DELETE',
            headers: authHeaders()
        });
        if (!res.ok) throw new Error('Falha ao remover faixa');
        loadTracks();
    } catch (err) {
        console.error(err);
        alert('Não foi possível remover a faixa.');
    }
}

function showEditForm() {
    if (!currentPlaylist) return;
    document.getElementById('edit-title').value = currentPlaylist.title || '';
    document.getElementById('edit-description').value = currentPlaylist.description || '';
    document.getElementById('edit-public').checked = !!currentPlaylist.isPublic;
    document.getElementById('edit-form').classList.remove('d-none');
}

function hideEditForm() {
    document.getElementById('edit-form').classList.add('d-none');
}

async function savePlaylist() {
    const title = document.getElementById('edit-title').value.trim();
    const description = document.getElementById('edit-description').value.trim();
    const isPublic = document.getElementById('edit-public').checked;

    if (!title) {
        alert('O título é obrigatório.');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/playlists/${playlistId}`, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify({ title, description, isPublic })
        });
        if (!res.ok) throw new Error('Falha ao salvar playlist');
        currentPlaylist = await res.json();
        renderPlaylistHeader(currentPlaylist);
        hideEditForm();
    } catch (err) {
        console.error(err);
        alert('Não foi possível salvar as alterações.');
    }
}

async function deletePlaylist() {
    if (!confirm('Tem certeza que deseja apagar esta playlist? Essa ação não pode ser desfeita.')) return;
    try {
        const res = await fetch(`${API_BASE}/playlists/${playlistId}`, {
            method: 'DELETE',
            headers: authHeaders()
        });
        if (!res.ok) throw new Error('Falha ao apagar playlist');
        window.location.href = '/dashboard.html';
    } catch (err) {
        console.error(err);
        alert('Não foi possível apagar a playlist.');
    }
}