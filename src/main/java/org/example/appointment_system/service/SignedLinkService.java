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
 * Service for generating and verifying HMAC-signed links.
 *
 * <p>This service provides secure, time-limited links for public access to
 * appointment booking pages without requiring user authentication.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>HMAC-SHA256 signature generation</li>
 *   <li>Configurable link expiration time</li>
 *   <li>Secure token verification</li>
 *   <li>Time-based expiration checking</li>
 * </ul>
 *
 * <h3>Link Format:</h3>
 * <pre>
 * /book/{taskId}?token={signature}&exp={expiryTimestamp}
 * </pre>
 *
 * <h3>Signature Algorithm:</h3>
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
     * Generate a signed link for an appointment task.
     *
     * <p>Creates a time-limited, cryptographically signed URL that allows
     * public access to the booking page for a specific appointment task.</p>
     *
     * @param taskId the ID of the appointment task
     * @return the signed link path (without base URL)
     */
    public String generateSignedLink(Long taskId) {
        return generateSignedLink(taskId, Instant.now().plus(expirationHours, ChronoUnit.HOURS));
    }

    /**
     * Generate a signed link with a custom expiration time.
     *
     * @param taskId the ID of the appointment task
     * @param expirationTime the instant when the link expires
     * @return the signed link path (without base URL)
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
     * Verify a signed link token.
     *
     * <p>Validates the signature and checks if the link has not expired.</p>
     *
     * @param taskId the ID of the appointment task
     * @param token the signature token from the URL
     * @param expiryTimestamp the expiration timestamp from the URL
     * @return true if the link is valid and not expired
     */
    public boolean verifySignedLink(Long taskId, String token, long expiryTimestamp) {
        if (taskId == null || token == null) {
            log.warn("Invalid signed link verification: taskId={}, token={}", taskId, token != null ? "present" : "null");
            return false;
        }

        // Check expiration first
        if (isExpired(expiryTimestamp)) {
            log.warn("Signed link expired for task {}: expiry={}", taskId, Instant.ofEpochMilli(expiryTimestamp));
            return false;
        }

        // Verify signature
        String expectedSignature = generateSignature(taskId, expiryTimestamp);
        String expectedToken = encodeToken(expectedSignature);

        boolean isValid = constantTimeEquals(expectedToken, token);
        if (!isValid) {
            log.warn("Invalid signature for task {}", taskId);
        }

        return isValid;
    }

    /**
     * Check if a link has expired.
     *
     * @param expiryTimestamp the expiration timestamp in milliseconds
     * @return true if the current time is past the expiration
     */
    public boolean isExpired(long expiryTimestamp) {
        return Instant.now().toEpochMilli() > expiryTimestamp;
    }

    /**
     * Get the configured expiration time from now.
     *
     * @return the expiration instant
     */
    public Instant getDefaultExpirationTime() {
        return Instant.now().plus(expirationHours, ChronoUnit.HOURS);
    }

    /**
     * Generate HMAC-SHA256 signature.
     *
     * @param taskId the task ID
     * @param expiryTimestamp the expiration timestamp
     * @return the hex-encoded signature
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
     * Encode signature to URL-safe token.
     *
     * @param signature the hex-encoded signature
     * @return the URL-safe encoded token
     */
    private String encodeToken(String signature) {
        // Use Base64URL encoding for URL safety
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Convert byte array to hexadecimal string.
     *
     * @param bytes the byte array
     * @return the hex string
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
     * Constant-time string comparison to prevent timing attacks.
     *
     * @param a the first string
     * @param b the second string
     * @return true if strings are equal
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
