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

// Busca o usuário logado em /users/me. É a fonte confiável do nome no topo
// da tela: o localStorage pode guardar o e-mail (se o login foi feito com
// e-mail), então aqui a gente sempre corrige com o username de verdade.
async function fetchMe() {
    try {
        const res = await fetch(`${API_BASE}/users/me`, { headers: getHeaders() });
        if (res.status === 401 || res.status === 403) {
            // token expirado/inválido
            localStorage.removeItem('b8box_token');
            localStorage.removeItem('b8box_username');
            window.location.href = '/';
            return null;
        }
        if (!res.ok) return null;
        const me = await res.json();
        if (me.username) {
            localStorage.setItem('b8box_username', me.username);
            if (userName) userName.textContent = me.username;
        }
        return me;
    } catch (err) {
        console.error('Erro ao carregar usuário:', err);
        return null;
    }
}

// ============================================================
// FUNÇÕES AUXILIARES (UI)
// ============================================================
function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function showMessage(container, message, type = 'info') {
    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show`;
    alert.role = 'alert';
    alert.innerHTML = `${message} <button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
    container.prepend(alert);
    setTimeout(() => alert.remove(), 5000);
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

// ============================================================
// MEUS ÁLBUNS (com filtro e ordenação)
// ============================================================
const meReady = fetchMe(); // dispara uma vez; as outras funções aguardam
let myId = null;
let myAlbums = [];
let sortMode = 'recent';
let filterText = '';

// A minha avaliação dentro de um álbum (o DTO traz as avaliações de todo
// mundo; a minha é a que tem o meu userId).
function getMyRating(album) {
    if (myId == null || !album.ratings) return null;
    return album.ratings.find(r => r.userId === myId) || null;
}

function communityAverage(album) {
    if (album.averageRating != null) return album.averageRating;
    if (album.ratings && album.ratings.length > 0) {
        return album.ratings.reduce((sum, r) => sum + r.score, 0) / album.ratings.length;
    }
    return null;
}

function renderAlbumCard(album) {
    const mine = getMyRating(album);
    const avg = communityAverage(album);
    const shown = mine ? mine.score : avg;
    const label = mine ? 'Sua nota' : 'Média da comunidade';
    const badge = shown != null ? Number(shown).toFixed(1) : '—';

    return `
        <div class="col-md-4 col-lg-3 mb-4">
            <a href="album-detail.html?id=${album.id}" class="card album-card h-100 text-decoration-none text-white d-block">
                <img src="${escapeHtml(album.coverUrl || 'https://via.placeholder.com/300x300/16102b/a79fc2?text=B8Box')}"
                     class="card-img-top album-cover" alt="${escapeHtml(album.title)}">
                <div class="card-body">
                    <h6 class="card-title text-truncate">${escapeHtml(album.title)}</h6>
                    <p class="card-text text-muted small">${escapeHtml(album.artist)}</p>
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="badge bg-warning text-dark" title="${label}">
                            <i class="bi bi-star-fill"></i> ${badge}
                        </span>
                        <small class="text-muted">${album.releaseYear || ''}</small>
                    </div>
                </div>
            </a>
        </div>
    `;
}

function sortAlbums(list) {
    const arr = [...list];
    const score = a => { const r = getMyRating(a); return r ? r.score : null; };
    const when = a => { const r = getMyRating(a); return r && r.createdAt ? r.createdAt : ''; };

    switch (sortMode) {
        case 'score-desc':
            return arr.sort((a, b) => (score(b) ?? -1) - (score(a) ?? -1));
        case 'score-asc':
            return arr.sort((a, b) => (score(a) ?? 11) - (score(b) ?? 11));
        case 'title':
            return arr.sort((a, b) => (a.title || '').localeCompare(b.title || '', 'pt-BR'));
        case 'artist':
            return arr.sort((a, b) => (a.artist || '').localeCompare(b.artist || '', 'pt-BR'));
        case 'recent':
        default:
            // mais recentemente avaliado primeiro; sem data, cai pro id (mais novo primeiro)
            return arr.sort((a, b) => {
                const wa = when(a), wb = when(b);
                if (wa && wb) return wb.localeCompare(wa);
                return b.id - a.id;
            });
    }
}

function renderAlbumsGrid() {
    const grid = document.getElementById('albums-grid');
    const counter = document.getElementById('albums-count');
    if (!grid) return;

    const q = filterText.trim().toLowerCase();
    const filtered = q
        ? myAlbums.filter(a =>
            (a.title || '').toLowerCase().includes(q) ||
            (a.artist || '').toLowerCase().includes(q))
        : myAlbums;

    if (counter) {
        counter.textContent = q
            ? `${filtered.length} de ${myAlbums.length} álbuns`
            : `${myAlbums.length} álbuns`;
    }

    if (filtered.length === 0) {
        grid.innerHTML = `<div class="col-12 text-center text-muted py-4">Nenhum álbum encontrado com esse filtro.</div>`;
        return;
    }

    grid.innerHTML = sortAlbums(filtered).map(renderAlbumCard).join('');
}

// MUDOU: /api/albums (todos os álbuns, de todo mundo) -> /api/albums/me
// (só os álbuns em que EU tenho avaliação — mecânica estilo Letterboxd).
async function loadMyAlbums() {
    try {
        const me = await meReady;
        myId = me && me.id != null ? me.id : null;

        const response = await fetch(`${API_BASE}/albums/me`, { headers: getHeaders() });
        const albums = await response.json();

        if (!response.ok) throw new Error(albums.message || 'Erro ao carregar álbuns');
        myAlbums = albums;

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

        const optStyle = 'background: var(--bg-card); color: var(--text-white);';
        contentArea.innerHTML = `
            <div class="d-flex flex-wrap gap-2 mb-3 align-items-center">
                <input type="text" class="form-control" id="albums-filter"
                       placeholder="Filtrar por título ou artista..." style="max-width: 300px;">
                <select class="form-control" id="albums-sort" style="max-width: 230px;">
                    <option value="recent" style="${optStyle}">Avaliados recentemente</option>
                    <option value="score-desc" style="${optStyle}">Minha nota (maior)</option>
                    <option value="score-asc" style="${optStyle}">Minha nota (menor)</option>
                    <option value="title" style="${optStyle}">Título (A–Z)</option>
                    <option value="artist" style="${optStyle}">Artista (A–Z)</option>
                </select>
                <span class="text-muted small ms-auto" id="albums-count"></span>
            </div>
            <div class="row" id="albums-grid"></div>
        `;

        const filterEl = document.getElementById('albums-filter');
        const sortEl = document.getElementById('albums-sort');
        filterEl.value = filterText;
        sortEl.value = sortMode;
        filterEl.addEventListener('input', () => { filterText = filterEl.value; renderAlbumsGrid(); });
        sortEl.addEventListener('change', () => { sortMode = sortEl.value; renderAlbumsGrid(); });

        renderAlbumsGrid();

    } catch (error) {
        contentArea.innerHTML = `<div class="alert alert-danger">❌ ${escapeHtml(error.message)}</div>`;
    }
}

// ============================================================
// BUSCAR ÁLBUNS DO SPOTIFY
// (cada resultado abre o detalhe — mesma tela do Explorar —, onde
// avaliar é que faz o import)
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
            <a href="album-detail.html?spotifyId=${encodeURIComponent(album.spotifyId)}" class="list-group-item list-group-item-action search-result-item d-flex align-items-center text-decoration-none text-white">
                <img src="${escapeHtml(album.coverUrl || 'https://via.placeholder.com/50')}"
                     style="width: 50px; height: 50px; object-fit: cover; border-radius: 4px; margin-right: 15px;">
                <div class="flex-grow-1">
                    <strong>${escapeHtml(album.title)}</strong>
                    <br>
                    <span class="text-muted small">${escapeHtml(album.artist)} · ${album.releaseYear || ''}</span>
                </div>
                <i class="bi bi-chevron-right"></i>
            </a>
        `).join('');

    } catch (error) {
        searchResults.innerHTML = `<div class="list-group-item text-danger">❌ ${escapeHtml(error.message)}</div>`;
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
                                <h5 class="card-title">${escapeHtml(p.title)}</h5>
                                <p class="card-text small text-muted">${escapeHtml(p.description || '')}</p>
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
                    <div class="card h-100 d-flex align-items-center justify-content-center" style="cursor: pointer; border: 2px dashed var(--border-soft);" onclick="showCreatePlaylist()">
                        <div class="text-center py-4">
                            <i class="bi bi-plus-circle" style="font-size: 3rem; color: var(--purple-2);"></i>
                            <p>Criar nova playlist</p>
                        </div>
                    </div>
                </div>
            </div>
        `;

    } catch (error) {
        contentArea.innerHTML = `<div class="alert alert-danger">❌ ${escapeHtml(error.message)}</div>`;
    }
}

