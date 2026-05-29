package com.testround.seven_eleven.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testround.seven_eleven.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return true;
        }

        String ip = request.getRemoteAddr();
        String key = ip + ":" + handlerMethod.getMethod().toString();

        Bucket bucket = cache.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(rateLimit.requests(),
                        Refill.greedy(rateLimit.requests(), Duration.ofSeconds(rateLimit.perSeconds()))))
                .build());

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            sendErrorResponse(response);
            return false;
        }
    }

    private void sendErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> apiResponse = ApiResponse.error("TOO_MANY_REQUESTS", "Quá nhiều yêu cầu, thử lại sau", 429);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
