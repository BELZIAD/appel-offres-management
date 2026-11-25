package com.aomanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskDeadlineScheduler {
    private final TaskService taskService;

    @Scheduled(cron = "0 10 8 * * *")
    public void notifyTomorrowDeadlines() {
        taskService.notifyTomorrowDeadlines();
    }
}
