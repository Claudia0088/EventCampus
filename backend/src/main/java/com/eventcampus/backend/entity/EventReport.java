package com.eventcampus.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_reports", uniqueConstraints = @UniqueConstraint(columnNames = {"id_reporter", "id_event"}))
public class EventReport {
    public enum Reason { INAPPROPRIATE, MISLEADING, SPAM, SAFETY, OTHER }
    public enum Status { PENDING, RESOLVED, REJECTED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_report") private Integer idReport;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_event", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Event event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reporter", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User reporter;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Reason reason;
    @Column(nullable = false, length = 2000) private String explanation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_moderator", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User moderator;
    @Column(name = "processed_at") private LocalDateTime processedAt;

    public Integer getIdReport() { return idReport; }
    public Event getEvent() { return event; }
    public void setEvent(Event value) { event = value; }
    public User getReporter() { return reporter; }
    public void setReporter(User value) { reporter = value; }
    public Reason getReason() { return reason; }
    public void setReason(Reason value) { reason = value; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String value) { explanation = value; }
    public Status getStatus() { return status; }
    public void setStatus(Status value) { status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public User getModerator() { return moderator; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
