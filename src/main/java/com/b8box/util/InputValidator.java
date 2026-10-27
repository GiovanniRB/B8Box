package com.b8box.util;

import java.nio.charset.StandardCharsets;
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
        if (score == null || score.isNaN() || score.isInfinite() || score < 0 || score > 10) {
            return "❌ A nota deve estar entre 0 e 10.";
        }
        if (review != null && review.length() > MAX_REVIEW_LENGTH) {
            return "❌ A resenha pode ter no máximo " + MAX_REVIEW_LENGTH + " caracteres.";
        }
        return null;
    }
}