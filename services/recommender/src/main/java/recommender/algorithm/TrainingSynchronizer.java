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
package recommender.algorithm;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import utilities.datamodel.*;
import utilities.rest.client.Http1Client;
import utilities.rest.client.Http1ClientHandler;
import utilities.rest.client.Http2Client;
import utilities.rest.client.Http2ClientStreamFrameHandler;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static utilities.rest.api.API.HTTPS;
import static utilities.rest.api.API.DEFAULT_PERSISTENCE_PORT;
import static utilities.rest.api.API.PERSISTENCE_ENDPOINT;

/**
 * This class organizes the communication with the other services and
 * synchronizes on startup and training.
 * Training data could be saved in the db to speed up new instance training.
 *
 * @author Johannes Grohmann
 *
 */
public final class TrainingSynchronizer {

	// HTTP client
	private String httpVersion;
	private ObjectMapper mapper;
	private String gatewayHost;
	private Integer persistencePort;
	private HttpRequest request;
	private Http1Client http1Client;
	private Http1ClientHandler http1Handler;
	private Http2Client http2Client;
	private Http2ClientStreamFrameHandler http2FrameHandler;
	private Http2Headers http2Header;

	/**
	 * This value signals that the maximum training time is not known.
	 */
	public static final long DEFAULT_MAX_TIME_VALUE = Long.MIN_VALUE;

	private static TrainingSynchronizer instance;

	private boolean isReady = false;

	/**
	 * @return the isReady
	 */
	public boolean isReady() {
		return isReady;
	}

