/*
 * Copyright 2012 Donghwan Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowersinthesand.chat;

import java.io.IOException;
import java.io.PrintWriter;

import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.BroadcasterFactory;

import com.google.gson.Gson;

public class ChatAtmosphereHandler implements AtmosphereHandler {

	@Override
	public void onRequest(AtmosphereResource resource) throws IOException {
		AtmosphereRequest request = resource.getRequest();
		AtmosphereResponse response = resource.getResponse();

		if (request.getMethod().equalsIgnoreCase("GET")) {
			final String id = request.getParameter("id");
			String transport = request.getParameter("transport");
			final boolean first = "1".equals(request.getParameter("count"));

			response.setCharacterEncoding("utf-8");
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("text/" + ("longpolljsonp".equals(transport) ? "javascript" : "plain"));
			
			resource.addEventListener(new AtmosphereResourceEventListener() {
				@Override
				public void onSuspend(AtmosphereResourceEvent event) {
					if (first) {
						fire(new SocketEvent("open").setSocket(id));
					}
				}

				@Override
				public void onBroadcast(AtmosphereResourceEvent event) {

				}

				@Override
				public void onThrowable(AtmosphereResourceEvent event) {
					cleanup(event);
				}

				@Override
				public void onResume(AtmosphereResourceEvent event) {
					cleanup(event);
				}

				@Override
				public void onDisconnect(AtmosphereResourceEvent event) {
					cleanup(event);
				}

				private void cleanup(AtmosphereResourceEvent event) {
					if (!first && !event.getResource().getResponse().isCommitted()) {
						fire(new SocketEvent("close").setSocket(id));
					}
				}
			});
			resource.suspend(20 * 1000, false);
			if (first) {
				resource.resume();
			}
		} else if (request.getMethod().equalsIgnoreCase("POST")) {
			request.setCharacterEncoding("utf-8");
			response.setHeader("Access-Control-Allow-Origin", "*");

			String data = request.getReader().readLine();
			if (data != null) {
				data = data.substring("data=".length());
				fire(new Gson().fromJson(data, SocketEvent.class));
			}
		}
	}

	@Override
	public void onStateChange(AtmosphereResourceEvent event) throws IOException {
		AtmosphereResource resource = event.getResource();
		AtmosphereRequest request = resource.getRequest();
		AtmosphereResponse response = resource.getResponse();
		if (event.getMessage() == null || event.isCancelled() || request.destroyed()) {
			return;
		}

		String data = new Gson().toJson(event.getMessage());
		PrintWriter writer = response.getWriter();

		if ("longpolljsonp".equals(request.getParameter("transport"))) {
			writer.print(request.getParameter("callback"));
			writer.print("(");
			writer.print(new Gson().toJson(data));
			writer.print(")");
		} else {
			writer.print(data);
		}

		writer.flush();
		resource.resume();
	}

	@Override
	public void destroy() {

	}

	private void fire(SocketEvent event) {
		handle(event);
	}

	private void handle(SocketEvent event) {
		if (event.getType().equals("message")) {
			BroadcasterFactory.getDefault().lookup("/chat").broadcast(new SocketEvent("message").setData(event.getData()));
		}
	}

}
