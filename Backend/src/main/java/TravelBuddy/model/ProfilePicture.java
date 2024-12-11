package TravelBuddy.model;

import jakarta.persistence.*;

@Entity
@Table(name="profile_pictures")
public class ProfilePicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name="file_path")
    private String filePath;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public ProfilePicture() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
