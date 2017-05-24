package com.ai.southernquiet.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@SpringBootApplication(scanBasePackages = {"com.ai.southernquiet"})
@EnableConfigurationProperties({CommonWebProperties.class})
public class WebTest {
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(WebTest.class);

        Map<String, Object> defaultProperties = new HashMap<>();
        defaultProperties.put("management.security.enabled", false);
        app.setDefaultProperties(defaultProperties);

        app.run();
    }

    @Bean
    static ServletContextInitializer servletContextInitializer() {
        return new WebInit() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {

                super.onStartup(servletContext);
            }
        };
    }

    @Bean
    static JettyEmbeddedServletContainerFactory servletContainerFactory(JettyConfiguration jettyConfiguration) {
        JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
        factory.addConfigurations(jettyConfiguration);
        return factory;
    }

    private Logger logger = LoggerFactory.getLogger(WebTest.class);

    @RequestMapping("/")
    String home(HttpSession session) {
        logger.info("你好，Spring Boot！");
        return "Hello World!";
    }
}
