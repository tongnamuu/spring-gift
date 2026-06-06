package gift.support;

import gift.Application;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("service")
@SpringBootTest(classes = Application.class)
@ActiveProfiles("service-test")
public abstract class AbstractMysqlServiceTest extends AbstractMysqlTest {
}
