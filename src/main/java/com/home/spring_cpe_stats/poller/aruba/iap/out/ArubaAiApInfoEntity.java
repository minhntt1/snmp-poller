package com.home.spring_cpe_stats.poller.aruba.iap.out;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "aruba_iap_ap_info_stg")
@NoArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class ArubaAiApInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime pollTime;
    private Long apMac;
    private String apName;
    private Integer apIp;
    private String apModel;
    private Long apUptimeSeconds;
    private Integer mark;
}

