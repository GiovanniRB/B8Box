// thread.js — página de uma discussão, com as respostas em árvore.
const API_BASE = '/api';

const REPLY_MAX = 2000;
// Recuo máximo: daí em diante a conversa continua, mas sem recuar mais,
// com "respondendo a @fulano", para o texto não ficar espremido no celular.
const MAX_INDENT = 4;

const threadId = Number(new URLSearchParams(window.location.search).get('id'));
let thread = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!threadId) {
        showStatus('Discussão não encontrada.');
        return;
    }
    document.getElementById('thread-card').addEventListener('click', onThreadAction);
    document.getElementById('reply-tree').addEventListener('click', onTreeClick);
    document.getElementById('replies-section').addEventListener('submit', onReplySubmit);
    document.getElementById('replies-section').addEventListener('input', onReplyInput);
    document.getElementById('reply-compose').addEventListener('click', onComposeClick);
    loadThread().then(scrollToHash);
});

function showStatus(text) {
    const status = document.getElementById('thread-status');
    status.textContent = text;
    status.classList.remove('d-none');
}

async function loadThread() {
    try {
        const res = await fetch(`${API_BASE}/communities/threads/${threadId}`, { headers: B8.forumHeaders() });
        if (res.status === 404) {
            showStatus('Esta discussão não existe mais.');
            return;
        }
        if (!res.ok) throw new Error('Falha ao carregar discussão');
        thread = await res.json();
        render();
    } catch (err) {
        console.error(err);
        showStatus('Não foi possível carregar a discussão agora.');
    }
}

// Veio de uma notificação (thread.html?id=1#reply-9): leva até a resposta
function scrollToHash() {
    if (!window.location.hash) return;
    const target = document.getElementById(window.location.hash.slice(1));
    if (target) {
        target.scrollIntoView({ block: 'center' });
        target.classList.add('reply-highlight');
    }
}

function render() {
    document.title = `B8Box - ${thread.title}`;
    document.getElementById('thread-status').classList.add('d-none');
    document.getElementById('back-to-community').href = `community.html?id=${Number(thread.communityId)}`;
    document.getElementById('back-label').textContent = thread.communityName;
    renderThreadCard();
    renderCompose();
    renderReplies();
    document.getElementById('replies-section').classList.remove('d-none');
}

function authorHtml(authorId, author, avatarUrl, when) {
    const time = `<span class="text-muted">${B8.escapeHtml(B8.timeAgo(when))}</span>`;
    if (!author) {
        return `<div class="forum-author">
            ${B8.avatarHtml(null, '?', 'b8-avatar-xs')}
            <span class="small"><span class="text-muted">conta excluída</span> ${time}</span>
        </div>`;
    }
    const url = `user-profile.html?id=${Number(authorId)}`;
    // o perfil exige login: para visitante, o nome fica sem link
    const name = B8.isLoggedIn()
        ? `<a href="${url}" class="fw-semibold text-white text-decoration-none">@${B8.escapeHtml(author)}</a>`
        : `<span class="fw-semibold">@${B8.escapeHtml(author)}</span>`;
    return `<div class="forum-author">
        ${B8.avatarHtml(avatarUrl, author, 'b8-avatar-xs')}
        <span class="small">${name} ${time}</span>
    </div>`;
}

function renderThreadCard() {
    const pin = thread.pinned
        ? '<span class="forum-badge"><i class="bi bi-pin-angle-fill" aria-hidden="true"></i> Fixada</span>' : '';
    const pinAction = thread.role === 'MODERATOR'
        ? `<button type="button" class="forum-action" data-action="pin">
               <i class="bi bi-pin-angle" aria-hidden="true"></i> ${thread.pinned ? 'Desafixar' : 'Fixar'}
           </button>` : '';
    const deleteAction = thread.canDelete
        ? `<button type="button" class="forum-action forum-action-danger" data-action="delete">
               <i class="bi bi-trash" aria-hidden="true"></i> Apagar discussão
           </button>` : '';

    const card = document.getElementById('thread-card');
    card.innerHTML = `
        <div class="d-flex align-items-start gap-2 flex-wrap mb-2">
            <h3 class="fw-bold mb-0 thread-title">${B8.escapeHtml(thread.title)}</h3>
            ${pin}
        </div>
        ${authorHtml(thread.authorId, thread.author, thread.avatarUrl, thread.createdAt)}
        <p class="forum-text mt-3 mb-0">${B8.escapeHtml(thread.content)}</p>
        ${pinAction || deleteAction ? `<div class="forum-actions mt-3">${pinAction}${deleteAction}</div>` : ''}`;
    card.classList.remove('d-none');
}

