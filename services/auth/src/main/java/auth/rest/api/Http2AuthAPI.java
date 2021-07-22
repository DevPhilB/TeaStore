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
import io.netty.handler.codec.http2.*;
import io.netty.util.CharsetUtil;
import utilities.datamodel.*;
import utilities.rest.api.API;
import utilities.rest.api.CookieUtil;
import utilities.rest.api.Http2Response;
import utilities.rest.client.Http2Client;
import utilities.rest.client.Http2ClientStreamFrameHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * API for auth service
 * /api/auth
 *
 * @author Philipp Backes
 */
public class Http2AuthAPI implements API {
    private Http2Client httpClient;
    private Http2ClientStreamFrameHandler frameHandler;
    private final ObjectMapper mapper;
    private final String gatewayHost;
    private final Integer persistencePort;
    private Http2Headers http2Header;
    private Http2DataFrame http2DataFrame;

    public Http2AuthAPI(String gatewayHost, Integer gatewayPort) {
        this.mapper = new ObjectMapper();
        if(gatewayHost.isEmpty()) {
            this.gatewayHost = "localhost";
            this.persistencePort = DEFAULT_PERSISTENCE_PORT;
        } else {
            this.gatewayHost = gatewayHost;
            this.persistencePort = gatewayPort;
        }
        http2Header = new DefaultHttp2Headers().scheme(HTTPS);
        http2Header.set(HttpHeaderNames.HOST, this.gatewayHost);
        http2Header.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        http2Header.set(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON);
        http2Header.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    }

    public Http2Response handle(Http2Headers headers, ByteBuf body) {
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
                                return Http2Response.badRequestResponse();
                            }
                        case "/cart/remove":
                            if (params.containsKey("productid")) {
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                return removeProductFromCart(sessionData, productId);
                            } else {
                                return Http2Response.badRequestResponse();
                            }
                        case "/useractions/placeorder":
                            if (body != null) {
                                return placeOrder(sessionData, body);
                            }
                            return Http2Response.badRequestResponse();
                        case "/useractions/login":
                            if (params.containsKey("name") && params.containsKey("password")) {
                                String name = params.get("name").get(0);
                                String password = params.get("password").get(0);
                                return login(sessionData, name, password);
                            } else {
                                return Http2Response.badRequestResponse();
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
                                return Http2Response.badRequestResponse();
                            }
                    }
                default:
                    break;
            }
        }
        return Http2Response.notFoundResponse();
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
    private Http2Response addProductToCart(SessionData sessionData, Long productId) {
        Product product = null;
        // GET api/persistence/products?id=productId
        String persistenceEndpointProduct = PERSISTENCE_ENDPOINT + "/products?id=" + productId;
        try {
            http2Header.method(GET.asciiName()).path(persistenceEndpointProduct);
            // Create client and send request
            httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
            frameHandler = new Http2ClientStreamFrameHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                product = mapper.readValue(frameHandler.jsonContent, Product.class);
                HashMap<Long, OrderItem> itemMap = new HashMap<>();
                OrderItem item = null;
                SessionData data = null;
                if(sessionData.orderItems().isEmpty()) {
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
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length()),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
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
    private Http2Response removeProductFromCart(SessionData sessionData, Long productId) {
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
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length()),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
                return Http2Response.notFoundResponse();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
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
    private Http2Response updateQuantity(SessionData sessionData, Long productId, Integer quantity) {
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
                    return new Http2Response(
                            Http2Response.okJsonHeader(json.length()),
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                }
            }
            return Http2Response.notFoundResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
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
    private Http2Response placeOrder(SessionData sessionData, ByteBuf body) {
        Order orderData = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        // POST api/persistence/orders
        String persistenceEndpointCreateOrder = PERSISTENCE_ENDPOINT + "/orders";
        // POST api/persistence/orderitems
        String persistenceEndpointCreateOrderItem = PERSISTENCE_ENDPOINT + "/orderitems";
        if (new ShaSecurityProvider().validate(sessionData) == null || sessionData.orderItems().isEmpty()) {
            return new Http2Response(http2Header.status(NOT_FOUND.codeAsText()), null);
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
            http2Header.method(POST.asciiName()).path(persistenceEndpointCreateOrder);
            http2Header.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(postOrderBody.readableBytes()));
            http2DataFrame = new DefaultHttp2DataFrame(postOrderBody);
            // Create client and send request
            httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, http2DataFrame);
            frameHandler = new Http2ClientStreamFrameHandler();
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
                    http2DataFrame = new DefaultHttp2DataFrame(postOrderItemBody);
                    http2Header.method(POST.asciiName()).path(persistenceEndpointCreateOrder);
                    http2Header.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(postOrderItemBody.readableBytes()));
                    http2DataFrame = new DefaultHttp2DataFrame(postOrderItemBody);
                    // Create client and send request
                    httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, http2DataFrame);
                    frameHandler= new Http2ClientStreamFrameHandler();
                    httpClient.sendRequest(frameHandler);
                    if (frameHandler.jsonContent.isEmpty()) {
                        return Http2Response.badRequestResponse();
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
                    return new Http2Response(
                            Http2Response.okJsonHeader(json.length()),
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
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
    private Http2Response login(SessionData sessionData, String name, String password) {
        User user = null;
        // GET api/persistence/users/name?name=name
        String persistenceEndpointUser = PERSISTENCE_ENDPOINT + "/users/name?name=" + name;
        try {
            http2Header.method(GET.asciiName()).path(persistenceEndpointUser);
            // Create client and send request
            httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
            frameHandler = new Http2ClientStreamFrameHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                user = mapper.readValue(frameHandler.jsonContent, User.class);
                if(user == null) {
                    return new Http2Response(http2Header.status(NOT_FOUND.codeAsText()), null);
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
                    return new Http2Response(
                            Http2Response.okJsonHeader(json.length()),
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                } else {
                    return Http2Response.badRequestResponse();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
    }

    /**
     * POST /useractions/logout
     *
     * Log out user.
     *
     * @param sessionData Session data from the current user
     * @return Updated session data
     */
    private Http2Response logout(SessionData sessionData) {
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
            return new Http2Response(
                    Http2Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
    }

    /**
     * POST /useractions/isloggedin
     *
     * Checks if user is logged in.
     *
     * @param sessionData Session data from the current user
     * @return Updated session data
     */
    private Http2Response isLoggedIn(SessionData sessionData) {
        SessionData data = new ShaSecurityProvider().validate(sessionData);
        try {
            String json = mapper.writeValueAsString(data);
            return new Http2Response(
                    Http2Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
    }


    /**
     * GET /isready
     *
     * This methods checks, if the service is ready
     *
     * @return True
     */
    private Http2Response isReady() {
        try {
            String json = mapper.writeValueAsString(Boolean.TRUE);
            return new Http2Response(
                    Http2Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
    }
}
