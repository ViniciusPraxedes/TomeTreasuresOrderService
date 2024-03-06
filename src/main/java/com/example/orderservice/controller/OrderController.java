package com.example.orderservice.controller;


import com.example.orderservice.DTO.OrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.service.OrderService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.param.checkout.SessionCreateParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    @Operation(summary = "Place order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, one or more parameters in the request is bad formatted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authorization required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "The code is broken", content = @Content)})
    @PostMapping()
    public Order placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
        return orderService.createOrder(orderRequest);
    }
    @Operation(summary = "Get all orders by user email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Bad request, one or more parameters in the request is bad formatted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authorization required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "The code is broken", content = @Content)})
    @GetMapping("/{userEmail}")
    public List<Order> getAllOrdersByUserEmail(@PathVariable String userEmail){
        return orderService.getAllOrdersByUserEmail(userEmail);
    }
    @Operation(summary = "Get all orders (you have to be an admin to call this request)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Bad request, one or more parameters in the request is bad formatted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authorization required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "The code is broken", content = @Content)})
    @GetMapping("/all")
    public List<Order> getAll(){
        return orderService.getAllOrders();
    }





    @PostMapping("/create-checkout-session")
    @ResponseBody
    public RedirectView createCheckoutSession(@RequestParam String price) throws StripeException {

        price = price.replaceAll("[^\\d.]", "");

        //Parsing the price string into a double
        double priceDouble = Double.parseDouble(price);

        //Converting the double price to an integer (if needed)
        int priceInt = (int) priceDouble;

        Stripe.apiKey = "sk_test_51OpHAyGxYKlKx6NCj9wGlKMD9YB7UiKnUGbCesS6H95Oie1uqu3zXtzcT0khJt5Bet62oBM1Xod9Cv4Zu07G2q7j005guGHl52";

        Map<String, Object> priceParams = new HashMap<>();
        priceParams.put("unit_amount", priceInt*100);
        priceParams.put("currency", "usd");
        priceParams.put("product_data", Collections.singletonMap("name", "Tome Treasures"));

        // Create the price
        Price priceObject = Price.create(priceParams);

        SessionCreateParams.LineItem item = SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPrice(priceObject.getId())
                .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setInvoiceCreation(
                        SessionCreateParams.InvoiceCreation.builder().setEnabled(true).build()

                )
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://tome-treasures.onrender.com/success")
                .setCancelUrl("https://tome-treasures.onrender.com/cancel")
                .addLineItem(item)
                .putExtraParam("billing_address_collection", "required")
                .build();

        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);




        return new RedirectView(session.getUrl(), true);
    }















}




