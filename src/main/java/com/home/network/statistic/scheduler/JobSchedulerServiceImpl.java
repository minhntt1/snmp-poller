package com.home.network.statistic.scheduler;

import com.home.network.statistic.common.util.SchedulerUtil;
import com.home.network.statistic.poller.aruba.iap.etl.job.ApInfoJob;
import com.home.network.statistic.poller.aruba.iap.etl.job.ApWlanTrafficJob;
import com.home.network.statistic.poller.aruba.iap.etl.job.ClientInfoJob;
import com.home.network.statistic.poller.aruba.iap.in.job.ArubaSnmpAiPollApInfoJob;
import com.home.network.statistic.poller.aruba.iap.in.job.ArubaSnmpAiPollClientInfoJob;
import com.home.network.statistic.poller.aruba.iap.in.job.ArubaSnmpAiPollWlanTrafficJob;
import com.home.network.statistic.poller.igate.gw240.etl.job.StatusWifiStationJob;
import com.home.network.statistic.poller.igate.gw240.in.job.Igate240StatusWifiStationJob;
import com.home.network.statistic.poller.rfc1213.igate.etl.job.Rfc1213IgateJob;
import com.home.network.statistic.poller.rfc1213.igate.in.job.Rfc1213SnmpIgatePollingJob;
import com.home.network.statistic.vendor.VendorJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Profile({"dev-scheduler", "prd-scheduler"})
@RequiredArgsConstructor
@Slf4j
// must wrap scheduler inside @transactional because quartz connection is not auto commit
public class JobSchedulerServiceImpl implements JobSchedulerService {
    private final ApplicationContext ctx;
    private static final List<Class<? extends Job>> jobList = new ArrayList<>();

    static {
        // aruba
        jobList.add(ApInfoJob.class);
        jobList.add(ApWlanTrafficJob.class);
        jobList.add(ClientInfoJob.class);
        jobList.add(ArubaSnmpAiPollApInfoJob.class);
        jobList.add(ArubaSnmpAiPollClientInfoJob.class);
        jobList.add(ArubaSnmpAiPollWlanTrafficJob.class);
        // rfc1213 igate
        jobList.add(Rfc1213IgateJob.class);
        jobList.add(Rfc1213SnmpIgatePollingJob.class);
        // vendor poll job
        jobList.add(VendorJob.class);
        // igate240 web job
        jobList.add(Igate240StatusWifiStationJob.class);
        jobList.add(StatusWifiStationJob.class);
    }

    @Override
    @Transactional(value = "quartzSchedulerTx", readOnly = true)
    public List<SchedulerDTO> getSchedulerNameList() {
        return SchedulerDTO.from(ctx.getBeansOfType(Scheduler.class));
    }

