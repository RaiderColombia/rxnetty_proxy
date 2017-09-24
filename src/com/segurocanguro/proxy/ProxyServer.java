package com.segurocanguro.proxy;

import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.ResponseHolder;
import io.reactivex.netty.protocol.http.server.HttpServer;

public class ProxyServer {

	static final int PORT = 8080;

	public HttpServer<ByteBuf, ByteBuf> createServer() {
		return RxNetty.createHttpServer(PORT, (request, response) -> {
			ResponseHolder<ByteBuf> obs = null;
			try {
				obs = new ProxyClient().sendRequest(request);
			} catch (Exception e) {
				e.printStackTrace();
			}
			printResponseHeaders(obs.getResponse());
			response.write(obs.getContent().retain());
			return response.close();
		});
	}
	
	public void printResponseHeaders(HttpClientResponse<ByteBuf> response) {
		System.out.println("Response Headers");
		for (Map.Entry<String, String> header : response.getHeaders().entries()) {
			System.out.println(header.getKey() + ": " + header.getValue());
		}
		System.out.println();
	}

	public static void main(final String[] args) {
		System.out.println("HTTP hello world server starting ...");
		new ProxyServer().createServer().startAndWait();
	}

}