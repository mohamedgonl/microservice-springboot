package com.gonl.orderservice.service;

import com.gonl.orderservice.dto.InventoryResponse;
import com.gonl.orderservice.dto.OrderLineItemDto;
import com.gonl.orderservice.dto.OrderRequest;
import com.gonl.orderservice.model.Order;
import com.gonl.orderservice.model.OrderLineItems;
import com.gonl.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient webClient;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemDtoList().stream().map(this::mapToDto).toList();
        order.setOrderLineItemList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemList().stream().map(OrderLineItems::getSkuCode).toList();
        // Call Inventory service and place order if product is in stock
        InventoryResponse[] inventoryResponses = webClient.get().uri("http://localhost:8082/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock =  Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

        if (allProductsInStock) {
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is not in stock");
        }


    }

    private OrderLineItems mapToDto(OrderLineItemDto orderLineItemDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemDto.getPrice());
        orderLineItems.setQuantity(orderLineItemDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemDto.getSkuCode());
        return orderLineItems;
    }
}