function replyFormHtml(parentId) {
    const cancel = parentId
        ? '<button type="button" class="btn btn-sm btn-outline-light reply-cancel">Cancelar</button>' : '';
    return `
        <form class="reply-form" data-parent-id="${parentId ? Number(parentId) : ''}">
            <textarea class="form-control form-control-sm" rows="${parentId ? 2 : 3}" maxlength="${REPLY_MAX}"
                      placeholder="Escreva sua resposta..." aria-label="Sua resposta" required></textarea>
            <div class="d-flex justify-content-between align-items-center gap-2 mt-1">
                <span class="reply-chars small text-muted">0/${REPLY_MAX}</span>
                <div class="d-flex gap-2">
                    ${cancel}
                    <button type="submit" class="btn btn-sm btn-primary">Responder</button>
                </div>
            </div>
        </form>`;
}

function renderCompose() {
    const compose = document.getElementById('reply-compose');
    if (thread.role) {
        compose.innerHTML = replyFormHtml(null);
    } else if (B8.isLoggedIn()) {
        compose.innerHTML = `<div class="forum-hint">
            Entre na comunidade para responder.
            <button type="button" class="btn btn-sm btn-primary ms-2" id="btn-join">Entrar na comunidade</button>
        </div>`;
    } else {
        compose.innerHTML = `<div class="forum-hint">
            <a href="/">Entre na sua conta</a> para participar da discussão.
        </div>`;
    }
}

function replyItemHtml(r, parent, showReplyTo) {
    let body;
    if (r.removed) {
        body = '<p class="reply-removed mb-0">[removido]</p>';
    } else {
        const replyTo = showReplyTo && parent
            ? `<div class="reply-to small text-muted">respondendo a ${parent.author ? `@${B8.escapeHtml(parent.author)}` : 'mensagem anterior'}</div>` : '';
        const replyBtn = thread.role
            ? '<button type="button" class="forum-action reply-open"><i class="bi bi-reply" aria-hidden="true"></i> Responder</button>' : '';
        const deleteBtn = r.canDelete
            ? '<button type="button" class="forum-action forum-action-danger reply-delete"><i class="bi bi-trash" aria-hidden="true"></i> Apagar</button>' : '';
        body = `
            ${authorHtml(r.authorId, r.author, r.avatarUrl, r.createdAt)}
            ${replyTo}
            <p class="forum-text mt-2 mb-1">${B8.escapeHtml(r.text)}</p>
            ${replyBtn || deleteBtn ? `<div class="forum-actions">${replyBtn}${deleteBtn}</div>` : ''}`;
    }
    return `<div class="reply-item" id="reply-${Number(r.id)}" data-reply-id="${Number(r.id)}">${body}</div>`;
}

function renderReplies() {
    const byParent = new Map();
    const byId = new Map();
    thread.replies.forEach(r => {
        byId.set(r.id, r);
        const key = r.parentId ?? 0;
        if (!byParent.has(key)) byParent.set(key, []);
        byParent.get(key).push(r);
    });

    const renderBranch = (parentId, depth) =>
        (byParent.get(parentId) || []).map(r => {
            const parent = byId.get(r.parentId);
            const children = byParent.has(r.id) ? renderBranch(r.id, Math.min(depth + 1, MAX_INDENT)) : '';
            const item = replyItemHtml(r, parent, depth >= MAX_INDENT);
            if (!children) return item;
            // no recuo máximo, as respostas seguem ao lado, sem novo nível
            return depth >= MAX_INDENT ? item + children : `${item}<div class="reply-children">${children}</div>`;
        }).join('');

    document.getElementById('replies-title').textContent = thread.replyCount
        ? B8.countLabel(thread.replyCount, 'resposta', 'respostas') : 'Respostas';
    document.getElementById('reply-tree').innerHTML = thread.replies.length
        ? renderBranch(0, 0)
        : '<div class="text-center text-muted py-4">Ninguém respondeu ainda.</div>';
}

