package web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import utilities.rest.API;
import web.rest.client.HttpClient;
import web.rest.client.HttpClientHandler;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * API for web service
 * /api/web
 */
public class WebAPI implements API {
    private final HttpVersion httpVersion;
    private HttpClient httpClient;
    private final String scheme;
    private final ObjectMapper mapper;
    private final int webPort;
    private final int imagePort;
    private final int authPort;
    private final int recommenderPort;
    private final int persistencePort;

    public WebAPI(HttpVersion httpVersion, String scheme) {
        this.httpVersion = httpVersion;
        this.scheme = scheme;
        mapper = new ObjectMapper();
        this.webPort = 80;
        this.imagePort = 80;
        this.authPort = 80;
        this.recommenderPort = 80;
        this.persistencePort = 80;
    }

    public FullHttpResponse handle(HttpRequest header, ByteBuf body, LastHttpContent trailer) {
        StringBuilder responseData = new StringBuilder();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(header.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String method = header.method().name();
        String path = queryStringDecoder.path();
        // Select endpoint
        if (path.startsWith("/api/web")) {
            String subPath = path.substring("/api/web".length());
            switch (method) {
                case "GET":
                    switch (subPath) {
                        case "/isready":
                            return isReady();
                        case "/about":
                            return aboutView();
                        case "/cart":
                            return cartView();
                        case "/category":
                            return categoryView();
                        case "/database":
                            return databaseView();
                        case "/error":
                            return errorView();
                        case "/index":
                            return indexView();
                        case "/login":
                            return loginView();
                        case "/product":
                            return productView();
                        case "/profile":
                            return profileView();
                        case "/status":
                            return statusView();
                    }
                case "POST":
                    switch (subPath) {
                        case "/cartAction":
                            return cartAction(body);
                        case "/databaseAction":
                            return databaseAction(body);
                        case "/loginAction":
                            return loginAction(body);
                        case "/order":
                            return orderView(body);
                    }
                case "PUT":
                    return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
                case "DELETE":
                    return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
                default:
                    return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
            }
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
    }

    /**
     * Required for testing as long as image service is not implemented
     */
    public String getWebImages() {
        String json = "{}";
        AboutView view = new AboutView();
        Map<String, String> portraits = new HashMap<>();
        portraits.put("portraitAndre", "PORTRAIT1");
        portraits.put("portraitJohannes", "PORTRAIT2");
        portraits.put("portraitSimon", "PORTRAIT3");
        portraits.put("portraitNorbert", "PORTRAIT4");
        portraits.put("portraitKounev", "PORTRAIT5");
        view.portraits = portraits;
        view.descartesLogo = "LOGO";
        view.storeIcon = "STOREICON";
        view.isLoggedIn = false;
        try {
            json = mapper.writeValueAsString(view);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Required for testing as long as persistence service is not implemented
     */
    public String getPersistenceProducts() {
        String json = "{}";
        Map<Long, Product> products = new HashMap<>();
        Product product = new Product();
        product.id = 1;
        product.categoryId = 1;
        product.name = "Product 1";
        product.description = "Product 1 description";
        product.listPriceInCents = 100;
        products.put(1L, product);
        products.put(2L, product);
        products.put(3L, product);
        try {
            json = mapper.writeValueAsString(products);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Required for testing as long as persistence service is not implemented
     */
    public String getPersistenceCategories() {
        String json = "{}";
        List<Category> categoryList = new ArrayList<>();
        Category category = new Category();
        category.id = 1;
        category.name = "Category 1";
        category.description = "Category 1 description";
        categoryList.add(category);
        categoryList.add(category);
        categoryList.add(category);
        try {
            json = mapper.writeValueAsString(categoryList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Required for testing as long as recommender service is not implemented
     */
    public String getRecommendations() {
        String json = "{}";
        List<Product> advertisments = new ArrayList<>();
        Product product = new Product();
        product.id = 2;
        product.categoryId = 1;
        product.name = "Ad: Product 1";
        product.description = "Ad: Product 1 description";
        product.listPriceInCents = 100;
        advertisments.add(product);
        advertisments.add(product);
        advertisments.add(product);
        try {
            json = mapper.writeValueAsString(advertisments);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }


    /**
     * GET /ready
     *
     * @return Service status
     */
    public FullHttpResponse isReady() {
        return new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.OK);
    }

    /**
     * GET /about
     *
     * Servlet implementation for the web view of "About us"
     *
     * @return Web view as JSON
     */
    private FullHttpResponse aboutView() {
        // POST image/getWebImages
        String host = "127.0.0.1";
        // TODO: Replace with "/api/image/getWebImages"
        String imageEndpoint = WEB_ENDPOINT + "/isready";
        // TODO: Replace with "/api/auth/useractions/isloggedin"
        String authEndpoint = WEB_ENDPOINT + "/isready";
        URI imageURI = null;
        URI authURI = null;
        try {
            imageURI = new URI(scheme + host + ":" + imagePort + imageEndpoint);
            authURI = new URI(scheme + host + ":" + authPort + authEndpoint);
            // TODO: POST
            HttpRequest request = new DefaultFullHttpRequest(
                httpVersion, HttpMethod.GET, imageURI.getRawPath(), Unpooled.EMPTY_BUFFER);
            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            // Create client and send request
            httpClient = new HttpClient(host, imagePort, request);
            HttpClientHandler handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if(handler.response instanceof HttpContent httpContent) {
                // TODO: ByteBuf imageData = httpContent.content();
                AboutView view = view = mapper.readValue(getWebImages(), AboutView.class);
                request.setUri(authURI.getRawPath());
                httpClient = new HttpClient(host, authPort, request);
                handler = new HttpClientHandler();
                httpClient.sendRequest(handler);
                String json = "{}";
                if(handler.response instanceof HttpResponse response) {
                    // Check if user is logged in
                    view.isLoggedIn = response.status() == OK;
                }
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
     * POST /cartAction
     *
     * Handling all cart actions
     *
     * @return Status of cart action
     */
    private FullHttpResponse cartAction(ByteBuf body) {
        ObjectMapper mapper = new ObjectMapper();
        CartAction action = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        String jsonString = "";
        try {
            action = mapper.readValue(jsonByte, CartAction.class);
            String jsonResponse = "{}";
            System.out.println("Action: " + action.name);
            switch(action.name) {
                case "addToCart": // Call auth/store service and redirect to /cart
                    break;
                case "removeProduct": // Call auth/store service and redirect to /cart
                    break;
                case "updateCartQuantities": // Call auth/store service for all items and redirect to /cart
                    break;
            }
            jsonString = mapper.writeValueAsString(jsonResponse);

            return new DefaultFullHttpResponse(
                    httpVersion,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(jsonString, CharsetUtil.UTF_8)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
    }

    private FullHttpResponse cartView() {
        // POST image/getWebImages
        String host = "127.0.0.1";
        // TODO: Replace with "/api/persistence/products"
        String persistenceEndpointProducts = WEB_ENDPOINT + "/isready"; // 2x products & advertisments
        URI productsURI = null;
        // TODO: Replace with "/api/image/getWebImage"
        String imageEndpoint = WEB_ENDPOINT + "/isready"; // 2x storeIcon & productImages for ads
        URI imageURI = null;
        // TODO: Replace with "/api/persistence/categories"
        String persistenceEndpointCategories = WEB_ENDPOINT + "/isready"; // categoryList
        URI categoriesURI = null;
        // TODO: Replace with "/api/auth/useractions/isloggedin"
        String authEndpoint = WEB_ENDPOINT + "/isready"; // isLoggedIn
        URI authURI = null;
        // TODO: Replace with POST ""/api/recommender/recommend
        String recommenderEndpoint = WEB_ENDPOINT + "/isready"; // productIds for advertisments
        URI recommenderURI = null;
        try {
            productsURI = new URI(scheme + host + ":" + persistencePort + persistenceEndpointProducts);
            categoriesURI = new URI(scheme + host + ":" + persistencePort + persistenceEndpointCategories);
            authURI = new URI(scheme + host + ":" + authPort + authEndpoint);
            recommenderURI = new URI(scheme + host + ":" + recommenderPort + recommenderEndpoint);
            imageURI = new URI(scheme + host + ":" + imagePort + imageEndpoint);
            // TODO: IMPLEMENT
            HttpRequest request = new DefaultFullHttpRequest(
                    httpVersion, HttpMethod.GET, productsURI.getRawPath(), Unpooled.EMPTY_BUFFER);
            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            // Create client and send request
            httpClient = new HttpClient(host, persistencePort, request);
            HttpClientHandler handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if(handler.response instanceof HttpContent httpContent) {
                CartView view = new CartView();
                // TODO: other requests
                view.storeIcon = "";
                // TODO: ByteBuf imageData = httpContent.content();
                view.products = mapper.readValue(
                        getPersistenceProducts(),
                        new TypeReference<Map<Long, Product>>(){}
                );
                view.categoryList = mapper.readValue(
                        getPersistenceCategories(),
                        new TypeReference<List<Category>>(){}
                );
                // TODO: Replace with service calls
                List<OrderItem> orderItems = new ArrayList<>();
                OrderItem item = new OrderItem();
                item.id = 1;
                item.productId = 1;
                item.orderId = 1;
                item.quantity = 1;
                item.unitPriceInCents = 100;
                orderItems.add(new OrderItem());
                view.orderItems = orderItems;
                view.storeIcon = "";
                // TODO: Filter products with recommendations
                view.advertisments = mapper.readValue(
                        getRecommendations(),
                        new TypeReference<List<Product>>(){}
                );
                view.productImages = new ArrayList<String>();
                request.setUri(authURI.getRawPath());
                httpClient = new HttpClient(host, authPort, request);
                handler = new HttpClientHandler();
                httpClient.sendRequest(handler);
                String json = "{}";
                if(handler.response instanceof HttpResponse response) {
                    // Check if user is logged in
                    view.isLoggedIn = response.status() == OK;
                }
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

    private FullHttpResponse categoryView() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse databaseAction(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse databaseView() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse errorView() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse indexView() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse loginAction(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse loginView() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse orderView(ByteBuf body) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse productView() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse profileView() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse statusView() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }
}
