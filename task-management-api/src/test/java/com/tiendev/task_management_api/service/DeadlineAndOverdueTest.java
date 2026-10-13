package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.enums.NotificationType;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.repository.NotificationRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineAndOverdueTest {

    @Mock private TaskRepository taskRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private DeadlineReminderService deadlineReminderService;

    @Test
    void sendsRemindersForOverdueAndUpcomingTasks() {
        Workspace ws = new Workspace();
        ws.setId(1L);
        Project prj = new Project();
        prj.setId(10L);
        prj.setWorkspace(ws);

        User assignee = new User();
        assignee.setId(100L);

        // Task 1: Overdue
        Task overdueTask = new Task();
        overdueTask.setId(1L);
        overdueTask.setTitle("Overdue Task");
        overdueTask.setActive(true);
        overdueTask.setStatus(TaskStatus.IN_PROGRESS);
        overdueTask.setDeadline(LocalDate.now().minusDays(2));
        overdueTask.setAssignee(assignee);
        overdueTask.setProject(prj);

        // Task 2: Due tomorrow
        Task dueSoonTask = new Task();
        dueSoonTask.setId(2L);
        dueSoonTask.setTitle("Due Soon Task");
        dueSoonTask.setActive(true);
        dueSoonTask.setStatus(TaskStatus.TODO);
        dueSoonTask.setDeadline(LocalDate.now().plusDays(1));
        dueSoonTask.setAssignee(assignee);
        dueSoonTask.setProject(prj);

        when(taskRepository.findActiveTasksForDeadlineReminder(any(LocalDate.class), eq(TaskStatus.DONE)))
                .thenReturn(List.of(overdueTask, dueSoonTask));
        when(notificationRepository.existsByTaskIdAndReceiverIdAndTypeAndCreatedAtAfter(any(), any(), any(), any()))
                .thenReturn(false);

        int sent = deadlineReminderService.checkAndSendDeadlineReminders();

        assertEquals(2, sent);
        verify(notificationService).createNotification(
                any(), any(), eq(NotificationType.TASK_OVERDUE), eq(100L), any(), eq(1L), eq(10L), eq(1L));
        verify(notificationService).createNotification(
                any(), any(), eq(NotificationType.TASK_DUE_SOON), eq(100L), any(), eq(2L), eq(10L), eq(1L));
    }

    @Test
    void idempotentReminderDoesNotDuplicateNotificationIfAlreadySentToday() {
        Workspace ws = new Workspace();
        ws.setId(1L);
        Project prj = new Project();
        prj.setId(10L);
        prj.setWorkspace(ws);

        User assignee = new User();
        assignee.setId(100L);

        Task overdueTask = new Task();
        overdueTask.setId(1L);
        overdueTask.setTitle("Overdue Task");
        overdueTask.setActive(true);
        overdueTask.setStatus(TaskStatus.IN_PROGRESS);
        overdueTask.setDeadline(LocalDate.now().minusDays(2));
        overdueTask.setAssignee(assignee);
        overdueTask.setProject(prj);

        when(taskRepository.findActiveTasksForDeadlineReminder(any(LocalDate.class), eq(TaskStatus.DONE)))
                .thenReturn(List.of(overdueTask));
        // Simulate that notification was already sent earlier today
        when(notificationRepository.existsByTaskIdAndReceiverIdAndTypeAndCreatedAtAfter(
                eq(1L), eq(100L), eq(NotificationType.TASK_OVERDUE), any(LocalDateTime.class)))
                .thenReturn(true);

        int sent = deadlineReminderService.checkAndSendDeadlineReminders();

        assertEquals(0, sent);
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
