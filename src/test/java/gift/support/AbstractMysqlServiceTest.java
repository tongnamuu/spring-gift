package gift.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

@Tag("service")
@SpringBootTest
@ActiveProfiles("service-test")
public abstract class AbstractMysqlServiceTest {
    private static final String MYSQL_URL =
        "jdbc:mysql://localhost:3307/spring_gift_test?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String MYSQL_USERNAME = "gift";
    private static final String MYSQL_PASSWORD = "gift";
    private static final Duration MYSQL_STARTUP_TIMEOUT = Duration.ofSeconds(60);
    private static boolean mysqlStarted = false;

    @BeforeAll
    static void startMysql() {
        synchronized (AbstractMysqlServiceTest.class) {
            if (mysqlStarted) {
                return;
            }

            runDockerComposeUp();
            waitForMysql();
            mysqlStarted = true;
        }
    }

    private static void runDockerComposeUp() {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "docker",
            "compose",
            "-f",
            "compose.yaml",
            "-f",
            "compose.service-test.yaml",
            "up",
            "-d",
            "mysql-test"
        );
        processBuilder.inheritIO();

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Failed to start MySQL with docker compose.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute docker compose.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting MySQL with docker compose.", e);
        }
    }

    private static void waitForMysql() {
        Instant deadline = Instant.now().plus(MYSQL_STARTUP_TIMEOUT);
        SQLException lastException = null;

        while (Instant.now().isBefore(deadline)) {
            try (var ignored = DriverManager.getConnection(MYSQL_URL, MYSQL_USERNAME, MYSQL_PASSWORD)) {
                return;
            } catch (SQLException e) {
                lastException = e;
                sleepBeforeRetry();
            }
        }

        throw new IllegalStateException("MySQL was not ready within " + MYSQL_STARTUP_TIMEOUT, lastException);
    }

    private static void sleepBeforeRetry() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for MySQL.", e);
        }
    }
}
