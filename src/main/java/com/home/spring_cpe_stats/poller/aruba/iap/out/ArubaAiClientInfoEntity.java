package com.home.spring_cpe_stats.poller.aruba.iap.out;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "aruba_iap_device_info_stg")
@NoArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class ArubaAiClientInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime pollTime;
    private Long deviceMac;
    private Long deviceWlanMac;
    private Integer deviceIp;
    private Integer deviceApIp;
    private String deviceName;
    private Long deviceRx;
    private Long deviceTx;
    private Integer deviceSnr;
    private Long deviceUptimeSeconds;
    private Integer mark;
}
