package com.b8box.repository;

import com.b8box.model.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {
    List<PlaylistItem> findByPlaylistId(Long playlistId);
    List<PlaylistItem> findByMusicId(Long musicId);
    void deleteByPlaylistId(Long playlistId);

    // Álbuns de várias playlists de uma vez, na ordem em que aparecem
    // (posição da primeira faixa de cada álbum). Usado para montar a capa
    // em mosaico dos cards sem uma consulta por playlist.
    interface AlbumPreviewRow {
        Long getPlaylistId();
        Long getAlbumId();
        String getCoverUrl();
    }

    @Query(value = "select pi.playlist_id as \"playlistId\", a.id as \"albumId\", a.cover_url as \"coverUrl\" "
            + "from playlist_items pi "
            + "join musics m on m.id = pi.music_id "
            + "join albums a on a.id = m.album_id "
            + "where pi.playlist_id in (:ids) "
            + "group by pi.playlist_id, a.id, a.cover_url "
            + "order by pi.playlist_id, min(pi.position), a.id", nativeQuery = true)
    List<AlbumPreviewRow> albumPreviews(@Param("ids") Collection<Long> ids);
}