package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.common.model.ListSqlQuery;
import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
@Slf4j
@Profile("!local")
public class ClientInfoSchedulerJob implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;
	private final ApplicationContext applicationContext;
	private final ListSqlQuery listSqlQuery;

	public ClientInfoSchedulerJob(JdbcTemplate jdbcTemplate,
								  ApplicationContext applicationContext,
								  @Qualifier("clientInfoQuery")
								  ListSqlQuery listSqlQuery) {
		this.jdbcTemplate = jdbcTemplate;
		this.applicationContext = applicationContext;
		this.listSqlQuery = listSqlQuery;
	}

	public  void commonNormalize() {
		// add to dim table
		// normalize unspecified vendor
		jdbcTemplate.execute(listSqlQuery.getQueryValue("addUndefinedVendorDim"));

		// normalize unspecified ap/iface column
		jdbcTemplate.execute(listSqlQuery.getQueryValue("addUndefinedApDim"));

		jdbcTemplate.execute(listSqlQuery.getQueryValue("addUndefinedGwIfaceDim"));
	}

	public  List<ArubaAiClientInfoEntity> getListUnprocessed() {
		return jdbcTemplate.query(listSqlQuery.getQueryValue("getListUnprocessed"),
			new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class)
		);
	}

	public  List<ArubaAiClientInfoEntity> getListProcessedUptime(String listIdUnprocessed) {
		return jdbcTemplate
			.query(
				String.format(listSqlQuery.getQueryValue("getListProcessedUptime"),
					listIdUnprocessed
				),
				new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class)
			);
	}

	public  List<ArubaAiClientInfoEntity> getListProcessedTraffic(String listIdUnprocessed) {
		return jdbcTemplate.query(
			String.format(listSqlQuery.getQueryValue("getListProcessedTraffic"),
				listIdUnprocessed
			),
			new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class)
		);
	}

	public  void insertIntoDateDim(String listIdUnprocessed) {
		// normalize date
		jdbcTemplate.execute(
			String.format(listSqlQuery.getQueryValue("insertIntoDateDim"),
				listIdUnprocessed
			)
		);
	}

	public  void insertIntoTimeDim(String listIdUnprocessed) {
		// normalize by time
		jdbcTemplate.execute(String.format(listSqlQuery.getQueryValue("insertIntoTimeDim"),
			listIdUnprocessed)
		);
	}

	public  void insertIntoTimeDimHourNorm(String listIdUnprocessed) {
		// normalize by time hour
		jdbcTemplate.execute(String.format(listSqlQuery.getQueryValue("insertIntoTimeDimHourNorm"),
			listIdUnprocessed)
		);
	}

	public  void insertIntoDeviceDim(String listIdUnprocessed) {
		// normalize by device
		jdbcTemplate.execute(String.format(
				listSqlQuery.getQueryValue("insertIntoDeviceDim"),
			listIdUnprocessed)
		);
	}

	public  void insertIntoIpDim(String listIdUnprocessed) {
		// normalize by ip
		jdbcTemplate.execute(String.format(listSqlQuery.getQueryValue("insertIntoIpDim"),
			listIdUnprocessed)
		);
	}

	public  void insertIntoDateDimDateUptimeNorm(String listIdUnprocessedAndProcessedUptime) {
		// normalize by date up time
		jdbcTemplate.execute(String.format(listSqlQuery
						.getQueryValue("insertIntoDateDimDateUptimeNorm"),
			listIdUnprocessedAndProcessedUptime)
		);
	}

	public  void insertIntoDateDimTimeUptimeNorm(String listIdUnprocessedAndProcessedUptime) {
		jdbcTemplate.execute(String.format(listSqlQuery.getQueryValue("insertIntoDateDimTimeUptimeNorm"),
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
			String.format(listSqlQuery.getQueryValue("getListUnprocessedForUpdate"),
				listIdUnprocessed
			)
		);

		jdbcTemplate.execute(
			String.format(listSqlQuery.getQueryValue("getListProcessedTrafficForShare"),
				listIdProcessedTraffic
			)
		);

		jdbcTemplate.execute(
			String.format(listSqlQuery.getQueryValue("getListProcessedUptimeForShare"),
				listIdProcessedUptime
			)
		);

		/* update fact table device_traffic_by_hour_fact */
		jdbcTemplate.execute(String.format(listSqlQuery.getQueryValue("updateFactTableDeviceTrafficByHour"),
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
		jdbcTemplate.execute(String.format(listSqlQuery.getQueryValue("updateFactTableDeviceWlanConnections"),
			listIdUnprocessedAndProcessedUptime)
		);

		// update fact device metrics
		jdbcTemplate.execute(String.format(listSqlQuery.getQueryValue("updateFactTableDeviceWlanMetrics"),
			listIdUnprocessed)
		);

		// update fact device uptime
		jdbcTemplate.execute(String.format(listSqlQuery.getQueryValue("updateFactTableDeviceWlanUptime"),
			listIdUnprocessed)
		);

		log.info("mark records start");

		// once completed, mark all completed
		jdbcTemplate.execute(
			String.format(listSqlQuery.getQueryValue("markComplete"),
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
