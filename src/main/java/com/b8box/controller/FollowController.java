package com.b8box.controller;

import com.b8box.model.Follow;
import com.b8box.model.User;
import com.b8box.repository.FollowRepository;
import com.b8box.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    // ============================================================
    // SEGUIR UM USUÁRIO
    // ============================================================
    @PostMapping("/{userId}")
    public ResponseEntity<?> follow(@PathVariable Long userId) {
        User me = getAuthenticatedUser();

        if (me.getId().equals(userId)) {
            return ResponseEntity.badRequest().body("❌ Você não pode seguir a si mesmo!");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (followRepository.existsByFollowerIdAndFollowedId(me.getId(), userId)) {
            return ResponseEntity.badRequest().body("❌ Você já segue este usuário!");
        }

        followRepository.save(new Follow(me, target));
        return ResponseEntity.status(HttpStatus.CREATED).body(buildStatus(userId, me.getId()));
    }

    // ============================================================
    // DEIXAR DE SEGUIR
    // ============================================================
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> unfollow(@PathVariable Long userId) {
        User me = getAuthenticatedUser();
        followRepository.deleteByFollowerIdAndFollowedId(me.getId(), userId);
        return ResponseEntity.ok(buildStatus(userId, me.getId()));
    }

    // ============================================================
    // STATUS (segue ou não + contadores) — usado pela tela de perfil
    // público pra saber o estado inicial do botão
    // ============================================================
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getStatus(@PathVariable Long userId) {
        User me = getAuthenticatedUser();
        return ResponseEntity.ok(buildStatus(userId, me.getId()));
    }

    // ============================================================
    // AUXILIARES
    // ============================================================
    private Map<String, Object> buildStatus(Long targetUserId, Long meId) {
        Map<String, Object> result = new HashMap<>();
        result.put("followersCount", followRepository.countByFollowedId(targetUserId));
        result.put("followingCount", followRepository.countByFollowerId(targetUserId));
        result.put("isFollowing", followRepository.existsByFollowerIdAndFollowedId(meId, targetUserId));
        return result;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}