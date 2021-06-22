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
            //String[] subPaths = path.substring("/api/web".length()).split("/");
            //String subPath = "";
            //String
            //if(subPaths.length > 2) {

            //}
            String subPath = path.substring("/api/web".length());
            //String action = if(subPath.startsWith("/cartAction"))
            switch (method) {
                case "GET":
                    switch (subPath) {
                        case "/isready":
                            return isReady();
                        case "/about":
                            return aboutView();
                        case "/cartAction/addToCart":
                        case "/cartAction/removeProduct":
                        case "/cartAction/updateCartQuantities":
                        case "/cartAction/proceedToCheckout":
                            if(params.containsKey("productId")) {
                                String action = subPath.substring("/cartAction/".length());
                                long productId = Long.parseLong(params.get("productId").get(0));
                                return cartAction(action, productId);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/cart":
                            return cartView(); // TODO: Session or body
                        case "/category":
                            if(params.containsKey("category")) {
                                long categoryId = Long.parseLong(params.get("category").get(0));
                                int productQuantity = 20;
                                int page = 1;
                                if(params.containsKey("productNumber")) {
                                    productQuantity = Integer.parseInt(params.get("productNumber").get(0));
                                }
                                if(params.containsKey("page")) {
                                    page = Integer.parseInt(params.get("page").get(0));
                                }
                                return categoryView(categoryId, productQuantity, page);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/databaseAction":
                            if(params.containsKey("categories")
                                    && params.containsKey("products")
                                    && params.containsKey("users")
                                    && params.containsKey("orders")
                            ) {
                                int categories = Integer.parseInt(params.get("categories").get(0));
                                int products = Integer.parseInt(params.get("products").get(0));
                                int users = Integer.parseInt(params.get("users").get(0));
                                int orders = Integer.parseInt(params.get("orders").get(0));
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
                        case "/product":
                            if(params.containsKey("id")) {
                                return productView(params.get("id").get(0));
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                        case "/profile":
                            return profileView();  // TODO: Session or body
                    }
                case "POST":
                    switch (subPath) {
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
        String descartesDescription= "We are part of the Descartes Research Group:";
        String description= "Our research is aimed at developing novel methods, ...";
        AboutView view = new AboutView(
                "STOREICON",
                title,
                portraits,
                descartesDescription,
                "DESCARTESLOGO",
                description,
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
        List<Product> products = new ArrayList<>();
        long id = 1;
        String addToCart = "/api/web/cartAction/addToCart?productId=" + id;
        Product product = new Product(
                id,
                1,
                "PRODUCTONE",
                "Product 1",
                100,
                "Product 1 description",
                addToCart
        );
        products.add(product);
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
        long id = 2;
        String addToCart = "/api/web/cartAction/addToCart?productId=" + id;
        Product product = new Product(
                2,
                1,
                "PRODUCTTWO",
                "Ad: Product 2",
                100,
                "Ad: Product 2 description",
                addToCart
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
     * Required for testing as long as persistence service is not implemented
     */
    public String getCategoryView() {
        String json = "{}";
        List<Product> products = new ArrayList<>();
        long id = 3;
        String addToCart = "/api/web/cartAction/addToCart?productId=" + id;
        products.add(new Product(
                3,
                1,
                "PRODUCTTHREE",
                "Ad: Product 3",
                100,
                "Ad: Product 3 description",
                addToCart
        ));
        long categoryId = 1;
        String title = "Tea";
        int productQuantity = 20;
        int page = 1;
        try {
            CategoryView view = new CategoryView(
                    "STOREICON",
                    title,
                    mapper.readValue(
                            getPersistenceCategories(),
                            new TypeReference<List<Category>>(){}
                    ),
                    products,
                    page,
                    productQuantity,
                    false
            );
            json = mapper.writeValueAsString(view);
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
        // POST api/image/getWebImages
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
     * GET /cartAction
     *
     * Handling all cart actions
     *
     * @return FullHttpResponse
     */
    private FullHttpResponse cartAction(String name, long productId) {
        //
        String authEndpointAdd = AUTH_ENDPOINT + "/cart/add"; // POST
        String authEndpointRemove = AUTH_ENDPOINT + "/cart/remove"; // POST
        String authEndpointUpdate = AUTH_ENDPOINT + "/cart"; // PUT
        try {
            System.out.println("Action: " + name);
            switch(name) {
                case "addToCart":
                    request.setMethod(HttpMethod.POST);
                    request.setUri(authEndpointAdd);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if(handler.response instanceof HttpResponse response) {
                        if(response.status() != OK) {
                            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                        }
                    }
                case "removeProduct":
                    request.setMethod(HttpMethod.POST);
                    request.setUri(authEndpointRemove);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if(handler.response instanceof HttpResponse response) {
                        if(response.status() != OK) {
                            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                        }
                    }
                case "updateCartQuantities":
                    request.setMethod(HttpMethod.PUT);
                    request.setUri(authEndpointUpdate);
                    handler = new HttpClientHandler();
                    httpClient.sendRequest(handler);
                    if(handler.response instanceof HttpResponse response) {
                        if(response.status() == UNAUTHORIZED) {
                            return loginView();
                        }
                    }
            }
            // And return cart view
             return cartView(); // TODO: Session or body?
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
    }

    private FullHttpResponse cartView() {
        // GET 2x products & advertisements
        String persistenceEndpointProducts = PERSISTENCE_ENDPOINT + "/products"; // products
        // POST api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/getWebImages"; // storeIcon
        String imageEndpointProduct = IMAGE_ENDPOINT + "/getProductImages"; // productImages for ads
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
                long id = 1;
                String removeProduct = "/api/web/cartAction/removeProduct?productId=" + id;
                List<CartItem> cartItems = new ArrayList<>();
                CartItem item = new CartItem(
                        1,
                        "Product 1",
                        "Product 1 description",
                        2,
                        100,
                        200,
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
                if(handler.response instanceof HttpResponse response) {
                    // Check if user is logged in
                    isLoggedIn = response.status() == OK;
                }
                String updateCart = "/api/web/cartAction/updateCartQuantities";
                String proceedToCheckout = "/api/web/cartAction/proceedToCheckout";
                CartView view = new CartView(
                        // TODO: ByteBuf imageData = httpContent.content();
                        "STOREICON",
                        "Shopping Cart",
                        mapper.readValue(
                                getPersistenceCategories(),
                                new TypeReference<List<Category>>(){}
                        ),
                        cartItems,
                        mapper.readValue(
                                getRecommendations(),
                                new TypeReference<List<Product>>(){}
                        ),
                        productImages,
                        updateCart,
                        proceedToCheckout,
                        false
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

    private FullHttpResponse categoryView(long categoryId, int productQuantity, int page) {
        // GET api/persistence/categories
        String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories"; // categoryList
        // GET api/persistence/products
        String persistenceEndpointProducts = PERSISTENCE_ENDPOINT + "/products"; // 2x products
        // GET api/image/getProductImages
        String imageEndpointProduct = IMAGE_ENDPOINT + "/getProductImages"; // productImages
        // GET api/image/getWebImages
        String imageEndpointWeb = IMAGE_ENDPOINT + "/getWebImages"; // storeIcon
        try {
            // TODO: IMPLEMENT
            request.setUri(persistenceEndpointCategories);
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, persistencePort, request);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if(handler.response instanceof HttpContent httpContent) {
                // TODO: Replace with service calls
                String json = getCategoryView();
                // TODO: other requests
                request.setUri(persistenceEndpointProducts);
                httpClient = new HttpClient(gatewayHost, authPort, request);
                handler = new HttpClientHandler();
                httpClient.sendRequest(handler);
                // String json = "{}";
                boolean isLoggedIn = false;
                if(handler.response instanceof HttpResponse response) {
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

    // Only if implemented in persistence
    private FullHttpResponse databaseAction(int categories, int products, int users, int orders) {
        // GET api/persistence/categories
        String authEndpoint = PERSISTENCE_ENDPOINT + "/generatedb" +
                "?categories=" + categories + "&products=" + products +
                "&users=" + users + "&orders=" + orders;
        try {
            request.setUri(authEndpoint);
            // Create client and send request
            httpClient = new HttpClient(gatewayHost, persistencePort, request);
            handler = new HttpClientHandler();
            httpClient.sendRequest(handler);
            if(handler.response instanceof HttpResponse response) {
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

    private FullHttpResponse databaseView() {
        DatabaseView view = new DatabaseView(
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

    private FullHttpResponse errorView() {
        // TODO: Persistence, image and auth service calls
        ErrorView view = new ErrorView(
                "STOREICON",
                "Oops, something went wrong!",
                "ERRORIMAGE",
                "/api/web/index",
                false
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

    private FullHttpResponse indexView() {
        // TODO: Persistence, image and auth service calls
        try {
            IndexView view = new IndexView(
                    "STOREICON",
                    "",
                    mapper.readValue(
                            getPersistenceCategories(),
                            new TypeReference<List<Category>>(){}
                    ),
                    "LARGESTOREICON",
                    false
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

    private FullHttpResponse loginAction(ByteBuf body) {
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
                    if(handler.response instanceof HttpResponse response) {
                        if(response.status() != OK) {
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
                    if(handler.response instanceof HttpResponse response) {
                        if(response.status() != OK) {
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

    private FullHttpResponse loginView() {
        // TODO: Persistence, image and auth service calls
        try {
            LoginView view = new LoginView(
                    "STOREICON",
                    "Login",
                    mapper.readValue(
                            getPersistenceCategories(),
                            new TypeReference<List<Category>>(){}
                    ),
                    "Please enter your username and password.",
                    "",
                    "",
                    "/api/web/loginAction/login",
                    false,
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

    private FullHttpResponse orderView(ByteBuf body) {
        // TODO: Continue
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse productView(String productId) {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    private FullHttpResponse profileView() {
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
    }

    // Status view is part of the API gateway
}
