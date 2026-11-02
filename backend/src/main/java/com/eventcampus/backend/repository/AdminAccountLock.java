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
public class AdminAccountLock {
    private final DataSource datasource;
    private final TransactionTemplate transaction;
    public AdminAccountLock(DataSource datasource, PlatformTransactionManager manager) {
        this.datasource = datasource; this.transaction = new TransactionTemplate(manager);
    }
    public <T> T execute(Supplier<T> work) {
        // Dedicated connection holds the named lock until the separate JPA transaction commits.
        // This serializes cooperating admin writers even with the existing MyISAM tables.
        try (var connection = datasource.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT GET_LOCK('eventcampus.admin.accounts', 10)");
                 var result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Une modification est en cours. Réessayez.");
            }
            try { return transaction.execute(status -> work.get()); }
            finally {
                try (var statement = connection.prepareStatement("SELECT RELEASE_LOCK('eventcampus.admin.accounts')")) {
                    statement.execute();
                }
            }
        } catch (SQLException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Modification indisponible. Réessayez.");
        }
    }
}
