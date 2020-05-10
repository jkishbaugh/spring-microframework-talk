package com.example.demo;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.autoconfigure.data.r2dbc.AutoConfigureDataR2dbc;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.example.demo.DemoApplication.buildApp;
import static org.hamcrest.Matchers.equalTo;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoApplicationTests {

    private ConfigurableApplicationContext applicationContext;
    private WebTestClient webTestClient;

    @BeforeAll
    void setUp() {
        applicationContext = buildApp().run();
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
