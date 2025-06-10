package com.ltalk.web.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordEncoder {

    public static String generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16]; // 16바이트 = 128비트
        secureRandom.nextBytes(salt);//16바이트 랜덤 생성
        return Base64.getEncoder().encodeToString(salt);// base64로 인코딩
    }

    public static String encode(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // salt + password
            String combined = salt + password;
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));

            // Base64 인코딩
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }

    public static boolean comparePassword(String password, String salt, String hash) {
        return hash.equals(encode(password, salt));
    }
}
