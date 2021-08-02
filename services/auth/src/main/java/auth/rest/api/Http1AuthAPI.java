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

import auth.security.BCryptProvider;
import auth.security.RandomSessionIdGenerator;
import auth.security.ShaSecurityProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utilities.datamodel.*;
import utilities.rest.api.API;
import utilities.rest.api.CookieUtil;
import utilities.rest.client.Http1Client;
import utilities.rest.client.Http1ClientHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * HTTP/1.1 API for auth service
 * /api/auth
 *
 * @author Philipp Backes
 */
public class Http1AuthAPI implements API {
    private Http1Client httpClient;
    private Http1ClientHandler httpHandler;
    private final ObjectMapper mapper;
    private final String gatewayHost;
    private final Integer persistencePort;
    private final HttpRequest request;
    private static final Logger LOG = LogManager.getLogger(Http1AuthAPI.class);

    public Http1AuthAPI(String gatewayHost, Integer gatewayPort) {
        mapper = new ObjectMapper();
        if (gatewayHost.isEmpty()) {
            this.gatewayHost = "localhost";
            persistencePort = DEFAULT_PERSISTENCE_PORT;
        } else {
            this.gatewayHost = gatewayHost;
            persistencePort = gatewayPort;
        }
        // HTTP/1.1
        request = new DefaultFullHttpRequest(
                HTTP_1_1,
                GET,
                "",
                Unpooled.EMPTY_BUFFER
        );
        request.headers().set(HttpHeaderNames.HOST, this.gatewayHost);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    }

    public FullHttpResponse handle(HttpRequest header, ByteBuf body, LastHttpContent trailer) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(header.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String method = header.method().name();
        String path = queryStringDecoder.path();
        String cookieValue = header.headers().get(HttpHeaderNames.COOKIE);
        SessionData sessionData = CookieUtil.decodeCookie(cookieValue);

        // Select endpoint
        if (path.startsWith("/api/auth")) {
            String subPath = path.substring("/api/auth".length());
            switch (method) {
                case "GET":
                    switch (subPath) {
                        case "/isready":
                            return isReady();
                    }
                case "POST":
                    switch (subPath) {
                        case "/cart/add":
                            if (params.containsKey("productid")) {
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                return addProductToCart(sessionData, productId);
                            } else {
                                return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
                            }
                        case "/cart/remove":
                            if (params.containsKey("productid")) {
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                return removeProductFromCart(sessionData, productId);
                            } else {
                                return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
                            }
                        case "/useractions/placeorder":
                            if (body != null) {
                                return placeOrder(sessionData, body);
                            }
                            return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
                        case "/useractions/login":
                            if (params.containsKey("name") && params.containsKey("password")) {
                                String name = params.get("name").get(0);
                                String password = params.get("password").get(0);
                                return login(sessionData, name, password);
                            } else {
                                return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
                            }
                        case "/useractions/logout":
                            return logout(sessionData);
                        case "/useractions/isloggedin":
                            return isLoggedIn(sessionData);
                    };
                case "PUT":
                    switch (subPath) {
                        case "/cart/update":
                            if (params.containsKey("productid") && params.containsKey("quantity")) {
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                Integer quantity = Integer.parseInt(params.get("quantity").get(0));
                                return updateQuantity(sessionData, productId, quantity);
                            } else {
                                return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
                            }
                    }
                default:
                    break;
            }
        }
        return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
    }