// ============================================================
// CRIAÇÃO DE PLAYLIST (agora em modal — o HTML do modal é criado aqui
// mesmo, na primeira vez que for aberto, então não precisa mexer no
// dashboard.html)
// ============================================================
function ensureCreatePlaylistModal() {
    if (document.getElementById('createPlaylistModal')) return;

    document.body.insertAdjacentHTML('beforeend', `
        <div class="modal fade" id="createPlaylistModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <form id="create-playlist-form">
                        <div class="modal-header" style="border-bottom: 1px solid var(--border-soft);">
                            <h5 class="modal-title"><i class="bi bi-plus-circle"></i> Nova playlist</h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div id="create-playlist-error" class="alert alert-danger d-none"></div>
                            <div class="mb-3">
                                <label class="form-label" for="new-playlist-title">Título *</label>
                                <input type="text" class="form-control" id="new-playlist-title" maxlength="100" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="new-playlist-description">Descrição</label>
                                <textarea class="form-control" id="new-playlist-description" rows="2"></textarea>
                            </div>
                            <div class="form-check">
                                <input type="checkbox" class="form-check-input" id="new-playlist-public">
                                <label class="form-check-label" for="new-playlist-public">Playlist pública</label>
                            </div>
                        </div>
                        <div class="modal-footer" style="border-top: 1px solid var(--border-soft);">
                            <button type="button" class="btn-pill-outline" data-bs-dismiss="modal">Cancelar</button>
                            <button type="submit" class="btn btn-primary" id="create-playlist-submit">Criar</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `);

    document.getElementById('create-playlist-form').addEventListener('submit', submitCreatePlaylist);
}

