package TravelBuddy.service;

import TravelBuddy.model.TripTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Service
public class ReminderService {

    @Autowired
    private TripTaskService tripTaskService;

    @Autowired
    private EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final long TOKEN_VALIDITY_HOURS = 24;
    private Map<String, TokenInfo> completionTokens = new HashMap<>();
    
    private static class TokenInfo {
        final Long taskId;
        final LocalDateTime expiryTime;
        
        TokenInfo(Long taskId) {
            this.taskId = taskId;
            this.expiryTime = LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS);
        }
        
        boolean isValid() {
            return LocalDateTime.now().isBefore(expiryTime);
        }
    }
    
    private String generateCompletionToken(Long taskId) {
        String token = UUID.randomUUID().toString();
        completionTokens.put(token, new TokenInfo(taskId));
        return token;
    }
    
    public boolean verifyCompletionToken(Long taskId, String token) {
        TokenInfo info = completionTokens.get(token);
        if (info == null || !info.isValid() || !info.taskId.equals(taskId)) {
            return false;
        }
        completionTokens.remove(token); // One-time use
        return true;
    }

    @Scheduled(fixedRate = 900000) // Run every 15 minutes
    public void checkAndSendReminders() {
        List<TripTask> incompleteTasks = tripTaskService.getIncompleteTasks();
        LocalDateTime now = LocalDateTime.now();

        for (TripTask task : incompleteTasks) {
            // Skip if task is completed
            if (task.isCompleted()) {
                continue;
            }

            Duration timeUntilDue = Duration.between(now, task.getDueDate());
            
            // Check for 24-hour reminder (only if not already sent)
            if (!task.isDayReminderSent() && 
                timeUntilDue.toHours() <= 24 && 
                timeUntilDue.toHours() > 4) {  // Add upper bound check
                sendReminderEmail(task, "24 hours", ReminderType.DAY);
                task.setDayReminderSent(true);
                tripTaskService.updateTask(task);
            }
            
            // Check for 4-hour reminder (only if not already sent)
            if (!task.isHourReminderSent() && 
                timeUntilDue.toHours() <= 4 && 
                timeUntilDue.toMinutes() > 0) {
                sendReminderEmail(task, "4 hours", ReminderType.HOUR);
                task.setHourReminderSent(true);
                tripTaskService.updateTask(task);
            }

            // Check for overdue tasks (only if not already sent)
            if (!task.isOverdueReminderSent() && 
                timeUntilDue.toMinutes() < 0) {
                sendOverdueReminder(task);
                task.setOverdueReminderSent(true);
                tripTaskService.updateTask(task);
            }
        }
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    private void cleanupExpiredTokens() {
        completionTokens.entrySet().removeIf(entry -> !entry.getValue().isValid());
    }

    private enum ReminderType {
        DAY,
        HOUR,
        OVERDUE
    }

    private void sendReminderEmail(TripTask task, String timeFrame, ReminderType type) {
        String subject = "Trip Task Reminder: " + task.getTitle();
        String content = buildEmailContent(task, timeFrame, type);
        emailService.sendSimpleEmail(task.getUser().getEmail(), subject, content);
    }

    private void sendOverdueReminder(TripTask task) {
        String subject = "OVERDUE Trip Task: " + task.getTitle();
        String content = buildEmailContent(task, "OVERDUE", ReminderType.OVERDUE);
        emailService.sendSimpleEmail(task.getUser().getEmail(), subject, content);
    }

    private String buildEmailContent(TripTask task, String timeFrame, ReminderType type) {
        StringBuilder contentBuilder = new StringBuilder();
        
        switch (type) {
            case DAY:
            case HOUR:
                contentBuilder.append(String.format(
                    "Your trip task '%s' is due in %s.\n\n",
                    task.getTitle(),
                    timeFrame
                ));
                break;
            case OVERDUE:
                contentBuilder.append(String.format(
                    "Your trip task '%s' is OVERDUE!\n\n",
                    task.getTitle()
                ));
                break;
        }

        contentBuilder.append("Task Details:\n");
        contentBuilder.append("-------------\n");
        contentBuilder.append(String.format("Description: %s\n", task.getDescription()));
        contentBuilder.append(String.format("Due Date: %s\n", task.getDueDate()));
        
        if (type == ReminderType.OVERDUE) {
            contentBuilder.append("\nPlease complete this task as soon as possible or update its due date if needed.");
        } else {
            contentBuilder.append("\nPlease make sure to complete this task before the deadline.");
        }

        contentBuilder.append("\n\nYou can mark this task as completed in your TravelBuddy app.");
        
        // Generate completion token and add completion link
        String token = generateCompletionToken(task.getId());
        String completionLink = String.format(
            "%s/api/tasks/complete/%d/%s",
            baseUrl,
            task.getId(),
            token
        );
        
        contentBuilder.append("\n\nClick here to mark this task as completed:\n");
        contentBuilder.append(completionLink);
        contentBuilder.append("\n\nThis completion link will expire in 24 hours.");
        
        return contentBuilder.toString();
    }
} 