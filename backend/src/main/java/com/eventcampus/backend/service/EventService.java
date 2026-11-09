package com.eventcampus.backend.service;

import com.eventcampus.backend.dto.EventRequest;
import com.eventcampus.backend.entity.Event;
import com.eventcampus.backend.repository.EventRepository;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Service 
public class EventService {
    private final EventRepository eventRepository;
    
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
    public Event getEventById(Integer idEvent) {
        return eventRepository.findById(idEvent)
             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));  
    }
    public Event createEvent(EventRequest request) {
        Event event = new Event();
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setImage(request.getImage());
        event.setEventDate(request.getEventDate());
        event.setEventTime(request.getEventTime());
        event.setLocation(request.getLocation().trim());
        event.setCapacity(request.getCapacity());
        event.setIdCreator(request.getIdCreator());
        event.setIdCategory(request.getIdCategory());
        return eventRepository.save(event);
    }
    public void deleteEvent(Integer idEvent) {
        Event event = getEventById(idEvent);
         eventRepository.delete(event);
    }
    public Event updateEvent(Integer idEvent, Event newEvent){
        Event event = getEventById(idEvent);
        event.setTitle(newEvent.getTitle());
        event.setDescription(newEvent.getDescription());
        event.setEventDate(newEvent.getEventDate());
        event.setEventTime(newEvent.getEventTime());
        event.setLocation(newEvent.getLocation());
        event.setCapacity(newEvent.getCapacity());
        event.setStatus(newEvent.getStatus());
        event.setIdCategory(newEvent.getIdCategory());
        return eventRepository.save(event);
    }
}