function showCreatePlaylist() {
    ensureCreatePlaylistModal();
    document.getElementById('new-playlist-title').value = '';
    document.getElementById('new-playlist-description').value = '';
    document.getElementById('new-playlist-public').checked = false;
    document.getElementById('create-playlist-error').classList.add('d-none');

    const modalEl = document.getElementById('createPlaylistModal');
    bootstrap.Modal.getOrCreateInstance(modalEl).show();
    modalEl.addEventListener('shown.bs.modal', () => {
        document.getElementById('new-playlist-title').focus();
    }, { once: true });
}

async function submitCreatePlaylist(e) {
    e.preventDefault();
    const title = document.getElementById('new-playlist-title').value.trim();
    const description = document.getElementById('new-playlist-description').value.trim();
    const isPublic = document.getElementById('new-playlist-public').checked;
    const errorEl = document.getElementById('create-playlist-error');
    const submitBtn = document.getElementById('create-playlist-submit');

    if (!title) {
        errorEl.textContent = 'O título é obrigatório.';
        errorEl.classList.remove('d-none');
        return;
    }

    submitBtn.disabled = true;
    try {
        const response = await fetch(`${API_BASE}/playlists`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({ title, description, isPublic })
        });
        if (!response.ok) {
            let msg = 'Erro ao criar playlist';
            try { const data = await response.json(); msg = data.message || msg; } catch (_) {}
            throw new Error(msg);
        }

        bootstrap.Modal.getOrCreateInstance(document.getElementById('createPlaylistModal')).hide();
        await loadMyPlaylists();
        showMessage(contentArea, '✅ Playlist criada com sucesso!', 'success');
        loadProfileHeader(); // atualiza o contador de playlists
    } catch (error) {
        errorEl.textContent = error.message;
        errorEl.classList.remove('d-none');
    } finally {
        submitBtn.disabled = false;
    }
}

// ============================================================
// PERFIL: nome + estatísticas do header
// ============================================================
async function loadProfileHeader() {
    const me = await meReady;
    const nameEl = document.getElementById('user-card-name');
    if (nameEl && me && me.username) nameEl.textContent = me.username;

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

    injectFollowStats(me);
}

// Seguidores/Seguindo no cabeçalho — clicáveis: abrem a lista no seu
// perfil público (user-profile.html abre o modal de lista via ?list=).
// Os blocos são criados aqui, ao lado dos stats que já existem.
async function injectFollowStats(me) {
    if (!me || me.id == null) return;
    if (document.getElementById('stat-followers')) return;

    const anchor = document.getElementById('stat-albuns');
    const statBlock = anchor && anchor.closest('.stat-mini');
    const container = statBlock && statBlock.parentElement;
    if (!container) return;

    try {
        const res = await fetch(`${API_BASE}/follows/status/${me.id}`, { headers: getHeaders() });
        if (!res.ok) return;
        const st = await res.json();
        if (document.getElementById('stat-followers')) return;

        container.insertAdjacentHTML('afterbegin', `
            <a class="stat-mini text-decoration-none text-white" style="cursor: pointer;"
               href="user-profile.html?id=${me.id}&list=followers" title="Ver seguidores">
                <span class="num" id="stat-followers">${st.followersCount}</span>
                <span class="label">Seguidores</span>
            </a>
            <a class="stat-mini text-decoration-none text-white" style="cursor: pointer;"
               href="user-profile.html?id=${me.id}&list=following" title="Ver quem você segue">
                <span class="num" id="stat-following">${st.followingCount}</span>
                <span class="label">Seguindo</span>
            </a>
        `);
    } catch (err) {
        console.error('Erro ao carregar seguidores:', err);
    }
}

// ============================================================
// Navegar pro detalhe da playlist
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
// Mostra o nome guardado só se não for um e-mail (evita piscar o e-mail
// antes do fetchMe() corrigir).
userName.textContent = (username && !username.includes('@')) ? username : '';

loadProfileHeader();
loadMyAlbums();