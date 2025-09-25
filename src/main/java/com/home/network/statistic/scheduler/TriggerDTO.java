package com.home.network.statistic.scheduler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.home.network.statistic.common.model.TriggerInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.quartz.*;
import org.quartz.utils.Key;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TriggerDTO {
    private String schedulerId;
    private String schedulerName;
    private String triggerName;
    private String triggerGroup;
    private String triggerState;
    private String jobName;
    private String jobGroup;
    private String triggerDescription;
    private String triggerCalendarName;
    private Integer triggerPriority;
    private Boolean triggerFireAgain;
    private String triggerStartTime;
    private String triggerEndTime;
    private String triggerNextFireTime;
    private String triggerPreviousFireTime;
    private String triggerFinalFireTime;
    private Integer triggerMisfireInstruction;
    private String cronTriggerCronExpression;
    private String cronTriggerTimeZone;
    private Integer simpleTriggerRepeatCount;
    private Long simpleTriggerRepeatInterval;
    private Integer simpleTriggerTimesTriggered;
    private String dailyTimeIntervalTriggerRepeatIntervalUnit;
    private Integer dailyTimeIntervalTriggerRepeatCount;
    private Integer dailyTimeIntervalTriggerRepeatInterval;
    private String dailyTimeIntervalTriggerStartTimeOfDay;
    private String dailyTimeIntervalTriggerEndTimeOfDay;
    private String dailyTimeIntervalTriggerDaysOfWeek;
    private Integer dailyTimeIntervalTriggerTimesTriggered;
    private Boolean cronTrigger;
    private Boolean simpleTrigger;
    private Boolean dailyTimeIntervalTrigger;

    public static List<TriggerDTO> from(String schedulerName, Scheduler bean, List<TriggerInfo> list) {
        return list.stream().map(trigger -> new TriggerDTO(schedulerName, bean, trigger)).toList();
    }

    @SneakyThrows
    public TriggerDTO(String schedulerName, Scheduler bean, TriggerInfo ti) {
        var t = ti.trigger();
        var ts = ti.ts();

        this.schedulerId = schedulerName;
        this.schedulerName = bean.getSchedulerName();
        this.triggerName = Optional.ofNullable(t.getKey()).map(Key::getName).orElse(null);
        this.triggerGroup = Optional.ofNullable(t.getKey()).map(Key::getGroup).orElse(null);
        this.triggerState = ts.name();
        this.jobName = Optional.ofNullable(t.getJobKey()).map(Key::getName).orElse(null);
        this.jobGroup = Optional.ofNullable(t.getJobKey()).map(Key::getGroup).orElse(null);
        this.triggerDescription = t.getDescription();
        this.triggerCalendarName = t.getCalendarName();
        this.triggerPriority = t.getPriority();
        this.triggerFireAgain = t.mayFireAgain();
        this.triggerStartTime = Optional.ofNullable(t.getStartTime()).map(Date::toString).orElse(null);
        this.triggerEndTime = Optional.ofNullable(t.getEndTime()).map(Date::toString).orElse(null);
        this.triggerNextFireTime = Optional.ofNullable(t.getNextFireTime()).map(Date::toString).orElse(null);
        this.triggerPreviousFireTime = Optional.ofNullable(t.getPreviousFireTime()).map(Date::toString).orElse(null);
        this.triggerFinalFireTime = Optional.ofNullable(t.getFinalFireTime()).map(Date::toString).orElse(null);
        this.triggerMisfireInstruction = t.getMisfireInstruction();
        this.cronTrigger = false;
        this.simpleTrigger = false;
        this.dailyTimeIntervalTrigger = false;

        switch (t) {
            case CronTrigger c -> {
                this.cronTrigger = true;
                this.cronTriggerCronExpression = c.getCronExpression();
                this.cronTriggerTimeZone = c.getTimeZone().toString();
            }
            case SimpleTrigger s -> {
                this.simpleTrigger = true;
                this.simpleTriggerRepeatCount = s.getRepeatCount();
                this.simpleTriggerRepeatInterval = s.getRepeatInterval();
                this.simpleTriggerTimesTriggered = s.getTimesTriggered();
            }
            case DailyTimeIntervalTrigger d -> {
                this.dailyTimeIntervalTrigger = true;
                this.dailyTimeIntervalTriggerRepeatIntervalUnit = d.getDaysOfWeek().toString();
                this.dailyTimeIntervalTriggerRepeatCount = d.getRepeatCount();
                this.dailyTimeIntervalTriggerRepeatInterval = d.getRepeatInterval();
                this.dailyTimeIntervalTriggerStartTimeOfDay = d.getStartTimeOfDay().toString();
                this.dailyTimeIntervalTriggerEndTimeOfDay = d.getEndTimeOfDay().toString();
                this.dailyTimeIntervalTriggerDaysOfWeek = d.getDaysOfWeek().toString();
                this.dailyTimeIntervalTriggerTimesTriggered = d.getTimesTriggered();
            }
            default -> {
            }
        }
    }

    public TriggerKey obtainTriggerKey() {
        return TriggerKey.triggerKey(this.triggerName, this.triggerGroup);
    }

    public JobKey obtainJobKey() {
        return JobKey.jobKey(this.jobName, this.jobGroup);
    }

    public Trigger toTrigger() {
        TriggerBuilder<Trigger> tB = TriggerBuilder.newTrigger();

        tB.withIdentity(this.triggerName, this.triggerGroup);
        tB.forJob(this.jobName, this.jobGroup);

        if (this.triggerDescription != null)
            tB.withDescription(this.triggerDescription);

        if (this.triggerPriority != null)
            tB.withPriority(this.triggerPriority);

        if (this.cronTrigger) {
            tB.withSchedule(CronScheduleBuilder
                    .cronSchedule(this.cronTriggerCronExpression)
                    .inTimeZone(TimeZone.getTimeZone(this.cronTriggerTimeZone)));
        }

        return tB.build();
    }
}
