package com.b8box.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // Chave secreta assinada (igual a senha do cadeado)
    @Value("${jwt.secret}")
    private String secret;

    // Tempo de expiração do token (1 dia em milissegundos)
    @Value("${jwt.expiration}")
    private Long expiration;

    // ============================================================
    // EXTRAIR INFORMAÇÕES DO TOKEN
    // ============================================================

    // Extrai o nome de usuário (subject) do token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extrai a data de expiração do token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extrai uma informação específica do token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extrai todas as informações (claims) do token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ============================================================
    // VALIDAÇÕES DO TOKEN
    // ============================================================

    // Verifica se o token é válido (não expirou e pertence ao usuário)
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Verifica se o token expirou
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ============================================================
    // GERAÇÃO DE TOKEN
    // ============================================================

    // Gera um token para o usuário
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getAuthorities()); // Adiciona roles ao token
        return createToken(claims, userDetails.getUsername());
    }

    // Cria o token propriamente dito
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)                           // Dados extras (role)
                .setSubject(subject)                         // Nome do usuário
                .setIssuedAt(new Date())                     // Data de criação
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // Expiração
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Assinatura
                .compact();
    }

    // ============================================================
    // CHAVE DE ASSINATURA
    // ============================================================

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}