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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import utilities.datamodel.*;
import utilities.rest.API;

import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * API for web service
 * /api/web
 *
 * @author Philipp Backes
 */
public class ImageAPI implements API {
    private final HttpVersion httpVersion;
    private final String scheme;
    private final ObjectMapper mapper;

    public ImageAPI(HttpVersion httpVersion, String scheme) {
        this.httpVersion = httpVersion;
        this.scheme = scheme;
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
                            return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
                        case "/regenerateimages":
                            return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
                        case "/state":
                            return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
                    }
                case "POST":
                    switch (subPath) {
                        case "/productimages":
                            return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
                        case "/webimages":
                            return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
                        case "/setCacheSize":
                            return new DefaultFullHttpResponse(httpVersion, NOT_IMPLEMENTED);
                    };
                default:
                    return new DefaultFullHttpResponse(httpVersion, INTERNAL_SERVER_ERROR);
            }
        }
        return new DefaultFullHttpResponse(httpVersion, NOT_FOUND);
    }

}
