package com.neu.riketiku.renzheng;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class WeiRenZhengChuLiQi implements AuthenticationEntryPoint {
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        AnQuanCuoWuXieRuQi.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHENTICATED", "请先登录");
    }
}
