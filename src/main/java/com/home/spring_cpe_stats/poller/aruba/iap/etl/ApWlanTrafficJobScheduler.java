package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiWlanTrafficEntity;
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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!local")
public class ApWlanTrafficJobScheduler implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext applicationContext;

    public List<ArubaAiWlanTrafficEntity> getListUnprocessed() {
        return jdbcTemplate
            .query("""
                select
                aiwts.id
                from aruba_iap_wlan_traffic_stg aiwts
                where aiwts.mark=0
                order by aiwts.id
                limit 10000
                """,
                new BeanPropertyRowMapper<>(ArubaAiWlanTrafficEntity.class)
            );
    }

    public List<ArubaAiWlanTrafficEntity> getListProcessed(String unprocessedIds) {
        return jdbcTemplate
            .query(String.format("""
                    with cte as(
                        select
                        distinct
                        date(aiwts.poll_time),
                        time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00')),
                        aiwts.wlan_mac,
                        aiwts.wlan_essid
                        from aruba_iap_wlan_traffic_stg aiwts
                        where aiwts.id in %s
                    )
                    select
                    id
                    from (
                        select
                        aiwts.id,
                        row_number() over(partition by date(aiwts.poll_time),time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00')),aiwts.wlan_mac,aiwts.wlan_essid 
                        order by aiwts.poll_time desc,aiwts.id desc) as rn
                        from (
                            select
                            *
                            from aruba_iap_wlan_traffic_stg aiwts
                            where mark=1
                            order by id desc
                            limit 100 -- limit to 100 because this table is small ~ last 2 hrs
                        ) aiwts
                        where (date(aiwts.poll_time),time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00')),aiwts.wlan_mac,aiwts.wlan_essid)
                        in (select * from cte)
                    ) x
                    where x.rn=1
                    """,
                        unprocessedIds),
                    new BeanPropertyRowMapper<>(ArubaAiWlanTrafficEntity.class)
            );
    }

    public void insertIntoApDim(String unprocessedIds) {
        // insert into ap_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into ap_dim(ap_mac,ap_name) (
                    select
                    distinct
                    aiwts.wlan_ap_mac,
                    ''  -- by default, current stg table does not have ap_name
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join ap_dim ad on aiwts.wlan_ap_mac=ad.ap_mac
                    where ad.ap_key is null
                    and aiwts.id in %s
                )""",
                unprocessedIds
            )
        );
    }

    public void insertIntoGwIfaceDim(String unprocessedIds) {
        // insert into gw_iface_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into gw_iface_dim(iface_mac,iface_name) (
                    select
                    distinct
                    aiwts.wlan_mac,
                    aiwts.wlan_essid
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join gw_iface_dim gid on aiwts.wlan_mac=gid.iface_mac
                    and aiwts.wlan_essid=gid.iface_name
                    where gid.iface_key is null
                    and aiwts.id in %s
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
                    date(aiwts.poll_time)
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join date_dim dd on date(aiwts.poll_time)=dd.date
                    where dd.date_key is null and aiwts.id in %s
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
                    time_to_sec(time(aiwts.poll_time))
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join time_dim td on time_to_sec(time(aiwts.poll_time))=td.time
                    where td.time_key is null and aiwts.id in %s
                )""",
            unprocessedIds)
        );
    }

    public void insertIntoTimeDimHourNorm(String unprocessedIds) {
        // insert hour into time dim
        jdbcTemplate.execute(
            String.format("""
            insert ignore into time_dim(time) (
                select
                distinct time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00'))
                from aruba_iap_wlan_traffic_stg aiwts
                left join time_dim td on time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00'))=td.time
                where td.time_key is null and aiwts.id in %s
            )""",
            unprocessedIds)
        );
    }

    @Transactional
    public void summarizeData(String unprocessedIds,
                              String processedIds,
                              String unprocessedAndProcessedIds) {
        log.info("start summarizing data");

        jdbcTemplate.execute(
            String.format("""
                select
                aiwts.id
                from aruba_iap_wlan_traffic_stg aiwts
                where aiwts.mark=0
                and aiwts.id in %s
                for update""",
                unprocessedIds
            )
        );

        jdbcTemplate.execute(
            String.format("""
                select
                aiwts.id
                from aruba_iap_wlan_traffic_stg aiwts
                where aiwts.mark=1
                and aiwts.id in %s
                for share
            """, processedIds)
        );

        // insert into fact table
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
                            -- consider case when previous tx/rx value is > current value (when counter reset to 0),
                            -- then accept cuurent value
                            -- if there is no previous value, then consider previous is current value
                            aiwts.wlan_tx - if(
                                lag(aiwts.wlan_tx,1,aiwts.wlan_tx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time,aiwts.id)
                                >
                                aiwts.wlan_tx,
                                0,
                                lag(aiwts.wlan_tx,1,aiwts.wlan_tx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time,aiwts.id)
                            )
                            +
                            aiwts.wlan_rx - if(
                                lag(aiwts.wlan_rx,1,aiwts.wlan_rx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time,aiwts.id)
                                >
                                aiwts.wlan_rx,
                                0,
                                lag(aiwts.wlan_rx,1,aiwts.wlan_rx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time,aiwts.id)
                            )
                            as transmission_bytes
                            from aruba_iap_wlan_traffic_stg aiwts
                            join date_dim dd on dd.date_key=(
                                select date_key from
                                date_dim
                                where date(aiwts.poll_time)=date
                                order by date_key desc limit 1
                            ) 
                            join time_dim td on td.time_key=(
                                select time_key from
                                time_dim
                                where time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00'))=time
                                order by time_key desc limit 1
                            ) 
                            join gw_iface_dim gid on gid.iface_key=(
                                select iface_key from
                                gw_iface_dim
                                where aiwts.wlan_mac=iface_mac and aiwts.wlan_essid=iface_name
                                order by iface_key desc limit 1
                            ) 
                            where aiwts.id in %s
                        ) x
                        group by 1,2,3
                    ) tmp
                )
                on duplicate key update transmission_bytes=transmission_bytes+transmission_bytes_val""",
                unprocessedAndProcessedIds
            )
        );

        log.info("mark records start");

        // once completed, mark all completed
        jdbcTemplate.execute(
            String.format("""
                update aruba_iap_wlan_traffic_stg set mark=1 where id in %s""",
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

        List<ArubaAiWlanTrafficEntity> listUnprocessed = this.getListUnprocessed();

        if (listUnprocessed.isEmpty()) {
            log.info("listUnprocessed is empty");
            return;
        }

        String unprocessedIds = ArubaAiWlanTrafficEntity.constructIdString(listUnprocessed.parallelStream());

        List<ArubaAiWlanTrafficEntity> listProcessed = this.getListProcessed(unprocessedIds);

        if (listProcessed.isEmpty()) {
            listProcessed.add(ArubaAiWlanTrafficEntity.builder().id(-1L).build());
        }

        String processedIds = ArubaAiWlanTrafficEntity.constructIdString(listProcessed.parallelStream());
        String unprocessedAndProcessedIds = ArubaAiWlanTrafficEntity.constructIdString(
                Stream.concat(listUnprocessed.parallelStream(), listProcessed.parallelStream())
        );

        this.insertIntoApDim(unprocessedIds);
        this.insertIntoGwIfaceDim(unprocessedIds);
        this.insertIntoDateDim(unprocessedIds);
        this.insertIntoTimeDim(unprocessedIds);
        this.insertIntoTimeDimHourNorm(unprocessedIds);

        ApWlanTrafficJobScheduler ctx = applicationContext.getBean(ApWlanTrafficJobScheduler.class);

        ctx.summarizeData(unprocessedIds, processedIds, unprocessedAndProcessedIds);

        log.info("end");
    }
}
