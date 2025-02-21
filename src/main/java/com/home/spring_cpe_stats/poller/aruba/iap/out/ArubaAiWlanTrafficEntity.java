package com.home.spring_cpe_stats.poller.aruba.iap.out;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "aruba_iap_wlan_traffic_stg")
@NoArgsConstructor
@Getter
@Setter // add setter for jdbc BeanPropertyRowMapper
@Builder
@AllArgsConstructor
public class ArubaAiWlanTrafficEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime pollTime;
    private Long wlanApMac;
    private String wlanEssid;
    private Long wlanMac;
    private Long wlanRx;
    private Long wlanTx;
    private Integer mark;
}
