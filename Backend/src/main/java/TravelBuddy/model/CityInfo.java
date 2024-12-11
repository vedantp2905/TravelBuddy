package TravelBuddy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "city_info")
public class CityInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String cityName;

    @Column(columnDefinition = "JSON")
    private String attractions;

    @Column(columnDefinition = "JSON")
    private String restaurants;

    @Column(columnDefinition = "JSON")
    private String historicalSites;

    private LocalDateTime lastUpdated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getAttractions() {
        return attractions;
    }

    public void setAttractions(String attractions) {
        this.attractions = attractions;
    }

    public String getRestaurants() {
        return restaurants;
    }

    public void setRestaurants(String restaurants) {
        this.restaurants = restaurants;
    }

    public String getHistoricalSites() {
        return historicalSites;
    }

    public void setHistoricalSites(String historicalSites) {
        this.historicalSites = historicalSites;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
} 