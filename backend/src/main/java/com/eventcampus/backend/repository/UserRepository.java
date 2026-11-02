package com.eventcampus.backend.repository;

import com.eventcampus.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    long countByRole_Name(String name);

    long countByRole_NameAndStatus(String name, String status);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update User u set u.role = :role where u.idUser = :id")
    int changeRole(@org.springframework.data.repository.query.Param("id") Integer id,
            @org.springframework.data.repository.query.Param("role") com.eventcampus.backend.entity.Role role);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update User u set u.status = :status where u.idUser = :id")
    int changeStatus(@org.springframework.data.repository.query.Param("id") Integer id,
            @org.springframework.data.repository.query.Param("status") String status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "role")
    java.util.List<User> findAllBy(org.springframework.data.domain.Sort sort);
}
