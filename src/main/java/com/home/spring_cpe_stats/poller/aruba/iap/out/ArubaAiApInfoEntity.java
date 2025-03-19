package com.home.spring_cpe_stats.poller.aruba.iap.out;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "aruba_iap_ap_info_stg")
@NoArgsConstructor
@Getter
@Setter
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

    public static String constructIdString(Stream<ArubaAiApInfoEntity> stream) {
        return stream
                .map(ArubaAiApInfoEntity::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(",","(",")"));
    }
}

