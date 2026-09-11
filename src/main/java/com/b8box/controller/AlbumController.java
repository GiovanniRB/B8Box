package com.b8box.controller;

import com.b8box.dto.AlbumResponseDTO;
import com.b8box.dto.RatingResponseDTO;
import com.b8box.model.Album;
import com.b8box.model.Rating;
import com.b8box.model.User;
import com.b8box.repository.AlbumRepository;
import com.b8box.repository.RatingRepository;
import com.b8box.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private UserRepository userRepository;

    // ============================================================
    // LISTAR TODOS OS ÁLBUNS (GLOBAL — usar na tela de Explorar/admin,
    // NÃO representa "meus álbuns")
    // ============================================================
    @GetMapping
    @Transactional(readOnly = true)
    public List<AlbumResponseDTO> getAllAlbums() {
        return albumRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    // ============================================================
    // "MEUS ÁLBUNS" — só os álbuns em que o usuário logado já tem
    // pelo menos uma avaliação.
    // ============================================================
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public List<AlbumResponseDTO> getMyAlbums() {
        User user = getAuthenticatedUser();
        List<Rating> myRatings = ratingRepository.findByUserId(user.getId());

        Map<Long, Album> albumsById = new LinkedHashMap<>();
        for (Rating r : myRatings) {
            if (r.getAlbum() != null) {
                albumsById.put(r.getAlbum().getId(), r.getAlbum());
            }
        }

        return albumsById.values().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    // ============================================================
    // BUSCAR ÁLBUM POR ID
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<Album> getAlbumById(@PathVariable Long id) {
        return albumRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================================================
    // BUSCAR ÁLBUNS POR ARTISTA
    // ============================================================
    @GetMapping("/artist/{artist}")
    public List<Album> getAlbumsByArtist(@PathVariable String artist) {
        return albumRepository.findByArtistContainingIgnoreCase(artist);
    }

    // ============================================================
    // BUSCAR ÁLBUNS POR TÍTULO
    // ============================================================
    @GetMapping("/search")
    public List<Album> searchAlbums(@RequestParam String title) {
        return albumRepository.findByTitleContainingIgnoreCase(title);
    }

    // ============================================================
    // CRIAR NOVO ÁLBUM
    // ============================================================
    @PostMapping
    public ResponseEntity<Album> createAlbum(@RequestBody Album album) {
        Album saved = albumRepository.save(album);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ============================================================
    // ATUALIZAR ÁLBUM
    // NOVO: agora também aceita spotifyId. Isso é o que permite "religar"
    // um álbum antigo (importado antes do dedupe existir, com
    // spotify_id = null) ao spotifyId real — assim ele passa a ser
    // reconhecido pelo dedupe em importações futuras.
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAlbum(@PathVariable Long id, @RequestBody Album album) {
        return albumRepository.findById(id)
                .map(existing -> {
                    existing.setTitle(album.getTitle());
                    existing.setArtist(album.getArtist());
                    existing.setCoverUrl(album.getCoverUrl());
                    existing.setReleaseYear(album.getReleaseYear());
                    if (album.getSpotifyId() != null && !album.getSpotifyId().isBlank()) {
                        existing.setSpotifyId(album.getSpotifyId());
                    }
                    return ResponseEntity.ok(albumRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================================================
    // DELETAR ÁLBUM
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable Long id) {
        return albumRepository.findById(id)
                .map(album -> {
                    albumRepository.delete(album);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================
    private AlbumResponseDTO toDto(Album album) {
        AlbumResponseDTO dto = new AlbumResponseDTO();
        dto.setId(album.getId());
        dto.setTitle(album.getTitle());
        dto.setArtist(album.getArtist());
        dto.setCoverUrl(album.getCoverUrl());
        dto.setReleaseYear(album.getReleaseYear());

        if (!album.getRatings().isEmpty()) {
            double avg = album.getRatings().stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);
            dto.setAverageRating(Math.round(avg * 10.0) / 10.0);
        }

        List<RatingResponseDTO> ratingDTOs = album.getRatings().stream()
            .map(r -> {
                RatingResponseDTO rdto = new RatingResponseDTO();
                rdto.setId(r.getId());
                rdto.setScore(r.getScore());
                rdto.setReview(r.getReview());
                rdto.setUsername(r.getUser().getUsername());
                rdto.setCreatedAt(r.getCreatedAt().toString());
                return rdto;
            })
            .collect(Collectors.toList());
        dto.setRatings(ratingDTOs);

        return dto;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}