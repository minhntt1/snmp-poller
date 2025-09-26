package com.home.network.statistic.poller.rfc1213.igate.out;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "rfc1213_iftable_traffic_stg")
@NoArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Rfc1213IgateIftableTrafficEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    private LocalDateTime pollTime;
    private Integer ifIndex;
    private String ifDescr;
    private Long ifPhysAddress;
    private String ifAdminStatus;
    private String ifOperStatus;
    private Long ifInOctets;
    private Long ifOutOctets;
    private Integer ipAdEntAddr;
    private Integer mark;

    public static String constructIdString(Stream<Rfc1213IgateIftableTrafficEntity> stream) {
        return stream
                .map(Rfc1213IgateIftableTrafficEntity::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(",","(",")"));
    }
}
