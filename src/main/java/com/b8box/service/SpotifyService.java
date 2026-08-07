/*package com.b8box.service;

import com.b8box.config.SpotifyConfig;
import com.b8box.model.spotify.SpotifyAlbumResponse;
import com.b8box.model.spotify.SpotifyTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;

@Service
public class SpotifyService {

    @Autowired
    private SpotifyConfig spotifyConfig;

    @Autowired
    private WebClient spotifyWebClient;

    private String accessToken;
    private long tokenExpiryTime;

    // ============================================================
    // AUTENTICAÇÃO COM SPOTIFY
    // ============================================================

    /**
     * Obtém o Access Token usando Client Credentials Flow
     * (para operações que não precisam de usuário logado)
     */
//    public String getAccessToken() {
//        // Se o token ainda não expirou, usa o que está em cache
//        if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
//            return accessToken;
//        }

//        String credentials = spotifyConfig.getClientId() + ":" + spotifyConfig.getClientSecret();
//        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

//        SpotifyTokenResponse response = WebClient.create("https://accounts.spotify.com")
//                .post()
//                .uri("/api/token")
//                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
//                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                .bodyValue("grant_type=client_credentials")
//                .retrieve()
//                .bodyToMono(SpotifyTokenResponse.class)
//                .block();

//        if (response != null) {
//            this.accessToken = response.getAccessToken();
//            this.tokenExpiryTime = System.currentTimeMillis() + (response.getExpiresIn() * 1000L);
//            System.out.println("✅ Token Spotify obtido com sucesso!");
//            return accessToken;
//        }

//        throw new RuntimeException("❌ Erro ao obter token do Spotify");
//    }

    // ============================================================
    // BUSCA DE ÁLBUNS
    // ============================================================

    /**
     * Busca álbuns no Spotify por nome
     */
//    public SpotifyAlbumResponse searchAlbums(String query) {
//        String token = getAccessToken();

//        return spotifyWebClient
//                .get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/search")
//                        .queryParam("q", query)
//                        .queryParam("type", "album")
//                        .queryParam("limit", 10)
//                        .build())
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
//                .retrieve()
//                .bodyToMono(SpotifyAlbumResponse.class)
//                .block();
//    }

    /**
     * Busca álbum por ID no Spotify
     */
//    public SpotifyAlbumResponse.AlbumItem getAlbumById(String spotifyId) {
//        String token = getAccessToken();

//        return spotifyWebClient
//                .get()
//                .uri("/albums/{id}", spotifyId)
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
//                .retrieve()
//                .bodyToMono(SpotifyAlbumResponse.AlbumItem.class)
//                .block();
//    }
//}