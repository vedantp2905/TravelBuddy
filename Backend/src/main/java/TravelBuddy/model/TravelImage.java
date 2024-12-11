package TravelBuddy.model;

import jakarta.persistence.*;

@Entity
@Table(name = "images")
public class TravelImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "travel_post_id")
    private TravelPost post;

    @Column(name = "file_path")
    private String filePath;

    public TravelImage() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public TravelPost getPost() {
        return post;
    }

    public void setPost(TravelPost post) {
        this.post = post;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
