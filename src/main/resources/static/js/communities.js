// communities.js — lista de comunidades. Visitante pode ver; criar exige login.
const API_BASE = '/api';

let activeCategory = '';

document.addEventListener('DOMContentLoaded', () => {
    const loggedIn = B8.isLoggedIn();
    document.getElementById('btn-new-community').classList.toggle('d-none', !loggedIn);
    document.getElementById('btn-login-to-create').classList.toggle('d-none', loggedIn);

    document.getElementById('btn-new-community').addEventListener('click', openCreateModal);
    document.getElementById('community-form').addEventListener('submit', createCommunity);
    document.getElementById('community-filters').addEventListener('click', (e) => {
        const btn = e.target.closest('.forum-filter');
        if (!btn) return;
        activeCategory = btn.dataset.category;
        document.querySelectorAll('.forum-filter').forEach(b => {
            b.classList.toggle('active', b === btn);
            b.setAttribute('aria-pressed', String(b === btn));
        });
        loadCommunities();
    });

    loadCommunities();
});

async function loadCommunities() {
    const list = document.getElementById('community-list');
    try {
        const query = activeCategory ? `?category=${encodeURIComponent(activeCategory)}` : '';
        const res = await fetch(`${API_BASE}/communities${query}`, { headers: B8.forumHeaders() });
        if (!res.ok) throw new Error('Falha ao carregar comunidades');
        const communities = await res.json();
        if (communities.length === 0) {
            list.innerHTML = `<div class="col-12 text-center text-muted py-4">
                ${activeCategory ? 'Nenhuma comunidade nesta categoria ainda.' : 'Nenhuma comunidade ainda. Que tal criar a primeira?'}
            </div>`;
            return;
        }
        list.innerHTML = communities.map(communityCardHtml).join('');
    } catch (err) {
        console.error(err);
        list.innerHTML = '<div class="col-12 text-center text-muted py-4">Não foi possível carregar as comunidades agora.</div>';
    }
}

function communityCardHtml(c) {
    const badge = c.role === 'MODERATOR' ? '<span class="forum-badge">Moderação</span>'
        : c.role === 'MEMBER' ? '<span class="forum-badge">Participando</span>' : '';
    const description = c.description
        ? `<p class="community-description mb-2">${B8.escapeHtml(c.description)}</p>` : '';
    return `
        <div class="col-md-6 col-xl-4">
            <a href="community.html?id=${Number(c.id)}" class="card community-card">
                <div class="d-flex justify-content-between align-items-start gap-2 mb-2">
                    <h5 class="mb-0">${B8.escapeHtml(c.name)}</h5>
                    ${badge}
                </div>
                ${description}
                <div class="community-meta">
                    <span class="forum-category">${B8.escapeHtml(B8.communityCategoryLabel(c.category))}</span>
                    <span>${B8.countLabel(c.memberCount, 'membro', 'membros')}</span>
                    <span>${B8.countLabel(c.threadCount, 'discussão', 'discussões')}</span>
                </div>
            </a>
        </div>`;
}

function openCreateModal() {
    document.getElementById('community-form').reset();
    document.getElementById('community-error').classList.add('d-none');
    bootstrap.Modal.getOrCreateInstance(document.getElementById('communityModal')).show();
}

async function createCommunity(e) {
    e.preventDefault();
    const errorEl = document.getElementById('community-error');
    const submit = e.target.querySelector('button[type="submit"]');
    errorEl.classList.add('d-none');
    submit.disabled = true;
    try {
        const res = await fetch(`${API_BASE}/communities`, {
            method: 'POST',
            headers: B8.forumHeaders(),
            body: JSON.stringify({
                name: document.getElementById('community-name').value,
                description: document.getElementById('community-description').value,
                category: document.getElementById('community-category').value
            })
        });
        if (!res.ok) throw new Error(await B8.forumErrorMessage(res));
        const community = await res.json();
        window.location.href = `community.html?id=${Number(community.id)}`;
    } catch (err) {
        console.error(err);
        errorEl.textContent = err.message;
        errorEl.classList.remove('d-none');
        submit.disabled = false;
    }
}
