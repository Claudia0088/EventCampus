package com.eventcampus.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations", uniqueConstraints = @UniqueConstraint(columnNames = {"id_user", "id_event"}))
public class Registration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_registration")
    private Integer idRegistration;
    @Column(name = "registered_at", nullable = false) private LocalDateTime registeredAt;
    @Column(nullable = false, length = 30) private String status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_user", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_event", nullable = false) private Event event;
    public Integer getIdRegistration() { return idRegistration; }
    public void setIdRegistration(Integer value) { idRegistration = value; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime value) { registeredAt = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public User getUser() { return user; }
    public void setUser(User value) { user = value; }
    public Event getEvent() { return event; }
    public void setEvent(Event value) { event = value; }
}
