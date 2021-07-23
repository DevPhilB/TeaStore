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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import image.rest.api.Http2ImageAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utilities.rest.api.Http2Response;

/**
 * HTTP/2 server handler for image service
 * @author Philipp Backes
 */
public class Http2ImageServiceHandler extends ChannelDuplexHandler {

    private Http2Headers headers;
    private ByteBuf body = Unpooled.EMPTY_BUFFER;
    private final Http2ImageAPI api;
    private static final Logger LOG = LogManager.getLogger();

    public Http2ImageServiceHandler(String gatewayHost, Integer gatewayPort) {
        api = new Http2ImageAPI(gatewayHost, gatewayPort);
    }

    private void handleRequest(ChannelHandlerContext context, Http2FrameStream stream) {
        // Handle request and response
        Http2Response response = api.handle(headers, body);
        sendResponse(context, stream, response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        LOG.error("Channel " + context.channel().id() + ": " + cause.getMessage());
        context.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof Http2HeadersFrame headersFrame) {
            headers = headersFrame.headers();
            if (headersFrame.isEndStream()) {
                handleRequest(context, headersFrame.stream());
            }
        } else if (message instanceof Http2DataFrame dataFrame) {
            onDataRead(context, dataFrame);
        } else {
            super.channelRead(context, message);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * Handle data frames
     */
    private void onDataRead(ChannelHandlerContext context, Http2DataFrame data) {
        Http2FrameStream stream = data.stream();
        body = Unpooled.copiedBuffer(body, data.content());
        data.release();
        if (data.isEndStream()) {
            handleRequest(context, stream);
        }
        // Update the flow controller
        context.write(new DefaultHttp2WindowUpdateFrame(data.initialFlowControlledBytes()).stream(stream));
    }

    /**
     * Send response to the client
     */
    private void sendResponse(
            ChannelHandlerContext context,
            Http2FrameStream stream,
            Http2Response response
    ) {
        // Send response frames
        if(response.body() == null) {
            context.writeAndFlush(new DefaultHttp2HeadersFrame(response.headers(), true).stream(stream));
        } else {
            context.write(new DefaultHttp2HeadersFrame(response.headers()).stream(stream));
            context.writeAndFlush(new DefaultHttp2DataFrame(response.body(), true).stream(stream));
        }
    }
}