package com.example.orderservice.DTO;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@Builder
public class OrderCreatedDTO {
    private String orderNumber;
    private String firstname;
    private String lastname;
    private String email;
    private String address;
    private String city;
    private Integer postcode;
    private String phoneNumber;
    private List<OrderItemDTO> orderItems;
}
