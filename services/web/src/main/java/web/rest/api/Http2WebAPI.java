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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.util.CharsetUtil;
import utilities.datamodel.*;
import utilities.enumeration.ImageSizePreset;
import utilities.rest.api.API;
import utilities.rest.api.CookieUtil;
import utilities.rest.api.Http2Response;
import utilities.rest.client.Http1Client;
import utilities.rest.client.Http1ClientHandler;
import utilities.rest.client.Http2Client;
import utilities.rest.client.Http2ClientStreamFrameHandler;

import java.io.IOException;
import java.util.*;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * API for web service
 * /api/web
 *
 * @author Philipp Backes
 */
public class Http2WebAPI implements API {
    private Http2Client httpClient;
    private Http2ClientStreamFrameHandler frameHandler;
    private final ObjectMapper mapper;
    private final String gatewayHost;
    private final Integer imagePort;
    private final Integer authPort;
    private final Integer persistencePort;
    private final Integer recommenderPort;
    private Http2Headers http2Header;
    private Http2DataFrame http2DataFrame;

    public Http2WebAPI(String gatewayHost, Integer gatewayPort) {
        mapper = new ObjectMapper();
        if(gatewayHost.isEmpty()) {
            this.gatewayHost = "localhost";
            authPort = API.DEFAULT_AUTH_PORT;
            imagePort = API.DEFAULT_IMAGE_PORT;
            persistencePort = API.DEFAULT_PERSISTENCE_PORT;
            recommenderPort = API.DEFAULT_RECOMMENDER_PORT;
        } else {
            this.gatewayHost = gatewayHost;
            imagePort = gatewayPort;
            authPort = gatewayPort;
            persistencePort = gatewayPort;
            recommenderPort = gatewayPort;
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
                                return Http2Response.badRequestResponse();
                            }
                        case "/cartaction/updatecartquantities":
                            if (params.containsKey("productid") && params.containsKey("quantity")) {
                                String action = subPath.substring("/cartaction/".length());
                                Long productId = Long.parseLong(params.get("productid").get(0));
                                Long quantity = Long.parseLong(params.get("quantity").get(0));
                                return cartAction(sessionData, action, productId, quantity);
                            } else {
                                return Http2Response.badRequestResponse();
                            }
                        case "/cartaction/proceedtocheckout":
                            String action = subPath.substring("/cartaction/".length());
                            return cartAction(sessionData, action, null, null);
                        case "/cart":
                            return cartView(sessionData);
                        case "/category":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                int productQuantity = 20;
                                int page = 1;
                                if (params.containsKey("productquantity")) {
                                    productQuantity = Integer.parseInt(params.get("productquantity").get(0));
                                }
                                if (params.containsKey("page")) {
                                    page = Integer.parseInt(params.get("page").get(0));
                                }
                                return categoryView(sessionData, id, productQuantity, page);
                            } else {
                                return Http2Response.badRequestResponse();
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
                                return databaseAction(sessionData, categories, products, users, orders);
                            } else {
                                return Http2Response.badRequestResponse();
                            }
                        case "/database":
                            return databaseView();
                        case "/error":
                            return errorView(sessionData);
                        case "/index":
                            return indexView(sessionData);
                        case "/login":
                            return loginView(sessionData);
                        case "/order":
                            return orderView(sessionData);
                        case "/product":
                            if (params.containsKey("id")) {
                                return productView(sessionData, Long.parseLong(params.get("id").get(0)));
                            } else {
                                return Http2Response.badRequestResponse();
                            }
                        case "/profile":
                            return profileView(sessionData);
                    }
                case "POST":
                    switch (subPath) {
                        case "/logioaction":
                            if (params.containsKey("username") && params.containsKey("password")) {
                                String username = params.get("username").get(0);
                                String password = params.get("password").get(0);
                                return logioAction(sessionData, "login", username, password);
                            } else {
                                return logioAction(sessionData, "logout", null, null);
                            }
                        case "/cartaction/confirm":
                            return confirmOrder(sessionData, body);
                    }
                default:
                    break;
            }
        }
        return Http2Response.notFoundResponse();
    }

    //
    // Helper methods
    //

    private Map<String, String> getWebImages(
            String imageEndpoint,
            Map<String, String> imageSizeMap
    ) throws IOException {
        Map<String, String> imageWebDataMap = new HashMap<>();
        String json = mapper.writeValueAsString(imageSizeMap);
        ByteBuf postBody = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);
        http2Header.method(POST.asciiName()).path(imageEndpoint);
        http2Header.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(postBody.readableBytes()));
        http2DataFrame = new DefaultHttp2DataFrame(postBody);
        // Create client and send request
        httpClient = new Http2Client(gatewayHost, imagePort, http2Header, http2DataFrame);
        frameHandler = new Http2ClientStreamFrameHandler();
        httpClient.sendRequest(frameHandler);
        if (!frameHandler.jsonContent.isEmpty()) {
            imageWebDataMap = mapper.readValue(
                    frameHandler.jsonContent,
                    new TypeReference<Map<String, String>>(){}
            );
        }
        return imageWebDataMap;
    }

    private Map<Long, String> getProductImages(
            String imageEndpoint,
            Map<Long, String> imageSizeMap
    ) throws IOException {
        Map<Long, String> imageProductDataMap = new HashMap<>();
        String json = mapper.writeValueAsString(imageSizeMap);
        ByteBuf postBody = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);
        http2Header.method(POST.asciiName()).path(imageEndpoint);
        http2Header.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(postBody.readableBytes()));
        http2DataFrame = new DefaultHttp2DataFrame(postBody);
        // Create client and send request
        httpClient = new Http2Client(gatewayHost, imagePort, http2Header, http2DataFrame);
        frameHandler = new Http2ClientStreamFrameHandler();
        httpClient.sendRequest(frameHandler);
        if (!frameHandler.jsonContent.isEmpty()) {
            imageProductDataMap = mapper.readValue(
                    frameHandler.jsonContent,
                    new TypeReference<Map<Long, String>>(){}
            );
        }
        return imageProductDataMap;
    }

    private List<Category> getCategories(String persistenceEndpointCategories) throws IOException {
        List<Category> categories = new ArrayList<>();
        http2Header.method(GET.asciiName()).path(persistenceEndpointCategories);
        // Create client and send request
        httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
        frameHandler = new Http2ClientStreamFrameHandler();
        httpClient.sendRequest(frameHandler);
        if (!frameHandler.jsonContent.isEmpty()) {
            categories = mapper.readValue(
                    frameHandler.jsonContent,
                    new TypeReference<List<Category>>(){}
            );
        }
        return categories;
    }

    private SessionData checkLogin(String authEndpoint, SessionData sessionData) throws IOException {
        SessionData newSessionData = null;
        http2Header.method(GET.asciiName()).path(authEndpoint);
        http2Header.setObject(HttpHeaderNames.COOKIE, CookieUtil.encodeSessionData(sessionData, gatewayHost));
        // Create client and send request
        httpClient = new Http2Client(gatewayHost, authPort, http2Header, null);
        frameHandler = new Http2ClientStreamFrameHandler();
        httpClient.sendRequest(frameHandler);
        if (!frameHandler.jsonContent.isEmpty()) {
            newSessionData = mapper.readValue(frameHandler.jsonContent, SessionData.class);
        }
        return newSessionData;
    }

    //
    // Endpoint methods
    //

    /**
     * GET /ready
     *
     * @return Service status
     */
    private Http2Response isReady() {
        return Http2Response.okResponse();
    }

    /**
     * GET /about
     *
     * Create web view "About us"
     *
     * @return About page view as JSON
     */
    private Http2Response aboutView(SessionData sessionData) {
        // POST api/image/getWebImages
        String imageEndpoint = IMAGE_ENDPOINT + "/webimages";
        String authEndpoint = AUTH_ENDPOINT + "/isloggedin";
        try {
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
            Map<String, String> imageDataMap = getWebImages(imageEndpoint, imageSizeMap);
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
            http2Header.method(GET.asciiName()).path(authEndpoint);
            http2Header.setObject(HttpHeaderNames.COOKIE, CookieUtil.encodeSessionData(sessionData, gatewayHost));
            // Create client and send request
            httpClient = new Http2Client(gatewayHost, authPort, http2Header, null);
            frameHandler = new Http2ClientStreamFrameHandler();
            httpClient.sendRequest(frameHandler);
            String json = mapper.writeValueAsString(view);
            if (!frameHandler.jsonContent.isEmpty()) {
                SessionData newSessionData = mapper.readValue(frameHandler.jsonContent, SessionData.class);
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length())
                                .set(
                                        HttpHeaderNames.SET_COOKIE,
                                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                                ),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
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
     * GET /cartaction
     *
     * Handling all cart actions
     *
     * @return Page view depending on cart action
     */
    private Http2Response cartAction(SessionData sessionData, String name, Long productId, Long quantity) {
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
            http2Header.setObject(HttpHeaderNames.COOKIE, CookieUtil.encodeSessionData(sessionData, gatewayHost));
            Http2Response response = null;
            switch (name) {
                case "addtocart":
                    http2Header.method(POST.asciiName()).path(authEndpointAdd);
                    // Create client and send request
                    httpClient = new Http2Client(gatewayHost, authPort, http2Header, null);
                    frameHandler = new Http2ClientStreamFrameHandler();
                    httpClient.sendRequest(frameHandler);
                    if (!frameHandler.jsonContent.isEmpty()) {
                        newSessionData = mapper.readValue(frameHandler.jsonContent, SessionData.class);
                        response = cartView(newSessionData);
                        break;
                    } else {
                        return Http2Response.internalServerErrorResponse();
                    }
                case "removeproduct":
                    http2Header.method(POST.asciiName()).path(authEndpointRemove);
                    // Create client and send request
                    httpClient = new Http2Client(gatewayHost, authPort, http2Header, null);
                    frameHandler = new Http2ClientStreamFrameHandler();
                    httpClient.sendRequest(frameHandler);
                    if (!frameHandler.jsonContent.isEmpty()) {
                        newSessionData = mapper.readValue(frameHandler.jsonContent, SessionData.class);
                        response = cartView(newSessionData);
                        break;
                    } else {
                        return Http2Response.internalServerErrorResponse();
                    }
                case "updatecartquantities":
                    http2Header.method(PUT.asciiName()).path(authEndpointUpdate);
                    // Create client and send request
                    httpClient = new Http2Client(gatewayHost, authPort, http2Header, null);
                    frameHandler = new Http2ClientStreamFrameHandler();
                    httpClient.sendRequest(frameHandler);
                    if (!frameHandler.jsonContent.isEmpty()) {
                        newSessionData = mapper.readValue(frameHandler.jsonContent, SessionData.class);
                        response = cartView(newSessionData);
                        break;
                    } else {
                        return Http2Response.internalServerErrorResponse();
                    }
                case "proceedtocheckout":
                    http2Header.method(GET.asciiName()).path(authEndpointCheck);
                    // Create client and send request
                    httpClient = new Http2Client(gatewayHost, authPort, http2Header, null);
                    frameHandler = new Http2ClientStreamFrameHandler();
                    httpClient.sendRequest(frameHandler);
                    if (!frameHandler.jsonContent.isEmpty()) {
                        newSessionData = mapper.readValue(frameHandler.jsonContent, SessionData.class);
                        response = orderView(newSessionData);
                        break;
                    } else {
                        return loginView(sessionData);
                    }
            }
            response.headers().set(
                    HttpHeaderNames.SET_COOKIE,
                    CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
            );
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
    }

    /**
     * POST /cartaction/confirm
     *
     * Confirm current order process
     *
     * @return Profile page view as JSON
     */
    private Http2Response confirmOrder(SessionData sessionData, ByteBuf body) {
        // POST /api/auth/useractions/placeorder
        String authEndpointPlaceOrder = AUTH_ENDPOINT + "/useractions/placeorder";
        try {
            http2Header.method(POST.asciiName()).path(authEndpointPlaceOrder);
            http2Header.setObject(HttpHeaderNames.COOKIE, CookieUtil.encodeSessionData(sessionData, gatewayHost));
            http2Header.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(body.readableBytes()));
            http2DataFrame = new DefaultHttp2DataFrame(body);
            // Create client and send request
            httpClient = new Http2Client(gatewayHost, authPort, http2Header, http2DataFrame);
            frameHandler = new Http2ClientStreamFrameHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                SessionData newSessionData = mapper.readValue(frameHandler.jsonContent, SessionData.class);
                Http2Response response = profileView(newSessionData);
                response.headers().set(
                        HttpHeaderNames.SET_COOKIE,
                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                );
                return response;
            } else {
                return Http2Response.badRequestResponse();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
    }

    /**
     * GET /cart
     *
     * Create web view "Cart"
     *
     * @return Cart page view as JSON
     */
    private Http2Response cartView(SessionData sessionData) {
        // GET products & advertisements
        String persistenceEndpointProducts = PERSISTENCE_ENDPOINT + "/products"; // products
        // POST api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages"; // storeIcon
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories?start=-1&max=-1"; // categoryList
        // POST /api/recommender/recommend
        String recommenderEndpoint = RECOMMENDER_ENDPOINT + "/recommend"; // productIds for advertisements
        // POST api/image/getProductImages
        String imageEndpointProduct = IMAGE_ENDPOINT + "/productimages"; // productImages for ads
        // GET /api/auth/useractions/isloggedin
        String authEndpoint = AUTH_ENDPOINT + "/useractions/isloggedin"; // isLoggedIn
        try {
            // Get products
            List<OrderItem> orderItems = new ArrayList<>();
            ArrayList<Long> ids = new ArrayList<>();
            if (sessionData.orderItems() != null) {
                for (OrderItem orderItem : sessionData.orderItems()) {
                    orderItems.add(orderItem);
                    ids.add(orderItem.productId());
                }
            }
            HashMap<Long, Product> products = new HashMap<>();
            for (Long id : ids) {
                http2Header.method(GET.asciiName()).path(persistenceEndpointProducts + "?id=" + id);
                // Create client and send request
                httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
                frameHandler = new Http2ClientStreamFrameHandler();
                httpClient.sendRequest(frameHandler);
                if (!frameHandler.jsonContent.isEmpty()) {
                    Product product = mapper.readValue(frameHandler.jsonContent, Product.class);
                    products.put(product.id(), product);
                }
            }
            // Get store icon
            Map<String, String> webImageSizeMap = new HashMap<>();
            String imageIconSize = ImageSizePreset.ICON.getSize().toString();
            webImageSizeMap.put("icon", imageIconSize);
            Map<String, String> webImageDataMap = getWebImages(imageEndpointWeb, webImageSizeMap);
            // Get categories
            List<Category> categories = getCategories(persistenceEndpointCategories);
            // Create cart items
            List<CartItem> cartItems = new ArrayList<>();
            for (OrderItem item : orderItems) {
                Long productId = item.productId();
                cartItems.add(new CartItem(
                        productId,
                        products.get(productId).name(),
                        products.get(productId).description(),
                        item.quantity(),
                        item.unitPriceInCents(),
                        item.quantity() * item.unitPriceInCents(),
                        "/api/web/cartaction/removeproduct?productId=" + productId
                ));
            }
            // Create product view with recommendations = advertisements
            List<ProductView> advertisements = new ArrayList<>();
            List<Product> recommendedProducts = new ArrayList<>();
            List<Long> productIds = new ArrayList<>();
            // Recommendations works only with user id
            if(sessionData.userId() != null) {
                String orderItemsJson = mapper.writeValueAsString(orderItems);
                ByteBuf postOrderItemsBody = Unpooled.copiedBuffer(orderItemsJson, CharsetUtil.UTF_8);
                http2Header.method(POST.asciiName()).path(recommenderEndpoint + "?userid=" + sessionData.userId());
                http2Header.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(postOrderItemsBody.readableBytes()));
                http2DataFrame = new DefaultHttp2DataFrame(postOrderItemsBody);
                // Create client and send request
                httpClient = new Http2Client(gatewayHost, recommenderPort, http2Header, http2DataFrame);
                frameHandler = new Http2ClientStreamFrameHandler();
                httpClient.sendRequest(frameHandler);
                if (!frameHandler.jsonContent.isEmpty()) {
                    productIds = mapper.readValue(
                            frameHandler.jsonContent,
                            new TypeReference<List<Long>>(){}
                    );
                }
            }
            // Get product images
            Map<Long, String> productImageSizeMap = new HashMap<>();
            String imageProductPreviewSize = ImageSizePreset.PREVIEW.getSize().toString();
            // Get recommended products
            for (Long productId : productIds) {
                productImageSizeMap.put(productId, imageProductPreviewSize);
                //
                http2Header.method(GET.asciiName()).path(persistenceEndpointProducts + "?id=" + productId);
                // Create client and send request
                httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
                frameHandler = new Http2ClientStreamFrameHandler();
                httpClient.sendRequest(frameHandler);
                if (!frameHandler.jsonContent.isEmpty()) {
                    recommendedProducts.add(mapper.readValue(frameHandler.jsonContent, Product.class));
                }
            }
            Map<Long, String> productImageDataMap = getProductImages(imageEndpointProduct, productImageSizeMap);
            // Create product views
            for (Product product : recommendedProducts) {
                Long productId = product.id();
                advertisements.add(
                        new ProductView(
                                productId,
                                product.categoryId(),
                                productImageDataMap.get(productId),
                                product.name(),
                                product.listPriceInCents(),
                                product.description(),
                                "/api/web/cartaction/addtocart?productId=" + product.id()
                        )
                );
            }
            String title = "TeaStore Cart";
            String updateCart = "/api/web/cartaction/updatecartquantities";
            String proceedToCheckout = "/api/web/cartaction/proceedtocheckout";
            CartPageView view = new CartPageView(
                    webImageDataMap.get("icon"),
                    title,
                    categories,
                    cartItems,
                    advertisements,
                    updateCart,
                    proceedToCheckout
            );
            // Check login
            SessionData newSessionData = checkLogin(authEndpoint, sessionData);
            String json = mapper.writeValueAsString(view);
            if (newSessionData != null) {
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length())
                                .set(
                                        HttpHeaderNames.SET_COOKIE,
                                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                                ),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
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
     * GET /category
     *
     * Create web view "Category"
     *
     * @return Category page view as JSON
     */
    private Http2Response categoryView(SessionData sessionData,
                                          Long id,
                                          Integer productQuantity,
                                          Integer page)
    {
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages"; // storeIcon
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories";
        // GET api/persistence/products
        String persistenceEndpointProducts = PERSISTENCE_ENDPOINT + "/products";
        String persistenceEndpointCategoryProducts = PERSISTENCE_ENDPOINT + "/products?category=" + id;
        // GET api/image/getProductImages
        String imageEndpointProduct = IMAGE_ENDPOINT + "/productimages"; // productImages
        // GET /api/auth/useractions/isloggedin
        String authEndpoint = AUTH_ENDPOINT + "/useractions/isloggedin"; // isLoggedIn
        try {
            // Get store icon
            Map<String, String> webImageSizeMap = new HashMap<>();
            String imageIconSize = ImageSizePreset.ICON.getSize().toString();
            webImageSizeMap.put("icon", imageIconSize);
            Map<String, String> webImageDataMap = getWebImages(imageEndpointWeb, webImageSizeMap);
            // Get categories
            List<Category> categories = getCategories(persistenceEndpointCategories);
            // Get number of all products
            int products = 0;
            http2Header.method(GET.asciiName()).path(persistenceEndpointProducts);
            // Create client and send request
            httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
            frameHandler = new Http2ClientStreamFrameHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                products = mapper.readValue(
                        frameHandler.jsonContent,
                        new TypeReference<List<Product>>(){}
                ).size();
            }
            // Check page number
            int maxPages = (int) Math.ceil(((double) products) / productQuantity);
            if (maxPages <= page) {
                page = maxPages;
            }
            // Get products for this category
            persistenceEndpointCategoryProducts += "&start=" +
                    (page - 1) * productQuantity + "&max=" + productQuantity;
            List<Product> productList = new ArrayList<>();
            http2Header.method(GET.asciiName()).path(persistenceEndpointCategoryProducts);
            // Create client and send request
            httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
            frameHandler = new Http2ClientStreamFrameHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                productList = mapper.readValue(
                        frameHandler.jsonContent,
                        new TypeReference<List<Product>>(){}
                );
            }
            // Get product images
            Map<Long, String> productImageSizeMap = new HashMap<>();
            String imageProductCategorySize = ImageSizePreset.ICON.getSize().toString();
            for (Product product : productList) {
                productImageSizeMap.put(product.id(), imageProductCategorySize);
            }
            Map<Long, String> productImageDataMap = getProductImages(imageEndpointProduct, productImageSizeMap);
            // Create productViews
            List<ProductView> productViews = new ArrayList<>();
            for (Product product : productList) {
                Long productId = product.id();
                productViews.add(
                        new ProductView(
                                productId,
                                product.categoryId(),
                                productImageDataMap.get(productId),
                                product.name(),
                                product.listPriceInCents(),
                                product.description(),
                                "/api/web/cartaction/addtocart?productId=" + product.id()
                        )
                );
            }
            // Create category page view
            String title = "TeaStore Categorie " + categories.get(id.intValue()).name();
            CategoryPageView view = new CategoryPageView(
                    webImageDataMap.get("icon"),
                    title,
                    categories,
                    productViews,
                    page,
                    productQuantity
            );
            // Check login
            SessionData newSessionData = checkLogin(authEndpoint, sessionData);
            String json = mapper.writeValueAsString(view);
            if (newSessionData != null) {
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length())
                                .set(
                                        HttpHeaderNames.SET_COOKIE,
                                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                                ),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
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
     * GET /databaseaction
     *
     * Generate database content
     *
     * @return Index page view as JSON
     */
    private Http2Response databaseAction(SessionData sessionData, Integer categories, Integer products, Integer users, Integer orders) {
        // GET api/persistence/generatedb
        String persistenceEndpoint = PERSISTENCE_ENDPOINT + "/generatedb" +
                "?categories=" + categories + "&products=" + products +
                "&users=" + users + "&orders=" + orders;
        try {
            http2Header.method(GET.asciiName()).path(persistenceEndpoint);
            // Create client and send request
            httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
            frameHandler = new Http2ClientStreamFrameHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                // And return to index view
                return indexView(sessionData);
            } else {
                return Http2Response.internalServerErrorResponse();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
    }

    /**
     * GET /database
     *
     * Create web view "Database"
     *
     * @return Database page view as JSON
     */
    private Http2Response databaseView() {
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages"; // storeIcon
        try {
            // Get store icon
            Map<String, String> webImageSizeMap = new HashMap<>();
            String imageIconSize = ImageSizePreset.ICON.getSize().toString();
            webImageSizeMap.put("icon", imageIconSize);
            Map<String, String> webImageDataMap = getWebImages(imageEndpointWeb, webImageSizeMap);
            // Create database page view
            DatabasePageView view = new DatabasePageView(
                    webImageDataMap.get("icon"),
                    "Setup the Database",
                    5,
                    100,
                    100,
                    5
            );
            String json = mapper.writeValueAsString(view);
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
     * GET /error
     *
     * Create web view "Error"
     *
     * @return Error page view as JSON
     */
    private Http2Response errorView(SessionData sessionData) {
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages"; // storeIcon
        // GET /api/auth/useractions/isloggedin
        String authEndpoint = AUTH_ENDPOINT + "/useractions/isloggedin";
        try {
            // Get store icon
            Map<String, String> webImageSizeMap = new HashMap<>();
            String imageIconSize = ImageSizePreset.ICON.getSize().toString();
            String imageErrorSize = ImageSizePreset.ERROR.getSize().toString();
            webImageSizeMap.put("icon", imageIconSize);
            webImageSizeMap.put("error", imageErrorSize);
            Map<String, String> webImageDataMap = getWebImages(imageEndpointWeb, webImageSizeMap);
            // Create error page view
            ErrorPageView view = new ErrorPageView(
                    webImageDataMap.get("icon"),
                    "Oops, something went wrong!",
                    webImageDataMap.get("error"),
                    "/api/web/index"
            );
            // Check login
            SessionData newSessionData = checkLogin(authEndpoint, sessionData);
            String json = mapper.writeValueAsString(view);
            if (newSessionData != null) {
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length())
                                .set(
                                        HttpHeaderNames.SET_COOKIE,
                                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                                ),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
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
     * GET /index
     *
     * Create web view "Index"
     *
     * @return Index page view as JSON
     */
    private Http2Response indexView(SessionData sessionData) {
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages";
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories?start=-1&max=-1";
        // GET /api/auth/useractions/isloggedin
        String authEndpoint = AUTH_ENDPOINT + "/useractions/isloggedin";
        try {
            // Get store icon
            Map<String, String> webImageIconSizeMap = new HashMap<>();
            Map<String, String> webImageIndexSizeMap = new HashMap<>();
            String imageIconSize = ImageSizePreset.ICON.getSize().toString();
            String imageIndexSize = ImageSizePreset.INDEX.getSize().toString();
            webImageIconSizeMap.put("icon", imageIconSize);
            webImageIndexSizeMap.put("icon", imageIndexSize);
            Map<String, String> webImageIconDataMap = getWebImages(imageEndpointWeb, webImageIconSizeMap);
            Map<String, String> webImageIndexDataMap = getWebImages(imageEndpointWeb, webImageIndexSizeMap);
            // Get categories
            List<Category> categories = getCategories(persistenceEndpointCategories);
            // Create index page view
            IndexPageView view = new IndexPageView(
                    webImageIconDataMap.get("icon"),
                    "TeaStore Home",
                    categories,
                    webImageIndexDataMap.get("icon")
            );
            // Check login
            SessionData newSessionData = checkLogin(authEndpoint, sessionData);
            String json = mapper.writeValueAsString(view);
            if (newSessionData != null) {
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length())
                                .set(
                                        HttpHeaderNames.SET_COOKIE,
                                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                                ),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
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
     * POST /logioaction
     *
     * User login or logout
     *
     * @return Profile or index page view as JSON
     */
    private Http2Response logioAction(SessionData sessionData, String action, String username, String password) {
        // POST api/auth/useractions/login?name=
        String authEndpointLogin = AUTH_ENDPOINT + "/useractions/login?name=";
        // POST api/auth/useractions/logout
        String authEndpointLogout = AUTH_ENDPOINT + "/useractions/logout";
        try {
            SessionData newSessionData = null;
            http2Header.method(POST.asciiName());
            http2Header.setObject(HttpHeaderNames.COOKIE, CookieUtil.encodeSessionData(sessionData, gatewayHost));
            switch (action) {
                case "login":
                    authEndpointLogin += username + "&password=" + password;
                    http2Header.path(authEndpointLogin);
                    // Create client and send request
                    httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
                    frameHandler = new Http2ClientStreamFrameHandler();
                    httpClient.sendRequest(frameHandler);
                    if (!frameHandler.jsonContent.isEmpty()) {
                        newSessionData = mapper.readValue(frameHandler.jsonContent, SessionData.class);
                    }
                    return profileView(newSessionData);
                case "logout":
                    http2Header.path(authEndpointLogout);
                    // Create client and send request
                    httpClient = new Http2Client(gatewayHost, authPort, http2Header, null);
                    frameHandler = new Http2ClientStreamFrameHandler();
                    httpClient.sendRequest(frameHandler);
                    if (!frameHandler.jsonContent.isEmpty()) {
                        newSessionData = mapper.readValue(frameHandler.jsonContent, SessionData.class);
                    }
                    return indexView(newSessionData);
            }
            return Http2Response.badRequestResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
    }

    /**
     * GET /login
     *
     * Create web view "Login"
     *
     * @return Login page view as JSON
     */
    private Http2Response loginView(SessionData sessionData) {
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages";
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories?start=-1&max=-1";
        // GET /api/auth/useractions/isloggedin
        String authEndpoint = AUTH_ENDPOINT + "/useractions/isloggedin";
        try {
            // Get store icon
            Map<String, String> webImageSizeMap = new HashMap<>();
            String imageIconSize = ImageSizePreset.ICON.getSize().toString();
            webImageSizeMap.put("icon", imageIconSize);
            Map<String, String> webImageDataMap = getWebImages(imageEndpointWeb, webImageSizeMap);
            // Get categories
            List<Category> categories = getCategories(persistenceEndpointCategories);
            // Create login page view
            LoginPageView view = new LoginPageView(
                    webImageDataMap.get("icon"),
                    "Login",
                    categories,
                    "Please enter your username and password.",
                    "",
                    "",
                    "/api/web/loginaction/login?name=USERNAME&password=PASSWORD"
            );
            // Check login
            SessionData newSessionData = checkLogin(authEndpoint, sessionData);
            String json = mapper.writeValueAsString(view);
            if (newSessionData != null) {
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length())
                                .set(
                                        HttpHeaderNames.SET_COOKIE,
                                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                                ),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
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
     * GET /order
     *
     * Create web view "Order"
     *
     * @return Order page view as JSON
     */
    private Http2Response orderView(SessionData sessionData) {
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages"; // storeIcon
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories?start=-1&max=-1";
        // GET /api/auth/useractions/isloggedin
        String authEndpoint = AUTH_ENDPOINT + "/useractions/isloggedin";
        try {
            // Get store icon
            Map<String, String> webImageSizeMap = new HashMap<>();
            String imageIconSize = ImageSizePreset.ICON.getSize().toString();
            webImageSizeMap.put("icon", imageIconSize);
            Map<String, String> webImageDataMap = getWebImages(imageEndpointWeb, webImageSizeMap);
            // Get categories
            List<Category> categories = getCategories(persistenceEndpointCategories);
            // Get order page view
            OrderPageView view = new OrderPageView(
                    webImageDataMap.get("icon"),
                    "TeaStore Order",
                    categories,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "/api/web/cartaction/proceedtocheckout"
            );
            // Check login
            SessionData newSessionData = checkLogin(authEndpoint, sessionData);
            String json = mapper.writeValueAsString(view);
            if (newSessionData != null) {
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length())
                                .set(
                                        HttpHeaderNames.SET_COOKIE,
                                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                                ),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
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
     * GET /product
     *
     * Create web view "Product"
     *
     * @return Product page view as JSON
     */
    private Http2Response productView(SessionData sessionData, Long productId) {
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages";
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories?start=-1&max=-1";
        // GET api/persistence/products
        String persistenceEndpointProducts = PERSISTENCE_ENDPOINT + "/products";
        // GET api/image/getProductImages
        String imageEndpointProduct = IMAGE_ENDPOINT + "/productimages";
        // POST /api/recommender/recommend
        String recommenderEndpoint = RECOMMENDER_ENDPOINT + "/recommend";
        // GET /api/auth/useractions/isloggedin
        String authEndpoint = AUTH_ENDPOINT + "/useractions/isloggedin";
        try {
            // Get store icon
            Map<String, String> webImageSizeMap = new HashMap<>();
            String imageIconSize = ImageSizePreset.ICON.getSize().toString();
            webImageSizeMap.put("icon", imageIconSize);
            Map<String, String> webImageDataMap = getWebImages(imageEndpointWeb, webImageSizeMap);
            // Get categories
            List<Category> categories = getCategories(persistenceEndpointCategories);
            // Get product
            Product product = null;
            http2Header.method(GET.asciiName()).path(persistenceEndpointProducts + "?id=" + productId);
            // Create client and send request
            httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
            frameHandler = new Http2ClientStreamFrameHandler();
            httpClient.sendRequest(frameHandler);
            if (!frameHandler.jsonContent.isEmpty()) {
                product = mapper.readValue(frameHandler.jsonContent, Product.class);
            }
            // Get product images
            Map<Long, String> productImageSizeMap = new HashMap<>();
            String imageProductFullSize = ImageSizePreset.FULL.getSize().toString();
            productImageSizeMap.put(productId, imageProductFullSize);
            Map<Long, String> productImageDataMap = getProductImages(imageEndpointProduct, productImageSizeMap);
            // Create product view
            String addToCart = "/api/web/cartaction/addtocart?productId=" + productId;
            ProductView productView = new ProductView(
                    productId,
                    product.categoryId(),
                    productImageDataMap.get(productId),
                    product.name(),
                    product.listPriceInCents(),
                    product.description(),
                    addToCart
            );
            // Create product view with recommendations = advertisements
            List<ProductView> advertisements = new ArrayList<>();
            List<OrderItem> orderItems = sessionData.orderItems();
            List<Product> recommendedProducts = new ArrayList<>();
            List<Long> productIds = new ArrayList<>();
            // Recommendations works only with user id
            if(sessionData.userId() != null) {
                String orderItemsJson = mapper.writeValueAsString(orderItems);
                ByteBuf postOrderItemsBody = Unpooled.copiedBuffer(orderItemsJson, CharsetUtil.UTF_8);
                http2Header.method(POST.asciiName()).path(recommenderEndpoint + "?userid=" + sessionData.userId());
                http2Header.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(postOrderItemsBody.readableBytes()));
                http2DataFrame = new DefaultHttp2DataFrame(postOrderItemsBody);
                // Create client and send request
                httpClient = new Http2Client(gatewayHost, recommenderPort, http2Header, http2DataFrame);
                frameHandler = new Http2ClientStreamFrameHandler();
                httpClient.sendRequest(frameHandler);
                if (!frameHandler.jsonContent.isEmpty()) {
                    productIds = mapper.readValue(
                            frameHandler.jsonContent,
                            new TypeReference<List<Long>>(){}
                    );
                }
            }
            // Get product images
            productImageSizeMap = new HashMap<>();
            String imageProductPreviewSize = ImageSizePreset.PREVIEW.getSize().toString();
            // Get recommended products
            for (Long id : productIds) {
                productImageSizeMap.put(id, imageProductPreviewSize);
                //
                http2Header.method(GET.asciiName()).path(persistenceEndpointProducts + "?id=" + id);
                // Create client and send request
                httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
                frameHandler = new Http2ClientStreamFrameHandler();
                httpClient.sendRequest(frameHandler);
                if (!frameHandler.jsonContent.isEmpty()) {
                    recommendedProducts.add(mapper.readValue(frameHandler.jsonContent, Product.class));
                }
            }
            productImageDataMap = getProductImages(imageEndpointProduct, productImageSizeMap);
            // Create product views
            for (Product recommendedProduct : recommendedProducts) {
                Long id = recommendedProduct.id();
                advertisements.add(
                        new ProductView(
                                id,
                                recommendedProduct.categoryId(),
                                productImageDataMap.get(id),
                                recommendedProduct.name(),
                                recommendedProduct.listPriceInCents(),
                                recommendedProduct.description(),
                                "/api/web/cartaction/addtocart?productId=" + id
                        )
                );
            }
            // Create product page view
            ProductPageView view = new ProductPageView(
                    webImageDataMap.get("icon"),
                    "TeaStore Product",
                    categories,
                    productView,
                    advertisements
            );
            // Check login
            SessionData newSessionData = checkLogin(authEndpoint, sessionData);
            String json = mapper.writeValueAsString(view);
            if (newSessionData != null) {
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length())
                                .set(
                                        HttpHeaderNames.SET_COOKIE,
                                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                                ),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            } else {
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
     * GET /profile
     *
     * Create web view "Profile"
     *
     * @return Profile page view as JSON
     */
    private Http2Response profileView(SessionData sessionData) {
        // GET /api/auth/useractions/isloggedin
        String authEndpoint = AUTH_ENDPOINT + "/useractions/isloggedin";
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/webimages"; // storeIcon
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories?start=-1&max=-1";
        // GET api/persistence/users
        String persistenceEndpointUsers = PERSISTENCE_ENDPOINT + "/users?id=";
        // GET api/persistence/users
        String persistenceEndpointUserOrders = PERSISTENCE_ENDPOINT + "/orders?userid=";
        try {
            // Check login
            SessionData newSessionData = checkLogin(authEndpoint, sessionData);
            if (newSessionData == null) {
                return loginView(sessionData);
            } else {
                // Get store icon
                Map<String, String> webImageSizeMap = new HashMap<>();
                String imageIconSize = ImageSizePreset.ICON.getSize().toString();
                webImageSizeMap.put("icon", imageIconSize);
                Map<String, String> webImageDataMap = getWebImages(imageEndpointWeb, webImageSizeMap);
                // Get categories
                List<Category> categories = getCategories(persistenceEndpointCategories);
                // Get user
                User user = null;
                Long userId = newSessionData.userId();
                http2Header.method(GET.asciiName()).path(persistenceEndpointUsers + userId);
                // Create client and send request
                httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
                frameHandler = new Http2ClientStreamFrameHandler();
                httpClient.sendRequest(frameHandler);
                if (!frameHandler.jsonContent.isEmpty()) {
                    user = mapper.readValue(frameHandler.jsonContent, User.class);
                }
                // Get user orders
                List<Order> orders = new ArrayList<>();
                http2Header.method(GET.asciiName()).path(persistenceEndpointUserOrders + userId + "&start=-1&max=-1");
                // Create client and send request
                httpClient = new Http2Client(gatewayHost, persistencePort, http2Header, null);
                frameHandler = new Http2ClientStreamFrameHandler();
                httpClient.sendRequest(frameHandler);
                if (!frameHandler.jsonContent.isEmpty()) {
                    orders = mapper.readValue(
                            frameHandler.jsonContent,
                            new TypeReference<List<Order>>(){}
                    );
                }
                // Create previous orders
                List<PreviousOrder> previousOrders = new ArrayList<>();
                for (Order order: orders) {
                    previousOrders.add(new PreviousOrder(
                            order.id(),
                            order.time(),
                            order.totalPriceInCents(),
                            order.addressName(),
                            order.address1() + ", " + order.address2()
                    ));
                }
                // Create profile page view
                ProfilePageView view = new ProfilePageView(
                        webImageDataMap.get("icon"),
                        "TeaStore Profile",
                        categories,
                        user.userName(),
                        user.realName(),
                        user.email(),
                        previousOrders
                );
                String json = mapper.writeValueAsString(view);
                return new Http2Response(
                        Http2Response.okJsonHeader(json.length())
                                .set(
                                        HttpHeaderNames.SET_COOKIE,
                                        CookieUtil.encodeSessionData(newSessionData, gatewayHost).toString()
                                ),
                        Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Http2Response.internalServerErrorResponse();
    }
    // Status view is part of the API gateway
}
