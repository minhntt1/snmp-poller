package com.home.network.statistic.poller.aruba.iap.out;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArubaAiClientInfoEntity Unit Tests")
class ArubaAiClientInfoEntityTest {

    private ArubaAiClientInfoEntity entity;
    private ArubaAiClientInfoEntity otherEntity;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2023, 10, 15, 14, 30, 45);
        entity = ArubaAiClientInfoEntity.builder()
                .id(1L)
                .pollTime(baseTime)
                .deviceMac(123456789L)
                .deviceWlanMac(987654321L)
                .deviceIp(19216811)
                .deviceApIp(19216812)
                .deviceName("TestDevice")
                .deviceRx(1000L)
                .deviceTx(2000L)
                .deviceSnr(25)
                .deviceUptimeSeconds(3600L)
                .build();

        otherEntity = ArubaAiClientInfoEntity.builder()
                .id(2L)
                .pollTime(baseTime.plusHours(1))
                .deviceMac(123456789L)
                .deviceWlanMac(987654321L)
                .deviceIp(19216811)
                .deviceApIp(19216812)
                .deviceName("TestDevice")
                .deviceRx(1500L)
                .deviceTx(2500L)
                .deviceSnr(28)
                .deviceUptimeSeconds(7200L)
                .build();
    }

    @Nested
    @DisplayName("Entity Construction Tests")
    class EntityConstructionTests {

        @Test
        @DisplayName("Should create entity with no-args constructor")
        void shouldCreateEntityWithNoArgsConstructor() {
            // When
            ArubaAiClientInfoEntity noArgsEntity = new ArubaAiClientInfoEntity();

            // Then
            assertNotNull(noArgsEntity);
            assertNull(noArgsEntity.getId());
            assertNull(noArgsEntity.getPollTime());
            assertNull(noArgsEntity.getDeviceMac());
            assertNull(noArgsEntity.getDeviceWlanMac());
            assertNull(noArgsEntity.getDeviceIp());
            assertNull(noArgsEntity.getDeviceApIp());
            assertNull(noArgsEntity.getDeviceName());
            assertNull(noArgsEntity.getDeviceRx());
            assertNull(noArgsEntity.getDeviceTx());
            assertNull(noArgsEntity.getDeviceSnr());
            assertNull(noArgsEntity.getDeviceUptimeSeconds());
        }

        @Test
        @DisplayName("Should create entity with all-args constructor")
        void shouldCreateEntityWithAllArgsConstructor() {
            // When
            ArubaAiClientInfoEntity allArgsEntity = new ArubaAiClientInfoEntity(
                    1L, baseTime, 123456789L, 987654321L, 19216811, 19216812,
                    "TestDevice", 1000L, 2000L, 25, 3600L
            );

            // Then
            assertEquals(1L, allArgsEntity.getId());
            assertEquals(baseTime, allArgsEntity.getPollTime());
            assertEquals(Long.valueOf(123456789L), allArgsEntity.getDeviceMac());
            assertEquals(Long.valueOf(987654321L), allArgsEntity.getDeviceWlanMac());
            assertEquals(Integer.valueOf(19216811), allArgsEntity.getDeviceIp());
            assertEquals(Integer.valueOf(19216812), allArgsEntity.getDeviceApIp());
            assertEquals("TestDevice", allArgsEntity.getDeviceName());
            assertEquals(Long.valueOf(1000L), allArgsEntity.getDeviceRx());
            assertEquals(Long.valueOf(2000L), allArgsEntity.getDeviceTx());
            assertEquals(Integer.valueOf(25), allArgsEntity.getDeviceSnr());
            assertEquals(Long.valueOf(3600L), allArgsEntity.getDeviceUptimeSeconds());
        }

        @Test
        @DisplayName("Should create entity with builder pattern")
        void shouldCreateEntityWithBuilder() {
            // When
            ArubaAiClientInfoEntity builderEntity = ArubaAiClientInfoEntity.builder()
                    .pollTime(baseTime)
                    .deviceMac(111111111L)
                    .deviceWlanMac(222222222L)
                    .deviceIp(19216813)
                    .deviceApIp(19216814)
                    .deviceName("BuilderDevice")
                    .deviceRx(3000L)
                    .deviceTx(4000L)
                    .deviceSnr(30)
                    .deviceUptimeSeconds(7200L)
                    .build();

            // Then
            assertEquals(baseTime, builderEntity.getPollTime());
            assertEquals(Long.valueOf(111111111L), builderEntity.getDeviceMac());
            assertEquals(Long.valueOf(222222222L), builderEntity.getDeviceWlanMac());
            assertEquals(Integer.valueOf(19216813), builderEntity.getDeviceIp());
            assertEquals(Integer.valueOf(19216814), builderEntity.getDeviceApIp());
            assertEquals("BuilderDevice", builderEntity.getDeviceName());
            assertEquals(Long.valueOf(3000L), builderEntity.getDeviceRx());
            assertEquals(Long.valueOf(4000L), builderEntity.getDeviceTx());
            assertEquals(Integer.valueOf(30), builderEntity.getDeviceSnr());
            assertEquals(Long.valueOf(7200L), builderEntity.getDeviceUptimeSeconds());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should return true for same date and hour")
        void shouldReturnTrueForSameDateHour() {
            // Given
            ArubaAiClientInfoEntity sameHourEntity = ArubaAiClientInfoEntity.builder()
                    .pollTime(baseTime.plusMinutes(10)) // Same hour, different minute
                    .deviceMac(123456789L)
                    .deviceWlanMac(987654321L)
                    .deviceIp(19216811)
                    .deviceApIp(19216812)
                    .deviceName("TestDevice")
                    .deviceRx(1000L)
                    .deviceTx(2000L)
                    .deviceSnr(25)
                    .deviceUptimeSeconds(3600L)
                    .build();

            // When
            boolean result = entity.sameDateHour(sameHourEntity);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false for different date")
        void shouldReturnFalseForDifferentDate() {
            // Given
            ArubaAiClientInfoEntity differentDateEntity = ArubaAiClientInfoEntity.builder()
                    .pollTime(baseTime.plusDays(1)) // Different date, same hour
                    .deviceMac(123456789L)
                    .deviceWlanMac(987654321L)
                    .deviceIp(19216811)
                    .deviceApIp(19216812)
                    .deviceName("TestDevice")
                    .deviceRx(1000L)
                    .deviceTx(2000L)
                    .deviceSnr(25)
                    .deviceUptimeSeconds(3600L)
                    .build();

            // When
            boolean result = entity.sameDateHour(differentDateEntity);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for different hour")
        void shouldReturnFalseForDifferentHour() {
            // Given
            ArubaAiClientInfoEntity differentHourEntity = ArubaAiClientInfoEntity.builder()
                    .pollTime(baseTime.plusHours(2)) // Different hour, same date
                    .deviceMac(123456789L)
                    .deviceWlanMac(987654321L)
                    .deviceIp(19216811)
                    .deviceApIp(19216812)
                    .deviceName("TestDevice")
                    .deviceRx(1000L)
                    .deviceTx(2000L)
                    .deviceSnr(25)
                    .deviceUptimeSeconds(3600L)
                    .build();

            // When
            boolean result = entity.sameDateHour(differentHourEntity);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should calculate TX difference correctly")
        void shouldCalculateTxDiffCorrectly() {
            // When
            long result = entity.calcDiffTxOldNew(otherEntity);

            // Then
            assertEquals(500L, result); // 2500 - 2000
        }

        @Test
        @DisplayName("Should handle TX counter reset")
        void shouldHandleTxCounterReset() {
            // Given
            ArubaAiClientInfoEntity resetEntity = ArubaAiClientInfoEntity.builder()
                    .deviceTx(500L) // Less than entity's 2000L (counter reset)
                    .build();

            // When
            long result = entity.calcDiffTxOldNew(resetEntity);

            // Then
            assertEquals(500L, result); // Returns new value when counter resets
        }

        @Test
        @DisplayName("Should calculate RX difference correctly")
        void shouldCalculateRxDiffCorrectly() {
            // When
            long result = entity.calcDiffRxOldNew(otherEntity);

            // Then
            assertEquals(500L, result); // 1500 - 1000
        }

        @Test
        @DisplayName("Should handle RX counter reset")
        void shouldHandleRxCounterReset() {
            // Given
            ArubaAiClientInfoEntity resetEntity = ArubaAiClientInfoEntity.builder()
                    .deviceRx(200L) // Less than entity's 1000L (counter reset)
                    .build();

            // When
            long result = entity.calcDiffRxOldNew(resetEntity);

            // Then
            assertEquals(200L, result); // Returns new value when counter resets
        }

        @Test
        @DisplayName("Should detect reconnect when AP IP changes")
        void shouldDetectReconnectWhenApIpChanges() {
            // Given
            ArubaAiClientInfoEntity reconnectEntity = ArubaAiClientInfoEntity.builder()
                    .deviceApIp(19216813) // Different AP IP
                    .deviceIp(19216811)
                    .deviceWlanMac(987654321L)
                    .deviceUptimeSeconds(3700L) // Recent uptime
                    .build();

            // When
            boolean result = entity.checkReconnect(reconnectEntity);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should detect reconnect when device IP changes")
        void shouldDetectReconnectWhenDeviceIpChanges() {
            // Given
            ArubaAiClientInfoEntity reconnectEntity = ArubaAiClientInfoEntity.builder()
                    .deviceApIp(19216812)
                    .deviceIp(19216815) // Different device IP
                    .deviceWlanMac(987654321L)
                    .deviceUptimeSeconds(3700L) // Recent uptime
                    .build();

            // When
            boolean result = entity.checkReconnect(reconnectEntity);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should detect reconnect when WLAN MAC changes")
        void shouldDetectReconnectWhenWlanMacChanges() {
            // Given
            ArubaAiClientInfoEntity reconnectEntity = ArubaAiClientInfoEntity.builder()
                    .deviceApIp(19216812)
                    .deviceIp(19216811)
                    .deviceWlanMac(555666777L) // Different WLAN MAC
                    .deviceUptimeSeconds(3700L) // Recent uptime
                    .build();

            // When
            boolean result = entity.checkReconnect(reconnectEntity);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should detect reconnect when uptime threshold exceeded")
        void shouldDetectReconnectWhenUptimeThresholdExceeded() {
            // Given
            ArubaAiClientInfoEntity reconnectEntity = ArubaAiClientInfoEntity.builder()
                    .pollTime(baseTime.plusHours(2)) // 2 hours later
                    .deviceMac(123456789L)
                    .deviceApIp(19216812)
                    .deviceIp(19216811)
                    .deviceWlanMac(987654321L)
                    .deviceName("TestDevice")
                    .deviceRx(1000L)
                    .deviceTx(2000L)
                    .deviceSnr(25)
                    .deviceUptimeSeconds(7200L) // 2 hours later
                    .build();

            // When
            boolean result = entity.checkReconnect(reconnectEntity);

            // Then
            assertTrue(result); // Should be true due to > 600 seconds (10 minutes) difference
        }

        @Test
        @DisplayName("Should not detect reconnect when all parameters same and within threshold")
        void shouldNotDetectReconnectWhenAllSameAndWithinThreshold() {
            // Given
            ArubaAiClientInfoEntity noReconnectEntity = ArubaAiClientInfoEntity.builder()
                    .pollTime(baseTime.plusMinutes(1)) // Only 1 minute later
                    .deviceMac(123456789L)
                    .deviceApIp(19216812)
                    .deviceIp(19216811)
                    .deviceWlanMac(987654321L)
                    .deviceName("TestDevice")
                    .deviceRx(1000L)
                    .deviceTx(2000L)
                    .deviceSnr(25)
                    .deviceUptimeSeconds(3660L) // Only 1 minute later (within 10 minute threshold)
                    .build();

            // When
            boolean result = entity.checkReconnect(noReconnectEntity);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should calculate correct uptime datetime")
        void shouldCalculateCorrectUptimeDatetime() {
            // When
            LocalDateTime result = entity.obtainUptime();

            // Then
            assertEquals(baseTime.minusSeconds(3600L), result);
        }

        @Test
        @DisplayName("Should return correct connect date")
        void shouldReturnCorrectConnectDate() {
            // When
            String result = entity.obtainConnectDate();

            // Then
            assertEquals("2023-10-15", result);
        }

        @Test
        @DisplayName("Should return correct connect time in seconds")
        void shouldReturnCorrectConnectTimeInSeconds() {
            // When
            Integer result = entity.obtainConnectTime();

            // Then
            assertEquals(Integer.valueOf(13 * 3600 + 30 * 60 + 45), result); // 13:30:45 in seconds
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
        @DisplayName("Should return correct poll time in seconds")
        void shouldReturnCorrectPollTimeInSeconds() {
            // When
            Integer result = entity.obtainPollTime();

            // Then
            assertEquals(Integer.valueOf(14 * 3600 + 30 * 60 + 45), result); // 14:30:45 in seconds
        }

        @Test
        @DisplayName("Should return correct poll time hour in seconds")
        void shouldReturnCorrectPollTimeHourInSeconds() {
            // When
            Integer result = entity.obtainPollTimeHour();

            // Then
            assertEquals(Integer.valueOf(14 * 3600), result); // 14:00:00 in seconds
        }

        @Test
        @DisplayName("Should generate correct job state key")
        void shouldGenerateCorrectJobStateKey() {
            // When
            String result = entity.obtainJobStateKey();

            // Then
            assertEquals("deviceMac_123456789_deviceName_TestDevice_ifaceWifi_1", result);
        }

        @Test
        @DisplayName("Should handle null device name in job state key")
        void shouldHandleNullDeviceNameInJobStateKey() {
            // Given
            entity.setDeviceName(null);

            // When
            String result = entity.obtainJobStateKey();

            // Then
            assertEquals("deviceMac_123456789_deviceName__ifaceWifi_1", result);
        }

        @Test
        @DisplayName("Should handle empty device name in job state key")
        void shouldHandleEmptyDeviceNameInJobStateKey() {
            // Given
            entity.setDeviceName("");

            // When
            String result = entity.obtainJobStateKey();

            // Then
            assertEquals("deviceMac_123456789_deviceName__ifaceWifi_1", result);
        }

        @Test
        @DisplayName("Should handle special characters in device name")
        void shouldHandleSpecialCharactersInDeviceName() {
            // Given
            entity.setDeviceName("Test-Device_2.4GHz");

            // When
            String result = entity.obtainJobStateKey();

            // Then
            assertEquals("deviceMac_123456789_deviceName_Test-Device_2.4GHz_ifaceWifi_1", result);
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
            assertTrue(json.contains("\"deviceName\":\"TestDevice\""));
            assertTrue(json.contains("\"deviceMac\":123456789"));
            assertTrue(json.contains("\"deviceRx\":1000"));
            assertTrue(json.contains("\"deviceTx\":2000"));
            assertTrue(json.contains("\"deviceSnr\":25"));
            assertTrue(json.contains("\"pollTime\""));
        }

        @Test
        @DisplayName("Should create entity from valid JSON")
        void shouldCreateEntityFromValidJson() {
            // Given
            String json = """
                    {
                        "deviceMac": 111111111,
                        "deviceWlanMac": 222222222,
                        "deviceIp": 19216813,
                        "deviceApIp": 19216814,
                        "deviceName": "JsonDevice",
                        "deviceRx": 3000,
                        "deviceTx": 4000,
                        "deviceSnr": 30,
                        "deviceUptimeSeconds": 7200
                    }
                    """;

            // When
            ArubaAiClientInfoEntity result = ArubaAiClientInfoEntity.from(json);

            // Then
            assertNotNull(result);
            assertEquals(Long.valueOf(111111111L), result.getDeviceMac());
            assertEquals(Long.valueOf(222222222L), result.getDeviceWlanMac());
            assertEquals(Integer.valueOf(19216813), result.getDeviceIp());
            assertEquals(Integer.valueOf(19216814), result.getDeviceApIp());
            assertEquals("JsonDevice", result.getDeviceName());
            assertEquals(Long.valueOf(3000L), result.getDeviceRx());
            assertEquals(Long.valueOf(4000L), result.getDeviceTx());
            assertEquals(Integer.valueOf(30), result.getDeviceSnr());
            assertEquals(Long.valueOf(7200L), result.getDeviceUptimeSeconds());
        }

        @Test
        @DisplayName("Should return null when creating entity from null JSON")
        void shouldReturnNullFromNullJson() {
            // When
            ArubaAiClientInfoEntity result = ArubaAiClientInfoEntity.from(null);

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
                ArubaAiClientInfoEntity.from(malformedJson);
            });
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null poll time in sameDateHour")
        void shouldHandleNullPollTimeInSameDateHour() {
            // Given
            ArubaAiClientInfoEntity nullTimeEntity = ArubaAiClientInfoEntity.builder()
                    .pollTime(null)
                    .build();

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.sameDateHour(nullTimeEntity);
            });
        }

        @Test
        @DisplayName("Should handle null other entity in sameDateHour")
        void shouldHandleNullOtherEntityInSameDateHour() {
            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.sameDateHour(null);
            });
        }

        @Test
        @DisplayName("Should handle null poll time in obtainUptime")
        void shouldHandleNullPollTimeInObtainUptime() {
            // Given
            entity.setPollTime(null);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.obtainUptime();
            });
        }

        @Test
        @DisplayName("Should handle null uptime seconds in obtainUptime")
        void shouldHandleNullUptimeSecondsInObtainUptime() {
            // Given
            entity.setDeviceUptimeSeconds(null);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.obtainUptime();
            });
        }

        @Test
        @DisplayName("Should handle zero uptime seconds")
        void shouldHandleZeroUptimeSeconds() {
            // Given
            entity.setDeviceUptimeSeconds(0L);

            // When
            LocalDateTime result = entity.obtainUptime();

            // Then
            assertEquals(baseTime, result);
        }

        @Test
        @DisplayName("Should handle negative uptime seconds")
        void shouldHandleNegativeUptimeSeconds() {
            // Given
            entity.setDeviceUptimeSeconds(-100L);

            // When
            LocalDateTime result = entity.obtainUptime();

            // Then
            assertEquals(baseTime.plusSeconds(100L), result);
        }

        @Test
        @DisplayName("Should handle very large uptime seconds")
        void shouldHandleVeryLargeUptimeSeconds() {
            // Given
            entity.setDeviceUptimeSeconds(Long.MAX_VALUE);

            // When & Then
            assertThrows(Exception.class, () -> {
                entity.obtainUptime();
            });
        }

        @Test
        @DisplayName("Should handle null values in checkReconnect")
        void shouldHandleNullValuesInCheckReconnect() {
            // Given
            ArubaAiClientInfoEntity nullEntity = ArubaAiClientInfoEntity.builder()
                    .pollTime(baseTime.plusMinutes(5))
                    .deviceMac(123456789L)
                    .deviceApIp(null)
                    .deviceIp(null)
                    .deviceWlanMac(null)
                    .deviceName("TestDevice")
                    .deviceRx(1000L)
                    .deviceTx(2000L)
                    .deviceSnr(25)
                    .deviceUptimeSeconds(3700L)
                    .build();

            // When
            boolean result = entity.checkReconnect(nullEntity);

            // Then
            assertTrue(result); // Should return true because of null value differences
        }

        @Test
        @DisplayName("Should handle null poll time in date/time methods")
        void shouldHandleNullPollTimeInDateTimeMethods() {
            // Given
            entity.setPollTime(null);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.obtainPollDate();
            });

            assertThrows(NullPointerException.class, () -> {
                entity.obtainPollTime();
            });

            assertThrows(NullPointerException.class, () -> {
                entity.obtainPollTimeHour();
            });
        }

        @Test
        @DisplayName("Should handle boundary time values")
        void shouldHandleBoundaryTimeValues() {
            // Given
            LocalDateTime boundaryTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
            entity.setPollTime(boundaryTime);

            // When
            String pollDate = entity.obtainPollDate();
            Integer pollTime = entity.obtainPollTime();
            Integer pollTimeHour = entity.obtainPollTimeHour();

            // Then
            assertEquals("2023-01-01", pollDate);
            assertEquals(Integer.valueOf(0), pollTime);
            assertEquals(Integer.valueOf(0), pollTimeHour);
        }

        @Test
        @DisplayName("Should handle year boundary")
        void shouldHandleYearBoundary() {
            // Given
            LocalDateTime yearBoundaryTime = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
            entity.setPollTime(yearBoundaryTime);

            // When
            String pollDate = entity.obtainPollDate();
            Integer pollTime = entity.obtainPollTime();
            Integer pollTimeHour = entity.obtainPollTimeHour();

            // Then
            assertEquals("2023-12-31", pollDate);
            assertEquals(Integer.valueOf(23 * 3600 + 59 * 60 + 59), pollTime);
            assertEquals(Integer.valueOf(23 * 3600), pollTimeHour);
        }
    }
}
