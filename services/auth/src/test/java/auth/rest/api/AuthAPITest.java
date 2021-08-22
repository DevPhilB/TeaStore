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
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static utilities.rest.api.API.AUTH_ENDPOINT;

class AuthAPITest {

    private HttpRequest header;
    private ByteBuf body;
    private HttpResponse response;
    private Http1AuthAPI api;

    @BeforeEach
    void setUp() {
        header = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                AUTH_ENDPOINT
        );
        body = null;
        api = new Http1AuthAPI("", null);
    }

    @AfterEach
    void tearDown() {
        api = null;
    }

    @Test
    void testAddProductToCart() {
        header.setMethod(HttpMethod.POST);
        header.setUri(AUTH_ENDPOINT + "/cart/add?productid=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testRemoveProductFromCart() {
        header.setMethod(HttpMethod.POST);
        header.setUri(AUTH_ENDPOINT + "/cart/remove?productid=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
    }

    @Test
    void testUpdateQuantity() {
        header.setMethod(HttpMethod.PUT);
        header.setUri(AUTH_ENDPOINT + "/cart/update?productid=42?quantity=2");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
    }

    @Test
    void testPlaceOrder() {
        header.setMethod(HttpMethod.POST);
        header.setUri(AUTH_ENDPOINT + "/useractions/placeorder");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
    }

    @Test
    void testLogin() {
        header.setMethod(HttpMethod.POST);
        header.setUri(AUTH_ENDPOINT + "/useractions/login?name=name&password=password");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testLogout() {
        header.setMethod(HttpMethod.POST);
        header.setUri(AUTH_ENDPOINT + "/useractions/logout");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }

    @Test
    void testIsLoggedIn() {
        header.setMethod(HttpMethod.POST);
        header.setUri(AUTH_ENDPOINT + "/useractions/isloggedin");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }

    @Test
    void testIsReady() {
        header.setMethod(HttpMethod.GET);
        header.setUri(AUTH_ENDPOINT + "/isready");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }
}