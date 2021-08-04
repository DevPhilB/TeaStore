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

import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTP/3 client stream inbound handler for inter-service communication
 * @author Philipp Backes
 */
public class Http3ClientStreamInboundHandler extends Http3RequestStreamInboundHandler {

    public String jsonContent = "";
    private static final Logger LOG = LogManager.getLogger(Http3ClientStreamInboundHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        LOG.error("Channel " + context.channel().id() + ": " + cause.getMessage());
    }

    @Override
    protected void channelRead(ChannelHandlerContext context, Http3HeadersFrame headersFrame, boolean isLast) {
        ReferenceCountUtil.release(headersFrame);
        if (isLast) {
            LOG.error("Missing content!");
        }
    }

    @Override
    protected void channelRead(ChannelHandlerContext context, Http3DataFrame dataFrame, boolean isLast) {
        jsonContent += dataFrame.content().toString(CharsetUtil.UTF_8);
        ReferenceCountUtil.release(dataFrame);
        if (isLast) {
            context.close();
        }
    }

}
