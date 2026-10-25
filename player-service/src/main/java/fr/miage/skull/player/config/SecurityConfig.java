package fr.miage.skull.player.config;

import fr.miage.skull.player.security.JwtAuthFilter;
import fr.miage.skull.player.security.JwtService;
import fr.miage.skull.player.security.PlayerUserDetailsService;
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
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, PlayerUserDetailsService userDetailsService) throws Exception {
        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, userDetailsService);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers(

                                HttpMethod.POST, "/api/v1/players/register","/api/v1/auth/login").permitAll()


                        .requestMatchers("/ping").permitAll()

                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**","/v3/api-docs", "/swagger-ui.html").permitAll()

                        .requestMatchers(HttpMethod.PUT, "/api/v1/players/*/ban").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/players/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/players/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/players").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/players/*").hasRole("ADMIN")

                        .anyRequest().denyAll()



                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())
                        )
                )
                .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
