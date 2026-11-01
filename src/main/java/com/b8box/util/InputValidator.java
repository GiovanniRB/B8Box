package com.b8box.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validações de entrada compartilhadas (cadastro, perfil, avaliações).
 * Cada método devolve a mensagem de erro, ou null se o valor é válido.
 */
public final class InputValidator {

    // Letras (com acento), números, ponto, hífen e underline. SEM "@" e sem
    // espaços: isso impede que alguém cadastre um username igual ao e-mail de
    // outra pessoa (o login diferencia e-mail de username pela presença do "@").
    private static final Pattern USERNAME = Pattern.compile("^[\\p{L}\\p{N}._-]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static final int MIN_PASSWORD = 6;
    // O BCrypt só considera os primeiros 72 bytes da senha
    public static final int MAX_PASSWORD_BYTES = 72;
    public static final int MAX_REVIEW_LENGTH = 5000;
    public static final int MAX_BIO_LENGTH = 300;
    public static final int MAX_GENRES = 5;
    public static final int MAX_GENRE_LENGTH = 30;

    // Gênero: letras (com acento), números, espaço e & / ' . + -
    // (cobre "Hip-Hop", "R&B", "Rock 'n' Roll", "Lo-fi", "MPB").
    private static final Pattern GENRE = Pattern.compile("^[\\p{L}\\p{N} &/'.+-]{1," + MAX_GENRE_LENGTH + "}$");

    private InputValidator() {}

    public static String validateUsername(String username) {
        if (username == null || !USERNAME.matcher(username).matches()) {
            return "❌ O username deve ter de 3 a 30 caracteres: letras, números, ponto, hífen ou underline (sem espaços e sem @).";
        }
        return null;
    }

    public static String validateEmail(String email) {
        if (email == null || email.length() > 254 || !EMAIL.matcher(email).matches()) {
            return "❌ Informe um e-mail válido.";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD) {
            return "❌ A senha deve ter pelo menos " + MIN_PASSWORD + " caracteres!";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return "❌ A senha é longa demais (máximo de " + MAX_PASSWORD_BYTES + " bytes).";
        }
        return null;
    }

    public static String validateRating(Double score, String review) {
        // 0 a 5 estrelas, em passos de meia estrela (0, 0.5, 1 ... 5)
        if (score == null || score.isNaN() || score.isInfinite() || score < 0 || score > 5
                || score * 2 != Math.rint(score * 2)) {
            return "❌ A nota deve ir de 0 a 5 estrelas, em passos de meia estrela.";
        }
        if (review != null && review.length() > MAX_REVIEW_LENGTH) {
            return "❌ A resenha pode ter no máximo " + MAX_REVIEW_LENGTH + " caracteres.";
        }
        return null;
    }

    public static String validateBio(String bio) {
        if (bio != null && bio.length() > MAX_BIO_LENGTH) {
            return "❌ A bio pode ter no máximo " + MAX_BIO_LENGTH + " caracteres.";
        }
        return null;
    }

    /**
     * Limpa a lista de gêneros vinda do formulário: tira espaços das pontas,
     * junta espaços repetidos, descarta itens vazios e repetidos (sem
     * diferenciar maiúsculas de minúsculas), mantendo a ordem digitada.
     */
    public static List<String> normalizeGenres(List<String> genres) {
        Map<String, String> unique = new LinkedHashMap<>();
        if (genres != null) {
            for (String g : genres) {
                if (g == null) continue;
                String clean = g.trim().replaceAll("\\s+", " ");
                if (clean.isEmpty()) continue;
                unique.putIfAbsent(clean.toLowerCase(Locale.ROOT), clean);
            }
        }
        return new ArrayList<>(unique.values());
    }

    /** Valida a lista JÁ normalizada (ver normalizeGenres). */
    public static String validateGenres(List<String> genres) {
        if (genres == null) return null;
        if (genres.size() > MAX_GENRES) {
            return "❌ Escolha no máximo " + MAX_GENRES + " gêneros favoritos.";
        }
        for (String g : genres) {
            if (!GENRE.matcher(g).matches()) {
                return "❌ Gênero inválido: \"" + g + "\". Use até " + MAX_GENRE_LENGTH
                        + " caracteres (letras, números, espaço e & / ' . + -).";
            }
        }
        return null;
    }
}
