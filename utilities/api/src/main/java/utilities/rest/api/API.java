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
package utilities.rest.api;

/**
 * API interface
 *
 * @author Philipp Backes
 *
 */
public interface API {
    // Auth service
    Integer DEFAULT_AUTH_PORT = 1010;
    String AUTH_ENDPOINT = "/api/auth";
    // Image service
    Integer DEFAULT_IMAGE_PORT = 2020;
    String IMAGE_ENDPOINT = "/api/image";
    // Persistence service
    Integer DEFAULT_PERSISTENCE_PORT = 3030;
    String PERSISTENCE_ENDPOINT = "/api/persistence";
    // Recommender service
    Integer DEFAULT_RECOMMENDER_PORT = 4040;
    String RECOMMENDER_ENDPOINT = "/api/recommender";
    // Web service
    Integer DEFAULT_WEB_PORT = 5050;
    String WEB_ENDPOINT = "/api/web";
}