package com.home.network.statistic.scheduler;

import java.util.List;

public interface JobSchedulerService {
    List<SchedulerDTO> getSchedulerNameList();

    List<JobDetailDTO> getAllJobDetails(String schedulerName);

    List<TriggerDTO> getAllTriggers(String schedulerName);

    boolean createJobDetail(JobDetailDTO jobDetailDTO);

    boolean scheduleJob(TriggerDTO triggerDTO, boolean removeOldTrigger);

    boolean pauseTrigger(TriggerDTO triggerDTO);

    boolean triggerJob(JobDetailDTO jobDetailDTO);

    boolean resumeTrigger(TriggerDTO triggerDTO);

    boolean deleteJobDetail(JobDetailDTO jobDetailDTO);

    boolean deleteTrigger(TriggerDTO triggerDTO);

    List<JobDetailDTO> getAllJobs();
}
