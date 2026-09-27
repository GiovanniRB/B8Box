package com.b8box.controller;

import com.b8box.dto.UserProfileDTO;
import com.b8box.dto.UserPublicProfileDTO;
import com.b8box.dto.UserSearchResultDTO;
import com.b8box.model.User;
import com.b8box.repository.FollowRepository;
import com.b8box.repository.PlaylistRepository;
import com.b8box.repository.RatingRepository;
import com.b8box.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private FollowRepository followRepository;

    // ============================================================
    // PERFIL (o meu, autenticado)
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
    // NOVO: BUSCAR USUÁRIOS POR NOME (aba "Perfis" do Explorar)
    // Filtra em memória — volume baixo o suficiente pra não precisar de
    // uma query dedicada no repository agora.
    // ============================================================
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public List<UserSearchResultDTO> searchUsers(@RequestParam String q) {
        String query = q.toLowerCase();
        return userRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().toLowerCase().contains(query))
                .limit(20)
                .map(u -> new UserSearchResultDTO(u.getId(), u.getUsername()))
                .collect(Collectors.toList());
    }

    // ============================================================
    // NOVO: PERFIL PÚBLICO DE UM USUÁRIO (tela user-profile.html)
    // ============================================================
    @GetMapping("/{id}/public")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPublicProfile(@PathVariable Long id) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        User me = getAuthenticatedUser();

        long albumsCount = ratingRepository.findByUserId(id).stream()
                .map(r -> r.getAlbum() != null ? r.getAlbum().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long publicPlaylistsCount = playlistRepository.findByUserIdAndIsPublicTrue(id).size();
        long followersCount = followRepository.countByFollowedId(id);
        long followingCount = followRepository.countByFollowerId(id);
        boolean followingByMe = followRepository.existsByFollowerIdAndFollowedId(me.getId(), id);

        UserPublicProfileDTO dto = new UserPublicProfileDTO();
        dto.setId(target.getId());
        dto.setUsername(target.getUsername());
        dto.setCreatedAt(target.getCreatedAt() != null ? target.getCreatedAt().toString() : null);
        dto.setAlbumsCount(albumsCount);
        dto.setPublicPlaylistsCount(publicPlaylistsCount);
        dto.setFollowersCount(followersCount);
        dto.setFollowingCount(followingCount);
        dto.setFollowingByMe(followingByMe);

        return ResponseEntity.ok(dto);
    }

    // ============================================================
    // AUXILIAR — busca por EMAIL (identidade do JWT)
    // ============================================================
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}