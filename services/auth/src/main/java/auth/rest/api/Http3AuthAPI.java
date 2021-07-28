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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.incubator.codec.http3.*;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utilities.datamodel.*;
import utilities.rest.api.API;
import utilities.rest.api.CookieUtil;
import utilities.rest.api.Http3Response;
import utilities.rest.client.Http3Client;
import utilities.rest.client.Http3ClientStreamInboundHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP/3 API for auth service
 * /api/auth
 *
 * @author Philipp Backes
 */
public class Http3AuthAPI implements API {
    private Http3Client httpClient;
    private Http3ClientStreamInboundHandler frameHandler;
    private final ObjectMapper mapper;
    private final String gatewayHost;
    private final Integer persistencePort;
    private Http3HeadersFrame http3HeadersFrame;
    private Http3DataFrame http3DataFrame;
    private static final Logger LOG = LogManager.getLogger(Http3AuthAPI.class);

    public Http3AuthAPI(String gatewayHost, Integer gatewayPort) {
        this.mapper = new ObjectMapper();
        if (gatewayHost.isEmpty()) {
            this.gatewayHost = "localhost";
            this.persistencePort = DEFAULT_PERSISTENCE_PORT;
        } else {
            this.gatewayHost = gatewayHost;
            this.persistencePort = gatewayPort;
        }
    }

