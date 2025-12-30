package com.home.network.statistic.poller.igate.gw240.out;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "igate_gw240_status_wifi_station_stg_ingest")
@NoArgsConstructor
@Getter
@Setter
// REMEMBER CAREFULLY: IF PERSIST OBJECT TO DB, BUT DOESN’T DECLARE SETTER ON OBJECT, HIBERNATE USES REFLECTION TO CREATE OBJECT -> NULL ATTRIBUTE VALUE
@Builder
@AllArgsConstructor
public class StatusWifiStationEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime pollTime;
    private String rawData;

    // if put all data in one raw string, later operations
    // in this class will be difficult because they will operate on one raw string
    // deserializing raw string need another separated class
    // but also have to consider json raw type in mysql
    // it likely supports join when joining json data with
    // another column in another table
}
