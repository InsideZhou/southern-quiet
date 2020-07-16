package me.insidezhou.southernquiet.file.web;

import me.insidezhou.southernquiet.Constants;
import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.WebFilter;

@SuppressWarnings("SpringFacetCodeInspection")
@Configuration
@EnableAsync
@EnableWebFlux
@ComponentScan
@EnableConfigurationProperties
@AutoConfigureOrder(Constants.AutoConfigLevel_Highest)
public class FileWebFluxAutoConfiguration {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean
    public WebFilter fileWebFilter(ServerProperties serverProperties) {
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

    @Bean
    @ConfigurationProperties(FrameworkAutoConfiguration.ConfigRoot + ".file-web")
    public Properties properties() {
        return new Properties();
    }

    public static class Properties {
        /**
         * IO的buffer。
         */
        private DataSize BufferSize = DataSize.ofKilobytes(64);

        public DataSize getBufferSize() {
            return BufferSize;
        }

        public void setBufferSize(DataSize bufferSize) {
            BufferSize = bufferSize;
        }
    }
}
