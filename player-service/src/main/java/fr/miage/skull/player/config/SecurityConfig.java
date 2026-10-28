package fr.miage.skull.player.config;

import fr.miage.skull.player.security.JwtAuthFilter;
import fr.miage.skull.player.security.JwtService;
import fr.miage.skull.player.security.PlayerUserDetailsService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity   // active les annotations @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   PlayerUserDetailsService userDetailsService) throws Exception {

        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, userDetailsService);

        http
                .csrf(AbstractHttpConfigurer::disable)   // pas de cookie de session : le token est dans un header
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, ex) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())))
                .authorizeHttpRequests(auth -> auth
                        // sans cette ligne, un refus (sendError) est renvoyé vers /error, qui exige
                        // un token : le 403 devenait un 401
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/ping", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/players/register").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/players/*/ban").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/players").hasRole("ADMIN")
                        // GET/PUT/DELETE /players/{id} : connecté ici, "admin ou propriétaire" dans le contrôleur
                        .requestMatchers(HttpMethod.GET, "/api/v1/players/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/players/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/players/*").authenticated()
                        .anyRequest().denyAll()   // tout le reste est refusé par défaut
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
