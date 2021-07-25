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
package recommender.rest.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import recommender.algorithm.RecommenderSelector;
import recommender.algorithm.TrainingSynchronizer;
import utilities.datamodel.*;
import utilities.rest.api.API;

import java.util.*;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * API for recommender service
 * /api/recommender
 *
 * @author Philipp Backes
 */
public class Http1RecommenderAPI implements API {
    private final ObjectMapper mapper;
    private static final Logger LOG = LogManager.getLogger(Http1RecommenderAPI.class);

    public Http1RecommenderAPI(String gatewayHost, Integer gatewayPort) {
        this.mapper = new ObjectMapper();
    }

    public FullHttpResponse handle(HttpRequest header, ByteBuf body, LastHttpContent trailer) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(header.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String method = header.method().name();
        String path = queryStringDecoder.path();

        // Select endpoint
        if (path.startsWith("/api/recommender")) {
            String subPath = path.substring("/api/recommender".length());
            switch (method) {
                case "GET":
                    switch (subPath) {
                        case "/train":
                            return train();
                        case "/train/timestamp":
                            return getTimeStamp();
                        case "/train/isready":
                            return isReady();
                    }
                case "POST":
                    switch (subPath) {
                        case "/recommend":
                            if (params.containsKey("userid")) {
                                Long userId = Long.parseLong(params.get("userid").get(0));
                                return getRecommendedProducts(userId, body, false);
                            } else {
                                return getRecommendedProducts(null, body, false);
                            }
                        case "/recommendsingle":
                            if (params.containsKey("userid")) {
                                Long userId = Long.parseLong(params.get("userid").get(0));
                                return getRecommendedProducts(userId, body, true);
                            } else {
                                return getRecommendedProducts(null, body, true);
                            }
                    };
                default:
                    break;
            }
        }
        return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
    }

    /**
     * POST /recommend
     * OR
     * POST /recommendsingle
     *
     * Return a list of all {@link Product}s, that are recommended for the given
     * {@link User} buying the given list of {@link OrderItem}s.
     * <br>
     * The returning list does not contain any {@link Product} that is already part
     * of the given list of {@link OrderItem}s. It might be empty, however.
     *
     * @param userId The id of the {@link User} to recommend for. May be null.
     * @param body List containing all {@link OrderItem}s in the current cart or
     *             an {@link OrderItem} to use as recommender as JSON.
     * @param singleItem Expect list of or single {@link OrderItem}.
     * @return List of {@link Long} objects as JSON, containing all {@link Product} IDs that
     *         are recommended to add to the cart or an INTERNALSERVERERROR, if the recommendation failed.
     */
    private FullHttpResponse getRecommendedProducts(Long userId, ByteBuf body, Boolean singleItem) {
        byte[] jsonByte = new byte[body.readableBytes()];
        body.readBytes(jsonByte);
        try {
            List<OrderItem> currentItems = new ArrayList<>();
            if (singleItem) {
                OrderItem item = mapper.readValue(jsonByte, OrderItem.class);
                currentItems.add(item);
            } else {
                currentItems = mapper.readValue(
                        jsonByte,
                        new TypeReference<List<OrderItem>>(){}
                );
            }
            List<Long> recommended = RecommenderSelector.getInstance().recommendProducts(userId, currentItems);
            String json = mapper.writeValueAsString(recommended);
            return new DefaultFullHttpResponse(
                    HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /train
     *
     * Triggers the training of the recommendation algorithm.
     * It retrieves all data order items and all orders from the database entity
     * and is therefore both very network and computation time intensive.
     * <br>
     * This method must be called before the endpoint is usable,
     * as the IRecommender will throw an UnsupportedOperationException.
     * <br>
     * Calling this method a second time initiates a new training process from scratch.
     *
     * @return OK or INTERNAL_SERVER_ERROR
     */
    private FullHttpResponse train() {
        try {
            long start = System.currentTimeMillis();
            long number = TrainingSynchronizer.getInstance().retrieveDataAndRetrain();
            long time = System.currentTimeMillis() - start;
            if (number != -1) {
                LOG.info(
                        "The (re)train was succesfully done. It took " + time + "ms and "
                                + number + " of Orderitems were retrieved from the database."
                );
                return new DefaultFullHttpResponse(HTTP_1_1, OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.error("The (re)trainprocess failed.");
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /train/timestamp
     *
     * Returns the last time stamp, which was considered at the training of this instance.
     *
     * @return Timestamp or INTERNAL_SERVER_ERROR
     */
    private FullHttpResponse getTimeStamp() {
        if (TrainingSynchronizer.getInstance().getMaxTime() == TrainingSynchronizer.DEFAULT_MAX_TIME_VALUE) {
            LOG.error("The collection of the current maxTime was not possible.");
            return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
        }
        try {
            String json = mapper.writeValueAsString(TrainingSynchronizer.getInstance().getMaxTime());
            return new DefaultFullHttpResponse(
                    HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /train/isready
     *
     * This methods checks, if the service is ready to serve recommendation requests,
     * i.e., if the algorithm has finished training and no retraining process is running.
     * However, this does not imply that issuing a recommendation will fail, if this method returns false.
     * For example, if a retraining is issued,
     * the old trained instance might still answer issued requests until the new instance is fully trained.
     * However, performance behavior is probably influenced.
     *
     * @return True or false
     */
    private FullHttpResponse isReady() {
        Boolean ready = TrainingSynchronizer.getInstance().isReady();
        try {
            String json = mapper.writeValueAsString(ready);
            return new DefaultFullHttpResponse(
                    HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
    }
}
