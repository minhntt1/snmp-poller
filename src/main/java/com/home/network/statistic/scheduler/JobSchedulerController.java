package com.home.network.statistic.scheduler;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Profile({"dev-scheduler", "prd-scheduler"})
@RequiredArgsConstructor
public class JobSchedulerController {
    private final JobSchedulerService jobSchedulerService;

    @GetMapping("/scheduler")
    @Operation(summary = "Get list of registered scheduler beans")
    public List<SchedulerDTO> getScheduler() {
        return jobSchedulerService.getSchedulerNameList();
    }

    @GetMapping("/trigger")
    @Operation(summary = "Get list of registered triggers with current scheduler")
    public List<TriggerDTO> getTrigger(@RequestParam("schedulerId") String schedulerId) {
        return jobSchedulerService.getAllTriggers(schedulerId);
    }

    @PostMapping("/trigger/schedule")
    @Operation(summary = "Scheduler / Reschedule a job with trigger, removeOldTrigger true if job link with trigger before, and different from new trigger")
    public Boolean scheduleJob(@RequestParam Boolean removeOldTrigger, @RequestBody TriggerDTO triggerDTO) {
        return jobSchedulerService.scheduleJob(triggerDTO, removeOldTrigger);
    }

    @PostMapping("/trigger/pause")
    @Operation(summary = "Pause current trigger, specify trigger identity")
    public Boolean pauseTrigger(@RequestBody TriggerDTO triggerDTO) {
        return jobSchedulerService.pauseTrigger(triggerDTO);
    }

    @PostMapping("/trigger/resume")
    @Operation(summary = "Resume current trigger, specify trigger identity")
    public Boolean resumeTrigger(@RequestBody TriggerDTO triggerDTO) {
        return jobSchedulerService.resumeTrigger(triggerDTO);
    }

    @DeleteMapping("/trigger")
    @Operation(summary = "Delete current trigger, specify trigger identity")
    public Boolean deleteTrigger(@RequestBody TriggerDTO triggerDTO) {
        return jobSchedulerService.deleteTrigger(triggerDTO);
    }

    @GetMapping("/job/detail")
    @Operation(summary = "Get list registered job details by scheduler id")
    public List<JobDetailDTO> getJobDetail(@RequestParam("schedulerId") String schedulerId) {
        return jobSchedulerService.getAllJobDetails(schedulerId);
    }

    @PostMapping("/job/detail")
    @Operation(summary = "Create job detail by scheduler id, specify job class, job id")
    public Boolean createJobDetail(@RequestBody JobDetailDTO jobDetailDTO) {
        return jobSchedulerService.createJobDetail(jobDetailDTO);
    }

    @DeleteMapping("/job/detail")
    @Operation(summary = "Delete job detail by scheduler id, specify job id")
    public Boolean deleteJobDetail(@RequestBody JobDetailDTO jobDetailDTO) {
        return jobSchedulerService.deleteJobDetail(jobDetailDTO);
    }

    @PostMapping("/job/trigger")
    @Operation(summary = "Manually trigger job detail by scheduler id, specify job id")
    public Boolean manualTrigger(@RequestBody JobDetailDTO jobDetailDTO) {
        return jobSchedulerService.triggerJob(jobDetailDTO);
    }

    @GetMapping("/job")
    @Operation(summary = "Get job class list, specify job id")
    public List<JobDetailDTO> getJobList() {
        return jobSchedulerService.getAllJobs();
    }
}
