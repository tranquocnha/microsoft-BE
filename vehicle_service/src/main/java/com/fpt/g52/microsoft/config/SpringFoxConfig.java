package com.fpt.g52.microsoft.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@EnableSwagger2
public class SpringFoxConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
//                .apis(RequestHandlerSelectors.basePackage(basePackage))
                .paths(PathSelectors.any())
                .build()
                .directModelSubstitute(LocalDate.class, java.sql.Date.class)
                .directModelSubstitute(LocalDateTime.class, java.util.Date.class)
                .apiInfo(apiInfo());
    }

//    @Bean
//    public Docket produceApi() {
//        return new Docket(DocumentationType.SWAGGER_2).select()
//                .apis(RequestHandlerSelectors.basePackage("com.fpt.g52.microsoft.controller"))
//                .build();
//    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "StudentSearchApplication",
                "An application to search Student from a Student repository by studentId",
                "StudentSearchApplication v1",
                "Terms of service",
                "tranquocnha180800@gmail.com",
                "License of API",
                "https://swagger.io/docs/");
        return apiInfo;
    }
}