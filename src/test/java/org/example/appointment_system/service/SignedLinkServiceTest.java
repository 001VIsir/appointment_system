package org.example.appointment_system.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SignedLinkService.
 */
class SignedLinkServiceTest {

    private SignedLinkService signedLinkService;

    @BeforeEach
    void setUp() {
        signedLinkService = new SignedLinkService();
        ReflectionTestUtils.setField(signedLinkService, "secretKey", "test-secret-key-for-unit-testing-min-32-chars");
        ReflectionTestUtils.setField(signedLinkService, "expirationHours", 72);
    }

    @Nested
    @DisplayName("generateSignedLink")
    class GenerateSignedLink {

        @Test
        @DisplayName("should generate valid signed link with default expiration")
        void shouldGenerateValidSignedLinkWithDefaultExpiration() {
            Long taskId = 123L;

            String link = signedLinkService.generateSignedLink(taskId);

            assertNotNull(link);
            assertTrue(link.startsWith("/book/123?token="));
            assertTrue(link.contains("&exp="));

            // Extract token and expiration from link
            String[] parts = link.split("\\?");
            assertEquals("/book/123", parts[0]);

            String[] params = parts[1].split("&");
            assertTrue(params[0].startsWith("token="));
            assertTrue(params[1].startsWith("exp="));

            String token = params[0].substring(6);
            long exp = Long.parseLong(params[1].substring(4));

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(exp > Instant.now().toEpochMilli());
        }

        @Test
        @DisplayName("should generate valid signed link with custom expiration")
        void shouldGenerateValidSignedLinkWithCustomExpiration() {
            Long taskId = 456L;
            Instant expirationTime = Instant.now().plus(24, ChronoUnit.HOURS);

            String link = signedLinkService.generateSignedLink(taskId, expirationTime);

            assertNotNull(link);
            assertTrue(link.startsWith("/book/456?token="));

            // Verify expiration timestamp is in the link
            long expTimestamp = expirationTime.toEpochMilli();
            assertTrue(link.contains("exp=" + expTimestamp));
        }

        @Test
        @DisplayName("should throw exception when taskId is null")
        void shouldThrowExceptionWhenTaskIdIsNull() {
            assertThrows(IllegalArgumentException.class, () -> {
                signedLinkService.generateSignedLink(null);
            });
        }

        @Test
        @DisplayName("should generate different tokens for different task IDs")
        void shouldGenerateDifferentTokensForDifferentTaskIds() {
            String link1 = signedLinkService.generateSignedLink(1L);
            String link2 = signedLinkService.generateSignedLink(2L);

            assertNotEquals(link1, link2);
        }

        @Test
        @DisplayName("should generate different tokens for same task with different expiration")
        void shouldGenerateDifferentTokensForSameTaskWithDifferentExpiration() {
            Long taskId = 100L;
            Instant exp1 = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant exp2 = Instant.now().plus(2, ChronoUnit.HOURS);

            String link1 = signedLinkService.generateSignedLink(taskId, exp1);
            String link2 = signedLinkService.generateSignedLink(taskId, exp2);

            assertNotEquals(link1, link2);
        }

        @Test
        @DisplayName("should generate same token for same task and expiration")
        void shouldGenerateSameTokenForSameTaskAndExpiration() {
            Long taskId = 100L;
            Instant expiration = Instant.now().plus(24, ChronoUnit.HOURS);

            String link1 = signedLinkService.generateSignedLink(taskId, expiration);
            String link2 = signedLinkService.generateSignedLink(taskId, expiration);

            assertEquals(link1, link2);
        }
    }

    @Nested
    @DisplayName("verifySignedLink")
    class VerifySignedLink {

        @Test
        @DisplayName("should verify valid signed link")
        void shouldVerifyValidSignedLink() {
            Long taskId = 123L;
            Instant expiration = Instant.now().plus(24, ChronoUnit.HOURS);

            String link = signedLinkService.generateSignedLink(taskId, expiration);

            // Extract token and exp from link
            String token = extractToken(link);
            long exp = extractExp(link);

            assertTrue(signedLinkService.verifySignedLink(taskId, token, exp));
        }

        @Test
        @DisplayName("should reject expired link")
        void shouldRejectExpiredLink() {
            Long taskId = 123L;
            Instant expiration = Instant.now().minus(1, ChronoUnit.HOURS);

            String link = signedLinkService.generateSignedLink(taskId, expiration);

            String token = extractToken(link);
            long exp = extractExp(link);

            assertFalse(signedLinkService.verifySignedLink(taskId, token, exp));
        }

        @Test
        @DisplayName("should reject invalid token")
        void shouldRejectInvalidToken() {
            Long taskId = 123L;
            Instant expiration = Instant.now().plus(24, ChronoUnit.HOURS);

            String link = signedLinkService.generateSignedLink(taskId, expiration);

            long exp = extractExp(link);
            String invalidToken = "invalid-token-string";

            assertFalse(signedLinkService.verifySignedLink(taskId, invalidToken, exp));
        }

        @Test
        @DisplayName("should reject token for different task")
        void shouldRejectTokenForDifferentTask() {
            Long taskId1 = 123L;
            Long taskId2 = 456L;
            Instant expiration = Instant.now().plus(24, ChronoUnit.HOURS);

            String link = signedLinkService.generateSignedLink(taskId1, expiration);

            String token = extractToken(link);
            long exp = extractExp(link);

            // Token was generated for taskId1, but we're trying to use it for taskId2
            assertFalse(signedLinkService.verifySignedLink(taskId2, token, exp));
        }

