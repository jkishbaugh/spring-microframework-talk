package com.example.demo;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.env.Profiles;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.fu.jafu.Jafu;
import org.springframework.fu.jafu.JafuApplication;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.fu.jafu.r2dbc.PostgresqlR2dbcDsl.r2dbcPostgresql;
import static org.springframework.fu.jafu.webflux.WebFluxServerDsl.webFlux;

public class DemoApplication {

    public static JafuApplication buildApp(int port) {
        var translationService = new TranslationService();

        return Jafu.reactiveWebApplication(app -> app
                .beans(beans -> beans.bean(TranslationService.class, () -> translationService))
                .enable(r2dbcPostgresql(db -> {
                    String url = db.env().getProperty("spring.r2dbc.url");
                    ConnectionFactoryOptions options = ConnectionFactoryOptions.parse(url);
                    db.host(options.getValue(Option.valueOf("host")))
                            .database(options.getValue(Option.valueOf("database")))
                            .username(db.env().getProperty("spring.r2dbc.username"))
                            .password(db.env().getProperty("spring.r2dbc.password"));
                }))
                .enable(webFlux(web -> web
                        .port(port)
                        .router(router -> {
                            router.GET("/book", request -> {
                                var lang = request.queryParam("lang").orElse("");
                                var client = app.ref(DatabaseClient.class);
                                var service = app.ref(TranslationService.class);
                                var translatedBooks = client.execute("select * from book").as(Book.class)
                                        .fetch().all()
                                        .map(book -> new Book(
                                                book.getId(),
                                                service.translateTitle(lang, book.getTitle())
                                        ));

                                return ServerResponse.ok().body(translatedBooks, Book.class);
                            });
                        }).codecs(codes -> codes.jackson().string()))));
    }

    public static void main(String[] args) {
        buildApp(8080).run(args);
    }

}
