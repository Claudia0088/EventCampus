package com.eventcampus.backend.dto;

import com.eventcampus.backend.entity.Event;

public record ModeratorEventResponse(EventResponse event, Creator creator) {
    public record Creator(Integer idUser, String firstName, String lastName) {}

    public static ModeratorEventResponse from(Event event) {
        var user = event.getCreator();
        return new ModeratorEventResponse(EventResponse.from(event),
                new Creator(user.getIdUser(), user.getFirstName(), user.getLastName()));
    }
}
