package com.example.demo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.SocketUtils;

import static com.example.demo.DemoApplication.buildApp;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.fu.jafu.r2dbc.H2R2dbcDsl.r2dbcH2;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoApplicationTests {

    private ConfigurableApplicationContext applicationContext;
    private WebTestClient webTestClient;

    @BeforeAll
    void setUp() {
        applicationContext = buildApp(SocketUtils.findAvailableTcpPort())
                .customize(app -> app.enable(r2dbcH2()))
                .run();

        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();

        DatabaseClient databaseClient = applicationContext.getBean(DatabaseClient.class);
        databaseClient.execute("create table if not exists book (id int auto_increment primary key, title varchar );").then()
                .then(databaseClient.execute("insert into book (title) values ('A year in Provence')").then())
                .then()
                .block();
    }

    @AfterAll
    void tearDown() {
        applicationContext.close();
    }

    @Test
    void shouldReturnListOfBooks() {
        webTestClient.get().uri("/book")
                .exchange()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].title").value(equalTo("A year in Provence"));
    }

    @Test
    void shouldTranslateTitle() {
        webTestClient.get().uri("/book?lang=fr")
                .exchange()
                .expectBody()
                .jsonPath("$[0].title").value(equalTo("Une année en Provence"));
    }

}
