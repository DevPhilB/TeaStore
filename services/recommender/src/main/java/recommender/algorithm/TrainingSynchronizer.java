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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import utilities.datamodel.*;
import utilities.rest.client.HttpClient;
import utilities.rest.client.HttpClientHandler;

import static utilities.rest.api.API.PERSISTENCE_ENDPOINT;

/**
 * This class organizes the communication with the other services and
 * synchronizes on startup and training.
 *
 * @author Johannes Grohmann
 *
 */
public final class TrainingSynchronizer {

	// HTTP client
	private String scheme;
	private String gatewayHost;
	private Integer persistencePort;
	private HttpRequest request;
	private HttpClient httpClient;
	private HttpClientHandler handler;
	private ObjectMapper mapper;

	/**
	 * This value signals that the maximum training time is not known.
	 */
	public static final long DEFAULT_MAX_TIME_VALUE = Long.MIN_VALUE;

	// Longest wait period before querying the persistence again if it is finished
	// creating entries
	private static final int PERSISTENCE_CREATION_MAX_WAIT_TIME = 120000;
	// Wait time in ms before checking again for an existing persistence service
	private static final List<Integer> PERSISTENCE_CREATION_WAIT_TIME = Arrays.asList(
			1000,
			2000,
			5000,
			10000,
			30000,
			60000
	);

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
			HttpVersion version,
			String scheme,
			String gatewayHost,
			Integer persistencePort
	) {
		this.mapper = new ObjectMapper();
		this.scheme = scheme;
		this.gatewayHost = gatewayHost;
		this.persistencePort = persistencePort;
		this.request = new DefaultFullHttpRequest(
				version,
				HttpMethod.GET,
				"",
				Unpooled.EMPTY_BUFFER
		);
		this.request.headers().set(HttpHeaderNames.HOST, this.gatewayHost);
		this.request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		this.request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
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

	private static final Logger LOG = LogManager.getLogger(TrainingSynchronizer.class);

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
		String persistenceEndpointOrderItems = PERSISTENCE_ENDPOINT +
				"/orderitems?start=-1&max=-1";
		// GET api/persistence/orders
		String persistenceEndpointOrders = PERSISTENCE_ENDPOINT +
				"/orders?start=-1&max=-1";
		// Retrieve order items
		try {
			request.setUri(persistenceEndpointOrderItems);
			httpClient = new HttpClient(gatewayHost, persistencePort, request);
			handler = new HttpClientHandler();
			httpClient.sendRequest(handler);
			if (!handler.jsonContent.isEmpty()) {
				items = mapper.readValue(
						handler.jsonContent,
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
		// Retrieve orders
		try {
			request.setUri(persistenceEndpointOrders);
			httpClient = new HttpClient(gatewayHost, persistencePort, request);
			handler = new HttpClientHandler();
			httpClient.sendRequest(handler);
			if (!handler.jsonContent.isEmpty()) {
				orders = mapper.readValue(
						handler.jsonContent,
						new TypeReference<List<Order>>() {}
				);
				long noOrders = orders.size();
				LOG.trace("Retrieved " + noOrders + " orders, starting training now.");
			}
		} catch (Exception e) {
			// set ready anyway to avoid deadlocks
			setReady(true);
			LOG.error("Database retrieving failed.");
			return -1;
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
		// TODO: To use existing training data, save them in db
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
