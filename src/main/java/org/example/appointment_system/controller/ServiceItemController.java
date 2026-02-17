package org.example.appointment_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.ServiceItemRequest;
import org.example.appointment_system.dto.response.ServiceItemResponse;
import org.example.appointment_system.service.ServiceItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST Controller for service item management operations.
 *
 * <p>Provides endpoints for merchants to manage their service offerings.
 * All endpoints require MERCHANT or ADMIN role.</p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>POST /api/merchants/services - Create a new service item</li>
 *   <li>GET /api/merchants/services - Get all service items for current merchant</li>
 *   <li>GET /api/merchants/services/{id} - Get a specific service item</li>
 *   <li>PUT /api/merchants/services/{id} - Update a service item</li>
 *   <li>DELETE /api/merchants/services/{id} - Delete (deactivate) a service item</li>
 *   <li>POST /api/merchants/services/{id}/reactivate - Reactivate a deleted service</li>
 *   <li>GET /api/merchants/services/active - Get active service items</li>
 *   <li>GET /api/merchants/services/inactive - Get inactive (deleted) service items</li>
 *   <li>GET /api/merchants/services/count - Count service items</li>
 *   <li>GET /api/merchants/services/exists - Check if service name exists</li>
 * </ul>
 *
 * <h3>Security:</h3>
 * <p>All endpoints are protected by Spring Security and require
 * either MERCHANT or ADMIN role as configured in SecurityConfig.</p>
 */
