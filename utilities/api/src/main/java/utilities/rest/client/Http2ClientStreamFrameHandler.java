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
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.util.CharsetUtil;

/**
 * HTTP/2 client stream frame handler for inter-service communication
 * @author Philipp Backes
 */
public class Http2ClientStreamFrameHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {
    public String jsonContent = "";

    @Override
    protected void channelRead0(ChannelHandlerContext context, Http2StreamFrame message) throws Exception {
        System.out.println("Received HTTP/2 'stream' frame: " + message);

        // isEndStream() is not from a common interface, so we currently must check both
        if (message instanceof Http2DataFrame dataFrame && dataFrame.isEndStream()) {
            System.out.println("Received HTTP/2 header frame: " + dataFrame.toString());
            jsonContent += dataFrame.content().toString(CharsetUtil.UTF_8);
        } else if (message instanceof Http2HeadersFrame headersFrame && headersFrame.isEndStream()) {
            System.out.println("Received HTTP/2 header frame: " + headersFrame.toString());
        }
    }
}
