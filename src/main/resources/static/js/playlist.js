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

const playlistId = getParam('id');
let currentPlaylist = null;

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('user-name').innerText = username || 'Usuário';

    if (!playlistId) {
        document.getElementById('playlist-title').innerText = 'Playlist não encontrada';
        return;
    }

    loadPlaylist();
    loadAlbums();

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

// ============================================================
// CARREGAR ÁLBUNS (usa endpoint NOVO /albums)
// ============================================================
async function loadAlbums() {
    const container = document.getElementById('track-list');

    container.innerHTML = `
        <div class="text-center text-muted py-4">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2">Carregando álbuns...</p>
        </div>
    `;

    try {
        const res = await fetch(`${API_BASE}/playlists/${playlistId}/albums`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar álbuns');
        const albums = await res.json();

        if (!albums.length) {
            container.innerHTML = `
                <div class="text-center text-muted py-4">
                    <i class="bi bi-collection" style="font-size: 2.5rem;"></i>
                    <p class="mt-3">Nenhum álbum nessa playlist ainda.</p>
                    <p class="small">Adicione álbuns a partir da página de um álbum.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="row g-3">
                ${albums.map(a => renderAlbumCard(a)).join('')}
            </div>
        `;
    } catch (err) {
        console.error(err);
        container.innerHTML = '<div class="text-center text-muted py-4">Não foi possível carregar os álbuns.</div>';
    }
}

function renderAlbumCard(album) {
    const cover = album.coverUrl || 'https://via.placeholder.com/300x300/cccccc/666666?text=Sem+Capa';
    const spotifyLink = album.spotifyId
        ? `https://open.spotify.com/album/${album.spotifyId}`
        : null;

    return `
        <div class="col-6 col-md-4 col-lg-3">
            <div class="card h-100 shadow-sm">
                <img src="${cover}" class="card-img-top" alt="${album.title}"
                     style="height: 180px; object-fit: cover;">
                <div class="card-body d-flex flex-column p-2">
                    <h6 class="card-title text-truncate mb-1" title="${album.title}">${album.title}</h6>
                    <p class="card-text text-muted small mb-2 text-truncate">${album.artist}</p>

                    <div class="mt-auto d-flex gap-1">
                        <a href="/album-detail.html?id=${album.id}" class="btn btn-sm btn-outline-primary flex-fill" title="Abrir álbum">
                            <i class="bi bi-eye"></i>
                        </a>
                        ${spotifyLink
                            ? `<a href="${spotifyLink}" target="_blank" class="btn btn-sm btn-outline-success" title="Abrir no Spotify">
                                   <i class="bi bi-spotify"></i>
                               </a>`
                            : ''}
                        <button class="btn btn-sm btn-outline-danger"
                                onclick="removeAlbum(${album.id}, '${album.title.replace(/'/g, "\\'")}')"
                                title="Remover da playlist">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
}

async function removeAlbum(albumId, albumTitle) {
    if (!confirm(`Remover "${albumTitle}" da playlist?`)) return;
    try {
        const res = await fetch(`${API_BASE}/playlists/${playlistId}/albums/${albumId}`, {
            method: 'DELETE',
            headers: authHeaders()
        });
        if (!res.ok) throw new Error('Falha ao remover álbum');
        loadAlbums();
    } catch (err) {
        console.error(err);
        alert('Não foi possível remover o álbum.');
    }
}

// ============================================================
// EDITAR / DELETAR PLAYLIST (mantido igual)
// ============================================================
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