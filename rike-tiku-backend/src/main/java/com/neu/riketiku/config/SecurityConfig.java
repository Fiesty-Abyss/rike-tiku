package com.neu.riketiku.config;

import java.util.Arrays;
import java.util.List;

import com.neu.riketiku.renzheng.ChuShiMiMaMenJinGuoLvQi;
import com.neu.riketiku.renzheng.JwtRenZhengGuoLvQi;
import com.neu.riketiku.renzheng.QuanXianBuZuChuLiQi;
import com.neu.riketiku.renzheng.WeiRenZhengChuLiQi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtRenZhengGuoLvQi jwtFilter,
            ChuShiMiMaMenJinGuoLvQi initialPasswordGateFilter,
            WeiRenZhengChuLiQi authenticationEntryPoint,
            QuanXianBuZuChuLiQi accessDeniedHandler) throws Exception {
        http
                .cors(cors -> { })
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/health", "/api/v1/auth/login", "/api/v1/auth/captcha-challenge", "/api/v1/auth/password-recovery-requests", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/test/student").hasRole("STUDENT")
                        .requestMatchers("/api/v1/test/teacher").hasRole("TEACHER")
                        .requestMatchers("/api/v1/test/admin").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/classes/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/students/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/student-import/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/question-import/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/teachers/**", "/api/v1/admin/teaching-assignments/**", "/api/v1/admin/subjects").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/operation-logs/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/ai-models/**", "/api/v1/admin/ai-generation/**", "/api/v1/admin/password-recovery-requests/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/questions/**", "/api/v1/admin/knowledge-points", "/api/v1/admin/question-attachments/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/student/practice-options", "/api/v1/student/practice-sessions/**", "/api/v1/student/wrong-questions/**", "/api/v1/student/high-frequency-points", "/api/v1/student/learning-summary").hasRole("STUDENT")
                        .requestMatchers("/api/v1/student/ai/**").hasRole("STUDENT")
                        .requestMatchers("/api/v1/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/api/v1/messages/**").hasAnyRole("STUDENT", "TEACHER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(initialPasswordGateFilter, JwtRenZhengGuoLvQi.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Accept", "Authorization"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
