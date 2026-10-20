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

userName.textContent = (username && !username.includes('@')) ? username : '';

// O localStorage pode ter o e-mail (se o login foi feito com e-mail), então
// o nome do topo é sempre confirmado em /users/me.
(async function refreshNavUsername() {
    try {
        const res = await fetch(`${API_BASE}/users/me`, { headers: getHeaders() });
        if (!res.ok) return;
        const me = await res.json();
        if (me.username) {
            localStorage.setItem('b8box_username', me.username);
            userName.textContent = me.username;
        }
    } catch (err) {
        console.error(err);
    }
})();

// Evita HTML injetado por texto de usuário (nomes, títulos, descrições)
function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

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

    saveRecentSearch(query);
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
                <img src="${album.coverUrl || 'https://via.placeholder.com/300x300/16102b/a79fc2?text=B8Box'}" class="album-cover w-100" alt="${escapeHtml(album.title)}">
                <div class="p-3">
                    <h6 class="mb-0 text-truncate">${escapeHtml(album.title)}</h6>
                    <p class="text-muted mb-0 small text-truncate">${escapeHtml(album.artist)}</p>
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
                        <div style="width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; flex-shrink: 0; background: linear-gradient(135deg, var(--purple-1), var(--purple-2));">${escapeHtml((user.username || '?').charAt(0).toUpperCase())}</div>
                        <span class="fw-semibold">@${escapeHtml(user.username)}</span>
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
                        <div style="width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: linear-gradient(135deg, var(--purple-1), #2a1a52);"><i class="bi bi-list-ul"></i></div>
                        <div>
                            <div class="fw-semibold">${escapeHtml(p.title)}</div>
                            <div class="text-muted small">${escapeHtml(p.description || '')}</div>
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


// ============================================================
// BUSCAS RECENTES (guardadas no navegador, últimas 5)
// ============================================================
const RECENT_KEY = 'b8box_recent_searches';
const RECENT_MAX = 5;

function getRecentSearches() {
    try {
        const list = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]');
        return Array.isArray(list) ? list : [];
    } catch (_) {
        return [];
    }
}

function saveRecentSearch(query) {
    const lower = query.toLowerCase();
    const list = getRecentSearches().filter(q => q.toLowerCase() !== lower);
    list.unshift(query);
    localStorage.setItem(RECENT_KEY, JSON.stringify(list.slice(0, RECENT_MAX)));
    renderRecentSearches();
}

function clearRecentSearches() {
    localStorage.removeItem(RECENT_KEY);
    renderRecentSearches();
}

function renderRecentSearches() {
    let box = document.getElementById('recent-searches');
    if (!box) {
        box = document.createElement('div');
        box.id = 'recent-searches';
        box.className = 'd-flex flex-wrap justify-content-center align-items-center gap-2 mb-4';
        resultsGrid.parentElement.insertBefore(box, resultsGrid);
    }

    box.textContent = '';
    const list = getRecentSearches();
    if (!list.length) return;

    const label = document.createElement('span');
    label.className = 'text-muted small';
    label.textContent = 'Recentes:';
    box.appendChild(label);

    list.forEach(q => {
        const chip = document.createElement('button');
        chip.type = 'button';
        chip.className = 'tag-pill';
        chip.style.cursor = 'pointer';
        chip.textContent = q;               // textContent: nada vira HTML
        chip.addEventListener('click', () => {
            searchInput.value = q;
            runSearch();
        });
        box.appendChild(chip);
    });

    const clear = document.createElement('button');
    clear.type = 'button';
    clear.className = 'btn btn-link btn-sm text-muted text-decoration-none';
    clear.textContent = 'Limpar';
    clear.addEventListener('click', clearRecentSearches);
    box.appendChild(clear);
}

renderRecentSearches();