package com.b8box.controller;

import com.b8box.model.User;
import com.b8box.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ============================================================
    // PERFIL DO USUÁRIO LOGADO
    // ============================================================
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        User user = getAuthenticatedUser();
        
        // Remove a senha antes de retornar
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // ============================================================
    // ATUALIZAR PERFIL
    // ============================================================
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody User userRequest) {
        User user = getAuthenticatedUser();
        
        // Atualiza apenas campos permitidos
        if (userRequest.getUsername() != null && !userRequest.getUsername().isEmpty()) {
            // Verifica se o novo username já está em uso
            if (userRepository.existsByUsername(userRequest.getUsername()) && 
                !user.getUsername().equals(userRequest.getUsername())) {
                return ResponseEntity.badRequest().body("❌ Username já está em uso!");
            }
            user.setUsername(userRequest.getUsername());
        }
        
        if (userRequest.getEmail() != null && !userRequest.getEmail().isEmpty()) {
            // Verifica se o novo email já está em uso
            if (userRepository.existsByEmail(userRequest.getEmail()) && 
                !user.getEmail().equals(userRequest.getEmail())) {
                return ResponseEntity.badRequest().body("❌ Email já está em uso!");
            }
            user.setEmail(userRequest.getEmail());
        }
        
        user.setUpdatedAt(java.time.LocalDateTime.now());
        User updated = userRepository.save(user);
        
        // Remove a senha antes de retornar
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }

    // ============================================================
    // ALTERAR SENHA
    // ============================================================
    @PostMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordRequest) {
        User user = getAuthenticatedUser();
        
        String currentPassword = passwordRequest.get("currentPassword");
        String newPassword = passwordRequest.get("newPassword");
        
        // Verifica se a senha atual está correta
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body("❌ Senha atual incorreta!");
        }
        
        // Valida a nova senha
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("❌ A nova senha deve ter pelo menos 6 caracteres!");
        }
        
        // Atualiza a senha
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("message", "✅ Senha alterada com sucesso!"));
    }

    // ============================================================
    // DELETAR CONTA
    // ============================================================
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount() {
        User user = getAuthenticatedUser();
        userRepository.delete(user);
        return ResponseEntity.ok("✅ Conta deletada com sucesso!");
    }

    // ============================================================
    // MÉTODO AUXILIAR PARA PEGAR O USUÁRIO LOGADO
    // ============================================================
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}