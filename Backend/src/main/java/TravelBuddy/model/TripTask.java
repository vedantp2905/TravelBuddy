package TravelBuddy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_tasks")
public class TripTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private boolean dayReminderSent;

    @Column(nullable = false)
    private boolean hourReminderSent;

    @Column(nullable = false)
    private boolean overdueReminderSent;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructors
    public TripTask() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isDayReminderSent() {
        return dayReminderSent;
    }

    public void setDayReminderSent(boolean dayReminderSent) {
        this.dayReminderSent = dayReminderSent;
    }

    public boolean isHourReminderSent() {
        return hourReminderSent;
    }

    public void setHourReminderSent(boolean hourReminderSent) {
        this.hourReminderSent = hourReminderSent;
    }

    public boolean isOverdueReminderSent() {
        return overdueReminderSent;
    }

    public void setOverdueReminderSent(boolean overdueReminderSent) {
        this.overdueReminderSent = overdueReminderSent;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
