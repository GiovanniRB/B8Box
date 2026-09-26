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
        if (!res.ok) throw new Error('Falha ao carregar perfil');

        const user = await res.json();

        // Header
        profileUsername.textContent = user.username;
        profileEmail.textContent = user.email;
        profileCreated.textContent = formatDate(user.createdAt);
        userNameNav.textContent = user.username;

        // Formulário
        document.getElementById('input-username').value = user.username;
        document.getElementById('input-email').value = user.email;

    } catch (err) {
        console.error(err);
        showMessage('❌ Não foi possível carregar o perfil.', 'danger');
    }
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
    const newEmail = document.getElementById('input-email').value.trim();

    if (!newUsername || !newEmail) {
        showMessage('❌ Preencha todos os campos.', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/users/me`, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify({ username: newUsername })
        });

        const data = await res.json();

        if (!res.ok) throw new Error(typeof data === 'string' ? data : 'Falha ao atualizar perfil');

        // Atualiza localStorage e header
        localStorage.setItem(USERNAME_KEY, data.username);
        userNameNav.textContent = data.username;
        profileUsername.textContent = data.username;
        profileEmail.textContent = data.email;

        showMessage('✅ Perfil atualizado com sucesso!', 'success');

    } catch (err) {
        console.error(err);
        showMessage(`❌ ${err.message}`, 'danger');
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

        const data = await res.json();

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
        localStorage.removeItem(AUTH_TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        window.location.href = '/';

    } catch (err) {
        console.error(err);
        showMessage(`❌ ${err.message}`, 'danger');
    }
});

// ============================================================
// LOGOUT
// ============================================================
document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    window.location.href = '/';
});

// ============================================================
// INIT
// ============================================================
userNameNav.textContent = username || 'Usuário';
loadProfile();
loadStats();