package com.aomap.gateway;

import com.aomap.common.auth.AuthToken;
import com.aomap.common.auth.AuthTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class AuthGatewayFilter implements GlobalFilter, Ordered {

    private final String jwtSecret;

    public AuthGatewayFilter(@Value("${auth.jwt-secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path) || "OPTIONS".equals(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }
        try {
            AuthToken token = AuthTokens.parse(jwtSecret, header.substring(7).trim());
            var request = exchange.getRequest().mutate()
                    .header("X-User-Id", Long.toString(token.userId()))
                    .header("X-Org-Id", Long.toString(token.orgId()))
                    .header("X-User-Role", token.role())
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (RuntimeException ex) {
            return unauthorized(exchange);
        }
    }

    private static boolean isPublic(String path) {
        return path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/register")
                || path.startsWith("/api/v1/shares/")
                || path.startsWith("/actuator");
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"code\":1,\"message\":\"请先登录\"}".getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
