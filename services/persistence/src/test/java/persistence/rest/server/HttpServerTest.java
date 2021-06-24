package persistence.rest.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpServerTest {

    private static final String HOST = "127.0.0.1";

    @Test
    public void isLocalhost() throws Exception {
        assertEquals("127.0.0.1", HOST);
    }
}