package com.home.network.statistic.poller.rfc1213.igate.etl;

import com.home.network.statistic.poller.rfc1213.igate.out.Rfc1213IgateIftableTrafficEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Rfc1213IgateTrafficHourlyCountTest {

    @Test
    void testConstructor() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 1, 12, 0);
        Rfc1213IgateIftableTrafficEntity entity = Rfc1213IgateIftableTrafficEntity.builder()
                .pollTime(pollTime)
                .ifPhysAddress(12345L)
                .ifDescr("eth0")
                .build();

        // Act
        Rfc1213IgateTrafficHourlyCount count = new Rfc1213IgateTrafficHourlyCount(entity);

        // Assert
        assertEquals("2023-10-01", count.getDate());
        assertEquals(43200, count.getTimeHourSecond()); // 12:00:00 in seconds
        assertEquals(12345L, count.getIfPhysAddress());
        assertEquals("eth0", count.getIfDescr());
        assertEquals(0, count.getInBytes());
        assertEquals(0, count.getOutBytes());
    }

    @Test
    void testAdjustTraffic() {
        // Arrange
        LocalDateTime pollTime = LocalDateTime.of(2023, 10, 1, 12, 0);
        Rfc1213IgateIftableTrafficEntity oldEntity = Rfc1213IgateIftableTrafficEntity.builder()
                .ifInOctets(1000L)
                .ifOutOctets(2000L)
                .build();
        Rfc1213IgateIftableTrafficEntity newEntity = Rfc1213IgateIftableTrafficEntity.builder()
                .ifInOctets(1500L)
                .ifOutOctets(2500L)
                .build();
        Rfc1213IgateTrafficHourlyCount count = new Rfc1213IgateTrafficHourlyCount(
                Rfc1213IgateIftableTrafficEntity.builder().pollTime(pollTime).ifPhysAddress(12345L).ifDescr("eth0").build()
        );

        // Act
        count.adjustTraffic(oldEntity, newEntity);

        // Assert
        assertEquals(500, count.getInBytes()); // 1500 - 1000
        assertEquals(500, count.getOutBytes()); // 2500 - 2000
    }

    @Test
    void testAdjustTrafficWithWrapAround() {
        // Arrange: simulate counter wrap-around (new < old)
        Rfc1213IgateIftableTrafficEntity oldEntity = Rfc1213IgateIftableTrafficEntity.builder()
                .ifInOctets(4000L)
                .ifOutOctets(3000L)
                .build();
        Rfc1213IgateIftableTrafficEntity newEntity = Rfc1213IgateIftableTrafficEntity.builder()
                .ifInOctets(1000L) // less than old, so use new value
                .ifOutOctets(500L)
                .build();
        Rfc1213IgateTrafficHourlyCount count = new Rfc1213IgateTrafficHourlyCount(
                Rfc1213IgateIftableTrafficEntity.builder().pollTime(LocalDateTime.now()).ifPhysAddress(12345L).ifDescr("eth0").build()
        );

        // Act
        count.adjustTraffic(oldEntity, newEntity);

        // Assert
        assertEquals(1000, count.getInBytes());
        assertEquals(500, count.getOutBytes());
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        Rfc1213IgateTrafficHourlyCount count1 = new Rfc1213IgateTrafficHourlyCount(
                Rfc1213IgateIftableTrafficEntity.builder()
                        .pollTime(LocalDateTime.of(2023, 10, 1, 12, 0))
                        .ifPhysAddress(12345L)
                        .ifDescr("eth0")
                        .build()
        );
        Rfc1213IgateTrafficHourlyCount count2 = new Rfc1213IgateTrafficHourlyCount(
                Rfc1213IgateIftableTrafficEntity.builder()
                        .pollTime(LocalDateTime.of(2023, 10, 1, 12, 0))
                        .ifPhysAddress(12345L)
                        .ifDescr("eth0")
                        .build()
        );
        Rfc1213IgateTrafficHourlyCount count3 = new Rfc1213IgateTrafficHourlyCount(
                Rfc1213IgateIftableTrafficEntity.builder()
                        .pollTime(LocalDateTime.of(2023, 10, 1, 13, 0))
                        .ifPhysAddress(67890L)
                        .ifDescr("eth1")
                        .build()
        );

        // Act & Assert
        assertEquals(count1, count2);
        assertEquals(count1.hashCode(), count2.hashCode());
        assertNotEquals(count1, count3);
        assertNotEquals(count1.hashCode(), count3.hashCode());
    }
}
