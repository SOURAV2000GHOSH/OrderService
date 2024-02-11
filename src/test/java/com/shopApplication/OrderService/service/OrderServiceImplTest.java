package com.shopApplication.OrderService.service;

import com.shopApplication.OrderService.dao.OrderRequest;
import com.shopApplication.OrderService.dao.OrderResponse;
import com.shopApplication.OrderService.entity.Order;
import com.shopApplication.OrderService.exception.CustomException;
import com.shopApplication.OrderService.external.client.PaymentService;
import com.shopApplication.OrderService.external.client.ProductService;
import com.shopApplication.OrderService.external.request.PaymentRequest;
import com.shopApplication.OrderService.external.response.PaymentResponse;
import com.shopApplication.OrderService.external.response.ProductResponse;
import com.shopApplication.OrderService.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductService productService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    OrderService orderService=new OrderServiceImpl();

    @DisplayName("Get Order - Success Scenario")
    @Test
    void test_When_Order_Success(){
    Order order=getMockOrder();

    //Mocking
    when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
    when(restTemplate.getForObject(
            "http://PRODUCT-SERVICE/product/"+order.getProductId(), ProductResponse.class
    )).thenReturn(getMockProductResponseBack());
    when(restTemplate.getForObject(
            "http://PAYMENT-SERVICE/payment/order/" + order.getId(), PaymentResponse.class
    )).thenReturn(getMockPaymentResponseBack());

    //Actual
    OrderResponse orderResponse = orderService.getOrderDetails(1);

    //Verification
        verify(orderRepository, times(1)).findById(anyLong());
        verify(restTemplate, times(1))
                .getForObject("http://PRODUCT-SERVICE/product/"+order.getProductId(), ProductResponse.class);
        verify(restTemplate, times(1))
                .getForObject("http://PAYMENT-SERVICE/payment/order/" + order.getId(), PaymentResponse.class);

        //Assert
        Assertions.assertNotNull(orderResponse);
        Assertions.assertEquals(order.getId(),orderResponse.getOrderId());


    }
    @DisplayName("Get Order - Unsuccessful Scenario")
    @Test
    void test_When_Get_Order_NOT_FOUND_then_Not_Found(){
        Order order=getMockOrder();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.ofNullable(null));
        //calling the method
        CustomException exception=Assertions.assertThrows(CustomException.class,()->orderService.getOrderDetails(1));
        Assertions.assertEquals("NOT_FOUND",exception.getErrorCode());
        Assertions.assertEquals(404,exception.getStatus());
    }
    @DisplayName("Place Order - Success Scenario")
    @Test
    void test_When_Place_Order_Success(){
        Order order=getMockOrder();
        OrderRequest orderRequest= getMockOrderRequest();
        when(productService.getProductById(anyLong())).thenReturn(ResponseEntity.of(Optional.of(getMockProductResponseBack())));
        when(productService.reduceQuantity(anyLong(), anyLong()))
                .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
        when(paymentService.doPayment(any(PaymentRequest.class))).thenReturn(new ResponseEntity<Long>(1l,HttpStatus.OK));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        //call method
        long orderId=orderService.placeOrder(orderRequest);
        //assertions
        verify(orderRepository, times(2)).save(any());
        verify(productService, times(1)).getProductById(anyLong());
        verify(productService, times(1)).reduceQuantity(anyLong(), anyLong());
        verify(paymentService, times(1)).doPayment(any());
        Assertions.assertEquals(order.getId(),orderId);

    }
    @DisplayName("Placed Order - Payment Failed Scenario")
    @Test
    void test_When_Place_Order_Payment_Fails_then_Order_Placed(){
        Order order=getMockOrder();
        OrderRequest orderRequest= getMockOrderRequest();
        when(productService.getProductById(anyLong())).thenReturn(ResponseEntity.of(Optional.of(getMockProductResponseBack())));
        when(productService.reduceQuantity(anyLong(), anyLong()))
                .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
        when(paymentService.doPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        long orderId=orderService.placeOrder(orderRequest);

        verify(orderRepository, times(2)).save(any());
        verify(productService, times(1)).getProductById(anyLong());
        verify(productService, times(1)).reduceQuantity(anyLong(), anyLong());
        Assertions.assertEquals(order.getId(),orderId);
    }
    private PaymentRequest getMockPaymentRequest() {
        return PaymentRequest.builder()
                .orderId(1)
                .amount(100)
                .paymentMode(com.shopApplication.OrderService.external.request.PaymentMode.CASH)
                .build();
    }

    private OrderRequest getMockOrderRequest() {
        return OrderRequest.builder()
                .productId(2)
                .quantity(100)
                .paymentMode(com.shopApplication.OrderService.external.request.PaymentMode.CASH)
                .build();
    }

    private PaymentResponse getMockPaymentResponseBack() {
        return PaymentResponse.builder()
                .orderId(1)
                .paymentId(1)
                .paymentDate(Instant.now())
                .paymentMode(com.shopApplication.OrderService.dao.PaymentMode.CASH)
                .amount(200)
                .orderId(1)
                .status("ACCEPTED")
                .build();
    }

    private ProductResponse getMockProductResponseBack() {
    return ProductResponse.builder()
            .productId(2)
            .price(100)
            .productName("iPhone")
            .quantity(200)
            .build();
    }

    private Order getMockOrder() {
    return Order.builder()
            .orderStatus("PLACED")
            .orderDate(Instant.now())
            .id(1)
            .amount(100)
            .quantity(200)
            .productId(2)
            .build();
    }
}