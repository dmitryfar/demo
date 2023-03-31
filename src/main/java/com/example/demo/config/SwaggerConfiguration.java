package com.example.demo.config;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * @see https://springdoc.org/#migrating-from-springfox
 * @see https://www.flowable.com/blog/the-road-to-spring-boot-2-0
 *  
 * The Flowable REST API is not registered as part of the root context.
 * @see https://forum.flowable.org/t/flowable-and-swagger/2177
 * 
 * Adding @ComponentScan(basePackages = "org.flowable.rest.service.api") would
 * add the Flowable REST Controllers again in the default Servlet. Which means
 * you will have twice the same Rest Controllers under different paths.
 * @see https://github.com/flowable/flowable-engine/issues/1114
 */
@Configuration
public class SwaggerConfiguration {

    @Bean
    public GroupedOpenApi demoApi() {
        return GroupedOpenApi.builder()
                .group("demo-api")
                .pathsToMatch("/demo/**")
                .build();
    }

    @Bean
    public GroupedOpenApi pmApi() {
        return GroupedOpenApi.builder()
                .group("pm-api")
                .pathsToMatch("/pm/**")
                .build();
    }

    @Bean
    public OpenAPI demoAppOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Demo App API")
                        .description("Demo Application")
                        .version("v0.0.1")
                        .license(new License().
                                name("Apache 2.0")
                                .url("https://github.com/dmitryfar/demo")
                        )
                )
        ;
    }
}
