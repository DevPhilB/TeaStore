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
package image.rest.server;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import image.rest.api.Http1ImageAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTP/1.1 server handler for image service
 * @author Philipp Backes
 */
public class Http1ImageServiceHandler extends SimpleChannelInboundHandler<HttpObject> {

    private HttpRequest request;
    private final Http1ImageAPI api;
    private static final Logger LOG = LogManager.getLogger(Http1ImageServiceHandler.class);

    public Http1ImageServiceHandler(String gatewayHost, Integer gatewayPort) {
        api = new Http1ImageAPI(gatewayHost, gatewayPort);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext channelHandlerContext) {
        channelHandlerContext.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        LOG.error("Channel " + context.channel().id() + ": " + cause.getMessage());
        context.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, HttpObject message) {
        if (message instanceof HttpRequest request) {
            this.request = request;
            // Check HTTP method
            if (request.method() != HttpMethod.GET
                && request.method() != HttpMethod.POST) {
                writeStatusResponse(context, METHOD_NOT_ALLOWED);
            }
            if (HttpUtil.is100ContinueExpected(request)) {
                writeContinueResponse(context);
            }
        }

        if (evaluateDecoderResult(request)) {
            writeStatusResponse(context, BAD_REQUEST);
        }

        if (message instanceof HttpContent httpContent) {
            if (evaluateDecoderResult(request)) {
                writeStatusResponse(context, BAD_REQUEST);
            }
            // Trailer response header gets ignored in handler
            if (message instanceof LastHttpContent trailer) {
                writeAPIResponse(context, api.handle(request, httpContent.content().copy(), trailer));
            }
        }
    }

    private void writeStatusResponse(ChannelHandlerContext context, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status);
        context.write(response);
    }

    private void writeContinueResponse(ChannelHandlerContext context) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER);
        context.write(response);
    }

    private void writeAPIResponse(ChannelHandlerContext context, FullHttpResponse httpResponse) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);

        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");

        if (keepAlive) {
            httpResponse.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        context.write(httpResponse);

        if (!keepAlive) {
            context.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private boolean evaluateDecoderResult(HttpObject object) {
        return !object.decoderResult().isSuccess();
    }
}