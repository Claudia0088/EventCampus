package com.eventcampus.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "qr_codes")
public class QrCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_qr_code") private Integer idQrCode;
    @Column(nullable = false, unique = true, length = 191) private String code;
    @Column(nullable = false) private boolean used;
    @Column(name = "used_at") private LocalDateTime usedAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_registration", nullable = false, unique = true) private Registration registration;
    public Integer getIdQrCode() { return idQrCode; }
    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public boolean isUsed() { return used; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public Registration getRegistration() { return registration; }
    public void setUsed(boolean value) { used = value; }
    public void setUsedAt(LocalDateTime value) { usedAt = value; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public void setRegistration(Registration value) { registration = value; }
}
