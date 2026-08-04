package com.neu.riketiku.renzheng;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtLingPaiFuWu {
    private final SecretKey secretKey;
    private final long expirationSeconds;

    public JwtLingPaiFuWu(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("RIKE_TIKU_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("JWT expiration seconds must be positive");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String shengChengLingPai(RenZhengYongHu user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(user.yongHuMing())
                .claim("uid", user.id())
                .claim("roles", user.jiaoSe())
                .claim("mustChangePassword", user.biXuXiuGaiMiMa())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(expirationSeconds)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public RenZhengYongHu jieXiLingPai(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number userId = claims.get("uid", Number.class);
        Object rawRoles = claims.get("roles");
        Boolean mustChangePassword = claims.get("mustChangePassword", Boolean.class);
        if (userId == null || claims.getSubject() == null || !(rawRoles instanceof List<?> roleList)
                || mustChangePassword == null) {
            throw new IllegalArgumentException("JWT required claims are missing");
        }

        List<String> roles = new ArrayList<>();
        for (Object rawRole : roleList) {
            String role = String.valueOf(rawRole);
            JiaoSeDaiMa.valueOf(role);
            roles.add(role);
        }
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("JWT roles are empty");
        }
        return new RenZhengYongHu(
                userId.longValue(), claims.getSubject(), List.copyOf(roles), mustChangePassword);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
