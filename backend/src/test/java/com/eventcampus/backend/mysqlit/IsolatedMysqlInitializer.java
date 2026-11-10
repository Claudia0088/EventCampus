package com.eventcampus.backend.mysqlit;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import java.sql.*;
import java.time.Duration;
import java.util.UUID;

/** No JDBC access is possible until the opt-in, Docker and live-container checks pass. */
public final class IsolatedMysqlInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static RuntimeDatabase runtime;

    @Override public void initialize(ConfigurableApplicationContext context) {
        if (!"true".equals(System.getProperty("eventcampus.mysql-it.enabled")))
            throw new IllegalStateException("MySQL IT disabled. Explicit mysql-it profile required; no fallback.");
        var environment = context.getEnvironment();
        if (!"classpath:/application-mysql-it.properties".equals(environment.getProperty("spring.config.location"))
                || environment.containsProperty("spring.config.additional-location")
                || environment.containsProperty("spring.config.import")
                || !"jdbc:blocked:missing-test-container".equals(environment.getProperty("spring.datasource.url")))
            throw new IllegalStateException("External/local configuration rejected before Docker or JDBC access.");
        {
            for (var source : environment.getPropertySources()) {
                if (source.getName().contains("application.properties"))
                    throw new IllegalStateException("Local application.properties must not be loaded.");
            }
        }
        synchronized (IsolatedMysqlInitializer.class) {
            if (runtime == null) runtime = new RuntimeDatabase();
        }
        runtime.datasource.checkTarget();
        RootBeanDefinition definition = new RootBeanDefinition(GuardedDataSource.class,
                () -> runtime.datasource);
        ((BeanDefinitionRegistry) context.getBeanFactory()).registerBeanDefinition("dataSource", definition);
    }

    private static final class RuntimeDatabase {
        final GenericContainer<?> container;
        final GuardedDataSource datasource;
        RuntimeDatabase() {
            // This contacts Docker only. Missing/unreachable Docker is fatal, never a skipped test.
            DockerClientFactory.instance().client().pingCmd().exec();
            String user = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String password = UUID.randomUUID().toString();
            container = new GenericContainer<>(DockerImageName.parse("mysql:8.4.7"))
                .withEnv("MYSQL_DATABASE", "eventcampus_it")
                .withEnv("MYSQL_USER", user)
                .withEnv("MYSQL_PASSWORD", password)
                .withEnv("MYSQL_ROOT_PASSWORD", UUID.randomUUID().toString())
                .withEnv("TZ", "Europe/Paris")
                .withExposedPorts(3306)
                .withReuse(false)
                .waitingFor(Wait.forListeningPort()).withStartupTimeout(Duration.ofSeconds(120));
            // GenericContainer performs no JDBC health probe and mounts no host files.
            container.start();
            try {
                datasource = new GuardedDataSource(container, user, password);
                new ResourceDatabasePopulator(new ClassPathResource("db/mysql-it-schema.sql"),
                    new ClassPathResource("db/mysql-it-fixtures.sql")).execute(datasource);
            } catch (RuntimeException failure) { container.stop(); throw failure; }
            Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
        }
    }

    /** The sole datasource: both JPA and the explicit GET_LOCK connections use this bean. */
    static final class GuardedDataSource extends AbstractDataSource {
        private final GenericContainer<?> container;
        private final String containerId, host, url, user, password;
        private final int port;
        GuardedDataSource(GenericContainer<?> container, String user, String password) {
            this.container = container; this.containerId = container.getContainerId();
            this.host = container.getHost(); this.port = container.getMappedPort(3306);
            this.user = user; this.password = password;
            // Host is supplied by Docker, never Spring/environment JDBC configuration.
            if (!host.matches("[A-Za-z0-9.-]+") || port == 3306 || port <= 0)
                throw new IllegalStateException("Unsafe disposable MySQL endpoint.");
            this.url = "jdbc:mysql://" + host + ":" + port
                + "/eventcampus_it?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Paris";
            checkTarget();
        }
        void checkTarget() {
            if (!"true".equals(System.getProperty("eventcampus.mysql-it.enabled"))
                    || containerId == null || !containerId.equals(container.getContainerId())
                    || !container.isRunning() || !host.equals(container.getHost())
                    || port == 3306 || port != container.getMappedPort(3306)
                    || !url.equals("jdbc:mysql://" + host + ":" + port
                        + "/eventcampus_it?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Paris"))
                throw new IllegalStateException("JDBC target rejected before connection; no fallback.");
        }
        @Override public Connection getConnection() throws SQLException {
            checkTarget();
            return DriverManager.getConnection(url, user, password);
        }
        @Override public Connection getConnection(String username, String suppliedPassword) throws SQLException {
            throw new SQLException("Externally supplied credentials are forbidden in isolated tests.");
        }
    }
}