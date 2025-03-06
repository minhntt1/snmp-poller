package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientInfoSchedulerJob implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @Scheduled(fixedRate = 60_000) // 1 min
    public void start() {
        List<ArubaAiClientInfoEntity> clientInfoEntities = jdbcTemplate
            .query("""
				with cte as(
				select
				
				aidis3.id,aidis3.mark
				
				from network_statistics.aruba_iap_device_info_stg  aidis3
				
				where aidis3.id in (
				select
				
				x.id
				
				from ( -- for every device mac, device name, select its latest poll time
				select
				
				aidis.id,
				row_number() over(partition by aidis.device_mac,aidis.device_name order by aidis.poll_time desc,aidis.id desc) as rn
				
				from network_statistics.aruba_iap_device_info_stg aidis
				
				where aidis.mark = 1
				) x
				
				where x.rn = 1
				)
				
				for update
				), cte2 as(
				select
				
				aidis.id,aidis.mark
				
				from network_statistics.aruba_iap_device_info_stg aidis
				
				where aidis.mark = 0
				
				order by aidis.poll_time,aidis.id
				
				limit 100
				
				for update
				)
				
				select * from cte
				union
				select * from cte2""",
                new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class)
            );

		log.info("total records: {}", clientInfoEntities.size());

		long minId = clientInfoEntities.stream()
				.map(ArubaAiClientInfoEntity::getId)
				.min(Long::compareTo)
				.orElse(0L);

		long maxId = clientInfoEntities.stream()
				.map(ArubaAiClientInfoEntity::getId)
				.max(Long::compareTo)
				.orElse(0L);

		// normalize date
		jdbcTemplate.execute(String.format("""
				insert ignore into network_statistics.date_dim(date) (
					select
					distinct date(aidis.poll_time)
					from network_statistics.aruba_iap_device_info_stg aidis
					left join network_statistics.date_dim dd on date(aidis.poll_time) = dd.date
					where dd.date_key is null and aidis.id between %d and %d
				)""",
				minId, maxId)
		);

		// normalize by time
		jdbcTemplate.execute(String.format("""
				insert ignore into network_statistics.time_dim(time) (
					select
					distinct time_to_sec(time(aidis.poll_time))
					from network_statistics.aruba_iap_device_info_stg aidis
					left join network_statistics.time_dim td on time_to_sec(time(aidis.poll_time)) = td.time
					where td.time_key is null and aidis.id between %d and %d
				 )""",
				minId, maxId)
		);

		// normalize by time hour
		jdbcTemplate.execute(String.format("""
				insert ignore into network_statistics.time_dim(time) (
					select
					distinct time_to_sec( date_format(time(aidis.poll_time),'%%H:00:00') )
					from network_statistics.aruba_iap_device_info_stg aidis
					left join network_statistics.time_dim td on time_to_sec(date_format(time(aidis.poll_time),'%%H:00:00')) = td.time
					where td.time_key is null and aidis.id between %d and %d
				)""",
				minId, maxId)
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
				where dd.device_key is null and aidis.id between %d and %d
			)""",
			minId, maxId)
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
				where id.ip_key is null and aidis.id between %d and %d
			)""",
			minId, maxId)
		);


    }
}
