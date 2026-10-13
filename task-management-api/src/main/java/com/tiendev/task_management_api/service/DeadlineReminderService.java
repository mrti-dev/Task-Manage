package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.enums.NotificationType;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.repository.NotificationRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineReminderService {

    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Value("${app.scheduling.timezone:Asia/Ho_Chi_Minh}")
    private String configuredTimezone;

    @Scheduled(cron = "${app.scheduling.deadline-reminder-cron:0 0 8 * * *}")
    @Transactional
    public void scheduleDeadlineReminders() {
        checkAndSendDeadlineReminders();
    }

    @Transactional
    public int checkAndSendDeadlineReminders() {
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(configuredTimezone);
        } catch (Exception e) {
            zoneId = ZoneId.systemDefault();
        }

        LocalDate today = LocalDate.now(zoneId);
        LocalDateTime startOfToday = today.atStartOfDay();

        List<Task> activeTasks = taskRepository.findActiveTasksForDeadlineReminder(today.plusDays(1), TaskStatus.DONE);

        int remindersSent = 0;

        for (Task task : activeTasks) {
            User assignee = task.getAssignee();
            LocalDate deadline = task.getDeadline();

            if (deadline.isBefore(today)) {
                // Overdue
                boolean alreadySentToday = notificationRepository.existsByTaskIdAndReceiverIdAndTypeAndCreatedAtAfter(
                        task.getId(), assignee.getId(), NotificationType.TASK_OVERDUE, startOfToday);

                if (!alreadySentToday) {
                    notificationService.createNotification(
                            "Nhiệm vụ quá hạn: " + task.getTitle(),
                            "Nhiệm vụ \"" + task.getTitle() + "\" đã quá hạn vào ngày " + deadline + ".",
                            NotificationType.TASK_OVERDUE,
                            assignee.getId(),
                            null,
                            task.getId(),
                            task.getProject().getId(),
                            task.getProject().getWorkspace().getId()
                    );
                    remindersSent++;
                }
            } else if (!deadline.isAfter(today.plusDays(1))) {
                // Due today or tomorrow (upcoming)
                boolean alreadySentToday = notificationRepository.existsByTaskIdAndReceiverIdAndTypeAndCreatedAtAfter(
                        task.getId(), assignee.getId(), NotificationType.TASK_DUE_SOON, startOfToday);

                if (!alreadySentToday) {
                    notificationService.createNotification(
                            "Nhiệm vụ sắp đến hạn: " + task.getTitle(),
                            "Nhiệm vụ \"" + task.getTitle() + "\" sẽ đến hạn vào ngày " + deadline + ".",
                            NotificationType.TASK_DUE_SOON,
                            assignee.getId(),
                            null,
                            task.getId(),
                            task.getProject().getId(),
                            task.getProject().getWorkspace().getId()
                    );
                    remindersSent++;
                }
            }
        }

        return remindersSent;
    }
}
