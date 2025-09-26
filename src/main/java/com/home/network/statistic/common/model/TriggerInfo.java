package com.home.network.statistic.common.model;

import org.quartz.Trigger;

public record TriggerInfo(Trigger trigger, Trigger.TriggerState ts) {
}
