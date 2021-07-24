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
package utilities.rest.client;

import io.netty.channel.*;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTP/2 client stream frame handler for inter-service communication
 * @author Philipp Backes
 */
public class Http2ClientStreamFrameHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {

    private Channel channel;
    public String jsonContent = "";
    private static final Logger LOG = LogManager.getLogger(Http2ClientStreamFrameHandler.class);

    public void setCloseableChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) {
        context.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        LOG.error("Channel " + context.channel().id() + ": " + cause.getMessage());
        context.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Http2StreamFrame message) {
        LOG.info("Received HTTP/2 'stream' frame: " + message);
        if (message instanceof Http2DataFrame dataFrame) {
            LOG.info("Received HTTP/2 data frame: " + dataFrame);
            jsonContent += dataFrame.content().toString(CharsetUtil.UTF_8);
            if (dataFrame.isEndStream()) {
                LOG.info("Received end data frame: " + dataFrame);
                channel.close();
            }
        } else if (message instanceof Http2HeadersFrame headersFrame && headersFrame.isEndStream()) {
            LOG.info("Received end header frame: " + headersFrame);
        }
    }
}
