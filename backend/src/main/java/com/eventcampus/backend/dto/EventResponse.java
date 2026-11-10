package com.eventcampus.backend.dto;

import com.eventcampus.backend.entity.Event;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

// Expose only event data and relation identifiers, never the User entity.
public record EventResponse(
        Integer idEvent, String title, String description, String image,
        LocalDate eventDate, LocalTime eventTime, String location,
        Integer capacity, String status, LocalDateTime createdAt,
        LocalDateTime updatedAt, Integer idCreator, Integer idCategory, String categoryName) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getIdEvent(), event.getTitle(), event.getDescription(),
                event.getImage(), event.getEventDate(), event.getEventTime(),
                event.getLocation(), event.getCapacity(), event.getStatus(),
                event.getCreatedAt(), event.getUpdatedAt(),
                event.getCreator().getIdUser(), event.getCategory().getIdCategory(),
                event.getCategory().getName());
    }
}
