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
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTTP/1.1 client object handler for inter-service communication
 * @author Philipp Backes
 */
public class Http1ClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    public String jsonContent = "";
    private static final Logger LOG = LogManager.getLogger(Http1ClientHandler.class);

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
        if(message instanceof HttpContent httpContent) {
            if (httpContent instanceof LastHttpContent) {
                jsonContent += httpContent.content().toString(CharsetUtil.UTF_8);
                context.close();
            } else {
                jsonContent += httpContent.content().toString(CharsetUtil.UTF_8);
                context.flush();
            }
        }
    }
}