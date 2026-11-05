// community.js — página de uma comunidade: dados, entrar/sair e discussões.
const API_BASE = '/api';

const communityId = Number(new URLSearchParams(window.location.search).get('id'));
let community = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!communityId) {
        showNotFound();
        return;
    }
    document.getElementById('btn-new-thread').addEventListener('click', openThreadModal);
    document.getElementById('thread-form').addEventListener('submit', createThread);
    document.getElementById('thread-content').addEventListener('input', (e) => {
        document.getElementById('thread-chars').textContent = `${e.target.value.length}/5000`;
    });
    document.getElementById('community-actions').addEventListener('click', onActionClick);

    loadCommunity();
    loadThreads();
});

function showNotFound() {
    document.getElementById('thread-list').innerHTML =
        '<div class="text-center text-muted py-4">Comunidade não encontrada.</div>';
}

async function loadCommunity() {
    try {
        const res = await fetch(`${API_BASE}/communities/${communityId}`, { headers: B8.forumHeaders() });
        if (res.status === 404) {
            showNotFound();
            return;
        }
        if (!res.ok) throw new Error('Falha ao carregar comunidade');
        community = await res.json();
        renderCommunity();
    } catch (err) {
        console.error(err);
        document.getElementById('thread-list').innerHTML =
            '<div class="text-center text-muted py-4">Não foi possível carregar a comunidade agora.</div>';
    }
}

function renderCommunity() {
    document.title = `B8Box - ${community.name}`;
    document.getElementById('community-header').classList.remove('d-none');
    document.getElementById('threads-head').classList.remove('d-none');
    document.getElementById('community-name').textContent = community.name;

    const description = document.getElementById('community-description');
    description.textContent = community.description || '';
    description.classList.toggle('d-none', !community.description);

    const creator = community.creator
        ? `<span>criada por @${B8.escapeHtml(community.creator)}</span>` : '';
    document.getElementById('community-meta').innerHTML = `
        <span class="forum-category">${B8.escapeHtml(B8.communityCategoryLabel(community.category))}</span>
        <span>${B8.countLabel(community.memberCount, 'membro', 'membros')}</span>
        <span>${B8.countLabel(community.threadCount, 'discussão', 'discussões')}</span>
        ${creator}`;

    let actions;
    if (!B8.isLoggedIn()) {
        actions = '<a href="/" class="btn btn-primary">Entre para participar</a>';
    } else if (!community.role) {
        actions = '<button type="button" class="btn btn-primary" data-action="join"><i class="bi bi-person-plus" aria-hidden="true"></i> Entrar na comunidade</button>';
    } else {
        actions = '<button type="button" class="btn btn-outline-light" data-action="leave">Sair da comunidade</button>';
    }
    if (community.role === 'MODERATOR') {
        actions += '<button type="button" class="btn btn-outline-light" data-action="delete"><i class="bi bi-trash" aria-hidden="true"></i> Apagar</button>';
    }
    document.getElementById('community-actions').innerHTML = actions;
    document.getElementById('btn-new-thread').classList.toggle('d-none', !community.role);
}

async function onActionClick(e) {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;

    if (action === 'leave' && !confirm('Sair desta comunidade?')) return;
    if (action === 'delete' && !confirm('Apagar a comunidade, com todas as discussões e respostas? Isso não pode ser desfeito.')) return;

    btn.disabled = true;
    try {
        const url = action === 'delete'
            ? `${API_BASE}/communities/${communityId}`
            : `${API_BASE}/communities/${communityId}/join`;
        const method = action === 'join' ? 'POST' : 'DELETE';
        const res = await fetch(url, { method, headers: B8.forumHeaders() });
        if (!res.ok) throw new Error(await B8.forumErrorMessage(res));
        if (action === 'delete') {
            window.location.href = '/communities.html';
            return;
        }
        community = await res.json();
        renderCommunity();
    } catch (err) {
        console.error(err);
        alert(err.message);
        btn.disabled = false;
    }
}

async function loadThreads() {
    const list = document.getElementById('thread-list');
    try {
        const res = await fetch(`${API_BASE}/communities/${communityId}/threads`, { headers: B8.forumHeaders() });
        if (res.status === 404) return; // loadCommunity já avisa
        if (!res.ok) throw new Error('Falha ao carregar discussões');
        const threads = await res.json();
        list.innerHTML = threads.length
            ? threads.map(threadRowHtml).join('')
            : '<div class="text-center text-muted py-4">Nenhuma discussão ainda. Comece a primeira!</div>';
    } catch (err) {
        console.error(err);
        list.innerHTML = '<div class="text-center text-muted py-4">Não foi possível carregar as discussões agora.</div>';
    }
}

function threadRowHtml(t) {
    const pin = t.pinned ? '<span class="forum-badge"><i class="bi bi-pin-angle-fill" aria-hidden="true"></i> Fixada</span>' : '';
    return `
        <a href="thread.html?id=${Number(t.id)}" class="card forum-card thread-row">
            <div class="d-flex align-items-start gap-2 flex-wrap mb-1">
                <h5 class="mb-0 thread-row-title">${B8.escapeHtml(t.title)}</h5>
                ${pin}
            </div>
            <p class="thread-row-excerpt mb-2">${B8.escapeHtml(t.excerpt)}</p>
            <div class="community-meta">
                <span>${t.author ? `@${B8.escapeHtml(t.author)}` : 'conta excluída'}</span>
                <span>${B8.countLabel(t.replyCount, 'resposta', 'respostas')}</span>
                <span>${B8.escapeHtml(B8.timeAgo(t.lastActivityAt))}</span>
            </div>
        </a>`;
}

function openThreadModal() {
    document.getElementById('thread-form').reset();
    document.getElementById('thread-chars').textContent = '0/5000';
    document.getElementById('thread-error').classList.add('d-none');
    bootstrap.Modal.getOrCreateInstance(document.getElementById('threadModal')).show();
}

async function createThread(e) {
    e.preventDefault();
    const errorEl = document.getElementById('thread-error');
    const submit = e.target.querySelector('button[type="submit"]');
    errorEl.classList.add('d-none');
    submit.disabled = true;
    try {
        const res = await fetch(`${API_BASE}/communities/${communityId}/threads`, {
            method: 'POST',
            headers: B8.forumHeaders(),
            body: JSON.stringify({
                title: document.getElementById('thread-title').value,
                content: document.getElementById('thread-content').value
            })
        });
        if (!res.ok) throw new Error(await B8.forumErrorMessage(res));
        const thread = await res.json();
        window.location.href = `thread.html?id=${Number(thread.id)}`;
    } catch (err) {
        console.error(err);
        errorEl.textContent = err.message;
        errorEl.classList.remove('d-none');
        submit.disabled = false;
    }
}
