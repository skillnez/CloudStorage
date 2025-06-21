package com.skillnez.cloudstorage.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF: обычно отключают для REST, или настраивают отдельный фильтр под SPA
                .csrf(AbstractHttpConfigurer::disable)
                // 2. Настройка авторизации по путям
                .authorizeHttpRequests(auth -> auth
                        // Открыты любые ресурсы React (статика)
                        .requestMatchers("/", "/index.html", "/static/**", "/favicon.ico", "/manifest.json").permitAll()
                        // Открыты эндпоинты регистрации и логина
                        .requestMatchers("/api/auth/sign-in", "/api/auth/sign-up").permitAll()
                        // Остальные /api/** требуют авторизации
                        .requestMatchers("/api/**").authenticated()
                        // Остальное тоже разрешить (чтобы React router работал, если у тебя есть fallback controller)
                        .anyRequest().permitAll()
                )
                // 3. Отключаем стандартную форму логина и httpBasic (т.к. login через контроллер)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 4. Настраиваем logout (если нужен отдельный endpoint)
                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT))
                )
                // 5. Разрешить CORS, если фронт отдельно
                .cors(Customizer.withDefaults())
                // 6. Политика сессий (по умолчанию, или можешь явно указать)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"user not authenticated\"}");
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );
        return http.build();
    }

}
