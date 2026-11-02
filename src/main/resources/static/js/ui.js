// ui.js — peças de interface usadas em várias telas (feed, perfis, álbum).
// Fica tudo dentro do objeto B8 para não colidir com as constantes que cada
// página já declara (API_BASE, token...). Carregue antes do script da página:
//   <script src="/js/ui.js"></script>
(function () {
    'use strict';

    // Texto escrito por usuários (nome, resenha, bio...) nunca entra como HTML cru.
    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // "agora", "há 5 min", "há 3 h", "há 2 dias" ou a data (dd/mm/aaaa).
    function timeAgo(iso) {
        if (!iso) return '';
        const date = new Date(iso);
        if (isNaN(date.getTime())) return '';
        const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
        if (seconds < 60) return 'agora';
        const minutes = Math.floor(seconds / 60);
        if (minutes < 60) return `há ${minutes} min`;
        const hours = Math.floor(minutes / 60);
        if (hours < 24) return `há ${hours} h`;
        const days = Math.floor(hours / 24);
        if (days < 7) return days === 1 ? 'há 1 dia' : `há ${days} dias`;
        return date.toLocaleDateString('pt-BR');
    }

    // Chips de gêneros favoritos dentro de um container (via DOM, sem HTML cru).
    function renderGenreChips(container, genres, emptyText) {
        if (!container) return;
        container.textContent = '';
        if (!genres || !genres.length) {
            if (emptyText) {
                const span = document.createElement('span');
                span.className = 'text-muted small';
                span.textContent = emptyText;
                container.appendChild(span);
            }
            return;
        }
        genres.forEach(g => {
            const chip = document.createElement('span');
            chip.className = 'tag-pill';
            const dot = document.createElement('span');
            dot.className = 'tag-dot';
            chip.appendChild(dot);
            chip.appendChild(document.createTextNode(' ' + g));
            container.appendChild(chip);
        });
    }

    // ------------------------------------------------------------
    // CURTIDAS
    // ------------------------------------------------------------
    function likeButtonHtml(ratingId, likesCount, likedByMe) {
        const count = Number(likesCount) || 0;
        return `
            <button type="button" class="like-btn ${likedByMe ? 'liked' : ''}" data-rating-id="${Number(ratingId)}"
                    aria-pressed="${likedByMe ? 'true' : 'false'}" title="${likedByMe ? 'Descurtir' : 'Curtir'}">
                <i class="bi ${likedByMe ? 'bi-heart-fill' : 'bi-heart'}" aria-hidden="true"></i>
                <span class="like-count">${count}</span>
                <span class="visually-hidden">${likedByMe ? 'Descurtir avaliação' : 'Curtir avaliação'}</span>
            </button>`;
    }

    // Contador sem botão (para a própria avaliação: ninguém curte a própria).
    function likeCountHtml(likesCount) {
        const count = Number(likesCount) || 0;
        const label = count === 1 ? '1 curtida' : `${count} curtidas`;
        return `<span class="like-count-static"><i class="bi bi-heart" aria-hidden="true"></i> ${label}</span>`;
    }

    function applyLikeState(btn, likedByMe, likesCount) {
        btn.classList.toggle('liked', !!likedByMe);
        btn.setAttribute('aria-pressed', likedByMe ? 'true' : 'false');
        btn.title = likedByMe ? 'Descurtir' : 'Curtir';
        const icon = btn.querySelector('i');
        if (icon) icon.className = 'bi ' + (likedByMe ? 'bi-heart-fill' : 'bi-heart');
        const count = btn.querySelector('.like-count');
        if (count) count.textContent = Number(likesCount) || 0;
        const hidden = btn.querySelector('.visually-hidden');
        if (hidden) hidden.textContent = likedByMe ? 'Descurtir avaliação' : 'Curtir avaliação';
    }

    async function toggleLike(btn, headers) {
        const ratingId = btn.dataset.ratingId;
        const liked = btn.classList.contains('liked');
        btn.disabled = true;
        try {
            const res = await fetch(`/api/ratings/${ratingId}/like`, {
                method: liked ? 'DELETE' : 'POST',
                headers
            });
            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || 'Falha ao curtir');
            }
            const st = await res.json();
            applyLikeState(btn, st.likedByMe, st.likesCount);
        } catch (err) {
            console.error(err);
            alert(err.message && err.message.startsWith('❌') ? err.message : 'Não foi possível curtir agora. Tente de novo.');
        } finally {
            btn.disabled = false;
        }
    }

    // Um único "ouvinte" por container: funciona para botões criados depois
    // (o conteúdo das abas é redesenhado a cada clique).
    function bindLikeButtons(container, getHeaders) {
        if (!container || container.dataset.likesBound) return;
        container.dataset.likesBound = '1';
        container.addEventListener('click', (e) => {
            const btn = e.target.closest('.like-btn');
            if (btn && container.contains(btn) && !btn.disabled) {
                toggleLike(btn, getHeaders());
            }
        });
    }

    // ------------------------------------------------------------
    // COMENTÁRIOS EM AVALIAÇÕES
    // O botão fica na linha .review-actions (junto do curtir); clicar abre,
    // logo abaixo dessa linha, um painel com os comentários e o campo de
    // resposta. Um único ouvinte por container, como nas curtidas.
    // ------------------------------------------------------------
    const COMMENT_MAX = 500;

    function commentButtonHtml(ratingId, commentsCount) {
        const count = Number(commentsCount) || 0;
        return `
            <button type="button" class="comment-btn" data-rating-id="${Number(ratingId)}"
                    aria-expanded="false" title="Comentários">
                <i class="bi bi-chat" aria-hidden="true"></i>
                <span class="comment-count">${count}</span>
                <span class="visually-hidden">Ver comentários</span>
            </button>`;
    }

    function commentItemHtml(c) {
        const profileUrl = `user-profile.html?id=${Number(c.userId)}`;
        const del = c.canDelete
            ? `<button type="button" class="comment-delete" title="Apagar comentário" aria-label="Apagar comentário">
                   <i class="bi bi-trash" aria-hidden="true"></i>
               </button>`
            : '';
        return `
            <li class="comment-item" data-comment-id="${Number(c.id)}">
                <a href="${profileUrl}" class="flex-shrink-0">${avatarHtml(c.avatarUrl, c.username, 'b8-avatar-xs')}</a>
                <div class="comment-body">
                    <div class="small">
                        <a href="${profileUrl}" class="fw-semibold text-white text-decoration-none">@${escapeHtml(c.username)}</a>
                        <span class="text-muted">${escapeHtml(timeAgo(c.createdAt))}</span>
                    </div>
                    <p class="comment-text mb-0">${escapeHtml(c.text)}</p>
                </div>
                ${del}
            </li>`;
    }

    function renderCommentsPanel(panel, comments) {
        panel.innerHTML = `
            <ul class="comment-list">${comments.map(commentItemHtml).join('')}</ul>
            <p class="comments-empty text-muted small mb-2 ${comments.length ? 'd-none' : ''}">Ninguém comentou ainda. Seja a primeira pessoa!</p>
            <form class="comment-form">
                <textarea class="form-control form-control-sm" rows="2" maxlength="${COMMENT_MAX}"
                          placeholder="Escreva um comentário..." aria-label="Escreva um comentário"></textarea>
                <div class="d-flex justify-content-between align-items-center mt-1">
                    <span class="comment-chars small text-muted">0/${COMMENT_MAX}</span>
                    <button type="submit" class="btn btn-sm btn-primary">Comentar</button>
                </div>
            </form>`;
    }

    // Atualiza o número do botão 💬 que abriu o painel
    function bumpCommentCount(panel, delta) {
        const row = panel.previousElementSibling;
        const count = row && row.querySelector('.comment-count');
        if (count) count.textContent = Math.max(0, (Number(count.textContent) || 0) + delta);
    }

    async function toggleComments(btn, headers) {
        const row = btn.closest('.review-actions');
        if (!row) return;
        const open = row.nextElementSibling;
        if (open && open.classList.contains('comments-panel')) {
            open.remove();
            btn.setAttribute('aria-expanded', 'false');
            return;
        }
        const panel = document.createElement('div');
        panel.className = 'comments-panel';
        panel.dataset.ratingId = btn.dataset.ratingId;
        panel.innerHTML = '<div class="text-muted small py-2">Carregando comentários...</div>';
        row.insertAdjacentElement('afterend', panel);
        btn.setAttribute('aria-expanded', 'true');
        try {
            const res = await fetch(`/api/ratings/${Number(btn.dataset.ratingId)}/comments`, { headers });
            if (!res.ok) throw new Error('Falha ao carregar comentários');
            renderCommentsPanel(panel, await res.json());
            const count = btn.querySelector('.comment-count');
            if (count) count.textContent = panel.querySelectorAll('.comment-item').length;
        } catch (err) {
            console.error(err);
            panel.innerHTML = '<div class="text-muted small py-2">Não foi possível carregar os comentários agora.</div>';
        }
    }

    async function submitComment(form, headers) {
        const panel = form.closest('.comments-panel');
        const textarea = form.querySelector('textarea');
        const submit = form.querySelector('button[type="submit"]');
        const text = textarea.value.trim();
        if (!text) {
            textarea.focus();
            return;
        }
        submit.disabled = true;
        try {
            const res = await fetch(`/api/ratings/${Number(panel.dataset.ratingId)}/comments`, {
                method: 'POST',
                headers,
                body: JSON.stringify({ text })
            });
            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || 'Falha ao comentar');
            }
            const comment = await res.json();
            panel.querySelector('.comment-list').insertAdjacentHTML('beforeend', commentItemHtml(comment));
            panel.querySelector('.comments-empty').classList.add('d-none');
            textarea.value = '';
            panel.querySelector('.comment-chars').textContent = `0/${COMMENT_MAX}`;
            bumpCommentCount(panel, 1);
        } catch (err) {
            console.error(err);
            alert(err.message && err.message.startsWith('❌') ? err.message : 'Não foi possível comentar agora. Tente de novo.');
        } finally {
            submit.disabled = false;
        }
    }

    async function deleteComment(btn, headers) {
        const item = btn.closest('.comment-item');
        const panel = btn.closest('.comments-panel');
        if (!confirm('Apagar este comentário?')) return;
        btn.disabled = true;
        try {
            const res = await fetch(`/api/ratings/comments/${Number(item.dataset.commentId)}`, { method: 'DELETE', headers });
            if (!res.ok && res.status !== 404) throw new Error('Falha ao apagar');
            item.remove();
            bumpCommentCount(panel, -1);
            if (!panel.querySelector('.comment-item')) panel.querySelector('.comments-empty').classList.remove('d-none');
        } catch (err) {
            console.error(err);
            alert('Não foi possível apagar o comentário agora. Tente de novo.');
            btn.disabled = false;
        }
    }

    function bindCommentButtons(container, getHeaders) {
        if (!container || container.dataset.commentsBound) return;
        container.dataset.commentsBound = '1';
        container.addEventListener('click', (e) => {
            const btn = e.target.closest('.comment-btn');
            if (btn && container.contains(btn)) {
                toggleComments(btn, getHeaders());
                return;
            }
            const del = e.target.closest('.comment-delete');
            if (del && container.contains(del) && !del.disabled) deleteComment(del, getHeaders());
        });
        container.addEventListener('submit', (e) => {
            const form = e.target.closest('.comment-form');
            if (!form) return;
            e.preventDefault();
            submitComment(form, getHeaders());
        });
        container.addEventListener('input', (e) => {
            if (!e.target.matches('.comment-form textarea')) return;
            const chars = e.target.closest('.comment-form').querySelector('.comment-chars');
            chars.textContent = `${e.target.value.length}/${COMMENT_MAX}`;
        });
    }

    // ------------------------------------------------------------
    // AVATAR (foto de perfil ou, sem foto, a inicial do username)
    // url = avatarUrl que vem da API (já é um caminho nosso, /api/public/...)
    // size = classe extra de tamanho: b8-avatar-xs, b8-avatar-sm, b8-avatar-fill
    // ------------------------------------------------------------
    function avatarHtml(url, username, size) {
        const cls = `b8-avatar ${size || ''}`;
        if (url) {
            return `<img src="${escapeHtml(url)}" class="${cls}" alt="" loading="lazy">`;
        }
        const initial = String(username || '').trim().charAt(0).toUpperCase() || '?';
        return `<span class="${cls} b8-avatar-initial" aria-hidden="true">${escapeHtml(initial)}</span>`;
    }

    // ------------------------------------------------------------
    // CAPA DE PLAYLIST
    // covers = capas dos primeiros álbuns (previewCovers da API)
    //   4 ou mais -> mosaico 2x2; 1 a 3 -> a primeira capa; nenhuma -> degradê
    // extraClass = classe de tamanho/forma (ex.: pl-cover-sm)
    // ------------------------------------------------------------
    function playlistCoverHtml(covers, extraClass) {
        const list = (covers || []).filter(Boolean);
        const cls = `pl-cover ${extraClass || ''}`;
        const img = url => `<img src="${escapeHtml(url)}" alt="" loading="lazy">`;
        if (list.length >= 4) {
            return `<div class="${cls} pl-cover-grid" aria-hidden="true">${list.slice(0, 4).map(img).join('')}</div>`;
        }
        if (list.length) {
            return `<div class="${cls}" aria-hidden="true">${img(list[0])}</div>`;
        }
        return `<div class="${cls} pl-cover-empty" aria-hidden="true"><i class="bi bi-music-note-list"></i></div>`;
    }

    // "1 álbum" / "3 álbuns"
    function albumsCountLabel(count) {
        const n = Number(count) || 0;
        return n === 1 ? '1 álbum' : `${n} álbuns`;
    }

    // Card de playlist (capa + título + "N álbuns"). showVisibility = mostra
    // se é pública ou privada (só faz sentido nas playlists da própria pessoa).
    function playlistCardHtml(p, showVisibility) {
        const visibility = showVisibility
            ? (p.isPublic
                ? ' · <i class="bi bi-globe2" aria-hidden="true"></i> Pública'
                : ' · <i class="bi bi-lock-fill" aria-hidden="true"></i> Privada')
            : '';
        return `
            <a href="/playlist-detail.html?id=${Number(p.id)}" class="card album-card pl-card h-100 text-decoration-none text-white d-block">
                ${playlistCoverHtml(p.previewCovers)}
                <div class="p-2 p-md-3">
                    <h3 class="h6 card-title text-truncate mb-1" title="${escapeHtml(p.title)}">${escapeHtml(p.title)}</h3>
                    <div class="pl-meta">${albumsCountLabel(p.albumsCount)}${visibility}</div>
                    ${p.description ? `<p class="pl-desc mb-0 mt-1">${escapeHtml(p.description)}</p>` : ''}
                </div>
            </a>`;
    }

    // ------------------------------------------------------------
    // ITEM DO FEED / DA LISTA DE AVALIAÇÕES DE UM PERFIL
    // it = FeedItemDTO; myId = id de quem está vendo (para não oferecer
    // "curtir" na própria avaliação).
    // ------------------------------------------------------------
    function renderFeedItem(it, myId) {
        const profileUrl = `user-profile.html?id=${Number(it.userId)}`;
        const who = `<a href="${profileUrl}" class="feed-who fw-semibold text-white text-decoration-none">` +
            `${avatarHtml(it.avatarUrl, it.username, 'b8-avatar-xs')}@${escapeHtml(it.username)}</a>`;
        const when = escapeHtml(timeAgo(it.createdAt));

        if (it.type === 'playlist') {
            const url = `playlist-detail.html?id=${Number(it.playlistId)}`;
            return `
                <article class="card feed-item">
                    <a href="${url}" class="feed-cover feed-cover-playlist" aria-label="Abrir a playlist ${escapeHtml(it.playlistTitle)}">
                        <i class="bi bi-list-ul" aria-hidden="true"></i>
                    </a>
                    <div class="feed-body">
                        <div class="feed-meta mb-1">${who} criou uma playlist <span class="feed-when">${when}</span></div>
                        <a href="${url}" class="feed-title">${escapeHtml(it.playlistTitle)}</a>
                        ${it.playlistDescription ? `<p class="text-muted small mb-0 mt-1">${escapeHtml(it.playlistDescription)}</p>` : ''}
                    </div>
                </article>`;
        }

        const albumUrl = `album-detail.html?id=${Number(it.albumId)}`;
        const cover = it.albumCoverUrl
            ? `<img src="${escapeHtml(it.albumCoverUrl)}" class="feed-cover" alt="Capa de ${escapeHtml(it.albumTitle)}" loading="lazy">`
            : `<span class="feed-cover feed-cover-playlist"><i class="bi bi-vinyl" aria-hidden="true"></i></span>`;
        const sub = [it.albumArtist, it.albumReleaseYear].filter(v => v !== null && v !== undefined && v !== '');
        const isMine = myId !== null && myId !== undefined && Number(it.userId) === Number(myId);
        const likes = isMine ? likeCountHtml(it.likesCount) : likeButtonHtml(it.ratingId, it.likesCount, it.likedByMe);

        return `
            <article class="card feed-item">
                <a href="${albumUrl}" class="flex-shrink-0">${cover}</a>
                <div class="feed-body">
                    <div class="feed-meta mb-1">${who} avaliou <span class="feed-when">${when}</span></div>
                    <a href="${albumUrl}" class="feed-title">${escapeHtml(it.albumTitle)}</a>
                    <div class="text-muted small mb-1">${escapeHtml(sub.join(' · '))}</div>
                    <span class="star-rating">★ ${escapeHtml(it.score)}</span>
                    ${it.review ? `<p class="feed-review mb-0 mt-2">${escapeHtml(it.review)}</p>` : ''}
                    <div class="review-actions mt-2">${likes}${commentButtonHtml(it.ratingId, it.commentsCount)}</div>
                </div>
            </article>`;
    }

    // ------------------------------------------------------------
    // NAVBAR (igual em todas as páginas)
    // Cada página tem só o marcador:
    //   <nav class="navbar navbar-expand-lg" data-b8-navbar="explore"></nav>
    // O valor diz qual link fica ativo: home, explore, dashboard, profile
    // (ou vazio). O conteúdo depende de haver alguém logado.
    // ------------------------------------------------------------
    const TOKEN_KEY = 'b8box_token';
    const USERNAME_KEY = 'b8box_username';
    // URL da minha foto, guardada para a navbar já nascer com ela em todas
    // as páginas (é atualizada quando home, dashboard ou perfil leem /users/me)
    const AVATAR_KEY = 'b8box_avatar';

    function isLoggedIn() {
        return !!localStorage.getItem(TOKEN_KEY);
    }

    function logout() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        localStorage.removeItem(AVATAR_KEY);
        window.location.href = '/';
    }

    function navAvatarHtml(url) {
        return url
            ? avatarHtml(url, '', 'b8-avatar-xs')
            : '<i class="bi bi-person-circle" aria-hidden="true"></i>';
    }

    // Chamado com a resposta de /users/me: atualiza nome e foto da navbar
    // e guarda a foto para as próximas páginas.
    function updateNavUser(me) {
        if (!me) return;
        if (me.avatarUrl) {
            localStorage.setItem(AVATAR_KEY, me.avatarUrl);
        } else {
            localStorage.removeItem(AVATAR_KEY);
        }
        const nameEl = document.getElementById('user-name');
        if (nameEl && me.username) nameEl.textContent = me.username;
        const avatarEl = document.getElementById('nav-avatar');
        if (avatarEl) avatarEl.innerHTML = navAvatarHtml(me.avatarUrl);
    }

    // ------------------------------------------------------------
    // NOTIFICAÇÕES (sino da navbar, só para quem está logado)
    // A bolinha mostra os não lidos (consultados a cada minuto com a aba
    // visível); abrir o painel carrega a lista e marca tudo como lido.
    // ------------------------------------------------------------
    const NOTIF_POLL_MS = 60000;

    function notifHeaders() {
        return { 'Authorization': `Bearer ${localStorage.getItem(TOKEN_KEY)}` };
    }

    function setNotifBadge(count) {
        const badge = document.getElementById('notif-badge');
        if (!badge) return;
        badge.textContent = count > 9 ? '9+' : String(count);
        badge.classList.toggle('d-none', !count);
        const btn = document.getElementById('notif-btn');
        if (btn) btn.setAttribute('aria-label', count ? `Notificações (${count} não lidas)` : 'Notificações');
    }

    // Texto e link de cada aviso. Tudo que vem do usuário passa pelo escapeHtml.
    function notificationItemHtml(n) {
        const who = `<strong>@${escapeHtml(n.actorUsername)}</strong>`;
        const album = n.albumTitle ? `<strong>${escapeHtml(n.albumTitle)}</strong>` : 'um álbum';
        const preview = n.preview ? `<span class="notif-preview">“${escapeHtml(n.preview)}”</span>` : '';
        let text;
        switch (n.type) {
            case 'FOLLOW':  text = `${who} começou a seguir você`; break;
            case 'LIKE':    text = `${who} curtiu sua avaliação de ${album}`; break;
            case 'COMMENT': text = `${who} comentou na sua avaliação de ${album}`; break;
            case 'REPLY':   text = `${who} respondeu na avaliação de ${album} que você comentou`; break;
            default:        text = who;
        }
        const href = n.type !== 'FOLLOW' && n.albumId != null
            ? `/album-detail.html?id=${Number(n.albumId)}`
            : `/user-profile.html?id=${Number(n.actorId)}`;
        return `
            <a href="${href}" class="notif-item ${n.read ? '' : 'unread'}">
                ${avatarHtml(n.actorAvatarUrl, n.actorUsername, 'b8-avatar-sm')}
                <span class="notif-text">
                    <span>${text}</span>
                    ${preview}
                    <span class="notif-when">${escapeHtml(timeAgo(n.createdAt))}</span>
                </span>
            </a>`;
    }

    async function refreshNotifCount() {
        if (!isLoggedIn() || document.hidden) return;
        try {
            const res = await fetch('/api/notifications/unread-count', { headers: notifHeaders() });
            if (!res.ok) return;
            setNotifBadge((await res.json()).count);
        } catch (err) {
            // sem rede: tenta de novo no próximo minuto
        }
    }

    async function openNotifications(panel) {
        const list = panel.querySelector('.notif-list');
        list.innerHTML = '<div class="notif-empty">Carregando...</div>';
        try {
            const res = await fetch('/api/notifications', { headers: notifHeaders() });
            if (!res.ok) throw new Error('Falha ao carregar notificações');
            const items = await res.json();
            list.innerHTML = items.length
                ? items.map(notificationItemHtml).join('')
                : '<div class="notif-empty">Nada por aqui ainda. Quando alguém seguir você, curtir ou comentar suas avaliações, aparece aqui.</div>';
            if (items.some(n => !n.read)) {
                await fetch('/api/notifications/read', { method: 'POST', headers: notifHeaders() });
                setNotifBadge(0);
            }
        } catch (err) {
            console.error(err);
            list.innerHTML = '<div class="notif-empty">Não foi possível carregar as notificações agora.</div>';
        }
    }

    function initNotifications(nav) {
        const btn = nav.querySelector('#notif-btn');
        const panel = nav.querySelector('#notif-panel');
        if (!btn || !panel) return;

        const close = () => {
            panel.classList.add('d-none');
            btn.setAttribute('aria-expanded', 'false');
        };
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (!panel.classList.contains('d-none')) {
                close();
                return;
            }
            panel.classList.remove('d-none');
            btn.setAttribute('aria-expanded', 'true');
            openNotifications(panel);
        });
        document.addEventListener('click', (e) => {
            if (!panel.classList.contains('d-none') && !panel.contains(e.target)) close();
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && !panel.classList.contains('d-none')) {
                close();
                btn.focus();
            }
        });

        refreshNotifCount();
        setInterval(refreshNotifCount, NOTIF_POLL_MS);
        document.addEventListener('visibilitychange', refreshNotifCount);
    }

    function renderNavbar(nav) {
        const active = nav.dataset.b8Navbar || '';
        const link = (key, href, label) =>
            `<a href="${href}" class="nav-link-ghost ${active === key ? 'active' : ''}"` +
            `${active === key ? ' aria-current="page"' : ''}>${label}</a>`;

        let right;
        let bell = '';
        if (isLoggedIn()) {
            // Fica fora do menu ☰: no celular aparece ao lado dele;
            // no computador, depois dos links (order-lg-last).
            bell = `
                <div class="notif-wrap order-lg-last ms-auto ms-lg-3">
                    <button type="button" class="notif-btn" id="notif-btn" aria-expanded="false"
                            aria-controls="notif-panel" aria-label="Notificações" title="Notificações">
                        <i class="bi bi-bell" aria-hidden="true"></i>
                        <span class="notif-badge d-none" id="notif-badge"></span>
                    </button>
                    <div class="notif-panel d-none" id="notif-panel" role="region" aria-label="Notificações">
                        <div class="notif-head">Notificações</div>
                        <div class="notif-list"></div>
                    </div>
                </div>`;
            // o nome guardado pode ser o e-mail (login com e-mail): nesse caso
            // fica vazio até a página confirmar o username em /users/me
            const stored = localStorage.getItem(USERNAME_KEY) || '';
            const shownName = stored.includes('@') ? '' : stored;
            right = `
                ${link('home', '/', 'Início')}
                ${link('explore', '/explore.html', 'Explorar')}
                ${link('dashboard', '/dashboard.html', 'Dashboard')}
                <a href="/profile.html" class="navbar-text text-white text-decoration-none ${active === 'profile' ? 'active' : ''}" title="Configurar perfil">
                    <span id="nav-avatar">${navAvatarHtml(localStorage.getItem(AVATAR_KEY))}</span> <span id="user-name">${escapeHtml(shownName)}</span>
                </a>
                <button type="button" class="btn-pill-outline btn-sm" id="logout-btn"><i class="bi bi-box-arrow-right" aria-hidden="true"></i> Sair</button>`;
        } else {
            // "Explorar" e "Entrar" abrem o modal de login (só existe na home;
            // nas outras páginas o link leva para a home)
            right = `
                ${link('home', '/', 'Início')}
                <a href="/" class="nav-link-ghost" data-b8-login>Explorar</a>
                <a href="/" class="btn-pill-outline" data-b8-login>Entrar</a>`;
        }

        // Em telas < 992px (navbar-expand-lg) os links ficam escondidos atrás
        // do botão ☰ e abrem num painel (collapse do Bootstrap).
        nav.innerHTML = `
            <div class="container-fluid px-5">
                <a class="navbar-brand" href="/"><i class="bi bi-music-note-beamed" aria-hidden="true"></i> B8Box</a>
                ${bell}
                <button class="navbar-toggler b8-nav-toggler" type="button" data-bs-toggle="collapse"
                        data-bs-target="#b8-nav-links" aria-controls="b8-nav-links" aria-expanded="false" aria-label="Abrir menu">
                    <i class="bi bi-list" aria-hidden="true"></i>
                </button>
                <div class="collapse navbar-collapse justify-content-end" id="b8-nav-links">
                    <div class="b8-nav-items">${right}</div>
                </div>
            </div>`;

        const logoutBtn = nav.querySelector('#logout-btn');
        if (logoutBtn) logoutBtn.addEventListener('click', logout);
        initNotifications(nav);

        nav.querySelectorAll('[data-b8-login]').forEach(el => {
            el.addEventListener('click', (e) => {
                const modal = document.getElementById('loginModal');
                if (!modal || !window.bootstrap) return; // segue o link para a home
                e.preventDefault();
                // no celular, fecha o menu antes de abrir o login
                const menu = bootstrap.Collapse.getInstance(nav.querySelector('#b8-nav-links'));
                if (menu) menu.hide();
                bootstrap.Modal.getOrCreateInstance(modal).show();
            });
        });
    }

    window.B8 = {
        escapeHtml,
        timeAgo,
        renderGenreChips,
        likeButtonHtml,
        likeCountHtml,
        bindLikeButtons,
        commentButtonHtml,
        bindCommentButtons,
        renderFeedItem,
        avatarHtml,
        playlistCoverHtml,
        playlistCardHtml,
        albumsCountLabel,
        isLoggedIn,
        logout,
        updateNavUser,
        renderNavbar
    };

    // Desenha a navbar já ao carregar o ui.js (ele vem no fim do <body>,
    // então o marcador já existe e os scripts das páginas, que vêm depois,
    // já encontram #user-name e #logout-btn).
    const navMarker = document.querySelector('[data-b8-navbar]');
    if (navMarker) renderNavbar(navMarker);
})();
