package com.b8box.controller;

import com.b8box.model.Album;
import com.b8box.model.Music;
import com.b8box.model.spotify.SpotifyAlbumResponse;
import com.b8box.repository.AlbumRepository;
import com.b8box.repository.MusicRepository;
import com.b8box.service.SpotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {

    @Autowired
    private SpotifyService spotifyService;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private MusicRepository musicRepository;

    // ============================================================
    // BUSCAR ÁLBUNS NO SPOTIFY
    // ============================================================
    @GetMapping("/search")
    public ResponseEntity<?> searchAlbums(@RequestParam String q) {
        try {
            SpotifyAlbumResponse response = spotifyService.searchAlbums(q);
            
            List<Map<String, Object>> results = new ArrayList<>();
            if (response != null && response.getAlbums() != null) {
                for (SpotifyAlbumResponse.AlbumItem item : response.getAlbums().getItems()) {
                    Map<String, Object> albumInfo = new HashMap<>();
                    albumInfo.put("spotifyId", item.getId());
                    albumInfo.put("title", item.getName());
                    albumInfo.put("artist", item.getArtists().get(0).getName());
                    albumInfo.put("releaseYear", extractYear(item.getReleaseDate()));
                    albumInfo.put("totalTracks", item.getTotalTracks());
                    albumInfo.put("coverUrl", item.getImages().get(0).getUrl());
                    results.add(albumInfo);
                }
            }
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Erro ao buscar no Spotify: " + e.getMessage());
        }
    }

    // ============================================================
    // IMPORTAR ÁLBUM DO SPOTIFY PARA O B8BOX
    // ============================================================
    @PostMapping("/import/{spotifyId}")
    public ResponseEntity<?> importAlbum(@PathVariable String spotifyId) {
        try {
            // 1. Busca o álbum no Spotify
            SpotifyAlbumResponse.AlbumItem spotifyAlbum = spotifyService.getAlbumById(spotifyId);
            
            if (spotifyAlbum == null) {
                return ResponseEntity.status(404).body("❌ Álbum não encontrado no Spotify");
            }

            // 2. Verifica se já existe no B8Box
            String artistName = spotifyAlbum.getArtists().get(0).getName();
            String albumTitle = spotifyAlbum.getName();
            
            // 3. Cria o álbum no B8Box
            Album album = new Album();
            album.setTitle(albumTitle);
            album.setArtist(artistName);
            album.setReleaseYear(extractYear(spotifyAlbum.getReleaseDate()));
            if (!spotifyAlbum.getImages().isEmpty()) {
                album.setCoverUrl(spotifyAlbum.getImages().get(0).getUrl());
            }
            
            Album savedAlbum = albumRepository.save(album);
            
            // 4. Importa as músicas
            if (spotifyAlbum.getTracks() != null && spotifyAlbum.getTracks().getItems() != null) {
                for (SpotifyAlbumResponse.TrackItem track : spotifyAlbum.getTracks().getItems()) {
                    Music music = new Music();
                    music.setTitle(track.getName());
                    music.setTrackNumber(track.getTrackNumber());
                    music.setDuration(track.getDurationMs() / 1000); // Converte ms para segundos
                    music.setAlbum(savedAlbum);
                    musicRepository.save(music);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Álbum importado com sucesso!");
            response.put("album", savedAlbum);
            response.put("tracks", spotifyAlbum.getTotalTracks());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Erro ao importar álbum: " + e.getMessage());
        }
    }

    // ============================================================
    // MÉTODO AUXILIAR
    // ============================================================
    private Integer extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.isEmpty()) return null;
        try {
            // Formato: "2024-01-15" ou "2024"
            if (releaseDate.contains("-")) {
                return Integer.parseInt(releaseDate.split("-")[0]);
            }
            return Integer.parseInt(releaseDate);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}