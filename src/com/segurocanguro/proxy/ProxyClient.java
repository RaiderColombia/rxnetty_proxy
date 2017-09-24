package com.segurocanguro.proxy;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.ssl.DefaultFactories;
import io.reactivex.netty.protocol.http.client.FlatResponseOperator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.ResponseHolder;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import rx.Observable;
import rx.functions.Func1;

public class ProxyClient {

	private static final int PORT = 443;
	private static final String HOST = "segurocanguro.com";

	public ResponseHolder<ByteBuf> sendRequest(HttpServerRequest<ByteBuf> clientRequest) throws Exception {
		HttpClient<ByteBuf, ByteBuf> rxClient = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder(HOST, PORT)
				.withSslEngineFactory(DefaultFactories.trustAll()).build();
				
		HttpClientRequest<ByteBuf> request = HttpClientRequest.create(clientRequest.getHttpMethod(),clientRequest.getUri());
		for (Iterator<Entry<String, String>> iterator = clientRequest.getHeaders().entries().iterator(); iterator.hasNext();) {
			Entry<String, String> entry = iterator.next();
			request.withHeader(entry.getKey(), entry.getValue());
		}
		printRequestHeaders(request);
		Observable<ResponseHolder<ByteBuf>> response = rxClient.submit(request)
				.lift(FlatResponseOperator.<ByteBuf>flatResponse())
				.map(new Func1<ResponseHolder<ByteBuf>, ResponseHolder<ByteBuf>>() {
                    @Override
                    public ResponseHolder<ByteBuf> call(ResponseHolder<ByteBuf> holder) {
                        System.out.println(holder.getContent().toString(Charset.defaultCharset()));
                        System.out.println("");
                        return holder;
                    }
				});
		return response.toBlocking().single();
	}
	
	public void printRequestHeaders(HttpClientRequest<ByteBuf> request) {
		System.out.println("Request Headers");
		for (Map.Entry<String, String> header : request.getHeaders().entries()) {
			System.out.println(header.getKey() + ": " + header.getValue());
		}
		System.out.println();
	}
}
