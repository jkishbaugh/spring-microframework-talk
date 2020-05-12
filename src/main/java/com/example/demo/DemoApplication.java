package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class DemoApplication {

    private final static Class<?>[] autoConfigurationClasses = {ReactiveWebServerFactoryAutoConfiguration.class,
            // web
            HttpHandlerAutoConfiguration.class,
            WebFluxAutoConfiguration.class,
            ErrorWebFluxAutoConfiguration.class,

            // data
            R2dbcAutoConfiguration.class,
            R2dbcDataAutoConfiguration.class};

    public static SpringApplication buildApp() {
        var translationService = new TranslationService();

        return new SpringApplicationBuilder(DemoApplication.class)
                .sources(autoConfigurationClasses)
                .initializers((GenericApplicationContext applicationContext) -> {
                    applicationContext.registerBean(RouterFunction.class, () -> {
                        var repo = applicationContext.getBean(DatabaseClient.class);
                        return route()
                                .GET("/book", request -> {
                                    var lang = request.queryParam("lang").orElse("");
                                    var translatedBooks = repo
                                            .execute("select * from book").as(Book.class)
                                            .fetch().all()
                                            .map(book -> new Book(
                                                    book.getId(),
                                                    translationService.translateTitle(lang, book.getTitle())
                                            ));

                                    return ServerResponse.ok().body(translatedBooks, Book.class);
                                })
                                .build();
                    });
                })
                .build();
    }

    public static void main(String[] args) {
        buildApp().run(args);
    }

}
