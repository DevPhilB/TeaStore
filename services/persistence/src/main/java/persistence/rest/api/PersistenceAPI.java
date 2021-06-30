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
import io.netty.util.CharsetUtil;
import persistence.database.*;
import utilities.datamodel.*;
import utilities.rest.api.API;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

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

    public PersistenceAPI(HttpVersion httpVersion, String scheme) {
        this.httpVersion = httpVersion;
        this.scheme = scheme;
        this.mapper = new ObjectMapper();
    }

    public FullHttpResponse handle(HttpRequest header, ByteBuf body, LastHttpContent trailer) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(header.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String method = header.method().name();
        String path = queryStringDecoder.path();

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
                            } else if (params.containsKey("userid")
                                    && params.containsKey("start")
                                    && params.containsKey("max")
                            ) {
                                Long id = Long.parseLong(params.get("userid").get(0));
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
                            if (params.containsKey("categoryid")) {
                                Long categoryId = Long.parseLong(params.get("categoryid").get(0));
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
                            if (params.containsKey("classname")) {
                                String className = params.get("classname").get(0);
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
                    return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
            }
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
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
     * @param id Required category Id
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
            long newId = CategoryRepository.REPOSITORY.createEntity(category);
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
     * GET /generatedb
     *
     * @param categories Number of categories
     * @param products Number of products per category
     * @param users Number of users
     * @param orders Maximum order per user
     * @return OK or INTERNAL_SERVER_ERROR
     */
    private FullHttpResponse generateDatabase(Integer categories, Integer products, Integer users, Integer orders) {
        if (DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
        }
        DataGenerator.GENERATOR.setMaintenanceModeGlobal(true);
        DataGenerator.GENERATOR.dropAndCreateTables();
        Executors.newSingleThreadScheduledExecutor().execute(() -> {
            DataGenerator.GENERATOR.generateDatabaseContent(
                    categories,
                    products,
                    users,
                    orders
            );
            CacheManager.MANAGER.resetRemoteEMFs();
            DataGenerator.GENERATOR.setMaintenanceModeGlobal(false);
        });
        return new DefaultFullHttpResponse(httpVersion, OK);
    }

    /**
     * GET /generatedb/finished
     *
     * @return True or false
     */
    private FullHttpResponse generateDatabaseFinishFlag() {
        Boolean finished = DataGenerator.GENERATOR.getGenerationFinishedFlag();
        try {
            String json = mapper.writeValueAsString(finished);
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
     * POST /generatedb/maintenance
     *
     * @param body New maintenance mode (Boolean)
     * @return OK or INTERNAL_SERVER_ERROR
     */
    private FullHttpResponse generateDatabaseToggleMaintenance(ByteBuf body) {
        Boolean maintenanceMode = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            maintenanceMode = mapper.readValue(jsonByte, Boolean.class);
            DataGenerator.GENERATOR.setMaintenanceModeInternal(maintenanceMode);
            return new DefaultFullHttpResponse(httpVersion, OK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /generatedb/maintenance
     *
     * @return True or false
     */
    private FullHttpResponse generateDatabaseMaintenanceFlag() {
        Boolean isMaintenanceMode = DataGenerator.GENERATOR.isMaintenanceMode();
        try {
            String json = mapper.writeValueAsString(isMaintenanceMode);
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
     * GET /orders?id=
     *
     * @param id Required order Id
     * @return Order or NOT_FOUND
     */
    private FullHttpResponse getOrder(Long id) {
        PersistenceOrder persistenceEntity = OrderRepository.REPOSITORY.getEntity(id);
        if (persistenceEntity == null) {
            return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
        }
        Order order = persistenceEntity.toRecord();
        try {
            String json = mapper.writeValueAsString(order);
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
     * GET /orders
     *
     * @param userId Optional user id
     * @param startIndex Optional start index
     * @param maxResultCount Optional max result count
     * @return All orders
     */
    private FullHttpResponse getAllOrders(Long userId, Integer startIndex, Integer maxResultCount) {
        List<PersistenceOrder> persistenceEntities = null;
        if(userId != null) {
            persistenceEntities = OrderRepository.REPOSITORY.getAllEntitiesWithUser(
                    userId,
                    startIndex,
                    maxResultCount
            );
        } else if (startIndex == null || maxResultCount == null) {
            persistenceEntities = OrderRepository.REPOSITORY.getAllEntities();
        } else {
            persistenceEntities = OrderRepository.REPOSITORY.getAllEntities(startIndex, maxResultCount);
        }
        List<Order> orders = new ArrayList<Order>();
        for (PersistenceOrder persistenceOrder : persistenceEntities) {
            orders.add(persistenceOrder.toRecord());
        }
        try {
            String json = mapper.writeValueAsString(orders);
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
     * POST /orders
     *
     * @param body Order as JSON
     * @return Created element as JSON
     */
    private FullHttpResponse createOrder(ByteBuf body) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        Order order = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            order = mapper.readValue(jsonByte, Order.class);
            long newId = OrderRepository.REPOSITORY.createEntity(order);
            Order newOrder = OrderRepository.REPOSITORY.getEntity(newId).toRecord();
            String json = mapper.writeValueAsString(newOrder);
            if (newOrder != null) {
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
     * PUT /orders
     *
     * @param body Order item as JSON
     * @return Updated element as JSON
     */
    private FullHttpResponse updateOrder(ByteBuf body) {
        Order order = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            order = mapper.readValue(jsonByte, Order.class);
            if (OrderRepository.REPOSITORY.getEntity(order.id()) == null) {
                return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
            }
            if (OrderRepository.REPOSITORY.updateEntity(order.id(), order)) {
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
     * DELETE /orders?id=
     *
     * @param id Order id
     * @return OK or NOT_FOUND
     */
    private FullHttpResponse deleteOrder(Long id) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        if (OrderRepository.REPOSITORY.removeEntity(id)) {
            return new DefaultFullHttpResponse(httpVersion, OK);
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
    }

    /**
     * GET /orderitems?id=
     *
     * @param id Required order item Id
     * @return Order item or NOT_FOUND
     */
    private FullHttpResponse getOrderItem(Long id) {
        PersistenceOrderItem persistenceEntity = OrderItemRepository.REPOSITORY.getEntity(id);
        if (persistenceEntity == null) {
            return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
        }
        OrderItem orderItem = persistenceEntity.toRecord();
        try {
            String json = mapper.writeValueAsString(orderItem);
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
     * GET /orderitems
     *
     * @param productId Optional product id
     * @param orderId Optional order id
     * @param startIndex Optional start index
     * @param maxResultCount Optional max result count
     * @return All order items
     */
    private FullHttpResponse getAllOrderItems(Long productId, Long orderId, Integer startIndex, Integer maxResultCount) {
        List<PersistenceOrderItem> persistenceEntities = null;
        if(productId != null) {
            persistenceEntities = OrderItemRepository.REPOSITORY.getAllEntitiesWithProduct(
                    productId,
                    startIndex,
                    maxResultCount
            );
        } else if (orderId != null) {
            persistenceEntities = OrderItemRepository.REPOSITORY.getAllEntitiesWithOrder(
                    orderId,
                    startIndex,
                    maxResultCount
            );
        } else if (startIndex == null || maxResultCount == null) {
            persistenceEntities = OrderItemRepository.REPOSITORY.getAllEntities();
        } else {
            persistenceEntities = OrderItemRepository.REPOSITORY.getAllEntities(startIndex, maxResultCount);
        }
        List<OrderItem> orderItems = new ArrayList<OrderItem>();
        for (PersistenceOrderItem persistenceOrderItem : persistenceEntities) {
            orderItems.add(persistenceOrderItem.toRecord());
        }
        try {
            String json = mapper.writeValueAsString(orderItems);
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
     * POST /orderitems
     *
     * @param body Order item as JSON
     * @return Created element as JSON
     */
    private FullHttpResponse createOrderItem(ByteBuf body) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        OrderItem orderItem = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            orderItem = mapper.readValue(jsonByte, OrderItem.class);
            long newId = OrderItemRepository.REPOSITORY.createEntity(orderItem);
            OrderItem newOrderItem = OrderItemRepository.REPOSITORY.getEntity(newId).toRecord();
            String json = mapper.writeValueAsString(newOrderItem);
            if (newOrderItem != null) {
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
     * PUT /orderitems
     *
     * @param body Order item as JSON
     * @return Updated element as JSON
     */
    private FullHttpResponse updateOrderItem(ByteBuf body) {
        OrderItem orderItem = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            orderItem = mapper.readValue(jsonByte, OrderItem.class);
            if (OrderItemRepository.REPOSITORY.getEntity(orderItem.id()) == null) {
                return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
            }
            if (OrderItemRepository.REPOSITORY.updateEntity(orderItem.id(), orderItem)) {
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
     * DELETE /orderitems?id=
     *
     * @param id Order item id
     * @return OK or NOT_FOUND
     */
    private FullHttpResponse deleteOrderItem(Long id) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        if (OrderItemRepository.REPOSITORY.removeEntity(id)) {
            return new DefaultFullHttpResponse(httpVersion, OK);
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
    }

    /**
     * GET /products?id=
     *
     * @param id Required product Id
     * @return Product or NOT_FOUND
     */
    private FullHttpResponse getProduct(Long id) {
        PersistenceProduct persistenceEntity = ProductRepository.REPOSITORY.getEntity(id);
        if (persistenceEntity == null) {
            return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
        }
        Product product = persistenceEntity.toRecord();
        try {
            String json = mapper.writeValueAsString(product);
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
     * GET /products
     *
     * @param categoryId Optional category id
     * @param startIndex Optional start index
     * @param maxResultCount Optional max result count
     * @return All products
     */
    private FullHttpResponse getAllProducts(Long categoryId, Integer startIndex, Integer maxResultCount) {
        List<PersistenceProduct> persistenceEntities = null;
        if(categoryId != null) {
            persistenceEntities = ProductRepository.REPOSITORY.getAllEntities(
                    categoryId,
                    startIndex,
                    maxResultCount
            );
        } else if (startIndex == null || maxResultCount == null) {
            persistenceEntities = ProductRepository.REPOSITORY.getAllEntities();
        } else {
            persistenceEntities = ProductRepository.REPOSITORY.getAllEntities(
                    startIndex,
                    maxResultCount
            );
        }
        List<Product> products = new ArrayList<Product>();
        for (PersistenceProduct persistenceProduct : persistenceEntities) {
            products.add(persistenceProduct.toRecord());
        }
        try {
            String json = mapper.writeValueAsString(products);
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
     * GET /products/count?categoryid=
     *
     * @param categoryId Required category id
     * @return Number of products in a category
     */
    private FullHttpResponse getProductCountForCategory(Long categoryId) {
        Long productCount = ProductRepository.REPOSITORY.getProductCount(categoryId);
        try {
            String json = mapper.writeValueAsString(productCount);
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
     * POST /products
     *
     * @param body Product as JSON
     * @return Created element as JSON
     */
    private FullHttpResponse createProduct(ByteBuf body) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        Product product = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            product = mapper.readValue(jsonByte, Product.class);
            long newId = ProductRepository.REPOSITORY.createEntity(product);
            Product newProduct = ProductRepository.REPOSITORY.getEntity(newId).toRecord();
            String json = mapper.writeValueAsString(newProduct);
            if (newProduct != null) {
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
     * PUT /products
     *
     * @param body Product as JSON
     * @return Updated element as JSON
     */
    private FullHttpResponse updateProduct(ByteBuf body) {
        Product product = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            product = mapper.readValue(jsonByte, Product.class);
            if (ProductRepository.REPOSITORY.getEntity(product.id()) == null) {
                return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
            }
            if (ProductRepository.REPOSITORY.updateEntity(product.id(), product)) {
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
     * DELETE /products?id=
     *
     * @param id Product item id
     * @return OK or NOT_FOUND
     */
    private FullHttpResponse deleteProduct(Long id) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        if (ProductRepository.REPOSITORY.removeEntity(id)) {
            return new DefaultFullHttpResponse(httpVersion, OK);
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
    }

    /**
     * GET /users?id=
     *
     * @param id Required user Id
     * @return User or NOT_FOUND
     */
    private FullHttpResponse getUserById(Long id) {
        PersistenceUser persistenceEntity = UserRepository.REPOSITORY.getEntity(id);
        if (persistenceEntity == null) {
            return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
        }
        User user = persistenceEntity.toRecord();
        try {
            String json = mapper.writeValueAsString(user);
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
     * GET /users/name?name=
     *
     * @param name Required user name
     * @return User or NOT_FOUND
     */
    private FullHttpResponse getUserByName(String name) {
        PersistenceUser persistenceEntity = UserRepository.REPOSITORY.getUserByName(name);
        if (persistenceEntity == null) {
            return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
        }
        User user = persistenceEntity.toRecord();
        try {
            String json = mapper.writeValueAsString(user);
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
     * GET /users
     *
     * @param startIndex Optional start index
     * @param maxResultCount Optional max result count
     * @return All users
     */
    private FullHttpResponse getAllUsers(Integer startIndex, Integer maxResultCount) {
        List<PersistenceUser> persistenceEntities = null;
        if (startIndex == null || maxResultCount == null) {
            persistenceEntities = UserRepository.REPOSITORY.getAllEntities();
        } else {
            persistenceEntities = UserRepository.REPOSITORY.getAllEntities(startIndex, maxResultCount);
        }
        List<User> users = new ArrayList<User>();
        for (PersistenceUser persistenceUser : persistenceEntities) {
            users.add(persistenceUser.toRecord());
        }
        try {
            String json = mapper.writeValueAsString(users);
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
     * POST /users
     *
     * @param body User as JSON
     * @return Created element as JSON
     */
    private FullHttpResponse createUser(ByteBuf body) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        User user = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            user = mapper.readValue(jsonByte, User.class);
            long newId = UserRepository.REPOSITORY.createEntity(user);
            User newUser = UserRepository.REPOSITORY.getEntity(newId).toRecord();
            String json = mapper.writeValueAsString(newUser);
            if (newUser != null) {
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
     * PUT /users
     *
     * @param body User as JSON
     * @return Updated element as JSON
     */
    private FullHttpResponse updateUser(ByteBuf body) {
        User user = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            user = mapper.readValue(jsonByte, User.class);
            if (UserRepository.REPOSITORY.getEntity(user.id()) == null) {
                return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
            }
            if (UserRepository.REPOSITORY.updateEntity(user.id(), user)) {
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
     * DELETE /users?id=
     *
     * @param id User item id
     * @return OK or NOT_FOUND
     */
    private FullHttpResponse deleteUser(Long id) {
        if(DataGenerator.GENERATOR.isMaintenanceMode()) {
            return new DefaultFullHttpResponse(httpVersion, SERVICE_UNAVAILABLE);
        }
        if (UserRepository.REPOSITORY.removeEntity(id)) {
            return new DefaultFullHttpResponse(httpVersion, OK);
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
    }
}
