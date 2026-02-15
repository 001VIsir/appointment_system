package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for signed link generation.
 *
 * <p>Contains the generated link and related metadata.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedLinkResponse {

    /**
     * The signed link path (e.g., /book/123?token=xxx&exp=xxx).
     */
    private String link;

    /**
     * The full URL including the base path (e.g., /api/public/book/123?token=xxx&exp=xxx).
     */
    private String fullUrl;

    /**
     * The task ID for which the link was generated.
     */
    private Long taskId;

    /**
     * The expiration timestamp (Unix epoch in milliseconds).
     */
    private Long expiresAt;

    /**
     * The expiration time as ISO-8601 string.
     */
    private String expiresAtIso;

    /**
     * Whether the link is currently valid.
     */
    private Boolean valid;
}
