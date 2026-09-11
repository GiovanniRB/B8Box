// album.js
const AUTH_TOKEN_KEY = 'b8box_token'; // CORRIGIDO: era 'token', mas seu auth.js/dashboard.js usam 'b8box_token'
const USERNAME_KEY = 'b8box_username';
const API_BASE = '/api';

const token = localStorage.getItem(AUTH_TOKEN_KEY);
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

// Estado da página. Um dos dois fluxos abaixo preenche currentAlbumId:
// - Modo "id" (já importado): veio direto na URL, OU
// - Modo "spotifyId" (preview): só é preenchido depois que o usuário avalia
//   (import acontece nesse momento, não antes).
let currentAlbumId = getParam('id') ? Number(getParam('id')) : null;
let currentSpotifyId = getParam('spotifyId');
let isPreviewMode = !currentAlbumId && !!currentSpotifyId;
let currentUserId = null;
let currentUserRating = null; // preenchido em loadRatings() quando o usuário já avaliou este álbum

document.addEventListener('DOMContentLoaded', () => {
    if (!currentAlbumId && !currentSpotifyId) {
        document.getElementById('album-title').innerText = 'Álbum não encontrado';
        return;
    }

    loadCurrentUser();

    if (isPreviewMode) {
        loadPreview();
    } else {
        loadImportedAlbum();
    }

    document.getElementById('btn-rate-album').addEventListener('click', onRateClick);
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
            localStorage.removeItem(USERNAME_KEY);
            window.location.href = '/';
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
        document.getElementById('user-name').innerText = user.username || 'Usuário';
    } catch (err) {
        console.error('Erro ao carregar usuário logado:', err);
    }
}

