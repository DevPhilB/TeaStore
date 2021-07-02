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

import org.mindrot.jbcrypt.BCrypt;

/**
 * Wrapper for BCrypt.
 * @author Simon
 *
 */
public final class BCryptProvider {

  /**
   * Hides default constructor.
   */
  private BCryptProvider() {}
  
  /**
   * validate password using BCrypt.
   * @param password password
   * @param password2 other password
   * @return true if password is correct
   */
  public static boolean checkPassword(String password, String password2) {
    return BCrypt.checkpw(password, password2);
  }
}
