// explore.js
const API_BASE = '/api';
const token = localStorage.getItem('b8box_token');
const username = localStorage.getItem('b8box_username');

if (!token) {
    window.location.href = '/';
}

function getHeaders() {
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

const searchInput = document.getElementById('search-input');
const searchBtn = document.getElementById('search-btn');
const resultsGrid = document.getElementById('results-grid');
const userName = document.getElementById('user-name');

userName.textContent = username || 'Usuário';

let activeTab = 'albums';

function switchTab(tab) {
    activeTab = tab;
    document.querySelectorAll('#search-tabs .nav-link').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tab);
    });
    // Se já tem algo buscado, refaz a busca na aba nova
    if (searchInput.value.trim()) {
        runSearch();
    } else {
        resultsGrid.innerHTML = `
            <div class="col-12 text-center text-muted py-5">
                <i class="bi bi-search" style="font-size: 2.5rem; color: var(--purple-2);"></i>
                <p class="mt-2">Busque algo pra começar a explorar.</p>
            </div>
        `;
    }
}

searchBtn.addEventListener('click', runSearch);
searchInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') runSearch();
});

async function runSearch() {
    const query = searchInput.value.trim();
    if (!query) return;

    resultsGrid.innerHTML = `<div class="col-12 text-center text-muted py-5"><p>Buscando...</p></div>`;

    try {
        if (activeTab === 'albums') await searchAlbums(query);
        else if (activeTab === 'profiles') await searchProfiles(query);
        else if (activeTab === 'playlists') await searchPlaylists(query);
    } catch (error) {
        resultsGrid.innerHTML = `<div class="col-12"><div class="alert alert-danger">❌ ${error.message}</div></div>`;
    }
}

// ============================================================
// ABA ÁLBUNS
// ============================================================
async function searchAlbums(query) {
    const response = await fetch(`${API_BASE}/spotify/search?q=${encodeURIComponent(query)}`, { headers: getHeaders() });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || 'Erro na busca');

    if (!data || data.length === 0) {
        resultsGrid.innerHTML = emptyState('Nenhum álbum encontrado.');
        return;
    }

    resultsGrid.innerHTML = data.map(album => `
        <div class="col-6 col-md-4 col-lg-3">
            <a href="album-detail.html?spotifyId=${encodeURIComponent(album.spotifyId)}" class="album-card d-block text-decoration-none text-white h-100">
                <img src="${album.coverUrl || 'https://via.placeholder.com/300x300/16102b/a79fc2?text=B8Box'}" class="album-cover w-100" alt="${album.title}">
                <div class="p-3">
                    <h6 class="mb-0 text-truncate">${album.title}</h6>
                    <p class="text-muted mb-0 small text-truncate">${album.artist}</p>
                    <p class="text-muted mb-0 small">${album.releaseYear || ''}</p>
                </div>
            </a>
        </div>
    `).join('');
}

// ============================================================
// ABA PERFIS
// ============================================================
async function searchProfiles(query) {
    const response = await fetch(`${API_BASE}/users/search?q=${encodeURIComponent(query)}`, { headers: getHeaders() });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || 'Erro na busca');

    if (!data || data.length === 0) {
        resultsGrid.innerHTML = emptyState('Nenhum perfil encontrado.');
        return;
    }

    resultsGrid.innerHTML = `
        <div class="col-12">
            <div class="d-flex flex-column gap-2">
                ${data.map(user => `
                    <a href="user-profile.html?id=${user.id}" class="card p-3 d-flex flex-row align-items-center gap-3 text-decoration-none text-white">
                        <div class="avatar-sm" style="background: linear-gradient(135deg, var(--purple-1), var(--purple-2));">${(user.username || '?').charAt(0).toUpperCase()}</div>
                        <span class="fw-semibold">@${user.username}</span>
                        <i class="bi bi-chevron-right ms-auto"></i>
                    </a>
                `).join('')}
            </div>
        </div>
    `;
}

// ============================================================
// ABA PLAYLISTS
// ============================================================
async function searchPlaylists(query) {
    const response = await fetch(`${API_BASE}/playlists/public/search?q=${encodeURIComponent(query)}`, { headers: getHeaders() });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || 'Erro na busca');

    if (!data || data.length === 0) {
        resultsGrid.innerHTML = emptyState('Nenhuma playlist pública encontrada.');
        return;
    }

    resultsGrid.innerHTML = `
        <div class="col-12">
            <div class="d-flex flex-column gap-2">
                ${data.map(p => `
                    <a href="playlist-detail.html?id=${p.id}" class="card p-3 d-flex flex-row align-items-center gap-3 text-decoration-none text-white">
                        <div class="avatar-sm" style="background: linear-gradient(135deg, var(--purple-1), #2a1a52);"><i class="bi bi-list-ul"></i></div>
                        <div>
                            <div class="fw-semibold">${p.title}</div>
                            <div class="text-muted small">${p.description || ''}</div>
                        </div>
                        <i class="bi bi-chevron-right ms-auto"></i>
                    </a>
                `).join('')}
            </div>
        </div>
    `;
}

function emptyState(message) {
    return `<div class="col-12 text-center text-muted py-5"><p>${message}</p></div>`;
}

document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem('b8box_token');
    localStorage.removeItem('b8box_username');
    window.location.href = '/';
});