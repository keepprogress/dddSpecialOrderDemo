package com.tgfc.som.common.util;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * JWT 工具類
 * 用於從 Keycloak JWT Token 提取資訊
 */
public final class JwtUtils {

    private JwtUtils() {
        // Utility class
    }

    /**
     * 從 JWT 提取使用者名稱
     * Keycloak 預設使用 preferred_username claim
     *
     * @param jwt JWT Token
     * @return 使用者名稱
     */
    public static String extractUsername(Jwt jwt) {
        // 優先使用 preferred_username (Keycloak 標準)
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // 備援: 使用 sub (subject)
        return jwt.getSubject();
    }

    /**
     * 從 JWT 提取電子郵件
     *
     * @param jwt JWT Token
     * @return 電子郵件，可能為 null
     */
    public static String extractEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    /**
     * 從 JWT 提取顯示名稱
     *
     * @param jwt JWT Token
     * @return 顯示名稱
     */
    public static String extractDisplayName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isEmpty()) {
            return name;
        }

        // 備援: 組合 given_name 和 family_name
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        if (givenName != null && familyName != null) {
            return givenName + " " + familyName;
        }

        return extractUsername(jwt);
    }

    /**
     * 檢查 JWT 是否包含指定的 realm role
     *
     * @param jwt  JWT Token
     * @param role 角色名稱
     * @return 是否擁有該角色
     */
    public static boolean hasRealmRole(Jwt jwt, String role) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return false;
        }

        var roles = realmAccess.get("roles");
        if (roles instanceof java.util.List<?> roleList) {
            return roleList.contains(role);
        }

        return false;
    }
}
