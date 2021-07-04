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
package image.rest.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import image.ImageProvider;
import image.setup.SetupController;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import utilities.datamodel.*;
import utilities.rest.api.API;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * API for web service
 * /api/web
 *
 * @author Philipp Backes
 */
public class ImageAPI implements API {
    private final HttpVersion httpVersion;
    private final ObjectMapper mapper;

    public ImageAPI(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        this.mapper = new ObjectMapper();
    }

    public FullHttpResponse handle(HttpRequest header, ByteBuf body, LastHttpContent trailer) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(header.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String method = header.method().name();
        String path = queryStringDecoder.path();

        // Select endpoint
        if (path.startsWith("/api/image")) {
            String subPath = path.substring("/api/image".length());
            switch (method) {
                case "GET":
                    switch (subPath) {
                        case "/finished":
                            return isFinished();
                        case "/regenerateimages":
                            return regenerateImages();
                        case "/state":
                            return getState();
                    }
                case "POST":
                    switch (subPath) {
                        case "/productimages":
                            return getProductImages(body);
                        case "/webimages":
                            return getWebImages(body);
                        case "/setCacheSize":
                            return setCacheSize(body);
                    };
                default:
                    return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
            }
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
    }

    /**
     * POST /productimages
     *
     * Queries the image provider for the given product IDs in the given size,
     * provided as strings
     *
     * @param body Map of product IDs and the corresponding image size as JSON
     * @return Map of product IDs and the image data (base64 encoded) as JSON
     */
    private FullHttpResponse getProductImages(ByteBuf body) {
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            Map<Long, String> images = mapper.readValue(
                    jsonByte,
                    new TypeReference<Map<Long, String>>(){}
            );
            images = ImageProvider.IP.getProductImages(
                images.entrySet().parallelStream().collect(
                    Collectors.toMap(Map.Entry::getKey,
                            e -> ImageSize.parseImageSize(e.getValue())
                    )
                )
            );
            String json = mapper.writeValueAsString(images);
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
     * POST /webimages
     *
     * Queries the image provider for the given web interface image names in the given size,
     * provided as strings
     *
     * @param body Map web interface image names and the corresponding image size as JSON
     * @return Map of web interface image names and the image data (base64 encoded) as JSON
     */
    private FullHttpResponse getWebImages(ByteBuf body) {
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            Map<String, String> imageSizeMap = mapper.readValue(
                    jsonByte,
                    new TypeReference<Map<String, String>>(){}
            );
            Map<String, String> imageDataMap = ImageProvider.IP.getWebImages(
                    imageSizeMap.entrySet().parallelStream().collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> ImageSize.parseImageSize(
                                        e.getValue()
                                )
                        )
                    )
            );
            String json = mapper.writeValueAsString(imageDataMap);
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
     * GET /regenerateimages
     *
     * Signals the image provider to regenerate all product images.
     * This is usually necessary if the product database changed
     *
     * @return OK
     */
    private FullHttpResponse regenerateImages() {
        SetupController.SETUP.reconfiguration();
        return new DefaultFullHttpResponse(httpVersion, OK);
    }

    /**
     * GET /finished
     *
     * Checks if the setup of the image provider and image generation has finished
     *
     * @return True or false
     */
    private FullHttpResponse isFinished() {
        Boolean finished = SetupController.SETUP.isFinished();
        try {
            String json = mapper.writeValueAsString(finished);
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
     * GET /state
     *
     * @return Service status
     */
    private FullHttpResponse getState() {
        String state = SetupController.SETUP.getState();
        try {
            String json = mapper.writeValueAsString(state);
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
     * POST /setcachesize
     *
     * Sets the cache size to the given value
     *
     * @return True or false
     */
    private FullHttpResponse setCacheSize(ByteBuf body) {
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            Long cacheSize = mapper.readValue(jsonByte, Long.class);
            Boolean success = SetupController.SETUP.setCacheSize(cacheSize);
            String json = mapper.writeValueAsString(success);
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
