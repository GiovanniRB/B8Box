// album.js
// Assume o token JWT é salvo no localStorage como "token" pelo seu auth.js.
// Se o nome da chave for outro no seu auth.js, ajuste AUTH_TOKEN_KEY abaixo.
const AUTH_TOKEN_KEY = 'token';
const API_BASE = '/api';

function getToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY);
}

function authHeaders() {
    const token = getToken();
    return token ? { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                 : { 'Content-Type': 'application/json' };
}

function getAlbumIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

let currentAlbumId = null;
let currentUserId = null; // preenchido a partir de /api/users/me, usado para achar "sua avaliação"

document.addEventListener('DOMContentLoaded', () => {
    currentAlbumId = getAlbumIdFromUrl();
    if (!currentAlbumId) {
        document.getElementById('album-title').innerText = 'Álbum não encontrado';
        return;
    }
    document.getElementById('rating-album-id').value = currentAlbumId;

    loadCurrentUser();
    loadAlbum();
    loadMusics();
    loadRatings();

    document.getElementById('btn-rate-album').addEventListener('click', () => openRatingModal());
    document.getElementById('save-rating-btn').addEventListener('click', saveRating);
    document.getElementById('delete-rating-btn').addEventListener('click', deleteRating);

    document.querySelectorAll('#star-input span').forEach(star => {
        star.addEventListener('click', () => {
            document.getElementById('rating-score').value = star.dataset.value;
            paintStars(star.dataset.value);
        });
    });

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            localStorage.removeItem(AUTH_TOKEN_KEY);
            window.location.href = '/index.html';
        });
    }
});

function paintStars(value) {
    document.querySelectorAll('#star-input span').forEach(s => {
        s.classList.toggle('filled', Number(s.dataset.value) <= Number(value));
    });
}

async function loadCurrentUser() {
    try {
        const res = await fetch(`${API_BASE}/users/me`, { headers: authHeaders() });
        if (!res.ok) return;
        const user = await res.json();
        currentUserId = user.id;
        document.getElementById('user-name').innerText = user.username || user.name || 'Usuário';
    } catch (err) {
        console.error('Erro ao carregar usuário logado:', err);
    }
}

