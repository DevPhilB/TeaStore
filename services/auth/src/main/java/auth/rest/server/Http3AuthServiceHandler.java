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
package auth.rest.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import auth.rest.api.Http3AuthAPI;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utilities.rest.api.Http3Response;

/**
 * HTTP/3 server handler for auth service
 * @author Philipp Backes
 */
public class Http3AuthServiceHandler extends Http3RequestStreamInboundHandler {

    private Http3Headers headers;
    private ByteBuf body = Unpooled.EMPTY_BUFFER;
    private final Http3AuthAPI api;
    private static final Logger LOG = LogManager.getLogger(Http3AuthServiceHandler.class);

    public Http3AuthServiceHandler(String gatewayHost, Integer persistencePort) {
        api = new Http3AuthAPI(gatewayHost, persistencePort);
    }

    private void handleRequest(ChannelHandlerContext context) {
        // Handle request and response
        Http3Response response = api.handle(headers, body);
        sendResponse(context, response);
        context.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        LOG.error("Channel " + context.channel().id() + ": " + cause.getMessage());
    }

    @Override
    protected void channelRead(ChannelHandlerContext context, Http3HeadersFrame headersFrame, boolean isLast) {
        ReferenceCountUtil.release(headersFrame);
        headers = headersFrame.headers();
        if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH) && isLast) {
            handleRequest(context);
        }
    }

    @Override
    protected void channelRead(ChannelHandlerContext context, Http3DataFrame dataFrame, boolean isLast) {
        body = Unpooled.copiedBuffer(body, dataFrame.content().copy());
        ReferenceCountUtil.release(dataFrame);
        if (isLast) {
            handleRequest(context);
        }
    }

    private void sendResponse(ChannelHandlerContext context, Http3Response response) {
        // Send response frames
        if (response.body() == null) {
            context.writeAndFlush(new DefaultHttp3HeadersFrame(response.headers()))
                    .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
        } else {
            context.write(new DefaultHttp3HeadersFrame(response.headers()));
            context.writeAndFlush(new DefaultHttp3DataFrame(response.body()))
                    .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
        }
    }

}
