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
package utilities.rest.api;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static utilities.rest.api.API.HTTPS;

/**
 * Intermediate HTTP/2 response and helper class
 *
 * @author Philipp Backes
 *
 */
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
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(contentLength));
    }

    public static Http2Headers getHeader(String gatewayHost, String endpoint) {
        return new DefaultHttp2Headers().scheme(HTTPS)
                .method(GET.asciiName()).path(endpoint)
                .set(HttpHeaderNames.HOST, gatewayHost)
                .set(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    }

    public static Http2Headers postHeader(String gatewayHost, String endpoint) {
        return new DefaultHttp2Headers().scheme(HTTPS)
                .method(POST.asciiName()).path(endpoint)
                .set(HttpHeaderNames.HOST, gatewayHost)
                .set(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    }

    public static Http2Headers postContentHeader(String gatewayHost, String endpoint, String contentLength) {
        return new DefaultHttp2Headers().scheme(HTTPS)
                .method(POST.asciiName()).path(endpoint)
                .set(HttpHeaderNames.HOST, gatewayHost)
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.CONTENT_LENGTH, contentLength)
                .set(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    }

    public static Http2Headers putHeader(String gatewayHost, String endpoint) {
        return new DefaultHttp2Headers().scheme(HTTPS)
                .method(PUT.asciiName()).path(endpoint)
                .set(HttpHeaderNames.HOST, gatewayHost)
                .set(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    }

}
