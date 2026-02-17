package org.example.appointment_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.MerchantProfileRequest;
import org.example.appointment_system.dto.request.MerchantSettingsRequest;
import org.example.appointment_system.dto.response.MerchantProfileResponse;
import org.example.appointment_system.dto.response.MerchantSettingsResponse;
import org.example.appointment_system.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST Controller for merchant management operations.
 *
 * <p>Provides endpoints for merchant profile and settings management.
 * All endpoints require MERCHANT or ADMIN role.</p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>POST /api/merchants/profile - Create merchant profile</li>
 *   <li>GET /api/merchants/profile - Get current merchant profile</li>
 *   <li>PUT /api/merchants/profile - Update merchant profile</li>
 *   <li>DELETE /api/merchants/profile - Delete merchant profile</li>
 *   <li>GET /api/merchants/settings - Get merchant settings</li>
 *   <li>PUT /api/merchants/settings - Update merchant settings</li>
 * </ul>
 *
 * <h3>Security:</h3>
 * <p>All endpoints are protected by Spring Security and require
 * either MERCHANT or ADMIN role as configured in SecurityConfig.</p>
 */
@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Merchant", description = "Endpoints for merchant profile and settings management")
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * Create a new merchant profile for the current user.
     *
     * <p>The current user must have MERCHANT role and must not already
     * have a merchant profile.</p>
     *
     * @param request the profile creation request
     * @return the created merchant profile
     */
    @PostMapping("/profile")
    @Operation(
        summary = "Create merchant profile",
        description = "Creates a new merchant profile for the current authenticated user. " +
                      "User must have MERCHANT role and must not already have a merchant profile.",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Merchant profile created successfully",
                content = @Content(schema = @Schema(implementation = MerchantProfileResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request or user already has a merchant profile"
            ),
            @ApiResponse(
                responseCode = "403",
                description = "User does not have MERCHANT role"
            )
        }
    )
    public ResponseEntity<MerchantProfileResponse> createProfile(@Valid @RequestBody MerchantProfileRequest request) {
        log.info("Creating merchant profile for business: {}", request.getBusinessName());
        MerchantProfileResponse response = merchantService.createProfile(request);
        return ResponseEntity
            .created(URI.create("/api/merchants/profile"))
            .body(response);
    }

    /**
     * Get the current user's merchant profile.
     *
     * @return the merchant profile or 404 if not found
     */
    @GetMapping("/profile")
    @Operation(
        summary = "Get merchant profile",
        description = "Retrieves the merchant profile for the current authenticated user.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Merchant profile found",
                content = @Content(schema = @Schema(implementation = MerchantProfileResponse.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Merchant profile not found for current user"
            )
        }
    )
    public ResponseEntity<MerchantProfileResponse> getProfile() {
        return merchantService.getCurrentMerchantProfile()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update the current user's merchant profile.
     *
     * @param request the profile update request
     * @return the updated merchant profile
     */
    @PutMapping("/profile")
    @Operation(
        summary = "Update merchant profile",
        description = "Updates the merchant profile for the current authenticated user. " +
                      "User must already have a merchant profile.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Merchant profile updated successfully",
                content = @Content(schema = @Schema(implementation = MerchantProfileResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request data"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Merchant profile not found for current user"
            )
        }
    )
    public ResponseEntity<MerchantProfileResponse> updateProfile(@Valid @RequestBody MerchantProfileRequest request) {
        log.info("Updating merchant profile for business: {}", request.getBusinessName());
        MerchantProfileResponse response = merchantService.updateProfile(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete the current user's merchant profile.
     *
     * @return empty response with 204 status
     */
    @DeleteMapping("/profile")
    @Operation(
        summary = "Delete merchant profile",
        description = "Deletes the merchant profile for the current authenticated user. " +
                      "This is a permanent deletion.",
        responses = {
            @ApiResponse(
                responseCode = "204",
                description = "Merchant profile deleted successfully"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Merchant profile not found for current user"
            )
        }
    )
    public ResponseEntity<Void> deleteProfile() {
        log.info("Deleting merchant profile");
        merchantService.deleteProfile();
        return ResponseEntity.noContent().build();
    }

    /**
     * Get the current user's merchant settings.
     *
     * @return the merchant settings
     */
    @GetMapping("/settings")
    @Operation(
        summary = "Get merchant settings",
        description = "Retrieves the settings for the current authenticated merchant. " +
                      "Returns default settings if no custom settings have been configured.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Merchant settings retrieved",
                content = @Content(schema = @Schema(implementation = MerchantSettingsResponse.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Merchant profile not found for current user"
            )
        }
    )
    public ResponseEntity<MerchantSettingsResponse> getSettings() {
        MerchantSettingsResponse response = merchantService.getSettings();
        return ResponseEntity.ok(response);
    }

    /**
     * Update the current user's merchant settings.
     *
     * <p>Only provided fields will be updated. Null fields are ignored.
     * Settings are merged with existing settings.</p>
     *
     * @param request the settings update request
     * @return the updated merchant settings
     */
    @PutMapping("/settings")
    @Operation(
        summary = "Update merchant settings",
        description = "Updates the settings for the current authenticated merchant. " +
                      "Only provided fields are updated; null fields are preserved. " +
                      "Settings are merged with existing settings.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Merchant settings updated successfully",
                content = @Content(schema = @Schema(implementation = MerchantSettingsResponse.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Merchant profile not found for current user"
            )
        }
    )
    public ResponseEntity<MerchantSettingsResponse> updateSettings(@RequestBody MerchantSettingsRequest request) {
        log.info("Updating merchant settings");
        MerchantSettingsResponse response = merchantService.updateSettings(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if the current user has a merchant profile.
     *
     * @return true if the user has a merchant profile, false otherwise
     */
    @GetMapping("/profile/exists")
    @Operation(
        summary = "Check merchant profile exists",
        description = "Checks if the current authenticated user has a merchant profile."
    )
    public ResponseEntity<Boolean> hasProfile() {
        boolean hasProfile = merchantService.hasMerchantProfile();
        return ResponseEntity.ok(hasProfile);
    }
}
