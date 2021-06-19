package web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.net.URISyntaxException;
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

    public WebAPI(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
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
     * For testing
     */
    public String getTestJson() {
        /*
        ObjectMapper mapper = new ObjectMapper();
        StorePage page = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        String jsonString = "";
        try {
            page = mapper.readValue(jsonByte, StorePage.class);
            jsonString = mapper.writeValueAsString(page);
            return new DefaultFullHttpResponse(
                    httpVersion,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(jsonString, CharsetUtil.UTF_8)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
        */
        String json = "{}";
        Map<String,String> map = new HashMap<>();
        map.put("name","TeaStore v2");
        map.put("url","http://localhost:80/teastore/");
        try {
            json = new ObjectMapper().writeValueAsString(map);
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
        int imagePort = 80;
        String imageEndpoint = "/api/web/isready"; // TODO: Replace with "/api/image/getWebImages"
        URI uri = null;
        try {
            uri = new URI("http://" + host + ":" + imagePort + imageEndpoint);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        HttpRequest request = new DefaultFullHttpRequest(
            httpVersion, HttpMethod.GET, uri.getRawPath(), Unpooled.EMPTY_BUFFER);
        request.headers().set(HttpHeaderNames.HOST, host);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        // Create client and send request
        httpClient = new HttpClient(host, imagePort, request);
        HttpClientHandler handler = new HttpClientHandler();
        httpClient.sendRequest(handler);
        if(handler.response instanceof HttpContent httpContent) {
            return new DefaultFullHttpResponse(
                    httpVersion,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(httpContent.content())
            );
        } else {
            return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
        }
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
        return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
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
