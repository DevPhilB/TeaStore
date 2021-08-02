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
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utilities.datamodel.ImageSize;
import utilities.rest.api.API;
import utilities.rest.api.Http3Response;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP/3 API for image service
 * /api/image
 *
 * @author Philipp Backes
 */
public class Http3ImageAPI implements API {
    private final ObjectMapper mapper;
    private static final Logger LOG = LogManager.getLogger(Http3ImageAPI.class);

    public Http3ImageAPI(String gatewayHost, Integer gatewayPort) {
        this.mapper = new ObjectMapper();
    }

    public Http3Response handle(Http3Headers headers, ByteBuf body) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(headers.path().toString());
        String method = headers.method().toString();
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
                        case "/setcachesize":
                            return setCacheSize(body);
                    }
                default:
                    break;
            }
        }
        return Http3Response.notFoundResponse();
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
    private Http3Response getProductImages(ByteBuf body) {
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
            return new Http3Response(
                    Http3Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
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
    private Http3Response getWebImages(ByteBuf body) {
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            Map<String, String> imageSizeMap = mapper.readValue(
                    jsonByte,
                    new TypeReference<Map<String, String>>(){}
            );
            LOG.info("IMAGE: " + imageSizeMap.toString());
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
            LOG.info("IMAGE: getWebImages returns: " + json);
            return new Http3Response(
                    Http3Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        LOG.info("IMAGE: getWebImages failed!");
        return Http3Response.internalServerErrorResponse();
    }

    /**
     * GET /regenerateimages
     *
     * Signals the image provider to regenerate all product images.
     * This is usually necessary if the product database changed
     *
     * @return OK
     */
    private Http3Response regenerateImages() {
        SetupController.SETUP.reconfiguration();
        return Http3Response.okResponse();
    }

    /**
     * GET /finished
     *
     * Checks if the setup of the image provider and image generation has finished
     *
     * @return True or false
     */
    private Http3Response isFinished() {
        Boolean finished = SetupController.SETUP.isFinished();
        try {
            String json = mapper.writeValueAsString(finished);
            return new Http3Response(
                    Http3Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
    }

    /**
     * GET /state
     *
     * @return Service status
     */
    private Http3Response getState() {
        String state = SetupController.SETUP.getState();
        try {
            String json = mapper.writeValueAsString(state);
            return new Http3Response(
                    Http3Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
    }

    /**
     * POST /setcachesize
     *
     * Sets the cache size to the given value
     *
     * @return True or false
     */
    private Http3Response setCacheSize(ByteBuf body) {
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            Long cacheSize = mapper.readValue(jsonByte, Long.class);
            Boolean success = SetupController.SETUP.setCacheSize(cacheSize);
            String json = mapper.writeValueAsString(success);
            return new Http3Response(
                    Http3Response.okJsonHeader(json.length()),
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return Http3Response.internalServerErrorResponse();
    }
}
