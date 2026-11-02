package com.b8box.controller;

import com.b8box.dto.UserProfileDTO;
import com.b8box.dto.UserPublicProfileDTO;
import com.b8box.dto.UserSearchResultDTO;
import com.b8box.model.User;
import com.b8box.repository.FollowRepository;
import com.b8box.repository.PlaylistRepository;
import com.b8box.repository.RatingCommentRepository;
import com.b8box.repository.RatingLikeRepository;
import com.b8box.repository.RatingRepository;
import com.b8box.repository.UserRepository;
import com.b8box.service.AvatarService;
import com.b8box.service.NotificationService;
import com.b8box.util.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired
    private RatingLikeRepository ratingLikeRepository;

    @Autowired
    private RatingCommentRepository ratingCommentRepository;

    @Autowired
    private AvatarService avatarService;

    @Autowired
    private NotificationService notificationService;

    // Corpo do PUT /api/users/me. Campos nulos = "não alterar" (assim um
    // cliente que só manda o username continua funcionando).
    public record UpdateProfileRequest(String username, String bio, List<String> favoriteGenres) {}

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
    // ATUALIZAR PERFIL (username, bio e gêneros favoritos — email é imutável)
    // Tudo é VALIDADO antes de qualquer alteração na entidade: como o método
    // é @Transactional, mexer em um campo e depois devolver 400 por causa de
    // outro faria o Hibernate gravar a mudança parcial mesmo assim.
    // ============================================================
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest body) {
        User user = getAuthenticatedUser();

        // ---------- 1. validar ----------
        String newUsername = body.username() == null ? null : body.username().trim();
        boolean changeUsername = newUsername != null && !newUsername.isBlank()
                && !newUsername.equals(user.getUsername());
        if (changeUsername) {
            // mesmas regras do cadastro (sem "@", sem espaços, 3 a 30 caracteres)
            String usernameError = InputValidator.validateUsername(newUsername);
            if (usernameError != null) {
                return ResponseEntity.badRequest().body(usernameError);
            }
            if (userRepository.existsByUsername(newUsername)) {
                return ResponseEntity.badRequest().body("❌ Username já está em uso!");
            }
        }

        String newBio = body.bio() == null ? null : body.bio().trim();
        if (newBio != null) {
            String bioError = InputValidator.validateBio(newBio);
            if (bioError != null) {
                return ResponseEntity.badRequest().body(bioError);
            }
        }

        List<String> newGenres = null;
        if (body.favoriteGenres() != null) {
            newGenres = InputValidator.normalizeGenres(body.favoriteGenres());
            String genresError = InputValidator.validateGenres(newGenres);
            if (genresError != null) {
                return ResponseEntity.badRequest().body(genresError);
            }
        }

        // ---------- 2. aplicar ----------
        if (changeUsername) {
            user.setUsername(newUsername);
        }
        if (newBio != null) {
            user.setBio(newBio.isEmpty() ? null : newBio);
        }
        if (newGenres != null) {
            user.setFavoriteGenres(newGenres);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

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

        String passwordError = InputValidator.validatePassword(newPassword);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(passwordError);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "✅ Senha alterada com sucesso!"));
    }

    // ============================================================
    // FOTO DE PERFIL
    // ============================================================
    // Recebe a imagem como multipart (campo "file"). A validação e o
    // tratamento (recorte, tamanho, remoção de metadados) ficam no AvatarService.
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        User user = getAuthenticatedUser();
        try {
            String url = avatarService.save(user, file);
            return ResponseEntity.ok(Map.of("avatarUrl", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<?> deleteAvatar() {
        avatarService.delete(getAuthenticatedUser());
        return ResponseEntity.noContent().build();
    }

    // Arquivo acima do limite do Spring (spring.servlet.multipart.max-file-size):
    // responde 400 com mensagem amigável em vez de um erro 500 genérico.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "A imagem deve ter no máximo 2 MB."));
    }

    // ============================================================
    // DELETAR CONTA
    // ============================================================
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<?> deleteAccount() {
        User user = getAuthenticatedUser();
        // Avisos que a pessoa recebeu ou gerou (os ligados às avaliações
        // dela também sairiam junto com elas, mas os outros não).
        notificationService.deleteAllFor(user.getId());
        // A tabela follows aponta pra users sem cascade: sem apagar essas linhas
        // antes, apagar a conta de quem tem seguidores (ou segue alguém) dava
        // erro de chave estrangeira.
        followRepository.deleteByFollowerId(user.getId());
        followRepository.deleteByFollowedId(user.getId());
        // Curtidas que a pessoa deu em avaliações de OUTROS usuários (as curtidas
        // recebidas nas avaliações dela somem junto com as avaliações).
        ratingLikeRepository.deleteByUserId(user.getId());
        // Mesma coisa com os comentários feitos em avaliações de outros.
        ratingCommentRepository.deleteByUserId(user.getId());
        avatarService.delete(user);
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
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        String query = q.trim().toLowerCase();
        return userRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().toLowerCase().contains(query))
                .limit(20)
                .map(u -> new UserSearchResultDTO(u.getId(), u.getUsername(), AvatarService.avatarUrl(u)))
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

        // os cinco contadores numa consulta só (ver UserRepository.profileStats)
        UserRepository.ProfileStats stats = userRepository.profileStats(id, me.getId());
        long albumsCount = stats.getAlbumsCount();
        long publicPlaylistsCount = stats.getPublicPlaylistsCount();
        long followersCount = stats.getFollowersCount();
        long followingCount = stats.getFollowingCount();
        boolean followingByMe = stats.getFollowingByMe() > 0;

        UserPublicProfileDTO dto = new UserPublicProfileDTO();
        dto.setId(target.getId());
        dto.setUsername(target.getUsername());
        dto.setBio(target.getBio());
        dto.setFavoriteGenres(new ArrayList<>(target.getFavoriteGenres()));
        dto.setCreatedAt(target.getCreatedAt() != null ? target.getCreatedAt().toString() : null);
        dto.setAlbumsCount(albumsCount);
        dto.setPublicPlaylistsCount(publicPlaylistsCount);
        dto.setFollowersCount(followersCount);
        dto.setFollowingCount(followingCount);
        dto.setFollowingByMe(followingByMe);
        dto.setAvatarUrl(AvatarService.avatarUrl(target));

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