// ============================================================
// MODO PREVIEW: álbum encontrado no Spotify, ainda sem import.
// Usa o endpoint GET /api/spotify/album/{spotifyId}.
// ============================================================
async function loadPreview() {
    document.getElementById('btn-add-to-playlist').classList.add('d-none'); // não dá pra adicionar à playlist sem existir localmente ainda

    try {
        const res = await fetch(`${API_BASE}/spotify/album/${currentSpotifyId}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar álbum do Spotify');
        const album = await res.json();

        // Se já tinha sido importado antes (por qualquer usuário), o backend
        // já devolve o id local — nesse caso, tratamos como álbum normal.
        if (album.alreadyImported && album.localAlbumId) {
            currentAlbumId = album.localAlbumId;
            isPreviewMode = false;
            history.replaceState(null, '', `album-detail.html?id=${currentAlbumId}`);
            document.getElementById('btn-add-to-playlist').classList.remove('d-none');
            loadImportedAlbum();
            return;
        }

        renderAlbumHeader(album);
        renderTracks(album.tracks || []);

        document.getElementById('album-ratings-count').innerText = '0';
        document.getElementById('album-avg-rating').innerText = '—';
        document.getElementById('reviews-list').innerHTML =
            '<div class="text-center text-muted py-4">Ainda não há avaliações — seja o primeiro a avaliar este álbum!</div>';
        document.getElementById('your-review-section').innerHTML = '';
    } catch (err) {
        console.error(err);
        document.getElementById('album-title').innerText = 'Erro ao carregar álbum';
    }
}

// ============================================================
// MODO NORMAL: álbum já importado, com id interno.
// ============================================================
async function loadImportedAlbum() {
    document.getElementById('rating-album-id').value = currentAlbumId;
    await Promise.all([loadAlbum(), loadTracks(), loadRatings()]);
}

async function loadAlbum() {
    try {
        const res = await fetch(`${API_BASE}/albums/${currentAlbumId}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar álbum');
        const album = await res.json();
        renderAlbumHeader(album);
    } catch (err) {
        console.error(err);
        document.getElementById('album-title').innerText = 'Erro ao carregar álbum';
    }
}

function renderAlbumHeader(album) {
    document.getElementById('album-title').innerText = album.title || 'Sem título';
    document.getElementById('album-artist').innerText = album.artist || '';

    const coverEl = document.getElementById('album-cover');
    if (album.coverUrl) {
        coverEl.innerHTML = `<img src="${album.coverUrl}" alt="${album.title || ''}">`;
    }
}

async function loadTracks() {
    // CORRIGIDO: era /api/musics?albumId=, o endpoint real é /api/musics/album/{id}
    const container = document.getElementById('track-list');
    try {
        const res = await fetch(`${API_BASE}/musics/album/${currentAlbumId}`, { headers: authHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar faixas');
        const musics = await res.json();
        renderTracks(musics.map(m => ({ title: m.title, duration: m.duration })));
    } catch (err) {
        console.error(err);
        container.innerHTML = '<div class="text-center text-muted py-4">Não foi possível carregar as faixas.</div>';
    }
}

function renderTracks(tracks) {
    const container = document.getElementById('track-list');
    if (!tracks.length) {
        container.innerHTML = '<div class="text-center text-muted py-4">Nenhuma faixa cadastrada.</div>';
        return;
    }
    container.innerHTML = tracks.map((m, i) => `
        <div class="track-row">
            <span class="track-num">${i + 1}</span>
            <span class="track-name">${m.title}</span>
            <span class="track-duration">${formatDuration(m.duration)}</span>
        </div>
    `).join('');
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

        const yourRating = ratings.find(r => r.userId === currentUserId);
        currentUserRating = yourRating || null;

        const rateBtn = document.getElementById('btn-rate-album');
        if (yourRating) {
            rateBtn.innerHTML = '<i class="bi bi-pencil-fill"></i> Editar Avaliação';
            yourSection.innerHTML = `
                <div class="your-review-banner">
                    <div class="text-muted mb-1">Sua avaliação</div>
                    <span class="star-rating">★ ${yourRating.score}</span>
                    <p class="mb-0 mt-1">${yourRating.review || ''}</p>
                </div>
            `;
        } else {
            rateBtn.innerHTML = '<i class="bi bi-star-fill"></i> Avaliar';
            yourSection.innerHTML = '';
        }

        const others = yourRating ? ratings.filter(r => r.id !== yourRating.id) : ratings;
        if (!others.length) {
            listEl.innerHTML = '<div class="text-center text-muted py-4">Ainda não há avaliações da comunidade.</div>';
            return;
        }

        listEl.innerHTML = others.map(r => {
            const uname = r.username || 'Usuário';
            const initial = uname.charAt(0).toUpperCase();
            return `
                <div class="card review-card">
                    <div class="d-flex align-items-center gap-2 mb-2">
                        <div class="avatar-sm">${initial}</div>
                        <div>
                            <div class="fw-semibold">@${uname}</div>
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

// ============================================================
// AVALIAR
// Se ainda estamos no modo preview (álbum não importado), o clique em
// "Avaliar" primeiro importa o álbum (find-or-create no backend, então é
// seguro chamar mesmo se outra pessoa já importou) e só então abre o modal.
// ============================================================
async function onRateClick() {
    if (isPreviewMode) {
        const btn = document.getElementById('btn-rate-album');
        const originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = 'Importando...';
        try {
            const res = await fetch(`${API_BASE}/spotify/import/${currentSpotifyId}`, {
                method: 'POST',
                headers: authHeaders()
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Erro ao importar álbum');

            currentAlbumId = data.album.id;
            isPreviewMode = false;
            history.replaceState(null, '', `album-detail.html?id=${currentAlbumId}`);
            document.getElementById('preview-banner').classList.add('d-none');
            document.getElementById('btn-add-to-playlist').classList.remove('d-none');
            document.getElementById('rating-album-id').value = currentAlbumId;
        } catch (err) {
            console.error(err);
            alert('Não foi possível importar o álbum: ' + err.message);
            btn.disabled = false;
            btn.innerHTML = originalText;
            return;
        }
        btn.disabled = false;
        btn.innerHTML = originalText;
    }

    openRatingModal(currentUserRating || undefined);
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

    document.getElementById('rating-album-id').value = currentAlbumId;
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

// ============================================================
// REMOVER DO PERFIL = apagar sua avaliação (é isso que faz o álbum
// "sair" de Meus Álbuns, já que /api/albums/me filtra por quem tem
// avaliação).
// ============================================================
async function deleteRating() {
    const ratingId = document.getElementById('rating-id').value;
    if (!ratingId) return;
    if (!confirm('Remover sua avaliação? O álbum vai sair de "Meus Álbuns".')) return;

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