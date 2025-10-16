package com.home.network.statistic.poller.rfc1213.igate.etl;

import com.home.network.statistic.poller.rfc1213.igate.out.Rfc1213IgateIftableTrafficEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class Rfc1213IgateTrafficHourlyCount {
    private final String date;
    private final Integer timeHourSecond;
    private final Long ifPhysAddress;
    private final String ifDescr;
    private long inBytes;
    private long outBytes;

    public Rfc1213IgateTrafficHourlyCount(Rfc1213IgateIftableTrafficEntity e) {
        this.date = e.obtainPollDate();
        this.timeHourSecond = e.obtainPollTimeHour();
        this.ifPhysAddress = e.getIfPhysAddress();
        this.ifDescr = e.getIfDescr();
        this.inBytes = 0;
        this.outBytes = 0;
    }

    public void adjustTraffic(Rfc1213IgateIftableTrafficEntity old, Rfc1213IgateIftableTrafficEntity nEW) {
        // always subtract prev state (because it is accumulated)
        this.inBytes += old.calcDiffRxOldNew(nEW);
        this.outBytes += old.calcDiffTxOldNew(nEW);
    }

    public static String obtainFirstSqlQuery(Map<Rfc1213IgateTrafficHourlyCount, Rfc1213IgateTrafficHourlyCount> map) {
        var query = new StringBuilder();

        for (var val : map.keySet()) {
            if (val == null) continue;

            if (query.isEmpty()) query.append(val.obtainFirstSqlQuery());
            else query.append(val.obtainSqlQuery());
        }

        return query.toString();
    }

    public static List<Object[]> obtainMappedRow(Map<Rfc1213IgateTrafficHourlyCount, Rfc1213IgateTrafficHourlyCount> map) {
        return map.values().stream().map(Rfc1213IgateTrafficHourlyCount::obtainMappedRow).toList();
    }

    public Object[] obtainMappedRow() {
        return new Object[] {date, timeHourSecond, ifPhysAddress, ifDescr, inBytes + outBytes};
    }

    public String obtainFirstSqlQuery() {
        return """
                select '%s' as `date`, '%d' as `time`, '%d' as `if_phys_address`, '%s' as `if_descr`, '%d' as `transmission_bytes_val`
                """.formatted(this.date, this.timeHourSecond, this.ifPhysAddress, this.ifDescr, this.inBytes+this.outBytes);
    }

    public String obtainSqlQuery() {
        return """
                union all
                select '%s', '%d', '%d', '%s', '%d'
                """.formatted(this.date, this.timeHourSecond, this.ifPhysAddress, this.ifDescr, this.inBytes+this.outBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rfc1213IgateTrafficHourlyCount that)) return false;
        return Objects.equals(date, that.date) && Objects.equals(timeHourSecond, that.timeHourSecond) && Objects.equals(ifPhysAddress, that.ifPhysAddress) && Objects.equals(ifDescr, that.ifDescr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, timeHourSecond, ifPhysAddress, ifDescr);
    }
}
