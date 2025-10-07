package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ClientWlanConnectEventTest {

    @Mock
    private ArubaAiClientInfoEntity mockEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConstructorWithValidEntity() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceIp()).thenReturn(1921681001);
        when(mockEntity.getDeviceWlanMac()).thenReturn(67890L);
        when(mockEntity.obtainConnectDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainConnectTime()).thenReturn(50400);

        // Act
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity);

        // Assert
        assertEquals(12345L, event.getDeviceMac());
        assertEquals("Test-Device", event.getDeviceName());
        assertEquals(1921681001, event.getDeviceIp());
        assertEquals(67890L, event.getDeviceWlanMac());
        assertEquals("2023-10-02", event.getDateConnect());
        assertEquals(50400, event.getTimeSecondConnect());
    }

    @Test
    void testConstructorWithNullDeviceName() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn(null); // null device name
        when(mockEntity.getDeviceIp()).thenReturn(1921681001);
        when(mockEntity.getDeviceWlanMac()).thenReturn(67890L);
        when(mockEntity.obtainConnectDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainConnectTime()).thenReturn(50400);

        // Act
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity);

        // Assert
        assertEquals("", event.getDeviceName()); // Should default to empty string
    }

    @Test
    void testConstructorWithNullDeviceWlanMac() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceIp()).thenReturn(1921681001);
        when(mockEntity.getDeviceWlanMac()).thenReturn(null); // null wlan mac
        when(mockEntity.obtainConnectDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainConnectTime()).thenReturn(50400);

        // Act
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity);

        // Assert
        assertEquals(Long.MIN_VALUE, event.getDeviceWlanMac()); // Should default to Long.MIN_VALUE
    }

    @Test
    void testObtainFirstSqlQuery() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceIp()).thenReturn(1921681001);
        when(mockEntity.getDeviceWlanMac()).thenReturn(67890L);
        when(mockEntity.obtainConnectDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainConnectTime()).thenReturn(50400);

        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity);

        // Act
        String sql = event.obtainFirstSqlQuery();

        // Assert
        String expected = """
                select '12345' as `device_mac`, 'Test-Device' as `device_name`, '1921681001' as `device_ip`, '67890' as `device_wlan_mac`, '2023-10-02' as `date_connect`, '50400' as `time_connect`
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQuery() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceIp()).thenReturn(1921681001);
        when(mockEntity.getDeviceWlanMac()).thenReturn(67890L);
        when(mockEntity.obtainConnectDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainConnectTime()).thenReturn(50400);

        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity);

        // Act
        String sql = event.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '1921681001', '67890', '2023-10-02', '50400'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithEmptyDeviceName() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn(""); // empty device name
        when(mockEntity.getDeviceIp()).thenReturn(1921681001);
        when(mockEntity.getDeviceWlanMac()).thenReturn(67890L);
        when(mockEntity.obtainConnectDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainConnectTime()).thenReturn(50400);

        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity);

        // Act
        String sql = event.obtainFirstSqlQuery();

        // Assert
        String expected = """
                select '12345' as `device_mac`, '' as `device_name`, '1921681001' as `device_ip`, '67890' as `device_wlan_mac`, '2023-10-02' as `date_connect`, '50400' as `time_connect`
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithMinValueWlanMac() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceIp()).thenReturn(1921681001);
        when(mockEntity.getDeviceWlanMac()).thenReturn(Long.MIN_VALUE); // min value wlan mac
        when(mockEntity.obtainConnectDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainConnectTime()).thenReturn(50400);

        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity);

        // Act
        String sql = event.obtainFirstSqlQuery();

        // Assert
        String expected = """
                select '12345' as `device_mac`, 'Test-Device' as `device_name`, '1921681001' as `device_ip`, '-9223372036854775808' as `device_wlan_mac`, '2023-10-02' as `date_connect`, '50400' as `time_connect`
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryStaticWithMultipleEvents() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ArubaAiClientInfoEntity entity2 = createEntity(67890L, "Device-2", 1921681002, 12345L, "2023-10-03", 54000);

        ClientWlanConnectEvent event1 = new ClientWlanConnectEvent(entity1);
        ClientWlanConnectEvent event2 = new ClientWlanConnectEvent(entity2);

        List<ClientWlanConnectEvent> events = Arrays.asList(event1, event2);

        // Act
        String sql = ClientWlanConnectEvent.obtainSqlQuery(events);

        // Assert
        String expected1 = """
                select '12345' as `device_mac`, 'Device-1' as `device_name`, '1921681001' as `device_ip`, '67890' as `device_wlan_mac`, '2023-10-02' as `date_connect`, '50400' as `time_connect`
                """.trim();
        String expected2 = """
                union all
                select '67890', 'Device-2', '1921681002', '12345', '2023-10-03', '54000'
                """.trim();

        String result = sql.trim();
        assertTrue(result.contains(expected1));
        assertTrue(result.contains(expected2));
    }

    @Test
    void testObtainSqlQueryStaticWithSingleEvent() {
        // Arrange
        ArubaAiClientInfoEntity entity = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(entity);

        List<ClientWlanConnectEvent> events = Arrays.asList(event);

        // Act
        String sql = ClientWlanConnectEvent.obtainSqlQuery(events);

        // Assert
        String expected = """
                select '12345' as `device_mac`, 'Device-1' as `device_name`, '1921681001' as `device_ip`, '67890' as `device_wlan_mac`, '2023-10-02' as `date_connect`, '50400' as `time_connect`
                """.trim();
        assertEquals(expected, sql.trim());
    }

    @Test
    void testObtainSqlQueryStaticWithEmptyList() {
        // Arrange
        List<ClientWlanConnectEvent> events = Collections.emptyList();

        // Act
        String sql = ClientWlanConnectEvent.obtainSqlQuery(events);

        // Assert
        assertEquals("", sql);
    }

    @Test
    void testObtainSqlQueryStaticWithNullList() {
        // Arrange & Act & Assert
        assertThrows(NullPointerException.class, () -> {
            ClientWlanConnectEvent.obtainSqlQuery(null);
        });
    }

    @Test
    void testObtainSqlQueryStaticWithNullEventInList() {
        // Arrange
        ArubaAiClientInfoEntity entity = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(entity);

        List<ClientWlanConnectEvent> events = Arrays.asList(event, null);

        // Act
        String sql = ClientWlanConnectEvent.obtainSqlQuery(events);

        // Assert
        // Should only include non-null events
        String expected = """
                select '12345' as `device_mac`, 'Device-1' as `device_name`, '1921681001' as `device_ip`, '67890' as `device_wlan_mac`, '2023-10-02' as `date_connect`, '50400' as `time_connect`
                """.trim();
        assertEquals(expected, sql.trim());
    }

    @Test
    void testObtainSqlQueryStaticWithAllNullEvents() {
        // Arrange
        List<ClientWlanConnectEvent> events = Arrays.asList(null, null, null);

        // Act
        String sql = ClientWlanConnectEvent.obtainSqlQuery(events);

        // Assert
        assertEquals("", sql);
    }

    @Test
    void testGetters() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceIp()).thenReturn(1921681001);
        when(mockEntity.getDeviceWlanMac()).thenReturn(67890L);
        when(mockEntity.obtainConnectDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainConnectTime()).thenReturn(50400);

        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity);

        // Act & Assert - Test getters
        assertEquals(12345L, event.getDeviceMac());
        assertEquals("Test-Device", event.getDeviceName());
        assertEquals(1921681001, event.getDeviceIp());
        assertEquals(67890L, event.getDeviceWlanMac());
        assertEquals("2023-10-02", event.getDateConnect());
        assertEquals(50400, event.getTimeSecondConnect());
    }

    @Test
    void testFieldValues() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ArubaAiClientInfoEntity entity2 = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ArubaAiClientInfoEntity entity3 = createEntity(67890L, "Device-2", 1921681002, 12345L, "2023-10-03", 54000);

        ClientWlanConnectEvent event1 = new ClientWlanConnectEvent(entity1);
        ClientWlanConnectEvent event2 = new ClientWlanConnectEvent(entity2);
        ClientWlanConnectEvent event3 = new ClientWlanConnectEvent(entity3);

        // Act & Assert
        // Test that objects with same field values have same field values
        assertEquals(event1.getDeviceMac(), event2.getDeviceMac());
        assertEquals(event1.getDeviceName(), event2.getDeviceName());
        assertEquals(event1.getDeviceIp(), event2.getDeviceIp());
        assertEquals(event1.getDeviceWlanMac(), event2.getDeviceWlanMac());
        assertEquals(event1.getDateConnect(), event2.getDateConnect());
        assertEquals(event1.getTimeSecondConnect(), event2.getTimeSecondConnect());

        // Test that objects with different field values have different field values
        assertNotEquals(event1.getDeviceMac(), event3.getDeviceMac());
        assertNotEquals(event1.getDeviceName(), event3.getDeviceName());
    }

    @Test
    void testEqualsWithNull() {
        // Arrange
        ArubaAiClientInfoEntity entity = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(entity);

        // Act & Assert
        assertNotEquals(event, null);
        assertFalse(event.equals("not a ClientWlanConnectEvent object"));
    }

    @Test
    void testHashCodeConsistency() {
        // Arrange
        ArubaAiClientInfoEntity entity = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(entity);

        // Act & Assert
        int initialHashCode = event.hashCode();
        int secondHashCode = event.hashCode();

        // Hash code should be consistent across multiple calls
        assertEquals(initialHashCode, secondHashCode);
    }

    // Helper method to create ArubaAiClientInfoEntity for testing
    private ArubaAiClientInfoEntity createEntity(Long deviceMac, String deviceName, Integer deviceIp,
                                               Long deviceWlanMac, String connectDate, Integer connectTime) {
        ArubaAiClientInfoEntity entity = new ArubaAiClientInfoEntity();
        // Note: Since we don't have access to the actual entity implementation,
        // we'll use Mockito to create a mock entity for testing
        ArubaAiClientInfoEntity mockEntity = org.mockito.Mockito.mock(ArubaAiClientInfoEntity.class);
        org.mockito.Mockito.when(mockEntity.getDeviceMac()).thenReturn(deviceMac);
        org.mockito.Mockito.when(mockEntity.getDeviceName()).thenReturn(deviceName);
        org.mockito.Mockito.when(mockEntity.getDeviceIp()).thenReturn(deviceIp);
        org.mockito.Mockito.when(mockEntity.getDeviceWlanMac()).thenReturn(deviceWlanMac);
        org.mockito.Mockito.when(mockEntity.obtainConnectDate()).thenReturn(connectDate);
        org.mockito.Mockito.when(mockEntity.obtainConnectTime()).thenReturn(connectTime);
        return mockEntity;
    }
}
