// ============================================================
// CONSTANTES E CONFIGURAÇÕES
// ============================================================
const API_BASE = '/api';
const token = localStorage.getItem('b8box_token');
const username = localStorage.getItem('b8box_username');

// Verifica autenticação
if (!token) {
    window.location.href = '/';
}

// Elementos do DOM
const contentArea = document.getElementById('content-area');
const searchInput = document.getElementById('search-input');
const searchBtn = document.getElementById('search-btn');
const searchResults = document.getElementById('search-results');
const userName = document.getElementById('user-name');

// ============================================================
// FUNÇÕES DE AUTENTICAÇÃO
// ============================================================
function getHeaders() {
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

// ============================================================
// FUNÇÕES AUXILIARES (UI)
// ============================================================
function showMessage(container, message, type = 'info') {
    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show`;
    alert.role = 'alert';
    alert.innerHTML = `${message} <button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
    container.prepend(alert);
    setTimeout(() => alert.remove(), 5000);
}

function renderAlbumCard(album) {
    const rating = album.averageRating != null
        ? album.averageRating.toFixed(1)
        : (album.ratings && album.ratings.length > 0
            ? (album.ratings.reduce((sum, r) => sum + r.score, 0) / album.ratings.length).toFixed(1)
            : '—');

    // MUDOU: antes era <div onclick="viewAlbumDetails(...)"> renderizando tudo
    // inline em #content-area. Agora é um link de verdade pra
    // album-detail.html, que também é a mesma tela usada a partir do Explorar.
    return `
        <div class="col-md-4 col-lg-3 mb-4">
            <a href="album-detail.html?id=${album.id}" class="card album-card h-100 text-decoration-none text-white d-block">
                <img src="${album.coverUrl || 'https://via.placeholder.com/300x300/16102b/a79fc2?text=B8Box'}"
                     class="card-img-top album-cover" alt="${album.title}">
                <div class="card-body">
                    <h6 class="card-title text-truncate">${album.title}</h6>
                    <p class="card-text text-muted small">${album.artist}</p>
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="badge bg-warning text-dark">
                            <i class="bi bi-star-fill"></i> ${rating}
                        </span>
                        <small class="text-muted">${album.releaseYear || ''}</small>
                    </div>
                </div>
            </a>
        </div>
    `;
}

// ============================================================
// CARREGAR DADOS PRINCIPAIS
// ============================================================
// MUDOU: /api/albums (todos os álbuns, de todo mundo) -> /api/albums/me
// (só os álbuns em que EU tenho avaliação — mecânica estilo Letterboxd).
async function loadMyAlbums() {
    try {
        const response = await fetch(`${API_BASE}/albums/me`, { headers: getHeaders() });
        const albums = await response.json();

        if (!response.ok) throw new Error(albums.message || 'Erro ao carregar álbuns');

        if (albums.length === 0) {
            contentArea.innerHTML = `
                <div class="text-center py-5">
                    <i class="bi bi-collection" style="font-size: 4rem;"></i>
                    <h4 class="mt-3">Você ainda não tem álbuns</h4>
                    <p class="text-muted">Explore ou busque um álbum no Spotify acima e avalie-o pra ele entrar aqui!</p>
                </div>
            `;
            return;
        }

        contentArea.innerHTML = `
            <div class="row">
                ${albums.map(album => renderAlbumCard(album)).join('')}
            </div>
        `;

    } catch (error) {
        contentArea.innerHTML = `<div class="alert alert-danger">❌ ${error.message}</div>`;
    }
}

// ============================================================
// BUSCAR E IMPORTAR ÁLBUNS DO SPOTIFY
// (a partir do dashboard isso ainda importa direto; a partir do Explorar,
// quem dispara o import é o botão "Avaliar" na tela de detalhe)
// ============================================================
searchBtn.addEventListener('click', searchSpotify);
searchInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') searchSpotify();
});

async function searchSpotify() {
    const query = searchInput.value.trim();
    if (!query) return;

    searchResults.classList.remove('d-none');
    searchResults.innerHTML = `<div class="list-group-item text-muted">Buscando...</div>`;

    try {
        const response = await fetch(`${API_BASE}/spotify/search?q=${encodeURIComponent(query)}`, { headers: getHeaders() });
        const data = await response.json();

        if (!response.ok) throw new Error(data.message || 'Erro na busca');

        if (!data || data.length === 0) {
            searchResults.innerHTML = `<div class="list-group-item text-muted">Nenhum álbum encontrado.</div>`;
            return;
        }

        // MUDOU: em vez de um botão "Importar" que jogava o álbum direto pro
        // seu perfil sem avaliação, cada resultado agora abre o detalhe
        // (mesma tela do Explorar), onde avaliar é que faz o import.
        searchResults.innerHTML = data.map(album => `
            <a href="album-detail.html?spotifyId=${encodeURIComponent(album.spotifyId)}" class="list-group-item list-group-item-action search-result-item d-flex align-items-center text-decoration-none text-white">
                <img src="${album.coverUrl || 'https://via.placeholder.com/50'}"
                     style="width: 50px; height: 50px; object-fit: cover; border-radius: 4px; margin-right: 15px;">
                <div class="flex-grow-1">
                    <strong>${album.title}</strong>
                    <br>
                    <span class="text-muted small">${album.artist} · ${album.releaseYear || ''}</span>
                </div>
                <i class="bi bi-chevron-right"></i>
            </a>
        `).join('');

    } catch (error) {
        searchResults.innerHTML = `<div class="list-group-item text-danger">❌ ${error.message}</div>`;
    }
}

