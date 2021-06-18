package web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import utilities.rest.API;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * API for web service
 * /api/web
 */
public class WebAPI implements API {

    public FullHttpResponse handle(HttpRequest header, ByteBuf body, LastHttpContent trailer) {
        StringBuilder responseData = new StringBuilder();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(header.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String method = header.method().name();
        String path = queryStringDecoder.path();
        // Select endpoint
        if(path.startsWith("/api/web")) {
            String subPath = path.substring("/api/web".length());
            switch(method) {
                case "GET": return isReady();
                case "POST": return data(body);
                case "PUT": return new DefaultFullHttpResponse(HTTP_1_1, NOT_IMPLEMENTED);
                case "DELETE": return new DefaultFullHttpResponse(HTTP_1_1, NOT_IMPLEMENTED);
                default: return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
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
     * @param body JSON input as bytes
     * @return Service data
     */
    public FullHttpResponse data(ByteBuf body) {
        System.out.println("Hello!");
        ObjectMapper mapper = new ObjectMapper();
        StorePage page = null;
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        String jsonString = "";
        try {
            page = mapper.readValue(jsonByte, StorePage.class);
            jsonString = mapper.writeValueAsString(page);
            return new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(jsonString, CharsetUtil.UTF_8)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
    }
}
