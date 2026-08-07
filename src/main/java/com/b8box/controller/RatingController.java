package com.b8box.controller;

import com.b8box.model.Album;
import com.b8box.model.Music;
import com.b8box.model.Rating;
import com.b8box.model.User;
import com.b8box.repository.AlbumRepository;
import com.b8box.repository.MusicRepository;
import com.b8box.repository.RatingRepository;
import com.b8box.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    // ============================================================
    @GetMapping("/album/{albumId}")
    public List<Rating> getRatingsByAlbum(@PathVariable Long albumId) {
        return ratingRepository.findByAlbumId(albumId);
    }

    // ============================================================
    // LISTAR AVALIAÇÕES DE UMA MÚSICA
    // ============================================================
    @GetMapping("/music/{musicId}")
    public List<Rating> getRatingsByMusic(@PathVariable Long musicId) {
        return ratingRepository.findByMusicId(musicId);
    }

    // ============================================================
    // CRIAR AVALIAÇÃO PARA UM ÁLBUM
    // ============================================================
    @PostMapping("/album/{albumId}")
    public ResponseEntity<?> rateAlbum(@PathVariable Long albumId, @RequestBody Rating ratingRequest) {
        User user = getAuthenticatedUser();
        
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Álbum não encontrado"));
        
        // Verifica se o usuário já avaliou este álbum
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
        
        Rating saved = ratingRepository.save(rating);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ============================================================
    // CRIAR AVALIAÇÃO PARA UMA MÚSICA
    // ============================================================
    @PostMapping("/music/{musicId}")
    public ResponseEntity<?> rateMusic(@PathVariable Long musicId, @RequestBody Rating ratingRequest) {
        User user = getAuthenticatedUser();
        
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new RuntimeException("Música não encontrada"));
        
        // Verifica se o usuário já avaliou esta música
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
        
        Rating saved = ratingRepository.save(rating);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ============================================================
    // ATUALIZAR AVALIAÇÃO
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRating(@PathVariable Long id, @RequestBody Rating ratingRequest) {
        User user = getAuthenticatedUser();
        
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));
        
        // Verifica se o usuário é o dono da avaliação
        if (!rating.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Você não tem permissão para alterar esta avaliação!");
        }
        
        rating.setScore(ratingRequest.getScore());
        rating.setReview(ratingRequest.getReview());
        rating.setUpdatedAt(java.time.LocalDateTime.now());
        
        Rating updated = ratingRepository.save(rating);
        return ResponseEntity.ok(updated);
    }

    // ============================================================
    // DELETAR AVALIAÇÃO
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRating(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));
        
        // Verifica se o usuário é o dono da avaliação
        if (!rating.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Você não tem permissão para deletar esta avaliação!");
        }
        
        ratingRepository.delete(rating);
        return ResponseEntity.ok("✅ Avaliação deletada com sucesso!");
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