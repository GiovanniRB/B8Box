package com.b8box.service;

import com.b8box.model.ForumReply;
import com.b8box.model.ForumThread;
import com.b8box.repository.ForumReplyRepository;
import com.b8box.repository.ForumThreadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remoção de conteúdo dos fóruns. Fica aqui porque acontece em dois lugares:
 * quando a pessoa apaga pela tela e quando ela exclui a conta.
 */
@Service
public class ForumService {

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private ForumReplyRepository replyRepository;

    @Autowired
    private NotificationService notificationService;

    // Com respostas abaixo, só marca como removida. Sem nenhuma, apaga de vez,
    // e o mesmo vale para a resposta de cima, se ela já estava removida.
    @Transactional
    public void removeReply(ForumReply reply) {
        notificationService.onForumReplyDeleted(reply.getId());
        if (replyRepository.existsByParentId(reply.getId())) {
            reply.setRemoved(true);
            reply.setText("");
            reply.setAuthor(null);
            return;
        }
        ForumReply parent = reply.getParent();
        replyRepository.delete(reply);
        replyRepository.flush();
        if (parent != null && parent.isRemoved()) {
            removeReply(parent);
        }
    }

    @Transactional
    public void deleteThread(ForumThread thread) {
        notificationService.onForumThreadDeleted(thread.getId());
        replyRepository.deleteByThreadId(thread.getId());
        threadRepository.delete(thread);
    }

    /**
     * Conta excluída: as respostas seguem a regra de apagar. A discussão sem
     * nenhuma resposta sai; com respostas, fica sem autor, para não levar junto
     * o que as outras pessoas escreveram.
     */
    @Transactional
    public void removeContentOf(Long userId) {
        // Das mais novas para as mais antigas: as respostas de baixo saem primeiro
        replyRepository.findByAuthorIdOrderByIdDesc(userId).forEach(this::removeReply);

        for (ForumThread thread : threadRepository.findByAuthorId(userId)) {
            if (replyRepository.existsByThreadId(thread.getId())) {
                thread.setAuthor(null);
            } else {
                deleteThread(thread);
            }
        }
    }
}
