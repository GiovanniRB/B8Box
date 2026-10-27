package com.b8box.service;

import com.b8box.model.User;
import com.b8box.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    // ============================================================
    // LOGIN: identifica a pessoa por e-mail (se o que foi digitado tem "@")
    // ou por username (se não tem). Os dois nunca se misturam: usernames não
    // podem ter "@" (ver InputValidator), então um username jamais "vira" o
    // e-mail de outra pessoa.
    // ============================================================
    public Optional<User> findByLogin(String login) {
        if (login == null || login.isBlank()) return Optional.empty();
        String value = login.trim();
        if (value.contains("@")) {
            return userRepository.findByEmail(value)
                    .or(() -> userRepository.findByEmail(value.toLowerCase()));
        }
        return userRepository.findByUsername(value);
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return toUserDetails(user);
    }

    // ============================================================
    // USADO PELO FILTRO JWT: o subject do token é SEMPRE o e-mail, então a
    // busca é só por e-mail (nunca cai num username parecido).
    // ============================================================
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return toUserDetails(user);
    }

    // ⚠️ O principal (subject do JWT) é o EMAIL — imutável
    private UserDetails toUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}