package com.shopApplication.OrderService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handledOrderServiceException(CustomException exception){
        return new ResponseEntity<>(
                ErrorResponse.builder()
                        .errorMessage(exception.getMessage())
                        .errorCode(exception.getErrorCode()).build(), HttpStatus.valueOf(exception.getStatus())
        );
    }
}
