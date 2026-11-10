package com.eventcampus.backend.mysqlit;

import com.eventcampus.backend.BackendApplication;
import com.eventcampus.backend.service.QrTokenCodec;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.convention.TestBean;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Arrays;

@SpringBootTest(classes = BackendApplication.class,
    properties = "spring.config.location=classpath:/application-mysql-it.properties")
@ContextConfiguration(initializers = IsolatedMysqlInitializer.class)
public abstract class IsolatedMysqlTestBase {
    // Override the production bean before instantiation; never run its default constructor.
    @TestBean(name = "qrTokenCodec", methodName = "temporaryQrCodec")
    QrTokenCodec qrTokenCodec;

    static QrTokenCodec temporaryQrCodec() throws Exception {
        Path directory = Files.createTempDirectory("eventcampus-mysql-it-key-");
        Path file = directory.resolve("qr.key");
        byte[] key = new byte[32]; new SecureRandom().nextBytes(key);
        try { Files.write(file, key); } finally { Arrays.fill(key, (byte) 0); }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
            catch (java.io.IOException ignored) { /* Temporary files contain only a disposable key. */ }
        }));
        return new QrTokenCodec(file);
    }
}