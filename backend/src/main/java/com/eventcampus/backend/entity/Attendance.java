package com.eventcampus.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendances")
public class Attendance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_attendance") private Integer idAttendance;
    @Column(nullable = false) private boolean present;
    @Column(name = "scanned_at") private LocalDateTime scannedAt;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_registration", nullable = false, unique = true)
    private Registration registration;

    public boolean isPresent() { return present; }
    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setPresent(boolean value) { present = value; }
    public void setScannedAt(LocalDateTime value) { scannedAt = value; }
    public void setRegistration(Registration value) { registration = value; }
}
