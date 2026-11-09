package com.eventcampus.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class EventRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @Size(max = 500)
    private String image;

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private LocalTime eventTime;

    @NotBlank
    @Size(max = 255)
    private String location;

    @NotNull
    @Positive
    private Integer capacity;

    @NotNull
    @Positive
    private Integer idCreator;

    @NotNull
    @Positive
    private Integer idCategory;

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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getIdCreator() {
        return idCreator;
    }

    public void setIdCreator(Integer idCreator) {
        this.idCreator = idCreator;
    }

    public Integer getIdCategory() {
        return idCategory;
    }

    public void setIdCategory(Integer idCategory) {
        this.idCategory = idCategory;
    }
}
