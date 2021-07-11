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
package recommender.rest.server;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import recommender.rest.api.RecommenderAPI;

/**
 * HTTP server handler for recommender service
 * @author Philipp Backes
 */
public class HttpRecommenderServiceHandler extends SimpleChannelInboundHandler<HttpObject> {

    private HttpRequest request;
    private final HttpVersion httpVersion;
    private final RecommenderAPI api;

    public HttpRecommenderServiceHandler(HttpVersion httpVersion, String gatewayHost, Integer gatewayPort) {
        this.httpVersion = httpVersion;
        api = new RecommenderAPI(httpVersion, gatewayHost, gatewayPort);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext channelHandlerContext) {
        channelHandlerContext.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
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
                writeAPIResponse(context, api.handle(request, httpContent.content(), trailer));
            }
        }
    }

    private void writeStatusResponse(ChannelHandlerContext context, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, status);
        context.write(response);
    }

    private void writeContinueResponse(ChannelHandlerContext context) {
        FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, CONTINUE, Unpooled.EMPTY_BUFFER);
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