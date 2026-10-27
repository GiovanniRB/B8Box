package com.b8box.config;

import com.b8box.filter.JwtRequestFilter;
import com.b8box.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, JwtRequestFilter jwtRequestFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {}) // Habilita o CORS configurado no WebConfig
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Sem token (ou token inválido) a API responde 401. Antes respondia
            // 403, que também é usado pra "você é logado mas não tem permissão".
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // 🔓 Libera completamente os endpoints de autenticação
                .requestMatchers("/api/auth/**").permitAll()

                // 🔓 Libera a raiz e os arquivos estáticos para o navegador carregar
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/dashboard.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()

                // 🚫 O catálogo (álbuns e músicas) só é criado pelo import do
                // Spotify. Criar/editar/apagar direto pela API não é usado pelo
                // site e deixava qualquer usuário logado apagar álbuns de todo
                // mundo (junto com faixas e avaliações). Leitura (GET) continua.
                .requestMatchers(HttpMethod.POST, "/api/albums/**", "/api/musics/**").denyAll()
                .requestMatchers(HttpMethod.PUT, "/api/albums/**", "/api/musics/**").denyAll()
                .requestMatchers(HttpMethod.DELETE, "/api/albums/**", "/api/musics/**").denyAll()

                // 🔒 Qualquer outra requisição de API exige autenticação
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll() // Permite qualquer outra coisa (importante para o Spring Boot servir o front)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }
}