package com.b8box.service;

import com.b8box.model.CommunityMember;
import com.b8box.repository.CommunityMemberRepository;
import com.b8box.repository.CommunityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Saída de comunidades. Fica aqui porque acontece em dois lugares (sair da
 * comunidade e excluir a conta) e nos dois a moderação não pode ficar órfã:
 * se saiu o último moderador, o membro mais antigo assume.
 */
@Service
public class CommunityService {

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMemberRepository memberRepository;

    @Transactional
    public void leave(Long communityId, Long userId) {
        memberRepository.deleteByCommunityIdAndUserId(communityId, userId);
        memberRepository.flush();
        ensureModerator(communityId);
    }

    /** Conta excluída: sai de todas. As comunidades que a pessoa criou continuam. */
    @Transactional
    public void removeFromAll(Long userId) {
        List<Long> communityIds = memberRepository.findByUserId(userId).stream()
                .map(m -> m.getCommunity().getId())
                .toList();
        memberRepository.deleteByUserId(userId);
        memberRepository.flush();
        communityIds.forEach(this::ensureModerator);
        communityRepository.clearCreator(userId);
    }

    private void ensureModerator(Long communityId) {
        if (memberRepository.countByCommunityIdAndRole(communityId, CommunityMember.Role.MODERATOR) > 0) {
            return;
        }
        memberRepository.findFirstByCommunityIdOrderByJoinedAtAsc(communityId)
                .ifPresent(m -> m.setRole(CommunityMember.Role.MODERATOR));
    }
}
