package web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * /api/web
 */
public class WebAPI {

    public FullHttpResponse handle(HttpRequest request) {
        StringBuilder responseData = new StringBuilder();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String path = queryStringDecoder.path();
        // Select endpoint
        if(path.startsWith("/api/web")) {
            switch(path.substring("/api/web".length())) {
                case "/ready": return isReady();
                case "/data": return data();
                default: break;
            }
        }
        return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
    }

    /**
     * /ready
     * @return Service status
     */
    public FullHttpResponse isReady() {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    }

    /**
     * /data
     * @return Service data
     */
    public FullHttpResponse data() {
        String json = "";
        Map<String, String> testData = new HashMap<>();
        testData.put("id", "607");
        testData.put("address", "361 East 1509 North District 104");
        testData.put("address name", "Utopia");
        try {
            json = new ObjectMapper().writeValueAsString(testData);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
        );
    }
}
