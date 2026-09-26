package com.b8box.controller;

import com.b8box.dto.UserProfileDTO;
import com.b8box.model.User;
import com.b8box.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ============================================================
    // PERFIL
    // ============================================================
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(UserProfileDTO.fromEntity(user));
    }

    // ============================================================
    // ATUALIZAR PERFIL (só username — email é imutável)
    // ============================================================
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        User user = getAuthenticatedUser();

        String newUsername = body.get("username");

        if (newUsername != null && !newUsername.isBlank()
                && !newUsername.equals(user.getUsername())) {

            if (userRepository.existsByUsername(newUsername)) {
                return ResponseEntity.badRequest().body("❌ Username já está em uso!");
            }
            user.setUsername(newUsername);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        // ⚠️ Email NÃO é atualizado (imutável)

        return ResponseEntity.ok(UserProfileDTO.fromEntity(user));
    }

    // ============================================================
    // ALTERAR SENHA
    // ============================================================
    @PostMapping("/me/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        User user = getAuthenticatedUser();

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("❌ Preencha todos os campos.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body("❌ Senha atual incorreta!");
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("❌ A nova senha deve ter pelo menos 6 caracteres!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "✅ Senha alterada com sucesso!"));
    }

    // ============================================================
    // DELETAR CONTA
    // ============================================================
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<?> deleteAccount() {
        User user = getAuthenticatedUser();
        userRepository.delete(user);
        return ResponseEntity.ok("✅ Conta deletada com sucesso!");
    }

    // ============================================================
    // AUXILIAR — agora busca por EMAIL
    // ============================================================
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // agora é o email
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}