package com.b8box.controller;

import com.b8box.dto.FeedItemDTO;
import com.b8box.model.User;
import com.b8box.repository.UserRepository;
import com.b8box.service.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FeedController {

    @Autowired
    private FeedService feedService;

    @Autowired
    private UserRepository userRepository;

    // ============================================================
    // FEED: avaliações e playlists públicas de quem eu sigo
    // ============================================================
    @GetMapping("/feed")
    public List<FeedItemDTO> getFeed() {
        User me = getAuthenticatedUser();
        return feedService.getFeed(me.getId());
    }

    // ============================================================
    // AVALIAÇÕES RECENTES DE UM USUÁRIO (perfil público)
    // ============================================================
    @GetMapping("/users/{userId}/ratings")
    public ResponseEntity<?> getUserRatings(@PathVariable Long userId) {
        User me = getAuthenticatedUser();
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(feedService.getUserRatings(userId, me.getId()));
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}