// ============================================================
// PLAYLISTS
// ============================================================
async function loadMyPlaylists() {
    try {
        const response = await fetch(`${API_BASE}/playlists/me`, { headers: getHeaders() });
        const playlists = await response.json();

        if (!response.ok) throw new Error(playlists.message || 'Erro ao carregar playlists');

        if (playlists.length === 0) {
            contentArea.innerHTML = `
                <div class="text-center py-5">
                    <i class="bi bi-list-ul" style="font-size: 4rem;"></i>
                    <h4 class="mt-3">Nenhuma playlist criada</h4>
                    <p class="text-muted">Crie sua primeira playlist!</p>
                    <button class="btn btn-primary" onclick="showCreatePlaylist()">
                        <i class="bi bi-plus-circle"></i> Criar Playlist
                    </button>
                </div>
            `;
            return;
        }

        contentArea.innerHTML = `
            <div class="row">
                ${playlists.map(p => `
                    <div class="col-md-4 mb-4">
                        <div class="card">
                            <div class="card-body">
                                <h5 class="card-title">${p.title}</h5>
                                <p class="card-text small text-muted">${p.description || ''}</p>
                                <span class="badge ${p.isPublic ? 'bg-success' : 'bg-secondary'}">
                                    ${p.isPublic ? 'Pública' : 'Privada'}
                                </span>
                                <button class="btn btn-sm btn-outline-primary mt-2" onclick="viewPlaylist(${p.id})">
                                    <i class="bi bi-eye"></i> Ver
                                </button>
                            </div>
                        </div>
                    </div>
                `).join('')}
                <div class="col-md-4 mb-4">
                    <div class="card h-100 d-flex align-items-center justify-content-center" style="cursor: pointer; border: 2px dashed #ccc;" onclick="showCreatePlaylist()">
                        <div class="text-center py-4">
                            <i class="bi bi-plus-circle" style="font-size: 3rem; color: #0d6efd;"></i>
                            <p>Criar nova playlist</p>
                        </div>
                    </div>
                </div>
            </div>
        `;

    } catch (error) {
        contentArea.innerHTML = `<div class="alert alert-danger">❌ ${error.message}</div>`;
    }
}

// ============================================================
// CRIAÇÃO DE PLAYLIST
// ============================================================
function showCreatePlaylist() {
    contentArea.innerHTML = `
        <div class="row justify-content-center">
            <div class="col-md-6">
                <h3><i class="bi bi-plus-circle"></i> Criar Nova Playlist</h3>
                <form id="create-playlist-form">
                    <div class="mb-3">
                        <label class="form-label">Título *</label>
                        <input type="text" class="form-control" id="playlist-title" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Descrição</label>
                        <textarea class="form-control" id="playlist-description" rows="2"></textarea>
                    </div>
                    <div class="mb-3 form-check">
                        <input type="checkbox" class="form-check-input" id="playlist-public">
                        <label class="form-check-label" for="playlist-public">Playlist pública</label>
                    </div>
                    <button type="submit" class="btn btn-success">Criar</button>
                    <button type="button" class="btn btn-secondary" onclick="loadMyPlaylists()">Cancelar</button>
                </form>
            </div>
        </div>
    `;

    document.getElementById('create-playlist-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        const title = document.getElementById('playlist-title').value.trim();
        const description = document.getElementById('playlist-description').value.trim();
        const isPublic = document.getElementById('playlist-public').checked;

        if (!title) return alert('O título é obrigatório.');

        try {
            const response = await fetch(`${API_BASE}/playlists`, {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({ title, description, isPublic })
            });
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'Erro ao criar playlist');

            showMessage(contentArea, '✅ Playlist criada com sucesso!', 'success');
            loadMyPlaylists();
        } catch (error) {
            showMessage(contentArea, `❌ ${error.message}`, 'danger');
        }
    });
}

// ============================================================
// PERFIL: nome + estatísticas do header
// (os elementos já existiam no dashboard.html mas nada os preenchia)
// ============================================================
async function loadProfileHeader() {
    try {
        const userRes = await fetch(`${API_BASE}/users/me`, { headers: getHeaders() });
        if (userRes.ok) {
            const user = await userRes.json();
            const nameEl = document.getElementById('user-card-name');
            if (nameEl) nameEl.textContent = user.username || username || 'Usuário';
        }
    } catch (err) {
        console.error('Erro ao carregar perfil:', err);
    }

    try {
        const [albumsRes, playlistsRes] = await Promise.all([
            fetch(`${API_BASE}/albums/me`, { headers: getHeaders() }),
            fetch(`${API_BASE}/playlists/me`, { headers: getHeaders() })
        ]);
        const albums = albumsRes.ok ? await albumsRes.json() : [];
        const playlists = playlistsRes.ok ? await playlistsRes.json() : [];

        setText('stat-albuns', albums.length);
        setText('stat-albuns-side', albums.length);
        setText('stat-playlists', playlists.length);
        setText('stat-playlists-side', playlists.length);

        const totalRatings = albums.reduce((sum, a) => sum + (a.ratings ? a.ratings.length : 0), 0);
        setText('stat-ratings-side', totalRatings);
    } catch (err) {
        console.error('Erro ao carregar estatísticas:', err);
    }
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

// ============================================================
// NOVO: navegar pro detalhe da playlist (o botão "Ver" já chamava
// essa função, mas ela nunca tinha sido definida)
// ============================================================
function viewPlaylist(playlistId) {
    window.location.href = `playlist-detail.html?id=${playlistId}`;
}

// ============================================================
// LOGOUT
// ============================================================
document.getElementById('logout-btn').addEventListener('click', function() {
    localStorage.removeItem('b8box_token');
    localStorage.removeItem('b8box_username');
    window.location.href = '/';
});

// ============================================================
// INIT
// ============================================================
userName.textContent = username || 'Usuário';

loadProfileHeader();
loadMyAlbums();