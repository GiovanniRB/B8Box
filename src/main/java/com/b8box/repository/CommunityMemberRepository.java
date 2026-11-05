package com.b8box.repository;

import com.b8box.model.CommunityMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, Long> {

    Optional<CommunityMember> findByCommunityIdAndUserId(Long communityId, Long userId);

    List<CommunityMember> findByUserId(Long userId);

    long countByCommunityId(Long communityId);

    long countByCommunityIdAndRole(Long communityId, CommunityMember.Role role);

    Optional<CommunityMember> findFirstByCommunityIdOrderByJoinedAtAsc(Long communityId);

    void deleteByCommunityId(Long communityId);

    void deleteByCommunityIdAndUserId(Long communityId, Long userId);

    void deleteByUserId(Long userId);

    // Pares [id da comunidade, nº de membros], pra listagem não fazer uma consulta por item
    @Query("SELECT m.community.id, COUNT(m) FROM CommunityMember m GROUP BY m.community.id")
    List<Object[]> countAllByCommunity();
}
