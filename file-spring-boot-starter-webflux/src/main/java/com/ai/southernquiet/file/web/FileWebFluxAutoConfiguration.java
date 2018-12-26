package com.ai.southernquiet.file.web;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.WebFilter;

@SuppressWarnings("SpringFacetCodeInspection")
@Configuration
@EnableAsync
@EnableWebFlux
@ComponentScan
public class FileWebFluxAutoConfiguration {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public WebFilter contextPathWebFilter(ServerProperties serverProperties) {
        String contextPath = serverProperties.getServlet().getContextPath();
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (request.getURI().getPath().startsWith(contextPath)) {
                return chain.filter(
                    exchange.mutate()
                        .request(request.mutate().contextPath(contextPath).build())
                        .build());
            }

            return chain.filter(exchange);
        };
    }
}
