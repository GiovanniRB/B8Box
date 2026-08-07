package com.b8box.controller;

import com.b8box.model.User;
import com.b8box.repository.UserRepository;
import com.b8box.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ============================================================
    // ENDPOINT DE CADASTRO
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        System.out.println("🔍 Senha recebida: " + user.getPassword());
        
        // ... validações ...
        
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        System.out.println("🔍 Senha criptografada: " + encodedPassword);
        user.setPassword(encodedPassword);
        
        User savedUser = userRepository.save(user);
        System.out.println("✅ Usuário salvo com ID: " + savedUser.getId());
        
        savedUser.setPassword(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // ============================================================
    // ENDPOINT DE LOGIN
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        try {
            // 1. Autentica o usuário com Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // 2. Busca os dados do usuário
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());

            // 3. Gera o token JWT
            String token = jwtUtil.generateToken(userDetails);

            // 4. Retorna o token
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", userDetails.getUsername());
            response.put("message", "✅ Login realizado com sucesso!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Usuário ou senha inválidos!");
        }
    }
}