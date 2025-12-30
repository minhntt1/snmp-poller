package com.home.network.statistic.poller.rfc1213.igate.out;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.home.network.statistic.common.util.JsonUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Entity
@Table(name = "rfc1213_iftable_traffic_stg_ingest")
@NoArgsConstructor
@Getter
@Setter // REMEMBER CAREFULLY: IF PERSIST OBJECT TO DB, BUT DOESN’T DECLARE SETTER ON OBJECT, HIBERNATE USES REFLECTION TO CREATE OBJECT -> NULL ATTRIBUTE VALUE
@Builder
@AllArgsConstructor
public class Rfc1213IgateIftableTrafficEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime pollTime;
    private Integer ifIndex;
    private String ifDescr;
    private Long ifPhysAddress;
    private String ifAdminStatus;
    private String ifOperStatus;
    private Long ifInOctets;
    private Long ifOutOctets;
    private Integer ipAdEntAddr;

    // check record usable for batch processing
    public boolean checkUsableEntry() {
        /*  where rits.if_oper_status=1 -- only allow up iface (up=1,down=2)
            and rits.if_phys_address is not null and rits.if_phys_address<>0 -- only allow iface has phys address*/
        return ifOperStatus.equals("1") && Optional.ofNullable(ifPhysAddress).map(val -> val != 0).orElse(false);
    }

    public boolean sameDateHour(Rfc1213IgateIftableTrafficEntity o) {
        return
            this.pollTime.toLocalDate().equals(o.pollTime.toLocalDate()) &&
            this.pollTime.toLocalTime().truncatedTo(ChronoUnit.HOURS).equals(o.pollTime.toLocalTime().truncatedTo(ChronoUnit.HOURS));
    }

    // o: entity with higher timestamp
    // this: entity with lower timestamp
    public long calcDiffTxOldNew(Rfc1213IgateIftableTrafficEntity o) {
        return o.ifOutOctets < this.ifOutOctets ? o.ifOutOctets : o.ifOutOctets - this.ifOutOctets;
    }

    public long calcDiffRxOldNew(Rfc1213IgateIftableTrafficEntity o) {
        return o.ifInOctets < this.ifInOctets ? o.ifInOctets : o.ifInOctets - this.ifInOctets;
    }

    public String obtainPollDate() {
        return pollTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public Integer obtainPollTimeHour() {
        return pollTime.toLocalTime().truncatedTo(ChronoUnit.HOURS).toSecondOfDay();
    }

    public String obtainJobStateKey() {// rfc1213 state key relects coulmns in gw_iface_dim table
        return "ifPhysAddress_%d_ifDescr_%s".formatted(this.ifPhysAddress, this.ifDescr);
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }

    public static Rfc1213IgateIftableTrafficEntity from(String json) {
        if (json == null) return null;
        return JsonUtil.fromJson(json, Rfc1213IgateIftableTrafficEntity.class);
    }
}
