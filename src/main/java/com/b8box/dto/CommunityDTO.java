package com.b8box.dto;

import com.b8box.model.Community;
import com.b8box.model.CommunityMember;

/** Comunidade como aparece nas telas. role é null para visitante ou não membro. */
public class CommunityDTO {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String creator;
    private long memberCount;
    private long threadCount;
    private String role;

    public static CommunityDTO of(Community community, long memberCount, long threadCount, CommunityMember.Role role) {
        CommunityDTO dto = new CommunityDTO();
        dto.id = community.getId();
        dto.name = community.getName();
        dto.description = community.getDescription();
        dto.category = community.getCategory().name();
        dto.creator = community.getCreator() != null ? community.getCreator().getUsername() : null;
        dto.memberCount = memberCount;
        dto.threadCount = threadCount;
        dto.role = role != null ? role.name() : null;
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getCreator() { return creator; }
    public long getMemberCount() { return memberCount; }
    public long getThreadCount() { return threadCount; }
    public String getRole() { return role; }
}
