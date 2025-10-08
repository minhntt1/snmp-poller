package com.home.network.statistic.poller.aruba.iap.etl.service;

import com.home.network.statistic.common.model.ListSqlQuery;
import com.home.network.statistic.poller.aruba.iap.etl.ClientTrafficHourlyCount;
import com.home.network.statistic.poller.aruba.iap.etl.ClientUptimeRecord;
import com.home.network.statistic.poller.aruba.iap.etl.ClientWlanConnectEvent;
import com.home.network.statistic.poller.aruba.iap.etl.ClientWlanMetricEvent;
import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Profile({"dev-executor","prd-executor"})
public class ClientInfoService implements BaseService {
    private final JdbcTemplate jdbcTemplate;
	private final ApplicationContext applicationContext;
	private final ListSqlQuery listSqlQuery;

	public ClientInfoService(@Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
							 ApplicationContext applicationContext,
							 @Qualifier("clientInfoQuery") ListSqlQuery listSqlQuery) {
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

	public  void insertIntoDateDim() {
		// normalize date
		jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoDateDim"));
	}

	public  void insertIntoTimeDim() {
		// normalize by time
		jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoTimeDim"));
	}

	public  void insertIntoTimeDimHourNorm() {
		// normalize by time hour
		jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoTimeDimHourNorm"));
	}

	public  void insertIntoDeviceDim() {
		// normalize by device
		jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoDeviceDim"));
	}

	public  void insertIntoIpDim() {
		// normalize by ip
		jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoIpDim"));
	}

	public  void insertIntoDateDimDateUptimeNorm() {
		// normalize by date up time
		jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoDateDimDateUptimeNorm"));
	}

	public  void insertIntoDateDimTimeUptimeNorm() {
		jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoDateDimTimeUptimeNorm"));
	}

	@Transactional(value = "appTx")
	public void summarizeData(JobExecutionContext context) {
		log.info("start summarizing data");

		// get device state context
		var deviceStateMap = context.getJobDetail().getJobDataMap();

		// maintain copy of state to keep track of what devices to remove
		var copyDeviceStateMap = new HashMap<>(Map.copyOf(deviceStateMap));

		// define list to store wlan connection, wlan metric, uptime per device, and hourly traffic
		var clientWlanConnections = new ArrayList<ClientWlanConnectEvent>();
		var clientWlanMetrics = new ArrayList<ClientWlanMetricEvent>();
		var clientUptimeRecords = new HashMap<ClientUptimeRecord, ClientUptimeRecord>();
		var clientHourlyTraffics = new HashMap<ClientTrafficHourlyCount, ClientTrafficHourlyCount>();

		try (var stream = jdbcTemplate.queryForStream(listSqlQuery.getQueryValue("getAllStaging"), new BeanPropertyRowMapper<>(ArubaAiClientInfoEntity.class))) {
			for (var it = stream.iterator(); it.hasNext();) {
				// curr device state
				var currState = it.next();
				// get device key based on device mac and device name, and device iface wifi because considering
				// if same mac but name change, it is also a reconnection
				var stateKey = currState.obtainJobStateKey();

				// get prev device state key
				var prevState = ArubaAiClientInfoEntity.from(deviceStateMap.getString(stateKey));

				// update uptime regardless of previous state
				var deviceUptime = new ClientUptimeRecord(currState);
				clientUptimeRecords.computeIfAbsent(deviceUptime, kk -> deviceUptime).updateDeviceUptimeSeconds(currState);

				// create metric event regardless of prev state
				clientWlanMetrics.add(new ClientWlanMetricEvent(currState));

				if (prevState != null) {
					var deviceHourlyTraffic = new ClientTrafficHourlyCount(currState);

					if (prevState.checkReconnect(currState))
						clientWlanConnections.add(new ClientWlanConnectEvent(currState));

					clientHourlyTraffics.computeIfAbsent(deviceHourlyTraffic, kk -> deviceHourlyTraffic).adjustTraffic(prevState, currState);
				} else {	// if device appear first time, no prev state
					clientWlanConnections.add(new ClientWlanConnectEvent(currState));
				}

				// update state
				deviceStateMap.put(stateKey, currState.toJson());

				// mark current state not going to remove in below commands
				copyDeviceStateMap.remove(stateKey);
			}
		}

		// free up state storage: removing devices dont appear in current batch (its state will highly likely not usable, because its state is not continuos)
		for (var key : copyDeviceStateMap.keySet()) {
			deviceStateMap.remove(key);
		}

		var queryClientWlanConnections = ClientWlanConnectEvent.obtainSqlQuery(clientWlanConnections);
		var queryClientWlanMetrics = ClientWlanMetricEvent.obtainSqlQuery(clientWlanMetrics);
		var queryClientUptimeRecords = ClientUptimeRecord.obtainSqlQuery(clientUptimeRecords);
		var queryClientHourlyTraffics = ClientTrafficHourlyCount.obtainSqlQuery(clientHourlyTraffics);

		// summarize only when there is data
		if (!queryClientWlanConnections.isBlank())
			jdbcTemplate.execute(listSqlQuery.getQueryValue("updateFactTableDeviceWlanConnections").formatted(queryClientWlanConnections));
		if (!queryClientHourlyTraffics.isBlank())
			jdbcTemplate.execute(listSqlQuery.getQueryValue("updateFactTableDeviceTrafficByHour").formatted(queryClientHourlyTraffics));
		if (!queryClientWlanMetrics.isBlank())
			jdbcTemplate.execute(listSqlQuery.getQueryValue("updateFactTableDeviceWlanMetrics").formatted(queryClientWlanMetrics));
		if (!queryClientUptimeRecords.isBlank())
			jdbcTemplate.execute(listSqlQuery.getQueryValue("updateFactTableDeviceWlanUptime").formatted(queryClientUptimeRecords));

		log.info("end summarizing data");
	}

	public void cleanUpBatch() {
		// copy all staging data to archive
		jdbcTemplate.execute(listSqlQuery.getQueryValue("copyStgToArchive"));

		// drop current staging (batch processing table)
		jdbcTemplate.execute(listSqlQuery.getQueryValue("dropCurrentStg"));

		// create new batch process table
		jdbcTemplate.execute(listSqlQuery.getQueryValue("createNewStg"));

		// flip new batch process table with ingest table for further processing
		jdbcTemplate.execute(listSqlQuery.getQueryValue("moveStgToIngest"));
	}

    @Override
    public void start(JobExecutionContext context) {
		log.info("start");

		// normalize by inserting null, etc
		this.commonNormalize();

		// normalize data in stg table
		insertIntoDateDim();
		insertIntoTimeDim();
		insertIntoTimeDimHourNorm();
		insertIntoDeviceDim();
		insertIntoIpDim();
		insertIntoDateDimDateUptimeNorm();
		insertIntoDateDimTimeUptimeNorm();

		// summarize data
		applicationContext.getBean(ClientInfoService.class).summarizeData(context);

		// clean up batch
		cleanUpBatch();

		log.info("end");
    }
}
