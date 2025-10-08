package com.home.network.statistic.poller.aruba.iap.out;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArubaAiApInfoEntity Unit Tests")
class ArubaAiApInfoEntityTest {

    private ArubaAiApInfoEntity entity;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.of(2023, 10, 15, 14, 30, 45);
        entity = ArubaAiApInfoEntity.builder()
                .id(1L)
                .pollTime(testTime)
                .apMac(123456789L)
                .apName("TestAP")
                .apIp(19216811)
                .apModel("AP-315")
                .apUptimeSeconds(3600L)
                .build();
    }

    @Nested
    @DisplayName("Entity Construction Tests")
    class EntityConstructionTests {

        @Test
        @DisplayName("Should create entity with no-args constructor")
        void shouldCreateEntityWithNoArgsConstructor() {
            // When
            ArubaAiApInfoEntity noArgsEntity = new ArubaAiApInfoEntity();

            // Then
            assertNotNull(noArgsEntity);
            assertNull(noArgsEntity.getId());
            assertNull(noArgsEntity.getPollTime());
            assertNull(noArgsEntity.getApMac());
            assertNull(noArgsEntity.getApName());
            assertNull(noArgsEntity.getApIp());
            assertNull(noArgsEntity.getApModel());
            assertNull(noArgsEntity.getApUptimeSeconds());
        }

        @Test
        @DisplayName("Should create entity with all-args constructor")
        void shouldCreateEntityWithAllArgsConstructor() {
            // When
            ArubaAiApInfoEntity allArgsEntity = new ArubaAiApInfoEntity(
                    1L, testTime, 123456789L, "TestAP", 19216811, "AP-315", 3600L
            );

            // Then
            assertEquals(1L, allArgsEntity.getId());
            assertEquals(testTime, allArgsEntity.getPollTime());
            assertEquals(Long.valueOf(123456789L), allArgsEntity.getApMac());
            assertEquals("TestAP", allArgsEntity.getApName());
            assertEquals(Integer.valueOf(19216811), allArgsEntity.getApIp());
            assertEquals("AP-315", allArgsEntity.getApModel());
            assertEquals(Long.valueOf(3600L), allArgsEntity.getApUptimeSeconds());
        }

        @Test
        @DisplayName("Should create entity with builder pattern")
        void shouldCreateEntityWithBuilder() {
            // When
            ArubaAiApInfoEntity builderEntity = ArubaAiApInfoEntity.builder()
                    .pollTime(testTime)
                    .apMac(111111111L)
                    .apName("BuilderAP")
                    .apIp(19216812)
                    .apModel("AP-335")
                    .apUptimeSeconds(7200L)
                    .build();

            // Then
            assertEquals(testTime, builderEntity.getPollTime());
            assertEquals(Long.valueOf(111111111L), builderEntity.getApMac());
            assertEquals("BuilderAP", builderEntity.getApName());
            assertEquals(Integer.valueOf(19216812), builderEntity.getApIp());
            assertEquals("AP-335", builderEntity.getApModel());
            assertEquals(Long.valueOf(7200L), builderEntity.getApUptimeSeconds());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should return 1 when current uptime is greater than other uptime")
        void shouldReturnOneWhenUptimeGreater() {
            // Given
            ArubaAiApInfoEntity other = ArubaAiApInfoEntity.builder()
                    .apUptimeSeconds(1800L) // Less than entity's 3600L
                    .build();

            // When
            int result = entity.checkUptimeGreater(other);

            // Then
            assertEquals(1, result);
        }

        @Test
        @DisplayName("Should return 0 when current uptime is less than or equal to other uptime")
        void shouldReturnZeroWhenUptimeLessOrEqual() {
            // Given
            ArubaAiApInfoEntity other = ArubaAiApInfoEntity.builder()
                    .apUptimeSeconds(7200L) // Greater than entity's 3600L
                    .build();

            // When
            int result = entity.checkUptimeGreater(other);

            // Then
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should return 0 when uptimes are equal")
        void shouldReturnZeroWhenUptimesEqual() {
            // Given
            ArubaAiApInfoEntity other = ArubaAiApInfoEntity.builder()
                    .apUptimeSeconds(3600L) // Equal to entity's uptime
                    .build();

            // When
            int result = entity.checkUptimeGreater(other);

            // Then
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should return correct week date for Monday")
        void shouldReturnCorrectWeekDateForMonday() {
            // Given
            LocalDateTime mondayTime = LocalDateTime.of(2023, 10, 16, 14, 30, 45); // Monday
            entity.setPollTime(mondayTime);

            // When
            LocalDate result = entity.obtainWeekDate();

            // Then
            assertEquals(LocalDate.of(2023, 10, 16), result);
        }

        @Test
        @DisplayName("Should return correct week date for Sunday")
        void shouldReturnCorrectWeekDateForSunday() {
            // Given
            LocalDateTime sundayTime = LocalDateTime.of(2023, 10, 15, 14, 30, 45); // Sunday
            entity.setPollTime(sundayTime);

            // When
            LocalDate result = entity.obtainWeekDate();

            // Then
            assertEquals(LocalDate.of(2023, 10, 9), result); // Should be the Monday of that week
        }

        @Test
        @DisplayName("Should return correct week date for Wednesday")
        void shouldReturnCorrectWeekDateForWednesday() {
            // Given
            LocalDateTime wednesdayTime = LocalDateTime.of(2023, 10, 18, 14, 30, 45); // Wednesday
            entity.setPollTime(wednesdayTime);

            // When
            LocalDate result = entity.obtainWeekDate();

            // Then
            assertEquals(LocalDate.of(2023, 10, 16), result); // Should be the Monday of that week
        }

        @Test
        @DisplayName("Should generate correct job AP state key")
        void shouldGenerateCorrectJobApStateKey() {
            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apMac_123456789_apName_TestAP", result);
        }

        @Test
        @DisplayName("Should handle null apMac in job AP state key")
        void shouldHandleNullApMacInJobApStateKey() {
            // Given
            entity.setApMac(null);

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apMac_null_apName_TestAP", result);
        }

        @Test
        @DisplayName("Should handle null apName in job AP state key")
        void shouldHandleNullApNameInJobApStateKey() {
            // Given
            entity.setApName(null);

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apMac_123456789_apName_null", result);
        }

        @Test
        @DisplayName("Should handle both null apMac and apName in job AP state key")
        void shouldHandleBothNullInJobApStateKey() {
            // Given
            entity.setApMac(null);
            entity.setApName(null);

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apMac_null_apName_null", result);
        }

        @Test
        @DisplayName("Should handle empty apName in job AP state key")
        void shouldHandleEmptyApNameInJobApStateKey() {
            // Given
            entity.setApName("");

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apMac_123456789_apName_", result);
        }

        @Test
        @DisplayName("Should handle special characters in apName")
        void shouldHandleSpecialCharactersInApName() {
            // Given
            entity.setApName("Test-AP_2.4GHz");

            // When
            String result = entity.obtainJobApStateKey();

            // Then
            assertEquals("apMac_123456789_apName_Test-AP_2.4GHz", result);
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
            assertTrue(json.contains("\"apName\":\"TestAP\""));
            assertTrue(json.contains("\"apMac\":123456789"));
            assertTrue(json.contains("\"apIp\":19216811"));
            assertTrue(json.contains("\"apModel\":\"AP-315\""));
            assertTrue(json.contains("\"apUptimeSeconds\":3600"));
            assertTrue(json.contains("\"pollTime\""));
        }

        @Test
        @DisplayName("Should create entity from valid JSON")
        void shouldCreateEntityFromValidJson() {
            // Given
            String json = """
                    {
                        "apMac": 111111111,
                        "apName": "JsonAP",
                        "apIp": 19216812,
                        "apModel": "AP-335",
                        "apUptimeSeconds": 7200
                    }
                    """;

            // When
            ArubaAiApInfoEntity result = ArubaAiApInfoEntity.from(json);

            // Then
            assertNotNull(result);
            assertEquals(Long.valueOf(111111111L), result.getApMac());
            assertEquals("JsonAP", result.getApName());
            assertEquals(Integer.valueOf(19216812), result.getApIp());
            assertEquals("AP-335", result.getApModel());
            assertEquals(Long.valueOf(7200L), result.getApUptimeSeconds());
        }

        @Test
        @DisplayName("Should return null when creating entity from null JSON")
        void shouldReturnNullFromNullJson() {
            // When
            ArubaAiApInfoEntity result = ArubaAiApInfoEntity.from(null);

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
                ArubaAiApInfoEntity.from(malformedJson);
            });
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null poll time in obtainWeekDate")
        void shouldHandleNullPollTimeInObtainWeekDate() {
            // Given
            entity.setPollTime(null);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.obtainWeekDate();
            });
        }

        @Test
        @DisplayName("Should handle null apUptimeSeconds in checkUptimeGreater")
        void shouldHandleNullApUptimeSecondsInCheckUptimeGreater() {
            // Given
            entity.setApUptimeSeconds(null);
            ArubaAiApInfoEntity other = ArubaAiApInfoEntity.builder()
                    .apUptimeSeconds(3600L)
                    .build();

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.checkUptimeGreater(other);
            });
        }

        @Test
        @DisplayName("Should handle null other entity in checkUptimeGreater")
        void shouldHandleNullOtherEntityInCheckUptimeGreater() {
            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.checkUptimeGreater(null);
            });
        }

        @Test
        @DisplayName("Should handle null apUptimeSeconds in other entity")
        void shouldHandleNullApUptimeSecondsInOtherEntity() {
            // Given
            ArubaAiApInfoEntity other = ArubaAiApInfoEntity.builder()
                    .apUptimeSeconds(null)
                    .build();

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                entity.checkUptimeGreater(other);
            });
        }

        @Test
        @DisplayName("Should handle zero uptime values")
        void shouldHandleZeroUptimeValues() {
            // Given
            entity.setApUptimeSeconds(0L);
            ArubaAiApInfoEntity other = ArubaAiApInfoEntity.builder()
                    .apUptimeSeconds(3600L)
                    .build();

            // When
            int result = entity.checkUptimeGreater(other);

            // Then
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should handle negative uptime values")
        void shouldHandleNegativeUptimeValues() {
            // Given
            entity.setApUptimeSeconds(-100L);
            ArubaAiApInfoEntity other = ArubaAiApInfoEntity.builder()
                    .apUptimeSeconds(3600L)
                    .build();

            // When
            int result = entity.checkUptimeGreater(other);

            // Then
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should handle very large uptime values")
        void shouldHandleVeryLargeUptimeValues() {
            // Given
            entity.setApUptimeSeconds(Long.MAX_VALUE);
            ArubaAiApInfoEntity other = ArubaAiApInfoEntity.builder()
                    .apUptimeSeconds(3600L)
                    .build();

            // When
            int result = entity.checkUptimeGreater(other);

            // Then
            assertEquals(1, result);
        }
    }
}