	/**
	 * @param isReady
	 *            the isReady to set
	 */
	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}

	private TrainingSynchronizer() {}

	/**
	 * Set up HTTP client for persistence queries
	 */
	public void setupHttpClient(
			String httpVersion,
			String gatewayHost,
			Integer gatewayPort
	) {
		this.httpVersion = httpVersion;
		mapper = new ObjectMapper();
		if(gatewayHost.isEmpty()) {
			this.gatewayHost = "localhost";
			persistencePort = DEFAULT_PERSISTENCE_PORT;
		} else {
			this.gatewayHost = gatewayHost;
			persistencePort = gatewayPort;
		}
		// HTTP/1.1
		request = new DefaultFullHttpRequest(
				HTTP_1_1,
				GET,
				"",
				Unpooled.EMPTY_BUFFER
		);
		request.headers().set(HttpHeaderNames.HOST, this.gatewayHost);
		request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
		// HTTP/2
		http2Header = new DefaultHttp2Headers().scheme(HTTPS);
		http2Header.add(HttpHeaderNames.HOST, this.gatewayHost);
		http2Header.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		http2Header.add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
	}

	/**
	 * Returns the instance for this singleton.
	 *
	 * @return An instance of {@link TrainingSynchronizer}
	 */
	public static synchronized TrainingSynchronizer getInstance() {
		if (instance == null) {
			instance = new TrainingSynchronizer();
		}
		return instance;
	}

	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The maximum considered time in milliseconds. DEFAULT_MAX_TIME_VALUE signals
	 * no entry, e.g. all orders are used for training.
	 */
	private long maxTime = DEFAULT_MAX_TIME_VALUE;

	/**
	 * @return the maxTime
	 */
	public long getMaxTime() {
		return maxTime;
	}

	/**
	 * @param maxTime
	 *            the maxTime to set
	 */
	public void setMaxTime(String maxTime) {
		setMaxTime(toMillis(maxTime));
	}

	/**
	 * @param maxTime
	 *            the maxTime to set
	 */
	public void setMaxTime(long maxTime) {
		this.maxTime = maxTime;
	}

	/**
	 * Connects via REST to the database and retrieves all {@link OrderItem}s and
	 * all {@link Order}s. Then, it triggers the training of the recommender.
	 *
	 * @return The number of elements retrieved from the database or -1 if the
	 *         process failed.
	 */
	public long retrieveDataAndRetrain() {
		setReady(false);
		LOG.trace("Retrieving data objects from database...");

		List<OrderItem> items = new ArrayList<>();
		List<Order> orders = new ArrayList<>();
		// GET api/persistence/orderitems
		String persistenceEndpointOrderItems = PERSISTENCE_ENDPOINT + "/orderitems?start=-1&max=-1";
		// GET api/persistence/orders
		String persistenceEndpointOrders = PERSISTENCE_ENDPOINT + "/orders?start=-1&max=-1";
		// Switch between http versions
		// Retrieve order items and orders
		switch (httpVersion) {
			case "HTTP/1.1":
				try {
					request.setUri(persistenceEndpointOrderItems);
					http1Client = new Http1Client(gatewayHost, persistencePort, request);
					http1Handler = new Http1ClientHandler();
					http1Client.sendRequest(http1Handler);
					if (!http1Handler.jsonContent.isEmpty()) {
						items = mapper.readValue(
								http1Handler.jsonContent,
								new TypeReference<List<OrderItem>>() {}
						);
						long noItems = items.size();
						LOG.trace("Retrieved " + noItems + " orderItems, starting retrieving of orders now.");
					}
				} catch (Exception e) {
					// Set ready anyway to avoid deadlocks
					setReady(true);
					LOG.error("Database retrieving failed.");
					return -1;
				}
				try {
					request.setUri(persistenceEndpointOrders);
					http1Client = new Http1Client(gatewayHost, persistencePort, request);
					http1Handler = new Http1ClientHandler();
					http1Client.sendRequest(http1Handler);
					if (!http1Handler.jsonContent.isEmpty()) {
						orders = mapper.readValue(
								http1Handler.jsonContent,
								new TypeReference<List<Order>>() {}
						);
						long noOrders = orders.size();
						LOG.trace("Retrieved " + noOrders + " orders, starting training now.");
					}
				} catch (Exception e) {
					// Set ready anyway to avoid deadlocks
					setReady(true);
					LOG.error("Database retrieving failed.");
					return -1;
				}
				break;
			case "HTTP/2":
				try {
					http2Header.method(GET.asciiName()).path(persistenceEndpointOrderItems);
					http2Client = new Http2Client(gatewayHost, persistencePort, http2Header, null);
					http2FrameHandler = new Http2ClientStreamFrameHandler();
					http2Client.sendRequest(http2FrameHandler);
					if (!http2FrameHandler.jsonContent.isEmpty()) {
						items = mapper.readValue(
								http2FrameHandler.jsonContent,
								new TypeReference<List<OrderItem>>() {}
						);
						long noItems = items.size();
						LOG.trace("Retrieved " + noItems + " orderItems, starting retrieving of orders now.");
					}
				} catch (Exception e) {
					// Set ready anyway to avoid deadlocks
					setReady(true);
					LOG.error("Database retrieving failed.");
					return -1;
				}
				try {
					http2Header.method(GET.asciiName()).path(persistenceEndpointOrders);
					http2Client = new Http2Client(gatewayHost, persistencePort, http2Header, null);
					http2FrameHandler = new Http2ClientStreamFrameHandler();
					http2Client.sendRequest(http2FrameHandler);
					if (!http2FrameHandler.jsonContent.isEmpty()) {
						orders = mapper.readValue(
								http2FrameHandler.jsonContent,
								new TypeReference<List<Order>>() {}
						);
						long noOrders = orders.size();
						LOG.trace("Retrieved " + noOrders + " orders, starting training now.");
					}
				} catch (Exception e) {
					// Set ready anyway to avoid deadlocks
					setReady(true);
					LOG.error("Database retrieving failed.");
					return -1;
				}
				break;
			case "HTTP/3":
				// TODO
				LOG.info("HTTP/3!");
				break;
			default:
				break;
		}

		// filter lists
		filterLists(items, orders);
		// train instance
		RecommenderSelector.getInstance().train(items, orders);
		LOG.trace("Finished training, ready for recommendation.");
		setReady(true);
		return items.size() + orders.size();
	}

	private void filterLists(List<OrderItem> orderItems, List<Order> orders) {
		if (maxTime == Long.MIN_VALUE) {
			// we are the only known service
			// therefore we find max and set it
			for (Order or : orders) {
				maxTime = Math.max(maxTime, toMillis(or.time()));
			}
		}
		filterForMaxTimeStamp(orderItems, orders);
	}

	private void filterForMaxTimeStamp(List<OrderItem> orderItems, List<Order> orders) {
		// filter orderItems and orders and ignore newer entries.
		List<Order> remove = new ArrayList<>();
		for (Order or : orders) {
			if (toMillis(or.time()) > maxTime) {
				remove.add(or);
			}
		}
		orders.removeAll(remove);

		List<OrderItem> removeItems = new ArrayList<>();
		for (OrderItem orderItem : orderItems) {
			boolean contained = false;
			for (Order or : orders) {
				if (or.id().equals(orderItem.orderId())) {
					contained = true;
					break;
				}
			}
			if (!contained) {
				removeItems.add(orderItem);
			}
		}
		orderItems.removeAll(removeItems);
	}

	private long toMillis(String date) {
		TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(date);
		LocalDateTime localDateTime = LocalDateTime.from(temporalAccessor);
		ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
		Instant instant = Instant.from(zonedDateTime);
		return instant.toEpochMilli();
	}

}
