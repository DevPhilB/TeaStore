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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.util.CharsetUtil;
import utilities.datamodel.*;
import utilities.enumeration.ImageSizePreset;
import utilities.rest.api.API;
import utilities.rest.api.CookieUtil;
import utilities.rest.client.HttpClient;
import utilities.rest.client.HttpClientHandler;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
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
public class WebAPI implements API {
    private final HttpVersion httpVersion;
    private HttpClient httpClient;
    private HttpClientHandler handler;
    private final String scheme;
    private final ObjectMapper mapper;
    private final String gatewayHost;
    private final Integer webPort;
    private final Integer imagePort;
    private final Integer authPort;
    private final Integer recommenderPort;
    private final Integer persistencePort;
    private final HttpRequest request;

    public WebAPI(HttpVersion httpVersion, String scheme) {
        this.httpVersion = httpVersion;
        this.scheme = scheme;
        this.mapper = new ObjectMapper();
        this.gatewayHost = "gateway";
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
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(header.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String method = header.method().name();
        String path = queryStringDecoder.path();
        String cookieValue = header.headers().get(HttpHeaderNames.COOKIE);
        SessionData sessionData = CookieUtil.decodeCookie(cookieValue);

        // Select endpoint
        if (path.startsWith("/api/web")) {
            String subPath = path.substring("/api/web".length());
            switch (method) {
                case "GET":
                    switch (subPath) {
                        case "/isready":
                            return isReady();
                        case "/about":
                            return aboutView(sessionData);
                        case "/cartaction/addtocart":
                        case "/cartaction/removeproduct":
                            if (params.containsKey("productid")) {
                                String action = subPath.substring("/cartaction/".length());
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                return cartAction(sessionData, action, productId, null);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/cartaction/updatecartquantities":
                            if (params.containsKey("productid") && params.containsKey("quantity")) {
                                String action = subPath.substring("/cartaction/".length());
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                Long quantity = Long.parseLong(params.get("quantity").get(0));
                                return cartAction(sessionData, action, productId, quantity);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/cartaction/proceedtocheckout":
                            String action = subPath.substring("/cartaction/".length());
                            return cartAction(sessionData, action, null, null);
                        case "/cart":
                            return cartView(sessionData);
                        case "/category":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                Integer productQuantity = 20;
                                Integer page = 1;
                                if (params.containsKey("productquantity")) {
                                    productQuantity = Integer.parseInt(params.get("productquantity").get(0));
                                }
                                if (params.containsKey("page")) {
                                    page = Integer.parseInt(params.get("page").get(0));
                                }
                                return categoryView(sessionData, id, productQuantity, page);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/databaseaction":
                            if (params.containsKey("categories")
                                    && params.containsKey("products")
                                    && params.containsKey("users")
                                    && params.containsKey("orders")
                            ) {
                                Integer categories = Integer.parseInt(params.get("categories").get(0));
                                Integer products = Integer.parseInt(params.get("products").get(0));
                                Integer users = Integer.parseInt(params.get("users").get(0));
                                Integer orders = Integer.parseInt(params.get("orders").get(0));
                                return databaseAction(categories, products, users, orders);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/database":
                            return databaseView();
                        case "/error":
                            return errorView();
                        case "/index":
                            return indexView();
                        case "/login":
                            return loginView();
                        case "/order":
                            return orderView(sessionData);
                        case "/product":
                            if (params.containsKey("id")) {
                                return productView(sessionData, params.get("id").get(0));
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/profile":
                            return profileView(sessionData);
                    }
                case "POST":
                    switch (subPath) {
                        case "/loginaction":
                            return loginAction(sessionData, body);
                        case "/cartaction/confirm":
                            if (params.containsKey("totalpriceincents")) {
                                return confirmOrder(sessionData, body);
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
     * GET /ready
     *
     * @return Service status
     */
    private FullHttpResponse isReady() {
        return new DefaultFullHttpResponse(httpVersion, OK);
    }

    /**
     * GET /about
     *
     * Create web view "About us"
     *
     * @return About page view as JSON
     */
    private FullHttpResponse aboutView(SessionData sessionData) {
        // POST api/image/getWebImages
        String imageEndpoint = IMAGE_ENDPOINT + "/webimages";
        String authEndpoint = AUTH_ENDPOINT + "/isloggedin";
        try {
            request.setUri(imageEndpoint);
            request.setMethod(POST);
            Map<String, String> imageSizeMap = new HashMap<>();
            String imagePortraitSize = ImageSizePreset.PORTRAIT.getSize().toString();
            String imageLogoSize = ImageSizePreset.LOGO.getSize().toString();
            imageSizeMap.put("icon", imageLogoSize);
            imageSizeMap.put("andreBauer", imagePortraitSize);
            imageSizeMap.put("johannesGrohmann", imagePortraitSize);
            imageSizeMap.put("joakimKistowski", imagePortraitSize);
            imageSizeMap.put("simonEismann", imagePortraitSize);
            imageSizeMap.put("norbertSchmitt", imagePortraitSize);
            imageSizeMap.put("samuelKounev", imagePortraitSize);
            imageSizeMap.put("descartesLogo", imageLogoSize);
            String json = mapper.writeValueAsString(imageSizeMap);
            FullHttpRequest postRequest = new DefaultFullHttpRequest(
                    request.protocolVersion(),
                    request.method(),
                    imageEndpoint,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, imagePort, postRequest);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if (handler.response instanceof HttpContent httpImageContent) {
                ByteBuf imageBody = httpImageContent.content();
                byte[] jsonImageByte = new byte[imageBody.readableBytes()];
                imageBody.readBytes(jsonImageByte);
                Map<String, String> imageDataMap = mapper.readValue(
                        jsonImageByte,
                        new TypeReference<Map<String, String>>(){}
                );
                String storeIcon = imageDataMap.remove("icon");
                String descartesLogo = imageDataMap.remove("descartesLogo");
                String title = "TeaStore About Us";
                String descartesDescription = "We are part of the Descartes Research Group:";
                String description = "Our research is aimed at developing novel methods, ...";
                AboutPageView view = new AboutPageView(
                        storeIcon,
                        title,
                        imageDataMap,
                        descartesDescription,
                        descartesLogo,
                        description
                );
                request.setUri(authEndpoint);
                request.headers().add("Cookie", CookieUtil.encodeSessionData(sessionData));
                request.setMethod(GET);
                httpClient = new HttpClient(gatewayHost, authPort, request);
                handler = new HttpClientHandler();
                httpClient.sendRequest(handler);
                json = mapper.writeValueAsString(view);
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                        httpVersion,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
                if (handler.response instanceof HttpContent httpSessionDataContent) {
                    ByteBuf sessionDataBody = httpSessionDataContent.content();
                    byte[] jsonSessionDataByte = new byte[sessionDataBody.readableBytes()];
                    sessionDataBody.readBytes(jsonSessionDataByte);
                    SessionData newSessionData = mapper.readValue(jsonSessionDataByte, SessionData.class);
                    response.headers().set("Set-Cookie", CookieUtil.encodeSessionData(newSessionData));
                }
                return response;
            } else {
                return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /cartaction
     *
     * Handling all cart actions
     *
     * @return Page view depending on cart action
     */
    private FullHttpResponse cartAction(SessionData sessionData, String name, Long productId, Long quantity) {
        // POST /api/auth/cart/add?productid=
        String authEndpointAdd = AUTH_ENDPOINT + "/cart/add?productid=" + productId;
        // POST /api/auth/cart/remove?productid=
        String authEndpointRemove = AUTH_ENDPOINT + "/cart/remove?productid=" + productId;
        // PUT /api/auth/cart/update?productid=X&quantity=Y
        String authEndpointUpdate = AUTH_ENDPOINT + "/cart/update?productid=" + productId + "&quantity=" + quantity;
        // GET /api/auth/useractions/isloggedin
        String authEndpointCheck = AUTH_ENDPOINT + "/useractions/isloggedin";
        try {
            SessionData newSessionData = sessionData;
            request.headers().add("Cookie", CookieUtil.encodeSessionData(sessionData));
            switch (name) {
                case "addtocart":
                    request.setMethod(HttpMethod.POST);
                    request.setUri(authEndpointAdd);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if (handler.response instanceof HttpContent httpSessionDataContent) {
                        ByteBuf sessionDataBody = httpSessionDataContent.content();
                        byte[] jsonSessionDataByte = new byte[sessionDataBody.readableBytes()];
                        sessionDataBody.readBytes(jsonSessionDataByte);
                        newSessionData = mapper.readValue(jsonSessionDataByte, SessionData.class);
                    }
                case "removeproduct":
                    request.setMethod(HttpMethod.POST);
                    request.setUri(authEndpointRemove);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if (handler.response instanceof HttpContent httpSessionDataContent) {
                        ByteBuf sessionDataBody = httpSessionDataContent.content();
                        byte[] jsonSessionDataByte = new byte[sessionDataBody.readableBytes()];
                        sessionDataBody.readBytes(jsonSessionDataByte);
                        newSessionData = mapper.readValue(jsonSessionDataByte, SessionData.class);
                    }
                case "updatecartquantities":
                    request.setMethod(HttpMethod.PUT);
                    request.setUri(authEndpointUpdate);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if (handler.response instanceof HttpContent httpSessionDataContent) {
                        ByteBuf sessionDataBody = httpSessionDataContent.content();
                        byte[] jsonSessionDataByte = new byte[sessionDataBody.readableBytes()];
                        sessionDataBody.readBytes(jsonSessionDataByte);
                        newSessionData = mapper.readValue(jsonSessionDataByte, SessionData.class);
                    }
                case "proceedtocheckout":
                    request.setMethod(HttpMethod.GET);
                    request.setUri(authEndpointCheck);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if (handler.response instanceof HttpContent httpSessionDataContent) {
                        ByteBuf sessionDataBody = httpSessionDataContent.content();
                        byte[] jsonSessionDataByte = new byte[sessionDataBody.readableBytes()];
                        sessionDataBody.readBytes(jsonSessionDataByte);
                        newSessionData = mapper.readValue(jsonSessionDataByte, SessionData.class);
                    } else {
                        return loginView();
                    }
            }
            FullHttpResponse response = cartView(newSessionData);
            response.headers().set("Set-Cookie", CookieUtil.encodeSessionData(newSessionData));
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * POST /cartaction/confirm
     *
     * Confirm current order process
     *
     * @return Profile page view as JSON
     */
    private FullHttpResponse confirmOrder(SessionData sessionData, ByteBuf body) {
        // POST /api/auth/useractions/placeorder
        String authEndpointPlaceOrder = AUTH_ENDPOINT + "/useractions/placeorder";
        try {
            request.setMethod(HttpMethod.POST);
            request.headers().add("Cookie", CookieUtil.encodeSessionData(sessionData));
            // Create client and send request
            FullHttpRequest postRequest = new DefaultFullHttpRequest(
                    request.protocolVersion(),
                    request.method(),
                    authEndpointPlaceOrder,
                    body
            );
            httpClient = new HttpClient(gatewayHost, persistencePort, postRequest);
            handler = new HttpClientHandler();
            if (handler.response instanceof HttpContent httpSessionDataContent) {
                ByteBuf sessionDataBody = httpSessionDataContent.content();
                byte[] jsonSessionDataByte = new byte[sessionDataBody.readableBytes()];
                sessionDataBody.readBytes(jsonSessionDataByte);
                SessionData newSessionData = mapper.readValue(jsonSessionDataByte, SessionData.class);
                FullHttpResponse response = profileView(newSessionData);
                response.headers().set("Set-Cookie", CookieUtil.encodeSessionData(newSessionData));
                return response;
            } else {
                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /cart
     *
     * Create web view "Cart"
     *
     * @return Cart page view as JSON
     */
    private FullHttpResponse cartView(SessionData sessionData) {
        // GET 2x products & advertisements
        String persistenceEndpointProducts = PERSISTENCE_ENDPOINT + "/products"; // products
        // POST api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages"; // storeIcon
        String imageEndpointProduct = IMAGE_ENDPOINT + "/productimages"; // productImages for ads
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories"; // categoryList
        // GET /api/auth/useractions/isloggedin
        String authEndpoint = AUTH_ENDPOINT + "/useractions/isloggedin"; // isLoggedIn
        // POST /api/recommender/recommend
        String recommenderEndpoint = RECOMMENDER_ENDPOINT + "/recommend"; // productIds for advertisments
        try {
            // TODO: IMPLEMENT
            request.setUri(persistenceEndpointProducts);
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, persistencePort, request);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if (handler.response instanceof HttpContent httpContent) {
                // TODO: Replace with service calls
                long id = 1L;
                String removeProduct = "/api/web/cartaction/removeproduct?productId=" + id;
                List<CartItem> cartItems = new ArrayList<>();
                CartItem item = new CartItem(
                        id,
                        "Product 1",
                        "Product 1 description",
                        2,
                        100L,
                        200L,
                        removeProduct
                );
                cartItems.add(item);
                // TODO: Filter products with recommendations
                List<String> productImages = new ArrayList<String>();
                // TODO: other requests
                request.setUri(authEndpoint);
                httpClient = new HttpClient(gatewayHost, authPort, request);
                handler = new HttpClientHandler();
                httpClient.sendRequest(handler);
                String json = "{}";
                boolean isLoggedIn = false;
                if (handler.response instanceof HttpResponse response) {
                    // Check if user is logged in
                    isLoggedIn = response.status() == OK;
                }
                String updateCart = "/api/web/cartaction/updatecartquantities";
                String proceedToCheckout = "/api/web/cartaction/proceedtocheckout";
                CartPageView view = new CartPageView(
                        // TODO: ByteBuf imageData = httpContent.content();
                        "STOREICON",
                        "Shopping Cart",
                        // TODO
                        new ArrayList<>(),
                        cartItems,
                        // TODO
                        new ArrayList<>(),
                        /*
                        mapper.readValue(
                                getPersistenceCategories(),
                                new TypeReference<List<Category>>(){}
                        ),
                        cartItems,
                        mapper.readValue(
                                getRecommendations(),
                                new TypeReference<List<ProductView>>(){}
                        ), */
                        productImages,
                        updateCart,
                        proceedToCheckout
                );
                json = mapper.writeValueAsString(view);
                return new DefaultFullHttpResponse(
                        httpVersion,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
                return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /category
     *
     * Create web view "Category"
     *
     * @return Category page view as JSON
     */
    private FullHttpResponse categoryView(SessionData sessionData,
                                          Long id,
                                          Integer productQuantity,
                                          Integer page)
    {
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories"; // categoryList
        // GET api/persistence/products
        String persistenceEndpointProducts = PERSISTENCE_ENDPOINT + "/products"; // 2x products
        // GET api/image/getProductImages
        String imageEndpointProduct = IMAGE_ENDPOINT + "/productimages"; // productImages
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages"; // storeIcon
        try {
            // TODO: IMPLEMENT
            request.setUri(persistenceEndpointCategories);
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, persistencePort, request);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if (handler.response instanceof HttpContent httpContent) {
                // TODO: Replace with service calls
                String json = null;
                // TODO: other requests
                request.setUri(persistenceEndpointProducts);
                httpClient = new HttpClient(gatewayHost, authPort, request);
                handler = new HttpClientHandler();
                httpClient.sendRequest(handler);
                // String json = "{}";
                boolean isLoggedIn = false;
                if (handler.response instanceof HttpResponse response) {
                    // Check if user is logged in
                    isLoggedIn = response.status() == OK;
                }
                // json = mapper.writeValueAsString(view);
                return new DefaultFullHttpResponse(
                        httpVersion,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
                return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /databaseaction
     *
     * Generate database content
     *
     * @return Index page view as JSON
     */
    private FullHttpResponse databaseAction(int categories, Integer products, Integer users, Integer orders) {
        // GET api/persistence/generatedb
        String authEndpoint = PERSISTENCE_ENDPOINT + "/generatedb" +
                "?categories=" + categories + "&products=" + products +
                "&users=" + users + "&orders=" + orders;
        try {
            request.setUri(authEndpoint);
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, persistencePort, request);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if (handler.response instanceof HttpResponse response) {
                // And return to index view
                return indexView();
            } else {
                return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /database
     *
     * Create web view "Database"
     *
     * @return Database page view as JSON
     */
    private FullHttpResponse databaseView() {
        DatabasePageView view = new DatabasePageView(
                "STOREICON",
                "Setup the Database",
                5,
                100,
                100,
                5
        );
        try {
            String json = mapper.writeValueAsString(view);
            return new DefaultFullHttpResponse(
                    httpVersion,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /error
     *
     * Create web view "Error"
     *
     * @return Error page view as JSON
     */
    private FullHttpResponse errorView() {
        // TODO: Persistence, image and auth service calls
        ErrorPageView view = new ErrorPageView(
                "STOREICON",
                "Oops, something went wrong!",
                "ERRORIMAGE",
                "/api/web/index"
        );
        try {
            String json = mapper.writeValueAsString(view);
            return new DefaultFullHttpResponse(
                    httpVersion,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /index
     *
     * Create web view "Index"
     *
     * @return Index page view as JSON
     */
    private FullHttpResponse indexView() {
        // TODO: Persistence, image and auth service calls
        try {
            IndexPageView view = new IndexPageView(
                    "STOREICON",
                    "",
                    new ArrayList<>(),
                    // TODO
                    /*
                    mapper.readValue(
                            getPersistenceCategories(),
                            new TypeReference<List<Category>>(){}
                    ), */
                    "LARGESTOREICON"
            );
            String json = mapper.writeValueAsString(view);
            return new DefaultFullHttpResponse(
                    httpVersion,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * POST /loginaction
     *
     * User login or logout
     *
     * @return Index page view as JSON
     */
    private FullHttpResponse loginAction(SessionData sessionData, ByteBuf body) {
        LoginAction action = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        //
        String authEndpointLogin = AUTH_ENDPOINT + "/useractions/login"; // POST
        String authEndpointLogout = AUTH_ENDPOINT + "/useractions/logout"; // POST
        try {
            action = mapper.readValue(jsonByte, LoginAction.class);
            request.setMethod(HttpMethod.POST);
            switch(action.name()) {
                case "login":
                    // Create client and send request
                    FullHttpRequest postRequest = new DefaultFullHttpRequest(
                            request.protocolVersion(),
                            request.method(),
                            authEndpointLogin,
                            body
                    );
                    httpClient = new HttpClient(gatewayHost, persistencePort, postRequest);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if (handler.response instanceof HttpResponse response) {
                        if (response.status() != OK) {
                            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                        }
                        // TODO: Save login state?
                    }
                case "logout":
                    request.setUri(authEndpointLogout);
                    // Create client and send request
                    httpClient = new HttpClient(gatewayHost, persistencePort, request);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if (handler.response instanceof HttpResponse response) {
                        if (response.status() != OK) {
                            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                        }
                    }
            }
            // And return cart view
            return indexView(); // TODO: Return to previous site
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /login
     *
     * Create web view "Login"
     *
     * @return Login page view as JSON
     */
    private FullHttpResponse loginView() {
        // TODO: Persistence, image and auth service calls
        try {
            LoginPageView view = new LoginPageView(
                    "STOREICON",
                    "Login",
                    new ArrayList<>(),
//                    mapper.readValue(
//                            getPersistenceCategories(),
//                            new TypeReference<List<Category>>(){}
//                    ),
                    "Please enter your username and password.",
                    "",
                    "",
                    "/api/web/loginaction/login",
                    "referer"
            );
            String json = mapper.writeValueAsString(view);
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
     * GET /order
     *
     * Create web view "Order"
     *
     * @return Order page view as JSON
     */
    private FullHttpResponse orderView(SessionData sessionData) {
        // TODO: Persistence, image and auth service calls
        try {
            long id = 1L;
            Order order = new Order(
                    id,
                    id + 1L,
                    "2021-03-23 13:17:00",
                    100000L,
                    "John Snow",
                    "Winterfell",
                    "1111 The North, Westeros",
                    "Visa",
                    "31459265359",
                    "12/2025"
            );
            OrderPageView view = new OrderPageView(
                    "STOREICON",
                    "Order",
                    new ArrayList<>(),
//                    mapper.readValue(
//                            getPersistenceCategories(),
//                            new TypeReference<List<Category>>(){}
//                    ),
                    order.addressName().split("")[0],
                    order.addressName().split("")[1],
                    order.address1(),
                    order.address2(),
                    order.creditCardCompany(),
                    order.creditCardNumber(),
                    order.creditCardExpiryDate(),
                    "/api/web/cartaction/proceedtocheckout"
            );
            String json = mapper.writeValueAsString(view);
            return new DefaultFullHttpResponse(
                    httpVersion,
                    OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /product
     *
     * Create web view "Product"
     *
     * @return Product page view as JSON
     */
    private FullHttpResponse productView(SessionData sessionData, String productId) {
        // TODO: Persistence, image and auth service calls
        try {
            long id = 4L;
            String addToCart = "/api/web/cartaction/addtocart?productId=" + id;
            ProductView product = new ProductView(
                    id,
                    1L,
                    "PRODUCTFOUR",
                    "Product 4",
                    150L,
                    "Product 4 description",
                    addToCart
            );
            ProductPageView view = new ProductPageView(
                    "STOREICON",
                    "Order",
                    // TODO
                    new ArrayList<>(),
                    product,
                    new ArrayList<>()
                    /*
                    mapper.readValue(
                            getPersistenceCategories(),
                            new TypeReference<List<Category>>(){}
                    ),
                    product,
                    mapper.readValue(
                            getRecommendations(),
                            new TypeReference<List<ProductView>>(){}
                    ) */
            );
            String json = mapper.writeValueAsString(view);
            return new DefaultFullHttpResponse(
                    httpVersion,
                    OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /profile
     *
     * Create web view "Profile"
     *
     * @return Profile page view as JSON
     */
    private FullHttpResponse profileView(SessionData sessionData) {
        // TODO: Persistence, image and auth service calls
        try {
            long id = 1L;
            String addToCart = "/api/web/cartaction/addtocart?productId=" + id;
            User user = new User(
                    id,
                    "jsnow",
                    "secret",
                    "John Snow",
                    "jsnow@teastorev2.com"
            );
            ProfilePageView view = new ProfilePageView(
                    "STOREICON",
                    "Order",
                    // TODO
                    new ArrayList<>(),
                    user,
                    new ArrayList<>()
                    /*
                    mapper.readValue(
                            getPersistenceCategories(),
                            new TypeReference<List<Category>>(){}
                    ),
                    user,
                    mapper.readValue(
                            getPersistencePreviousOrder(),
                            new TypeReference<List<PreviousOrder>>(){}
                    ) */
            );
            String json = mapper.writeValueAsString(view);
            return new DefaultFullHttpResponse(
                    httpVersion,
                    OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }
    // Status view is part of the API gateway
}
