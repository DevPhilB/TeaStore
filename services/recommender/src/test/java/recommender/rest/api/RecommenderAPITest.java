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
package recommender.rest.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static utilities.rest.api.API.RECOMMENDER_ENDPOINT;

class RecommenderAPITest {

    private HttpRequest header;
    private ByteBuf body;
    private HttpResponse response;
    private Http1RecommenderAPI api;

    @BeforeEach
    void setUp() {
        header = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                RECOMMENDER_ENDPOINT
        );
        body = null;
        api = new Http1RecommenderAPI("", null);
    }

    @AfterEach
    void tearDown() {
        api = null;
    }

    @Test
    void testGetRecommendedProducts() {
        header.setMethod(HttpMethod.POST);
        header.setUri(RECOMMENDER_ENDPOINT + "/recommend?userid=42");
        body = Unpooled.buffer();
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testTrain() {
        header.setMethod(HttpMethod.GET);
        header.setUri(RECOMMENDER_ENDPOINT + "/train");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testGetTimeStamp() {
        header.setMethod(HttpMethod.GET);
        header.setUri(RECOMMENDER_ENDPOINT + "/train/timestamp");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testIsReady() {
        header.setMethod(HttpMethod.GET);
        header.setUri(RECOMMENDER_ENDPOINT + "/train/isready");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }
}