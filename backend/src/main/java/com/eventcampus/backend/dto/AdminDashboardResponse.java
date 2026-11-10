package com.eventcampus.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(Stats stats, List<Account> users,
        List<CategoryItem> categories, List<EventItem> events) {
    public record Stats(long totalUsers, long userAccounts, long moderatorAccounts,
            long adminAccounts, long totalEvents, long publishedEvents, long pendingEvents) {}
    public record Account(Integer idUser, String firstName, String lastName,
            String email, String role, String status) {}
    public record CategoryItem(Integer idCategory, String name, String description) {}
    public record EventItem(Integer idEvent, String title, String creatorName,
            String categoryName, LocalDate eventDate, String status) {}
}
