package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiClientInfoEntity;
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
public class ClientInfoSchedulerJob implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;
	private final ApplicationContext applicationContext;

	public  void commonNormalize() {
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
	}

	public  List<ArubaAiClientInfoEntity> getListUnprocessed() {
		return jdbcTemplate.query("""
			select
			
			aidis.id
			
			from network_statistics.aruba_iap_device_info_stg aidis
			
			where aidis.mark = 0
			
			order by aidis.id
			
			limit 10000""",
			new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class)
		);
	}

	public  List<ArubaAiClientInfoEntity> getListProcessedUptime(String listIdUnprocessed) {
		return jdbcTemplate
			.query(
				String.format("""
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
						from (
							select
							*
							from aruba_iap_device_info_stg aidi
							where mark=1
							order by id desc
							limit 2000 -- limit to earliest 1000 processed records to look back (~ last 2 hrs)
						) aidis
						where (aidis.device_mac,aidis.device_name,aidis.device_ip,aidis.device_wlan_mac)
						in (select * from uptime_list_device_unprocessed)
					) x
					where rn=1""",
					listIdUnprocessed
				),
				new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class)
			);
	}

	public  List<ArubaAiClientInfoEntity> getListProcessedTraffic(String listIdUnprocessed) {
		return jdbcTemplate.query(
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
					from (
						select
						*
						from aruba_iap_device_info_stg aidi
						where mark=1
						order by id desc
						limit 2000 -- limit to earliest 1000 processed records to look back (~ last 2 hrs)
					) aidi
					where (date_format(aidi.poll_time,'%%Y-%%m-%%d %%H:00:00'),aidi.device_mac,aidi.device_name)
					in (select * from uptime_list_device_unprocessed)
				) x
				where rn=1;""",
				listIdUnprocessed
			),
			new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class)
		);
	}

	public  void insertIntoDateDim(String listIdUnprocessed) {
		// normalize date
		jdbcTemplate.execute(
			String.format("""
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
	}

	public  void insertIntoTimeDim(String listIdUnprocessed) {
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
	}

	public  void insertIntoTimeDimHourNorm(String listIdUnprocessed) {
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
	}

	public  void insertIntoDeviceDim(String listIdUnprocessed) {
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
	}

	public  void insertIntoIpDim(String listIdUnprocessed) {
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
	}

	public  void insertIntoDateDimDateUptimeNorm(String listIdUnprocessedAndProcessedUptime) {
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
							over(partition by device_mac,device_name,device_ip,device_wlan_mac order by poll_time,id),
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
	}

	public  void insertIntoDateDimTimeUptimeNorm(String listIdUnprocessedAndProcessedUptime) {
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
							over(partition by device_mac,device_name,device_ip,device_wlan_mac order by poll_time,id),
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
	}

	@Transactional
	public void summarizeData(String listIdUnprocessed,
							   String listIdProcessedTraffic,
							   String listIdProcessedUptime,
							   String listIdUnprocessedAndProcessedUptime,
							   String listIdUnprocessedAndProcessedTraffic) {
		log.info("start summarizing data");

		jdbcTemplate.execute(
			String.format("""
				select
				aidis.id
				from network_statistics.aruba_iap_device_info_stg aidis
				where aidis.mark = 0
				and aidis.id in %s
				for update""",
				listIdUnprocessed
			)
		);

		jdbcTemplate.execute(
			String.format("""
				select
				aidis.id
				from network_statistics.aruba_iap_device_info_stg aidis
				where aidis.mark = 1
				and aidis.id in %s
				for share""",
				listIdProcessedTraffic
			)
		);

		jdbcTemplate.execute(
			String.format("""
				select
				aidis.id
				from network_statistics.aruba_iap_device_info_stg aidis
				where aidis.mark = 1
				and aidis.id in %s
				for share""",
				listIdProcessedUptime
			)
		);

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
							lag(aidi.device_rx,1,aidi.device_rx) over(partition by dd.date_key,td.time_key,dd1.device_key order by aidi.poll_time,aidi.id)
							>
							aidi.device_rx,
							0,
							lag(aidi.device_rx,1,aidi.device_rx) over(partition by dd.date_key,td.time_key,dd1.device_key order by aidi.poll_time,aidi.id)
						)
						+
						aidi.device_tx - if(
							lag(aidi.device_tx,1,aidi.device_tx) over(partition by dd.date_key,td.time_key,dd1.device_key order by aidi.poll_time,aidi.id)
							>
							aidi.device_tx,
							0,
							lag(aidi.device_tx,1,aidi.device_tx) over(partition by dd.date_key,td.time_key,dd1.device_key order by aidi.poll_time,aidi.id)
						)
						as transmission_bytes
						from aruba_iap_device_info_stg aidi
						join date_dim dd on dd.date_key=(
							select date_key from
							date_dim
							where date(aidi.poll_time)=date
							order by date_key desc limit 1
						)
						join time_dim td on td.time_key=(
							select time_key from
							time_dim
							where time_to_sec(date_format(time(aidi.poll_time),'%%H:00:00'))=time
							order by time_key desc limit 1
						) 
						join device_dim dd1 on dd1.device_key=(
							select device_key from
							device_dim
							where device_iface_wifi=1 and aidi.device_mac=device_mac and aidi.device_name=device_name
							order by device_key desc limit 1
						) 
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
		/*
		* NOTE: WHEN THERE IS STILL TWO ENTRIES WITH SAME MAC,NAME,IP,WLAN_MAC COEXIST IN FACT TABLE
		* BUT DIFF CONNECT TIME IS < 600 (10 MINS)
		* BECAUSE EARLIER FETCH LATEST MARKED FOR UPTIME BY POLLTIME DESC
		* BUT DEVICE UPTIME GET FROM AP IS UNSTABLE, LATEST MARKED IS NOT THE LATEST UPTIME, EXAMPLE:
		* IN FIRST POLL
		* ID,DEVICE,IP,IFACE,CONNECT_TIME,MARK
		* 1,1,1,1,1900-01-01 00:00:00,0
		* 2,1,1,1,1900-01-01 00:10:00,0 -> THIS ENTRY DIFFERENCE WITH PREV IS >= 10 -> PUT INTO FACT TABLE
		*
		* IN SECOND POLL
		* 3,1,1,1,1900-01-01 00:00:00,1
		* 4,1,1,1,1900-01-01 00:10:00,1
		* 5,1,1,1,1900-01-01 00:06:00,0	-> THIS ENTRY DIFFERENCE WITH PREV LATEST POLL TIMR MARKED IS < 10 -> IGNORE
		* 6,1,1,1,1900-01-01 00:16:00,0	-> THIS ENTRY DIFFERENCE WITH PREV IS >= 10 -> WRITE TO FACT TABLE
		*
		* -> 1900-01-01 00:16:00 and 1900-01-01 00:10:00 coexist in db
		* */
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
							over(partition by device_mac,device_name,device_ip,device_wlan_mac order by poll_time,id),
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
				join date_dim dd on dd.date_key=(
					select date_key from
					date_dim
					where date(aidi.connect_time)=date
					order by date_key desc limit 1
				) 
				join time_dim td on td.time_key=(
					select time_key from
					time_dim
					where time_to_sec(time(aidi.connect_time))=time
					order by time_key desc limit 1
				) 
				join device_dim dd1 on dd1.device_key=(
					select device_key from
					device_dim
					where device_iface_wifi=1 and aidi.device_mac=device_mac and aidi.device_name=device_name
					order by device_key desc limit 1
				)
				join ip_dim id on id.ip_key=(
					select ip_key from
					ip_dim
					where aidi.device_ip=ipv4
					order by ip_key desc limit 1
				) 
				join ap_dim ad on ad.ap_key=(
					select ap_key from
					ap_dim
					where ap_key=-2147483648
					order by ap_key desc limit 1
				) 
				join gw_iface_dim gid on gid.iface_key=(
					select iface_key from
					gw_iface_dim
					where aidi.device_wlan_mac=iface_mac
					order by iface_key desc limit 1 
				)
				left join vendor_dim vd on vd.vendor_key=(
					select vendor_key from
					vendor_dim
					where aidi.device_mac>>24=vendor_prefix 
					order by vendor_key desc limit 1
				) 
				left join vendor_dim vd1 on vd1.vendor_key=(
					select vendor_key from
					vendor_dim
					where aidi.device_wlan_mac>>24=vendor_prefix 
					order by vendor_key desc limit 1
				) 
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
			
				join date_dim dd on dd.date_key=(
					select date_key from
					date_dim 
					where date(aidi.poll_time)=date
					order by date_key desc limit 1
				)
				join time_dim td on td.time_key=(
					select time_key from
					time_dim
					where time_to_sec(time(aidi.poll_time))=time
					order by time_key desc limit 1
				)
				join device_dim dd1 on dd1.device_key=(
					select device_key from
					device_dim
					where device_iface_wifi=1 and  aidi.device_mac=device_mac and aidi.device_name=device_name
					order by device_key desc limit 1
				)
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
			
				join device_dim dd1 on dd1.device_key=(
					select device_key from
					device_dim
					where device_iface_wifi=1 and aidi.device_mac=device_mac and aidi.device_name=device_name
					order by device_key desc limit 1
				) 
				join ip_dim id on id.ip_key=(
					select ip_key from
					ip_dim
					where aidi.device_ip=ipv4
					order by ip_key desc limit 1
				) 
			
				where aidi.id in %s
				and aidi.mark=0 -- exclude mark=1 because already inserted
			
				order by 1,2,3 desc
			)""",
			listIdUnprocessed)
		);

		log.info("mark records start");

		// once completed, mark all completed
		jdbcTemplate.execute(
			String.format("""
				update aruba_iap_device_info_stg set mark=1 where id in %s""",
				listIdUnprocessed
			)
		);

		log.info("mark records completed");

		log.info("end summarizing data");
	}

    @Override
    @Scheduled(fixedRate = 600_000) // 10 min
    public void start() {
		log.info("start");

		this.commonNormalize();

		List<ArubaAiClientInfoEntity> listUnprocessed = this.getListUnprocessed();

		if (listUnprocessed.isEmpty()) {
			log.info("no data found");
			return;
		}

		String listIdUnprocessed = ArubaAiClientInfoEntity.constructIdString(listUnprocessed.parallelStream());

		this.insertIntoDateDim(listIdUnprocessed);
		this.insertIntoTimeDim(listIdUnprocessed);
		this.insertIntoTimeDimHourNorm(listIdUnprocessed);
		this.insertIntoDeviceDim(listIdUnprocessed);
		this.insertIntoIpDim(listIdUnprocessed);
		this.insertIntoDateDimDateUptimeNorm(listIdUnprocessed);
		this.insertIntoDateDimTimeUptimeNorm(listIdUnprocessed);

		List<ArubaAiClientInfoEntity> listProcessedTraffic = this.getListProcessedTraffic(listIdUnprocessed);
		List<ArubaAiClientInfoEntity> listProcessedUptime = this.getListProcessedUptime(listIdUnprocessed);

		if (listProcessedTraffic.isEmpty()) {
			listProcessedTraffic.add(ArubaAiClientInfoEntity.builder().id(-1L).build());
		}
		if (listProcessedUptime.isEmpty()) {
			listProcessedUptime.add(ArubaAiClientInfoEntity.builder().id(-1L).build());
		}

		String listIdProcessedTraffic = ArubaAiClientInfoEntity.constructIdString(listProcessedTraffic.parallelStream());
		String listIdProcessedUptime = ArubaAiClientInfoEntity.constructIdString(listProcessedUptime.parallelStream());
		String listIdUnprocessedAndProcessedUptime = ArubaAiClientInfoEntity.constructIdString(
			Stream.concat(listUnprocessed.parallelStream(), listProcessedTraffic.parallelStream())
		);
		String listIdUnprocessedAndProcessedTraffic = ArubaAiClientInfoEntity.constructIdString(
			Stream.concat(listUnprocessed.parallelStream(), listProcessedUptime.parallelStream())
		);

		ClientInfoSchedulerJob ctx = applicationContext.getBean(this.getClass());

		ctx.summarizeData(listIdUnprocessed, listIdProcessedTraffic, listIdProcessedUptime, listIdUnprocessedAndProcessedUptime, listIdUnprocessedAndProcessedTraffic);

		log.info("end");
    }
}
