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

package com.yammer.httptunnel.util;

import java.io.UnsupportedEncodingException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * Utility class for creating http requests for the operation of the full duplex
 * http tunnel, and verifying that received requests are of the correct types.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 * @author Jamie Furness (jamie@onedrum.com)
 * @author OneDrum Ltd.
 */
public class HttpTunnelMessageUtils {

	public static final HttpVersion HTTP_VERSION = HttpVersion.HTTP_1_0;
	public static final int MAX_BODY_SIZE = 1024 * 1024; // 1Mb

	private static final String OPEN_TUNNEL_REQUEST_URI = "/http-tunnel/open";
	private static final String CLOSE_TUNNEL_REQUEST_URI = "/http-tunnel/close";
	private static final String CLIENT_SEND_REQUEST_URI = "/http-tunnel/send";
	private static final String CLIENT_RECV_REQUEST_URI = "/http-tunnel/poll";

	public static HttpRequest createOpenTunnelRequest(SocketAddress host, String userAgent) {
		return createOpenTunnelRequest(convertToHostString(host), userAgent);
	}

	public static HttpRequest createOpenTunnelRequest(String host, String userAgent) {
		final HttpRequest request = createRequestTemplate(host, null, OPEN_TUNNEL_REQUEST_URI, userAgent);

		setNoData(request);

		return request;
	}

	public static boolean isOpenTunnelRequest(HttpRequest request, String userAgent) {
		return isRequestTo(request, OPEN_TUNNEL_REQUEST_URI, userAgent);
	}

	public static boolean checkHost(HttpRequest request, SocketAddress expectedHost) {
		final String host = request.getHeader(HttpHeaders.Names.HOST);
		return expectedHost == null ? host == null : convertToHostString(expectedHost).equals(host);
	}

	public static HttpRequest createSendDataRequest(SocketAddress host, String cookie, ChannelBuffer data, String userAgent) {
		return createSendDataRequest(convertToHostString(host), cookie, data, userAgent);
	}

	public static HttpRequest createSendDataRequest(String host, String cookie, ChannelBuffer data, String userAgent) {
		final HttpRequest request = createRequestTemplate(host, cookie, CLIENT_SEND_REQUEST_URI, userAgent);

		request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, Long.toString(data.readableBytes()));
		request.setContent(data);

