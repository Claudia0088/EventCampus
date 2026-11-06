package com.eventcampus.backend.controller;

import com.eventcampus.backend.service.AdminCategoryService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    private final AdminCategoryService categories;
    public AdminCategoryController(AdminCategoryService categories) { this.categories = categories; }
    @PostMapping public ResponseEntity<?> create(@RequestBody AdminCategoryService.Input input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Catégorie créée.", "category", categories.create(input)));
    }
    @PatchMapping("/{id}") public Map<String,Object> update(@PathVariable Integer id, @RequestBody AdminCategoryService.Input input) {
        return Map.of("message", "Catégorie modifiée.", "category", categories.update(id, input));
    }
    @GetMapping("/{id}/deletion-check") public AdminCategoryService.DeletionCheck check(@PathVariable Integer id) { return categories.deletionCheck(id); }
    @DeleteMapping("/{id}") public Map<String,String> delete(@PathVariable Integer id) {
        categories.delete(id); return Map.of("message", "Catégorie supprimée.");
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> error(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) public ResponseEntity<?> duplicate(Exception error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Ce nom existe déjà ou la catégorie ne peut pas être enregistrée."));
    }
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid(Exception error) { return ResponseEntity.badRequest().body(Map.of("message", "Identifiant ou données invalides.")); }
}
