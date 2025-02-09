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
package image.setup;

import java.util.Arrays;

/**
 * This enum contains the different cache implementations and their string representation.
 * @author Norbert Schmitt
 */
public enum CachingMode {

  /**
   * First in first out cache.
   */
  FIFO("FIFO"), 
  /**
   * Last in first out cache.
   */
  LIFO("LIFO"), 
  /**
   * Random replacement cache.
   */
  RR("RR"), 
  /**
   * Least frequently used cache.
   */
  LFU("LFU"), 
  /**
   * Least recently used cache.
   */
  LRU("LRU"), 
  /**
   * Most recently used cache.
   */
  MRU("MRU"), 
  /**
   * Use no cache (Cache disabled).
   */
  NONE("Disabled");

  /**
   * Standard cache implementation used by the image provider service.
   */
  public static final CachingMode STD_CACHING_MODE = LFU;

  private final String strRepresentation;

  private CachingMode(String strRepresentation) {
    this.strRepresentation = strRepresentation;
  }

  /**
   * Returns the string representation of the used cache implementation.
   * @return String representation.
   */
  public String getStrRepresentation() {
    return strRepresentation;
  }

  /**
   * Convert string representation to the correct object. Will return the standard cache implementation if the string 
   * representation is unknown.
   * @param strCachingMode String representation of the cache implementation.
   * @return Enum value of the cache implementation.
   */
  public static CachingMode getCachingModeFromString(String strCachingMode) {
    return Arrays.asList(CachingMode.values()).stream()
        .filter(mode -> mode.strRepresentation.equals(strCachingMode)).findFirst()
        .orElse(STD_CACHING_MODE);
  }
}
