package com.neu.riketiku.renzheng;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtRenZhengGuoLvQi extends OncePerRequestFilter {
    private final JwtLingPaiFuWu jwtService;

    public JwtRenZhengGuoLvQi(JwtLingPaiFuWu jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            AnQuanCuoWuXieRuQi.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "TOKEN_INVALID", "访问令牌格式不正确");
            return;
        }

        try {
            RenZhengYongHu principal = jwtService.jieXiLingPai(authorization.substring(7));
            List<SimpleGrantedAuthority> authorities = principal.jiaoSe().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            AnQuanCuoWuXieRuQi.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "TOKEN_EXPIRED", "访问令牌已过期");
        } catch (JwtException | IllegalArgumentException exception) {
            AnQuanCuoWuXieRuQi.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "TOKEN_INVALID", "访问令牌无效");
        }
    }
}
