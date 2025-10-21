package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ClientUptimeRecordTest {

    @Mock
    private ArubaAiClientInfoEntity mockEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConstructor() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        // Act
        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Assert
        assertEquals(12345L, record.getDeviceMac());
        assertEquals("Test-Device", record.getDeviceName());
        assertEquals(3600L, record.getDeviceUptimeSeconds());
        assertEquals(19216811, record.getDeviceIp());
    }

    @Test
    void testUpdateDeviceUptimeSeconds() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Update with new uptime
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(7200L);

        // Act
        record.updateDeviceUptimeSeconds(mockEntity);

        // Assert
        assertEquals(7200L, record.getDeviceUptimeSeconds());
    }

    @Test
    void testEqualsWithSameObject() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Act & Assert
        assertEquals(record, record);
    }

    @Test
    void testEqualsWithEqualObjects() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Test-Device")
                .deviceUptimeSeconds(3600L)
                .deviceIp(19216811)
                .build();
        ClientUptimeRecord record1 = new ClientUptimeRecord(entity1);

        ArubaAiClientInfoEntity entity2 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Test-Device")
                .deviceUptimeSeconds(7200L) // Different uptime but same mac/name
                .deviceIp(19216812) // Different IP but same mac/name
                .build();
        ClientUptimeRecord record2 = new ClientUptimeRecord(entity2);

        // Act & Assert
        assertEquals(record1, record2);
    }

    @Test
    void testEqualsWithDifferentDeviceMac() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Test-Device")
                .build();
        ClientUptimeRecord record1 = new ClientUptimeRecord(entity1);

        ArubaAiClientInfoEntity entity2 = ArubaAiClientInfoEntity.builder()
                .deviceMac(67890L)
                .deviceName("Test-Device")
                .build();
        ClientUptimeRecord record2 = new ClientUptimeRecord(entity2);

        // Act & Assert
        assertNotEquals(record1, record2);
    }

    @Test
    void testEqualsWithDifferentDeviceName() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Test-Device")
                .build();
        ClientUptimeRecord record1 = new ClientUptimeRecord(entity1);

        ArubaAiClientInfoEntity entity2 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Different-Device")
                .build();
        ClientUptimeRecord record2 = new ClientUptimeRecord(entity2);

        // Act & Assert
        assertNotEquals(record1, record2);
    }

    @Test
    void testEqualsWithNullObject() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Act & Assert
        assertNotEquals(record, null);
        assertFalse(record.equals("not a ClientUptimeRecord object"));
    }

    @Test
    void testHashCodeConsistency() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Test-Device")
                .deviceUptimeSeconds(3600L)
                .deviceIp(19216811)
                .build();
        ClientUptimeRecord record1 = new ClientUptimeRecord(entity1);

        ArubaAiClientInfoEntity entity2 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Test-Device")
                .deviceUptimeSeconds(7200L)
                .deviceIp(19216812)
                .build();
        ClientUptimeRecord record2 = new ClientUptimeRecord(entity2);

        // Act & Assert
        assertEquals(record1.hashCode(), record2.hashCode());
    }

    @Test
    void testHashCodeDifferentObjects() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Test-Device")
                .build();
        ClientUptimeRecord record1 = new ClientUptimeRecord(entity1);

        ArubaAiClientInfoEntity entity2 = ArubaAiClientInfoEntity.builder()
                .deviceMac(67890L)
                .deviceName("Test-Device")
                .build();
        ClientUptimeRecord record2 = new ClientUptimeRecord(entity2);

        ArubaAiClientInfoEntity entity3 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Different-Device")
                .build();
        ClientUptimeRecord record3 = new ClientUptimeRecord(entity3);

        // Act & Assert
        assertNotEquals(record1.hashCode(), record2.hashCode());
        assertNotEquals(record1.hashCode(), record3.hashCode());
    }

    @Test
    void testHashCodeConsistencyAfterUpdate() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Act
        int initialHashCode = record.hashCode();

        // Update uptime (should not affect hash code since it's not part of equals/hashCode)
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(7200L);
        record.updateDeviceUptimeSeconds(mockEntity);

        // Assert
        assertEquals(initialHashCode, record.hashCode());
    }

    @Test
    void testEqualsAndHashCodeContract() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Test-Device")
                .build();
        ClientUptimeRecord record1 = new ClientUptimeRecord(entity1);

        ArubaAiClientInfoEntity entity2 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Test-Device")
                .build();
        ClientUptimeRecord record2 = new ClientUptimeRecord(entity2);

        // Act & Assert
        // Test that equal objects have equal hash codes
        assertEquals(record1, record2);
        assertEquals(record1.hashCode(), record2.hashCode());

        // Test that hash code is consistent
        assertEquals(record1.hashCode(), record1.hashCode());
        assertEquals(record2.hashCode(), record2.hashCode());
    }

    @Test
    void testConstructorWithNullValues() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(null);
        when(mockEntity.getDeviceName()).thenReturn(null);
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(null);
        when(mockEntity.getDeviceIp()).thenReturn(null);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);
            assertNull(record.getDeviceMac());
            assertNull(record.getDeviceName());
            assertNull(record.getDeviceUptimeSeconds());
            assertNull(record.getDeviceIp());
        });
    }

    @Test
    void testUpdateDeviceUptimeSecondsWithNull() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Update with null uptime
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(null);

        // Act
        record.updateDeviceUptimeSeconds(mockEntity);

        // Assert
        assertNull(record.getDeviceUptimeSeconds());
    }
}
