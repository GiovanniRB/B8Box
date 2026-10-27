package com.b8box.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas de login erradas (força bruta): 5 falhas em 10 minutos
 * bloqueiam novas tentativas para aquele login até a janela passar.
 * Fica em memória — suficiente para uma instância só.
 * Observação: como a chave é o login digitado, alguém pode "travar" a conta
 * de outra pessoa por 10 minutos errando de propósito; é o preço de não
 * depender de IP (que atrás de proxy não é confiável).
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS = 10 * 60 * 1000L;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final ConcurrentHashMap<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Deque<Long> attempts = failures.get(key);
        if (attempts == null) return false;
        synchronized (attempts) {
            prune(attempts);
            return attempts.size() >= MAX_FAILURES;
        }
    }

    public void recordFailure(String key) {
        if (failures.size() > MAX_TRACKED_KEYS) cleanup();
        Deque<Long> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(System.currentTimeMillis());
        }
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Long> attempts) {
        long limit = System.currentTimeMillis() - WINDOW_MS;
        while (!attempts.isEmpty() && attempts.peekFirst() < limit) {
            attempts.pollFirst();
        }
    }

    // Evita crescer sem limite se alguém bombardear logins inexistentes
    private void cleanup() {
        failures.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                prune(e.getValue());
                return e.getValue().isEmpty();
            }
        });
    }
}