@RestController
@RequestMapping("/api/merchants/services")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Service Item", description = "Endpoints for merchant service item management")
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    /**
     * Create a new service item for the current merchant.
     *
     * <p>The current user must have a merchant profile. Service names must be
     * unique within a merchant's catalog.</p>
     *
     * @param request the service item creation request
     * @return the created service item with 201 status
     */
    @PostMapping
    @Operation(
        summary = "Create a service item",
        description = "Creates a new service item for the current authenticated merchant. " +
                      "Service name must be unique within the merchant's catalog.",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Service item created successfully",
                content = @Content(schema = @Schema(implementation = ServiceItemResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request or service name already exists"
            ),
            @ApiResponse(
                responseCode = "403",
                description = "User does not have MERCHANT role or no merchant profile"
            )
        }
    )
    public ResponseEntity<ServiceItemResponse> createServiceItem(@Valid @RequestBody ServiceItemRequest request) {
        log.info("Creating service item: {}", request.getName());
        ServiceItemResponse response = serviceItemService.createServiceItem(request);
        return ResponseEntity
            .created(URI.create("/api/merchants/services/" + response.getId()))
            .body(response);
    }

    /**
     * Get all service items for the current merchant.
     *
     * <p>Returns both active and inactive service items.</p>
     *
     * @return list of all service items for the current merchant
     */
    @GetMapping
    @Operation(
        summary = "Get all service items",
        description = "Retrieves all service items for the current authenticated merchant, " +
                      "including both active and inactive items.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "List of service items",
                content = @Content(schema = @Schema(implementation = ServiceItemResponse.class))
            ),
            @ApiResponse(
                responseCode = "403",
                description = "User does not have MERCHANT role or no merchant profile"
            )
        }
    )
    public ResponseEntity<List<ServiceItemResponse>> getAllServiceItems() {
        List<ServiceItemResponse> services = serviceItemService.getAllServiceItems();
        return ResponseEntity.ok(services);
    }

    /**
     * Get a specific service item by ID.
     *
     * <p>Only returns service items owned by the current merchant.</p>
     *
     * @param id the service item ID
     * @return the service item or 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get service item by ID",
        description = "Retrieves a specific service item by ID. " +
                      "Only returns items owned by the current authenticated merchant.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Service item found",
                content = @Content(schema = @Schema(implementation = ServiceItemResponse.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Service item not found or not owned by current merchant"
            )
        }
    )
    public ResponseEntity<ServiceItemResponse> getServiceItemById(
            @Parameter(description = "Service item ID") @PathVariable Long id) {
        return serviceItemService.getServiceItemById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a service item.
     *
     * <p>Only the merchant who owns the service item can update it.
     * If the name is being changed, the new name must not conflict with
     * existing service names.</p>
     *
     * @param id the service item ID
     * @param request the update request
     * @return the updated service item
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update service item",
        description = "Updates a service item. Only the merchant who owns the service can update it. " +
                      "If changing the name, the new name must be unique.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Service item updated successfully",
                content = @Content(schema = @Schema(implementation = ServiceItemResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request or service name already exists"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Service item not found or not owned by current merchant"
            )
        }
    )
    public ResponseEntity<ServiceItemResponse> updateServiceItem(
            @Parameter(description = "Service item ID") @PathVariable Long id,
            @Valid @RequestBody ServiceItemRequest request) {
        log.info("Updating service item {}: {}", id, request.getName());
        ServiceItemResponse response = serviceItemService.updateServiceItem(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete (deactivate) a service item.
     *
     * <p>Performs a soft delete by setting active=false. The record
     * remains in the database but is hidden from active listings.</p>
     *
     * @param id the service item ID
     * @return empty response with 204 status
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete service item",
        description = "Soft deletes a service item by setting active=false. " +
                      "The record remains in the database and can be reactivated.",
        responses = {
            @ApiResponse(
                responseCode = "204",
                description = "Service item deleted successfully"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Service item not found or not owned by current merchant"
            )
        }
    )
    public ResponseEntity<Void> deleteServiceItem(
            @Parameter(description = "Service item ID") @PathVariable Long id) {
        log.info("Deleting service item {}", id);
        serviceItemService.deleteServiceItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivate a previously deleted service item.
     *
     * @param id the service item ID
     * @return the reactivated service item
     */
    @PostMapping("/{id}/reactivate")
    @Operation(
        summary = "Reactivate service item",
        description = "Reactivates a previously soft-deleted service item.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Service item reactivated successfully",
                content = @Content(schema = @Schema(implementation = ServiceItemResponse.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Service item not found or not owned by current merchant"
            )
        }
    )
    public ResponseEntity<ServiceItemResponse> reactivateServiceItem(
            @Parameter(description = "Service item ID") @PathVariable Long id) {
        log.info("Reactivating service item {}", id);
        ServiceItemResponse response = serviceItemService.reactivateServiceItem(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all active service items for the current merchant.
     *
     * <p>Only returns service items that are available for booking.</p>
     *
     * @return list of active service items
     */
    @GetMapping("/active")
    @Operation(
        summary = "Get active service items",
        description = "Retrieves all active service items for the current authenticated merchant. " +
                      "These are items available for booking."
    )
    public ResponseEntity<List<ServiceItemResponse>> getActiveServiceItems() {
        List<ServiceItemResponse> services = serviceItemService.getActiveServiceItems();
        return ResponseEntity.ok(services);
    }

    /**
     * Get all inactive (soft-deleted) service items for the current merchant.
     *
     * <p>Useful for managing/reactivating previously deleted services.</p>
     *
     * @return list of inactive service items
     */
    @GetMapping("/inactive")
    @Operation(
        summary = "Get inactive service items",
        description = "Retrieves all inactive (soft-deleted) service items for the current merchant. " +
                      "Useful for managing or reactivating previously deleted services."
    )
    public ResponseEntity<List<ServiceItemResponse>> getInactiveServiceItems() {
        List<ServiceItemResponse> services = serviceItemService.getInactiveServiceItems();
        return ResponseEntity.ok(services);
    }

    /**
     * Count service items for the current merchant.
     *
     * @param activeOnly if true, only count active items
     * @return the count of service items
     */
    @GetMapping("/count")
    @Operation(
        summary = "Count service items",
        description = "Returns the count of service items for the current merchant."
    )
    public ResponseEntity<Long> countServiceItems(
            @Parameter(description = "Only count active items")
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        long count = activeOnly
            ? serviceItemService.countActiveServiceItems()
            : serviceItemService.countAllServiceItems();
        return ResponseEntity.ok(count);
    }

    /**
     * Check if a service name exists for the current merchant.
     *
     * @param name the service name to check
     * @return true if the name exists, false otherwise
     */
    @GetMapping("/exists")
    @Operation(
        summary = "Check service name exists",
        description = "Checks if a service with the given name already exists for the current merchant."
    )
    public ResponseEntity<Boolean> checkNameExists(
            @Parameter(description = "Service name to check") @RequestParam String name) {
        boolean exists = serviceItemService.existsByName(name);
        return ResponseEntity.ok(exists);
    }
}
