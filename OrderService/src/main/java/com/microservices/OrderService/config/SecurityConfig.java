package com.microservices.OrderService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration
 * Practice: Secure POST /api/v1/orders endpoint with role-based access
 * 
 * Security Rules:
 * - POST /api/v1/orders - Requires ADMIN role
 * - GET /api/v1/orders/** - Accessible to USER and ADMIN roles
 * - DELETE /api/v1/orders/** - Requires ADMIN role
 * - Other endpoints - Authenticated users only
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configure HTTP security with role-based access control
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST API (stateless)
            .csrf(csrf -> csrf.disable())
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // POST /api/v1/orders - Only ADMIN can create orders
                .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasRole("ADMIN")
                
                // DELETE /api/v1/orders/** - Only ADMIN can delete orders
                .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**").hasRole("ADMIN")
                
                // GET /api/v1/orders/** - Both USER and ADMIN can view orders
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").hasAnyRole("USER", "ADMIN")
                
                // Test rollback endpoint - ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/test-rollback").hasRole("ADMIN")
                
                // H2 Console - Development only (remove in production)
                .requestMatchers("/h2-console/**").permitAll()
                
                // Actuator endpoints - permit all (configure properly in production)
                .requestMatchers("/actuator/**").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Enable HTTP Basic Authentication
            .httpBasic(Customizer.withDefaults())
            
            // Allow H2 Console frames (development only)
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    /**
     * In-memory user store for demonstration
     * In production, use database-backed UserDetailsService
     * 
     * Users:
     * - admin/admin123 - ADMIN role (can create, view, delete orders)
     * - user/user123 - USER role (can only view orders)
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN")
                .build();

        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode("user123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    /**
     * Password encoder - BCrypt for secure password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
