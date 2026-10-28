package fr.miage.skull.player.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final PlayerUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, PlayerUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(PREFIX.length());


            if (jwtService.isTokenValid(token)) {
                try {
                    String email = jwtService.extractEmail(token);
                    // On recharge le joueur à chaque requête : un joueur banni ou supprimé perd l'accès tout de suite
                    UserDetails user = userDetailsService.loadUserByUsername(email);


                    if (!user.isAccountNonLocked()) {SecurityContextHolder.clearContext();
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,"Ce compte a été banni");
                        return;
                    }

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);


                } catch (UsernameNotFoundException ignored) {
                    // joueur supprimé : requête laissée anonyme, elle sera refusée (401)
                }
            }
        }

        chain.doFilter(request, response);
    }
}
