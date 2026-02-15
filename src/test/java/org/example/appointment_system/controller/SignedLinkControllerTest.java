package org.example.appointment_system.controller;

import org.example.appointment_system.dto.response.SignedLinkResponse;
import org.example.appointment_system.service.SignedLinkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SignedLinkController.
 */
@ExtendWith(MockitoExtension.class)
class SignedLinkControllerTest {

    @Mock
    private SignedLinkService signedLinkService;

    @InjectMocks
    private SignedLinkController signedLinkController;

    private static final Long TASK_ID = 123L;
    private static final String TEST_LINK = "/book/123?token=abc123&exp=1234567890000";
    private static final Instant TEST_EXPIRATION = Instant.now().plus(72, ChronoUnit.HOURS);

    @Nested
    @DisplayName("generateSignedLink")
    class GenerateSignedLink {

        @Test
        @DisplayName("should generate signed link successfully")
        void shouldGenerateSignedLinkSuccessfully() {
            when(signedLinkService.getDefaultExpirationTime()).thenReturn(TEST_EXPIRATION);
            when(signedLinkService.generateSignedLink(any(Long.class), any(Instant.class))).thenReturn(TEST_LINK);

            ResponseEntity<SignedLinkResponse> response = signedLinkController.generateSignedLink(TASK_ID);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());

            SignedLinkResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(TEST_LINK, body.getLink());
            assertEquals("/api/public" + TEST_LINK, body.getFullUrl());
            assertEquals(TASK_ID, body.getTaskId());
            assertEquals(TEST_EXPIRATION.toEpochMilli(), body.getExpiresAt());
            assertTrue(body.getValid());
        }

        @Test
        @DisplayName("should call service with correct parameters")
        void shouldCallServiceWithCorrectParameters() {
            when(signedLinkService.getDefaultExpirationTime()).thenReturn(TEST_EXPIRATION);
            when(signedLinkService.generateSignedLink(TASK_ID, TEST_EXPIRATION)).thenReturn(TEST_LINK);

            signedLinkController.generateSignedLink(TASK_ID);

            verify(signedLinkService).getDefaultExpirationTime();
            verify(signedLinkService).generateSignedLink(TASK_ID, TEST_EXPIRATION);
        }
    }

    @Nested
    @DisplayName("verifySignedLink")
    class VerifySignedLink {

        @Test
        @DisplayName("should return valid=true for valid link")
        void shouldReturnValidTrueForValidLink() {
            String token = "abc123";
            long exp = System.currentTimeMillis() + 3600000L;

            when(signedLinkService.verifySignedLink(TASK_ID, token, exp)).thenReturn(true);

            ResponseEntity<SignedLinkResponse> response = signedLinkController.verifySignedLink(TASK_ID, token, exp);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());

            SignedLinkResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(TASK_ID, body.getTaskId());
            assertEquals(exp, body.getExpiresAt());
            assertTrue(body.getValid());
        }

        @Test
        @DisplayName("should return valid=false for invalid link")
        void shouldReturnValidFalseForInvalidLink() {
            String token = "invalid-token";
            long exp = System.currentTimeMillis() + 3600000L;

            when(signedLinkService.verifySignedLink(TASK_ID, token, exp)).thenReturn(false);

            ResponseEntity<SignedLinkResponse> response = signedLinkController.verifySignedLink(TASK_ID, token, exp);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());

            SignedLinkResponse body = response.getBody();
            assertNotNull(body);
            assertFalse(body.getValid());
        }

        @Test
        @DisplayName("should return valid=false for expired link")
        void shouldReturnValidFalseForExpiredLink() {
            String token = "abc123";
            long exp = System.currentTimeMillis() - 3600000L; // Expired 1 hour ago

            when(signedLinkService.verifySignedLink(TASK_ID, token, exp)).thenReturn(false);

            ResponseEntity<SignedLinkResponse> response = signedLinkController.verifySignedLink(TASK_ID, token, exp);

            SignedLinkResponse body = response.getBody();
            assertNotNull(body);
            assertFalse(body.getValid());
        }

        @Test
        @DisplayName("should call service with correct parameters")
        void shouldCallServiceWithCorrectParameters() {
            String token = "abc123";
            long exp = 1234567890000L;

            signedLinkController.verifySignedLink(TASK_ID, token, exp);

            verify(signedLinkService).verifySignedLink(TASK_ID, token, exp);
        }
    }
}
