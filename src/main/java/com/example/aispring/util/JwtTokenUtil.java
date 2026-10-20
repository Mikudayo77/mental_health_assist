package com.example.aispring.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.aispring.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
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
    // 验证token
    public static TokenVerificationResult validateToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        String username = jwt.getClaim("username").asString();

        // 角色类型
        Integer roleType = null;
        try {
            roleType = jwt.getClaim("roleType").asInt();
        } catch (Exception e) {
            String roleTypeStr = jwt.getClaim("roleType").asString();
            if (StringUtils.hasText(roleTypeStr)) {
                roleType = Integer.valueOf(roleTypeStr);
            }
        }
        if (userId != null && StringUtils.hasText(username) && roleType != null) {
            return new TokenVerificationResult(userId, username, roleType, true);
        }
        return null;
    }
    public static DecodedJWT verifyToken(String token) {
        if(!StringUtils.hasText(token)){
            throw new JWTVerificationException("token is empty");
        }
        // token解码
        JwtConfig jwtConfig = getJwtConfig();
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        JWTVerifier verifier =  JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return  verifier.verify(token);
    }
    // Token验证结果封装类
    @Getter
    public static class TokenVerificationResult {
        private final Long userId;
        private final String username;
        private final Integer roleType;
        private final boolean valid;

        public TokenVerificationResult(Long userId, String username, Integer roleType, boolean valid) {
            this.userId = userId;
            this.username = username;
            this.roleType = roleType;
            this.valid = valid;
        }
    }
}
