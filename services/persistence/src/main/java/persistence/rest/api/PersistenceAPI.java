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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.util.CharsetUtil;
import persistence.database.*;
import utilities.datamodel.*;
import utilities.rest.API;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpMethod.*;

/**
 * API for web service
 * /api/web
 *
 * @author Philipp Backes
 */
public class PersistenceAPI implements API {
    private final HttpVersion httpVersion;
    private final String scheme;
    private final ObjectMapper mapper;
    private final String gatewayHost;
    private final Integer webPort;
    private final Integer imagePort;
    private final Integer authPort;
    private final Integer recommenderPort;
    private final Integer persistencePort;
    private final HttpRequest request;

    public PersistenceAPI(HttpVersion httpVersion, String scheme) {
        this.httpVersion = httpVersion;
        this.scheme = scheme;
        this.mapper = new ObjectMapper();
        this.gatewayHost = "127.0.0.1";
        this.webPort = 80;
        this.imagePort = 80;
        this.authPort = 80;
        this.recommenderPort = 80;
        this.persistencePort = 80;
        this.request = new DefaultFullHttpRequest(
                this.httpVersion,
                HttpMethod.GET,
                "",
                Unpooled.EMPTY_BUFFER
        );
        this.request.headers().set(HttpHeaderNames.HOST, this.gatewayHost);
        this.request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        this.request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    }

    public FullHttpResponse handle(HttpRequest header, ByteBuf body, LastHttpContent trailer) {
        StringBuilder responseData = new StringBuilder();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(header.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String method = header.method().name();
        String path = queryStringDecoder.path();
        String cookieValue = header.headers().get(HttpHeaderNames.COOKIE);
        SessionData sessionData = decodeCookie(cookieValue);

        // Select endpoint
        if (path.startsWith("/api/persistence")) {
            String subPath = path.substring("/api/persistence".length());
            switch (method) {
                case "GET":
                    switch (subPath) {
                        case "/categories":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return getCategory(id);
                            } else if (params.containsKey("start") && params.containsKey("max")) {
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllCategories(start, max);
                            } else {
                                return getAllCategories(null, null);
                            }
                        case "/generatedb":
                            if (params.containsKey("categories")
                                    && params.containsKey("products")
                                    && params.containsKey("users")
                                    && params.containsKey("orders")
                            ) {
                                Integer categories = Integer.parseInt(params.get("categories").get(0));
                                Integer products = Integer.parseInt(params.get("products").get(0));
                                Integer users = Integer.parseInt(params.get("users").get(0));
                                Integer orders = Integer.parseInt(params.get("orders").get(0));
                                return generateDatabase(categories, products, users, orders);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/generatedb/finished":
                            return generateDatabaseFinishFlag();
                        case "/generatedb/maintenance":
                            return generateDatabaseMaintenanceFlag();
                        case "/orders":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return getOrder(id);
                            } else if (params.containsKey("userId")
                                    && params.containsKey("start")
                                    && params.containsKey("max")
                            ) {
                                Long id = Long.parseLong(params.get("userId").get(0));
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllOrders(id, start, max);
                            } else if (params.containsKey("start") && params.containsKey("max")) {
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllOrders(null, start, max);
                            } else {
                                return getAllOrders(null, null, null);
                            }
                        case "/orderitems":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return getOrderItem(id);
                            } else if (params.containsKey("product")
                                    && params.containsKey("start")
                                    && params.containsKey("max")
                            ) {
                                Long productId = Long.parseLong(params.get("product").get(0));
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllOrderItems(productId, null, start, max);
                            } else if (params.containsKey("order")
                                    && params.containsKey("start")
                                    && params.containsKey("max")
                            ) {
                                Long orderId = Long.parseLong(params.get("order").get(0));
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllOrderItems(null, orderId, start, max);
                            } else if (params.containsKey("start") && params.containsKey("max")) {
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllOrderItems(null, null, start, max);
                            } else {
                                return getAllOrderItems(null, null, null, null);
                            }
                        case "/products":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return getProduct(id);
                            } else if (params.containsKey("category")
                                    && params.containsKey("start")
                                    && params.containsKey("max")
                            ) {
                                Long categoryId = Long.parseLong(params.get("category").get(0));
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllProducts(categoryId, start, max);
                            } else if (params.containsKey("start") && params.containsKey("max")) {
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllProducts(null, start, max);
                            } else {
                                return getAllProducts(null, null, null);
                            }
                        case "/products/count":
                            if (params.containsKey("category")) {
                                Long categoryId = Long.parseLong(params.get("category").get(0));
                                return getProductCountForCategory(categoryId);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/users":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return getUserById(id);
                            } else if (params.containsKey("start") && params.containsKey("max")) {
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllUsers(start, max);
                            } else {
                                return getAllUsers(null, null);
                            }
                        case "/users/name":
                            if (params.containsKey("name")) {
                                String name = params.get("name").get(0);
                                return getUserByName(name);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                    }
                case "POST":
                    switch (subPath) {
                        case "/categories":
                            return createCategory(body);
                        case "/generatedb/maintenance":
                            return generateDatabaseToggleMaintenance(body);
                        case "/orders":
                            return createOrder(body);
                        case "/orderitems":
                            return createOrderItem(body);
                        case "/products":
                            return createProduct(body);
                        case "/users":
                            return createUser(body);
                    };
                case "PUT":
                    switch (subPath) {
                        case "/categories":
                            return updateCategory(body);
                        case "/orders":
                            return updateOrder(body);
                        case "/orderitems":
                            return updateOrderItem(body);
                        case "/products":
                            return updateProduct(body);
                        case "/users":
                            return updateUser(body);
                    }
                case "DELETE":
                    switch (subPath) {
                        case "/cache":
                            if (params.containsKey("className")) {
                                String className = params.get("className").get(0);
                                return clearCache(className);
                            } else {
                                return clearCache(null);
                            }
                        case "/emf":
                            return clearEMF();
                        case "/categories":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return deleteCategory(id);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/orders":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return deleteOrder(id);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/orderitems":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return deleteOrderItem(id);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/products":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return deleteProduct(id);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/users":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return deleteUser(id);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                    };
                default:
                    return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
            }
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
    }

