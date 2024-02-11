package com.shopApplication.OrderService.external.client;

import com.shopApplication.OrderService.exception.CustomException;
import com.shopApplication.OrderService.external.request.PaymentRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
@CircuitBreaker(name = "EXTERNAL_CIRCUIT_BREAKER", fallbackMethod = "fallback")
@FeignClient(name = "PAYMENT-SERVICE/payment")
public interface PaymentService {
    @PostMapping
    public ResponseEntity<Long> doPayment(@RequestBody PaymentRequest paymentRequest);

    default void fallback(Exception exception){
        throw new CustomException("Payment service is not available !","UNAVAILABLE",500);
    }
}
