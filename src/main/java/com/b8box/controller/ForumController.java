package com.b8box.controller;

import com.b8box.dto.ForumReplyDTO;
import com.b8box.dto.ForumThreadDTO;
import com.b8box.model.Community;
import com.b8box.model.CommunityMember;
import com.b8box.model.ForumReply;
import com.b8box.model.ForumThread;
import com.b8box.model.User;
import com.b8box.repository.CommunityMemberRepository;
import com.b8box.repository.CommunityRepository;
import com.b8box.repository.ForumReplyRepository;
import com.b8box.repository.ForumThreadRepository;
import com.b8box.repository.UserRepository;
import com.b8box.service.ForumService;
import com.b8box.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/communities")
public class ForumController {

    private static final int THREADS_LIMIT = 50;
    private static final int TITLE_MIN = 3;
    private static final int TITLE_MAX = 120;
    private static final int CONTENT_MAX = 5000;
    private static final int REPLY_MAX = 2000;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMemberRepository memberRepository;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private ForumReplyRepository replyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ForumService forumService;

    @Autowired
    private NotificationService notificationService;

    // Corpo do POST /{id}/threads
    public record ThreadRequest(String title, String content) {}

    // Corpo do POST /threads/{id}/replies; parentId vazio = responde à discussão
    public record ReplyRequest(String text, Long parentId) {}

    // Corpo do PUT /threads/{id}/pin
    public record PinRequest(boolean pinned) {}

    // ============================================================
    // LISTAR AS DISCUSSÕES DE UMA COMUNIDADE (público)
    // ============================================================
    @GetMapping("/{communityId}/threads")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listThreads(@PathVariable Long communityId) {
        if (!communityRepository.existsById(communityId)) {
            return ResponseEntity.notFound().build();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : replyRepository.countByCommunity(communityId)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        List<ForumThreadDTO> result = threadRepository.findLatest(communityId, PageRequest.of(0, THREADS_LIMIT))
                .stream()
                .map(t -> ForumThreadDTO.summary(t, counts.getOrDefault(t.getId(), 0L)))
                .toList();
        return ResponseEntity.ok(result);
    }

    // ============================================================
    // ABRIR UMA DISCUSSÃO (público)
    // ============================================================
    @GetMapping("/threads/{threadId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getThread(@PathVariable Long threadId) {
        ForumThread thread = threadRepository.findWithDetails(threadId).orElse(null);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDetail(thread, currentUserIdOrNull()));
    }

    // ============================================================
    // ABRIR NOVA DISCUSSÃO (só membros)
    // ============================================================
    @PostMapping("/{communityId}/threads")
    @Transactional
    public ResponseEntity<?> createThread(@PathVariable Long communityId, @RequestBody ThreadRequest body) {
        Community community = communityRepository.findById(communityId).orElse(null);
        if (community == null) {
            return ResponseEntity.notFound().build();
        }
        User me = getAuthenticatedUser();
        if (roleOf(communityId, me.getId()) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Entre na comunidade para participar.");
        }

        String title = body == null || body.title() == null ? "" : body.title().trim();
        String content = body == null || body.content() == null ? "" : body.content().trim();
        if (title.length() < TITLE_MIN || title.length() > TITLE_MAX) {
            return ResponseEntity.badRequest()
                    .body("❌ O título deve ter entre " + TITLE_MIN + " e " + TITLE_MAX + " caracteres.");
        }
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Escreva o texto da discussão.");
        }
        if (content.length() > CONTENT_MAX) {
            return ResponseEntity.badRequest().body("❌ O texto pode ter até " + CONTENT_MAX + " caracteres.");
        }

