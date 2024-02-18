package com.improvetest.booking.integrationtesting.initializer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.Durations;

import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@Slf4j
public class UserAccountServiceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>{
    public static final WireMockServer userAccountMockServer = new WireMockServer(
            options().dynamicHttpsPort().notifier(new ConsoleNotifier(true))
    );

    static {
        userAccountMockServer.start();
    }

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
        var port = userAccountMockServer.port();
        TestPropertyValues
                .of(Map.of("user-account.url", "http://localhost:" + port))
                .applyTo(applicationContext);
    }

    public static void waitingForUserAccountMockServerToBeReady(){
        log.info("Waiting for userAccountMockserver to be ready...");
        Awaitility.await()
                .atMost(Durations.FIVE_SECONDS)
                .until(userAccountMockServer::isRunning);
    }

    public static void resetUserAccountMockServerRequest(){
        userAccountMockServer.resetAll();
    }
}
