package TravelBuddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/api/admin/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/admin/test-send-newsletter")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
            )
            .headers(headers -> headers
                .frameOptions(Customizer.withDefaults())
                .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'self'"))
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        try {
            return new BCryptPasswordEncoder();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create password encoder: " + e.getMessage(), e);
        }
    }
}
