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
package auth.security;

import utilities.datamodel.SessionData;

/**
 * Utilities for securing (e.g. encrypting) session data.
 * 
 * @author Joakim von Kistowski
 *
 */
public interface ISecurityProvider {

  /**
   * Get the key provider for this security provider.
   * 
   * @return The key provider.
   */
  public IKeyProvider getKeyProvider();

  /**
   * Secures a session data. May encrypt or hash values within the data.
   * 
   * @param data
   *          The data to secure.
   * @return A secure data to be passed on to the web ui.
   */
  public SessionData secure(SessionData data);

  /**
   * Validates a secured session data.
   * Returns a valid and readable (e.g., decrypted) data.
   * Returns null for invalid data.
   * 
   * @param data The data to secure.
   * @return The valid and readable (e.g. decrypted) data. Returns null for invalid data.
   */
  public SessionData validate(SessionData data);

}
