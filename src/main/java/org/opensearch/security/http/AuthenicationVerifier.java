package org.opensearch.security.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.security.filter.SecurityRequest;
import org.opensearch.security.filter.SecurityRequestFactory;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;

public class AuthenicationVerifier extends ChannelInboundHandlerAdapter {

    final static Logger log = LogManager.getLogger(AuthenicationVerifier.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpRequest)) {
            ctx.fireChannelRead(msg);
        }

        HttpRequest request = (HttpRequest) msg;
        if (!isAuthenticated(request)) {
            final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            ReferenceCountUtil.release(msg);
        } else {
            // Lets the request pass to the next channel handler
            ctx.fireChannelRead(msg);
        }
    }

    private boolean isAuthenticated(HttpRequest request) {

        final SecurityRequest securityRequset = SecurityRequestFactory.from(request);

        log.info("Checking if request is authenticated:\n" + request);

        final boolean shouldBlock = request.headers().contains("blockme");

        return !shouldBlock;
    }

}
