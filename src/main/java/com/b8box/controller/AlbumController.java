package com.b8box.controller;

import com.b8box.dto.AlbumResponseDTO;
import com.b8box.dto.RatingResponseDTO;
import com.b8box.model.Album;
import com.b8box.model.Rating;  // ← IMPORT ADICIONADO
import com.b8box.repository.AlbumRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;  // ← IMPORT CORRETO
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;  // ← IMPORT ADICIONADO

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    @Autowired
    private AlbumRepository albumRepository;

    // ============================================================
    // LISTAR TODOS OS ÁLBUNS
    // ============================================================
    @GetMapping
    @Transactional(readOnly = true)  // ← AGORA FUNCIONA
    public List<AlbumResponseDTO> getAllAlbums() {
        return albumRepository.findAll().stream()
            .map(album -> {
                AlbumResponseDTO dto = new AlbumResponseDTO();
                dto.setId(album.getId());
                dto.setTitle(album.getTitle());
                dto.setArtist(album.getArtist());
                dto.setCoverUrl(album.getCoverUrl());
                dto.setReleaseYear(album.getReleaseYear());
                
                // Calcula média
                if (!album.getRatings().isEmpty()) {
                    double avg = album.getRatings().stream()
                        .mapToDouble(Rating::getScore)
                        .average()
                        .orElse(0.0);
                    dto.setAverageRating(Math.round(avg * 10.0) / 10.0);
                }
                
                // Mapeia avaliações
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
            })
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
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<Album> updateAlbum(@PathVariable Long id, @RequestBody Album album) {
        return albumRepository.findById(id)
                .map(existing -> {
                    existing.setTitle(album.getTitle());
                    existing.setArtist(album.getArtist());
                    existing.setCoverUrl(album.getCoverUrl());
                    existing.setReleaseYear(album.getReleaseYear());
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
}