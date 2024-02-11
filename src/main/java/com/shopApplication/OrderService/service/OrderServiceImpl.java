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
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
public class OrderServiceImpl implements OrderService{
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RestTemplate restTemplate;
    @Override
    public long placeOrder(OrderRequest orderRequest) {

        //Order Entity -> Save the data with status order created
        //Product Service -> Block Products (Reduce the quantity)
        //Payment Service -> Payments -> Success -> Complete, Else
        //Cancelled
        log.info("Place order request: {}"+orderRequest);
        ResponseEntity<ProductResponse> productById = productService.getProductById(orderRequest.getProductId());
        ProductResponse productResponse = productById.getBody();
        productService.reduceQuantity(orderRequest.getProductId(),orderRequest.getQuantity());
        log.info("Creating Order with status CREATED");
        Order order= Order.builder()
                .orderStatus("CREATED")
                .orderDate(Instant.now())
                .productId(orderRequest.getProductId())
                .amount(productResponse.getPrice()*orderRequest.getQuantity())
                .quantity(orderRequest.getQuantity())
                .build();
        order=orderRepository.save(order);

        log.info("Calling payment service to complete the payment..........");
        PaymentRequest paymentRequest=PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(productResponse.getPrice()*productResponse.getQuantity())
                .build();
        String orderStatus=null;
        try {
            paymentService.doPayment(paymentRequest);
            log.info("Payment done successfully. change the order status PLACED");
            orderStatus="PLACED";
        }catch (Exception e){
            log.error("Error occured in payment. Changing order status to PAYMENT_FAILED");
            orderStatus="PAYMENT_FAILED";
        }
        order.setOrderStatus(orderStatus);
        orderRepository.save(order);
        log.info("Order Placed successfully with Order id: {}"+order.getId());
        return order.getId();
    }

    @Override
    public OrderResponse getOrderDetails(long orderId) {
        log.info("Get order details for order id:{}"+orderId);
        Order order=orderRepository.findById(orderId).orElseThrow(()-> new CustomException("Order not found with id :{}"+orderId,
                "NOT_FOUND",404));

        log.info("Invoking Product service to fetch the product for:{}"+order.getProductId());
        ProductResponse productResponse
                =restTemplate.getForObject(
                        "http://PRODUCT-SERVICE/product/"+order.getProductId(), ProductResponse.class
        );
        OrderResponse.ProductDetails productDetails
                =OrderResponse.ProductDetails.builder()
                .productName(productResponse.getProductName())
                .quantity(productResponse.getQuantity())
                .productId(productResponse.getProductId())
                .price(productResponse.getPrice())
                .build();
        OrderResponse.PaymentResponse paymentResponse1 = null;
        if(order.getOrderStatus().equalsIgnoreCase("PLACED")) {
            log.info("Getting payment information from the payment Service");
            PaymentResponse paymentResponse = restTemplate.getForObject(
                    "http://PAYMENT-SERVICE/payment/order/" + order.getId(), PaymentResponse.class
            );
            paymentResponse1
                    = OrderResponse.PaymentResponse.builder()
                    .paymentId(paymentResponse.getPaymentId())
                    .paymentDate(paymentResponse.getPaymentDate())
                    .status(paymentResponse.getStatus())
                    .paymentMode(paymentResponse.getPaymentMode())
                    .orderId(paymentResponse.getOrderId())
                    .amount(paymentResponse.getAmount())
                    .build();
        }else if(order.getOrderStatus().equalsIgnoreCase("CREATED")){
            paymentResponse1=
                    OrderResponse.PaymentResponse.builder()
                            .status(order.getOrderStatus())
                            .amount(-1)
                            .orderId(order.getId())
                            .paymentId(-1)
                            .build();
        }
        OrderResponse orderResponse=OrderResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .orderDate(order.getOrderDate())
                .quantity(order.getQuantity())
                .productDetails(productDetails)
                .paymentResponse(paymentResponse1)
                .build();
        return orderResponse;
    }
}
