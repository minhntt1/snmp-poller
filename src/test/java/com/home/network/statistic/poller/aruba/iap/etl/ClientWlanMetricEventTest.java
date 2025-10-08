package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientWlanMetricEventTest {

    @Mock
    private ArubaAiClientInfoEntity mockEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConstructor() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        // Act
        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Assert
        assertEquals("2023-10-01", event.getDateMetric());
        assertEquals(3600, event.getTimeSecondMetric());
        assertEquals(12345L, event.getDeviceMac());
        assertEquals("Test-Device", event.getDeviceName());
        assertEquals(25, event.getDeviceSnr());
    }

    @Test
    void testObtainFirstSqlQuery() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainFirstSqlQuery();

        // Assert
        String expected = """
                select '12345' as `device_mac`, 'Test-Device' as `device_name`, '25' as `device_snr`, '2023-10-01' as `date_metric`, '3600' as `time_metric`
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQuery() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithSpecialCharacters() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device'With'Special");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device'With'Special', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithNullDeviceName() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn(null);
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'null', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithZeroTime() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(0);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', '2023-10-01', '0'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithLargeNumbers() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(86400);
        when(mockEntity.getDeviceMac()).thenReturn(999999999L);
        when(mockEntity.getDeviceName()).thenReturn("Large-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(100);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '999999999', 'Large-Device', '100', '2023-10-01', '86400'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryStaticWithEmptyList() {
        // Arrange
        List<ClientWlanMetricEvent> emptyList = Collections.emptyList();

        // Act
        String sql = ClientWlanMetricEvent.obtainSqlQuery(emptyList);

        // Assert
        assertEquals("", sql);
    }

    @Test
    void testObtainSqlQueryStaticWithSingleEvent() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);
        List<ClientWlanMetricEvent> list = Arrays.asList(event);

        // Act
        String sql = ClientWlanMetricEvent.obtainSqlQuery(list);

        // Assert
        String expected = """
                select '12345' as `device_mac`, 'Test-Device' as `device_name`, '25' as `device_snr`, '2023-10-01' as `date_metric`, '3600' as `time_metric`
                """.trim();
        assertEquals(expected, sql.trim());
    }

    @Test
    void testObtainSqlQueryStaticWithNullEvent() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);
        List<ClientWlanMetricEvent> list = Arrays.asList(event, null);

        // Act
        String sql = ClientWlanMetricEvent.obtainSqlQuery(list);

        // Assert
        String expected = """
                select '12345' as `device_mac`, 'Test-Device' as `device_name`, '25' as `device_snr`, '2023-10-01' as `date_metric`, '3600' as `time_metric`
                """.trim();
        assertEquals(expected, sql.trim());
    }

    @Test
    void testObtainSqlQueryStaticWithAllNullEvents() {
        // Arrange
        List<ClientWlanMetricEvent> list = Arrays.asList(null, null, null);

        // Act
        String sql = ClientWlanMetricEvent.obtainSqlQuery(list);

        // Assert
        assertEquals("", sql);
    }

    @Test
    void testObtainSqlQueryStaticWithMixedValidAndNullEvents() {
        // Arrange
        // First valid event
        ArubaAiClientInfoEntity entity1 = mock(ArubaAiClientInfoEntity.class);
        when(entity1.getDeviceMac()).thenReturn(12345L);
        when(entity1.getDeviceName()).thenReturn("Device-1");
        when(entity1.getDeviceSnr()).thenReturn(25);
        when(entity1.obtainPollDate()).thenReturn("2023-10-01");
        when(entity1.obtainPollTime()).thenReturn(3600);

        ClientWlanMetricEvent event1 = new ClientWlanMetricEvent(entity1);

        // Second valid event
        ArubaAiClientInfoEntity entity2 = mock(ArubaAiClientInfoEntity.class);
        when(entity2.getDeviceMac()).thenReturn(67890L);
        when(entity2.getDeviceName()).thenReturn("Device-2");
        when(entity2.getDeviceSnr()).thenReturn(30);
        when(entity2.obtainPollDate()).thenReturn("2023-10-02");
        when(entity2.obtainPollTime()).thenReturn(7200);

        ClientWlanMetricEvent event2 = new ClientWlanMetricEvent(entity2);

        List<ClientWlanMetricEvent> list = Arrays.asList(event1, null, event2, null);

        // Act
        String sql = ClientWlanMetricEvent.obtainSqlQuery(list);

        // Assert
        String expected1 = """
                select '12345' as `device_mac`, 'Device-1' as `device_name`, '25' as `device_snr`, '2023-10-01' as `date_metric`, '3600' as `time_metric`
                """.trim();
        String expected2 = """
                union all
                select '67890', 'Device-2', '30', '2023-10-02', '7200'
                """.trim();

        String result = sql.trim();
        assertTrue(result.contains(expected1));
        assertTrue(result.contains(expected2));
    }

    @Test
    void testConstructorWithNullValues() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn(null);
        when(mockEntity.obtainPollTime()).thenReturn(null);
        when(mockEntity.getDeviceMac()).thenReturn(null);
        when(mockEntity.getDeviceName()).thenReturn(null);
        when(mockEntity.getDeviceSnr()).thenReturn(null);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);
            assertNull(event.getDateMetric());
            assertNull(event.getTimeSecondMetric());
            assertNull(event.getDeviceMac());
            assertNull(event.getDeviceName());
            assertNull(event.getDeviceSnr());
        });
    }

    @Test
    void testObtainSqlQueryWithNullDateMetric() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn(null);
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', 'null', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithNullTimeMetric() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(null);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', '2023-10-01', 'null'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithNullSnr() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(null);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', 'null', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithNullMac() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(null);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select 'null', 'Test-Device', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithEmptyDeviceName() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', '', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithNegativeSnr() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(-5);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '-5', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithNegativeTime() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(-100);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', '2023-10-01', '-100'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithVeryLargeSnr() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(Integer.MAX_VALUE);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '2147483647', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithVeryLargeTime() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(Integer.MAX_VALUE);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', '2023-10-01', '2147483647'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithVeryLargeMac() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(Long.MAX_VALUE);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '9223372036854775807', 'Test-Device', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithUnicodeDeviceName() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("デバイス-テスト");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'デバイス-テスト', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithSqlInjectionAttempt() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test'; DROP TABLE users; --");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert - The SQL injection should be treated as literal string content
        String expected = """
                union all
                select '12345', 'Test'; DROP TABLE users; --', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithQuotesInDeviceName() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Device \"With\" Quotes");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Device "With" Quotes', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithNewlinesInDeviceName() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Device\nWith\nNewlines");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Device
                With
                Newlines', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithTabsInDeviceName() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Device\tWith\tTabs");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Device	With	Tabs', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithBackslashesInDeviceName() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Device\\With\\Backslashes");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Device\\With\\Backslashes', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithCommasInDeviceName() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Device,With,Commas");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Device,With,Commas', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithDateContainingSpecialCharacters() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01'TIME'--");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', '2023-10-01'TIME'--', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithTimeAsZero() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(0);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', '2023-10-01', '0'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithMinimumIntegerTime() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(Integer.MIN_VALUE);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', '2023-10-01', '-2147483648'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithMinimumIntegerSnr() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(Integer.MIN_VALUE);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '-2147483648', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithMinimumLongMac() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(Long.MIN_VALUE);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '-9223372036854775808', 'Test-Device', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithEmptyDate() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '25', '', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithLongDeviceName() {
        // Arrange
        String longDeviceName = "A".repeat(1000); // Very long device name
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn(longDeviceName);
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expectedStart = """
                union all
                select '12345', '
                """;
        String expectedEnd = """
                ', '25', '2023-10-01', '3600'
                """;

        assertTrue(sql.trim().startsWith(expectedStart.trim()));
        assertTrue(sql.trim().endsWith(expectedEnd.trim()));
        assertTrue(sql.contains(longDeviceName));
    }

    @Test
    void testObtainSqlQueryWithComplexUnicodeDeviceName() {
        // Arrange
        String unicodeName = "测试设备-Device-デバイス-기기";
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn(unicodeName);
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', '测试设备-Device-デバイス-기기', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithEmojisInDeviceName() {
        // Arrange
        String emojiName = "📱📶 Device 📊";
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-01");
        when(mockEntity.obtainPollTime()).thenReturn(3600);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn(emojiName);
        when(mockEntity.getDeviceSnr()).thenReturn(25);

        ClientWlanMetricEvent event = new ClientWlanMetricEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', '📱📶 Device 📊', '25', '2023-10-01', '3600'
                """;
        assertEquals(expected.trim(), sql.trim());
    }
}
