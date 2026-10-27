package com.b8box.controller;

import com.b8box.model.User;
import com.b8box.repository.UserRepository;
import com.b8box.service.LoginAttemptService;
import com.b8box.service.UserDetailsServiceImpl;
import com.b8box.util.InputValidator;
import com.b8box.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // O corpo das requisições vem em objetos próprios (e não na entidade User).
    // Antes o cadastro recebia um User direto: dava pra mandar "id" no JSON e o
    // save() tratava como ATUALIZAÇÃO da conta com aquele id (sobrescrevendo
    // username, e-mail e senha de outra pessoa). Com estes records só os
    // campos listados entram; qualquer outro é ignorado.
    public record RegisterRequest(String username, String email, String password) {}
    public record LoginRequest(String username, String password) {}

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttempts;

    // ============================================================
    // ENDPOINT DE CADASTRO
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String username = request.username() == null ? null : request.username().trim();
        String email = request.email() == null ? null : request.email().trim().toLowerCase();
        String password = request.password();

        String error = InputValidator.validateUsername(username);
        if (error == null) error = InputValidator.validateEmail(email);
        if (error == null) error = InputValidator.validatePassword(password);
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body("❌ Username já está em uso!");
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("❌ Email já está em uso!");
        }

        User user = new User(username, email, passwordEncoder.encode(password));

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // dois cadastros simultâneos com o mesmo username/e-mail
            return ResponseEntity.badRequest().body("❌ Username ou email já está em uso!");
        }

        // Resposta só com dados públicos (nunca a senha, nem o hash)
        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("username", saved.getUsername());
        response.put("email", saved.getEmail());
        response.put("createdAt", saved.getCreatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================================
    // ENDPOINT DE LOGIN (aceita username OU e-mail)
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String login = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password();

        if (login.isEmpty() || password.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Usuário ou senha inválidos!");
        }

        // Proteção contra força bruta: 5 erros em 10 minutos bloqueiam o login
        String attemptKey = login.toLowerCase();
        if (loginAttempts.isBlocked(attemptKey)) {
            log.warn("Login bloqueado temporariamente por excesso de tentativas");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("❌ Muitas tentativas de login. Tente de novo em alguns minutos.");
        }

        try {
            // 1. Autentica com Spring Security (o principal resultante é o e-mail)
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login, password));
            UserDetails userDetails = (UserDetails) auth.getPrincipal();

            // 2. Busca o usuário pra devolver o USERNAME de verdade.
            // (userDetails.getUsername() é o e-mail — foi por isso que algumas
            // telas mostravam o e-mail no lugar do nome no topo.)
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado"));

            // 3. Gera o token JWT
            String token = jwtUtil.generateToken(userDetails);

            loginAttempts.reset(attemptKey);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("message", "✅ Login realizado com sucesso!");
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            loginAttempts.recordFailure(attemptKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Usuário ou senha inválidos!");
        }
    }
}