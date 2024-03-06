package com.example.orderservice.service;

import com.example.orderservice.DTO.*;
import com.example.orderservice.model.*;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient webClient;

    @Autowired
    public OrderService(OrderRepository orderRepository, WebClient.Builder webClientBuilder) {
        this.orderRepository = orderRepository;
        this.webClient = webClientBuilder.baseUrl("http://inventory-service.eu-north-1.elasticbeanstalk.com:8040/inventory").build();
    }

    public Order createOrder(OrderRequest request) {

        //Check if quantity of item is greater than zero and if itemCode is not null
        for (int i = 0; i < request.getOrderItemsDTO().size(); i ++){
            if (request.getOrderItemsDTO().get(i).getQuantity() <= 0){
                throw new IllegalStateException("Quantity of item: "+request.getOrderItemsDTO().get(i).getProductName()+" must be greater than 0");
            }

            if (request.getOrderItemsDTO().get(i).getItemCode().isEmpty()){
                throw new IllegalStateException("ItemCode can not be empty");
            }
        }


        //Generates random order number
        String orderNumber = generateRandomOrderNumber();
        request.setOrderNumber(orderNumber);

        //Although the possibility is low, this will check if the order number already exists
        if (orderRepository.findByOrderNumber(request.getOrderNumber()).isPresent()) {
            throw new IllegalStateException("Order number already existent");
        }

        //Creates a list of items that belongs to the order and set it to the items from the request
        List<OrderItem> orderItems = request.getOrderItemsDTO()
                .stream()
                .map(this::mapToDto)
                .toList();

        //Creates order and sets the products within the order to the products from the request
        Order order = Order.builder()
                .orderNumber(generateRandomOrderNumber())
                .orderItems(orderItems).build();

        //Gets the itemCode from the items within the order and put them into a list
        List<String> itemCodes = order.getOrderItems().stream().map(OrderItem::getItemCode).toList();

        //Creates an array to store the response from inventory service
        InventoryResponse[] inventoryResponses;

        //Asks the inventory if all items in the order are in the inventory
        try {

            inventoryResponses = webClient.get()
                    .uri("/isItemInStockManyItems", uriBuilder -> uriBuilder.queryParam("itemCodes", itemCodes).build())
                    .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

        } catch (WebClientRequestException e) {
            throw new IllegalStateException("Inventory service is down");
        } catch (WebClientResponseException e){
            throw new IllegalStateException("Item not found in the inventory, make sure ItemCode is correct");
        }


        //If all items in the order are in stock then it will return true
        boolean allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

        //Checks which items are out of stock
        List<InventoryResponse> filteredResponses = Arrays.asList(inventoryResponses)
                .stream()
                .filter(response -> response.getQuantity() <= 0)
                .collect(Collectors.toList());

        //Get the quantity of the items in the order, so that it can be reduced once the order has been placed
        List<Integer> quantity = order.getOrderItems().stream().map(OrderItem::getQuantity).toList();

        if (allProductsInStock) {

            order.setFirstname(request.getFirstname());
            order.setLastname(request.getLastname());
            order.setEmail(request.getEmail());
            order.setPhoneNumber(request.getPhoneNumber());
            order.setAddress(request.getAddress());
            order.setPostcode(request.getPostcode());
            order.setCity(request.getCity());

            //Saves the order to the database
            orderRepository.save(order);

            OrderCreatedDTO orderToBeSent = OrderCreatedDTO.builder()
                    .orderNumber(orderNumber)
                    .firstname(order.getFirstname())
                    .lastname(order.getLastname())
                    .email(order.getEmail())
                    .address(order.getAddress())
                    .city(order.getCity())
                    .postcode(order.getPostcode())
                    .phoneNumber(order.getPhoneNumber())
                    .orderItems(request.getOrderItemsDTO())
                    .build();

            //Send email


            try {
                webClient.post()
                        .uri("http://email-service.eu-north-1.elasticbeanstalk.com:8060/email/order")
                        .body(BodyInserters.fromValue(orderToBeSent))
                        .retrieve()
                        .bodyToMono(Void.class) // Assuming the response is void, adjust accordingly
                        .block();

            } catch (WebClientRequestException e) {
                throw new IllegalStateException("Email service is down");
            } catch (WebClientResponseException e){
                throw new IllegalStateException("Bad request when sending request to email service");
            }

            //Reduces the quantity of the items in the inventory once the order is placed
            try {
                webClient.post()
                        .uri("/decreaseQuantityManyItems", uriBuilder -> uriBuilder.queryParam("itemCodes", itemCodes).queryParam("quantities", quantity).build())
                        .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                        .retrieve()
                        .toBodilessEntity()
                        .block();

            } catch (WebClientRequestException e) {
               throw new IllegalStateException("Inventory service is down");
            } catch (WebClientResponseException e){
                throw new IllegalStateException("Item not found in the inventory/ iventory does not have enough of the item you are trying to buy/ Item has 0 units in the inventory");
            }

            return order;
        } else {
            throw new IllegalStateException("Items out of stock: " + filteredResponses.toString());
        }
    }


    //Sets each value of the item inside the list to the values of the dto
    //This function is needed in order for the createOrder() function to work properly
    private OrderItem mapToDto(OrderItemDTO orderItemDTO) {
        OrderItem orderItem = new OrderItem();
        orderItem.setPrice(orderItemDTO.getPrice());
        orderItem.setQuantity(orderItemDTO.getQuantity());
        orderItem.setItemCode(orderItemDTO.getItemCode());
        orderItem.setProductName(orderItemDTO.getProductName());
        return orderItem;
    }

    //Generates a random order number using letters and numbers
    //This method is needed so createOrder can function properly
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS = "0123456789";
    private static final Random random = new Random();
    public static String generateRandomOrderNumber() {
        StringBuilder randomString = new StringBuilder();

        String source = LETTERS + NUMBERS;

        for (int i = 0; i < 9; i++) {
            char randomChar = source.charAt(random.nextInt(source.length()));
            randomString.append(randomChar);
        }

        return randomString.toString();
    }


    public List<Order> getAllOrdersByUserEmail(String userEmail){
       return orderRepository.findAllByEmail(userEmail);
    }

    public List<Order> getAllOrders(){
        return orderRepository.findAll();
    }


}