async function loadAlbum() {
    try {
        const res = await fetch(`${API_BASE}/albums/${currentAlbumId}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar álbum');
        const album = await res.json();

        document.getElementById('album-title').innerText = album.title || album.name || 'Sem título';
        document.getElementById('album-artist').innerText = album.artist || '';

        const coverEl = document.getElementById('album-cover');
        if (album.coverUrl || album.imageUrl) {
            coverEl.innerHTML = `<img src="${album.coverUrl || album.imageUrl}" alt="${album.title || ''}">`;
        }
    } catch (err) {
        console.error(err);
        document.getElementById('album-title').innerText = 'Erro ao carregar álbum';
    }
}

async function loadMusics() {
    const container = document.getElementById('track-list');
    try {
        // Ajuste este endpoint se sua API filtrar músicas por álbum de outra forma
        // (ex: GET /api/musics?albumId=ID, ou album.musics embutido na resposta de /api/albums/{id})
        const res = await fetch(`${API_BASE}/musics?albumId=${currentAlbumId}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar faixas');
        const musics = await res.json();

        if (!musics.length) {
            container.innerHTML = '<div class="text-center text-muted py-4">Nenhuma faixa cadastrada.</div>';
            return;
        }

        container.innerHTML = musics.map((m, i) => `
            <div class="track-row">
                <span class="track-num">${i + 1}</span>
                <span class="track-name">${m.title || m.name}</span>
                <span class="track-duration">${m.duration || ''}</span>
            </div>
        `).join('');
    } catch (err) {
        console.error(err);
        container.innerHTML = '<div class="text-center text-muted py-4">Não foi possível carregar as faixas.</div>';
    }
}

async function loadRatings() {
    const listEl = document.getElementById('reviews-list');
    const yourSection = document.getElementById('your-review-section');
    try {
        const res = await fetch(`${API_BASE}/ratings/album/${currentAlbumId}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar avaliações');
        const ratings = await res.json();

        document.getElementById('album-ratings-count').innerText = ratings.length;
        if (ratings.length) {
            const avg = ratings.reduce((sum, r) => sum + (r.score || 0), 0) / ratings.length;
            document.getElementById('album-avg-rating').innerText = avg.toFixed(1);
        } else {
            document.getElementById('album-avg-rating').innerText = '—';
        }

        const yourRating = ratings.find(r => r.userId === currentUserId || (r.user && r.user.id === currentUserId));
        if (yourRating) {
            yourSection.innerHTML = `
                <div class="your-review-banner d-flex justify-content-between align-items-start">
                    <div>
                        <div class="text-muted mb-1">Sua avaliação</div>
                        <span class="star-rating">★ ${yourRating.score}</span>
                        <p class="mb-0 mt-1">${yourRating.review || ''}</p>
                    </div>
                    <button class="btn-pill-outline btn-sm" onclick='openRatingModal(${JSON.stringify(yourRating)})'>Editar</button>
                </div>
            `;
        } else {
            yourSection.innerHTML = '';
        }

        const others = ratings.filter(r => r !== yourRating);
        if (!others.length) {
            listEl.innerHTML = '<div class="text-center text-muted py-4">Ainda não há avaliações da comunidade.</div>';
            return;
        }

        listEl.innerHTML = others.map(r => {
            const username = (r.user && r.user.username) || r.username || 'Usuário';
            const initial = username.charAt(0).toUpperCase();
            return `
                <div class="card review-card">
                    <div class="d-flex align-items-center gap-2 mb-2">
                        <div class="avatar-sm">${initial}</div>
                        <div>
                            <div class="fw-semibold">@${username}</div>
                            <span class="star-rating">★ ${r.score}</span>
                        </div>
                    </div>
                    <p class="mb-0 text-muted">${r.review || ''}</p>
                </div>
            `;
        }).join('');
    } catch (err) {
        console.error(err);
        listEl.innerHTML = '<div class="text-center text-muted py-4">Não foi possível carregar as avaliações.</div>';
    }
}

function openRatingModal(existingRating) {
    const modalTitle = document.getElementById('rating-modal-title');
    const deleteBtn = document.getElementById('delete-rating-btn');

    if (existingRating) {
        modalTitle.innerText = 'Editar Avaliação';
        document.getElementById('rating-id').value = existingRating.id;
        document.getElementById('rating-score').value = existingRating.score;
        document.getElementById('rating-review').value = existingRating.review || '';
        paintStars(existingRating.score);
        deleteBtn.classList.remove('d-none');
    } else {
        modalTitle.innerText = 'Avaliar Álbum';
        document.getElementById('rating-id').value = '';
        document.getElementById('rating-score').value = '';
        document.getElementById('rating-review').value = '';
        paintStars(0);
        deleteBtn.classList.add('d-none');
    }

    new bootstrap.Modal(document.getElementById('ratingModal')).show();
}

async function saveRating() {
    const ratingId = document.getElementById('rating-id').value;
    const score = document.getElementById('rating-score').value;
    const review = document.getElementById('rating-review').value;

    if (!score) {
        alert('Informe uma nota de 0 a 10.');
        return;
    }

    try {
        const isEdit = !!ratingId;
        const url = isEdit ? `${API_BASE}/ratings/${ratingId}` : `${API_BASE}/ratings/album/${currentAlbumId}`;
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method,
            headers: authHeaders(),
            body: JSON.stringify({ score: Number(score), review })
        });
        if (!res.ok) throw new Error('Falha ao salvar avaliação');

        bootstrap.Modal.getInstance(document.getElementById('ratingModal')).hide();
        loadRatings();
    } catch (err) {
        console.error(err);
        alert('Não foi possível salvar sua avaliação.');
    }
}

async function deleteRating() {
    const ratingId = document.getElementById('rating-id').value;
    if (!ratingId) return;
    if (!confirm('Tem certeza que deseja excluir sua avaliação?')) return;

    try {
        const res = await fetch(`${API_BASE}/ratings/${ratingId}`, { method: 'DELETE', headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao excluir avaliação');

        bootstrap.Modal.getInstance(document.getElementById('ratingModal')).hide();
        loadRatings();
    } catch (err) {
        console.error(err);
        alert('Não foi possível excluir sua avaliação.');
    }
}