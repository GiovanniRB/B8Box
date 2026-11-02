package com.b8box.controller;

import com.b8box.model.User;
import com.b8box.repository.UserRepository;
import com.b8box.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Avisos do usuário logado (sino da navbar)
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    // Últimos avisos, dos mais novos para os mais antigos
    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(notificationService.list(getAuthenticatedUser()));
    }

    // Só o número de não lidos (a navbar pergunta isso de tempos em tempos)
    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(getAuthenticatedUser())));
    }

    // Abrir o sino marca tudo como lido
    @PostMapping("/read")
    public ResponseEntity<?> markAllRead() {
        notificationService.markAllRead(getAuthenticatedUser());
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // AUXILIAR — busca por EMAIL (identidade do JWT)
    // ============================================================
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}
