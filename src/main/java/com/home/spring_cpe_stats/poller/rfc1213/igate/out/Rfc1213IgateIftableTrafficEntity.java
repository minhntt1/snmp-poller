package com.home.spring_cpe_stats.poller.rfc1213.igate.out;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rfc1213_iftable_traffic_stg")
@NoArgsConstructor
@Getter
@Setter
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
}
