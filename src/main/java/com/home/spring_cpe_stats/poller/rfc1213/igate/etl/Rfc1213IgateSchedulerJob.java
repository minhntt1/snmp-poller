package com.home.spring_cpe_stats.poller.rfc1213.igate.etl;

import com.home.spring_cpe_stats.poller.rfc1213.igate.out.Rfc1213IgateIftableTrafficEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!local")
public class Rfc1213IgateSchedulerJob implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @Scheduled(fixedRate = 3_600_000) // 1 hour
    public void start() {
        log.info("start");

        /*
        * select in stg table
        * then also use partition by iface descr,iface phys address,
        * */
        List<Rfc1213IgateIftableTrafficEntity> rfc1213IgateIftableTrafficEntities =
            jdbcTemplate.query("""            
            select
            rits.id
            from rfc1213_iftable_traffic_stg rits
            where mark=0
            order by rits.poll_time,rits.id
            limit 100000
            for update""",
            new BeanPropertyRowMapper<>(Rfc1213IgateIftableTrafficEntity.class)
        );

        log.info("total records: {}", rfc1213IgateIftableTrafficEntities.size());

        long minId = rfc1213IgateIftableTrafficEntities.stream()
                .map(Rfc1213IgateIftableTrafficEntity::getId)
                .min(Long::compare)
                .orElse(0L);

        long maxId = rfc1213IgateIftableTrafficEntities.stream()
                .map(Rfc1213IgateIftableTrafficEntity::getId)
                .max(Long::compareTo)
                .orElse(0L);

        log.info("min id: {}, maxId: {}", minId, maxId);

        /*
        * determine necessary table dim
        * date_dim
        * time_dim
        * gw_iface_dim
        * */

        log.info("insert into dim table start");

        // insert into gw_iface_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into gw_iface_dim(iface_mac,iface_phy_name) (
                    select
                    distinct
                    rits.if_phys_address,
                    rits.if_descr
                    from rfc1213_iftable_traffic_stg rits
                    left join gw_iface_dim gid on rits.if_phys_address=gid.iface_mac
                    and rits.if_descr=gid.iface_phy_name
                    where gid.iface_key is null
                    and rits.id between %d and %d
                    and rits.if_oper_status=1 -- only allow up iface (up=1,down=2)
                    and rits.if_phys_address is not null and rits.if_phys_address<>0 -- only allow iface has phys address
                )""",
                minId,maxId
            )
        );

        // insert into date_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into date_dim(date) (
                    select
                    distinct
                    date(rits.poll_time)
                    from rfc1213_iftable_traffic_stg rits
                    left join date_dim dd on date(rits.poll_time)=dd.date
                    where dd.date_key is null and rits.id between %d and %d
                );""",
                minId, maxId
            )
        );

        // insert into time_dim
        jdbcTemplate.execute(
            String.format("""
            insert ignore into time_dim(time) (
                select
                distinct
                time_to_sec(time(rits.poll_time))
                from rfc1213_iftable_traffic_stg rits
                left join time_dim td on time_to_sec(time(rits.poll_time))=td.time
                where td.time_key is null and rits.id between %d and %d
            )""",
            minId, maxId)
        );

        // insert hour into time dim
        jdbcTemplate.execute(
                String.format("""
                insert ignore into time_dim(time) (
                    select
                    distinct time_to_sec(date_format(time(rits.poll_time),'%%H:00:00'))
                    from rfc1213_iftable_traffic_stg rits
                    left join time_dim td on time_to_sec(date_format(time(rits.poll_time),'%%H:00:00'))=td.time
                    where td.time_key is null and rits.id between %d and %d
                )""",
                minId, maxId)
        );

        log.info("insert into dim table end");

        log.info("update fact table start");

        jdbcTemplate.execute(
            String.format("""
                insert into iface_traffic_by_hour_fact(date_key,time_key,iface_key,transmission_bytes) (
                    select
                    *
                    from (
                        select
                        x.date_key,
                        x.time_key,
                        x.iface_key,
                        sum(transmission_bytes) as transmission_bytes_val
                        from (
                            select
                            dd.date_key,
                            td.time_key,
                            gid.iface_key,
                            -- notice: octet = 8 bits = 1 byte
                            rits.if_in_octets - if(
                                lag(rits.if_in_octets,1,rits.if_in_octets) over(partition by dd.date_key,td.time_key,gid.iface_key order by rits.poll_time)
                                >
                                rits.if_in_octets,
                                0,
                                lag(rits.if_in_octets,1,rits.if_in_octets) over(partition by dd.date_key,td.time_key,gid.iface_key order by rits.poll_time)
                            )
                            +
                            rits.if_out_octets - if(
                                lag(rits.if_out_octets,1,rits.if_out_octets) over(partition by dd.date_key,td.time_key,gid.iface_key order by rits.poll_time)
                                >
                                rits.if_out_octets,
                                0,
                                lag(rits.if_out_octets,1,rits.if_out_octets) over(partition by dd.date_key,td.time_key,gid.iface_key order by rits.poll_time)
                            )
                            as transmission_bytes
                            from rfc1213_iftable_traffic_stg rits
                            join date_dim dd on date(rits.poll_time)=dd.date
                            join time_dim td on time_to_sec(date_format(time(rits.poll_time),'%%H:00:00'))=td.time
                            join gw_iface_dim gid on rits.if_phys_address=gid.iface_mac and rits.if_descr=gid.iface_phy_name
                            where rits.id between %d and %d
                            and rits.if_oper_status=1 -- only allow up iface (up=1,down=2)
                            and rits.if_phys_address is not null and rits.if_phys_address<>0 -- only allow iface has phys address
                        ) x
                        group by 1,2,3
                    ) tmp
                )
                on duplicate key update transmission_bytes=transmission_bytes+transmission_bytes_val"""
            , minId, maxId)
        );

        log.info("update fact table end");

        log.info("mark records start");

        // once completed, mark all completed
        jdbcTemplate.execute(
            String.format("""
            update rfc1213_iftable_traffic_stg set mark=1 where id between %d and %d""",
                minId, maxId
            )
        );

        log.info("mark records completed");

        log.info("end");
    }
}
