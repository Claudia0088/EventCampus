package com.eventcampus.backend.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.sql.DataSource;
import java.sql.*;
import java.util.function.Supplier;

@Repository
public class RegistrationWriteLock {
    private final DataSource datasource;
    private final TransactionTemplate transaction;
    public RegistrationWriteLock(DataSource datasource, PlatformTransactionManager manager) {
        this.datasource = datasource; this.transaction = new TransactionTemplate(manager);
    }
    private ResultSet lockedResult(PreparedStatement statement, Integer eventId) throws SQLException { statement.setInt(1, eventId); return statement.executeQuery(); }
    public <T> T execute(Integer eventId, Supplier<T> work) {
        // Dedicated connection holds the named lock until the separate JPA transaction commits.
        // Registration and cancellation share the same event-specific lock, including on MyISAM.
        try (var connection = datasource.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT GET_LOCK(CONCAT('eventcampus.registration.', ?), 10)");
                 var result = lockedResult(statement, eventId)) {
                if (!result.next() || result.getInt(1) != 1) throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Une modification est en cours. Réessayez.");
            }
            try { return transaction.execute(status -> work.get()); }
            finally {
                try (var statement = connection.prepareStatement("SELECT RELEASE_LOCK(CONCAT('eventcampus.registration.', ?))")) {
                    statement.setInt(1, eventId); statement.execute();
                }
            }
        } catch (SQLException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Modification indisponible. Réessayez.");
        }
    }
}
