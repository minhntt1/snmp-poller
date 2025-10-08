package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientTrafficHourlyCountTest {

    @Mock
    private ArubaAiClientInfoEntity mockEntity;

    @Mock
    private ArubaAiClientInfoEntity mockOldEntity;

    @Mock
    private ArubaAiClientInfoEntity mockNewEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConstructor() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollTimeHour()).thenReturn(50400); // 14:00:00 in seconds
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");

        // Act
        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(mockEntity);

        // Assert
        assertEquals("2023-10-02", count.getDate());
        assertEquals(50400, count.getTimeSecond());
        assertEquals(12345L, count.getDeviceMac());
        assertEquals("Test-Device", count.getDeviceName());
        assertEquals(0L, count.getTx());
        assertEquals(0L, count.getRx());
    }

    @Test
    void testObtainFirstSqlQuery() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollTimeHour()).thenReturn(50400);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");

        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(mockEntity);
        count.setTx(1000L);
        count.setRx(2000L);

        // Act
        String sql = count.obtainFirstSqlQuery();

        // Assert
        String expected = """
                select '2023-10-02' as `date`, '50400' as `time`, '12345' as `device_mac`, 'Test-Device' as `device_name`, '3000' as `transmission_bytes_val`
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlQuery() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollTimeHour()).thenReturn(50400);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");

        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(mockEntity);
        count.setTx(1500L);
        count.setRx(2500L);

        // Act
        String sql = count.obtainSqlQuery();

        // Assert
        String expected = """
                union all
                select '2023-10-02', '50400', '12345', 'Test-Device', '4000'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testAdjustTrafficWithValidDiff() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollTimeHour()).thenReturn(50400);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");

        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(mockEntity);

        // Mock old entity (lower values)
        when(mockOldEntity.getDeviceTx()).thenReturn(1000L);
        when(mockOldEntity.getDeviceRx()).thenReturn(500L);
        when(mockOldEntity.calcDiffTxOldNew(mockNewEntity)).thenReturn(2000L);
        when(mockOldEntity.calcDiffRxOldNew(mockNewEntity)).thenReturn(1000L);

        // Act
        count.adjustTraffic(mockOldEntity, mockNewEntity);

        // Assert
        assertEquals(2000L, count.getTx());
        assertEquals(1000L, count.getRx());
    }

    @Test
    void testAdjustTrafficWithReconnect() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollTimeHour()).thenReturn(50400);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");

        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(mockEntity);

        // Mock old entity
        when(mockOldEntity.getDeviceTx()).thenReturn(4000L);
        when(mockOldEntity.getDeviceRx()).thenReturn(2000L);
        when(mockOldEntity.calcDiffTxOldNew(mockNewEntity)).thenReturn(500L); // New value due to reconnect
        when(mockOldEntity.calcDiffRxOldNew(mockNewEntity)).thenReturn(100L); // New value due to reconnect

        // Act
        count.adjustTraffic(mockOldEntity, mockNewEntity);

        // Assert
        assertEquals(500L, count.getTx());
        assertEquals(100L, count.getRx());
    }

    @Test
    void testAdjustTrafficMultipleUpdates() {
        // Arrange
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollTimeHour()).thenReturn(50400);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");

        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(mockEntity);

        // First update - use mock entities
        ArubaAiClientInfoEntity oldEntity1 = mock(ArubaAiClientInfoEntity.class);
        ArubaAiClientInfoEntity newEntity1 = mock(ArubaAiClientInfoEntity.class);
        when(oldEntity1.calcDiffTxOldNew(newEntity1)).thenReturn(1000L);
        when(oldEntity1.calcDiffRxOldNew(newEntity1)).thenReturn(500L);
        count.adjustTraffic(oldEntity1, newEntity1);

        // Second update - use mock entities
        ArubaAiClientInfoEntity oldEntity2 = mock(ArubaAiClientInfoEntity.class);
        ArubaAiClientInfoEntity newEntity2 = mock(ArubaAiClientInfoEntity.class);
        when(oldEntity2.calcDiffTxOldNew(newEntity2)).thenReturn(1500L);
        when(oldEntity2.calcDiffRxOldNew(newEntity2)).thenReturn(800L);
        count.adjustTraffic(oldEntity2, newEntity2);

        // Assert
        assertEquals(2500L, count.getTx()); // 1000 + 1500
        assertEquals(1300L, count.getRx()); // 500 + 800
    }

    @Test
    void testObtainSqlQueryStatic() {
        // Arrange
        LocalDateTime pollTime1 = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiClientInfoEntity entity1 = createEntity(pollTime1, 12345L, "Device-1");
        ClientTrafficHourlyCount count1 = new ClientTrafficHourlyCount(entity1);
        count1.setTx(1000L);
        count1.setRx(2000L);

        LocalDateTime pollTime2 = LocalDateTime.of(2023, 10, 2, 15, 30);
        ArubaAiClientInfoEntity entity2 = createEntity(pollTime2, 67890L, "Device-2");
        ClientTrafficHourlyCount count2 = new ClientTrafficHourlyCount(entity2);
        count2.setTx(1500L);
        count2.setRx(2500L);

        HashMap<ClientTrafficHourlyCount, ClientTrafficHourlyCount> map = new LinkedHashMap<>();
        map.put(count1, count1);
        map.put(count2, count2);

        // Act
        String sql = ClientTrafficHourlyCount.obtainSqlQuery(map);

        // Assert
        String expected1 = """
                select '2023-10-02' as `date`, '50400' as `time`, '12345' as `device_mac`, 'Device-1' as `device_name`, '3000' as `transmission_bytes_val`
                """.trim();
        String expected2 = """
                union all
                select '2023-10-02', '54000', '67890', 'Device-2', '4000'
                """.trim();

        String result = sql.trim();
        assertTrue(result.contains(expected1));
        assertTrue(result.contains(expected2));
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiClientInfoEntity entity1 = createEntity(pollTime, 12345L, "Test-Device");
        ClientTrafficHourlyCount count1 = new ClientTrafficHourlyCount(entity1);

        ArubaAiClientInfoEntity entity2 = createEntity(pollTime, 12345L, "Test-Device");
        ClientTrafficHourlyCount count2 = new ClientTrafficHourlyCount(entity2);

        ArubaAiClientInfoEntity entity3 = createEntity(LocalDateTime.of(2023, 10, 2, 15, 30), 67890L, "Different-Device");
        ClientTrafficHourlyCount count3 = new ClientTrafficHourlyCount(entity3);

        // Act & Assert
        assertEquals(count1, count2);
        assertEquals(count1.hashCode(), count2.hashCode());
        assertNotEquals(count1, count3);
        assertNotEquals(count1.hashCode(), count3.hashCode());
    }

    @Test
    void testEqualsDifferentDate() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = createEntity(LocalDateTime.of(2023, 10, 2, 14, 30), 12345L, "Test-Device");
        ClientTrafficHourlyCount count1 = new ClientTrafficHourlyCount(entity1);

        ArubaAiClientInfoEntity entity2 = createEntity(LocalDateTime.of(2023, 10, 3, 14, 30), 12345L, "Test-Device");
        ClientTrafficHourlyCount count2 = new ClientTrafficHourlyCount(entity2);

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testEqualsDifferentHour() {
        // Arrange
        ArubaAiClientInfoEntity entity1 = createEntity(LocalDateTime.of(2023, 10, 2, 14, 30), 12345L, "Test-Device");
        ClientTrafficHourlyCount count1 = new ClientTrafficHourlyCount(entity1);

        ArubaAiClientInfoEntity entity2 = createEntity(LocalDateTime.of(2023, 10, 2, 15, 30), 12345L, "Test-Device");
        ClientTrafficHourlyCount count2 = new ClientTrafficHourlyCount(entity2);

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testEqualsDifferentDeviceMac() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiClientInfoEntity entity1 = createEntity(pollTime, 12345L, "Test-Device");
        ClientTrafficHourlyCount count1 = new ClientTrafficHourlyCount(entity1);

        ArubaAiClientInfoEntity entity2 = createEntity(pollTime, 67890L, "Test-Device");
        ClientTrafficHourlyCount count2 = new ClientTrafficHourlyCount(entity2);

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testEqualsDifferentDeviceName() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiClientInfoEntity entity1 = createEntity(pollTime, 12345L, "Test-Device");
        ClientTrafficHourlyCount count1 = new ClientTrafficHourlyCount(entity1);

        ArubaAiClientInfoEntity entity2 = createEntity(pollTime, 12345L, "Different-Device");
        ClientTrafficHourlyCount count2 = new ClientTrafficHourlyCount(entity2);

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testGettersAndSetters() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollTimeHour()).thenReturn(50400);
        when(mockEntity.getDeviceMac()).thenReturn(12345L);
        when(mockEntity.getDeviceName()).thenReturn("Test-Device");

        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(mockEntity);

        // Act
        count.setTx(5000L);
        count.setRx(8000L);

        // Assert
        assertEquals(5000L, count.getTx());
        assertEquals(8000L, count.getRx());
    }

    @Test
    void testEqualsWithNullObject() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiClientInfoEntity entity = createEntity(pollTime, 12345L, "Test-Device");
        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(entity);

        // Act & Assert
        assertNotEquals(count, null);
        assertFalse(count.equals("not a ClientTrafficHourlyCount object"));
    }

    @Test
    void testHashCodeConsistency() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiClientInfoEntity entity = createEntity(pollTime, 12345L, "Test-Device");
        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(entity);

        // Act & Assert
        int initialHashCode = count.hashCode();
        count.setTx(1000L);
        count.setRx(2000L);

        // Hash code should remain the same as it doesn't depend on tx/rx values
        assertEquals(initialHashCode, count.hashCode());
    }

    @Test
    void testEmptyMapInStaticSqlQuery() {
        // Arrange
        HashMap<ClientTrafficHourlyCount, ClientTrafficHourlyCount> emptyMap = new LinkedHashMap<>();

        // Act
        String sql = ClientTrafficHourlyCount.obtainSqlQuery(emptyMap);

        // Assert
        assertEquals("", sql);
    }

    @Test
    void testSingleEntryInStaticSqlQuery() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiClientInfoEntity entity = createEntity(pollTime, 12345L, "Test-Device");
        ClientTrafficHourlyCount count = new ClientTrafficHourlyCount(entity);
        count.setTx(1000L);
        count.setRx(2000L);

        HashMap<ClientTrafficHourlyCount, ClientTrafficHourlyCount> map = new LinkedHashMap<>();
        map.put(count, count);

        // Act
        String sql = ClientTrafficHourlyCount.obtainSqlQuery(map);

        // Assert
        String expected = """
                select '2023-10-02' as `date`, '50400' as `time`, '12345' as `device_mac`, 'Test-Device' as `device_name`, '3000' as `transmission_bytes_val`
                """.trim();
        assertEquals(expected, sql.trim());
    }

    // Helper method to create ArubaAiClientInfoEntity for testing
    private ArubaAiClientInfoEntity createEntity(LocalDateTime pollTime, Long deviceMac, String deviceName) {
        return ArubaAiClientInfoEntity.builder()
                .pollTime(pollTime)
                .deviceMac(deviceMac)
                .deviceName(deviceName)
                .build();
    }

    // Helper method to create mock ArubaAiClientInfoEntity for testing
    private ArubaAiClientInfoEntity createMockEntity(Long deviceTx, Long deviceRx) {
        ArubaAiClientInfoEntity entity = ArubaAiClientInfoEntity.builder()
                .deviceTx(deviceTx)
                .deviceRx(deviceRx)
                .build();
        return entity;
    }
}
