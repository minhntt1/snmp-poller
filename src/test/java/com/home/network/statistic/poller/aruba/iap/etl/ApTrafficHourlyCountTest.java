package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiWlanTrafficEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ApTrafficHourlyCountTest {

    @Mock
    private ArubaAiWlanTrafficEntity mockEntity;

    @Mock
    private ArubaAiWlanTrafficEntity mockOldEntity;

    @Mock
    private ArubaAiWlanTrafficEntity mockNewEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConstructor() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollHour()).thenReturn(50400); // 14:00:00 in seconds
        when(mockEntity.getWlanMac()).thenReturn(12345L);
        when(mockEntity.getWlanEssid()).thenReturn("Test-ESSID");

        // Act
        ApTrafficHourlyCount count = new ApTrafficHourlyCount(mockEntity);

        // Assert
        assertEquals("2023-10-02", count.getApDate());
        assertEquals(50400, count.getApHour());
        assertEquals(12345L, count.getApWlanMac());
        assertEquals("Test-ESSID", count.getApWlanEssid());
        assertEquals(0L, count.getApWlanRxTotal());
        assertEquals(0L, count.getApWlanTxTotal());
    }

    @Test
    void testObtainFirstSqlValues() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollHour()).thenReturn(50400);
        when(mockEntity.getWlanMac()).thenReturn(12345L);
        when(mockEntity.getWlanEssid()).thenReturn("Test-ESSID");

        ApTrafficHourlyCount count = new ApTrafficHourlyCount(mockEntity);
        count.setApWlanRxTotal(1000L);
        count.setApWlanTxTotal(2000L);

        // Act
        String sql = count.obtainFirstSqlValues();

        // Assert
        String expected = """
                select
                '2023-10-02' as `date`, '50400' as `hour`, '12345' as `wlan_mac`, 'Test-ESSID' as `wlan_essid`, '3000' as `transmission_bytes_val`
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlValues() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollHour()).thenReturn(50400);
        when(mockEntity.getWlanMac()).thenReturn(12345L);
        when(mockEntity.getWlanEssid()).thenReturn("Test-ESSID");

        ApTrafficHourlyCount count = new ApTrafficHourlyCount(mockEntity);
        count.setApWlanRxTotal(1500L);
        count.setApWlanTxTotal(2500L);

        // Act
        String sql = count.obtainSqlValues();

        // Assert
        String expected = """
                union all
                select '2023-10-02', '50400', '12345', 'Test-ESSID', '4000'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testUpdateTrafficWithValidDiff() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollHour()).thenReturn(50400);
        when(mockEntity.getWlanMac()).thenReturn(12345L);
        when(mockEntity.getWlanEssid()).thenReturn("Test-ESSID");

        ApTrafficHourlyCount count = new ApTrafficHourlyCount(mockEntity);

        // Mock old entity (lower values)
        when(mockOldEntity.getWlanTx()).thenReturn(1000L);
        when(mockOldEntity.getWlanRx()).thenReturn(500L);

        // Mock new entity (higher values)
        when(mockNewEntity.getWlanTx()).thenReturn(3000L);
        when(mockNewEntity.getWlanRx()).thenReturn(1500L);

        // Act
        count.updateTraffic(mockOldEntity, mockNewEntity);

        // Assert
        assertEquals(2000L, count.getApWlanTxTotal()); // 3000 - 1000
        assertEquals(1000L, count.getApWlanRxTotal()); // 1500 - 500
    }

    @Test
    void testUpdateTrafficWithRollover() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollHour()).thenReturn(50400);
        when(mockEntity.getWlanMac()).thenReturn(12345L);
        when(mockEntity.getWlanEssid()).thenReturn("Test-ESSID");

        ApTrafficHourlyCount count = new ApTrafficHourlyCount(mockEntity);

        // Mock old entity (higher values - indicates rollover)
        when(mockOldEntity.getWlanTx()).thenReturn(4000L);
        when(mockOldEntity.getWlanRx()).thenReturn(2000L);

        // Mock new entity (lower values - indicates rollover)
        when(mockNewEntity.getWlanTx()).thenReturn(500L);
        when(mockNewEntity.getWlanRx()).thenReturn(100L);

        // Act
        count.updateTraffic(mockOldEntity, mockNewEntity);

        // Assert
        assertEquals(500L, count.getApWlanTxTotal()); // Uses new value when old > new
        assertEquals(100L, count.getApWlanRxTotal()); // Uses new value when old > new
    }

    @Test
    void testUpdateTrafficMultipleUpdates() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollHour()).thenReturn(50400);
        when(mockEntity.getWlanMac()).thenReturn(12345L);
        when(mockEntity.getWlanEssid()).thenReturn("Test-ESSID");

        ApTrafficHourlyCount count = new ApTrafficHourlyCount(mockEntity);

        // First update
        ArubaAiWlanTrafficEntity oldEntity1 = ArubaAiWlanTrafficEntity.builder()
                .wlanTx(1000L).wlanRx(500L).build();
        ArubaAiWlanTrafficEntity newEntity1 = ArubaAiWlanTrafficEntity.builder()
                .wlanTx(2000L).wlanRx(1000L).build();
        count.updateTraffic(oldEntity1, newEntity1);

        // Second update
        ArubaAiWlanTrafficEntity oldEntity2 = ArubaAiWlanTrafficEntity.builder()
                .wlanTx(2000L).wlanRx(1000L).build();
        ArubaAiWlanTrafficEntity newEntity2 = ArubaAiWlanTrafficEntity.builder()
                .wlanTx(3500L).wlanRx(1800L).build();
        count.updateTraffic(oldEntity2, newEntity2);

        // Assert
        assertEquals(2500L, count.getApWlanTxTotal()); // (2000-1000) + (3500-2000) = 1000 + 1500 = 2500
        assertEquals(1300L, count.getApWlanRxTotal()); // (1000-500) + (1800-1000) = 500 + 800 = 1300
    }

    @Test
    void testObtainSqlValuesStatic() {
        // Arrange
        LocalDateTime pollTime1 = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiWlanTrafficEntity entity1 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(pollTime1)
                .wlanMac(12345L)
                .wlanEssid("Test-ESSID-1")
                .build();
        ApTrafficHourlyCount count1 = new ApTrafficHourlyCount(entity1);
        count1.setApWlanRxTotal(1000L);
        count1.setApWlanTxTotal(2000L);

        LocalDateTime pollTime2 = LocalDateTime.of(2023, 10, 2, 15, 30);
        ArubaAiWlanTrafficEntity entity2 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(pollTime2)
                .wlanMac(67890L)
                .wlanEssid("Test-ESSID-2")
                .build();
        ApTrafficHourlyCount count2 = new ApTrafficHourlyCount(entity2);
        count2.setApWlanRxTotal(1500L);
        count2.setApWlanTxTotal(2500L);

        Map<ApTrafficHourlyCount, ApTrafficHourlyCount> map = new LinkedHashMap<>();
        map.put(count1, count1);
        map.put(count2, count2);

        // Act
        String sql = ApTrafficHourlyCount.obtainSqlValues(map);

        // Assert
        String expected1 = """
                select
                '2023-10-02' as `date`, '50400' as `hour`, '12345' as `wlan_mac`, 'Test-ESSID-1' as `wlan_essid`, '3000' as `transmission_bytes_val`
                """.trim();
        String expected2 = """
                union all
                select '2023-10-02', '54000', '67890', 'Test-ESSID-2', '4000'
                """.trim();

        String result = sql.trim();
        assertTrue(result.contains(expected1));
        assertTrue(result.contains(expected2));
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiWlanTrafficEntity entity1 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(pollTime)
                .wlanMac(12345L)
                .wlanEssid("Test-ESSID")
                .build();
        ApTrafficHourlyCount count1 = new ApTrafficHourlyCount(entity1);

        ArubaAiWlanTrafficEntity entity2 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(pollTime)
                .wlanMac(12345L)
                .wlanEssid("Test-ESSID")
                .build();
        ApTrafficHourlyCount count2 = new ApTrafficHourlyCount(entity2);

        ArubaAiWlanTrafficEntity entity3 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(LocalDateTime.of(2023, 10, 2, 15, 30))
                .wlanMac(67890L)
                .wlanEssid("Different-ESSID")
                .build();
        ApTrafficHourlyCount count3 = new ApTrafficHourlyCount(entity3);

        // Act & Assert
        assertEquals(count1, count2);
        assertEquals(count1.hashCode(), count2.hashCode());
        assertNotEquals(count1, count3);
        assertNotEquals(count1.hashCode(), count3.hashCode());
    }

    @Test
    void testEqualsDifferentDate() {
        // Arrange
        ArubaAiWlanTrafficEntity entity1 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(LocalDateTime.of(2023, 10, 2, 14, 30))
                .wlanMac(12345L)
                .wlanEssid("Test-ESSID")
                .build();
        ApTrafficHourlyCount count1 = new ApTrafficHourlyCount(entity1);

        ArubaAiWlanTrafficEntity entity2 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(LocalDateTime.of(2023, 10, 3, 14, 30)) // Different date
                .wlanMac(12345L)
                .wlanEssid("Test-ESSID")
                .build();
        ApTrafficHourlyCount count2 = new ApTrafficHourlyCount(entity2);

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testEqualsDifferentHour() {
        // Arrange
        ArubaAiWlanTrafficEntity entity1 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(LocalDateTime.of(2023, 10, 2, 14, 30))
                .wlanMac(12345L)
                .wlanEssid("Test-ESSID")
                .build();
        ApTrafficHourlyCount count1 = new ApTrafficHourlyCount(entity1);

        ArubaAiWlanTrafficEntity entity2 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(LocalDateTime.of(2023, 10, 2, 15, 30)) // Different hour
                .wlanMac(12345L)
                .wlanEssid("Test-ESSID")
                .build();
        ApTrafficHourlyCount count2 = new ApTrafficHourlyCount(entity2);

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testEqualsDifferentWlanMac() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiWlanTrafficEntity entity1 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(pollTime)
                .wlanMac(12345L)
                .wlanEssid("Test-ESSID")
                .build();
        ApTrafficHourlyCount count1 = new ApTrafficHourlyCount(entity1);

        ArubaAiWlanTrafficEntity entity2 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(pollTime)
                .wlanMac(67890L) // Different MAC
                .wlanEssid("Test-ESSID")
                .build();
        ApTrafficHourlyCount count2 = new ApTrafficHourlyCount(entity2);

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testEqualsDifferentWlanEssid() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        ArubaAiWlanTrafficEntity entity1 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(pollTime)
                .wlanMac(12345L)
                .wlanEssid("Test-ESSID")
                .build();
        ApTrafficHourlyCount count1 = new ApTrafficHourlyCount(entity1);

        ArubaAiWlanTrafficEntity entity2 = ArubaAiWlanTrafficEntity.builder()
                .pollTime(pollTime)
                .wlanMac(12345L)
                .wlanEssid("Different-ESSID") // Different ESSID
                .build();
        ApTrafficHourlyCount count2 = new ApTrafficHourlyCount(entity2);

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testGettersAndSetters() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 14, 30);
        when(mockEntity.obtainPollDate()).thenReturn("2023-10-02");
        when(mockEntity.obtainPollHour()).thenReturn(50400);
        when(mockEntity.getWlanMac()).thenReturn(12345L);
        when(mockEntity.getWlanEssid()).thenReturn("Test-ESSID");

        ApTrafficHourlyCount count = new ApTrafficHourlyCount(mockEntity);

        // Act
        count.setApWlanRxTotal(5000L);
        count.setApWlanTxTotal(8000L);

        // Assert
        assertEquals(5000L, count.getApWlanRxTotal());
        assertEquals(8000L, count.getApWlanTxTotal());
    }
}
