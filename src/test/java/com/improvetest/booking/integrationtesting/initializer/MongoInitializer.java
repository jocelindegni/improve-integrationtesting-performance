package com.improvetest.booking.integrationtesting.initializer;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.Durations;

import java.util.Map;

@Slf4j
public class MongoInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0")
            .withTmpFs(Map.of("/data/db", "rw"));
    static {
        mongoDBContainer.start();
    }

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
        TestPropertyValues
                .of(Map.of("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl()))
                .applyTo(applicationContext);
    }
}
