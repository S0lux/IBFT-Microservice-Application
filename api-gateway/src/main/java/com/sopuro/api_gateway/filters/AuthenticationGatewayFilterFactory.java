package com.sopuro.api_gateway.filters;

import com.sopuro.api_gateway.configs.AuthenticationFilterConfig;
import com.sopuro.api_gateway.clients.AuthServiceClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationFilterConfig> {

    private final AuthServiceClient authServiceClient;

    public AuthenticationGatewayFilterFactory(@Lazy AuthServiceClient authServiceClient) {
        super(AuthenticationFilterConfig.class);
        this.authServiceClient = authServiceClient;
    }

    @Override
    public GatewayFilter apply(AuthenticationFilterConfig config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            String bearerToken = authorizationHeader.substring(7);

            return authServiceClient.authenticate("Bearer " + bearerToken)
                    .flatMap(userDetails -> {
                        ServerHttpRequest authenticatedRequest = request.mutate()
                                .header("x-user-id", userDetails.getId().toString())
                                .header("x-user-status", userDetails.getStatus().name())
                                .build();
                        return chain.filter(exchange.mutate().request(authenticatedRequest).build());
                    })
                    .onErrorResume(AuthServiceClient.UnauthorizedException.class, e -> {
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return response.setComplete();
                    })
                    .onErrorResume(AuthServiceClient.ForbiddenException.class, e -> {
                        response.setStatusCode(HttpStatus.FORBIDDEN);
                        return response.setComplete();
                    })
                    .onErrorResume(Throwable.class, e -> {
                        System.err.println("Error during authentication: " + e.getMessage());
                        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return response.setComplete();
                    });
        };
    }
}
