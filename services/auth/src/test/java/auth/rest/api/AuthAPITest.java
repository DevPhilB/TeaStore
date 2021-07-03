/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package auth.rest.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static utilities.rest.api.API.AUTH_ENDPOINT;

class AuthAPITest {

    private final HttpVersion version = HttpVersion.HTTP_1_1;
    private HttpRequest header;
    private ByteBuf body;
    private HttpResponse response;
    private AuthAPI api;

    @BeforeEach
    void setUp() {
        header = new DefaultFullHttpRequest(
                version,
                HttpMethod.GET,
                AUTH_ENDPOINT
        );
        body = null;
        api = new AuthAPI(version);
    }

    @AfterEach
    void tearDown() {
        api = null;
    }

    @Test
    void testGetTest() {
        header.setMethod(HttpMethod.POST);
        header.setUri(AUTH_ENDPOINT + "/test");
        body = Unpooled.buffer();
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
    }
}