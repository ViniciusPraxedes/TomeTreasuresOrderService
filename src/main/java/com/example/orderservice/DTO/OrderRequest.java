package com.example.orderservice.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String orderNumber;

    @NotBlank(message = "First name is required")
    private String firstname;

    @NotBlank(message = "Last name is required")
    private String lastname;

    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Postcode is required")
    @Min(value = 10000, message = "Postcode must be at least 10000")
    @Max(value = 99999, message = "Postcode must be at most 99999")
    private Integer postcode;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemDTO> orderItemsDTO;
}
