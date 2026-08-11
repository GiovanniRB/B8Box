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
    const rating = album.ratings && album.ratings.length > 0 
        ? (album.ratings.reduce((sum, r) => sum + r.score, 0) / album.ratings.length).toFixed(1)
        : '—';
    
    return `
        <div class="col-md-4 col-lg-3 mb-4">
            <div class="card album-card h-100" onclick="viewAlbumDetails(${album.id})">
                <img src="${album.coverUrl || 'https://via.placeholder.com/300x300/cccccc/666666?text=Sem+Capa'}" 
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
            </div>
        </div>
    `;
}

// ============================================================
// CARREGAR DADOS PRINCIPAIS
// ============================================================
async function loadMyAlbums() {
    try {
        const response = await fetch(`${API_BASE}/albums`, { headers: getHeaders() });
        const albums = await response.json();
        
        if (!response.ok) throw new Error(albums.message || 'Erro ao carregar álbuns');
        
        if (albums.length === 0) {
            contentArea.innerHTML = `
                <div class="text-center py-5">
                    <i class="bi bi-collection" style="font-size: 4rem;"></i>
                    <h4 class="mt-3">Você ainda não tem álbuns</h4>
                    <p class="text-muted">Busque um álbum no Spotify acima e importe-o para sua coleção!</p>
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
        
        searchResults.innerHTML = data.map(album => `
            <div class="list-group-item list-group-item-action search-result-item d-flex align-items-center">
                <img src="${album.coverUrl || 'https://via.placeholder.com/50'}" 
                     style="width: 50px; height: 50px; object-fit: cover; border-radius: 4px; margin-right: 15px;">
                <div class="flex-grow-1">
                    <strong>${album.title}</strong>
                    <br>
                    <span class="text-muted small">${album.artist} · ${album.releaseYear || ''}</span>
                </div>
                <button class="btn btn-sm btn-success" onclick="importAlbum('${album.spotifyId}')">
                    <i class="bi bi-cloud-download"></i> Importar
                </button>
            </div>
        `).join('');
        
    } catch (error) {
        searchResults.innerHTML = `<div class="list-group-item text-danger">❌ ${error.message}</div>`;
    }
}

// Importar álbum do Spotify
async function importAlbum(spotifyId) {
    try {
        const response = await fetch(`${API_BASE}/spotify/import/${spotifyId}`, {
            method: 'POST',
            headers: getHeaders()
        });
        const data = await response.json();
        
        if (!response.ok) throw new Error(data.message || 'Erro ao importar');
        
        showMessage(contentArea, `✅ ${data.message}`, 'success');
        searchResults.classList.add('d-none');
        searchInput.value = '';
        loadMyAlbums();
        
    } catch (error) {
        showMessage(contentArea, `❌ ${error.message}`, 'danger');
    }
}

// ============================================================
// VER DETALHES DO ÁLBUM
// ============================================================
async function viewAlbumDetails(albumId) {
    try {
        const response = await fetch(`${API_BASE}/albums/${albumId}`, { headers: getHeaders() });
        const album = await response.json();
        if (!response.ok) throw new Error(album.message || 'Erro ao carregar álbum');
        
        const ratings = album.ratings || [];
        const avgRating = ratings.length > 0 
            ? (ratings.reduce((sum, r) => sum + r.score, 0) / ratings.length).toFixed(1)
            : 'Sem avaliações';
        
        contentArea.innerHTML = `
            <div class="row">
                <div class="col-md-4">
                    <img src="${album.coverUrl || 'https://via.placeholder.com/400'}" class="img-fluid rounded shadow" alt="${album.title}">
                    <button class="btn btn-primary w-100 mt-3" onclick="openRatingModal(${album.id})">
                        <i class="bi bi-star"></i> Avaliar este álbum
                    </button>
                    <button class="btn btn-outline-secondary w-100 mt-2" onclick="loadMyAlbums()">
                        <i class="bi bi-arrow-left"></i> Voltar
                    </button>
                </div>
                <div class="col-md-8">
                    <h2>${album.title}</h2>
                    <h5 class="text-muted">${album.artist}</h5>
                    <p><strong>Lançamento:</strong> ${album.releaseYear || 'N/A'}</p>
                    <h5 class="mt-4">Avaliações (média: ${avgRating})</h5>
                    ${ratings.length === 0 ? '<p class="text-muted">Nenhuma avaliação ainda.</p>' : ''}
                    ${ratings.map(r => `
                        <div class="border-bottom py-2">
                            <strong>${r.user?.username || 'Usuário'}</strong>
                            <span class="badge bg-warning text-dark">${r.score}</span>
                            ${r.review ? `<p class="mb-0 small">${r.review}</p>` : ''}
                            <small class="text-muted">${new Date(r.createdAt).toLocaleDateString()}</small>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
        
    } catch (error) {
        showMessage(contentArea, `❌ ${error.message}`, 'danger');
    }
}

// ============================================================
// AVALIAR ÁLBUM
// ============================================================
function openRatingModal(albumId) {
    document.getElementById('rating-album-id').value = albumId;
    document.getElementById('rating-score').value = '';
    document.getElementById('rating-review').value = '';
    const modal = new bootstrap.Modal(document.getElementById('ratingModal'));
    modal.show();
}

document.getElementById('save-rating-btn').addEventListener('click', async function() {
    const albumId = document.getElementById('rating-album-id').value;
    const score = parseFloat(document.getElementById('rating-score').value);
    const review = document.getElementById('rating-review').value.trim();
    
    if (isNaN(score) || score < 0 || score > 10) {
        alert('Por favor, insira uma nota entre 0 e 10.');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/ratings/album/${albumId}`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({ score, review })
        });
        const data = await response.json();
        
        if (!response.ok) throw new Error(data.message || 'Erro ao salvar avaliação');
        
        bootstrap.Modal.getInstance(document.getElementById('ratingModal')).hide();
        showMessage(contentArea, '✅ Avaliação salva com sucesso!', 'success');
        viewAlbumDetails(albumId);
        
    } catch (error) {
        alert(`❌ ${error.message}`);
    }
});

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

// Carrega os álbuns ao iniciar
loadMyAlbums();

// ============================================================
// FUNÇÕES DE PLAYLIST (adicionar)
// ============================================================
// ... (funções para ver detalhes da playlist, adicionar/remover músicas)
// Vamos adicionar estas funções na próxima etapa.