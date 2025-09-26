package com.home.network.statistic.common.util;

import com.home.network.statistic.common.model.TriggerInfo;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SchedulerUtil(Scheduler scheduler) {
    // get trigger of current job
    public Optional<Trigger> checkJobHasTrigger(JobKey currentJk) throws SchedulerException {
        for (var jobGroup : scheduler.getTriggerGroupNames())
            for (var triggerKey : scheduler.getTriggerKeys(GroupMatcher.groupEquals(jobGroup)))
                if (scheduler.getTrigger(triggerKey).getJobKey().equals(currentJk)) {
                    return Optional.of(scheduler.getTrigger(triggerKey));
                }

        return Optional.empty();
    }

    public List<JobDetail> getJobDetails() throws SchedulerException {
        var details = new ArrayList<JobDetail>();

        for (var jobGroup : scheduler.getJobGroupNames()) {
            for (var jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)))
                details.add(scheduler.getJobDetail(jobKey));
        }

        return details;
    }

    public List<TriggerInfo> getTriggers() throws SchedulerException {
        var details = new ArrayList<TriggerInfo>();

        for (var jobGroup : scheduler.getTriggerGroupNames()) {
            for (var triggerKey : scheduler.getTriggerKeys(GroupMatcher.groupEquals(jobGroup)))
                details.add(new TriggerInfo(scheduler.getTrigger(triggerKey), scheduler.getTriggerState(triggerKey)));
        }

        return details;
    }
}
