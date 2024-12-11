package TravelBuddy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "polls")
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String options;
    
    private LocalDateTime createdAt;
    
    @Column(name = "creator_id")
    private Long creatorId;
    
    private boolean active = true;

    @Transient // This field won't be persisted to database
    private Map<String, Integer> votes = new HashMap<>();

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @JsonIgnore
    private List<Vote> votesList = new ArrayList<>();

    @Column
    private LocalDateTime expiresAt;

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

    public List<String> getOptions() {
        return options != null ? Arrays.asList(options.split(",")) : new ArrayList<>();
    }

    public void setOptions(List<String> optionsList) {
        this.options = optionsList != null ? String.join(",", optionsList) : "";
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Map<String, Integer> getVotes() {
        return votes;
    }

    public void setVotes(Map<String, Integer> votes) {
        this.votes = votes;
    }

    public List<Vote> getVotesList() {
        return votesList;
    }

    public void setVotesList(List<Vote> votes) {
        this.votesList = votes;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
} 