package com.travelmap.api.security;

import com.travelmap.api.auth.service.JwtClaims;
import com.travelmap.api.auth.service.TokenService;
import com.travelmap.api.common.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                JwtClaims claims = tokenService.parseAccessToken(authorization.substring(7));
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.email(), null, List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ApiException exception) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
