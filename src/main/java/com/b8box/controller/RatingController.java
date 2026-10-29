package com.b8box.controller;

import com.b8box.dto.RatingResponseDTO;
import com.b8box.model.Album;
import com.b8box.model.Music;
import com.b8box.model.Rating;
import com.b8box.model.User;
import com.b8box.repository.AlbumRepository;
import com.b8box.repository.MusicRepository;
import com.b8box.repository.RatingRepository;
import com.b8box.repository.UserRepository;
import com.b8box.service.LikeService;
import com.b8box.util.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private MusicRepository musicRepository;

    @Autowired
    private LikeService likeService;

    // ============================================================
    // LISTAR AVALIAÇÕES DO USUÁRIO LOGADO
    // ============================================================
    @GetMapping("/me")
    public List<Rating> getMyRatings() {
        User user = getAuthenticatedUser();
        return ratingRepository.findByUserId(user.getId());
    }

    // ============================================================
    // LISTAR AVALIAÇÕES DE UM ÁLBUM
    // Devolve um DTO (e não a entidade crua) porque Rating.user tem
    // @JsonIgnore — pra não vazar a senha do User na serialização. O DTO
    // leva userId + username explícitos.
    // ============================================================
    @GetMapping("/album/{albumId}")
    @Transactional(readOnly = true)
    public List<RatingResponseDTO> getRatingsByAlbum(@PathVariable Long albumId) {
        User me = getAuthenticatedUser();
        List<Rating> ratings = ratingRepository.findByAlbumId(albumId);

        // curtidas de todas as avaliações do álbum em duas consultas
        List<Long> ids = ratings.stream().map(Rating::getId).collect(Collectors.toList());
        Map<Long, Long> counts = likeService.countFor(ids);
        Set<Long> liked = likeService.likedBy(me.getId(), ids);

        return ratings.stream()
                .map(r -> {
                    RatingResponseDTO dto = toDto(r);
                    dto.setLikesCount(counts.getOrDefault(r.getId(), 0L));
                    dto.setLikedByMe(liked.contains(r.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ============================================================
    // LISTAR AVALIAÇÕES DE UMA MÚSICA
    // ============================================================
    @GetMapping("/music/{musicId}")
    @Transactional(readOnly = true)
    public List<RatingResponseDTO> getRatingsByMusic(@PathVariable Long musicId) {
        return ratingRepository.findByMusicId(musicId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CRIAR AVALIAÇÃO PARA UM ÁLBUM
    // ============================================================
    @PostMapping("/album/{albumId}")
    public ResponseEntity<?> rateAlbum(@PathVariable Long albumId, @RequestBody Rating ratingRequest) {
        User user = getAuthenticatedUser();

        // NOVO: o servidor valida a nota (0 a 10) e o tamanho da resenha — antes
        // só o formulário limitava, e quem chamasse a API direto podia mandar
        // nota 9999 e estragar a média do álbum.
        String error = InputValidator.validateRating(ratingRequest.getScore(), ratingRequest.getReview());
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Álbum não encontrado"));

        List<Rating> existingRatings = ratingRepository.findByUserIdAndAlbumId(user.getId(), albumId);
        if (!existingRatings.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Você já avaliou este álbum!");
        }

        Rating rating = new Rating();
        rating.setScore(ratingRequest.getScore());
        rating.setReview(ratingRequest.getReview());
        rating.setUser(user);
        rating.setAlbum(album);
        rating.setCreatedAt(java.time.LocalDateTime.now());

        try {
            Rating saved = ratingRepository.save(rating);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (DataIntegrityViolationException e) {
            // dois cliques rápidos: a restrição única (usuário, álbum) barrou o segundo
            return ResponseEntity.badRequest().body("❌ Você já avaliou este álbum!");
        }
    }

    // ============================================================
    // CRIAR AVALIAÇÃO PARA UMA MÚSICA
    // ============================================================
    @PostMapping("/music/{musicId}")
    public ResponseEntity<?> rateMusic(@PathVariable Long musicId, @RequestBody Rating ratingRequest) {
        User user = getAuthenticatedUser();

        String error = InputValidator.validateRating(ratingRequest.getScore(), ratingRequest.getReview());
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }

        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new RuntimeException("Música não encontrada"));

        List<Rating> existingRatings = ratingRepository.findByUserIdAndMusicId(user.getId(), musicId);
        if (!existingRatings.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Você já avaliou esta música!");
        }

        Rating rating = new Rating();
        rating.setScore(ratingRequest.getScore());
        rating.setReview(ratingRequest.getReview());
        rating.setUser(user);
        rating.setMusic(music);
        rating.setCreatedAt(java.time.LocalDateTime.now());

        try {
            Rating saved = ratingRepository.save(rating);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("❌ Você já avaliou esta música!");
        }
    }

    // ============================================================
    // ATUALIZAR AVALIAÇÃO
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRating(@PathVariable Long id, @RequestBody Rating ratingRequest) {
        User user = getAuthenticatedUser();

        String error = InputValidator.validateRating(ratingRequest.getScore(), ratingRequest.getReview());
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }

        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));

        if (!rating.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Você não tem permissão para alterar esta avaliação!");
        }

        rating.setScore(ratingRequest.getScore());
        rating.setReview(ratingRequest.getReview());
        rating.setUpdatedAt(java.time.LocalDateTime.now());

        Rating updated = ratingRepository.save(rating);
        return ResponseEntity.ok(toDto(updated));
    }

    // ============================================================
    // DELETAR AVALIAÇÃO
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRating(@PathVariable Long id) {
        User user = getAuthenticatedUser();

        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));

        if (!rating.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Você não tem permissão para deletar esta avaliação!");
        }

        ratingRepository.delete(rating);
        return ResponseEntity.ok("✅ Avaliação deletada com sucesso!");
    }

    // ============================================================
    // CURTIR / DESCURTIR UMA AVALIAÇÃO
    // Respondem { likesCount, likedByMe }. Repetir a ação não dá erro.
    // ============================================================
    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeRating(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        Rating rating = ratingRepository.findById(id).orElse(null);
        if (rating == null) {
            return ResponseEntity.notFound().build();
        }
        if (rating.getUser().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body("❌ Você não pode curtir a sua própria avaliação.");
        }
        likeService.like(user, rating);
        return ResponseEntity.ok(likeService.status(id, user.getId()));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlikeRating(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        if (!ratingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        likeService.unlike(user.getId(), id);
        return ResponseEntity.ok(likeService.status(id, user.getId()));
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================
    private RatingResponseDTO toDto(Rating r) {
        RatingResponseDTO dto = new RatingResponseDTO();
        dto.setId(r.getId());
        dto.setScore(r.getScore());
        dto.setReview(r.getReview());
        if (r.getUser() != null) {
            dto.setUserId(r.getUser().getId());
            dto.setUsername(r.getUser().getUsername());
        }
        dto.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return dto;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // agora é o email
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}