package com.improvetest.booking.integrationtesting;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.improvetest.booking.dto.BookingNotificationDTO;
import com.improvetest.booking.model.Booking;
import com.improvetest.booking.model.Travel;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.improvetest.booking.integrationtesting.initializer.KafkaInitializer.*;
import static com.improvetest.booking.integrationtesting.initializer.UserAccountServiceInitializer.userAccountMockServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class ReminderIntegrationTest extends AbstractIntegrationTest {

    private static final String TRAVEL_ID_WILL_START_SOON_1 = "411d7482-1093-4976-ada0-ef902f337126";
    private static final String TRAVEL_ID_WILL_START_SOON_2 = "511d7482-1093-4976-ada0-ef902f337126";


    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private Consumer<String, BookingNotificationDTO> bookingNotificationDTOConsumer;

    @BeforeEach
    void subscribeToMessagingTopic() {
        bookingNotificationDTOConsumer = buildConsumerAndSubscribeTo(GROUP_ID, messagingTopic);
    }

    @AfterEach
    void close() {
        unsubscribe(bookingNotificationDTOConsumer);
    }

    @Test
    public void given4BookingWith2MustBeReminded_WhenTriggerReminderProcess_2NotificationAreSent() throws Exception {
        // Given
        userAccountMockServer.addStubMapping(StubMapping.buildFrom(GET_USER_BY_ID_OK_MAPPING_JSON));
        saveBookingsForReminding();

        // When then
        mockMvc.perform(post(INTERNAL_BOOKING_V1_API_BASE_PATH + "/trigger-reminder")).andExpect(status().isOk());

        waitingForReminderProcessToBeEnded();
        var bookingNotificationDTOS = getRecordsFromKafka(bookingNotificationDTOConsumer, messagingTopic);
        assertEquals(2, bookingNotificationDTOS.size());
        var travelIds = bookingNotificationDTOS.stream().map(BookingNotificationDTO::getTravelId).toList();
        assertTrue(travelIds.contains(TRAVEL_ID_WILL_START_SOON_1));
        assertTrue(travelIds.contains(TRAVEL_ID_WILL_START_SOON_2));
    }

    private void saveBookingsForReminding() {
        var travelWillStartSoon1 = Travel.builder()
                .id(TRAVEL_ID_WILL_START_SOON_1)
                .departureCity("Rennes")
                .arrivalCity("Paris")
                .departureDatetime(LocalDateTime.now().plusHours(1)).build();

        var travelWillStartSoon2 = Travel.builder()
                .id(TRAVEL_ID_WILL_START_SOON_2)
                .departureCity("Barcelone")
                .arrivalCity("Madrid")
                .departureDatetime(LocalDateTime.now().plusMinutes(5)).build();

        var travelWillStartSoon3 = Travel.builder()
                .id(UUID.randomUUID().toString())
                .departureCity("Abidjan")
                .arrivalCity("San pedro")
                .departureDatetime(LocalDateTime.now().plusHours(1)).build();

        var travelWillNotStartSoon = Travel.builder()
                .id(UUID.randomUUID().toString())
                .departureCity("Abidjan")
                .arrivalCity("San pedro")
                .departureDatetime(LocalDateTime.now().minusDays(3)).build();

        travelRepository.saveAll(List.of(travelWillStartSoon1, travelWillStartSoon2, travelWillStartSoon3,
                travelWillNotStartSoon));


        var bookingWillStartSoon1 = Booking.builder()
                .id(UUID.randomUUID().toString())
                .userId(USER_ID)
                .isReminded(false)
                .travelId(TRAVEL_ID_WILL_START_SOON_1).build();
        var bookingWillStartSoon2 = Booking.builder()
                .id(UUID.randomUUID().toString())
                .userId(USER_ID)
                .isReminded(false)
                .travelId(TRAVEL_ID_WILL_START_SOON_2).build();
        var bookingWillStartSoonButAlreadyReminded = Booking.builder()
                .id(UUID.randomUUID().toString())
                .userId(USER_ID)
                .isReminded(true)
                .travelId(travelWillNotStartSoon.getId()).build();
        var bookingWillNotStartSoon = Booking.builder()
                .id(UUID.randomUUID().toString())
                .userId(USER_ID)
                .isReminded(false)
                .travelId(travelWillNotStartSoon.getId()).build();

        bookingRepository.saveAll(List.of(bookingWillStartSoon1, bookingWillStartSoon2,
                bookingWillStartSoonButAlreadyReminded, bookingWillNotStartSoon));
    }

    private void waitingForReminderProcessToBeEnded() {
        Awaitility.await()
                .atMost(Durations.FIVE_SECONDS)
                .until(() -> threadPoolTaskExecutor.getThreadPoolExecutor().getActiveCount() == 0);
    }
}
