package web.rest.api;

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
import web.rest.datamodel.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpMethod.*;

/**
 * API for web service
 * /api/web
 */
public class WebAPI implements API {
    private final HttpVersion httpVersion;
    private HttpClient httpClient;
    private HttpClientHandler handler;
    private final String scheme;
    private final ObjectMapper mapper;
    private final String gatewayHost;
    private final int webPort;
    private final int imagePort;
    private final int authPort;
    private final int recommenderPort;
    private final int persistencePort;
    private final HttpRequest request;

    public WebAPI(HttpVersion httpVersion, String scheme) {
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
        Map<String, String> portraits = new HashMap<>();
        portraits.put("portraitAndre", "PORTRAIT1");
        portraits.put("portraitJohannes", "PORTRAIT2");
        portraits.put("portraitSimon", "PORTRAIT3");
        portraits.put("portraitNorbert", "PORTRAIT4");
        portraits.put("portraitKounev", "PORTRAIT5");
        String title = "TeaStore About Us";
        AboutView view = new AboutView(
                portraits,
                "LOGO",
                "STOREICON",
                title,
                false
        );
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
        Product product = new Product(
                1,
                1,
                "Product 1",
                "Product 1 description",
                100
        );
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
        Category category = new Category(
                1,
                "Category 1",
                "Category 1 description"
        );
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
        List<Product> advertisements = new ArrayList<>();
        Product product = new Product(
                2,
                1,
                "Ad: Product 1",
                "Ad: Product 1 description",
                100
        );
        advertisements.add(product);
        advertisements.add(product);
        advertisements.add(product);
        try {
            json = mapper.writeValueAsString(advertisements);
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
        String imageEndpoint = IMAGE_ENDPOINT + "/getWebImages";
        String authEndpoint = AUTH_ENDPOINT + "/isloggedin";
        try {
            request.setUri(imageEndpoint);
            request.setMethod(POST);
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, imagePort, request);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if(handler.response instanceof HttpContent httpContent) {
                // TODO: ByteBuf imageData = httpContent.content();
                AboutView view = mapper.readValue(getWebImages(), AboutView.class);
                request.setUri(authEndpoint);
                request.setMethod(GET);
                httpClient = new HttpClient(gatewayHost, authPort, request);
                handler = new HttpClientHandler();
                httpClient.sendRequest(handler);
                String json = "{}";
                // TODO: Use response status?
                //if(handler.response instanceof HttpResponse response) {
                    // Check if user is logged in
                    // view.isLoggedIn = response.status() == OK;
                //}
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
        //
        String authEndpointAdd = AUTH_ENDPOINT + "/cart/add"; // POST
        String authEndpointRemove = AUTH_ENDPOINT + "/cart/remove"; // POST
        String authEndpointUpdate = AUTH_ENDPOINT + "/cart"; // PUT
        try {
            action = mapper.readValue(jsonByte, CartAction.class);
            System.out.println("Action: " + action.name());
            switch(action.name()) {
                case "addToCart":
                    request.setMethod(HttpMethod.POST);
                    request.setUri(authEndpointAdd);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    // Call auth/store service
                case "removeProduct":
                    request.setMethod(HttpMethod.POST);
                    request.setUri(authEndpointRemove);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    // Call auth/store service
                case "updateCartQuantities":
                    request.setMethod(HttpMethod.PUT);
                    request.setUri(authEndpointUpdate);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    // Call auth/store service for all items
            }
            // And return cart view
            return cartView(); // TODO: Session or body?
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
    }

    private FullHttpResponse cartView() {
        // GET 2x products & advertisments
        String persistenceEndpointProducts = PERSISTENCE_ENDPOINT + "/products"; // products
        // POST image/getWebImages
        String imageEndpoint = IMAGE_ENDPOINT + "/getWebImages";; // storeIcon & productImages for ads
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
            if(handler.response instanceof HttpContent httpContent) {
                // TODO: Replace with service calls
                List<OrderItem> orderItems = new ArrayList<>();
                OrderItem item = new OrderItem(
                        1,
                        1,
                        1,
                        1,
                        100
                );
                orderItems.add(item);
                // TODO: Filter products with recommendations
                List<String> productImages = new ArrayList<String>();
                // TODO: other requests
                request.setUri(authEndpoint);
                httpClient = new HttpClient(gatewayHost, authPort, request);
                handler = new HttpClientHandler();
                httpClient.sendRequest(handler);
                String json = "{}";
                boolean isLoggedIn = false;
                if(handler.response instanceof HttpResponse response) {
                    // Check if user is logged in
                    isLoggedIn = response.status() == OK;
                }
                CartView view = new CartView(
                        // TODO: ByteBuf imageData = httpContent.content();
                        "",
                        "TeaStore Cart",
                        mapper.readValue(
                                getPersistenceCategories(),
                                new TypeReference<List<Category>>(){}
                        ),
                        orderItems,
                        mapper.readValue(
                                getPersistenceProducts(),
                                new TypeReference<Map<Long, Product>>(){}
                        ),
                        isLoggedIn,
                        mapper.readValue(
                                getRecommendations(),
                                new TypeReference<List<Product>>(){}
                        ),
                        productImages
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

    private FullHttpResponse categoryView() {

        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
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
