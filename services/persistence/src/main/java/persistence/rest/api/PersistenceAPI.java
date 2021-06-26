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
package persistence.rest.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.util.CharsetUtil;
import persistence.database.OrderRepository;
import persistence.database.PersistenceOrder;
import utilities.datamodel.*;
import utilities.rest.API;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
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
public class PersistenceAPI implements API {
    private final HttpVersion httpVersion;
    private final String scheme;
    private final ObjectMapper mapper;
    private final String gatewayHost;
    private final Integer webPort;
    private final Integer imagePort;
    private final Integer authPort;
    private final Integer recommenderPort;
    private final Integer persistencePort;
    private final HttpRequest request;

    public PersistenceAPI(HttpVersion httpVersion, String scheme) {
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
        String cookieValue = header.headers().get(HttpHeaderNames.COOKIE);
        SessionData sessionData = decodeCookie(cookieValue);

        // Select endpoint
        if (path.startsWith("/api/persistence")) {
            String subPath = path.substring("/api/persistence".length());
            switch (method) {
                case "GET":
                    switch (subPath) {
                        case "/orders":
                            return orders();
                    }
                case "POST":
                    return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
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
     * Decode cookie to session data
     *
     * @param cookieValue Cookie value as String
     * @return SessionData
     */
    private SessionData decodeCookie(String cookieValue) {
        SessionData cookie = null;
        if(cookieValue != null) {
            try {
                cookie = mapper.readValue(
                        URLDecoder.decode(
                                cookieValue.substring("SessionData=".length()),
                                CharsetUtil.UTF_8
                        ),
                        SessionData.class
                );
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            cookie = new SessionData(
                    null,
                    null,
                    null,
                    null,
                    new ArrayList<>(),
                    null
            );
        }
        return cookie;
    }

    /**
     * Encode session data as cookie
     *
     * @param sessionData Session data
     * @return Cookie
     */
    private Cookie encodeSessionData(SessionData sessionData) {
        try {
            String encodedCookie = URLEncoder.encode(
                    mapper.writeValueAsString(sessionData),
                    CharsetUtil.UTF_8
            );
            return new DefaultCookie("SessionData", encodedCookie);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * GET /order
     *
     * @return All orders
     */
    private FullHttpResponse orders() {
        List<Order> order = new ArrayList<Order>();
        for (PersistenceOrder persistenceOrder : OrderRepository.REPOSITORY.getAllEntities()) {
            order.add(persistenceOrder.toRecord());
        }
        try {
            String json = mapper.writeValueAsString(order);
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
