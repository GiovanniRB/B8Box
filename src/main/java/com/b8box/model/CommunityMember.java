package com.b8box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// Participação de um usuário em uma comunidade. Quem cria vira moderador.
@Entity
@Table(name = "community_members",
       uniqueConstraints = @UniqueConstraint(name = "uk_community_member", columnNames = {"community_id", "user_id"}))
public class CommunityMember {

    public enum Role { MEMBER, MODERATOR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @JsonIgnore
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.MEMBER;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    public CommunityMember() {}

    public CommunityMember(Community community, User user, Role role) {
        this.community = community;
        this.user = user;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Community getCommunity() { return community; }
    public void setCommunity(Community community) { this.community = community; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
