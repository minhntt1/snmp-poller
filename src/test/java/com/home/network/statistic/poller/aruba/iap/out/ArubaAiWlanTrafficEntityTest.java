package com.home.network.statistic.poller.aruba.iap.out;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArubaAiWlanTrafficEntity Unit Tests")
class ArubaAiWlanTrafficEntityTest {

    private ArubaAiWlanTrafficEntity entity;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.of(2023, 10, 15, 14, 30, 45);
        entity = ArubaAiWlanTrafficEntity.builder()
                .id(1L)
                .pollTime(testTime)
                .wlanApMac(123456789L)
                .wlanEssid("TestWiFi")
                .wlanMac(987654321L)
                .wlanRx(1000L)
                .wlanTx(2000L)
                .build();
    }

    @Nested
    @DisplayName("Entity Construction Tests")
    class EntityConstructionTests {

        @Test
        @DisplayName("Should create entity with no-args constructor")
        void shouldCreateEntityWithNoArgsConstructor() {
            // When
            ArubaAiWlanTrafficEntity noArgsEntity = new ArubaAiWlanTrafficEntity();

            // Then
            assertNotNull(noArgsEntity);
            assertNull(noArgsEntity.getId());
            assertNull(noArgsEntity.getPollTime());
            assertNull(noArgsEntity.getWlanApMac());
            assertNull(noArgsEntity.getWlanEssid());
            assertNull(noArgsEntity.getWlanMac());
            assertNull(noArgsEntity.getWlanRx());
            assertNull(noArgsEntity.getWlanTx());
        }

        @Test
        @DisplayName("Should create entity with all-args constructor")
        void shouldCreateEntityWithAllArgsConstructor() {
            // When
            ArubaAiWlanTrafficEntity allArgsEntity = new ArubaAiWlanTrafficEntity(
                    1L, testTime, 123456789L, "TestWiFi", 987654321L, 1000L, 2000L
            );

            // Then
            assertEquals(1L, allArgsEntity.getId());
            assertEquals(testTime, allArgsEntity.getPollTime());
            assertEquals(Long.valueOf(123456789L), allArgsEntity.getWlanApMac());
            assertEquals("TestWiFi", allArgsEntity.getWlanEssid());
            assertEquals(Long.valueOf(987654321L), allArgsEntity.getWlanMac());
            assertEquals(Long.valueOf(1000L), allArgsEntity.getWlanRx());
            assertEquals(Long.valueOf(2000L), allArgsEntity.getWlanTx());
        }

        @Test
        @DisplayName("Should create entity with builder pattern")
        void shouldCreateEntityWithBuilder() {
            // When
            ArubaAiWlanTrafficEntity builderEntity = ArubaAiWlanTrafficEntity.builder()
                    .pollTime(testTime)
                    .wlanApMac(111111111L)
                    .wlanEssid("BuilderWiFi")
                    .wlanMac(222222222L)
                    .wlanRx(1500L)
                    .wlanTx(2500L)
                    .build();

            // Then
            assertEquals(testTime, builderEntity.getPollTime());
            assertEquals(Long.valueOf(111111111L), builderEntity.getWlanApMac());
            assertEquals("BuilderWiFi", builderEntity.getWlanEssid());
            assertEquals(Long.valueOf(222222222L), builderEntity.getWlanMac());
            assertEquals(Long.valueOf(1500L), builderEntity.getWlanRx());
            assertEquals(Long.valueOf(2500L), builderEntity.getWlanTx());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should return true when both entities have same date and hour")
        void shouldReturnTrueForSameDateHour() {
            // Given
            ArubaAiWlanTrafficEntity other = ArubaAiWlanTrafficEntity.builder()
                    .pollTime(LocalDateTime.of(2023, 10, 15, 14, 45, 30)) // Same date and hour
                    .wlanApMac(123456789L)
                    .wlanEssid("TestWiFi")
                    .build();

            // When
            boolean result = entity.sameDateHour(other);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when entities have different date")
        void shouldReturnFalseForDifferentDate() {
            // Given
            ArubaAiWlanTrafficEntity other = ArubaAiWlanTrafficEntity.builder()
                    .pollTime(LocalDateTime.of(2023, 10, 16, 14, 30, 45)) // Different date
                    .wlanApMac(123456789L)
                    .wlanEssid("TestWiFi")
                    .build();

            // When
            boolean result = entity.sameDateHour(other);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when entities have different hour")
        void shouldReturnFalseForDifferentHour() {
            // Given
            ArubaAiWlanTrafficEntity other = ArubaAiWlanTrafficEntity.builder()
                    .pollTime(LocalDateTime.of(2023, 10, 15, 15, 30, 45)) // Different hour
                    .wlanApMac(123456789L)
                    .wlanEssid("TestWiFi")
                    .build();

            // When
            boolean result = entity.sameDateHour(other);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should calculate correct transmit difference")
        void shouldCalculateCorrectTxDifference() {
            // Given
            ArubaAiWlanTrafficEntity oldEntity = ArubaAiWlanTrafficEntity.builder()
                    .wlanTx(1000L)
                    .build();

            ArubaAiWlanTrafficEntity newEntity = ArubaAiWlanTrafficEntity.builder()
                    .wlanTx(1500L)
                    .build();

            // When
            long result = oldEntity.calcDiffTxOldNew(newEntity);

            // Then
            assertEquals(500L, result);
        }

        @Test
        @DisplayName("Should handle transmit counter overflow correctly")
        void shouldHandleTxCounterOverflow() {
            // Given
            ArubaAiWlanTrafficEntity oldEntity = ArubaAiWlanTrafficEntity.builder()
                    .wlanTx(Long.MAX_VALUE - 500L)
                    .build();

            ArubaAiWlanTrafficEntity newEntity = ArubaAiWlanTrafficEntity.builder()
                    .wlanTx(1000L)
                    .build();

            // When
            long result = oldEntity.calcDiffTxOldNew(newEntity);

            // Then
            assertEquals(1000L, result); // Should use new value when overflow detected
        }

        @Test
        @DisplayName("Should calculate correct receive difference")
        void shouldCalculateCorrectRxDifference() {
            // Given
            ArubaAiWlanTrafficEntity oldEntity = ArubaAiWlanTrafficEntity.builder()
                    .wlanRx(2000L)
                    .build();

            ArubaAiWlanTrafficEntity newEntity = ArubaAiWlanTrafficEntity.builder()
                    .wlanRx(2500L)
                    .build();

            // When
            long result = oldEntity.calcDiffRxOldNew(newEntity);

            // Then
            assertEquals(500L, result);
        }

        @Test
        @DisplayName("Should handle receive counter overflow correctly")
        void shouldHandleRxCounterOverflow() {
            // Given
            ArubaAiWlanTrafficEntity oldEntity = ArubaAiWlanTrafficEntity.builder()
                    .wlanRx(Long.MAX_VALUE - 300L)
                    .build();

            ArubaAiWlanTrafficEntity newEntity = ArubaAiWlanTrafficEntity.builder()
                    .wlanRx(1500L)
                    .build();

            // When
            long result = oldEntity.calcDiffRxOldNew(newEntity);

            // Then
            assertEquals(1500L, result); // Should use new value when overflow detected
        }

        @Test
        @DisplayName("Should return correct poll date")
        void shouldReturnCorrectPollDate() {
            // When
            String result = entity.obtainPollDate();

            // Then
            assertEquals("2023-10-15", result);
        }

        @Test
        @DisplayName("Should return correct poll time hour in seconds")
        void shouldReturnCorrectPollTimeHour() {
            // When
            int result = entity.obtainPollHour();

            // Then
            assertEquals(14 * 3600, result); // 14:00:00 in seconds
        }

        @Test
        @DisplayName("Should generate correct job AP state key")
        void shouldGenerateCorrectJobApStateKey() {
            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apWlanMac_987654321_apWlanName_TestWiFi", result);
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should convert entity to JSON correctly")
        void shouldConvertEntityToJson() {
            // When
            String json = entity.toJson();

            // Then
            assertNotNull(json);
            assertTrue(json.contains("\"wlanEssid\":\"TestWiFi\""));
            assertTrue(json.contains("\"wlanApMac\":123456789"));
            assertTrue(json.contains("\"wlanMac\":987654321"));
            assertTrue(json.contains("\"wlanRx\":1000"));
            assertTrue(json.contains("\"wlanTx\":2000"));
            assertTrue(json.contains("\"pollTime\""));
        }

        @Test
        @DisplayName("Should create entity from valid JSON")
        void shouldCreateEntityFromValidJson() {
            // Given
            String json = """
                    {
                        "wlanApMac": 111111111,
                        "wlanEssid": "JsonWiFi",
                        "wlanMac": 222222222,
                        "wlanRx": 3000,
                        "wlanTx": 4000
                    }
                    """;

            // When
            ArubaAiWlanTrafficEntity result = ArubaAiWlanTrafficEntity.from(json);

            // Then
            assertNotNull(result);
            assertEquals(Long.valueOf(111111111L), result.getWlanApMac());
            assertEquals("JsonWiFi", result.getWlanEssid());
            assertEquals(Long.valueOf(222222222L), result.getWlanMac());
            assertEquals(Long.valueOf(3000L), result.getWlanRx());
            assertEquals(Long.valueOf(4000L), result.getWlanTx());
        }

        @Test
        @DisplayName("Should return null when creating entity from null JSON")
        void shouldReturnNullFromNullJson() {
            // When
            ArubaAiWlanTrafficEntity result = ArubaAiWlanTrafficEntity.from(null);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() {
            // Given
            String malformedJson = "{ invalid json }";

            // When & Then
            assertThrows(Exception.class, () -> {
                ArubaAiWlanTrafficEntity.from(malformedJson);
            });
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null poll time in sameDateHour comparison")
        void shouldHandleNullPollTimeInSameDateHour() {
            // Given
            entity.setPollTime(null);
            ArubaAiWlanTrafficEntity other = ArubaAiWlanTrafficEntity.builder()
                    .pollTime(testTime)
                    .wlanApMac(123456789L)
                    .wlanEssid("TestWiFi")
                    .build();

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.sameDateHour(other);
            });
        }

        @Test
        @DisplayName("Should handle null poll time in obtainPollDate")
        void shouldHandleNullPollTimeInObtainPollDate() {
            // Given
            entity.setPollTime(null);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.obtainPollDate();
            });
        }

        @Test
        @DisplayName("Should handle null poll time in obtainPollHour")
        void shouldHandleNullPollTimeInObtainPollHour() {
            // Given
            entity.setPollTime(null);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.obtainPollHour();
            });
        }

        @Test
        @DisplayName("Should handle null wlanApMac in obtainJobApStateKey")
        void shouldHandleNullWlanApMacInObtainJobApStateKey() {
            // Given
            entity.setWlanMac(null);

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apWlanMac_null_apWlanName_TestWiFi", result);
        }

        @Test
        @DisplayName("Should handle null wlanEssid in obtainJobApStateKey")
        void shouldHandleNullWlanEssidInObtainJobApStateKey() {
            // Given
            entity.setWlanEssid(null);

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apWlanMac_987654321_apWlanName_null", result);
        }

        @Test
        @DisplayName("Should handle both null wlanApMac and wlanEssid in obtainJobApStateKey")
        void shouldHandleBothNullInObtainJobApStateKey() {
            // Given
            entity.setWlanMac(null);
            entity.setWlanEssid(null);

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apWlanMac_null_apWlanName_null", result);
        }

        @Test
        @DisplayName("Should handle empty wlanEssid in obtainJobApStateKey")
        void shouldHandleEmptyWlanEssidInObtainJobApStateKey() {
            // Given
            entity.setWlanEssid("");

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apWlanMac_987654321_apWlanName_", result);
        }

        @Test
        @DisplayName("Should handle special characters in wlanEssid")
        void shouldHandleSpecialCharactersInWlanEssid() {
            // Given
            entity.setWlanEssid("Test-WiFi_2.4GHz");

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apWlanMac_987654321_apWlanName_Test-WiFi_2.4GHz", result);
        }
    }
}
