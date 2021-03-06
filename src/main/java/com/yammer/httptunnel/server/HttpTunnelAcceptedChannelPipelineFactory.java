/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.yammer.httptunnel.server;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

import com.yammer.httptunnel.util.HttpTunnelMessageUtils;

/**
 * Creates pipelines for incoming http tunnel connections, capable of decoding
 * the incoming HTTP requests, determining their type (client sending data,
 * client polling data, or unknown) and handling them appropriately.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 * @author Jamie Furness (jamie@onedrum.com)
 * @author OneDrum Ltd.
 */
class HttpTunnelAcceptedChannelPipelineFactory implements ChannelPipelineFactory {

	private final ChannelHandler channelHandler;

	public HttpTunnelAcceptedChannelPipelineFactory(HttpTunnelServerChannel parent) {
		channelHandler = new HttpTunnelAcceptedChannelHandler(parent);
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		final ChannelPipeline pipeline = Channels.pipeline();

		pipeline.addLast("httpResponseEncoder", new HttpResponseEncoder());
		pipeline.addLast("httpRequestDecoder", new HttpRequestDecoder());
		pipeline.addLast("httpChunkAggregator", new HttpChunkAggregator(HttpTunnelMessageUtils.MAX_BODY_SIZE));
		pipeline.addLast("messageSwitchClient", channelHandler);

		return pipeline;
	}
}