async function onThreadAction(e) {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    if (btn.dataset.action === 'delete') {
        if (!confirm('Apagar esta discussão e todas as respostas?')) return;
        btn.disabled = true;
        try {
            const res = await fetch(`${API_BASE}/communities/threads/${threadId}`, {
                method: 'DELETE', headers: B8.forumHeaders()
            });
            if (!res.ok) throw new Error(await B8.forumErrorMessage(res));
            window.location.href = `community.html?id=${Number(thread.communityId)}`;
        } catch (err) {
            console.error(err);
            alert(err.message);
            btn.disabled = false;
        }
        return;
    }
    btn.disabled = true;
    try {
        const res = await fetch(`${API_BASE}/communities/threads/${threadId}/pin`, {
            method: 'PUT',
            headers: B8.forumHeaders(),
            body: JSON.stringify({ pinned: !thread.pinned })
        });
        if (!res.ok) throw new Error(await B8.forumErrorMessage(res));
        thread = await res.json();
        renderThreadCard();
    } catch (err) {
        console.error(err);
        alert(err.message);
        btn.disabled = false;
    }
}

async function onComposeClick(e) {
    if (!e.target.closest('#btn-join')) return;
    e.target.disabled = true;
    try {
        const res = await fetch(`${API_BASE}/communities/${Number(thread.communityId)}/join`, {
            method: 'POST', headers: B8.forumHeaders()
        });
        if (!res.ok) throw new Error(await B8.forumErrorMessage(res));
        await loadThread();
    } catch (err) {
        console.error(err);
        alert(err.message);
        e.target.disabled = false;
    }
}

function onTreeClick(e) {
    const item = e.target.closest('.reply-item');
    if (e.target.closest('.reply-open')) {
        if (item.querySelector('.reply-form')) return;
        item.insertAdjacentHTML('beforeend', replyFormHtml(item.dataset.replyId));
        item.querySelector('textarea').focus();
    } else if (e.target.closest('.reply-cancel')) {
        e.target.closest('.reply-form').remove();
    } else if (e.target.closest('.reply-delete')) {
        deleteReply(item, e.target.closest('.reply-delete'));
    }
}

function onReplyInput(e) {
    if (e.target.tagName !== 'TEXTAREA') return;
    e.target.closest('.reply-form').querySelector('.reply-chars').textContent =
        `${e.target.value.length}/${REPLY_MAX}`;
}

async function onReplySubmit(e) {
    const form = e.target.closest('.reply-form');
    if (!form) return;
    e.preventDefault();
    const textarea = form.querySelector('textarea');
    const submit = form.querySelector('button[type="submit"]');
    const text = textarea.value.trim();
    if (!text) {
        textarea.focus();
        return;
    }
    submit.disabled = true;
    try {
        const res = await fetch(`${API_BASE}/communities/threads/${threadId}/replies`, {
            method: 'POST',
            headers: B8.forumHeaders(),
            body: JSON.stringify({ text, parentId: form.dataset.parentId ? Number(form.dataset.parentId) : null })
        });
        if (!res.ok) throw new Error(await B8.forumErrorMessage(res));
        const reply = await res.json();
        await loadThread();
        const created = document.getElementById(`reply-${Number(reply.id)}`);
        if (created) created.scrollIntoView({ block: 'center' });
    } catch (err) {
        console.error(err);
        alert(err.message);
        submit.disabled = false;
    }
}

async function deleteReply(item, btn) {
    if (!confirm('Apagar esta resposta?')) return;
    btn.disabled = true;
    try {
        const res = await fetch(`${API_BASE}/communities/replies/${Number(item.dataset.replyId)}`, {
            method: 'DELETE', headers: B8.forumHeaders()
        });
        if (!res.ok && res.status !== 404) throw new Error(await B8.forumErrorMessage(res));
        await loadThread();
    } catch (err) {
        console.error(err);
        alert(err.message);
        btn.disabled = false;
    }
}
