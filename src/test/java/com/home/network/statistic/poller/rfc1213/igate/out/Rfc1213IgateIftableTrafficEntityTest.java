package com.home.network.statistic.poller.rfc1213.igate.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rfc1213IgateIftableTrafficEntity Unit Tests")
class Rfc1213IgateIftableTrafficEntityTest {

    private Rfc1213IgateIftableTrafficEntity entity;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.of(2023, 10, 15, 14, 30, 45);
        entity = Rfc1213IgateIftableTrafficEntity.builder()
                .id(1L)
                .pollTime(testTime)
                .ifIndex(1)
                .ifDescr("Ethernet0/0")
                .ifPhysAddress(123456789L)
                .ifAdminStatus("1")
                .ifOperStatus("1")
                .ifInOctets(1000L)
                .ifOutOctets(2000L)
                .ipAdEntAddr(19216811)
                .build();
    }

    @Nested
    @DisplayName("Entity Construction Tests")
    class EntityConstructionTests {

        @Test
        @DisplayName("Should create entity with no-args constructor")
        void shouldCreateEntityWithNoArgsConstructor() {
            // When
            Rfc1213IgateIftableTrafficEntity noArgsEntity = new Rfc1213IgateIftableTrafficEntity();

            // Then
            assertNotNull(noArgsEntity);
            assertNull(noArgsEntity.getId());
            assertNull(noArgsEntity.getPollTime());
        }

        @Test
        @DisplayName("Should create entity with all-args constructor")
        void shouldCreateEntityWithAllArgsConstructor() {
            // When
            Rfc1213IgateIftableTrafficEntity allArgsEntity = new Rfc1213IgateIftableTrafficEntity(
                    1L, testTime, 1, "Ethernet0/0", 123456789L, "1", "1", 1000L, 2000L, 19216811
            );

            // Then
            assertEquals(1L, allArgsEntity.getId());
            assertEquals(testTime, allArgsEntity.getPollTime());
            assertEquals(Integer.valueOf(1), allArgsEntity.getIfIndex());
            assertEquals("Ethernet0/0", allArgsEntity.getIfDescr());
            assertEquals(Long.valueOf(123456789L), allArgsEntity.getIfPhysAddress());
            assertEquals("1", allArgsEntity.getIfAdminStatus());
            assertEquals("1", allArgsEntity.getIfOperStatus());
            assertEquals(Long.valueOf(1000L), allArgsEntity.getIfInOctets());
            assertEquals(Long.valueOf(2000L), allArgsEntity.getIfOutOctets());
            assertEquals(Integer.valueOf(19216811), allArgsEntity.getIpAdEntAddr());
        }

        @Test
        @DisplayName("Should create entity with builder pattern")
        void shouldCreateEntityWithBuilder() {
            // When
            Rfc1213IgateIftableTrafficEntity builderEntity = Rfc1213IgateIftableTrafficEntity.builder()
                    .pollTime(testTime)
                    .ifIndex(2)
                    .ifDescr("Ethernet0/1")
                    .build();

            // Then
            assertEquals(testTime, builderEntity.getPollTime());
            assertEquals(Integer.valueOf(2), builderEntity.getIfIndex());
            assertEquals("Ethernet0/1", builderEntity.getIfDescr());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should return true for usable entry when oper status is 1 and phys address is valid")
        void shouldReturnTrueForUsableEntry() {
            // Given
            entity.setIfOperStatus("1");
            entity.setIfPhysAddress(123456789L);

            // When
            boolean result = entity.checkUsableEntry();

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false for unusable entry when oper status is not 1")
        void shouldReturnFalseWhenOperStatusNotOne() {
            // Given
            entity.setIfOperStatus("2");
            entity.setIfPhysAddress(123456789L);

            // When
            boolean result = entity.checkUsableEntry();

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for unusable entry when phys address is null")
        void shouldReturnFalseWhenPhysAddressIsNull() {
            // Given
            entity.setIfOperStatus("1");
            entity.setIfPhysAddress(null);

            // When
            boolean result = entity.checkUsableEntry();

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for unusable entry when phys address is zero")
        void shouldReturnFalseWhenPhysAddressIsZero() {
            // Given
            entity.setIfOperStatus("1");
            entity.setIfPhysAddress(0L);

            // When
            boolean result = entity.checkUsableEntry();

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return true when both entities have same date and hour")
        void shouldReturnTrueForSameDateHour() {
            // Given
            Rfc1213IgateIftableTrafficEntity other = Rfc1213IgateIftableTrafficEntity.builder()
                    .pollTime(LocalDateTime.of(2023, 10, 15, 14, 45, 30)) // Same date and hour
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
            Rfc1213IgateIftableTrafficEntity other = Rfc1213IgateIftableTrafficEntity.builder()
                    .pollTime(LocalDateTime.of(2023, 10, 16, 14, 30, 45)) // Different date
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
            Rfc1213IgateIftableTrafficEntity other = Rfc1213IgateIftableTrafficEntity.builder()
                    .pollTime(LocalDateTime.of(2023, 10, 15, 15, 30, 45)) // Different hour
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
            Rfc1213IgateIftableTrafficEntity oldEntity = Rfc1213IgateIftableTrafficEntity.builder()
                    .ifOutOctets(1000L)
                    .build();

            Rfc1213IgateIftableTrafficEntity newEntity = Rfc1213IgateIftableTrafficEntity.builder()
                    .ifOutOctets(1500L)
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
            Rfc1213IgateIftableTrafficEntity oldEntity = Rfc1213IgateIftableTrafficEntity.builder()
                    .ifOutOctets(Long.MAX_VALUE - 500L)
                    .build();

            Rfc1213IgateIftableTrafficEntity newEntity = Rfc1213IgateIftableTrafficEntity.builder()
                    .ifOutOctets(1000L)
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
            Rfc1213IgateIftableTrafficEntity oldEntity = Rfc1213IgateIftableTrafficEntity.builder()
                    .ifInOctets(2000L)
                    .build();

            Rfc1213IgateIftableTrafficEntity newEntity = Rfc1213IgateIftableTrafficEntity.builder()
                    .ifInOctets(2500L)
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
            Rfc1213IgateIftableTrafficEntity oldEntity = Rfc1213IgateIftableTrafficEntity.builder()
                    .ifInOctets(Long.MAX_VALUE - 300L)
                    .build();

            Rfc1213IgateIftableTrafficEntity newEntity = Rfc1213IgateIftableTrafficEntity.builder()
                    .ifInOctets(1500L)
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
            assertEquals("ifPhysAddress_123456789_ifDescr_Ethernet0/0", result);
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
            assertTrue(json.contains("\"ifDescr\":\"Ethernet0/0\""));
            assertTrue(json.contains("\"ifIndex\":1"));
            assertTrue(json.contains("\"pollTime\""));
        }

        @Test
        @DisplayName("Should create entity from valid JSON")
        void shouldCreateEntityFromValidJson() {
            // Given
            String json = """
                    {
                        "ifIndex": 2,
                        "ifDescr": "Ethernet0/1",
                        "ifPhysAddress": 987654321,
                        "ifAdminStatus": "1",
                        "ifOperStatus": "1",
                        "ifInOctets": 3000,
                        "ifOutOctets": 4000,
                        "ipAdEntAddr": 19216812
                    }
                    """;

            // When
            Rfc1213IgateIftableTrafficEntity result = Rfc1213IgateIftableTrafficEntity.from(json);

            // Then
            assertNotNull(result);
            assertEquals(Integer.valueOf(2), result.getIfIndex());
            assertEquals("Ethernet0/1", result.getIfDescr());
            assertEquals(Long.valueOf(987654321L), result.getIfPhysAddress());
            assertEquals("1", result.getIfAdminStatus());
            assertEquals("1", result.getIfOperStatus());
            assertEquals(Long.valueOf(3000L), result.getIfInOctets());
            assertEquals(Long.valueOf(4000L), result.getIfOutOctets());
            assertEquals(Integer.valueOf(19216812), result.getIpAdEntAddr());
        }

        @Test
        @DisplayName("Should return null when creating entity from null JSON")
        void shouldReturnNullFromNullJson() {
            // When
            Rfc1213IgateIftableTrafficEntity result = Rfc1213IgateIftableTrafficEntity.from(null);

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
                Rfc1213IgateIftableTrafficEntity.from(malformedJson);
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
            Rfc1213IgateIftableTrafficEntity other = Rfc1213IgateIftableTrafficEntity.builder()
                    .pollTime(testTime)
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
        @DisplayName("Should handle null poll time in obtainPollTimeHour")
        void shouldHandleNullPollTimeInObtainPollTimeHour() {
            // Given
            entity.setPollTime(null);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.obtainPollTimeHour();
            });
        }


        @Test
        @DisplayName("Should handle null phys address in obtainJobStateKey")
        void shouldHandleNullPhysAddressInObtainJobStateKey() {
            // Given
            entity.setIfPhysAddress(null);

            // When
            String result = entity.obtainJobStateKey();

            // Then
            assertEquals("ifPhysAddress_null_ifDescr_Ethernet0/0", result);
        }

        @Test
        @DisplayName("Should handle null descr in obtainJobStateKey")
        void shouldHandleNullDescrInObtainJobStateKey() {
            // Given
            entity.setIfDescr(null);

            // When
            String result = entity.obtainJobStateKey();

            // Then
            assertEquals("ifPhysAddress_123456789_ifDescr_null", result);
        }
    }
}
