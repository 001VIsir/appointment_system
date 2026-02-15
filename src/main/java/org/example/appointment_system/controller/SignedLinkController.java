package org.example.appointment_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.response.SignedLinkResponse;
import org.example.appointment_system.service.SignedLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * REST Controller for signed link operations.
 *
 * <p>Provides endpoints for merchants to generate secure, time-limited links
 * that allow public access to appointment booking pages.</p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>POST /api/merchants/links - Generate a signed link for a task</li>
 *   <li>GET /api/public/book/{taskId} - Public booking page (verified by SignedLinkFilter)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Signed Links", description = "Operations for generating and verifying signed links")
public class SignedLinkController {

    private final SignedLinkService signedLinkService;

    private static final DateTimeFormatter ISO_FORMATTER =
        DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));

    /**
     * Generate a signed link for an appointment task.
     *
     * <p>Creates a time-limited, cryptographically signed URL that allows
     * public access to the booking page for a specific appointment task.</p>
     *
     * @param taskId the ID of the appointment task
     * @return SignedLinkResponse containing the generated link and metadata
     */
    @PostMapping("/merchants/links")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    @Operation(
        summary = "Generate signed link",
        description = "Generate a time-limited, signed link for public access to a booking page"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signed link generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid task ID"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized (requires MERCHANT role)")
    })
    public ResponseEntity<SignedLinkResponse> generateSignedLink(
        @Parameter(description = "The appointment task ID")
        @RequestParam Long taskId
    ) {
        log.info("Generating signed link for task: {}", taskId);

        Instant expirationTime = signedLinkService.getDefaultExpirationTime();
        String link = signedLinkService.generateSignedLink(taskId, expirationTime);
        String fullUrl = "/api/public" + link;

        SignedLinkResponse response = SignedLinkResponse.builder()
            .link(link)
            .fullUrl(fullUrl)
            .taskId(taskId)
            .expiresAt(expirationTime.toEpochMilli())
            .expiresAtIso(ISO_FORMATTER.format(expirationTime))
            .valid(true)
            .build();

        log.info("Signed link generated for task {}: expires at {}", taskId, response.getExpiresAtIso());
        return ResponseEntity.ok(response);
    }

    /**
     * Verify a signed link.
     *
     * <p>Public endpoint that verifies the validity of a signed link.
     * This is used by the frontend to check if a link is still valid before
     * displaying the booking form.</p>
     *
     * @param taskId the ID of the appointment task
     * @param token the signature token
     * @param exp the expiration timestamp
     * @return SignedLinkResponse with validity information
     */
    @GetMapping("/public/links/verify")
    @Operation(
        summary = "Verify signed link",
        description = "Verify the validity of a signed link"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification completed"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<SignedLinkResponse> verifySignedLink(
        @Parameter(description = "The appointment task ID")
        @RequestParam Long taskId,
        @Parameter(description = "The signature token")
        @RequestParam String token,
        @Parameter(description = "The expiration timestamp")
        @RequestParam Long exp
    ) {
        log.debug("Verifying signed link for task: {}", taskId);

        boolean isValid = signedLinkService.verifySignedLink(taskId, token, exp);

        SignedLinkResponse response = SignedLinkResponse.builder()
            .taskId(taskId)
            .expiresAt(exp)
            .expiresAtIso(ISO_FORMATTER.format(Instant.ofEpochMilli(exp)))
            .valid(isValid)
            .build();

        return ResponseEntity.ok(response);
    }
}
