package utilities.rest.api;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public record Http2Response (
        Http2Headers headers,
        ByteBuf body
) {
    public static Http2Response okResponse() {
        return new Http2Response(new DefaultHttp2Headers().status(OK.codeAsText()), null);
    }

    public static Http2Response badRequestResponse() {
        return new Http2Response(new DefaultHttp2Headers().status(BAD_REQUEST.codeAsText()), null);
    }

    public static Http2Response notFoundResponse() {
        return new Http2Response(new DefaultHttp2Headers().status(NOT_FOUND.codeAsText()), null);
    }

    public static Http2Response internalServerErrorResponse() {
        return new Http2Response(new DefaultHttp2Headers().status(INTERNAL_SERVER_ERROR.codeAsText()), null);
    }

    public static Http2Response serviceUnavailableErrorResponse() {
        return new Http2Response(new DefaultHttp2Headers().status(SERVICE_UNAVAILABLE.codeAsText()), null);
    }

    public static Http2Headers okJsonHeader(int contentLength) {
        return new DefaultHttp2Headers().status(OK.codeAsText())
                .add(HttpHeaderNames.CONTENT_TYPE, "application/json")
                .add(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(contentLength));
    }

}
