package com.shopApplication.OrderService.controller;

import com.shopApplication.OrderService.dao.OrderRequest;
import com.shopApplication.OrderService.dao.OrderResponse;
import com.shopApplication.OrderService.service.OrderService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@Log4j2
public class OrderController {
    @Autowired
    public OrderService orderService;

    @PreAuthorize("hasAuthority('Customer')")
    @PostMapping("/placeOrder")
    public ResponseEntity<Long> placeOrder(@RequestBody OrderRequest orderRequest){
        long orderId=orderService.placeOrder(orderRequest);
        log.info("Order Id: {}",orderId);
        return new ResponseEntity<>(orderId, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('Customer') || hasAuthority('Admin')")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable("orderId") long orderId){
        OrderResponse orderResponse=orderService.getOrderDetails(orderId);
        return new ResponseEntity<>(orderResponse,HttpStatus.OK);
    }
}
