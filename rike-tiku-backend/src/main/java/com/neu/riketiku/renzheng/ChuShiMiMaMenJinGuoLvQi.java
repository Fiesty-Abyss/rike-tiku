package com.neu.riketiku.renzheng;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ChuShiMiMaMenJinGuoLvQi extends OncePerRequestFilter {
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/health",
            "/api/v1/auth/me",
            "/api/v1/auth/change-initial-password");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof RenZhengYongHu user
                && user.biXuXiuGaiMiMa()
                && !ALLOWED_PATHS.contains(request.getRequestURI())) {
            AnQuanCuoWuXieRuQi.write(response, HttpServletResponse.SC_FORBIDDEN,
                    "MUST_CHANGE_PASSWORD", "必须先修改初始密码");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
