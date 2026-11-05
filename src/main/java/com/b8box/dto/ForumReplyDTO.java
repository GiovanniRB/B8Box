package com.b8box.dto;

import com.b8box.model.ForumReply;
import com.b8box.model.User;
import com.b8box.service.AvatarService;

/** Resposta de uma discussão. Removida: sem autor e sem texto, só o lugar na conversa. */
public class ForumReplyDTO {

    private Long id;
    private Long parentId;
    private Long authorId;
    private String author;
    private String avatarUrl;
    private String text;
    private boolean removed;
    private String createdAt;
    private boolean canDelete;

    public static ForumReplyDTO of(ForumReply reply, boolean canDelete) {
        ForumReplyDTO dto = new ForumReplyDTO();
        dto.id = reply.getId();
        dto.parentId = reply.getParent() != null ? reply.getParent().getId() : null;
        dto.removed = reply.isRemoved();
        dto.createdAt = reply.getCreatedAt().withNano(0).toString();
        if (!reply.isRemoved()) {
            User author = reply.getAuthor();
            dto.authorId = author != null ? author.getId() : null;
            dto.author = author != null ? author.getUsername() : null;
            dto.avatarUrl = author != null ? AvatarService.avatarUrl(author) : null;
            dto.text = reply.getText();
            dto.canDelete = canDelete;
        }
        return dto;
    }

    public Long getId() { return id; }
    public Long getParentId() { return parentId; }
    public Long getAuthorId() { return authorId; }
    public String getAuthor() { return author; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getText() { return text; }
    public boolean isRemoved() { return removed; }
    public String getCreatedAt() { return createdAt; }
    public boolean isCanDelete() { return canDelete; }
}
