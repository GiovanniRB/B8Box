// profile.js
const API_BASE = '/api';
const AUTH_TOKEN_KEY = 'b8box_token';
const USERNAME_KEY = 'b8box_username';

const token = localStorage.getItem(AUTH_TOKEN_KEY);
const username = localStorage.getItem(USERNAME_KEY);

if (!token) {
    window.location.href = '/';
}

function authHeaders() {
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

// ============================================================
// ELEMENTOS DO DOM
// ============================================================
const userNameNav = document.getElementById('user-name');
const profileUsername = document.getElementById('profile-username');
const profileEmail = document.getElementById('profile-email');
const profileCreated = document.getElementById('profile-created');
const statAlbums = document.getElementById('stat-albums');
const statPlaylists = document.getElementById('stat-playlists');
const statRatings = document.getElementById('stat-ratings');
const messageArea = document.getElementById('message-area');
const editForm = document.getElementById('edit-profile-form');
const passwordForm = document.getElementById('change-password-form');
const deleteBtn = document.getElementById('delete-account-btn');

// ============================================================
// HELPERS
// ============================================================
function showMessage(message, type = 'info') {
    messageArea.className = `alert alert-${type}`;
    messageArea.textContent = message;
    messageArea.classList.remove('d-none');
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setTimeout(() => messageArea.classList.add('d-none'), 5000);
}

// O backend responde erros como TEXTO PURO (ex: "❌ Username já está em
// uso!") e sucessos como JSON. Chamar res.json() direto quebrava nas
// mensagens de erro ("Unexpected token"). Lê como texto e tenta JSON.
async function readBody(response) {
    const text = await response.text();
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch (_) {
        return text;
    }
}

function clearSessionAndGoHome() {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    window.location.href = '/';
}

function formatDate(isoString) {
    if (!isoString) return '—';
    try {
        const date = new Date(isoString);
        return date.toLocaleDateString('pt-BR', {
            day: '2-digit',
            month: 'long',
            year: 'numeric'
        });
    } catch {
        return '—';
    }
}

// ============================================================
// CARREGAR PERFIL
// ============================================================
async function loadProfile() {
    try {
        const res = await fetch(`${API_BASE}/users/me`, { headers: authHeaders() });

        // token expirado/inválido (a API responde 401 sem token válido)
        if (res.status === 401 || res.status === 403) {
            clearSessionAndGoHome();
            return;
        }
        if (!res.ok) throw new Error('Falha ao carregar perfil');

        const user = await res.json();

        // Header
        profileUsername.textContent = user.username;
        profileEmail.textContent = user.email;
        profileCreated.textContent = formatDate(user.createdAt);
        userNameNav.textContent = user.username;
        localStorage.setItem(USERNAME_KEY, user.username);

        renderBioAndGenres(user);

        // Formulário
        document.getElementById('input-username').value = user.username;
        document.getElementById('input-email').value = user.email;
        document.getElementById('input-bio').value = user.bio || '';
        document.getElementById('input-genres').value = (user.favoriteGenres || []).join(', ');
        updateBioCount();

    } catch (err) {
        console.error(err);
        showMessage('❌ Não foi possível carregar o perfil.', 'danger');
    }
}

// Bio e gêneros no cabeçalho do perfil
function renderBioAndGenres(user) {
    const bioEl = document.getElementById('profile-bio');
    bioEl.textContent = user.bio || '';
    bioEl.classList.toggle('d-none', !user.bio);
    B8.renderGenreChips(document.getElementById('profile-genres'), user.favoriteGenres || []);
}

function updateBioCount() {
    document.getElementById('bio-count').textContent = document.getElementById('input-bio').value.length;
}
document.getElementById('input-bio').addEventListener('input', updateBioCount);

// "Indie pop, Grunge,, MPB " -> ["Indie pop", "Grunge", "MPB"]
// (o servidor também limpa e valida; isto só dá o aviso mais cedo)
function parseGenres(text) {
    const seen = new Set();
    const out = [];
    text.split(',').forEach(part => {
        const g = part.trim().replace(/\s+/g, ' ');
        if (g && !seen.has(g.toLowerCase())) {
            seen.add(g.toLowerCase());
            out.push(g);
        }
    });
    return out;
}

// As mensagens do servidor já começam com "❌"; evita mostrar "❌ ❌".
function errorText(message) {
    return String(message || '').startsWith('❌') ? message : `❌ ${message}`;
}

// ============================================================
// CARREGAR ESTATÍSTICAS
// ============================================================
async function loadStats() {
    try {
        const [albumsRes, playlistsRes, ratingsRes] = await Promise.all([
            fetch(`${API_BASE}/albums/me`, { headers: authHeaders() }),
            fetch(`${API_BASE}/playlists/me`, { headers: authHeaders() }),
            fetch(`${API_BASE}/ratings/me`, { headers: authHeaders() })
        ]);

        const albums = albumsRes.ok ? await albumsRes.json() : [];
        const playlists = playlistsRes.ok ? await playlistsRes.json() : [];
        const ratings = ratingsRes.ok ? await ratingsRes.json() : [];

        statAlbums.textContent = Array.isArray(albums) ? albums.length : 0;
        statPlaylists.textContent = Array.isArray(playlists) ? playlists.length : 0;
        statRatings.textContent = Array.isArray(ratings) ? ratings.length : 0;

    } catch (err) {
        console.error(err);
        // Se um endpoint falhar, apenas mostra 0. Não quebra a página.
    }
}

// ============================================================
// ATUALIZAR PERFIL
// ============================================================
editForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const newUsername = document.getElementById('input-username').value.trim();
    const newBio = document.getElementById('input-bio').value.trim();
    const newGenres = parseGenres(document.getElementById('input-genres').value);

    if (!newUsername) {
        showMessage('❌ O nome de usuário é obrigatório.', 'warning');
        return;
    }
    if (newGenres.length > 5) {
        showMessage('❌ Escolha no máximo 5 gêneros favoritos.', 'warning');
        return;
    }
    if (newGenres.some(g => g.length > 30)) {
        showMessage('❌ Cada gênero pode ter no máximo 30 caracteres.', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/users/me`, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify({ username: newUsername, bio: newBio, favoriteGenres: newGenres })
        });

        const data = await readBody(res);

        if (!res.ok) throw new Error(typeof data === 'string' ? data : 'Falha ao atualizar perfil');

        // Atualiza localStorage e header
        localStorage.setItem(USERNAME_KEY, data.username);
        userNameNav.textContent = data.username;
        profileUsername.textContent = data.username;
        profileEmail.textContent = data.email;
        renderBioAndGenres(data);
        document.getElementById('input-genres').value = (data.favoriteGenres || []).join(', ');

        showMessage('✅ Perfil atualizado com sucesso!', 'success');

    } catch (err) {
        console.error(err);
        showMessage(errorText(err.message), 'danger');
    }
});

// ============================================================
// ALTERAR SENHA
// ============================================================
passwordForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const currentPassword = document.getElementById('input-current-password').value;
    const newPassword = document.getElementById('input-new-password').value;
    const confirmPassword = document.getElementById('input-confirm-password').value;

    if (newPassword !== confirmPassword) {
        showMessage('❌ As senhas não coincidem.', 'warning');
        return;
    }

    if (newPassword.length < 6) {
        showMessage('❌ A nova senha deve ter pelo menos 6 caracteres.', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/users/me/change-password`, {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ currentPassword, newPassword })
        });

        const data = await readBody(res);

        if (!res.ok) throw new Error(typeof data === 'string' ? data : 'Falha ao alterar senha');

        showMessage('✅ Senha alterada com sucesso!', 'success');
        passwordForm.reset();

    } catch (err) {
        console.error(err);
        showMessage(`❌ ${err.message}`, 'danger');
    }
});

// ============================================================
// APAGAR CONTA
// ============================================================
deleteBtn.addEventListener('click', async () => {
    const confirmText = prompt(
        'Esta ação é permanente e removerá TODOS os seus dados.\n\n' +
        'Digite "APAGAR" (em maiúsculas) para confirmar:'
    );

    if (confirmText !== 'APAGAR') {
        showMessage('Ação cancelada.', 'info');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/users/me`, {
            method: 'DELETE',
            headers: authHeaders()
        });

        if (!res.ok) {
            const data = await res.text();
            throw new Error(data || 'Falha ao apagar conta');
        }

        alert('✅ Conta apagada. Você será redirecionado.');
        clearSessionAndGoHome();

    } catch (err) {
        console.error(err);
        showMessage(`❌ ${err.message}`, 'danger');
    }
});

// ============================================================
// LOGOUT
// ============================================================
document.getElementById('logout-btn').addEventListener('click', clearSessionAndGoHome);

// ============================================================
// INIT
// ============================================================
// Mostra o nome guardado só se não for um e-mail (loadProfile() confirma o
// valor certo em seguida).
userNameNav.textContent = (username && !username.includes('@')) ? username : '';
loadProfile();
loadStats();