        @Test
        @DisplayName("should reject null taskId")
        void shouldRejectNullTaskId() {
            assertFalse(signedLinkService.verifySignedLink(null, "token", System.currentTimeMillis()));
        }

        @Test
        @DisplayName("should reject null token")
        void shouldRejectNullToken() {
            assertFalse(signedLinkService.verifySignedLink(123L, null, System.currentTimeMillis()));
        }

        @Test
        @DisplayName("should reject tampered expiration")
        void shouldRejectTamperedExpiration() {
            Long taskId = 123L;
            Instant expiration = Instant.now().plus(24, ChronoUnit.HOURS);

            String link = signedLinkService.generateSignedLink(taskId, expiration);

            String token = extractToken(link);
            // Tamper with expiration (add 1 hour)
            long tamperedExp = extractExp(link) + 3600000L;

            assertFalse(signedLinkService.verifySignedLink(taskId, token, tamperedExp));
        }
    }

    @Nested
    @DisplayName("isExpired")
    class IsExpired {

        @Test
        @DisplayName("should return true for past timestamp")
        void shouldReturnTrueForPastTimestamp() {
            long pastTimestamp = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();
            assertTrue(signedLinkService.isExpired(pastTimestamp));
        }

        @Test
        @DisplayName("should return false for future timestamp")
        void shouldReturnFalseForFutureTimestamp() {
            long futureTimestamp = Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli();
            assertFalse(signedLinkService.isExpired(futureTimestamp));
        }

        @Test
        @DisplayName("should return true for current timestamp (edge case)")
        void shouldReturnTrueForCurrentTimestamp() {
            // Using a timestamp slightly in the past to account for execution time
            long now = Instant.now().minusMillis(1).toEpochMilli();
            assertTrue(signedLinkService.isExpired(now));
        }
    }

    @Nested
    @DisplayName("getDefaultExpirationTime")
    class GetDefaultExpirationTime {

        @Test
        @DisplayName("should return expiration time 72 hours from now")
        void shouldReturnExpirationTime72HoursFromNow() {
            Instant expiration = signedLinkService.getDefaultExpirationTime();

            Instant expectedMin = Instant.now().plus(71, ChronoUnit.HOURS);
            Instant expectedMax = Instant.now().plus(73, ChronoUnit.HOURS);

            assertTrue(expiration.isAfter(expectedMin));
            assertTrue(expiration.isBefore(expectedMax));
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("should generate different signatures with different secret keys")
        void shouldGenerateDifferentSignaturesWithDifferentSecretKeys() {
            Long taskId = 123L;

            String link1 = signedLinkService.generateSignedLink(taskId);

            // Change the secret key
            ReflectionTestUtils.setField(signedLinkService, "secretKey", "different-secret-key-for-testing-32-ch");
            String link2 = signedLinkService.generateSignedLink(taskId);

            assertNotEquals(link1, link2);
        }

        @Test
        @DisplayName("should reject link generated with different secret key")
        void shouldRejectLinkGeneratedWithDifferentSecretKey() {
            Long taskId = 123L;
            Instant expiration = Instant.now().plus(24, ChronoUnit.HOURS);

            String link = signedLinkService.generateSignedLink(taskId, expiration);
            String token = extractToken(link);
            long exp = extractExp(link);

            // Change the secret key
            ReflectionTestUtils.setField(signedLinkService, "secretKey", "different-secret-key-for-testing-32-ch");

            // Verification should fail with different secret
            assertFalse(signedLinkService.verifySignedLink(taskId, token, exp));
        }

        @Test
        @DisplayName("should prevent timing attacks with constant time comparison")
        void shouldPreventTimingAttacks() {
            Long taskId = 123L;
            Instant expiration = Instant.now().plus(24, ChronoUnit.HOURS);

            String link = signedLinkService.generateSignedLink(taskId, expiration);
            String token = extractToken(link);
            long exp = extractExp(link);

            // Measure time for correct token
            long startCorrect = System.nanoTime();
            signedLinkService.verifySignedLink(taskId, token, exp);
            long timeCorrect = System.nanoTime() - startCorrect;

            // Measure time for completely wrong token
            long startWrong = System.nanoTime();
            signedLinkService.verifySignedLink(taskId, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", exp);
            long timeWrong = System.nanoTime() - startWrong;

            // Time difference should not be significant (within 10x factor)
            // Note: This is a rough check; proper timing attack tests need more samples
            double ratio = (double) timeCorrect / timeWrong;
            assertTrue(ratio > 0.1 && ratio < 10.0,
                "Timing ratio suggests potential timing attack vulnerability: " + ratio);
        }
    }

    // Helper methods to extract token and exp from link
    private String extractToken(String link) {
        int tokenStart = link.indexOf("token=") + 6;
        int tokenEnd = link.indexOf("&", tokenStart);
        return link.substring(tokenStart, tokenEnd);
    }

    private long extractExp(String link) {
        int expStart = link.indexOf("exp=") + 4;
        return Long.parseLong(link.substring(expStart));
    }
}
