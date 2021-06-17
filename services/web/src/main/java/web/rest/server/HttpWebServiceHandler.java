package web.rest.server;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import utilities.netty.RequestUtils;
import web.rest.WebAPI;

public class HttpWebServiceHandler extends SimpleChannelInboundHandler<HttpObject> {

    private HttpRequest request;
    StringBuilder responseData = new StringBuilder();

    @Override
    public void channelReadComplete(ChannelHandlerContext channelHandlerContext) {
        channelHandlerContext.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, HttpObject message) {
        if (message instanceof HttpRequest request) {
            this.request = request;
            // Check HTTP method
            if(request.method() != HttpMethod.GET
                && request.method() != HttpMethod.POST
                && request.method() != HttpMethod.PUT
                && request.method() != HttpMethod.DELETE) {
                writeMethodNotAllowedResponse(context);
            }
            if (HttpUtil.is100ContinueExpected(request)) {
                writeResponse(context);
            }

            responseData.setLength(0);
            WebAPI api = new WebAPI();
            writeAPIResponse(context, api.handle(request));
            return;
            // TODO: Handle body and last content/trailer
            //responseData.append(RequestUtils.formatParams(request));
        }

        responseData.append(RequestUtils.evaluateDecoderResult(request));

        if (message instanceof HttpContent httpContent) {

            responseData.append(RequestUtils.formatBody(httpContent));
            responseData.append(RequestUtils.evaluateDecoderResult(request));

            if (message instanceof LastHttpContent) {
                LastHttpContent trailer = (LastHttpContent) message;
                responseData.append(RequestUtils.prepareLastResponse(request, trailer));
                writeResponse(context, trailer, responseData);
            }
        }
    }

    private void writeMethodNotAllowedResponse(ChannelHandlerContext context) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
        context.write(response);
    }

    private void writeResponse(ChannelHandlerContext context) {
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

    private void writeResponse(ChannelHandlerContext context, LastHttpContent trailer, StringBuilder responseData) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);

        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, ((HttpObject) trailer).decoderResult()
            .isSuccess() ? OK : BAD_REQUEST, Unpooled.copiedBuffer(responseData.toString(), CharsetUtil.UTF_8));

        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            httpResponse.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        context.write(httpResponse);

        if (!keepAlive) {
            context.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }
}