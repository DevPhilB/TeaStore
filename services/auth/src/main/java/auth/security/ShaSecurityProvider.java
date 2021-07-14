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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import utilities.datamodel.SessionData;

/**
 * Secruity provider using AES.
 * 
 * @author Simon
 *
 */
public class ShaSecurityProvider implements ISecurityProvider {

  @Override
  public IKeyProvider getKeyProvider() {
    return new ConstantKeyProvider();
  }

  @Override
  public SessionData secure(SessionData data) {
    if (data.userId() == null || data.sessionId() == null) {
      return data;
    }
    data = new SessionData(
            data.userId(),
            data.sessionId(),
            null,
            data.order(),
            data.orderItems(),
            data.message()
    );
    String dataString = dataToString(data);
    data = new SessionData(
            data.userId(),
            data.sessionId(),
            getSha512(dataString),
            data.order(),
            data.orderItems(),
            data.message()
    );
    return data;
  }

  private String dataToString(SessionData data) {
    ObjectMapper o = new ObjectMapper();
    try {
      return URLEncoder.encode(o.writeValueAsString(data), "UTF-8");
    } catch (JsonProcessingException | UnsupportedEncodingException e) {
      throw new IllegalStateException("Could not save data!");
    }
  }

  @Override
  public SessionData validate(SessionData data) {
    if (data.token() == null) {
      return null;
    }

    String token = data.token();
    SessionData testData = new SessionData(
            data.userId(),
            data.sessionId(),
            null,
            data.order(),
            data.orderItems(),
            data.message()
    );
    String testDataString = dataToString(testData);
    String validationToken = getSha512(testDataString);
    if (validationToken.equals(token)) {
      return data;
    }
    return null;
  }

  private String getSha512(String passwordToHash) {
    String generatedPassword = null;
    try {
      String salt = getKeyProvider().getKey(null);
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      md.update(salt.getBytes("UTF-8"));
      byte[] bytes = md.digest(passwordToHash.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < bytes.length; i++) {
        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
      }
      generatedPassword = sb.toString();
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return generatedPassword;
  }
}
