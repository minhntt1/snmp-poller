package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!local")
public class ClientInfoSchedulerJob implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @Scheduled(fixedRate = 600_000) // 10 min
    public void start() {
		log.info("start");

		// add to dim table
		// normalize unspecified vendor
		jdbcTemplate.execute("""
			insert ignore into vendor_dim (vendor_key,vendor_name,vendor_prefix) values(
				-2147483648,
				'<undefined>',
				-2147483648
			);""");

		// normalize unspecified ap/iface column
		jdbcTemplate.execute("""		
			insert ignore into ap_dim(ap_key,ap_mac,ap_name) values (
				-2147483648,
				-9223372036854775808,
				'<undefined>'
			)""");

		jdbcTemplate.execute("""
			insert ignore into gw_iface_dim(iface_key,iface_mac,iface_name,iface_phy_name) values (
				-2147483648,
				-9223372036854775808,
				'<undefined>',
				'<undefined>'
			)""");

        List<ArubaAiClientInfoEntity> unprocessed = jdbcTemplate.query("""
			select
			
			aidis.id
			
			from network_statistics.aruba_iap_device_info_stg aidis
			
			where aidis.mark = 0
			
			order by aidis.poll_time,aidis.id
			
			limit 1000
			
			for update""",
			new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class)
		);

		String listIdUnprocessed = unprocessed
			.parallelStream()
			.map(ArubaAiClientInfoEntity::getId)
			.map(String::valueOf)
			.collect(Collectors.joining(",","(",")"));

		List<ArubaAiClientInfoEntity> processedUptime = jdbcTemplate
			.query(String.format("""
				with uptime_list_device_unprocessed as (
					select
					distinct
					aidis.device_mac,aidis.device_name,aidis.device_ip,aidis.device_wlan_mac
					from aruba_iap_device_info_stg aidis
					where aidis.id in %s
				)
				select
				id
				from (
					select
					aidis.id,
					row_number() over(
						partition by aidis.device_mac,aidis.device_name,aidis.device_ip,aidis.device_wlan_mac
						order by aidis.poll_time desc,aidis.id desc
					) as rn
					from aruba_iap_device_info_stg aidis
					where mark=1
					and (aidis.device_mac,aidis.device_name,aidis.device_ip,aidis.device_wlan_mac)
					in (select * from uptime_list_device_unprocessed)
					for update
				) x
				where rn=1""",
				listIdUnprocessed
			),
			new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class)
		);

		List<ArubaAiClientInfoEntity> processedTraffic = jdbcTemplate.query(
				String.format("""
					with uptime_list_device_unprocessed as (
						select
						distinct
						date_format(aidi.poll_time,'%%Y-%%m-%%d %%H:00:00'),
						aidi.device_mac,
						aidi.device_name
						from aruba_iap_device_info_stg aidi
						where aidi.id in %s
					)
					select
					id
					from (
						select
						aidi.id,
						row_number() over(
							partition by
								date_format(aidi.poll_time,'%%Y-%%m-%%d %%H:00:00'),
								aidi.device_mac,
								aidi.device_name
							order by aidi.poll_time desc,aidi.id desc
						) as rn
						from aruba_iap_device_info_stg aidi
						where mark=1
						and (date_format(aidi.poll_time,'%%Y-%%m-%%d %%H:00:00'),aidi.device_mac,aidi.device_name)
						in (select * from uptime_list_device_unprocessed)
						for update
					) x
					where rn=1;""",
					listIdUnprocessed
				),
				new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class));

		StringBuilder listIdUnprocessedAndProcessedUptime = new StringBuilder();
		StringBuilder listIdUnprocessedAndProcessedTraffic = new StringBuilder();

		listIdUnprocessedAndProcessedUptime.append('(');
		for (ArubaAiClientInfoEntity ai : processedUptime) {
			if (listIdUnprocessedAndProcessedUptime.length() > 1)
				listIdUnprocessedAndProcessedUptime.append(',');

			listIdUnprocessedAndProcessedUptime.append(ai.getId());
		}

		listIdUnprocessedAndProcessedTraffic.append('(');
		for (ArubaAiClientInfoEntity ai : unprocessed) {
			if (listIdUnprocessedAndProcessedTraffic.length() > 1)
				listIdUnprocessedAndProcessedTraffic.append(',');
			if (listIdUnprocessedAndProcessedUptime.length() > 1)
				listIdUnprocessedAndProcessedUptime.append(',');

			listIdUnprocessedAndProcessedUptime.append(ai.getId());
			listIdUnprocessedAndProcessedTraffic.append(ai.getId());
		}

		listIdUnprocessedAndProcessedUptime.append(')');
		for (ArubaAiClientInfoEntity ai : processedTraffic) {
			if (listIdUnprocessedAndProcessedTraffic.length() > 1)
				listIdUnprocessedAndProcessedTraffic.append(',');

			listIdUnprocessedAndProcessedTraffic.append(ai.getId());
		}
		listIdUnprocessedAndProcessedTraffic.append(')');

		log.info("listIdUnprocessed: {}", listIdUnprocessed);
		log.info("listIdUnprocessedAndProcessedUptime: {}", listIdUnprocessedAndProcessedUptime);
		log.info("listIdUnprocessedAndProcessedTraffic: {}", listIdUnprocessedAndProcessedTraffic);

		log.info("normalizing dim table");

		// normalize date
		jdbcTemplate.execute(String.format("""
				insert ignore into network_statistics.date_dim(date) (
					select
					distinct date(aidis.poll_time)
					from network_statistics.aruba_iap_device_info_stg aidis
					left join network_statistics.date_dim dd on date(aidis.poll_time) = dd.date
					where dd.date_key is null and aidis.id in %s
				)""",
				listIdUnprocessed
			)
		);

		// normalize by time
		jdbcTemplate.execute(String.format("""
				insert ignore into network_statistics.time_dim(time) (
					select
					distinct time_to_sec(time(aidis.poll_time))
					from network_statistics.aruba_iap_device_info_stg aidis
					left join network_statistics.time_dim td on time_to_sec(time(aidis.poll_time)) = td.time
					where td.time_key is null and aidis.id in %s
				 )""",
				listIdUnprocessed)
		);

		// normalize by time hour
		jdbcTemplate.execute(String.format("""
				insert ignore into network_statistics.time_dim(time) (
					select
					distinct time_to_sec( date_format(time(aidis.poll_time),'%%H:00:00') )
					from network_statistics.aruba_iap_device_info_stg aidis
					left join network_statistics.time_dim td on time_to_sec(date_format(time(aidis.poll_time),'%%H:00:00')) = td.time
					where td.time_key is null and aidis.id in %s
				)""",
				listIdUnprocessed)
		);

		// normalize by device
		jdbcTemplate.execute(String.format("""
			insert ignore into network_statistics.device_dim(device_mac,device_name,device_iface_wifi) (
				select
				distinct
				aidis.device_mac,
				aidis.device_name,
				1
				from network_statistics.aruba_iap_device_info_stg aidis
				left join network_statistics.device_dim dd on aidis.device_mac=dd.device_mac
				and aidis.device_name=dd.device_name
				where dd.device_key is null and aidis.id in %s
			)""",
			listIdUnprocessed)
		);

		// normalize by ip
		jdbcTemplate.execute(String.format("""
			insert ignore into network_statistics.ip_dim(ipv4,ipv6) (
				select
				distinct
				aidis.device_ip,
				null
				from network_statistics.aruba_iap_device_info_stg aidis
				left join network_statistics.ip_dim id on aidis.device_ip=id.ipv4
				where id.ip_key is null and aidis.id in %s
			)""",
			listIdUnprocessed)
		);

		// normalize by date up time
		jdbcTemplate.execute(String.format("""
				insert ignore into date_dim(date) (
					with connect_time as (
						select
						connect_time
						from (
							select
							aidi.device_mac,
							aidi.device_name,
							aidi.device_ip,
							aidi.device_wlan_mac,
							aidi.poll_time - interval aidi.device_uptime_seconds second as connect_time,
							timestampdiff(
								second,
								lag(aidi.poll_time - interval aidi.device_uptime_seconds second,1,'1970-01-01 00:00:00')
								over(partition by device_mac,device_name,device_ip,device_wlan_mac order by poll_time asc),
								aidi.poll_time - interval aidi.device_uptime_seconds second
							) as diff_uptime,
							aidi.mark
							from aruba_iap_device_info_stg aidi
							where aidi.id in %s
						) x
						where diff_uptime>600 and mark=0 -- ignore processed
					)
				
					select
				
					distinct date(connect_time)
				
					from connect_time ct
				
					left join date_dim dd on date(connect_time)=dd.date
					where dd.date_key is null
				)
				""",
				listIdUnprocessedAndProcessedUptime)
		);

		// normalize time uptime
		jdbcTemplate.execute(String.format("""
			insert ignore into time_dim(time) (
				with connect_time as (
					select
					connect_time
					from (
						select
						aidi.device_mac,
						aidi.device_name,
						aidi.device_ip,
						aidi.device_wlan_mac,
						aidi.poll_time - interval aidi.device_uptime_seconds second as connect_time,
						timestampdiff(
							second,
							lag(aidi.poll_time - interval aidi.device_uptime_seconds second,1,'1970-01-01 00:00:00')
							over(partition by device_mac,device_name,device_ip,device_wlan_mac order by poll_time asc),
							aidi.poll_time - interval aidi.device_uptime_seconds second
						) as diff_uptime,
						aidi.mark
						from aruba_iap_device_info_stg aidi
						where aidi.id in %s
					) x
					where diff_uptime>600 and mark=0 -- ignore processed
				)
			
				select
				distinct time_to_sec(time(connect_time))
				from connect_time ct
				left join time_dim td on time_to_sec(time(connect_time))=td.time
				where td.time_key is null
			)
			""",
			listIdUnprocessedAndProcessedUptime)
		);

		log.info("finished normalizing dim table");

		log.info("start insert into fact table");

		/*
		 * device traffic by hour fact
		 * device wlan connection fact
		 * device wlan uptime fact
		 * */
		/* update fact table device_traffic_by_hour_fact */
		jdbcTemplate.execute(String.format("""
			insert into network_statistics.device_traffic_by_hour_fact(date_key,time_key,device_key,transmission_bytes) (
				select
				*
				from (
					select
					x.date_key,
					x.time_key,
					x.device_key,
					sum(transmission_bytes) as transmission_bytes_val
					from (
						select
						dd.date_key,
						td.time_key,
						dd1.device_key,
						aidi.device_rx - if(
							lag(aidi.device_rx,1,aidi.device_rx) over(partition by dd.date_key,td.time_key,dd1.device_key order by aidi.poll_time)
							>
							aidi.device_rx,
							0,
							lag(aidi.device_rx,1,aidi.device_rx) over(partition by dd.date_key,td.time_key,dd1.device_key order by aidi.poll_time)
						)
						+
						aidi.device_tx - if(
							lag(aidi.device_tx,1,aidi.device_tx) over(partition by dd.date_key,td.time_key,dd1.device_key order by aidi.poll_time)
							>
							aidi.device_tx,
							0,
							lag(aidi.device_tx,1,aidi.device_tx) over(partition by dd.date_key,td.time_key,dd1.device_key order by aidi.poll_time)
						)
						as transmission_bytes
						from aruba_iap_device_info_stg aidi
						join date_dim dd on date(aidi.poll_time)=dd.date
						join time_dim td on time_to_sec(date_format(time(aidi.poll_time),'%%H:00:00'))=td.time
						join device_dim dd1 on aidi.device_mac=dd1.device_mac and aidi.device_name=dd1.device_name
						where aidi.id in %s
					) x
					group by 1,2,3
				) tmp
			)
			on duplicate key update transmission_bytes=transmission_bytes+transmission_bytes_val
			""",
			listIdUnprocessedAndProcessedTraffic)
		);

		// update fact device connection
		jdbcTemplate.execute(String.format("""
			insert ignore into device_wlan_connections_fact (
				with first_up_time_per_device as (
					select
					device_mac,
					device_name,
					device_ip,
					device_wlan_mac,
					connect_time
					from (
						select
						-- each time mac, name, ip, wlan mac changed -> consider it creates new connection
						-- or, can detect based on diff uptime threshold
						aidi.device_mac,
						aidi.device_name,
						aidi.device_ip,
						aidi.device_wlan_mac,
						aidi.poll_time - interval aidi.device_uptime_seconds second as connect_time,
						timestampdiff(
							second,
							lag(aidi.poll_time - interval aidi.device_uptime_seconds second,1,'1970-01-01 00:00:00')
							over(partition by device_mac,device_name,device_ip,device_wlan_mac order by poll_time asc),
							aidi.poll_time - interval aidi.device_uptime_seconds second
						) as diff_uptime,
						aidi.mark
						from aruba_iap_device_info_stg aidi
						where aidi.id in %s
					) x
					where diff_uptime>600 and mark=0 -- threshold uptime is 10 seconds
					-- ignore processed because previous of processed is 1970 -> already counted
				)
			
				select
			
				dd.date_key,
				td.time_key,
				dd1.device_key,
				id.ip_key,
				ad.ap_key,
				gid.iface_key,
				ifnull(vd.vendor_key,-2147483648),
				ifnull(vd1.vendor_key,-2147483648)
			
				from first_up_time_per_device aidi
				join date_dim dd on date(aidi.connect_time)=dd.date
				join time_dim td on time_to_sec(time(aidi.connect_time))=td.time
				join device_dim dd1 on aidi.device_mac=dd1.device_mac and aidi.device_name=dd1.device_name
				join ip_dim id on aidi.device_ip=id.ipv4
				join ap_dim ad on ad.ap_key=-2147483648
				join gw_iface_dim gid on aidi.device_wlan_mac=gid.iface_mac
				left join vendor_dim vd on aidi.device_mac>>24=vd.vendor_prefix
				left join vendor_dim vd1 on aidi.device_wlan_mac>>24=vd1.vendor_prefix
			)""",
			listIdUnprocessedAndProcessedUptime)
		);

		// update fact device metrics
		jdbcTemplate.execute(String.format("""
			insert ignore into device_wlan_metrics_fact  (
				select
			
				dd.date_key,
				td.time_key,
				dd1.device_key,
				aidi.device_snr
			
				from aruba_iap_device_info_stg aidi
			
				join date_dim dd on date(aidi.poll_time)=dd.date
				join time_dim td on time_to_sec(time(aidi.poll_time))=td.time
				join device_dim dd1 on aidi.device_mac=dd1.device_mac and aidi.device_name=dd1.device_name
			
				where aidi.id in %s
				and aidi.mark=0 -- exclude mark=1 because already inserted
			)""",
			listIdUnprocessed)
		);

		// update fact device uptime
		jdbcTemplate.execute(String.format("""
			replace into device_wlan_uptime_fact  (
				select
			
				dd1.device_key,
				id.ip_key,
				aidi.device_uptime_seconds
			
				from aruba_iap_device_info_stg aidi
			
				join device_dim dd1 on aidi.device_mac=dd1.device_mac and aidi.device_name=dd1.device_name
				join ip_dim id on aidi.device_ip=id.ipv4
			
				where aidi.id in %s
				and aidi.mark=0 -- exclude mark=1 because already inserted
			
				order by 1,2,3 desc
			)""",
			listIdUnprocessed)
		);

		log.info("insert into fact table finish");

		log.info("mark records start");

		// once completed, mark all completed
		jdbcTemplate.execute(
			String.format("""
				update aruba_iap_device_info_stg set mark=1 where id in %s""",
				listIdUnprocessed
			)
		);

		log.info("mark records completed");

		log.info("end");
    }
}
