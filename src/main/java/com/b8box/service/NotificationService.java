package com.b8box.service;

import com.b8box.model.Album;
import com.b8box.model.ForumReply;
import com.b8box.model.Notification;
import com.b8box.model.Notification.Type;
import com.b8box.model.Rating;
import com.b8box.model.User;
import com.b8box.repository.NotificationRepository;
import com.b8box.repository.RatingCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cria e lista os avisos (sino da navbar).
 * - Ninguém recebe aviso da própria ação.
 * - Desfazer a ação (descurtir, deixar de seguir, apagar o comentário)
 *   apaga o aviso, para clicar e desclicar não encher a lista.
 */
@Service
public class NotificationService {

    private static final int LIST_LIMIT = 30;
    private static final int PREVIEW_LENGTH = 140;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RatingCommentRepository commentRepository;

    @Transactional
    public void onFollow(User actor, User followed) {
        if (actor.getId().equals(followed.getId())) return;
        notificationRepository.save(new Notification(followed, actor, Type.FOLLOW, null));
    }

    @Transactional
    public void onUnfollow(Long actorId, Long followedId) {
        notificationRepository.deleteByActorIdAndRecipientIdAndType(actorId, followedId, Type.FOLLOW);
    }

    @Transactional
    public void onLike(User actor, Rating rating) {
        if (actor.getId().equals(rating.getUser().getId())) return;
        notificationRepository.save(new Notification(rating.getUser(), actor, Type.LIKE, rating));
    }

    @Transactional
    public void onUnlike(Long actorId, Long ratingId) {
        notificationRepository.deleteByActorIdAndRatingIdAndType(actorId, ratingId, Type.LIKE);
    }

    /**
     * Comentário novo. Se quem comentou NÃO é o dono da avaliação, avisa o dono.
     * Se é o dono respondendo, avisa quem já tinha comentado ali.
     */
    @Transactional
    public void onComment(User actor, Rating rating, Long commentId, String text) {
        User owner = rating.getUser();
        List<User> recipients = actor.getId().equals(owner.getId())
                ? commentRepository.findOtherCommenters(rating.getId(), owner.getId())
                : List.of(owner);
        Type type = actor.getId().equals(owner.getId()) ? Type.REPLY : Type.COMMENT;
        String preview = text.length() > PREVIEW_LENGTH ? text.substring(0, PREVIEW_LENGTH - 1) + "…" : text;
        for (User recipient : recipients) {
            Notification n = new Notification(recipient, actor, type, rating);
            n.setCommentId(commentId);
            n.setPreview(preview);
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void onCommentDeleted(Long commentId) {
        notificationRepository.deleteByCommentId(commentId);
    }

    /** Avisa o autor da mensagem respondida e quem abriu a discussão, uma vez cada. */
    @Transactional
    public void onForumReply(User actor, ForumReply reply) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        User threadAuthor = reply.getThread().getAuthor();
        if (reply.getParent() != null && reply.getParent().getAuthor() != null) {
            recipients.put(reply.getParent().getAuthor().getId(), reply.getParent().getAuthor());
        }
        if (threadAuthor != null) {
            recipients.put(threadAuthor.getId(), threadAuthor);
        }
        recipients.remove(actor.getId());

        String text = reply.getText();
        String preview = text.length() > PREVIEW_LENGTH ? text.substring(0, PREVIEW_LENGTH - 1) + "…" : text;
        for (User recipient : recipients.values()) {
            Notification n = new Notification(recipient, actor, Type.REPLY, null);
            n.setThreadId(reply.getThread().getId());
            n.setReplyId(reply.getId());
            n.setPreview(preview);
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void onForumReplyDeleted(Long replyId) {
        notificationRepository.deleteByReplyId(replyId);
    }

    @Transactional
    public void onForumThreadDeleted(Long threadId) {
        notificationRepository.deleteByThreadId(threadId);
    }

    @Transactional
    public void onCommunityDeleted(Long communityId) {
        notificationRepository.deleteByCommunityId(communityId);
    }

    @Transactional
    public void deleteAllFor(Long userId) {
        notificationRepository.deleteByRecipientIdOrActorId(userId, userId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(User me) {
        return notificationRepository.findLatest(me.getId(), PageRequest.of(0, LIST_LIMIT)).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    public long unreadCount(User me) {
        return notificationRepository.countByRecipientIdAndReadFalse(me.getId());
    }

    @Transactional
    public void markAllRead(User me) {
        notificationRepository.markAllRead(me.getId());
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("type", n.getType().name());
        m.put("read", n.isRead());
        m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        m.put("actorId", n.getActor().getId());
        m.put("actorUsername", n.getActor().getUsername());
        m.put("actorAvatarUrl", AvatarService.avatarUrl(n.getActor()));
        m.put("preview", n.getPreview());
        m.put("threadId", n.getThreadId());
        m.put("replyId", n.getReplyId());
        Rating r = n.getRating();
        Album album = r != null ? r.getAlbum() : null;
        m.put("ratingId", r != null ? r.getId() : null);
        m.put("albumId", album != null ? album.getId() : null);
        m.put("albumTitle", album != null ? album.getTitle() : null);
        m.put("albumCoverUrl", album != null ? album.getCoverUrl() : null);
        return m;
    }
}