    /**
     * POST /cart/add?productid=X
     *
     * Adds product to cart.
     * If the product is already in the cart the quantity is increased.
     *
     * @param sessionData Session data from the current user
     * @param productId Product id
     * @return Updated session data
     */
    private FullHttpResponse addProductToCart(SessionData sessionData, Long productId) {
        Product product = null;
        // GET api/persistence/products?id=productId
        String persistenceEndpointProduct = PERSISTENCE_ENDPOINT + "/products?id=" + productId;
        try {
            request.setUri(persistenceEndpointProduct);
            request.setMethod(GET);
            // Create client and send request
            httpClient = new Http1Client(gatewayHost, persistencePort, request);
            httpHandler = new Http1ClientHandler();
            httpClient.sendRequest(httpHandler);
            if (!httpHandler.jsonContent.isEmpty()) {
                product = mapper.readValue(httpHandler.jsonContent, Product.class);
                HashMap<Long, OrderItem> itemMap = new HashMap<>();
                SessionData data = null;
                if (sessionData.orderItems().isEmpty()) {
                    itemMap.put(product.id(),
                        new OrderItem(
                            null,
                            product.id(),
                            null,
                            1,
                            product.listPriceInCents()
                        )
                    );
                } else {
                    for (OrderItem orderItem : sessionData.orderItems()) {
                        if (!itemMap.containsKey(orderItem.productId())) {
                            itemMap.put(orderItem.productId(), orderItem);
                        }
                        if (orderItem.productId().equals(productId)) {
                            itemMap.put(productId, new OrderItem(
                                    orderItem.id(),
                                    productId,
                                    orderItem.orderId(),
                                    orderItem.quantity() + 1,
                                    orderItem.unitPriceInCents()
                                )
                            );
                        } else if (!itemMap.containsKey(productId)) {
                            itemMap.put(productId, new OrderItem(
                                    null,
                                    productId,
                                    null,
                                    1,
                                    product.listPriceInCents()
                                )
                            );
                        }
                    }
                }
                List<OrderItem> items = new ArrayList<>(itemMap.values());
                data = new SessionData(
                        sessionData.userId(),
                        sessionData.sessionId(),
                        sessionData.token(),
                        sessionData.order(),
                        items,
                        sessionData.message()
                );
                data = new ShaSecurityProvider().secure(data);
                String json = mapper.writeValueAsString(data);
                return new DefaultFullHttpResponse(
                        HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    /**
     * POST /cart/remove?productid=X
     *
     * Remove product from cart.
     *
     * @param sessionData Session data from the current user
     * @param productId Product id
     * @return Updated session data
     */
    private FullHttpResponse removeProductFromCart(SessionData sessionData, Long productId) {
        OrderItem toRemove = null;
        try {
            for (OrderItem item : sessionData.orderItems()) {
                if (item.productId().equals(productId)) {
                    toRemove = item;
                }
            }
            if (toRemove != null) {
                sessionData.orderItems().remove(toRemove);
                SessionData data = new ShaSecurityProvider().secure(sessionData);
                String json = mapper.writeValueAsString(data);
                return new DefaultFullHttpResponse(
                        HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
                return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    /**
     * PUT /cart/update?productid=X&quantity=Y
     *
     * Updates quantity of product in cart.
     *
     * @param sessionData Session data from the current user
     * @param productId Product id
     * @param quantity New quantity
     * @return Updated session data
     */
    private FullHttpResponse updateQuantity(SessionData sessionData, Long productId, Integer quantity) {
        try {
            for (OrderItem item : sessionData.orderItems()) {
                if (item.productId().equals(productId)) {
                    OrderItem newItem = new OrderItem(
                            item.id(),
                            item.productId(),
                            item.orderId(),
                            quantity,
                            item.unitPriceInCents()
                    );
                    int index = sessionData.orderItems().indexOf(item);
                    sessionData.orderItems().set(index, newItem);
                    SessionData data = new ShaSecurityProvider().secure(sessionData);
                    String json = mapper.writeValueAsString(data);
                    return new DefaultFullHttpResponse(
                            HTTP_1_1,
                            HttpResponseStatus.OK,
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                }
            }
            return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    /**
     * POST /useractions/placeorder
     *
     * Persists order in database.
     *
     * @param sessionData Session data from the current user
     * @param body Order as JSON
     * @return Updated session data
     */
    private FullHttpResponse placeOrder(SessionData sessionData, ByteBuf body) {
        Order orderData = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        // POST api/persistence/orders
        String persistenceEndpointCreateOrder = PERSISTENCE_ENDPOINT + "/orders";
        // POST api/persistence/orderitems
        String persistenceEndpointCreateOrderItem = PERSISTENCE_ENDPOINT + "/orderitems";
        if (new ShaSecurityProvider().validate(sessionData) == null || sessionData.orderItems().isEmpty()) {
            return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
        }

        try {
            long totalPrice = 0;
            for (OrderItem item : sessionData.orderItems()) {
                totalPrice += item.quantity() * item.unitPriceInCents();
            }
            orderData = mapper.readValue(jsonByte, Order.class);
            Order newOrder = new Order(
                    null,
                    sessionData.userId(),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    totalPrice,
                    orderData.addressName(),
                    orderData.address1(),
                    orderData.address2(),
                    orderData.creditCardCompany(),
                    orderData.creditCardNumber(),
                    orderData.creditCardExpiryDate()
            );
            Long orderId = null;
            String orderJson = mapper.writeValueAsString(newOrder);
            ByteBuf postOrderBody = Unpooled.copiedBuffer(orderJson, CharsetUtil.UTF_8);
            FullHttpRequest postOrderRequest = new DefaultFullHttpRequest(
                    HTTP_1_1,
                    POST,
                    persistenceEndpointCreateOrder,
                    Unpooled.copiedBuffer(orderJson, CharsetUtil.UTF_8)
            );
            postOrderRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, postOrderBody.readableBytes());
            postOrderRequest.headers().setAll(request.headers());
            // Create client and send request
            httpClient = new Http1Client(gatewayHost, persistencePort, postOrderRequest);
            httpHandler = new Http1ClientHandler();
            httpClient.sendRequest(httpHandler);
            if (!httpHandler.jsonContent.isEmpty()) {
                orderId = mapper.readValue(httpHandler.jsonContent, Order.class).id();
                for (OrderItem item : sessionData.orderItems()) {
                    OrderItem orderItem = new OrderItem(
                            item.id(),
                            item.productId(),
                            orderId,
                            item.quantity(),
                            item.unitPriceInCents()
                    );
                    String orderItemJson = mapper.writeValueAsString(orderItem);
                    ByteBuf postOrderItemBody = Unpooled.copiedBuffer(orderItemJson, CharsetUtil.UTF_8);
                    FullHttpRequest postOrderItemRequest = new DefaultFullHttpRequest(
                            HTTP_1_1,
                            POST,
                            persistenceEndpointCreateOrderItem,
                            Unpooled.copiedBuffer(orderItemJson, CharsetUtil.UTF_8)
                    );
                    postOrderItemRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, postOrderItemBody.readableBytes());
                    postOrderItemRequest.headers().setAll(request.headers());
                    // Create client and send request
                    httpClient = new Http1Client(gatewayHost, persistencePort, postOrderItemRequest);
                    httpHandler = new Http1ClientHandler();
                    httpClient.sendRequest(httpHandler);
                    if (httpHandler.jsonContent.isEmpty()) {
                        return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
                    }
                    sessionData.orderItems().clear();
                    SessionData data = new SessionData(
                            sessionData.userId(),
                            sessionData.sessionId(),
                            sessionData.token(),
                            new Order(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            ),
                            sessionData.orderItems(),
                            sessionData.message()
                    );
                    data = new ShaSecurityProvider().secure(data);
                    String json = mapper.writeValueAsString(data);
                    return new DefaultFullHttpResponse(
                            HTTP_1_1,
                            HttpResponseStatus.OK,
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    /**
     * POST /useractions/login?name=name&password=password
     *
     * Log in user.
     *
     * @param sessionData Session data from the current user
     * @param name User name
     * @param password User password
     * @return Updated session data
     */
    private FullHttpResponse login(SessionData sessionData, String name, String password) {
        User user = null;
        // GET api/persistence/users/name?name=name
        String persistenceEndpointUser = PERSISTENCE_ENDPOINT + "/users/name?name=" + name;
        try {
            request.setUri(persistenceEndpointUser);
            request.setMethod(GET);
            // Create client and send request
            httpClient = new Http1Client(gatewayHost, persistencePort, request);
            httpHandler = new Http1ClientHandler();
            httpClient.sendRequest(httpHandler);
            if (!httpHandler.jsonContent.isEmpty()) {
                user = mapper.readValue(httpHandler.jsonContent, User.class);
                if (user == null) {
                    return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
                } else if (BCryptProvider.checkPassword(password, user.password())) {
                    SessionData data = new SessionData(
                            user.id(),
                            new RandomSessionIdGenerator().getSessionId(),
                            sessionData.token(),
                            sessionData.order(),
                            sessionData.orderItems(),
                            sessionData.message()
                    );
                    data = new ShaSecurityProvider().secure(data);
                    String json = mapper.writeValueAsString(data);
                    return new DefaultFullHttpResponse(
                            HTTP_1_1,
                            HttpResponseStatus.OK,
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                } else {
                    return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    /**
     * POST /useractions/logout
     *
     * Log out user.
     *
     * @param sessionData Session data from the current user
     * @return Updated session data
     */
    private FullHttpResponse logout(SessionData sessionData) {
        try {
            sessionData.orderItems().clear();
            SessionData data = new SessionData(
                    null,
                    null,
                    sessionData.token(),
                    new Order(
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    sessionData.orderItems(),
                    sessionData.message()
            );
            String json = mapper.writeValueAsString(data);
            return new DefaultFullHttpResponse(
                    HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    /**
     * POST /useractions/isloggedin
     *
     * Checks if user is logged in.
     *
     * @param sessionData Session data from the current user
     * @return Updated session data
     */
    private FullHttpResponse isLoggedIn(SessionData sessionData) {
        SessionData data = new ShaSecurityProvider().validate(sessionData);
        try {
            String json = mapper.writeValueAsString(data);
            return new DefaultFullHttpResponse(
                    HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }


    /**
     * GET /isready
     *
     * This methods checks, if the service is ready
     *
     * @return True
     */
    private FullHttpResponse isReady() {
        try {
            String json = mapper.writeValueAsString(Boolean.TRUE);
            return new DefaultFullHttpResponse(
                    HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }
}
