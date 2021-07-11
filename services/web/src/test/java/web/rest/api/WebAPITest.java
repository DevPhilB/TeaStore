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
package web.rest.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static utilities.rest.api.API.WEB_ENDPOINT;

class WebAPITest {

    private final HttpVersion version = HttpVersion.HTTP_1_1;
    private HttpRequest header;
    private ByteBuf body;
    private HttpResponse response;
    private WebAPI api;

    @BeforeEach
    void setUp() {
        header = new DefaultFullHttpRequest(
                version,
                HttpMethod.GET,
                WEB_ENDPOINT
        );
        body = null;
        api = new WebAPI(version, "", null);
    }

    @AfterEach
    void tearDown() {
        api = null;
    }

    @Test
    void testIsReady() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/isready");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }

    @Test
    void testAboutView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/about");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testCartAction() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/cartaction/addtocart?productid=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testConfirmOrder() {
        header.setMethod(HttpMethod.POST);
        header.setUri(WEB_ENDPOINT + "/cartaction/confirm");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
    }

    @Test
    void testCartView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/cart");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testCategoryView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/category?id=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testDatabaseAction() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/databaseaction?categories=1&products=2&users=3&orders=4");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testDatabaseView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/database");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testErrorView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/error");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testIndexView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/index");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testLoginAction() {
        header.setMethod(HttpMethod.POST);
        header.setUri(WEB_ENDPOINT + "/loginaction");
        body = Unpooled.buffer();
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testLoginView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/login");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testOrderView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/order");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testProductView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/product?id=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testProfileView() {
        header.setMethod(HttpMethod.GET);
        header.setUri(WEB_ENDPOINT + "/profile");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }
}