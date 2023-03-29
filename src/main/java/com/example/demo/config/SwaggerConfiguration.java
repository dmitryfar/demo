package com.example.demo.config;

import org.flowable.spring.boot.process.FlowableProcessProperties;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
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
@ComponentScan(basePackages = "org.flowable.rest.service.api")
public class SwaggerConfiguration {

//    @Autowired
//    protected ObjectMapper objectMapper;
//    
//    @Bean()
//    public RestResponseFactory restResponseFactory() {
//        RestResponseFactory restResponseFactory = new RestResponseFactory(objectMapper);
//        return restResponseFactory;
//    }
    @Autowired
    private FlowableProcessProperties flowableProcessProperties;

    @Bean
    public GroupedOpenApi demoApi() {
        return GroupedOpenApi.builder().group("demo-api").pathsToMatch("/demo/**").build();
    }

    @Bean
    public GroupedOpenApi pmApi() {
        String packagesToscan[] = { "com.example.demo.api" };
        return GroupedOpenApi.builder().group("pm-api")
//                .packagesToScan(packagesToscan)
                .pathsToMatch("/pm/**").build();
    }

    @Bean
    public GroupedOpenApi processApi() {
//        String packagesToscan[] = {"org.flowable.rest.service.api"};
        String packagesToscan[] = { "org.flowable.rest.service.api" };
        return GroupedOpenApi.builder().group("process-api").packagesToScan(packagesToscan)
//                .pathsToMatch("/pm/**")
//                .pathsToMatch("/process-api/**")
                .build();
    }

    @Bean
    public OpenAPI demoAppOpenAPI() {
        return new OpenAPI().info(new Info().title("Demo App API").description("Demo Application").version("v0.0.1")
                .license(new License().name("Apache 2.0").url("http://example.com")))
//                .externalDocs(new ExternalDocumentation()
//                .description("SpringShop Wiki Documentation")
//                .url("https://springshop.wiki.github.org/docs")
//                )
        ;
    }
}