    public Http3Response handle(Http3Headers headers, ByteBuf body) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(headers.path().toString());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String method = headers.method().toString();
        String path = queryStringDecoder.path();
        String cookieValue = headers.get(HttpHeaderNames.COOKIE) != null
                ? headers.get(HttpHeaderNames.COOKIE).toString() : null;
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
                                return Http3Response.badRequestResponse();
                            }
                        case "/cart/remove":
                            if (params.containsKey("productid")) {
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                return removeProductFromCart(sessionData, productId);
                            } else {
                                return Http3Response.badRequestResponse();
                            }
                        case "/useractions/placeorder":
                            if (body != null) {
                                return placeOrder(sessionData, body);
                            }
                            return Http3Response.badRequestResponse();
                        case "/useractions/login":
                            if (params.containsKey("name") && params.containsKey("password")) {
                                String name = params.get("name").get(0);
                                String password = params.get("password").get(0);
                                return login(sessionData, name, password);
                            } else {
                                return Http3Response.badRequestResponse();
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
                                return Http3Response.badRequestResponse();
                            }
                    }
                default:
                    break;
            }
        }
        return Http3Response.notFoundResponse();
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
    private Http3Response addProductToCart(SessionData sessionData, Long productId) {
        Product product = null;
        // GET api/persistence/products?id=productId
        String persistenceEndpointProduct = PERSISTENCE_ENDPOINT + "/products?id=" + productId;
        try {
            http3HeadersFrame = new DefaultHttp3HeadersFrame(
                    Http3Response.getHeader(
                            gatewayHost + ":" + persistencePort,
                            persistenceEndpointProduct
                    )
            );
            // Create client and send request
            httpClient = new Http3Client(gatewayHost, persistencePort, http3HeadersFrame, null);
            frameHandler = new Http3ClientStreamInboundHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                product = mapper.readValue(frameHandler.jsonContent, Product.class);
                HashMap<Long, OrderItem> itemMap = new HashMap<>();
                OrderItem item = null;
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
                return new Http3Response(
                        Http3Response.okJsonHeader(json.length()),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
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
    private Http3Response removeProductFromCart(SessionData sessionData, Long productId) {
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
                return new Http3Response(
                        Http3Response.okJsonHeader(json.length()),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
                return Http3Response.notFoundResponse();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
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
    private Http3Response updateQuantity(SessionData sessionData, Long productId, Integer quantity) {
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
                    return new Http3Response(
                            Http3Response.okJsonHeader(json.length()),
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                }
            }
            return Http3Response.notFoundResponse();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
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
    private Http3Response placeOrder(SessionData sessionData, ByteBuf body) {
        Order orderData = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        // POST api/persistence/orders
        String persistenceEndpointCreateOrder = PERSISTENCE_ENDPOINT + "/orders";
        // POST api/persistence/orderitems
        String persistenceEndpointCreateOrderItem = PERSISTENCE_ENDPOINT + "/orderitems";
        if (new ShaSecurityProvider().validate(sessionData) == null || sessionData.orderItems().isEmpty()) {
            return Http3Response.notFoundResponse();
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
            http3HeadersFrame = new DefaultHttp3HeadersFrame(
                    Http3Response.postContentHeader(
                            gatewayHost + ":" + persistencePort,
                            persistenceEndpointCreateOrder,
                            String.valueOf(postOrderBody.readableBytes())
                    )
            );
            http3DataFrame = new DefaultHttp3DataFrame(postOrderBody);
            // Create client and send request
            httpClient = new Http3Client(gatewayHost, persistencePort, http3HeadersFrame, http3DataFrame);
            frameHandler = new Http3ClientStreamInboundHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                orderId = mapper.readValue(frameHandler.jsonContent, Order.class).id();
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
                    http3HeadersFrame = new DefaultHttp3HeadersFrame(
                            Http3Response.postContentHeader(
                                    gatewayHost + ":" + persistencePort,
                                    persistenceEndpointCreateOrderItem,
                                    String.valueOf(postOrderItemBody.readableBytes())
                            )
                    );
                    http3DataFrame = new DefaultHttp3DataFrame(postOrderItemBody);
                    // Create client and send request
                    httpClient = new Http3Client(gatewayHost, persistencePort, http3HeadersFrame, http3DataFrame);
                    frameHandler= new Http3ClientStreamInboundHandler();
                    httpClient.sendRequest(frameHandler);
                    if (frameHandler.jsonContent.isEmpty()) {
                        return Http3Response.badRequestResponse();
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
                    return new Http3Response(
                            Http3Response.okJsonHeader(json.length()),
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
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
    private Http3Response login(SessionData sessionData, String name, String password) {
        User user = null;
        // GET api/persistence/users/name?name=name
        String persistenceEndpointUser = PERSISTENCE_ENDPOINT + "/users/name?name=" + name;
        try {
            http3HeadersFrame = new DefaultHttp3HeadersFrame(
                    Http3Response.getHeader(
                            gatewayHost + ":" + persistencePort,
                            persistenceEndpointUser
                    )
            );
            // Create client and send request
            httpClient = new Http3Client(gatewayHost, persistencePort, http3HeadersFrame, null);
            frameHandler = new Http3ClientStreamInboundHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                user = mapper.readValue(frameHandler.jsonContent, User.class);
                if (user == null) {
                    return Http3Response.notFoundResponse();
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
                    return new Http3Response(
                            Http3Response.okJsonHeader(json.length()),
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                } else {
                    return Http3Response.badRequestResponse();
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
    }

    /**
     * POST /useractions/logout
     *
     * Log out user.
     *
     * @param sessionData Session data from the current user
     * @return Updated session data
     */
    private Http3Response logout(SessionData sessionData) {
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
            return new Http3Response(
                    Http3Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
    }

    /**
     * POST /useractions/isloggedin
     *
     * Checks if user is logged in.
     *
     * @param sessionData Session data from the current user
     * @return Updated session data
     */
    private Http3Response isLoggedIn(SessionData sessionData) {
        SessionData data = new ShaSecurityProvider().validate(sessionData);
        try {
            String json = mapper.writeValueAsString(data);
            return new Http3Response(
                    Http3Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
    }


    /**
     * GET /isready
     *
     * This methods checks, if the service is ready
     *
     * @return True
     */
    private Http3Response isReady() {
        try {
            String json = mapper.writeValueAsString(Boolean.TRUE);
            return new Http3Response(
                    Http3Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
    }
}
