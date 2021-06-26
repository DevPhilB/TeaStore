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
package persistence.database;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostRemove;
import utilities.datamodel.Category;

/**
 * Entity for persisting Categories in database.
 * @author Joakim von Kistowski
 *
 */
@Entity
public class PersistenceCategory {

	@Id
	@GeneratedValue
	private long id;
	
	@Column(length = 100)
	private String name;
	@Lob
	private String description;
	
	@OneToMany(mappedBy = "category", orphanRemoval = true, cascade = {CascadeType.ALL})
	private List<PersistenceProduct> products;
	
	/**
	 * Creates a new and empty category.
	 */
	public PersistenceCategory() {
		products = new ArrayList<>();
	}

	/**
	 * Clear products from cache to update relationships.
	 */
	@PostRemove
	private void clearCaches() {
		CacheManager.MANAGER.clearCache(PersistenceProduct.class);
		CacheManager.MANAGER.clearRemoteCache(PersistenceCategory.class);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setId(long id) {
		this.id = id;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Get all products in this category.
	 * @return All products in the category.
	 */
	public List<PersistenceProduct> getProducts() {
		return products;
	}

	/**
	 * Convert entity to record.
	 * @return New record object.
	 */
	public Category toRecord() {
		return new Category(
				this.getId(),
				this.getName(),
				this.getDescription()
		);
	}
}
