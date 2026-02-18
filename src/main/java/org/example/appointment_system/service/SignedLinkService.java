package org.example.appointment_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * 用于生成和验证HMAC签名链接的服务类。
 *
 * <p>此服务提供安全的、有时间限制的链接，用于公开访问预约页面，
 * 无需用户认证。</p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>HMAC-SHA256签名生成</li>
 *   <li>可配置的链接过期时间</li>
 *   <li>安全的令牌验证</li>
 *   <li>基于时间的过期检查</li>
 * </ul>
 *
 * <h3>链接格式：</h3>
 * <pre>
 * /book/{taskId}?token={signature}&exp={expiryTimestamp}
 * </pre>
 *
 * <h3>签名算法：</h3>
 * <pre>
 * signature = HMAC-SHA256(secretKey, taskId + ":" + expiryTimestamp)
 * token = Base64URL(signature)
 * </pre>
 */
@Service
@Slf4j
public class SignedLinkService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String URL_PATH_PREFIX = "/book/";

    @Value("${app.signed-link.secret:default-secret-key-change-in-production}")
    private String secretKey;

    @Value("${app.signed-link.expiration-hours:72}")
    private int expirationHours;

    /**
     * 为预约任务生成签名链接。
     *
     * <p>创建加密签名的、有时间限制的URL，允许
     * 公开访问特定预约任务的预约页面。</p>
     *
     * @param taskId 预约任务的ID
     * @return 签名链接路径（不含基础URL）
     */
    public String generateSignedLink(Long taskId) {
        return generateSignedLink(taskId, Instant.now().plus(expirationHours, ChronoUnit.HOURS));
    }

    /**
     * 生成具有自定义过期时间的签名链接。
     *
     * @param taskId 预约任务的ID
     * @param expirationTime 链接过期的时刻
     * @return 签名链接路径（不含基础URL）
     */
    public String generateSignedLink(Long taskId, Instant expirationTime) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID cannot be null");
        }

        long expiryTimestamp = expirationTime.toEpochMilli();
        String signature = generateSignature(taskId, expiryTimestamp);
        String token = encodeToken(signature);

        String link = String.format("%s%d?token=%s&exp=%d", URL_PATH_PREFIX, taskId, token, expiryTimestamp);
        log.debug("Generated signed link for task {}: {}", taskId, link);

        return link;
    }

    /**
     * 验证签名链接令牌。
     *
     * <p>验证签名并检查链接是否未过期。</p>
     *
     * @param taskId 预约任务的ID
     * @param token URL中的签名令牌
     * @param expiryTimestamp URL中的过期时间戳
     * @return 如果链接有效且未过期返回true
     */
    public boolean verifySignedLink(Long taskId, String token, long expiryTimestamp) {
        if (taskId == null || token == null) {
            log.warn("Invalid signed link verification: taskId={}, token={}", taskId, token != null ? "present" : "null");
            return false;
        }

        // 首先检查过期时间
        if (isExpired(expiryTimestamp)) {
            log.warn("Signed link expired for task {}: expiry={}", taskId, Instant.ofEpochMilli(expiryTimestamp));
            return false;
        }

        // 验证签名
        String expectedSignature = generateSignature(taskId, expiryTimestamp);
        String expectedToken = encodeToken(expectedSignature);

        boolean isValid = constantTimeEquals(expectedToken, token);
        if (!isValid) {
            log.warn("Invalid signature for task {}", taskId);
        }

        return isValid;
    }

    /**
     * 检查链接是否已过期。
     *
     * @param expiryTimestamp 过期时间戳（毫秒）
     * @return 如果当前时间已超过过期时间返回true
     */
    public boolean isExpired(long expiryTimestamp) {
        return Instant.now().toEpochMilli() > expiryTimestamp;
    }

    /**
     * 获取从现在开始的配置的过期时间。
     *
     * @return 过期时刻
     */
    public Instant getDefaultExpirationTime() {
        return Instant.now().plus(expirationHours, ChronoUnit.HOURS);
    }

    /**
     * 生成HMAC-SHA256签名。
     *
     * @param taskId 任务ID
     * @param expiryTimestamp 过期时间戳
     * @return 十六进制编码的签名
     */
    private String generateSignature(Long taskId, long expiryTimestamp) {
        String message = taskId + ":" + expiryTimestamp;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate signature for task {}", taskId, e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * 将签名编码为URL安全的令牌。
     *
     * @param signature 十六进制编码的签名
     * @return URL安全编码的令牌
     */
    private String encodeToken(String signature) {
        // 使用Base64URL编码以确保URL安全
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 恒定时间的字符串比较以防止时序攻击。
     *
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return 如果字符串相等返回true
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
