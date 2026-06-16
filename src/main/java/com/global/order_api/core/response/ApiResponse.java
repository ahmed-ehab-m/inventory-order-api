package com.global.order_api.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// give me setters ,getters ,to string , hashcode ,equals
@Data
// to apply builder design pattern
@Builder
// lombok require arg constructor when builder work to create object from data recieved
@AllArgsConstructor
// because when jackson try to send or recieve an data 
// firsr create empty object then add data into it using setters
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // tell jackson if any field = null remove it from response

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timeStamp;
    private List<String> errors;

    // success with default message
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message("Success")
                .timeStamp(LocalDateTime.now())
                .build();
    }

    // success with specific message
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timeStamp(LocalDateTime.now())
                .build();
    }

    // general error with single message
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timeStamp(LocalDateTime.now())
                .build();
    }

    // validation error so i may have list of errors
    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .timeStamp(LocalDateTime.now())
                .build();
    }
}
