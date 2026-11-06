package com.b8box.repository;

import com.b8box.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Contadores do perfil público. Cada um seria uma consulta separada; com o
    // banco longe (~0,2 s por ida e volta) juntar as cinco numa só economiza
    // quase 1 segundo por tela.
    interface ProfileStats {
        Long getAlbumsCount();
        Long getPublicPlaylistsCount();
        Long getFollowersCount();
        Long getFollowingCount();
        Long getFollowingByMe();   // 0 ou 1
    }

    @Query(value = "select "
            + "(select count(distinct r.album_id) from ratings r where r.user_id = :userId and r.album_id is not null) as \"albumsCount\", "
            + "(select count(*) from playlists p where p.user_id = :userId and p.is_public = true) as \"publicPlaylistsCount\", "
            + "(select count(*) from follows f where f.followed_id = :userId) as \"followersCount\", "
            + "(select count(*) from follows f where f.follower_id = :userId) as \"followingCount\", "
            + "(select count(*) from follows f where f.follower_id = :meId and f.followed_id = :userId) as \"followingByMe\"",
            nativeQuery = true)
    ProfileStats profileStats(@Param("userId") Long userId, @Param("meId") Long meId);

    // Tudo o que a reputação conta, também numa consulta só (ver ReputationService)
    interface ReputationStats {
        Long getReviewsCount();
        Long getScoreOnlyCount();
        Long getAlbumsCount();
        Long getLikesReceivedCount();
        Long getThreadsCount();
        Long getRepliesCount();
        Long getPublicPlaylistsCount();
        Long getFollowersCount();
        Long getCommunitiesCreatedCount();
    }

    @Query(value = "select "
            + "(select count(*) from ratings r where r.user_id = :userId and trim(coalesce(r.review, '')) <> '') as \"reviewsCount\", "
            + "(select count(*) from ratings r where r.user_id = :userId and trim(coalesce(r.review, '')) = '') as \"scoreOnlyCount\", "
            + "(select count(distinct r.album_id) from ratings r where r.user_id = :userId and r.album_id is not null) as \"albumsCount\", "
            + "(select count(*) from rating_likes l join ratings r on r.id = l.rating_id where r.user_id = :userId) as \"likesReceivedCount\", "
            + "(select count(*) from forum_threads t where t.author_id = :userId) as \"threadsCount\", "
            + "(select count(*) from forum_replies fr where fr.author_id = :userId and fr.removed = false) as \"repliesCount\", "
            + "(select count(*) from playlists p where p.user_id = :userId and p.is_public = true) as \"publicPlaylistsCount\", "
            + "(select count(*) from follows f where f.followed_id = :userId) as \"followersCount\", "
            + "(select count(*) from communities c where c.creator_id = :userId) as \"communitiesCreatedCount\"",
            nativeQuery = true)
    ReputationStats reputationStats(@Param("userId") Long userId);
}