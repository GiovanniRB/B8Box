package com.b8box.dto;

import com.b8box.model.CommunityMember;
import com.b8box.model.ForumThread;
import com.b8box.model.User;
import com.b8box.service.AvatarService;

import java.util.List;

/**
 * Discussão. Na lista vem com o trecho do texto (excerpt); ao abrir, com o texto
 * inteiro (content), as respostas e o papel de quem está vendo (role).
 */
public class ForumThreadDTO {

    private static final int EXCERPT_LENGTH = 160;

    private Long id;
    private Long communityId;
    private String communityName;
    private String title;
    private String excerpt;
    private String content;
    private Long authorId;
    private String author;
    private String avatarUrl;
    private boolean pinned;
    private long replyCount;
    private String createdAt;
    private String lastActivityAt;
    private boolean canDelete;
    private String role;
    private List<ForumReplyDTO> replies;

    public static ForumThreadDTO summary(ForumThread thread, long replyCount) {
        ForumThreadDTO dto = base(thread, replyCount);
        String text = thread.getContent();
        dto.excerpt = text.length() > EXCERPT_LENGTH ? text.substring(0, EXCERPT_LENGTH) + "…" : text;
        return dto;
    }

    public static ForumThreadDTO detail(ForumThread thread, List<ForumReplyDTO> replies, long replyCount,
                                        boolean canDelete, CommunityMember.Role role) {
        ForumThreadDTO dto = base(thread, replyCount);
        dto.communityName = thread.getCommunity().getName();
        dto.content = thread.getContent();
        dto.canDelete = canDelete;
        dto.role = role != null ? role.name() : null;
        dto.replies = replies;
        return dto;
    }

    private static ForumThreadDTO base(ForumThread thread, long replyCount) {
        ForumThreadDTO dto = new ForumThreadDTO();
        dto.id = thread.getId();
        dto.communityId = thread.getCommunity().getId();
        dto.title = thread.getTitle();
        User author = thread.getAuthor();
        dto.authorId = author != null ? author.getId() : null;
        dto.author = author != null ? author.getUsername() : null;
        dto.avatarUrl = author != null ? AvatarService.avatarUrl(author) : null;
        dto.pinned = thread.isPinned();
        dto.replyCount = replyCount;
        dto.createdAt = thread.getCreatedAt().withNano(0).toString();
        dto.lastActivityAt = thread.getLastActivityAt().withNano(0).toString();
        return dto;
    }

    public Long getId() { return id; }
    public Long getCommunityId() { return communityId; }
    public String getCommunityName() { return communityName; }
    public String getTitle() { return title; }
    public String getExcerpt() { return excerpt; }
    public String getContent() { return content; }
    public Long getAuthorId() { return authorId; }
    public String getAuthor() { return author; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isPinned() { return pinned; }
    public long getReplyCount() { return replyCount; }
    public String getCreatedAt() { return createdAt; }
    public String getLastActivityAt() { return lastActivityAt; }
    public boolean isCanDelete() { return canDelete; }
    public String getRole() { return role; }
    public List<ForumReplyDTO> getReplies() { return replies; }
}
