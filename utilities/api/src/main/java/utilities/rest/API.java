package utilities.rest;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * API interface
 *
 * @author Philipp Backes
 *
 */
public interface API {
    public FullHttpResponse handle(HttpRequest header, ByteBuf body, LastHttpContent trailer);
}