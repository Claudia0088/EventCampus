package com.eventcampus.backend.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.function.Supplier;

@Repository
public class EventReportWriteLock {
    private final DataSource datasource;
    private final TransactionTemplate transaction;
    public EventReportWriteLock(DataSource datasource, PlatformTransactionManager manager) {
        this.datasource = datasource; this.transaction = new TransactionTemplate(manager);
    }
    public <T> T execute(Integer userId, Supplier<T> work) {
        // Hold the named lock through commit so two servers cannot bypass the hourly limit.
        String name = "eventcampus.reports.user." + userId;
        try (var connection = datasource.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT GET_LOCK(?, 10)")) {
                statement.setString(1, name);
                try (var result = statement.executeQuery()) {
                    if (!result.next() || result.getInt(1) != 1)
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Un signalement est en cours d'envoi. Réessayez.");
                }
            }
            try { return transaction.execute(status -> work.get()); }
            finally {
                try (var statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    statement.setString(1, name); statement.execute();
                }
            }
        } catch (SQLException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Envoi indisponible. Réessayez plus tard.");
        }
    }
}
