package com.dormpower.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;

/**
 * 加密工具类
 */
public class EncryptionUtil {

    private static final int ITERATIONS = 160000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * 生成随机盐值
     * @return 16字节的十六进制字符串
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * 使用PBKDF2算法哈希密码
     * @param password 明文密码
     * @param salt 盐值
     * @param iterations 迭代次数
     * @return 哈希后的十六进制字符串
     */
    public static String hashPassword(String password, String salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                HexFormat.of().parseHex(salt),
                iterations,
                KEY_LENGTH
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    /**
     * 哈希密码（使用默认迭代次数）
     * @param password 明文密码
     * @return 格式为"pbkdf2_sha256$iterations$salt$digest"的密码哈希字符串
     */
    public static String hashPassword(String password) {
        String salt = generateSalt();
        String digest = hashPassword(password, salt, ITERATIONS);
        return String.format("pbkdf2_sha256$%d$%s$%s", ITERATIONS, salt, digest);
    }

    /**
     * 验证密码
     * @param password 明文密码
     * @param encoded 存储的密码哈希字符串
     * @return 密码是否匹配
     */
    public static boolean verifyPassword(String password, String encoded) {
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            String algorithm = parts[0];
            if (!"pbkdf2_sha256".equals(algorithm)) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            String salt = parts[2];
            String expectedDigest = parts[3];
            String actualDigest = hashPassword(password, salt, iterations);
            return constantTimeEquals(expectedDigest, actualDigest);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 恒定时间比较，防止时序攻击
     * @param a 字符串a
     * @param b 字符串b
     * @return 是否相等
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * MD5加密
     * @param input 输入字符串
     * @return 加密后的字符串
     */
    public static String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA-256加密
     * @param input 输入字符串
     * @return 加密后的字符串
     */
    public static String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
