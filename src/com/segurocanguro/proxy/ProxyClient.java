package com.segurocanguro.proxy;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
		/*for (Iterator<Entry<String, String>> iterator = clientRequest.getHeaders().entries().iterator(); iterator.hasNext();) {
			Entry<String, String> entry = iterator.next();
			request.withHeader(entry.getKey(), entry.getValue());
		}*/
		printRequestHeaders(request);
		Observable<ResponseHolder<ByteBuf>> response = rxClient.submit(request)
				.lift(FlatResponseOperator.<ByteBuf>flatResponse())
				.map(new Func1<ResponseHolder<ByteBuf>, ResponseHolder<ByteBuf>>() {
                    @Override
                    public ResponseHolder<ByteBuf> call(ResponseHolder<ByteBuf> holder) {
                    	String content = holder.getContent().toString(Charset.defaultCharset());
                    	System.out.println(content);
                        JSONObject json = new JSONObject(content);
                		JSONObject array = json.getJSONArray("fields").getJSONObject(0);
                		JSONArray sortedArray = sortResults(array.toString());
                    	System.out.println(sortedArray.toString());
                        return holder;
                    }
				});
		return response.toBlocking().single();
	}
	
	public JSONArray sortResults(String content){
		JSONObject json = new JSONObject(content);
		JSONArray jsonArr = new JSONArray(json.getJSONArray("values").toString());
		JSONArray sortedJsonArray = new JSONArray();

	    List<JSONObject> jsonValues = new ArrayList<JSONObject>();
	    for (int i = 0; i < jsonArr.length(); i++) {
	        jsonValues.add(jsonArr.getJSONObject(i));
	    }
	    Collections.sort( jsonValues, new Comparator<JSONObject>() {
	        private static final String KEY_NAME = "id";

	        @Override
	        public int compare(JSONObject a, JSONObject b) {
	            String valA = new String();
	            String valB = new String();

	            try {
	                valA = (String) a.get(KEY_NAME);
	                valB = (String) b.get(KEY_NAME);
	            } 
	            catch (JSONException e) {}

	            return valA.compareTo(valB);
	        }
	    });

	    for (int i = 0; i < jsonArr.length(); i++) {
	        sortedJsonArray.put(jsonValues.get(i));
	    }
	    return sortedJsonArray;
	}
	
	public void printRequestHeaders(HttpClientRequest<ByteBuf> request) {
		System.out.println("Request Headers");
		for (Map.Entry<String, String> header : request.getHeaders().entries()) {
			System.out.println(header.getKey() + ": " + header.getValue());
		}
		System.out.println();
	}
}
