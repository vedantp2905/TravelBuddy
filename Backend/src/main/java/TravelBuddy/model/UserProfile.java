package TravelBuddy.model;

import java.util.List;

public class UserProfile {
    private Long id;
    private String aboutMe;
    private String preferredLanguage;
    private String currencyPreference;
    private int travelBudget;
    private String travelStyle;
    private String travelExperienceLevel;
    private Integer maxTripDuration;
    private List<String> preferredDestinations;
    private List<String> interests;
    private List<String> preferredAirlines;
    private String preferredAccommodationType;
    private List<String> dietaryRestrictions;
    private String passportCountry;
    private List<String> frequentFlyerPrograms;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAboutMe() {
        return aboutMe;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getCurrencyPreference() {
        return currencyPreference;
    }

    public void setCurrencyPreference(String currencyPreference) {
        this.currencyPreference = currencyPreference;
    }

    public int getTravelBudget() {
        return travelBudget;
    }

    public void setTravelBudget(int travelBudget) {
        this.travelBudget = travelBudget;
    }

    public String getTravelStyle() {
        return travelStyle;
    }

    public void setTravelStyle(String travelStyle) {
        this.travelStyle = travelStyle;
    }

    public String getTravelExperienceLevel() {
        return travelExperienceLevel;
    }

    public void setTravelExperienceLevel(String travelExperienceLevel) {
        this.travelExperienceLevel = travelExperienceLevel;
    }

    public Integer getMaxTripDuration() {
        return maxTripDuration;
    }

    public void setMaxTripDuration(Integer maxTripDuration) {
        this.maxTripDuration = maxTripDuration;
    }

    public List<String> getPreferredDestinations() {
        return preferredDestinations;
    }

    public void setPreferredDestinations(List<String> preferredDestinations) {
        this.preferredDestinations = preferredDestinations;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public List<String> getPreferredAirlines() {
        return preferredAirlines;
    }

    public void setPreferredAirlines(List<String> preferredAirlines) {
        this.preferredAirlines = preferredAirlines;
    }

    public String getPreferredAccommodationType() {
        return preferredAccommodationType;
    }

    public void setPreferredAccommodationType(String preferredAccommodationType) {
        this.preferredAccommodationType = preferredAccommodationType;
    }

    public List<String> getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public void setDietaryRestrictions(List<String> dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
    }

    public String getPassportCountry() {
        return passportCountry;
    }

    public void setPassportCountry(String passportCountry) {
        this.passportCountry = passportCountry;
    }

    public List<String> getFrequentFlyerPrograms() {
        return frequentFlyerPrograms;
    }

    public void setFrequentFlyerPrograms(List<String> frequentFlyerPrograms) {
        this.frequentFlyerPrograms = frequentFlyerPrograms;
    }
}
