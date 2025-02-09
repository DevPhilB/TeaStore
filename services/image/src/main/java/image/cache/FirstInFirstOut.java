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
package image.cache;

import java.util.function.Predicate;

import image.cache.entry.ICachable;
import image.cache.rules.CacheAll;
import image.storage.IDataStorage;

/**
 * FIFO cache implementation.
 * @author Norbert Schmitt
 *
 * @param <T> Entry Type implementing ICachable.
 */
public class FirstInFirstOut<T extends ICachable<T>> extends AbstractQueueCache<T> {

  /**
   * FIFO cache standard constructor setting the maximum cache size to the standard value 
   * {@link image.cache.IDataCache.STD_MAX_CACHE_SIZE} and allowing all data to be cached.
   */
  public FirstInFirstOut() {
    this(IDataCache.STD_MAX_CACHE_SIZE);
  }

  /**
   * FIFO cache constructor setting the maximum cache size to the given size and allowing all data to be cached.
   * @param maxCacheSize Maximum cache size in bytes.
   */
  public FirstInFirstOut(long maxCacheSize) {
    this(maxCacheSize, new CacheAll<T>());
  }

  /**
   * FIFO cache constructor setting the maximum cache size to the given size and caching only data that is tested true 
   * for the given caching rule.
   * @param maxCacheSize Maximum cache size in bytes.
   * @param cachingRule Cache rule determining which data will be cached.
   */
  public FirstInFirstOut(long maxCacheSize, Predicate<T> cachingRule) {
    this(null, maxCacheSize, cachingRule);
  }

  /**
   * FIFO cache constructor setting the maximum cache size to the given size and caching only data that is tested true 
   * for the given caching rule. This constructor also lets you set the underlying storage, queried if an entry is not 
   * found in the cache.
   * @param cachedStorage Storage object to query if an entry is not found in the cache.
   * @param maxCacheSize Maximum cache size in bytes.
   * @param cachingRule Cache rule determining which data will be cached.
   */
  public FirstInFirstOut(IDataStorage<T> cachedStorage, long maxCacheSize,
      Predicate<T> cachingRule) {
    super(cachedStorage, maxCacheSize, cachingRule);
  }

  @Override
  protected void removeEntryByCachingStrategy() {
    dataRemovedFromCache(getEntries().pollFirst().getData().getByteSize());
  }

}
