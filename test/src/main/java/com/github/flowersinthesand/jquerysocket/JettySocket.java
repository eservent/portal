package com.github.flowersinthesand.jquerysocket;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class JettySocket extends AbstractSocket {

	private HttpServletRequest request;
	private WebSocket webSocket;
	private Connection connection;

	@Override
	public Socket open() {
		webSocket = new WebSocket.OnTextMessage() {
			@Override
			public void onOpen(Connection c) {
				connection = c;
				fire("open", null);
			}

			@Override
			public void onClose(int closeCode, String message) {
				fire("close", null);
			}

			@Override
			public void onMessage(String json) {
				Map<String, Object> event = new Gson().fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
				fire((String) event.get("type"), event.get("data"));
			}
		};

		return this;
	}
	
	@Override
	protected boolean sendable() {
		return connection != null;
	}

	@Override
	protected void transmit(String data) throws IOException {
		connection.sendMessage(data);
	}

	@Override
	public Socket close() {
		connection.close();
		return this;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
		params.put("id", request.getParameter("id"));
		params.put("heartbeat", request.getParameter("heartbeat"));
	}

	public WebSocket getWebSocket() {
		return webSocket;
	}

	public Connection getConnection() {
		return connection;
	}

}