		return request;
	}

	public static boolean isSendDataRequest(HttpRequest request, String userAgent) {
		return isRequestTo(request, CLIENT_SEND_REQUEST_URI, userAgent);
	}

	public static HttpRequest createReceiveDataRequest(SocketAddress host, String tunnelId, String userAgent) {
		return createReceiveDataRequest(convertToHostString(host), tunnelId, userAgent);
	}

	public static HttpRequest createReceiveDataRequest(String host, String tunnelId, String userAgent) {
		final HttpRequest request = createRequestTemplate(host, tunnelId, CLIENT_RECV_REQUEST_URI, userAgent);

		setNoData(request);

		return request;
	}

	public static boolean isReceiveDataRequest(HttpRequest request, String userAgent) {
		return isRequestTo(request, CLIENT_RECV_REQUEST_URI, userAgent);
	}

	public static HttpRequest createCloseTunnelRequest(String host, String tunnelId, String userAgent) {
		final HttpRequest request = createRequestTemplate(host, tunnelId, CLOSE_TUNNEL_REQUEST_URI, userAgent);

		setNoData(request);

		return request;
	}

	public static boolean isCloseTunnelRequest(HttpRequest request, String userAgent) {
		return isRequestTo(request, CLOSE_TUNNEL_REQUEST_URI, userAgent);
	}

	public static boolean isServerToClientRequest(HttpRequest request, String userAgent) {
		return isRequestTo(request, CLIENT_RECV_REQUEST_URI, userAgent);
	}

	public static String convertToHostString(SocketAddress hostAddress) {
		final StringBuilder host = new StringBuilder();

		final InetSocketAddress inetSocketAddr = (InetSocketAddress) hostAddress;
		final InetAddress addr = inetSocketAddr.getAddress();

		if (addr instanceof Inet6Address) {
			host.append('[');
			host.append(addr.getHostAddress());
			host.append(']');
		}
		else if (addr != null) {
			host.append(addr.getHostAddress());
		}
		else {
			host.append(inetSocketAddr.getHostName());
		}

		host.append(':');
		host.append(inetSocketAddr.getPort());

		return host.toString();
	}

	private static HttpRequest createRequestTemplate(String host, String tunnelId, String uri, String userAgent) {
		final HttpRequest request = new DefaultHttpRequest(HTTP_VERSION, HttpMethod.POST, createCompleteUri(host, uri));

		request.setHeader(HttpHeaders.Names.HOST, host);
		request.setHeader(HttpHeaders.Names.USER_AGENT, userAgent);

		if (tunnelId != null)
			request.setHeader(HttpHeaders.Names.COOKIE, tunnelId);

		return request;
	}

	private static String createCompleteUri(String host, String uri) {
		final StringBuilder builder = new StringBuilder(7 + host.length() + uri.length());

		builder.append("http://");
		builder.append(host);
		builder.append(uri);

		return builder.toString();
	}

	private static boolean isRequestTo(HttpRequest request, String uri, String userAgent) {
		final URI decodedUri;
		try {
			decodedUri = new URI(request.getUri());
		}
		catch (URISyntaxException e) {
			return false;
		}

		return userAgent.equals(request.getHeader(HttpHeaders.Names.USER_AGENT)) && HttpMethod.POST.equals(request.getMethod()) && uri.equals(decodedUri.getPath());
	}

	private static void setNoData(HttpRequest request) {
		request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, "0");
		request.setContent(null);
	}

	public static String extractTunnelId(HttpRequest request) {
		return request.getHeader(HttpHeaders.Names.COOKIE);
	}

	private static byte[] toBytes(String string) {
		try {
			return string.getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			// UTF-8 is meant to be supported on all platforms
			throw new RuntimeException("UTF-8 encoding not supported!");
		}
	}

	public static HttpResponse createTunnelOpenResponse(String tunnelId) {
		final HttpResponse response = createResponseTemplate(HttpResponseStatus.CREATED, null);

		response.setHeader(HttpHeaders.Names.SET_COOKIE, tunnelId);

		return response;
	}

	public static boolean isTunnelOpenResponse(HttpResponse response) {
		return isResponseWithCode(response, HttpResponseStatus.CREATED);
	}

	public static boolean isProxyAuthResponse(HttpResponse response) {
		return isResponseWithCode(response, HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED);
	}

	public static boolean isOKResponse(HttpResponse response) {
		return isResponseWithCode(response, HttpResponseStatus.OK);
	}

	public static HttpResponse createTunnelPingResponse(String tunnelId) {
		final HttpResponse response = createResponseTemplate(HttpResponseStatus.NO_CONTENT, null);

		response.setHeader(HttpHeaders.Names.SET_COOKIE, tunnelId);

		return response;
	}

	public static boolean isPingResponse(HttpResponse response) {
		return isResponseWithCode(response, HttpResponseStatus.NO_CONTENT);
	}

	public static boolean hasContents(HttpResponse response, byte[] expectedContents) {
		if (response.getContent() != null && HttpHeaders.getContentLength(response, 0) == expectedContents.length && response.getContent().readableBytes() == expectedContents.length) {
			final byte[] compareBytes = new byte[expectedContents.length];
			response.getContent().readBytes(compareBytes);
			return Arrays.equals(expectedContents, compareBytes);
		}

		return false;
	}

	public static HttpResponse createTunnelCloseResponse() {
		return createResponseTemplate(HttpResponseStatus.RESET_CONTENT, null);
	}

	public static boolean isTunnelCloseResponse(HttpResponse response) {
		return isResponseWithCode(response, HttpResponseStatus.RESET_CONTENT);
	}

	public static String extractCookie(HttpResponse response) {
		if (response.containsHeader(HttpHeaders.Names.SET_COOKIE))
			return response.getHeader(HttpHeaders.Names.SET_COOKIE);

		return null;
	}

	public static HttpResponse createSendDataResponse() {
		return createOKResponseTemplate(null);
	}

	public static HttpResponse createRecvDataResponse(ChannelBuffer data) {
		return createOKResponseTemplate(data);
	}

	public static HttpResponse createRejection(HttpRequest request, String reason) {
		final HttpVersion version = request != null ? request.getProtocolVersion() : HTTP_VERSION;
		final ChannelBuffer reasonBuffer = ChannelBuffers.wrappedBuffer(toBytes(reason));
		final HttpResponse response = new DefaultHttpResponse(version, HttpResponseStatus.BAD_REQUEST);

		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=\"utf-8\"");
		response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, Integer.toString(reasonBuffer.readableBytes()));
		response.setContent(reasonBuffer);

		return response;
	}

	public static boolean isRejection(HttpResponse response) {
		return !HttpResponseStatus.OK.equals(response.getStatus());
	}

	public static Object extractErrorMessage(HttpResponse response) {
		if (response.getContent() == null || HttpHeaders.getContentLength(response, 0) == 0)
			return "";

		final byte[] bytes = new byte[response.getContent().readableBytes()];
		response.getContent().readBytes(bytes);

		try {
			return new String(bytes, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	private static boolean isResponseWithCode(HttpResponse response, HttpResponseStatus status) {
		return status.equals(response.getStatus());
	}

	private static HttpResponse createOKResponseTemplate(ChannelBuffer data) {
		return createResponseTemplate(HttpResponseStatus.OK, data);
	}

	private static HttpResponse createResponseTemplate(HttpResponseStatus status, ChannelBuffer data) {
		final HttpResponse response = new DefaultHttpResponse(HTTP_VERSION, status);

		if (data != null) {
			response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, Integer.toString(data.readableBytes()));
			response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/octet-stream");
		}
		else
			response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, "0");

		response.setContent(data);

		return response;
	}
}