    @Override
    @Transactional(value = "quartzSchedulerTx", readOnly = true)
    public List<JobDetailDTO> getAllJobDetails(String schedulerName) {
        try {
            Scheduler scheduler = ctx.getBean(schedulerName, Scheduler.class);
            var schedulerWrapper = new SchedulerUtil(scheduler);

            return JobDetailDTO.from(schedulerName, scheduler, schedulerWrapper.getJobDetails());
        } catch (Exception e) {
            log.error("error", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(value = "quartzSchedulerTx", readOnly = true)
    public List<TriggerDTO> getAllTriggers(String schedulerName) {
        try {
            Scheduler scheduler = ctx.getBean(schedulerName, Scheduler.class);
            var schedulerWrapper = new SchedulerUtil(scheduler);

            return TriggerDTO.from(schedulerName, scheduler, schedulerWrapper.getTriggers());
        } catch (Exception e) {
            log.error("getAllTriggers", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(value = "quartzSchedulerTx")
    public boolean createJobDetail(JobDetailDTO jobDetailDTO) {
        try {
            JobDetail jd = jobDetailDTO.toJobDetail();;
            Scheduler scheduler = ctx.getBean(jobDetailDTO.getSchedulerId(), Scheduler.class);
            scheduler.addJob(jd, true, true);

            return true;
        } catch (Exception e) {
            log.error("createJobDetail", e);
            return false;
        }
    }

    @Override
    @Transactional(value = "quartzSchedulerTx")
    public boolean scheduleJob(TriggerDTO triggerDTO, boolean removeOldTrigger) {
        try {
            Scheduler scheduler = ctx.getBean(triggerDTO.getSchedulerId(), Scheduler.class);
            var util = new SchedulerUtil(scheduler);

            // a job, if have old trigger, and new trigger is same as old trigger key -> reschedule
            // if old trigger and new trigger have different trigger key -> schedule, and if possible, delete old trigger
            // if old job not link to trigger -> schedule

            var oldTrigger = util.checkJobHasTrigger(triggerDTO.obtainJobKey());
            var newTrigger = triggerDTO.toTrigger();

            if (oldTrigger.isPresent()) { // if job exist with trigger before
                if (oldTrigger.get().equals(newTrigger)) { // if new trigger and old trigger same with current job
                    // reschedule job
                    scheduler.rescheduleJob(triggerDTO.obtainTriggerKey(), newTrigger);
                } else {        // new trigger not same with current job's trigger
                    if (removeOldTrigger)
                        scheduler.unscheduleJob(oldTrigger.get().getKey());

                    // schedule job
                    scheduler.scheduleJob(newTrigger);
                }
            } else {
                // schedule job
                scheduler.scheduleJob(newTrigger);
            }

            return true;
        } catch (Exception e) {
            log.error("scheduleJob", e);
            return false;
        }
    }

    @Override
    @Transactional(value = "quartzSchedulerTx")
    public boolean pauseTrigger(TriggerDTO triggerDTO) {
        try {
            Scheduler scheduler = ctx.getBean(triggerDTO.getSchedulerId(), Scheduler.class);
            scheduler.pauseTrigger(triggerDTO.obtainTriggerKey());
            return true;
        } catch (Exception e) {
            log.error("pauseTrigger", e);
            return false;
        }
    }

    @Override
    @Transactional(value = "quartzSchedulerTx")
    public boolean triggerJob(JobDetailDTO jobDetailDTO) {
        try {
            Scheduler scheduler = ctx.getBean(jobDetailDTO.getSchedulerId(), Scheduler.class);
            scheduler.triggerJob(jobDetailDTO.obtainJobKey());
            return true;
        } catch (Exception e) {
            log.error("triggerJob", e);
            return false;
        }
    }

    @Override
    @Transactional(value = "quartzSchedulerTx")
    public boolean resumeTrigger(TriggerDTO triggerDTO) {
        try {
            Scheduler scheduler = ctx.getBean(triggerDTO.getSchedulerId(), Scheduler.class);
            scheduler.resumeTrigger(triggerDTO.obtainTriggerKey());
            return true;
        } catch (Exception e) {
            log.error("resumeTrigger", e);
            return false;
        }
    }

    @Override
    @Transactional(value = "quartzSchedulerTx")
    public boolean deleteJobDetail(JobDetailDTO jobDetailDTO) {
        try {
            Scheduler scheduler = ctx.getBean(jobDetailDTO.getSchedulerId(), Scheduler.class);
            scheduler.deleteJob(jobDetailDTO.obtainJobKey());
            return true;
        } catch (Exception e) {
            log.error("deleteJobDetail", e);
            return false;
        }
    }

    @Override
    @Transactional(value = "quartzSchedulerTx")
    public boolean deleteTrigger(TriggerDTO triggerDTO) {
        try {
            Scheduler scheduler = ctx.getBean(triggerDTO.getSchedulerId(), Scheduler.class);
            scheduler.unscheduleJob(triggerDTO.obtainTriggerKey());
            return true;
        } catch (Exception e) {
            log.error("deleteTrigger", e);
            return false;
        }
    }

    @Override
    public List<JobDetailDTO> getAllJobs() {
        return JobDetailDTO.from(jobList);
    }
}
