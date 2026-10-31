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
    // ITEM DO FEED / DA LISTA DE AVALIAÇÕES DE UM PERFIL
    // it = FeedItemDTO; myId = id de quem está vendo (para não oferecer
    // "curtir" na própria avaliação).
    // ------------------------------------------------------------
    function renderFeedItem(it, myId) {
        const profileUrl = `user-profile.html?id=${Number(it.userId)}`;
        const who = `<a href="${profileUrl}" class="fw-semibold text-white text-decoration-none">@${escapeHtml(it.username)}</a>`;
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
                    <div class="mt-2">${likes}</div>
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

    function isLoggedIn() {
        return !!localStorage.getItem(TOKEN_KEY);
    }

    function logout() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        window.location.href = '/';
    }

    function renderNavbar(nav) {
        const active = nav.dataset.b8Navbar || '';
        const link = (key, href, label) =>
            `<a href="${href}" class="nav-link-ghost ${active === key ? 'active' : ''}"` +
            `${active === key ? ' aria-current="page"' : ''}>${label}</a>`;

        let right;
        if (isLoggedIn()) {
            // o nome guardado pode ser o e-mail (login com e-mail): nesse caso
            // fica vazio até a página confirmar o username em /users/me
            const stored = localStorage.getItem(USERNAME_KEY) || '';
            const shownName = stored.includes('@') ? '' : stored;
            right = `
                ${link('home', '/', 'Início')}
                ${link('explore', '/explore.html', 'Explorar')}
                ${link('dashboard', '/dashboard.html', 'Dashboard')}
                <a href="/profile.html" class="navbar-text text-white text-decoration-none ${active === 'profile' ? 'active' : ''}" title="Configurar perfil">
                    <i class="bi bi-person-circle" aria-hidden="true"></i> <span id="user-name">${escapeHtml(shownName)}</span>
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
        renderFeedItem,
        isLoggedIn,
        logout,
        renderNavbar
    };

    // Desenha a navbar já ao carregar o ui.js (ele vem no fim do <body>,
    // então o marcador já existe e os scripts das páginas, que vêm depois,
    // já encontram #user-name e #logout-btn).
    const navMarker = document.querySelector('[data-b8-navbar]');
    if (navMarker) renderNavbar(navMarker);
})();
