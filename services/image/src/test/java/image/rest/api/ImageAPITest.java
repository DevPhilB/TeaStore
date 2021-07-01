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
package image.rest.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static utilities.rest.api.API.IMAGE_ENDPOINT;

class ImageAPITest {

    private final HttpVersion version = HttpVersion.HTTP_1_1;
    private HttpRequest header;
    private ByteBuf body;
    private HttpResponse response;
    private ImageAPI api;

    @BeforeEach
    void setUp() {
        header = new DefaultFullHttpRequest(
                version,
                HttpMethod.GET,
                IMAGE_ENDPOINT
        );
        body = null;
        api = new ImageAPI(version);
    }

    @AfterEach
    void tearDown() {
        api = null;
    }

    @Test
    void testGetProductImages() {
        header.setMethod(HttpMethod.POST);
        header.setUri(IMAGE_ENDPOINT + "/productimages");
        body = Unpooled.buffer();
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testGetWebImages() {
        header.setMethod(HttpMethod.POST);
        header.setUri(IMAGE_ENDPOINT + "/webimages");
        body = Unpooled.buffer();
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testRegenerateImages() {
        header.setMethod(HttpMethod.GET);
        header.setUri(IMAGE_ENDPOINT + "/regenerateimages");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }

    @Test
    void testIsFinished() {
        header.setMethod(HttpMethod.GET);
        header.setUri(IMAGE_ENDPOINT + "/finished");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }

    @Test
    void testGetState() {
        header.setMethod(HttpMethod.GET);
        header.setUri(IMAGE_ENDPOINT + "/state");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }

    @Test
    void testSetCacheSize() {
        header.setMethod(HttpMethod.POST);
        header.setUri(IMAGE_ENDPOINT + "/setcachesize");
        body = Unpooled.copyLong(42L);
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }
}