package com.home.network.statistic.scheduler;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.quartz.*;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobDetailDTO {
    private String schedulerId;
    private String schedulerName;
    private String jobClassName;
    private String jobNm;
    private String jobGr;
    private String jobDesc;
    private Boolean isDurable;
    private Boolean persistJobDataAfterExecution;
    private Boolean concurrentExecutionDisallowed;
    private Boolean requestsRecovery;

    public static List<JobDetailDTO> from(List<Class<? extends Job>> jobs) {
        return jobs.stream().map(JobDetailDTO::new).toList();
    }

    public static List<JobDetailDTO> from(String schedulerId, Scheduler scheduler, List<JobDetail> jobDetails) {
        return jobDetails.stream().map(job -> new JobDetailDTO(schedulerId, scheduler, job)).toList();
    }

    @SneakyThrows
    public JobDetailDTO(String schedulerId, Scheduler scheduler, JobDetail jobDetail) {
        this.schedulerId = schedulerId;
        this.schedulerName = scheduler.getSchedulerName();
        this.jobNm = jobDetail.getKey().getName();
        this.jobGr = jobDetail.getKey().getGroup();
        this.jobDesc = jobDetail.getDescription();
        this.isDurable = jobDetail.isDurable();
        this.persistJobDataAfterExecution = jobDetail.isPersistJobDataAfterExecution();
        this.concurrentExecutionDisallowed = jobDetail.isConcurrentExecutionDisallowed();
        this.requestsRecovery = jobDetail.requestsRecovery();
    }

    public JobDetailDTO(Class<? extends Job> jobClass) {
        this.jobClassName = jobClass.getName();
    }

    public JobKey obtainJobKey() {
        return JobKey.jobKey(this.jobNm, this.jobGr);
    }

    @SuppressWarnings("unchecked")
    public JobDetail toJobDetail() throws ClassNotFoundException {
        Class<?> jobClass = Class.forName(this.jobClassName);
        JobBuilder jobBuilder = JobBuilder.newJob();

        if (Job.class.isAssignableFrom(jobClass)) {
            jobBuilder.ofType((Class<Job>) jobClass);
            jobBuilder.withIdentity(this.jobNm, this.jobGr);
            jobBuilder.storeDurably(this.isDurable);
            jobBuilder.withDescription(this.jobDesc);
            jobBuilder.requestRecovery(this.requestsRecovery);

            return jobBuilder.build();
        }

        return null;
    }
}
