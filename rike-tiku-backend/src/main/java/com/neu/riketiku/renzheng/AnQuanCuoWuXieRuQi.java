package com.neu.riketiku.renzheng;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

public final class AnQuanCuoWuXieRuQi {
    private AnQuanCuoWuXieRuQi() {
    }

    public static void write(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + escape(code)
                + "\",\"message\":\"" + escape(message)
                + "\",\"timestamp\":\"" + Instant.now() + "\"}");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
