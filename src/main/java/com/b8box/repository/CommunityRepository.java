package com.b8box.repository;

import com.b8box.model.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Community> findAllByOrderByNameAsc();

    List<Community> findByCategoryOrderByNameAsc(Community.Category category);

    // Conta excluída: a comunidade fica, só perde a referência a quem criou
    @Modifying
    @Query("UPDATE Community c SET c.creator = null WHERE c.creator.id = :userId")
    void clearCreator(@Param("userId") Long userId);
}
