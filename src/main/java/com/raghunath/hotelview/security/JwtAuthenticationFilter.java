package com.raghunath.hotelview.security;

import com.raghunath.hotelview.entity.AdminRefreshToken;
import com.raghunath.hotelview.repository.AdminRefreshTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AdminRefreshTokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();

            if (jwtUtil.validateToken(token)) {
                String hotelId = jwtUtil.extractHotelId(token);
                Long versionInJwt = jwtUtil.extractVersion(token);

                // 1. Fetch ALL active sessions for this hotel (Returns a List)
                // This prevents the 'NonUniqueResultException' crash during multiple logins
                List<AdminRefreshToken> activeSessions = tokenRepository.findByUserId(hotelId);

                // 2. Check if the version in the JWT matches ANY of the active sessions in DB
                boolean isVersionValid = activeSessions.stream()
                        .anyMatch(session -> session.getVersion() != null &&
                                session.getVersion().equals(versionInJwt));

                if (isVersionValid) {
                    String role = jwtUtil.extractRole(token);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    hotelId,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    // This specific session version is old/superseded by a refresh on THIS device
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"SESSION_INVALID\", \"message\": \"Session superseded. Please use latest token.\"}");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}