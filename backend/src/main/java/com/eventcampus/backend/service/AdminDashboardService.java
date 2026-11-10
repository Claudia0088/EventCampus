package com.eventcampus.backend.service;

import com.eventcampus.backend.dto.AdminDashboardResponse;
import com.eventcampus.backend.dto.AdminDashboardResponse.*;
import com.eventcampus.backend.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final EventRepository events;

    public AdminDashboardService(UserRepository users, CategoryRepository categories, EventRepository events) {
        this.users = users; this.categories = categories; this.events = events;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        var counts = events.getEventStats();
        var stats = new Stats(users.count(), users.countByRole_Name("USER"),
                users.countByRole_Name("MODERATOR"), users.countByRole_Name("ADMIN"),
                counts.getTotal(), counts.getPublished(), counts.getPending());
        var accounts = users.findAllBy(Sort.by("idUser")).stream().map(user ->
                new Account(user.getIdUser(), user.getFirstName(), user.getLastName(),
                        user.getEmail(), user.getRole() == null ? null : user.getRole().getName(), user.getStatus())).toList();
        var categoryItems = categories.findAll(Sort.by("name")).stream().map(category ->
                new CategoryItem(category.getIdCategory(), category.getName(), category.getDescription())).toList();
        var eventItems = events.findAllBy(Sort.by(Sort.Direction.DESC, "createdAt", "idEvent"))
                .stream().map(event -> new EventItem(event.getIdEvent(), event.getTitle(),
                        event.getCreator() == null ? "Créateur indisponible" :
                            event.getCreator().getFirstName() + " " + event.getCreator().getLastName(),
                        event.getCategory() == null ? "Catégorie indisponible" : event.getCategory().getName(),
                        event.getEventDate(), event.getStatus())).toList();
        return new AdminDashboardResponse(stats, accounts, categoryItems, eventItems);
    }
}
