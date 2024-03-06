package com.example.orderservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String orderNumber;
    private String firstname;
    private String lastname;
    private String email;
    private String address;
    private String city;
    private Integer postcode;
    private String phoneNumber;
    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

}
