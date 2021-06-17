package utilities.netty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestUtilsTest {

    private static final String PARAMETER = "?query=42";

    @Test
    public void isPort() throws Exception {
        assertEquals( "?query=42", PARAMETER);
    }
}