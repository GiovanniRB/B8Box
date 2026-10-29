// ============================================================
// Elementos do DOM
// ============================================================
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const toggleLink = document.getElementById('toggle-link');
const toggleText = document.getElementById('toggle-text');
const formTitle = document.getElementById('form-title');
const messageArea = document.getElementById('message-area');
const registerMessageArea = document.getElementById('register-message-area');

let isLoginMode = true;

// ============================================================
// Função para mostrar mensagens
// ============================================================
function showMessage(element, message, type = 'danger') {
    element.textContent = message;
    element.className = `alert alert-${type}`;
    element.classList.remove('d-none');
}

function hideMessage(element) {
    element.classList.add('d-none');
}

// O backend responde erros como TEXTO PURO (ex: "❌ Usuário ou senha
// inválidos!") e sucessos como JSON. Chamar response.json() direto quebrava
// nas mensagens de erro ("Unexpected token"). Aqui lemos como texto e
// tentamos interpretar como JSON; se não der, devolvemos o próprio texto.
async function readBody(response) {
    const text = await response.text();
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch (_) {
        return text;
    }
}

// ============================================================
// Função para alternar entre Login e Cadastro
// ============================================================
function toggleForms() {
    isLoginMode = !isLoginMode;
    
    if (isLoginMode) {
        loginForm.style.display = 'block';
        registerForm.style.display = 'none';
        formTitle.textContent = 'Entre para continuar';
        toggleText.innerHTML = `Não tem uma conta? <a href="#" id="toggle-link" class="text-primary fw-bold">Cadastre-se</a>`;
        hideMessage(messageArea);
    } else {
        loginForm.style.display = 'none';
        registerForm.style.display = 'block';
        formTitle.textContent = 'Crie sua conta';
        toggleText.innerHTML = `Já tem uma conta? <a href="#" id="toggle-link" class="text-primary fw-bold">Faça login</a>`;
        hideMessage(registerMessageArea);
    }
    
    // Reatribui o evento ao novo link
    document.getElementById('toggle-link').addEventListener('click', (e) => {
        e.preventDefault();
        toggleForms();
    });
}

// ============================================================
// Função de Login
// ============================================================
async function handleLogin(event) {
    event.preventDefault();
    hideMessage(messageArea);
    
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    
    if (!username || !password) {
        showMessage(messageArea, 'Por favor, preencha todos os campos.', 'warning');
        return;
    }
    
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        const data = await readBody(response);
        
        if (!response.ok) {
            // 401 = credenciais erradas; 429 = muitas tentativas (a mensagem
            // vem do servidor)
            const msg = typeof data === 'string' ? data : '❌ Usuário ou senha inválidos!';
            showMessage(messageArea, msg, 'danger');
            return;
        }
        
        // ✅ Sucesso! Salva o token e recarrega a home, que agora mostra a
        // versão logada (aba "Seguindo")
        localStorage.setItem('b8box_token', data.token);
        localStorage.setItem('b8box_username', data.username);
        showMessage(messageArea, '✅ Login realizado com sucesso!', 'success');

        setTimeout(() => {
            window.location.href = '/';
        }, 800);
        
    } catch (error) {
        showMessage(messageArea, `❌ Erro ao conectar com o servidor: ${error.message}`, 'danger');
    }
}

// ============================================================
// Função de Cadastro
// ============================================================
async function handleRegister(event) {
    event.preventDefault();
    hideMessage(registerMessageArea);
    
    const username = document.getElementById('reg-username').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value.trim();
    
    if (!username || !email || !password) {
        showMessage(registerMessageArea, 'Por favor, preencha todos os campos.', 'warning');
        return;
    }
    
    if (password.length < 6) {
        showMessage(registerMessageArea, 'A senha deve ter pelo menos 6 caracteres.', 'warning');
        return;
    }
    
    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password })
        });
        
        const data = await readBody(response);
        
        if (!response.ok) {
            // inclui as validações do servidor (username com @, e-mail inválido...)
            const msg = typeof data === 'string' ? data : '❌ Erro ao criar conta!';
            showMessage(registerMessageArea, msg, 'danger');
            return;
        }
        
        // ✅ Sucesso! Mostra mensagem e alterna para login
        showMessage(registerMessageArea, '✅ Conta criada com sucesso! Faça login.', 'success');
        
        setTimeout(() => {
            if (!isLoginMode) toggleForms();
            document.getElementById('username').value = username;
            document.getElementById('password').value = '';
            hideMessage(registerMessageArea);
        }, 2000);
        
    } catch (error) {
        showMessage(registerMessageArea, `❌ Erro ao conectar com o servidor: ${error.message}`, 'danger');
    }
}

// ============================================================
// Event Listeners
// ============================================================
loginForm.addEventListener('submit', handleLogin);
registerForm.addEventListener('submit', handleRegister);

// Configura o toggle inicial
document.getElementById('toggle-link').addEventListener('click', (e) => {
    e.preventDefault();
    toggleForms();
});

// ============================================================
// Verifica se o usuário já está logado
// ============================================================
function checkAuth() {
    const token = localStorage.getItem('b8box_token');
    if (token && window.location.pathname === '/' || window.location.pathname === '/index.html') {
        // Se já tiver token, tenta redirecionar para o dashboard
        // Por enquanto, só mantém na página
        console.log('Usuário já logado');
    }
}

checkAuth();