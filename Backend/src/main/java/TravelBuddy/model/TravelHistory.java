package TravelBuddy.model;

import java.time.Month;

public class TravelHistory {
    private Long id;
    private Long userId;
    private String destination;
    private Integer tripDuration;
    private Month travelMonth;
    private String itinerary;
    private String airline;
    private String flightNumber;
    private String hotel;
    private String flightReview;
    private String hotelReview;
    private String overallTripReview;
    private Integer rating;
    private String photos;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getTripDuration() {
        return tripDuration;
    }

    public void setTripDuration(Integer tripDuration) {
        this.tripDuration = tripDuration;
    }

    public Month getTravelMonth() {
        return travelMonth;
    }

    public void setTravelMonth(Month travelMonth) {
        this.travelMonth = travelMonth;
    }

    public String getItinerary() {
        return itinerary;
    }

    public void setItinerary(String itinerary) {
        this.itinerary = itinerary;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getHotel() {
        return hotel;
    }

    public void setHotel(String hotel) {
        this.hotel = hotel;
    }

    public String getFlightReview() {
        return flightReview;
    }

    public void setFlightReview(String flightReview) {
        this.flightReview = flightReview;
    }

    public String getHotelReview() {
        return hotelReview;
    }

    public void setHotelReview(String hotelReview) {
        this.hotelReview = hotelReview;
    }

    public String getOverallTripReview() {
        return overallTripReview;
    }

    public void setOverallTripReview(String overallTripReview) {
        this.overallTripReview = overallTripReview;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getPhotos() {
        return photos;
    }

    public void setPhotos(String photos) {
        this.photos = photos;
    }

    // Constructor
    public TravelHistory() {}

    // You can add a constructor with parameters if needed

    // toString method for debugging
    @Override
    public String toString() {
        return "TravelHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", destination='" + destination + '\'' +
                ", tripDuration=" + tripDuration +
                ", travelMonth=" + travelMonth +
                ", airline='" + airline + '\'' +
                ", hotel='" + hotel + '\'' +
                ", rating=" + rating +
                '}';
    }
}
