package com.b8box.controller;

import com.b8box.dto.CommunityDTO;
import com.b8box.model.Community;
import com.b8box.model.CommunityMember;
import com.b8box.model.User;
import com.b8box.repository.CommunityMemberRepository;
import com.b8box.repository.CommunityRepository;
import com.b8box.repository.ForumReplyRepository;
import com.b8box.repository.ForumThreadRepository;
import com.b8box.repository.UserRepository;
import com.b8box.service.CommunityService;
import com.b8box.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 60;
    private static final int DESCRIPTION_MAX = 300;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private ForumReplyRepository replyRepository;

    @Autowired
    private NotificationService notificationService;

    // Corpo do POST /api/communities
    public record CommunityRequest(String name, String description, String category) {}

    // ============================================================
    // LISTAR (público; ?category=GENRE|DECADE|ARTIST filtra)
    // ============================================================
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(@RequestParam(required = false) String category) {
        List<Community> communities;
        if (category == null || category.isBlank()) {
            communities = communityRepository.findAllByOrderByNameAsc();
        } else {
            Community.Category parsed = parseCategory(category);
            if (parsed == null) {
                return ResponseEntity.badRequest().body("❌ Categoria inválida.");
            }
            communities = communityRepository.findByCategoryOrderByNameAsc(parsed);
        }

        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : memberRepository.countAllByCommunity()) {
            counts.put((Long) row[0], (Long) row[1]);
        }

        Map<Long, Long> threadCounts = new HashMap<>();
        for (Object[] row : threadRepository.countAllByCommunity()) {
            threadCounts.put((Long) row[0], (Long) row[1]);
        }

        Long userId = currentUserIdOrNull();
        Map<Long, CommunityMember.Role> roles = userId == null ? Map.of()
                : memberRepository.findByUserId(userId).stream()
                    .collect(Collectors.toMap(m -> m.getCommunity().getId(), CommunityMember::getRole));

        List<CommunityDTO> result = communities.stream()
                .map(c -> CommunityDTO.of(c, counts.getOrDefault(c.getId(), 0L),
                        threadCounts.getOrDefault(c.getId(), 0L), roles.get(c.getId())))
                .toList();
        return ResponseEntity.ok(result);
    }

    // ============================================================
    // DETALHES (público)
    // ============================================================
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> get(@PathVariable Long id) {
        Community community = communityRepository.findById(id).orElse(null);
        if (community == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(community, currentUserIdOrNull()));
    }

    // ============================================================
    // CRIAR (quem cria entra como moderador)
    // ============================================================
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody CommunityRequest body) {
        String name = body == null || body.name() == null ? "" : body.name().trim();
        String description = body == null || body.description() == null ? "" : body.description().trim();

        if (name.length() < NAME_MIN || name.length() > NAME_MAX) {
            return ResponseEntity.badRequest()
                    .body("❌ O nome deve ter entre " + NAME_MIN + " e " + NAME_MAX + " caracteres.");
        }
        if (description.length() > DESCRIPTION_MAX) {
            return ResponseEntity.badRequest()
                    .body("❌ A descrição pode ter até " + DESCRIPTION_MAX + " caracteres.");
        }
        Community.Category category = parseCategory(body.category());
        if (category == null) {
            return ResponseEntity.badRequest().body("❌ Escolha uma categoria: gênero, década ou artista.");
        }
        if (communityRepository.existsByNameIgnoreCase(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("❌ Já existe uma comunidade com esse nome.");
        }

        User me = getAuthenticatedUser();
        Community community = communityRepository.save(new Community(name, description, category, me));
        memberRepository.save(new CommunityMember(community, me, CommunityMember.Role.MODERATOR));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(community, me.getId()));
    }

    // ============================================================
    // ENTRAR
    // ============================================================
    @PostMapping("/{id}/join")
    @Transactional
    public ResponseEntity<?> join(@PathVariable Long id) {
        Community community = communityRepository.findById(id).orElse(null);
        if (community == null) {
            return ResponseEntity.notFound().build();
        }
        User me = getAuthenticatedUser();
        if (memberRepository.findByCommunityIdAndUserId(id, me.getId()).isEmpty()) {
            memberRepository.save(new CommunityMember(community, me, CommunityMember.Role.MEMBER));
        }
        return ResponseEntity.ok(toDto(community, me.getId()));
    }

    // ============================================================
    // SAIR
    // ============================================================
    @DeleteMapping("/{id}/join")
    @Transactional
    public ResponseEntity<?> leave(@PathVariable Long id) {
        Community community = communityRepository.findById(id).orElse(null);
        if (community == null) {
            return ResponseEntity.notFound().build();
        }
        User me = getAuthenticatedUser();
        communityService.leave(id, me.getId());
        return ResponseEntity.ok(toDto(community, me.getId()));
    }

    // ============================================================
    // APAGAR (só moderador)
    // ============================================================
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Community community = communityRepository.findById(id).orElse(null);
        if (community == null) {
            return ResponseEntity.notFound().build();
        }
        User me = getAuthenticatedUser();
        boolean moderator = memberRepository.findByCommunityIdAndUserId(id, me.getId())
                .map(m -> m.getRole() == CommunityMember.Role.MODERATOR)
                .orElse(false);
        if (!moderator) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Só moderadores podem apagar a comunidade.");
        }
        notificationService.onCommunityDeleted(id);
        replyRepository.deleteByCommunityId(id);
        threadRepository.deleteByCommunityId(id);
        memberRepository.deleteByCommunityId(id);
        communityRepository.delete(community);
        return ResponseEntity.noContent().build();
    }

    private CommunityDTO toDto(Community community, Long userId) {
        CommunityMember.Role role = userId == null ? null
                : memberRepository.findByCommunityIdAndUserId(community.getId(), userId)
                    .map(CommunityMember::getRole).orElse(null);
        return CommunityDTO.of(community, memberRepository.countByCommunityId(community.getId()),
                threadRepository.countByCommunityId(community.getId()), role);
    }

    private Community.Category parseCategory(String value) {
        if (value == null) return null;
        try {
            return Community.Category.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Visitante (sem token ou token inválido) -> null
    private Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}
