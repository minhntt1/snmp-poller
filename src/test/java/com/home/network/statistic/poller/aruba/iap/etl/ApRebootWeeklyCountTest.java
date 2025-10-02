package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiApInfoEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApRebootWeeklyCountTest {

    @Test
    void testConstructor() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 12, 0); // Monday is 2023-10-02
        ArubaAiApInfoEntity entity = ArubaAiApInfoEntity.builder()
                .pollTime(pollTime)
                .apMac(12345L)
                .apName("AP-001")
                .apIp(123456789)
                .apModel("AP-123")
                .apUptimeSeconds(3600L)
                .build();

        // Act
        ApRebootWeeklyCount count = new ApRebootWeeklyCount(entity);

        // Assert
        assertEquals("2023-10-02", count.getWeek());
        assertEquals(12345L, count.getApMac());
        assertEquals("AP-001", count.getApName());
        assertEquals(123456789, count.getApIp());
        assertEquals(0, count.getRebootCnt());
    }

    @Test
    void testAdjustRebootCntWithReboot() {
        // Arrange
        ArubaAiApInfoEntity oldEntity = ArubaAiApInfoEntity.builder()
                .apUptimeSeconds(7200L) // 2 hours
                .build();
        ArubaAiApInfoEntity newEntity = ArubaAiApInfoEntity.builder()
                .apUptimeSeconds(1800L) // 30 minutes - less than old, indicates reboot
                .build();
        ApRebootWeeklyCount count = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(LocalDateTime.now())
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );

        // Act
        count.adjustRebootCnt(oldEntity, newEntity);

        // Assert
        assertEquals(1, count.getRebootCnt());
    }

    @Test
    void testAdjustRebootCntWithoutReboot() {
        // Arrange
        ArubaAiApInfoEntity oldEntity = ArubaAiApInfoEntity.builder()
                .apUptimeSeconds(1800L) // 30 minutes
                .build();
        ArubaAiApInfoEntity newEntity = ArubaAiApInfoEntity.builder()
                .apUptimeSeconds(3600L) // 1 hour - greater than old, no reboot
                .build();
        ApRebootWeeklyCount count = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(LocalDateTime.now())
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );

        // Act
        count.adjustRebootCnt(oldEntity, newEntity);

        // Assert
        assertEquals(0, count.getRebootCnt());
    }

    @Test
    void testAdjustRebootCntMultipleReboots() {
        // Arrange
        ApRebootWeeklyCount count = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(LocalDateTime.now())
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );

        // First reboot
        ArubaAiApInfoEntity oldEntity1 = ArubaAiApInfoEntity.builder().apUptimeSeconds(7200L).build();
        ArubaAiApInfoEntity newEntity1 = ArubaAiApInfoEntity.builder().apUptimeSeconds(1800L).build();
        count.adjustRebootCnt(oldEntity1, newEntity1);

        // Second reboot
        ArubaAiApInfoEntity oldEntity2 = ArubaAiApInfoEntity.builder().apUptimeSeconds(3600L).build();
        ArubaAiApInfoEntity newEntity2 = ArubaAiApInfoEntity.builder().apUptimeSeconds(900L).build();
        count.adjustRebootCnt(oldEntity2, newEntity2);

        // Assert
        assertEquals(2, count.getRebootCnt());
    }

    @Test
    void testObtainFirstSqlValues() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 12, 0);
        ApRebootWeeklyCount count = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );
        count.setRebootCnt(3);

        // Act
        String sql = count.obtainFirstSqlValues();

        // Assert
        String expected = """
                select
                '2023-10-02' as `ap_week`, '12345' as `ap_mac`, 'AP-001' as `ap_name`, '123456789' as `ap_ip`, '3' as `reboot_cnt`
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlValues() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 12, 0);
        ApRebootWeeklyCount count = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );
        count.setRebootCnt(5);

        // Act
        String sql = count.obtainSqlValues();

        // Assert
        String expected = """
                union all
                select '2023-10-02', '12345', 'AP-001', '123456789', '5'
                """;
        assertEquals(expected.trim(), sql.trim());
    }

    @Test
    void testObtainSqlValuesStatic() {
        // Arrange
        LocalDateTime pollTime1 = LocalDateTime.of(2023, 10, 2, 12, 0);
        ApRebootWeeklyCount count1 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime1)
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );
        count1.setRebootCnt(3);

        LocalDateTime pollTime2 = LocalDateTime.of(2023, 10, 9, 12, 0); // Different week
        ApRebootWeeklyCount count2 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime2)
                        .apMac(67890L)
                        .apName("AP-002")
                        .apIp(987654321)
                        .build()
        );
        count2.setRebootCnt(1);

        Map<ApRebootWeeklyCount, ApRebootWeeklyCount> map = new LinkedHashMap<>();
        map.put(count1, count1);
        map.put(count2, count2);

        // Act
        String sql = ApRebootWeeklyCount.obtainSqlValues(map);

        // Assert
        String expected1 = """
                select
                '2023-10-02' as `ap_week`, '12345' as `ap_mac`, 'AP-001' as `ap_name`, '123456789' as `ap_ip`, '3' as `reboot_cnt`
                """.trim();
        String expected2 = """
                union all
                select '2023-10-09', '67890', 'AP-002', '987654321', '1'
                """.trim();

        String result = sql.trim();
        assertTrue(result.contains(expected1));
        assertTrue(result.contains(expected2));
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 12, 0);
        ApRebootWeeklyCount count1 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );
        ApRebootWeeklyCount count2 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );
        ApRebootWeeklyCount count3 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(LocalDateTime.of(2023, 10, 9, 12, 0))
                        .apMac(67890L)
                        .apName("AP-002")
                        .apIp(987654321)
                        .build()
        );

        // Act & Assert
        assertEquals(count1, count2);
        assertEquals(count1.hashCode(), count2.hashCode());
        assertNotEquals(count1, count3);
        assertNotEquals(count1.hashCode(), count3.hashCode());
    }

    @Test
    void testEqualsDifferentWeek() {
        // Arrange
        ApRebootWeeklyCount count1 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(LocalDateTime.of(2023, 10, 2, 12, 0)) // Week: 2023-10-02
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );
        ApRebootWeeklyCount count2 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(LocalDateTime.of(2023, 10, 9, 12, 0)) // Week: 2023-10-09
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testEqualsDifferentApMac() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 12, 0);
        ApRebootWeeklyCount count1 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );
        ApRebootWeeklyCount count2 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(67890L) // Different MAC
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testEqualsDifferentApName() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 12, 0);
        ApRebootWeeklyCount count1 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );
        ApRebootWeeklyCount count2 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(12345L)
                        .apName("AP-002") // Different name
                        .apIp(123456789)
                        .build()
        );

        // Act & Assert
        assertNotEquals(count1, count2);
    }

    @Test
    void testEqualsDifferentApIp() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 2, 12, 0);
        ApRebootWeeklyCount count1 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(123456789)
                        .build()
        );
        ApRebootWeeklyCount count2 = new ApRebootWeeklyCount(
                ArubaAiApInfoEntity.builder()
                        .pollTime(pollTime)
                        .apMac(12345L)
                        .apName("AP-001")
                        .apIp(987654321) // Different IP
                        .build()
        );

        // Act & Assert
        assertNotEquals(count1, count2);
    }
}
