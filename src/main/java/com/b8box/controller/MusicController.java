package com.b8box.controller;

import com.b8box.model.Music;
import com.b8box.repository.MusicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/musics")
public class MusicController {

    @Autowired
    private MusicRepository musicRepository;

    @GetMapping
    public List<Music> getAllMusics() {
        return musicRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Music> getMusicById(@PathVariable Long id) {
        return musicRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/album/{albumId}")
    public List<Music> getMusicsByAlbum(@PathVariable Long albumId) {
        return musicRepository.findByAlbumId(albumId);
    }

    @GetMapping("/search")
    public List<Music> searchMusics(@RequestParam String title) {
        return musicRepository.findByTitleContainingIgnoreCase(title);
    }

    @PostMapping
    public ResponseEntity<Music> createMusic(@RequestBody Music music) {
        Music saved = musicRepository.save(music);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Music> updateMusic(@PathVariable Long id, @RequestBody Music music) {
        return musicRepository.findById(id)
                .map(existing -> {
                    existing.setTitle(music.getTitle());
                    existing.setTrackNumber(music.getTrackNumber());
                    existing.setDuration(music.getDuration());
                    existing.setAlbum(music.getAlbum());
                    return ResponseEntity.ok(musicRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMusic(@PathVariable Long id) {
        return musicRepository.findById(id)
                .map(music -> {
                    musicRepository.delete(music);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}