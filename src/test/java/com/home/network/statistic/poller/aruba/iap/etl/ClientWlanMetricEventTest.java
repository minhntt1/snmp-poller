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
}
