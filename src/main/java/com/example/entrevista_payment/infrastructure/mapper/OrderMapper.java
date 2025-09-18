package com.example.entrevista_payment.infrastructure.mapper;

import com.example.entrevista_payment.domain.model.*;
import com.example.entrevista_payment.domain.model.valueobjects.CustomerId;
import com.example.entrevista_payment.domain.model.valueobjects.Money;
import com.example.entrevista_payment.domain.model.valueobjects.OrderId;
import com.example.entrevista_payment.domain.model.valueobjects.ProductId;
import com.example.entrevista_payment.infrastructure.adapter.out.persistence.entity.OrderDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public Order toDomain(OrderDto dto) {
        List<OrderItem> items = dto.getItems().stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());

        return new Order(
                new OrderId(dto.getOrderId()),
                new CustomerId(dto.getCustomerId()),
                items,
                new Money(dto.getTotalAmount())
        );
    }

    public OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setOrderId(order.getOrderId().getValue());
        dto.setCustomerId(order.getCustomerId().getValue());
        dto.setTotalAmount(order.getTotalAmount().getAmount().doubleValue());

        List<OrderDto.ItemDto> itemDtos = order.getItems().stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());
        dto.setItems(itemDtos);

        return dto;
    }

    private OrderItem toOrderItem(OrderDto.ItemDto dto) {
        return new OrderItem(
                new ProductId(dto.getProductId()),
                dto.getQuantity()
        );
    }

    private OrderDto.ItemDto toItemDto(OrderItem item) {
        OrderDto.ItemDto dto = new OrderDto.ItemDto();
        dto.setProductId(item.getProductId().getValue());
        dto.setQuantity(item.getQuantity());
        return dto;
    }
}