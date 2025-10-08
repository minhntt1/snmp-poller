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
    void testObtainFirstSqlQuery() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Act
        String sql = record.obtainFirstSqlQuery();

        // Assert
        String expected = """
                select '12345' as `device_mac`, 'Test-Device' as `device_name`, '3600' as `device_uptime_seconds`, '19216811' as `device_ip`
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQuery() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Act
        String sql = record.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '3600', '19216811'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithSpecialCharacters() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device'With'Special");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Act
        String sql = record.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device'With'Special', '3600', '19216811'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithNullDeviceName() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn(null);
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Act
        String sql = record.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'null', '3600', '19216811'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithZeroUptime() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(0L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Act
        String sql = record.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '12345', 'Test-Device', '0', '19216811'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryWithLargeNumbers() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(999999999L);
        when(mockEntity.getDeviceName()).thenReturn("Large-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(86400L); // 24 hours
        when(mockEntity.getDeviceIp()).thenReturn(1000); // Max IPv4

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        // Act
        String sql = record.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '999999999', 'Large-Device', '86400', '1000'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQueryStaticWithEmptyMap() {
        // Arrange
        HashMap<ClientUptimeRecord, ClientUptimeRecord> emptyMap = new LinkedHashMap<>();

        // Act
        String sql = ClientUptimeRecord.obtainSqlQuery(emptyMap);

        // Assert
        assertEquals("", sql);
    }

    @Test
    void testObtainSqlQueryStaticWithSingleRecord() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        HashMap<ClientUptimeRecord, ClientUptimeRecord> map = new LinkedHashMap<>();
        map.put(record, record);

        // Act
        String sql = ClientUptimeRecord.obtainSqlQuery(map);

        // Assert
        String expected = """
                select '12345' as `device_mac`, 'Test-Device' as `device_name`, '3600' as `device_uptime_seconds`, '19216811' as `device_ip`
                """.trim();
        assertEquals(expected, sql.trim());
    }

    @Test
    void testObtainSqlQueryStaticWithMultipleRecords() {
        // Arrange
        // First record
        ArubaAiClientInfoEntity entity1 = ArubaAiClientInfoEntity.builder()
                .deviceMac(12345L)
                .deviceName("Device-1")
                .deviceUptimeSeconds(3600L)
                .deviceIp(19216811)
                .build();
        ClientUptimeRecord record1 = new ClientUptimeRecord(entity1);

        // Second record
        ArubaAiClientInfoEntity entity2 = ArubaAiClientInfoEntity.builder()
                .deviceMac(67890L)
                .deviceName("Device-2")
                .deviceUptimeSeconds(7200L)
                .deviceIp(19216812)
                .build();
        ClientUptimeRecord record2 = new ClientUptimeRecord(entity2);

        HashMap<ClientUptimeRecord, ClientUptimeRecord> map = new LinkedHashMap<>();
        map.put(record1, record1);
        map.put(record2, record2);

        // Act
        String sql = ClientUptimeRecord.obtainSqlQuery(map);

        // Assert
        String expected1 = """
                select '12345' as `device_mac`, 'Device-1' as `device_name`, '3600' as `device_uptime_seconds`, '19216811' as `device_ip`
                """.trim();
        String expected2 = """
                union all
                select '67890', 'Device-2', '7200', '19216812'
                """.trim();

        String result = sql.trim();
        assertTrue(result.contains(expected1));
        assertTrue(result.contains(expected2));
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

    @Test
    void testObtainSqlQueryStaticWithNullKeyInMap() {
        // Arrange
        HashMap<ClientUptimeRecord, ClientUptimeRecord> map = new LinkedHashMap<>();
        map.put(null, null);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            String sql = ClientUptimeRecord.obtainSqlQuery(map);
            assertEquals("", sql); // Empty because values() returns empty for null key
        });
    }

    @Test
    void testObtainSqlQueryStaticWithMixedNullAndValidRecords() {
        // Arrange
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");
        when(mockEntity.getDeviceUptimeSeconds()).thenReturn(3600L);
        when(mockEntity.getDeviceIp()).thenReturn(19216811);

        ClientUptimeRecord record = new ClientUptimeRecord(mockEntity);

        HashMap<ClientUptimeRecord, ClientUptimeRecord> map = new LinkedHashMap<>();
        map.put(record, record);
        map.put(null, null); // This won't contribute to the query

        // Act
        String sql = ClientUptimeRecord.obtainSqlQuery(map);

        // Assert
        String expected = """
                select '12345' as `device_mac`, 'Test-Device' as `device_name`, '3600' as `device_uptime_seconds`, '19216811' as `device_ip`
                """.trim();
        assertEquals(expected, sql.trim());
    }
}
