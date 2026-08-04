package com.neu.riketiku.renzheng;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class QuanXianBuZuChuLiQi implements AccessDeniedHandler {
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {
        AnQuanCuoWuXieRuQi.write(response, HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED", "没有访问该资源的权限");
    }
}
