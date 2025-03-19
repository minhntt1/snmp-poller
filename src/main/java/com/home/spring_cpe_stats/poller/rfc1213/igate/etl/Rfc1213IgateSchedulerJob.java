package com.home.spring_cpe_stats.poller.rfc1213.igate.etl;

import com.home.spring_cpe_stats.poller.rfc1213.igate.out.Rfc1213IgateIftableTrafficEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!local")
public class Rfc1213IgateSchedulerJob implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext applicationContext;

    public List<Rfc1213IgateIftableTrafficEntity> getUnprocessedList() {
        return jdbcTemplate.query("""            
            select
            rits.id
            from rfc1213_iftable_traffic_stg rits
            where mark=0
            order by rits.id
            limit 10000""",
            new BeanPropertyRowMapper<>(Rfc1213IgateIftableTrafficEntity.class)
        );
    }

    public List<Rfc1213IgateIftableTrafficEntity> getProcessedList(String unprocessedIds) {
        return jdbcTemplate.query(
            String.format("""
                with cte as(
                    select
                    distinct
                    date(rits.poll_time),
                    time_to_sec(date_format(time(rits.poll_time),'%%H:00:00')),
                    rits.if_phys_address,
                    rits.if_descr
                    from rfc1213_iftable_traffic_stg rits
                    where rits.id in %s
                )
                select
                id
                from (
                    select
                    rits.id,
                    row_number() over(partition by 
                        date(rits.poll_time),
                        time_to_sec(date_format(time(rits.poll_time),'%%H:00:00')),
                        rits.if_phys_address,
                        rits.if_descr
                        order by rits.poll_time desc,rits.id desc
                    ) as rn
                    from (
                        select
                        *
                        from rfc1213_iftable_traffic_stg rits
                        where rits.mark = 1
                        order by rits.id desc
                        limit 2000 -- limit to earliest 2000 processed records to look back (~ last 3 hours)
                    ) rits
                    where (date(rits.poll_time),
                        time_to_sec(date_format(time(rits.poll_time),'%%H:00:00')),
                        rits.if_phys_address,
                        rits.if_descr)
                    in (select * from cte)
                ) x
                where x.rn=1
                """,
                unprocessedIds),
                new BeanPropertyRowMapper<>(Rfc1213IgateIftableTrafficEntity.class
            )
        );
    }

    public void insertIntoGwIfaceDim(String unprocessedIds) {
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
                    and rits.id in %s
                    and rits.if_oper_status=1 -- only allow up iface (up=1,down=2)
                    and rits.if_phys_address is not null and rits.if_phys_address<>0 -- only allow iface has phys address
                )""",
                unprocessedIds
            )
        );
    }

    public void insertIntoDateDim(String unprocessedIds) {
        // insert into date_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into date_dim(date) (
                    select
                    distinct
                    date(rits.poll_time)
                    from rfc1213_iftable_traffic_stg rits
                    left join date_dim dd on date(rits.poll_time)=dd.date
                    where dd.date_key is null and rits.id in %s
                );""",
                unprocessedIds
            )
        );
    }

    public void insertIntoTimeDim(String unprocessedIds) {
        // insert into time_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into time_dim(time) (
                    select
                    distinct
                    time_to_sec(time(rits.poll_time))
                    from rfc1213_iftable_traffic_stg rits
                    left join time_dim td on time_to_sec(time(rits.poll_time))=td.time
                    where td.time_key is null and rits.id in %s
                )""",
                unprocessedIds
            )
        );
    }

    public void insertIntoTimeDimHourNorm(String unprocessedIds) {
        // insert into time_dim
        // insert hour into time dim
        jdbcTemplate.execute(
            String.format("""
            insert ignore into time_dim(time) (
                select
                distinct time_to_sec(date_format(time(rits.poll_time),'%%H:00:00'))
                from rfc1213_iftable_traffic_stg rits
                left join time_dim td on time_to_sec(date_format(time(rits.poll_time),'%%H:00:00'))=td.time
                where td.time_key is null and rits.id in %s
            )""",
            unprocessedIds)
        );
    }

    @Transactional
    public void summarizeData(String unprocessedIds,
                              String processedIds,
                              String processedAndUnprocessedIds) {
        log.info("start summarizing data");

        jdbcTemplate.execute(
            String.format("""
                select
                rits.id
                from rfc1213_iftable_traffic_stg rits
                where mark=0
                and rits.id in %s
                for update
            """, unprocessedIds)
        );

        jdbcTemplate.execute(
            String.format("""
                select
                rits.id
                from rfc1213_iftable_traffic_stg rits
                where mark=1
                and rits.id in %s
                for share
            """, processedIds)
        );

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
                            lag(rits.if_in_octets,1,rits.if_in_octets) over(partition by dd.date_key,td.time_key,gid.iface_key order by rits.poll_time,rits.id)
                            >
                            rits.if_in_octets,
                            0,
                            lag(rits.if_in_octets,1,rits.if_in_octets) over(partition by dd.date_key,td.time_key,gid.iface_key order by rits.poll_time,rits.id)
                        )
                        +
                        rits.if_out_octets - if(
                            lag(rits.if_out_octets,1,rits.if_out_octets) over(partition by dd.date_key,td.time_key,gid.iface_key order by rits.poll_time,rits.id)
                            >
                            rits.if_out_octets,
                            0,
                            lag(rits.if_out_octets,1,rits.if_out_octets) over(partition by dd.date_key,td.time_key,gid.iface_key order by rits.poll_time,rits.id)
                        )
                        as transmission_bytes
                        from rfc1213_iftable_traffic_stg rits
                        join date_dim dd on dd.date_key=(
                            select date_key from
                            date_dim
                            where date(rits.poll_time)=date
                            order by date_key desc limit 1
                        )
                        join time_dim td on td.time_key=(
                            select time_key from
                            time_dim
                            where time_to_sec(date_format(time(rits.poll_time),'%%H:00:00'))=time
                            order by time_key desc limit 1
                        )
                        join gw_iface_dim gid on gid.iface_key=(
                            select iface_key from
                            gw_iface_dim
                            where rits.if_phys_address=iface_mac and rits.if_descr=iface_phy_name
                            order by iface_key desc limit 1
                        )
                        where rits.id in %s
                        and rits.if_oper_status=1 -- only allow up iface (up=1,down=2)
                        and rits.if_phys_address is not null and rits.if_phys_address<>0 -- only allow iface has phys address
                    ) x
                    group by 1,2,3
                ) tmp
            )
            on duplicate key update transmission_bytes=transmission_bytes+transmission_bytes_val"""
            , processedAndUnprocessedIds)
        );

        log.info("mark records start");

        // once completed, mark all completed
        jdbcTemplate.execute(
            String.format("""
                update rfc1213_iftable_traffic_stg set mark=1 where id in %s""",
                unprocessedIds
            )
        );

        log.info("mark records completed");

        log.info("end summarizing data");
    }

    @Override
    @Scheduled(fixedRate = 600_000) // 10 min
    public void start() {
        log.info("start");

        List<Rfc1213IgateIftableTrafficEntity> unprocessedList = this.getUnprocessedList();

        if (unprocessedList.isEmpty()) {
            log.info("unprocessed list is empty");
            return;
        }

        String unprocessedIds = Rfc1213IgateIftableTrafficEntity.constructIdString(unprocessedList.parallelStream());

        List<Rfc1213IgateIftableTrafficEntity> processedList = this.getProcessedList(unprocessedIds);

        if (processedList.isEmpty()) {
            processedList.add(Rfc1213IgateIftableTrafficEntity.builder().id(-1L).build());
        }

        String processedIds = Rfc1213IgateIftableTrafficEntity.constructIdString(processedList.parallelStream());
        String processedAndUnprocessedIds = Rfc1213IgateIftableTrafficEntity.constructIdString(
                Stream.concat(unprocessedList.parallelStream(), processedList.parallelStream())
        );

        this.insertIntoGwIfaceDim(unprocessedIds);
        this.insertIntoDateDim(unprocessedIds);
        this.insertIntoTimeDim(unprocessedIds);
        this.insertIntoTimeDimHourNorm(unprocessedIds);

        Rfc1213IgateSchedulerJob ctx = applicationContext.getBean(Rfc1213IgateSchedulerJob.class);

        ctx.summarizeData(unprocessedIds, processedIds, processedAndUnprocessedIds);

        log.info("end");
    }
}
