package TravelBuddy.model;

import jakarta.persistence.*;

@Entity
@Table(name = "conversations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
})
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user1_id")
    private Long user1Id;
    @Column(name = "user2_id")
    private Long user2Id;
    @Column(name = "deleted_for_user1")
    private Boolean deletedForUser1;
    @Column(name = "deleted_for_user2")
    private Boolean deletedForUser2;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(Long user1Id) {
        this.user1Id = user1Id;
    }

    public Long getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(Long user2Id) {
        this.user2Id = user2Id;
    }

    public Boolean getDeletedForUser1() {
        return deletedForUser1;
    }

    public void setDeletedForUser1(Boolean deletedForUser1) {
        this.deletedForUser1 = deletedForUser1;
    }

    public Boolean getDeletedForUser2() {
        return deletedForUser2;
    }

    public void setDeletedForUser2(Boolean deletedForUser2) {
        this.deletedForUser2 = deletedForUser2;
    }
}
