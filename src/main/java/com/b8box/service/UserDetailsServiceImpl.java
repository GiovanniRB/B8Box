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

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("🔍 Buscando usuário: " + username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("❌ Usuário não encontrado: " + username);
                    return new UsernameNotFoundException("Usuário não encontrado: " + username);
                });
        
        System.out.println("✅ Usuário encontrado: " + user.getUsername());
        System.out.println("🔍 Senha no banco: " + user.getPassword());
        
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}