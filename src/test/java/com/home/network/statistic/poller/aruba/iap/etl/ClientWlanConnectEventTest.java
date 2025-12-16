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
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity, true);

        // Assert
        assertEquals(12345L, event.getDeviceMac());
        assertEquals("Test-Device", event.getDeviceName());
        assertEquals(1921681001, event.getDeviceIp());
        assertEquals(67890L, event.getDeviceWlanMac());
        assertEquals("2023-10-02", event.getDateConnect());
        assertEquals(50400, event.getTimeSecondConnect());
        assertEquals(1, event.getConnectStatus()); // Connect status should be 1 for connect event
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
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity, true);

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
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity, true);

        // Assert
        assertEquals(Long.MIN_VALUE, event.getDeviceWlanMac()); // Should default to Long.MIN_VALUE
    }

    @Test
    void testConstructorWithDisconnectEvent() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceIp()).thenReturn(1921681001);
        when(mockEntity.getDeviceWlanMac()).thenReturn(67890L);
        // For disconnect, date and time are not obtained from entity

        // Act
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity, false);

        // Assert
        assertEquals(12345L, event.getDeviceMac());
        assertEquals("Test-Device", event.getDeviceName());
        assertEquals(1921681001, event.getDeviceIp());
        assertEquals(67890L, event.getDeviceWlanMac());
        assertEquals(2, event.getConnectStatus()); // Connect status should be 2 for disconnect event
        assertNotNull(event.getDateConnect()); // Date should be set to current UTC date
        assertNotNull(event.getTimeSecondConnect()); // Time should be set to current UTC time
        assertTrue(event.getTimeSecondConnect() >= 0 && event.getTimeSecondConnect() < 86400); // Valid second of day
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

        ClientWlanConnectEvent event = new ClientWlanConnectEvent(mockEntity, true);

        // Act & Assert - Test getters
        assertEquals(12345L, event.getDeviceMac());
        assertEquals("Test-Device", event.getDeviceName());
        assertEquals(1921681001, event.getDeviceIp());
        assertEquals(67890L, event.getDeviceWlanMac());
        assertEquals("2023-10-02", event.getDateConnect());
        assertEquals(50400, event.getTimeSecondConnect());
        assertEquals(1, event.getConnectStatus());
    }

    @Test
    void testFieldValues() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ArubaAiClientInfoEntity entity2 = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ArubaAiClientInfoEntity entity3 = createEntity(67890L, "Device-2", 1921681002, 12345L, "2023-10-03", 54000);

        ClientWlanConnectEvent event1 = new ClientWlanConnectEvent(entity1, true);
        ClientWlanConnectEvent event2 = new ClientWlanConnectEvent(entity2, true);
        ClientWlanConnectEvent event3 = new ClientWlanConnectEvent(entity3, true);

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
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(entity, true);

        // Act & Assert
        assertNotEquals(event, null);
        assertFalse(event.equals("not a ClientWlanConnectEvent object"));
    }

    @Test
    void testHashCodeConsistency() {
        // Arrange
        ArubaAiClientInfoEntity entity = createEntity(12345L, "Device-1", 1921681001, 67890L, "2023-10-02", 50400);
        ClientWlanConnectEvent event = new ClientWlanConnectEvent(entity, true);

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
