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
}