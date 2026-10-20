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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Transactional  // deleteBy... derivado exige transação ativa
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
    // LISTA DE SEGUIDORES de um usuário (quem segue o userId)
    // Cada item já diz se EU sigo aquela pessoa (followingByMe) e se a
    // pessoa sou eu (isMe), pra tela montar o botão Seguir/Seguindo.
    // ============================================================
    @GetMapping("/{userId}/followers")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFollowers(@PathVariable Long userId) {
        User me = getAuthenticatedUser();
        List<Map<String, Object>> result = followRepository.findByFollowedId(userId).stream()
                .sorted(newestFirst())
                .map(f -> toUserRow(f.getFollower(), me.getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ============================================================
    // LISTA DE SEGUINDO de um usuário (quem o userId segue)
    // ============================================================
    @GetMapping("/{userId}/following")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFollowing(@PathVariable Long userId) {
        User me = getAuthenticatedUser();
        List<Map<String, Object>> result = followRepository.findByFollowerId(userId).stream()
                .sorted(newestFirst())
                .map(f -> toUserRow(f.getFollowed(), me.getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ============================================================
    // AUXILIARES
    // ============================================================
    private Comparator<Follow> newestFirst() {
        return Comparator.comparing(Follow::getCreatedAt,
                Comparator.nullsLast(Comparator.<java.time.LocalDateTime>reverseOrder()));
    }

    private Map<String, Object> toUserRow(User u, Long meId) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", u.getId());
        row.put("username", u.getUsername());
        row.put("isMe", u.getId().equals(meId));
        row.put("followingByMe", followRepository.existsByFollowerIdAndFollowedId(meId, u.getId()));
        return row;
    }

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