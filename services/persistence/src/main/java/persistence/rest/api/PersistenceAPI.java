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
import persistence.database.*;
import utilities.datamodel.*;
import utilities.rest.API;

import java.io.IOException;
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
                        case "/categories":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return getCategory(id);
                            } else if (params.containsKey("start") && params.containsKey("max")) {
                                Integer start = Integer.parseInt(params.get("start").get(0));
                                Integer max = Integer.parseInt(params.get("max").get(0));
                                return getAllCategories(start, max);
                            } else {
                                return getAllCategories(null, null);
                            }
                    }
                case "POST":
                    return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
                case "PUT":
                    switch (subPath) {
                        case "/categories":
                            return updateCategory(body);
                    }
                case "DELETE":
                    switch (subPath) {
                        case "/cache":
                            if (params.containsKey("className")) {
                                String className = params.get("className").get(0);
                                return clearCache(className);
                            } else {
                                return clearCache(null);
                            }
                        case "/emf":
                            return clearEMF();
                        case "/categories":
                            if (params.containsKey("id")) {
                                Long id = Long.parseLong(params.get("id").get(0));
                                return deleteCategory(id);
                            } else {
                                return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
                            }
                    };
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
        if (cookieValue != null) {
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
     * DELETE /cache || /cache?classname=
     *
     * @return Status code 200 or 404
     */
    private FullHttpResponse clearCache(String className) {
        if (className == null) {
            CacheManager.MANAGER.clearLocalCacheOnly();
        } else {
            try {
                Class<?> entityClass = Class.forName(className);
                CacheManager.MANAGER.clearLocalCacheOnly(entityClass);
            } catch (Exception e) {
                e.printStackTrace();
                return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
            }
        }
        return new DefaultFullHttpResponse(httpVersion, OK);
    }

    /**
     * DELETE /emf
     *
     * @return Status code 200
     */
    private FullHttpResponse clearEMF() {
        CacheManager.MANAGER.resetLocalEMF();
        return new DefaultFullHttpResponse(httpVersion, OK);
    }

    /**
     * GET /categories?id=
     *
     * @param id  Required category Id
     * @return Category or NOT_FOUND
     */
    private FullHttpResponse getCategory(Long id) {
        PersistenceCategory persistenceEntity = CategoryRepository.REPOSITORY.getEntity(id);
        if (persistenceEntity == null) {
            return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
        }
        Category category = persistenceEntity.toRecord();
        try {
            String json = mapper.writeValueAsString(category);
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
     * GET /categories
     *
     * @param startIndex Optional start index
     * @param maxResultCount Optional max result count
     * @return Category or NOT_FOUND
     */
    private FullHttpResponse getAllCategories(Integer startIndex, Integer maxResultCount) {
        List<PersistenceCategory> persistenceEntities = null;
        if (startIndex == null || maxResultCount == null) {
            persistenceEntities = CategoryRepository.REPOSITORY.getAllEntities();
        } else {
            persistenceEntities = CategoryRepository.REPOSITORY.getAllEntities(startIndex, maxResultCount);
        }
        List<Category> categories = new ArrayList<Category>();
        for (PersistenceCategory persistenceCategory : persistenceEntities) {
            categories.add(persistenceCategory.toRecord());
        }
        try {
            String json = mapper.writeValueAsString(categories);
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
     * PUT /categories
     *
     * @param body Category as JSON
     * @return Updated element as JSON
     */
    private FullHttpResponse updateCategory(ByteBuf body) {
        Category category = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            category = mapper.readValue(jsonByte, Category.class);
            if (CategoryRepository.REPOSITORY.getEntity(category.id()) == null) {
                return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
            }
            if (CategoryRepository.REPOSITORY.updateEntity(category.id(), category)) {
                return new DefaultFullHttpResponse(
                        httpVersion,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(body)
                );
            }
            return new DefaultFullHttpResponse(httpVersion, BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
    }

    /**
     * DELETE /categories?id=
     *
     * @return OK or NOT_FOUND
     */
    private FullHttpResponse deleteCategory(Long id) {
        if (CategoryRepository.REPOSITORY.removeEntity(id)) {
            return new DefaultFullHttpResponse(httpVersion, OK);
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);

    }
}
