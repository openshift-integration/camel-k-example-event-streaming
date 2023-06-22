package com.redhat.integration.pollution;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class PollutionData {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DateInfo {
        private Date utc;

        public Date getUtc() {
            return utc;
        }

        public void setUtc(Date utc) {
            this.utc = utc;
        }

    }

    public static class Coordinates {
        private double longitude;
        private double latitude;

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }
    }

    private String locationId;
    private String location;
    private String parameter;
    private DateInfo date;
    private double value;
    private String unit;
    private Coordinates coordinates;
    private String country;
    private String city;
    private Boolean isMobile;
    private Boolean isAnalysis;
    private String entity;
    private String sensorType;

    public String getLocationId() {
        return locationId;
    }
    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public DateInfo getDate() {
        return date;
    }

    public void setDate(DateInfo date) {
        this.date = date;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Boolean getIsMobile() {
        return isMobile;
    }

    public void setIsMobile(Boolean isMobile) {
        this.isMobile = isMobile;
    }

    public Boolean getIsAnalysis() {
        return isAnalysis;
    }

    public void setIsAnalysis(Boolean isAnalysis) {
        this.isAnalysis = isAnalysis;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }
}
