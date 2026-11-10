package com.eventcampus.backend.service;

import com.eventcampus.backend.entity.Category;
import com.eventcampus.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
public class AdminCategoryService {
    private final CategoryRepository categories;
    private final EventRepository events;
    private final CategoryWriteLock lock;
    public AdminCategoryService(CategoryRepository categories, EventRepository events, CategoryWriteLock lock) {
        this.categories = categories; this.events = events; this.lock = lock;
    }
    public record Input(String name, String description) {}
    public record Item(Integer idCategory, String name, String description) {
        static Item from(Category category) { return new Item(category.getIdCategory(), category.getName(), category.getDescription()); }
    }
    public record DeletionCheck(Integer idCategory, boolean deletable, long references, String message) {}
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private Category existing(Integer id) {
        if (id == null || id <= 0) throw bad("Identifiant de catégorie invalide.");
        return categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catégorie introuvable."));
    }
    private String normalize(String value) { return value == null ? "" : value.replaceAll("(?U)\\s+", " ").strip(); }
    private void fill(Category category, Input input) {
        if (input == null) throw bad("Données requises.");
        String name = normalize(input.name());
        String description = input.description() == null ? null : input.description().strip();
        if (name.isBlank() || name.length() > 100) throw bad("Le nom est obligatoire et limité à 100 caractères.");
        if (description != null && description.length() > 255) throw bad("La description est limitée à 255 caractères.");
        String key = name.toLowerCase(Locale.ROOT);
        boolean duplicate = categories.findAll().stream().anyMatch(other ->
                !java.util.Objects.equals(other.getIdCategory(), category.getIdCategory())
                && normalize(other.getName()).toLowerCase(Locale.ROOT).equals(key));
        if (duplicate) throw new ResponseStatusException(HttpStatus.CONFLICT, "Une catégorie portant ce nom existe déjà.");
        category.setName(name); category.setDescription(description == null || description.isEmpty() ? null : description);
    }
    public Item create(Input input) {
        return lock.execute(() -> { var category = new Category(); fill(category, input); return Item.from(categories.saveAndFlush(category)); });
    }
    public Item update(Integer id, Input input) {
        return lock.execute(() -> { var category = existing(id); fill(category, input); return Item.from(categories.saveAndFlush(category)); });
    }
    @Transactional(readOnly = true)
    public DeletionCheck deletionCheck(Integer id) {
        existing(id);
        long count = events.countByCategory_IdCategory(id);
        return new DeletionCheck(id, count == 0, count, count == 0 ? "Cette catégorie peut être supprimée."
                : "Suppression impossible : cette catégorie est utilisée par " + count + " événement(s), quel que soit leur statut.");
    }
    public void delete(Integer id) {
        lock.execute(() -> {
            var category = existing(id);
            if (events.countByCategory_IdCategory(id) != 0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Suppression impossible : un événement utilise cette catégorie. Aucun événement n'a été supprimé.");
            categories.delete(category); categories.flush(); return null;
        });
    }
}
