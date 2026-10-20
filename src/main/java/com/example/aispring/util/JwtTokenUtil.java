package com.example.aispring.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.aispring.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
@Component
public class JwtTokenUtil implements ApplicationContextAware {
    private static final String ISSUER = "mental-health-assistant";
    private static ApplicationContext applicationContext;

    //用于静态工具类中获取Spring容器管理的Bean
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        // 获取Spring容器管理的Bean
        JwtTokenUtil.applicationContext = applicationContext;
    }

    private static JwtConfig getJwtConfig() {
        return applicationContext.getBean(JwtConfig.class);
    }

    public static String generateToken(String userID, String username, String roletype) {
        // 生成JWT token的逻辑
        try {
            JwtConfig jwtConfig = getJwtConfig();
            Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
            Date expiration = new Date((System.currentTimeMillis() + jwtConfig.getExpiration()));
            String token = JWT.create()
                    .withClaim("userID", userID)
                    .withClaim("username", username)
                    .withClaim("roletype", roletype)
                    .withIssuedAt(new Date())
                    .withIssuer(ISSUER)
                    .withExpiresAt(expiration)
                    .sign(algorithm);
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }
    public static String extractTokenFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String tokenHeader = request.getHeader("token");
        if (StringUtils.hasText(tokenHeader)) {
            return tokenHeader;
        }
        return null;
    }
}
