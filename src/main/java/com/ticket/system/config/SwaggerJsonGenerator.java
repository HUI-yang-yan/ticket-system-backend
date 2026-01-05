package com.ticket.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Configuration
@Profile("!test")
public class SwaggerJsonGenerator {

    @Bean
    public ApplicationRunner generateSwaggerJson() {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) throws Exception {
                String url = "http://localhost:8080/api/v3/api-docs"; // springdoc 默认接口
                RestTemplate restTemplate = new RestTemplate();
                Object swaggerJson = restTemplate.getForObject(url, Object.class);

                ObjectMapper mapper = new ObjectMapper();
                File file = new File("swagger.json");
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, swaggerJson);

                System.out.println("完整 swagger.json 已生成: " + file.getAbsolutePath());
            }
        };
    }
}
