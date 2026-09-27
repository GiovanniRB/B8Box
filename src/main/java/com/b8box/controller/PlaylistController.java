package com.b8box.controller;

import com.b8box.model.Album;
import com.b8box.model.Music;
import com.b8box.model.Playlist;
import com.b8box.model.PlaylistItem;
import com.b8box.model.User;
import com.b8box.repository.MusicRepository;
import com.b8box.repository.PlaylistItemRepository;
import com.b8box.repository.PlaylistRepository;
import com.b8box.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistItemRepository playlistItemRepository;

    @Autowired
    private MusicRepository musicRepository;

    @Autowired
    private UserRepository userRepository;

    // ============================================================
    // LISTAR MINHAS PLAYLISTS
    // ============================================================
    @GetMapping("/me")
    public List<Playlist> getMyPlaylists() {
        User user = getAuthenticatedUser();
        return playlistRepository.findByUserId(user.getId());
    }

    // ============================================================
    // LISTAR PLAYLISTS PÚBLICAS
    // ============================================================
    @GetMapping("/public")
    public List<Playlist> getPublicPlaylists() {
        return playlistRepository.findByIsPublicTrue();
    }

    // ============================================================
    // NOVO: BUSCAR PLAYLISTS PÚBLICAS POR TÍTULO (aba "Playlists" do
    // Explorar). Filtra em memória sobre as públicas — mesmo raciocínio
    // da busca de usuários: volume baixo, não compensa nova query agora.
    // ============================================================
    @GetMapping("/public/search")
    public List<Playlist> searchPublicPlaylists(@RequestParam String q) {
        String query = q.toLowerCase();
        return playlistRepository.findByIsPublicTrue().stream()
                .filter(p -> p.getTitle() != null && p.getTitle().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    // ============================================================
    // NOVO: PLAYLISTS PÚBLICAS DE UM USUÁRIO ESPECÍFICO
    // Usado pela tela de perfil público (user-profile.html)
    // ============================================================
    @GetMapping("/user/{userId}")
    public List<Playlist> getUserPublicPlaylists(@PathVariable Long userId) {
        return playlistRepository.findByUserIdAndIsPublicTrue(userId);
    }

    // ============================================================
    // BUSCAR PLAYLIST POR ID
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getPlaylistById(@PathVariable Long id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada"));

        User user = getAuthenticatedUser();
        if (!playlist.getIsPublic() && !playlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Esta playlist é privada!");
        }

        return ResponseEntity.ok(playlist);
    }

    // ============================================================
    // CRIAR PLAYLIST
    // ============================================================
    @PostMapping
    public ResponseEntity<?> createPlaylist(@RequestBody Playlist playlistRequest) {
        User user = getAuthenticatedUser();

        Playlist playlist = new Playlist();
        playlist.setTitle(playlistRequest.getTitle());
        playlist.setDescription(playlistRequest.getDescription());
        playlist.setIsPublic(playlistRequest.getIsPublic() != null && playlistRequest.getIsPublic());
        playlist.setUser(user);
        playlist.setCreatedAt(java.time.LocalDateTime.now());

        Playlist saved = playlistRepository.save(playlist);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ============================================================
    // ATUALIZAR PLAYLIST
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlaylist(@PathVariable Long id, @RequestBody Playlist playlistRequest) {
        User user = getAuthenticatedUser();

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada"));

        if (!playlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Você não tem permissão para alterar esta playlist!");
        }

        playlist.setTitle(playlistRequest.getTitle());
        playlist.setDescription(playlistRequest.getDescription());
        playlist.setIsPublic(playlistRequest.getIsPublic() != null && playlistRequest.getIsPublic());
        playlist.setUpdatedAt(java.time.LocalDateTime.now());

        Playlist updated = playlistRepository.save(playlist);
        return ResponseEntity.ok(updated);
    }

    // ============================================================
    // DELETAR PLAYLIST
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlaylist(@PathVariable Long id) {
        User user = getAuthenticatedUser();

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada"));

        if (!playlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Você não tem permissão para deletar esta playlist!");
        }

        playlistItemRepository.deleteByPlaylistId(id);
        playlistRepository.delete(playlist);

        return ResponseEntity.ok("✅ Playlist deletada com sucesso!");
    }

    // ============================================================
    // ADICIONAR MÚSICA À PLAYLIST
    // ============================================================
    @PostMapping("/{playlistId}/musics/{musicId}")
    public ResponseEntity<?> addMusicToPlaylist(@PathVariable Long playlistId, @PathVariable Long musicId) {
        User user = getAuthenticatedUser();

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada"));

        if (!playlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Você não tem permissão para adicionar músicas a esta playlist!");
        }

        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new RuntimeException("Música não encontrada"));

        List<PlaylistItem> existingItems = playlistItemRepository.findByPlaylistId(playlistId);
        for (PlaylistItem item : existingItems) {
            if (item.getMusic().getId().equals(musicId)) {
                return ResponseEntity.badRequest().body("❌ Esta música já está na playlist!");
            }
        }

        PlaylistItem item = new PlaylistItem();
        item.setPlaylist(playlist);
        item.setMusic(music);
        item.setPosition(existingItems.size() + 1);

        PlaylistItem saved = playlistItemRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ============================================================
    // REMOVER MÚSICA DA PLAYLIST
    // ============================================================
    @DeleteMapping("/{playlistId}/musics/{musicId}")
    public ResponseEntity<?> removeMusicFromPlaylist(@PathVariable Long playlistId, @PathVariable Long musicId) {
        User user = getAuthenticatedUser();

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada"));

        if (!playlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Você não tem permissão para remover músicas desta playlist!");
        }

        List<PlaylistItem> items = playlistItemRepository.findByPlaylistId(playlistId);
        PlaylistItem itemToRemove = items.stream()
                .filter(item -> item.getMusic().getId().equals(musicId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Música não encontrada na playlist"));

        playlistItemRepository.delete(itemToRemove);
        reorganizePositions(playlistId);

        return ResponseEntity.ok("✅ Música removida da playlist com sucesso!");
    }

    // ============================================================
    // LISTAR MÚSICAS DE UMA PLAYLIST (cru — mantido por compatibilidade)
    // ============================================================
    @GetMapping("/{playlistId}/musics")
    public ResponseEntity<?> getPlaylistMusics(@PathVariable Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada"));

        User user = getAuthenticatedUser();
        if (!playlist.getIsPublic() && !playlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Esta playlist é privada!");
        }

        List<PlaylistItem> items = playlistItemRepository.findByPlaylistId(playlistId);
        return ResponseEntity.ok(items);
    }

    // ============================================================
    // LISTAR ÁLBUNS DE UMA PLAYLIST (agrupado)
    // ============================================================
    @GetMapping("/{playlistId}/albums")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPlaylistAlbums(@PathVariable Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada"));

        User user = getAuthenticatedUser();
        if (!playlist.getIsPublic() && !playlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Esta playlist é privada!");
        }

        List<PlaylistItem> items = playlistItemRepository.findByPlaylistId(playlistId);

        Map<Long, Album> albumsById = new LinkedHashMap<>();
        for (PlaylistItem item : items) {
            Album album = item.getMusic().getAlbum();
            if (album != null) {
                albumsById.put(album.getId(), album);
            }
        }

        List<Map<String, Object>> result = albumsById.values().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("artist", a.getArtist());
            m.put("coverUrl", a.getCoverUrl());
            m.put("spotifyId", a.getSpotifyId());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ============================================================
    // REMOVER UM ÁLBUM INTEIRO DA PLAYLIST
    // ============================================================
    @DeleteMapping("/{playlistId}/albums/{albumId}")
    public ResponseEntity<?> removeAlbumFromPlaylist(@PathVariable Long playlistId, @PathVariable Long albumId) {
        User user = getAuthenticatedUser();

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada"));

        if (!playlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Você não tem permissão para remover álbuns desta playlist!");
        }

        List<PlaylistItem> items = playlistItemRepository.findByPlaylistId(playlistId);
        List<PlaylistItem> toRemove = items.stream()
                .filter(item -> item.getMusic().getAlbum() != null
                        && item.getMusic().getAlbum().getId().equals(albumId))
                .collect(Collectors.toList());

        if (toRemove.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        playlistItemRepository.deleteAll(toRemove);
        reorganizePositions(playlistId);

        return ResponseEntity.ok("✅ Álbum removido da playlist com sucesso!");
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================
    private void reorganizePositions(Long playlistId) {
        List<PlaylistItem> remainingItems = playlistItemRepository.findByPlaylistId(playlistId);
        for (int i = 0; i < remainingItems.size(); i++) {
            remainingItems.get(i).setPosition(i + 1);
            playlistItemRepository.save(remainingItems.get(i));
        }
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}