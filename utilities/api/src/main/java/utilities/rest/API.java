package utilities.rest;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * API interface
 *
 * @author Philipp Backes
 *
 */
public interface API {
    String WEB_ENDPOINT = "/api/web";
    String IMAGE_ENDPOINT = "/api/image";
    String AUTH_ENDPOINT = "/api/auth";
    String RECOMMENDER_ENDPOINT = "/api/recommender";
    String PERSISTENCE_ENDPOINT = "/api/persistence";
    FullHttpResponse handle(HttpRequest header, ByteBuf body, LastHttpContent trailer);
}