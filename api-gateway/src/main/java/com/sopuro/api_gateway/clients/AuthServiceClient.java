package com.sopuro.api_gateway.clients;

import com.sopuro.api_gateway.dtos.UserDetailsDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AuthServiceClient {

    private final WebClient.Builder webClientBuilder;

    public AuthServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<UserDetailsDTO> authenticate(String authorizationHeader) {
        WebClient webClient = webClientBuilder.baseUrl("http://auth-service").build();

        return webClient.get()
                .uri("/v1/auth/me")
                .header("Authorization", authorizationHeader)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> response.bodyToMono(String.class)
                        .flatMap(body -> {
                            if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
                                return Mono.error(new UnauthorizedException("Authentication failed: " + body));
                            } else if (response.statusCode() == HttpStatus.FORBIDDEN) {
                                return Mono.error(new ForbiddenException("Access forbidden: " + body));
                            }
                            return Mono.error(new RuntimeException("Auth service error (" + response.statusCode() + "): " + body));
                        }))
                .bodyToMono(UserDetailsDTO.class);
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }
}