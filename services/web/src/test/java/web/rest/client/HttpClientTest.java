package web.rest.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpClientTest {

    private static final int PORT = 8080;

    @Test
    public void isPort() throws Exception {
        assertEquals(8080, PORT);
    }
}