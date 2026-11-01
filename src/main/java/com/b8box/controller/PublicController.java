package com.b8box.controller;

import com.b8box.dto.FeedItemDTO;
import com.b8box.dto.PopularAlbumDTO;
import com.b8box.model.User;
import com.b8box.repository.UserRepository;
import com.b8box.service.AvatarService;
import com.b8box.service.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

/**
 * Rotas da home que funcionam SEM login (liberadas no SecurityConfig só
 * para GET). Se a pessoa estiver logada, o "curtido por mim" vem preenchido;
 * sem login, vem sempre false.
 * Nada aqui expõe e-mail ou dados privados: só username, álbum, nota e resenha.
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private FeedService feedService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvatarService avatarService;

    // Últimas avaliações de qualquer pessoa
    @GetMapping("/reviews/recent")
    public List<FeedItemDTO> getRecentReviews() {
        return feedService.getRecentReviews(currentUserIdOrNull());
    }

    // Avaliações mais curtidas dos últimos 30 dias
    @GetMapping("/reviews/popular")
    public List<FeedItemDTO> getPopularReviews() {
        return feedService.getPopularReviews(currentUserIdOrNull());
    }

    // Álbuns mais avaliados dos últimos 30 dias
    @GetMapping("/albums/popular")
    public List<PopularAlbumDTO> getPopularAlbums() {
        return feedService.getPopularAlbums();
    }

    // Foto de perfil (pública, como no Letterboxd). A URL tem "?v=<data da
    // troca>", então pode ficar em cache por bastante tempo no navegador.
    @GetMapping("/users/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
        return avatarService.find(id)
                .map(a -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(a.getContentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(a.getImage()))
                .orElse(ResponseEntity.notFound().build());
    }

    // Visitante (sem token ou token inválido) -> null
    private Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
