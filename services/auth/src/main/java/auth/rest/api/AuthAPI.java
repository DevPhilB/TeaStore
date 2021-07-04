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
import utilities.datamodel.*;
import utilities.rest.api.API;
import utilities.rest.api.CookieUtil;
import utilities.rest.client.HttpClient;
import utilities.rest.client.HttpClientHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * API for web service
 * /api/web
 *
 * @author Philipp Backes
 */
public class AuthAPI implements API {
    private final HttpVersion httpVersion;
    private HttpClient httpClient;
    private HttpClientHandler handler;
    private final ObjectMapper mapper;
    private final String gatewayHost;
    private final Integer persistencePort;
    private final HttpRequest request;

    public AuthAPI(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        this.mapper = new ObjectMapper();
        this.gatewayHost = "gateway";
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
                        case "/ready":
                            return isReady();
                    }
                case "POST":
                    switch (subPath) {
                        case "/cart/add":
                            if (params.containsKey("productid")) {
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                return addProductToCart(sessionData, productId);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                    case "/cart/remove":
                        if (params.containsKey("productid")) {
                            Long productId = Long.parseLong(params.get("productid").get(0));
                            return removeProductFromCart(sessionData, productId);
                        } else {
                            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                        }
                    };
                    case "/useractions/placeorder":
                        if(body != null) {
                            return placeOrder(sessionData, body);
                        }
                        return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                    case "/useractions/login":
                        if (params.containsKey("name") && params.containsKey("password")) {
                            String name = params.get("name").get(0);
                            String password = params.get("password").get(0);
                            return login(sessionData, name, password);
                        } else {
                            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                        }
                    case "/useractions/logout":
                        return logout(sessionData);
                    case "/useractions/isloggedin":
                        return isLoggedIn(sessionData);
                case "PUT":
                    switch (subPath) {
                        case "/cart/update":
                            if (params.containsKey("productid") && params.containsKey("quantity")) {
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                Integer quantity = Integer.parseInt(params.get("quantity").get(0));
                                return updateQuantity(sessionData, productId, quantity);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                    }
                default:
                    return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
            }
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
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
            request.setMethod(HttpMethod.GET);
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, persistencePort, request);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if (handler.response instanceof HttpContent httpContent) {
                ByteBuf body = httpContent.content();
                byte[] jsonByte = new byte[body.readableBytes()];
                body.readBytes(jsonByte);
                product = mapper.readValue(jsonByte, Product.class);
                OrderItem item = null;
                SessionData data = null;
                for (OrderItem orderItem : sessionData.orderItems()) {
                    if (orderItem.productId().equals(productId)) {
                        item = new OrderItem(
                                orderItem.id(),
                                orderItem.productId(),
                                orderItem.orderId(),
                                orderItem.quantity() + 1,
                                orderItem.unitPriceInCents()
                        );
                        data = new ShaSecurityProvider().secure(sessionData);
                        String json = mapper.writeValueAsString(data);
                        return new DefaultFullHttpResponse(
                                httpVersion,
                                HttpResponseStatus.OK,
                                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                        );
                    }
                    item = new OrderItem(
                            null,
                            productId,
                            null,
                            1,
                            product.listPriceInCents()
                    );
                    List<OrderItem> items = sessionData.orderItems();
                    items.add(item);
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
                            httpVersion,
                            HttpResponseStatus.OK,
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
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
                        httpVersion,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
                return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
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
                            httpVersion,
                            HttpResponseStatus.OK,
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                }
            }
            return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
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
            return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
        }

        try {
            orderData = mapper.readValue(jsonByte, Order.class);
            Order newOrder = new Order(
                    sessionData.order().id(),
                    sessionData.order().userId(),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    orderData.totalPriceInCents(),
                    orderData.addressName(),
                    orderData.address1(),
                    orderData.address2(),
                    orderData.creditCardCompany(),
                    orderData.creditCardNumber(),
                    orderData.creditCardExpiryDate()
            );
            Long orderId = null;
            request.setUri(persistenceEndpointCreateOrder);
            request.setMethod(HttpMethod.POST);
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, persistencePort, request);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if (handler.response instanceof HttpContent httpContent) {
                ByteBuf contentBody = httpContent.content();
                byte[] jsonContentByte = new byte[contentBody.readableBytes()];
                body.readBytes(jsonContentByte);
                orderId = mapper.readValue(jsonContentByte, Long.class);
                for (OrderItem item : sessionData.orderItems()) {
                    OrderItem orderItem = new OrderItem(
                            null,
                            item.productId(),
                            item.orderId(),
                            item.quantity(),
                            item.unitPriceInCents()
                    );
                    request.setUri(persistenceEndpointCreateOrderItem);
                    request.setMethod(HttpMethod.POST);
                    // Create client and send request
                    httpClient = new HttpClient(gatewayHost, persistencePort, request);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if (handler.response instanceof HttpResponse response) {
                        if (response.status() != OK) {
                            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                        }
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
                            httpVersion,
                            HttpResponseStatus.OK,
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
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
            request.setMethod(HttpMethod.GET);
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, persistencePort, request);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if (handler.response instanceof HttpContent httpContent) {
                ByteBuf contentBody = httpContent.content();
                byte[] jsonContentByte = new byte[contentBody.readableBytes()];
                contentBody.readBytes(jsonContentByte);
                user = mapper.readValue(jsonContentByte, User.class);
                if(user == null) {
                    return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
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
                            httpVersion,
                            HttpResponseStatus.OK,
                            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                    );
                } else {
                    return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
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
                    httpVersion,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }
}