package com.eventcampus.backend.service;

import com.eventcampus.backend.dto.EventResponse;
import com.eventcampus.backend.repository.EventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class EventService {
    // Publication status agreed for the student catalogue. All other states stay private.
    public static final String PUBLISHED_STATUS = "PUBLIE";
    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findByStatus(PUBLISHED_STATUS, Sort.by("eventDate", "eventTime", "idEvent"))
                .stream()
                .filter(event -> PUBLISHED_STATUS.equals(event.getStatus()))
                .map(EventResponse::from).toList();
    }
}
