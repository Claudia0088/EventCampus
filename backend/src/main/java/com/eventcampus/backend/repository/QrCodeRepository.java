package com.eventcampus.backend.repository;
import com.eventcampus.backend.entity.QrCode;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface QrCodeRepository extends JpaRepository<QrCode, Integer> {
    Optional<QrCode> findByRegistration_IdRegistration(Integer idRegistration);
    @EntityGraph(attributePaths = {"registration", "registration.event", "registration.user", "registration.user.role"})
    java.util.List<QrCode> findByRegistration_Event_IdEventAndCodeEndingWith(Integer eventId, String suffix);
    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE qr_codes SET code=CONCAT('revoked:',id_qr_code) WHERE id_registration=:id", nativeQuery = true)
    int revoke(@Param("id") Integer id);
}