    /**
     * Decode cookie to session data
     *
     * @param cookieValue Cookie value as String
     * @return SessionData
     */
    private SessionData decodeCookie(String cookieValue) {
        SessionData cookie = null;
        if (cookieValue != null) {
            try {
                cookie = mapper.readValue(
                        URLDecoder.decode(
                                cookieValue.substring("SessionData=".length()),
                                CharsetUtil.UTF_8
                        ),
                        SessionData.class
                );
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            cookie = new SessionData(
                    null,
                    null,
                    null,
                    null,
                    new ArrayList<>(),
                    null
            );
        }
        return cookie;
    }

    /**
     * Encode session data as cookie
     *
     * @param sessionData Session data
     * @return Cookie
     */
    private Cookie encodeSessionData(SessionData sessionData) {
        try {
            String encodedCookie = URLEncoder.encode(
                    mapper.writeValueAsString(sessionData),
                    CharsetUtil.UTF_8
            );
            return new DefaultCookie("SessionData", encodedCookie);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * DELETE /cache || /cache?classname=
     *
     * @return Status code 200 or 404
     */
    private FullHttpResponse clearCache(String className) {
        if (className == null) {
            CacheManager.MANAGER.clearLocalCacheOnly();
        } else {
            try {
                Class<?> entityClass = Class.forName(className);
                CacheManager.MANAGER.clearLocalCacheOnly(entityClass);
            } catch (Exception e) {
                e.printStackTrace();
                return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
            }
        }
        return new DefaultFullHttpResponse(httpVersion, OK);
    }

    /**
     * DELETE /emf
     *
     * @return Status code 200
     */
    private FullHttpResponse clearEMF() {
        CacheManager.MANAGER.resetLocalEMF();
        return new DefaultFullHttpResponse(httpVersion, OK);
    }

    /**
     * GET /categories?id=
     *
     * @param id  Required category Id
     * @return Category or NOT_FOUND
     */
    private FullHttpResponse getCategory(Long id) {
        PersistenceCategory persistenceEntity = CategoryRepository.REPOSITORY.getEntity(id);
        if (persistenceEntity == null) {
            return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
        }
        Category category = persistenceEntity.toRecord();
        try {
            String json = mapper.writeValueAsString(category);
            return new DefaultFullHttpResponse(
                    httpVersion,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /categories
     *
     * @param startIndex Optional start index
     * @param maxResultCount Optional max result count
     * @return Category or NOT_FOUND
     */
    private FullHttpResponse getAllCategories(Integer startIndex, Integer maxResultCount) {
        List<PersistenceCategory> persistenceEntities = null;
        if (startIndex == null || maxResultCount == null) {
            persistenceEntities = CategoryRepository.REPOSITORY.getAllEntities();
        } else {
            persistenceEntities = CategoryRepository.REPOSITORY.getAllEntities(startIndex, maxResultCount);
        }
        List<Category> categories = new ArrayList<Category>();
        for (PersistenceCategory persistenceCategory : persistenceEntities) {
            categories.add(persistenceCategory.toRecord());
        }
        try {
            String json = mapper.writeValueAsString(categories);
            return new DefaultFullHttpResponse(
                    httpVersion,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * POST /categories
     *
     * @param body Category as JSON
     * @return Created element as JSON
     */
    private FullHttpResponse createCategory(ByteBuf body) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        Category category = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            category = mapper.readValue(jsonByte, Category.class);
            Long newId = CategoryRepository.REPOSITORY.createEntity(category);
            Category newCategory = CategoryRepository.REPOSITORY.getEntity(newId).toRecord();
            String json = mapper.writeValueAsString(newCategory);
            if (newCategory != null) {
                return new DefaultFullHttpResponse(
                        httpVersion,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            }
            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * PUT /categories
     *
     * @param body Category as JSON
     * @return Updated element as JSON
     */
    private FullHttpResponse updateCategory(ByteBuf body) {
        Category category = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            category = mapper.readValue(jsonByte, Category.class);
            if (CategoryRepository.REPOSITORY.getEntity(category.id()) == null) {
                return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
            }
            if (CategoryRepository.REPOSITORY.updateEntity(category.id(), category)) {
                return new DefaultFullHttpResponse(
                        httpVersion,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(body)
                );
            }
            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * DELETE /categories?id=
     *
     * @return OK or NOT_FOUND
     */
    private FullHttpResponse deleteCategory(Long id) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        if (CategoryRepository.REPOSITORY.removeEntity(id)) {
            return new DefaultFullHttpResponse(httpVersion, OK);
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);

    }

    /**
     *
     * @param categories
     * @param products
     * @param users
     * @param orders
     * @return
     */
    private FullHttpResponse generateDatabase(Integer categories, Integer products, Integer users, Integer orders) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param body
     * @return
     */
    private FullHttpResponse generateDatabaseToggleMaintenance(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }


    /**
     *
     * @return
     */
    private FullHttpResponse generateDatabaseFinishFlag() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @return
     */
    private FullHttpResponse generateDatabaseMaintenanceFlag() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param id
     * @return
     */
    private FullHttpResponse getOrder(Long id) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param id
     * @param start
     * @param max
     * @return
     */
    private FullHttpResponse getAllOrders(Long id, Integer start, Integer max) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param body
     * @return
     */
    private FullHttpResponse createOrder(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param body
     * @return
     */
    private FullHttpResponse updateOrder(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param id
     * @return
     */
    private FullHttpResponse deleteOrder(Long id) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }


    /**
     *
     * @param id
     * @return
     */
    private FullHttpResponse getOrderItem(Long id) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param productId
     * @param orderId
     * @param start
     * @param max
     * @return
     */
    private FullHttpResponse getAllOrderItems(Long productId, Long orderId, Integer start, Integer max) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param body
     * @return
     */
    private FullHttpResponse createOrderItem(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param body
     * @return
     */
    private FullHttpResponse updateOrderItem(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param id
     * @return
     */
    private FullHttpResponse deleteOrderItem(Long id) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param id
     * @return
     */
    private FullHttpResponse getProduct(Long id) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param categoryId
     * @param start
     * @param max
     * @return
     */
    private FullHttpResponse getAllProducts(Long categoryId, Integer start, Integer max) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }


    /**
     *
     * @param categoryId
     * @return
     */
    private FullHttpResponse getProductCountForCategory(Long categoryId) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param body
     * @return
     */
    private FullHttpResponse createProduct(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param body
     * @return
     */
    private FullHttpResponse updateProduct(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param id
     * @return
     */
    private FullHttpResponse deleteProduct(Long id) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param id
     * @return
     */
    private FullHttpResponse getUserById(Long id) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param name
     * @return
     */
    private FullHttpResponse getUserByName(String name) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }


    /**
     *
     * @param start
     * @param max
     * @return
     */
    private FullHttpResponse getAllUsers(Integer start, Integer max) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param body
     * @return
     */
    private FullHttpResponse createUser(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param body
     * @return
     */
    private FullHttpResponse updateUser(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    /**
     *
     * @param id
     * @return
     */
    private FullHttpResponse deleteUser(Long id) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }
}
