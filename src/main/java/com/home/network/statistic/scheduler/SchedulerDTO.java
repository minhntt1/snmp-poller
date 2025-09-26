package com.home.network.statistic.scheduler;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.quartz.Scheduler;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchedulerDTO {
    private String schedulerId;
    private String schedulerInstanceId;
    private String schedulerName;
    private String schedulerRunningSince;
    private Boolean schedulerRemote;
    private Boolean schedulerStarted;
    private Boolean schedulerInStandByMode;
    private Integer schedulerNumJobsExec;
    private String schedulerVersion;
    private Boolean schedulerJsPersistent;
    private Boolean schedulerJsClustered;

    public static List<SchedulerDTO> from(Map<String, Scheduler> beanToScheduler)  {
        return beanToScheduler.entrySet().stream().map(e -> new SchedulerDTO(e.getKey(), e.getValue())).toList();
    }

    @SneakyThrows
    public SchedulerDTO(String beanId, Scheduler scheduler)  {
        this.schedulerId = beanId;
        this.schedulerInstanceId = scheduler.getSchedulerInstanceId();
        this.schedulerName = scheduler.getSchedulerName();
        this.schedulerRunningSince = Optional.ofNullable(scheduler.getMetaData().getRunningSince()).map(Date::toString).orElse(null);
        this.schedulerRemote = scheduler.getMetaData().isSchedulerRemote();
        this.schedulerStarted = scheduler.isStarted();
        this.schedulerInStandByMode = scheduler.isInStandbyMode();
        this.schedulerNumJobsExec = scheduler.getMetaData().getNumberOfJobsExecuted();
        this.schedulerVersion = scheduler.getMetaData().getVersion();
        this.schedulerJsPersistent = scheduler.getMetaData().isJobStoreSupportsPersistence();
        this.schedulerJsClustered = scheduler.getMetaData().isJobStoreClustered();
    }
}