        ForumThread thread = threadRepository.save(new ForumThread(community, me, title, content));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDetail(thread, me.getId()));
    }

    // ============================================================
    // RESPONDER (só membros): à discussão ou a outra resposta
    // ============================================================
    @PostMapping("/threads/{threadId}/replies")
    @Transactional
    public ResponseEntity<?> createReply(@PathVariable Long threadId, @RequestBody ReplyRequest body) {
        ForumThread thread = threadRepository.findWithDetails(threadId).orElse(null);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }
        User me = getAuthenticatedUser();
        if (roleOf(thread.getCommunity().getId(), me.getId()) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Entre na comunidade para participar.");
        }

        String text = body == null || body.text() == null ? "" : body.text().trim();
        if (text.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Escreva a resposta.");
        }
        if (text.length() > REPLY_MAX) {
            return ResponseEntity.badRequest().body("❌ A resposta pode ter até " + REPLY_MAX + " caracteres.");
        }

        ForumReply parent = null;
        if (body.parentId() != null) {
            parent = replyRepository.findById(body.parentId()).orElse(null);
            // A resposta citada precisa ser desta discussão e ainda existir
            if (parent == null || !parent.getThread().getId().equals(threadId) || parent.isRemoved()) {
                return ResponseEntity.badRequest().body("❌ Essa resposta não está mais disponível.");
            }
        }

        ForumReply reply = replyRepository.save(new ForumReply(thread, parent, me, text));
        thread.setLastActivityAt(LocalDateTime.now());
        notificationService.onForumReply(me, reply);
        return ResponseEntity.status(HttpStatus.CREATED).body(ForumReplyDTO.of(reply, true));
    }

    // ============================================================
    // FIXAR OU DESAFIXAR (só moderador)
    // ============================================================
    @PutMapping("/threads/{threadId}/pin")
    @Transactional
    public ResponseEntity<?> pinThread(@PathVariable Long threadId, @RequestBody PinRequest body) {
        ForumThread thread = threadRepository.findWithDetails(threadId).orElse(null);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }
        User me = getAuthenticatedUser();
        if (roleOf(thread.getCommunity().getId(), me.getId()) != CommunityMember.Role.MODERATOR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Só moderadores podem fixar discussões.");
        }
        thread.setPinned(body != null && body.pinned());
        return ResponseEntity.ok(toDetail(thread, me.getId()));
    }

    // ============================================================
    // APAGAR DISCUSSÃO (autor ou moderador)
    // ============================================================
    @DeleteMapping("/threads/{threadId}")
    @Transactional
    public ResponseEntity<?> deleteThread(@PathVariable Long threadId) {
        ForumThread thread = threadRepository.findWithDetails(threadId).orElse(null);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }
        User me = getAuthenticatedUser();
        CommunityMember.Role role = roleOf(thread.getCommunity().getId(), me.getId());
        if (!canDelete(thread.getAuthor(), me.getId(), role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Você não pode apagar esta discussão.");
        }
        forumService.deleteThread(thread);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // APAGAR RESPOSTA (autor ou moderador)
    // ============================================================
    @DeleteMapping("/replies/{replyId}")
    @Transactional
    public ResponseEntity<?> deleteReply(@PathVariable Long replyId) {
        ForumReply reply = replyRepository.findById(replyId).orElse(null);
        if (reply == null || reply.isRemoved()) {
            return ResponseEntity.notFound().build();
        }
        User me = getAuthenticatedUser();
        CommunityMember.Role role = roleOf(reply.getThread().getCommunity().getId(), me.getId());
        if (!canDelete(reply.getAuthor(), me.getId(), role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Você não pode apagar esta resposta.");
        }
        forumService.removeReply(reply);
        return ResponseEntity.noContent().build();
    }

    private ForumThreadDTO toDetail(ForumThread thread, Long userId) {
        CommunityMember.Role role = userId == null ? null : roleOf(thread.getCommunity().getId(), userId);
        List<ForumReplyDTO> replies = replyRepository.findByThread(thread.getId()).stream()
                .map(r -> ForumReplyDTO.of(r, canDelete(r.getAuthor(), userId, role)))
                .toList();
        long replyCount = replies.stream().filter(r -> !r.isRemoved()).count();
        return ForumThreadDTO.detail(thread, replies, replyCount, canDelete(thread.getAuthor(), userId, role), role);
    }

    private boolean canDelete(User author, Long userId, CommunityMember.Role role) {
        if (userId == null) return false;
        return role == CommunityMember.Role.MODERATOR || (author != null && author.getId().equals(userId));
    }

    // Papel do usuário na comunidade; null = não é membro
    private CommunityMember.Role roleOf(Long communityId, Long userId) {
        return memberRepository.findByCommunityIdAndUserId(communityId, userId)
                .map(CommunityMember::getRole)
                .orElse(null);
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
