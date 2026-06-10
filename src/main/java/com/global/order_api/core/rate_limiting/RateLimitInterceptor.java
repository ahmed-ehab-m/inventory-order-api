package com.global.order_api.core.rate_limiting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.order_api.core.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


import java.util.concurrent.TimeUnit;

@Component
/// HandlerInterceptor => interface to intercept any request before controller
/// preHandle => run before controller layer
/// postHandle => run after controller finish his work to edit returned data
/// afterCompletion => run after response already go to user
public class RateLimitInterceptor implements HandlerInterceptor {

    /// template = Design Pattern to avoid complex code
    /// to call redis we should open connection + try catch + close connection
    /// String => to make data readable in redis without encryption or symbols
    /// because RedisTemplate without string => that operate with objects and make serialization
    /// to binary
    @Autowired
    private StringRedisTemplate redisTemplate;
    /// settings
    private final int MAX_REQUESTS = 10;
    private final int WINDOW_SECONDS = 60;

    @Autowired /// TO convert our api response error model to json
    /// because we are in interceptor layer
    private ObjectMapper objectMapper;

    /// handler => the final route that request will go to (method in controller)
    /// to control if any endpoint i want to remove rate limiting on it
    @Override
    public boolean preHandle(HttpServletRequest request , HttpServletResponse response, Object hander)
            throws Exception
    {
        /// 1=> get user IP Address
        String clientIp= request.getRemoteAddr();
        /// 2=> create redis key
        String redisKey= "rate_limit:" + clientIp;
        /// 3=> increase redis counter +1
        Long requestCount = redisTemplate.opsForValue().increment(redisKey);
        /// 4=> check if this first request
        if(requestCount !=null && requestCount ==1)
        {
            redisTemplate.expire(redisKey,WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        /// 5=> if user get limit 10 requests
        if(requestCount !=null && requestCount > MAX_REQUESTS)
        {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); /// 429
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            /// call our base response
            ApiResponse errorResponse =  ApiResponse.error("error.tooMany.request");
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(
                    jsonResponse);
            return false; /// stop request
        }
        return true; /// go to controller
    }
}
