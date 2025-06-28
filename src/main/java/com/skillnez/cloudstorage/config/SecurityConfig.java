package com.skillnez.cloudstorage.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost")); // или http://localhost:80
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF: обычно отключают для REST, или настраивают отдельный фильтр под SPA
                .csrf(AbstractHttpConfigurer::disable)
                // 2. Настройка авторизации по путям
                .authorizeHttpRequests(auth -> auth
                        // Открыты любые ресурсы React (статика)
                        .requestMatchers("/", "/index.html", "/static/**", "/favicon.ico", "/manifest.json").permitAll()
                        // Открыты эндпоинты регистрации и логина
                        .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll().requestMatchers(HttpMethod.POST, "/api/auth/sign-up").permitAll()
                        // Остальные /api/** требуют авторизации
                        .requestMatchers("/api/**").authenticated()
                        // Остальное тоже разрешить (чтобы React router работал, если у тебя есть fallback controller)
                        .requestMatchers("/", "/index.html", "/config.js", "/assets/**", "/login", "/registration", "/files/**").permitAll())
                // 3. Отключаем стандартную форму логина и httpBasic (т.к. login через контроллер)
                .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                // 4. Настраиваем logout (если нужен отдельный endpoint)
                .logout(logout -> logout.logoutUrl("/api/auth/sign-out").logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                // 6. Политика сессий (по умолчанию, или можешь явно указать)
                .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"user not authenticated\"}");
                })).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)).cors(cors -> cors.configurationSource(corsConfigurationSource()));
        return http.build();
    }

}
