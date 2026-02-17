package org.example.appointment_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.appointment_system.dto.request.ServiceItemRequest;
import org.example.appointment_system.dto.response.ServiceItemResponse;
import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.service.ServiceItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ServiceItemController using standalone MockMvc setup.
 */
@ExtendWith(MockitoExtension.class)
class ServiceItemControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ServiceItemService serviceItemService;

    @InjectMocks
    private ServiceItemController serviceItemController;

    private ServiceItemRequest validRequest;
    private ServiceItemResponse validResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(serviceItemController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        validRequest = ServiceItemRequest.builder()
            .name("Haircut")
            .description("Professional haircut service")
            .category(ServiceCategory.BEAUTY)
            .duration(30)
            .price(new BigDecimal("25.00"))
            .active(true)
            .build();

        validResponse = ServiceItemResponse.builder()
            .id(1L)
            .merchantId(1L)
            .merchantBusinessName("Test Salon")
            .name("Haircut")
            .description("Professional haircut service")
            .category(ServiceCategory.BEAUTY)
            .categoryDisplayName("Beauty & Wellness")
            .duration(30)
            .price(new BigDecimal("25.00"))
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("POST /api/merchants/services")
    class CreateServiceItemEndpointTests {

        @Test
        @DisplayName("should create service item successfully and return 201")
        void createServiceItem_withValidRequest_shouldReturn201() throws Exception {
            // Given
            when(serviceItemService.createServiceItem(any(ServiceItemRequest.class)))
                .thenReturn(validResponse);

            // When/Then
            mockMvc.perform(post("/api/merchants/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.merchantId").value(1))
                .andExpect(jsonPath("$.merchantBusinessName").value("Test Salon"))
                .andExpect(jsonPath("$.name").value("Haircut"))
                .andExpect(jsonPath("$.description").value("Professional haircut service"))
                .andExpect(jsonPath("$.category").value("BEAUTY"))
                .andExpect(jsonPath("$.categoryDisplayName").value("Beauty & Wellness"))
                .andExpect(jsonPath("$.duration").value(30))
                .andExpect(jsonPath("$.price").value(25.00))
                .andExpect(jsonPath("$.active").value(true));

            verify(serviceItemService).createServiceItem(any(ServiceItemRequest.class));
        }

        @Test
        @DisplayName("should create service item with default active=true when not specified")
        void createServiceItem_withoutActive_shouldDefaultToTrue() throws Exception {
            // Given
            ServiceItemRequest requestWithoutActive = ServiceItemRequest.builder()
                .name("Consultation")
                .description("Initial consultation")
                .category(ServiceCategory.CONSULTATION)
                .duration(60)
                .price(new BigDecimal("100.00"))
                .build();

            ServiceItemResponse responseWithActive = ServiceItemResponse.builder()
                .id(2L)
                .merchantId(1L)
                .merchantBusinessName("Test Salon")
                .name("Consultation")
                .description("Initial consultation")
                .category(ServiceCategory.CONSULTATION)
                .categoryDisplayName("Consulting Services")
                .duration(60)
                .price(new BigDecimal("100.00"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            when(serviceItemService.createServiceItem(any(ServiceItemRequest.class)))
                .thenReturn(responseWithActive);

            // When/Then
            mockMvc.perform(post("/api/merchants/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestWithoutActive)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/services")
    class GetAllServiceItemsEndpointTests {

        @Test
        @DisplayName("should return all service items for merchant")
        void getAllServiceItems_whenItemsExist_shouldReturnList() throws Exception {
            // Given
            ServiceItemResponse service2 = ServiceItemResponse.builder()
                .id(2L)
                .merchantId(1L)
                .merchantBusinessName("Test Salon")
                .name("Hair Coloring")
                .description("Professional hair coloring")
                .category(ServiceCategory.BEAUTY)
                .categoryDisplayName("Beauty & Wellness")
                .duration(90)
                .price(new BigDecimal("75.00"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            List<ServiceItemResponse> services = Arrays.asList(validResponse, service2);
            when(serviceItemService.getAllServiceItems()).thenReturn(services);

            // When/Then
            mockMvc.perform(get("/api/merchants/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Haircut"))
                .andExpect(jsonPath("$[1].name").value("Hair Coloring"));
        }

        @Test
        @DisplayName("should return empty list when no service items")
        void getAllServiceItems_whenNoItems_shouldReturnEmptyList() throws Exception {
            // Given
            when(serviceItemService.getAllServiceItems()).thenReturn(Collections.emptyList());

            // When/Then
            mockMvc.perform(get("/api/merchants/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/services/{id}")
    class GetServiceItemByIdEndpointTests {

        @Test
        @DisplayName("should return service item when found")
        void getServiceItemById_whenFound_shouldReturn200() throws Exception {
            // Given
            when(serviceItemService.getServiceItemById(1L))
                .thenReturn(Optional.of(validResponse));

            // When/Then
            mockMvc.perform(get("/api/merchants/services/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Haircut"))
                .andExpect(jsonPath("$.category").value("BEAUTY"));
        }

        @Test
        @DisplayName("should return 404 when service item not found")
        void getServiceItemById_whenNotFound_shouldReturn404() throws Exception {
            // Given
            when(serviceItemService.getServiceItemById(999L))
                .thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/merchants/services/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/merchants/services/{id}")
    class UpdateServiceItemEndpointTests {

        @Test
        @DisplayName("should update service item successfully and return 200")
        void updateServiceItem_withValidRequest_shouldReturn200() throws Exception {
            // Given
            ServiceItemRequest updateRequest = ServiceItemRequest.builder()
                .name("Premium Haircut")
                .description("Premium haircut with styling")
                .category(ServiceCategory.BEAUTY)
                .duration(45)
                .price(new BigDecimal("35.00"))
                .active(true)
                .build();

            ServiceItemResponse updatedResponse = ServiceItemResponse.builder()
                .id(1L)
                .merchantId(1L)
                .merchantBusinessName("Test Salon")
                .name("Premium Haircut")
                .description("Premium haircut with styling")
                .category(ServiceCategory.BEAUTY)
                .categoryDisplayName("Beauty & Wellness")
                .duration(45)
                .price(new BigDecimal("35.00"))
                .active(true)
                .createdAt(validResponse.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

            when(serviceItemService.updateServiceItem(eq(1L), any(ServiceItemRequest.class)))
                .thenReturn(updatedResponse);

            // When/Then
            mockMvc.perform(put("/api/merchants/services/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Premium Haircut"))
                .andExpect(jsonPath("$.description").value("Premium haircut with styling"))
                .andExpect(jsonPath("$.duration").value(45))
                .andExpect(jsonPath("$.price").value(35.00));

            verify(serviceItemService).updateServiceItem(eq(1L), any(ServiceItemRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/merchants/services/{id}")
    class DeleteServiceItemEndpointTests {

        @Test
        @DisplayName("should delete service item successfully and return 204")
        void deleteServiceItem_shouldReturn204() throws Exception {
            // When/Then
            mockMvc.perform(delete("/api/merchants/services/1"))
                .andExpect(status().isNoContent());

            verify(serviceItemService).deleteServiceItem(1L);
        }
    }

    @Nested
    @DisplayName("POST /api/merchants/services/{id}/reactivate")
    class ReactivateServiceItemEndpointTests {

        @Test
        @DisplayName("should reactivate service item successfully")
        void reactivateServiceItem_shouldReturn200() throws Exception {
            // Given
            ServiceItemResponse reactivatedResponse = ServiceItemResponse.builder()
                .id(1L)
                .merchantId(1L)
                .merchantBusinessName("Test Salon")
                .name("Haircut")
                .description("Professional haircut service")
                .category(ServiceCategory.BEAUTY)
                .categoryDisplayName("Beauty & Wellness")
                .duration(30)
                .price(new BigDecimal("25.00"))
                .active(true)
                .createdAt(validResponse.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

            when(serviceItemService.reactivateServiceItem(1L))
                .thenReturn(reactivatedResponse);

            // When/Then
            mockMvc.perform(post("/api/merchants/services/1/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(true));

            verify(serviceItemService).reactivateServiceItem(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/services/active")
    class GetActiveServiceItemsEndpointTests {

        @Test
        @DisplayName("should return only active service items")
        void getActiveServiceItems_shouldReturnActiveOnly() throws Exception {
            // Given
            List<ServiceItemResponse> activeServices = List.of(validResponse);
            when(serviceItemService.getActiveServiceItems()).thenReturn(activeServices);

            // When/Then
            mockMvc.perform(get("/api/merchants/services/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/services/inactive")
    class GetInactiveServiceItemsEndpointTests {

        @Test
        @DisplayName("should return only inactive service items")
        void getInactiveServiceItems_shouldReturnInactiveOnly() throws Exception {
            // Given
            ServiceItemResponse inactiveService = ServiceItemResponse.builder()
                .id(2L)
                .merchantId(1L)
                .merchantBusinessName("Test Salon")
                .name("Old Service")
                .description("Discontinued service")
                .category(ServiceCategory.GENERAL)
                .categoryDisplayName("General Services")
                .duration(30)
                .price(new BigDecimal("10.00"))
                .active(false)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();

            List<ServiceItemResponse> inactiveServices = List.of(inactiveService);
            when(serviceItemService.getInactiveServiceItems()).thenReturn(inactiveServices);

            // When/Then
            mockMvc.perform(get("/api/merchants/services/inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/services/count")
    class CountServiceItemsEndpointTests {

        @Test
        @DisplayName("should return total count when activeOnly is false")
        void countServiceItems_totalCount_shouldReturnCount() throws Exception {
            // Given
            when(serviceItemService.countAllServiceItems()).thenReturn(5L);

            // When/Then
            mockMvc.perform(get("/api/merchants/services/count")
                    .param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

            verify(serviceItemService).countAllServiceItems();
        }

        @Test
        @DisplayName("should return active count when activeOnly is true")
        void countServiceItems_activeOnly_shouldReturnActiveCount() throws Exception {
            // Given
            when(serviceItemService.countActiveServiceItems()).thenReturn(3L);

            // When/Then
            mockMvc.perform(get("/api/merchants/services/count")
                    .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));

            verify(serviceItemService).countActiveServiceItems();
        }

        @Test
        @DisplayName("should default to total count when activeOnly not specified")
        void countServiceItems_default_shouldReturnTotalCount() throws Exception {
            // Given
            when(serviceItemService.countAllServiceItems()).thenReturn(5L);

            // When/Then
            mockMvc.perform(get("/api/merchants/services/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

            verify(serviceItemService).countAllServiceItems();
        }
    }

    @Nested
    @DisplayName("GET /api/merchants/services/exists")
    class CheckNameExistsEndpointTests {

        @Test
        @DisplayName("should return true when name exists")
        void checkNameExists_whenExists_shouldReturnTrue() throws Exception {
            // Given
            when(serviceItemService.existsByName("Haircut")).thenReturn(true);

            // When/Then
            mockMvc.perform(get("/api/merchants/services/exists")
                    .param("name", "Haircut"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("should return false when name does not exist")
        void checkNameExists_whenNotExists_shouldReturnFalse() throws Exception {
            // Given
            when(serviceItemService.existsByName("New Service")).thenReturn(false);

            // When/Then
            mockMvc.perform(get("/api/merchants/services/exists")
                    .param("name", "New Service"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
        }
    }
}
