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

searchBtn.addEventListener('click', searchSpotify);
searchInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') searchSpotify();
});

async function searchSpotify() {
    const query = searchInput.value.trim();
    if (!query) return;

    resultsGrid.innerHTML = `
        <div class="col-12 text-center text-muted py-5">
            <p>Buscando...</p>
        </div>
    `;

    try {
        const response = await fetch(`${API_BASE}/spotify/search?q=${encodeURIComponent(query)}`, { headers: getHeaders() });
        const data = await response.json();

        if (!response.ok) throw new Error(data.message || 'Erro na busca');

        if (!data || data.length === 0) {
            resultsGrid.innerHTML = `
                <div class="col-12 text-center text-muted py-5">
                    <p>Nenhum álbum encontrado.</p>
                </div>
            `;
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

    } catch (error) {
        resultsGrid.innerHTML = `<div class="col-12"><div class="alert alert-danger">❌ ${error.message}</div></div>`;
    }
}

document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem('b8box_token');
    localStorage.removeItem('b8box_username');
    window.location.href = '/';
});