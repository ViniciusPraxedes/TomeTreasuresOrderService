package com.example.orderservice.DTO;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    @NotBlank(message = "Item code cannot be blank")
    private String itemCode;

    @NotBlank(message = "Product name cannot be blank")
    private String productName;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be a positive integer")
    private Integer quantity;
}
