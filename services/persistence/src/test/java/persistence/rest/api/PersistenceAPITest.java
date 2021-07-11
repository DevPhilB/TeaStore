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
package persistence.rest.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static utilities.rest.api.API.PERSISTENCE_ENDPOINT;

class PersistenceAPITest {

    private final HttpVersion version = HttpVersion.HTTP_1_1;
    private HttpRequest header;
    private ByteBuf body;
    private HttpResponse response;
    private PersistenceAPI api;

    @BeforeEach
    void setUp() {
        header = new DefaultFullHttpRequest(
                version,
                HttpMethod.GET,
                PERSISTENCE_ENDPOINT
        );
        body = null;
        api = new PersistenceAPI(version, "", null);
    }

    @AfterEach
    void tearDown() {
        api = null;
    }

    @Test
    void testClearCache() {
        header.setMethod(HttpMethod.DELETE);
        header.setUri(PERSISTENCE_ENDPOINT + "/cache?classname=test");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
    }

    @Test
    void testClearEMF() {
        header.setMethod(HttpMethod.DELETE);
        header.setUri(PERSISTENCE_ENDPOINT + "/emf");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }

    @Test
    void testGetCategory() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/categories?id=42");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testGetAllCategories() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/categories");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testCreateCategory() {
        header.setMethod(HttpMethod.POST);
        header.setUri(PERSISTENCE_ENDPOINT + "/categories");
        api.handle(header, body, null);
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.status());
    }

    @Test
    void testUpdateCategory() {
        header.setMethod(HttpMethod.PUT);
        header.setUri(PERSISTENCE_ENDPOINT + "/categories");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testDeleteCategory() {
        header.setMethod(HttpMethod.DELETE);
        header.setUri(PERSISTENCE_ENDPOINT + "/categories?id=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.status());
    }

    @Test
    void testGenerateDatabase() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/generatedb?categories=1&products=2&users=3&orders=4");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testGenerateDatabaseFinishFlag() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/generatedb/finished");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testGenerateDatabaseToggleMaintenance() {
        header.setMethod(HttpMethod.POST);
        header.setUri(PERSISTENCE_ENDPOINT + "/generatedb/maintenance");
        body = Unpooled.copyBoolean(true);
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testGenerateDatabaseMaintenanceFlag() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/generatedb/maintenance");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.OK, response.status());
    }

    @Test
    void testGetOrder() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/orders?id=42");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testGetAllOrders() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/orders");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testCreateOrder() {
        header.setMethod(HttpMethod.POST);
        header.setUri(PERSISTENCE_ENDPOINT + "/orders");
        api.handle(header, body, null);
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.status());
    }

    @Test
    void testUpdateOrder() {
        header.setMethod(HttpMethod.PUT);
        header.setUri(PERSISTENCE_ENDPOINT + "/orders");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testDeleteOrder() {
        header.setMethod(HttpMethod.DELETE);
        header.setUri(PERSISTENCE_ENDPOINT + "/orders?id=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.status());
    }

    @Test
    void testGetOrderItem() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/orderitems?id=42");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testGetAllOrderItems() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/orderitems");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testCreateOrderItem() {
        header.setMethod(HttpMethod.POST);
        header.setUri(PERSISTENCE_ENDPOINT + "/orderitems");
        body = Unpooled.buffer();
        api.handle(header, body, null);
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
    }

    @Test
    void testUpdateOrderItem() {
        header.setMethod(HttpMethod.PUT);
        header.setUri(PERSISTENCE_ENDPOINT + "/orderitems");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testDeleteOrderItem() {
        header.setMethod(HttpMethod.DELETE);
        header.setUri(PERSISTENCE_ENDPOINT + "/orders?id=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.status());
    }

    @Test
    void testGetProduct() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/products?id=42");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testGetAllProducts() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/products");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testGetProductCountForCategory() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/products/count?categoryid=1");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testCreateProduct() {
        header.setMethod(HttpMethod.POST);
        header.setUri(PERSISTENCE_ENDPOINT + "/products");
        api.handle(header, body, null);
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.status());
    }

    @Test
    void testUpdateProduct() {
        header.setMethod(HttpMethod.PUT);
        header.setUri(PERSISTENCE_ENDPOINT + "/products");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testDeleteProduct() {
        header.setMethod(HttpMethod.DELETE);
        header.setUri(PERSISTENCE_ENDPOINT + "/products?id=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.status());
    }

    @Test
    void testGetUserById() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/users?id=42");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testGetUserByName() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/users/name?name=Test");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testGetAllUsers() {
        header.setMethod(HttpMethod.GET);
        header.setUri(PERSISTENCE_ENDPOINT + "/users");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testCreateUser() {
        header.setMethod(HttpMethod.POST);
        header.setUri(PERSISTENCE_ENDPOINT + "/users");
        api.handle(header, body, null);
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.status());
    }

    @Test
    void testUpdateUser() {
        header.setMethod(HttpMethod.PUT);
        header.setUri(PERSISTENCE_ENDPOINT + "/users");
        assertThrows(Exception.class, ()-> {
            api.handle(header, body, null);
        });
    }

    @Test
    void testDeleteUser() {
        header.setMethod(HttpMethod.DELETE);
        header.setUri(PERSISTENCE_ENDPOINT + "/users?id=42");
        response = api.handle(header, body, null);
        assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.status());
    }
}