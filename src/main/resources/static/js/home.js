// home.js — página inicial (feed público). Funciona com e sem login:
//   - visitante: boas-vindas + Populares + Críticas (Recentes / Mais curtidas)
//   - logado: saudação + aba "Seguindo" (atividade de quem você segue) como padrão
// Carregue depois do ui.js (B8) e do auth.js (modal de login).
(function () {
    'use strict';

    const API_BASE = '/api';
    const token = localStorage.getItem('b8box_token');
    const loggedIn = B8.isLoggedIn();
    const esc = B8.escapeHtml;

    const contentEl = document.getElementById('home-content');
    const popularEl = document.getElementById('popular-albums');
    const reviewsEl = document.getElementById('reviews-list');
    const tabsEl = document.getElementById('review-tabs');

    let myId = null; // para não oferecer "curtir" na própria avaliação
    // As críticas são pedidas junto com /users/me e só esperam por ele na
    // hora de desenhar (antes uma esperava a outra terminar).
    let meReady = Promise.resolve();

    function headers() {
        return token ? { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' } : {};
    }

    // Abre o modal de login (register = true abre direto no cadastro).
    // isLoginMode e toggleForms() vêm do auth.js.
    function openLogin(register) {
        bootstrap.Modal.getOrCreateInstance(document.getElementById('loginModal')).show();
        if (register === isLoginMode) toggleForms();
    }

    function loadingHtml(text) {
        return `
            <div class="text-center text-muted py-4">
                <div class="spinner-border text-primary" role="status"></div>
                <p class="mt-2 mb-0">${esc(text)}</p>
            </div>`;
    }

    // ============================================================
    // EU (só com login): id, nome na saudação e na navbar
    // ============================================================
    async function loadMe() {
        try {
            const res = await fetch(`${API_BASE}/users/me`, { headers: headers() });
            if (res.status === 401 || res.status === 403) {
                B8.logout(); // token expirado: limpa e volta como visitante
                return null;
            }
            if (!res.ok) return null;
            const me = await res.json();
            if (me.username) {
                localStorage.setItem('b8box_username', me.username);
                document.getElementById('greeting-name').textContent = `, ${me.username}`;
                const navName = document.getElementById('user-name');
                if (navName) navName.textContent = me.username;
            }
            B8.updateNavUser(me);
            return me;
        } catch (err) {
            console.error('Erro ao carregar usuário:', err);
            return null;
        }
    }

    // ============================================================
    // POPULARES NO B8BOX
    // ============================================================
    function renderPopularAlbum(a) {
        const cover = a.coverUrl
            ? `<img src="${esc(a.coverUrl)}" class="home-cover" alt="Capa de ${esc(a.title)}" loading="lazy">`
            : `<div class="home-cover album-cover-placeholder"><i class="bi bi-vinyl-fill" aria-hidden="true"></i></div>`;
        const avg = a.averageRating != null ? Number(a.averageRating).toFixed(1) : '—';
        const count = Number(a.ratingsCount) || 0;
        const countLabel = count === 1 ? '1 avaliação' : `${count} avaliações`;

        return `
            <div class="col-6 col-md-4 col-lg-2">
                <a href="album-detail.html?id=${Number(a.id)}" class="card album-card h-100 text-decoration-none text-white d-block">
                    ${cover}
                    <div class="card-body p-2">
                        <h3 class="h6 card-title text-truncate mb-0" title="${esc(a.title)}">${esc(a.title)}</h3>
                        <p class="text-muted small text-truncate mb-1">${esc(a.artist)}</p>
                        <div class="small"><span class="star-rating">★ ${avg}</span> <span class="text-muted">· ${countLabel}</span></div>
                    </div>
                </a>
            </div>`;
    }

    async function loadPopularAlbums() {
        popularEl.innerHTML = `<div class="col-12">${loadingHtml('Carregando álbuns...')}</div>`;
        try {
            const res = await fetch(`${API_BASE}/public/albums/popular`);
            if (!res.ok) throw new Error('Falha ao carregar populares');
            const albums = await res.json();
            popularEl.innerHTML = albums.length
                ? albums.map(renderPopularAlbum).join('')
                : `<div class="col-12 text-muted">Nenhum álbum foi avaliado ainda. Que tal ser o primeiro?</div>`;
        } catch (err) {
            console.error(err);
            popularEl.innerHTML = `<div class="col-12 text-muted">Não foi possível carregar os álbuns populares agora.</div>`;
        }
    }

    // ============================================================
    // CRÍTICAS (abas)
    // ============================================================
    const TABS = {
        following: {
            url: `${API_BASE}/feed`,
            loading: 'Carregando o que quem você segue anda ouvindo...',
            empty: `
                <div class="text-center py-5">
                    <i class="bi bi-people" style="font-size: 3rem; color: var(--purple-2);" aria-hidden="true"></i>
                    <h3 class="h4 mt-3">Seu feed está vazio</h3>
                    <p class="text-muted">Siga pessoas no Explorar para ver aqui as avaliações e as playlists delas.</p>
                    <a href="/explore.html" class="btn btn-primary"><i class="bi bi-search" aria-hidden="true"></i> Encontrar pessoas</a>
                </div>`
        },
        recent: {
            url: `${API_BASE}/public/reviews/recent`,
            loading: 'Carregando críticas...',
            empty: `<p class="text-muted py-4 mb-0">Ainda não há críticas. Avalie um álbum e seja o primeiro!</p>`
        },
        popular: {
            url: `${API_BASE}/public/reviews/popular`,
            loading: 'Carregando críticas...',
            empty: `<p class="text-muted py-4 mb-0">Nenhuma crítica recebeu curtidas ainda.</p>`
        }
    };

    // Se a pessoa trocar de aba antes da resposta chegar, a resposta
    // atrasada não pode desenhar por cima da aba nova (igual à dashboard).
    let loadSeq = 0;

    async function showTab(name) {
        const tab = TABS[name];
        if (!tab) return;
        tabsEl.querySelectorAll('[data-tab]').forEach(btn => {
            const on = btn.dataset.tab === name;
            btn.classList.toggle('active', on);
            btn.setAttribute('aria-selected', on ? 'true' : 'false');
        });

        const seq = ++loadSeq;
        reviewsEl.innerHTML = loadingHtml(tab.loading);
        try {
            const res = await fetch(tab.url, { headers: headers() });
            if (seq !== loadSeq) return;
            if (res.status === 401 && name === 'following') {
                B8.logout(); // sessão expirou
                return;
            }
            if (!res.ok) throw new Error('Falha ao carregar críticas');
            const items = await res.json();
            await meReady;
            if (seq !== loadSeq) return;

            reviewsEl.innerHTML = items.length
                ? `<div class="d-flex flex-column gap-3">${items.map(it => B8.renderFeedItem(it, myId)).join('')}</div>`
                : tab.empty;
        } catch (err) {
            if (seq !== loadSeq) return;
            console.error(err);
            reviewsEl.innerHTML = `<div class="alert alert-danger">Não foi possível carregar agora. Tente de novo.</div>`;
        }
    }

    tabsEl.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-tab]');
        if (btn) showTab(btn.dataset.tab);
    });

    // ============================================================
    // INIT
    // ============================================================
    if (loggedIn) {
        document.getElementById('home-hero').classList.add('d-none');
        document.getElementById('home-greeting').classList.remove('d-none');
        document.getElementById('tab-following-item').classList.remove('d-none');

        // nome guardado (se não for e-mail) até o /users/me confirmar
        const stored = localStorage.getItem('b8box_username') || '';
        if (stored && !stored.includes('@')) {
            document.getElementById('greeting-name').textContent = `, ${stored}`;
        }

        B8.bindLikeButtons(reviewsEl, headers);
        B8.bindCommentButtons(reviewsEl, headers);
        loadPopularAlbums();
        meReady = loadMe().then(me => {
            myId = me && me.id != null ? me.id : null;
        });
        showTab('following');
    } else {
        document.getElementById('hero-register-btn').addEventListener('click', () => openLogin(true));
        document.getElementById('hero-login-btn').addEventListener('click', () => openLogin(false));

        // Visitante: álbum, perfil, playlist, curtir e comentar ainda exigem conta.
        // Em vez de mandar para uma página que devolveria para cá, abre o login.
        contentEl.addEventListener('click', (e) => {
            const target = e.target.closest('a[href], .like-btn, .comment-btn');
            if (!target || !contentEl.contains(target)) return;
            e.preventDefault();
            openLogin(false);
        });

        loadPopularAlbums();
        showTab('recent');
    }
})();
