package com.shopApplication.OrderService.service;

import com.shopApplication.OrderService.dao.OrderRequest;
import com.shopApplication.OrderService.dao.OrderResponse;

public interface OrderService {
    long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(long orderId);
}
