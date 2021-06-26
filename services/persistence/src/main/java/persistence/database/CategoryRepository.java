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

import jakarta.persistence.EntityManager;

import utilities.datamodel.Category;

/**
 * Repository that performs transactional CRUD operations cor Categories on database.
 * @author Joakim von Kistowski
 *
 */
public final class CategoryRepository extends AbstractPersistenceRepository<Category, PersistenceCategory> {

	/**
	 * Singleton for the CategoryRepository.
	 */
	public static final CategoryRepository REPOSITORY = new CategoryRepository();
	
	// Private constructor.
	private CategoryRepository() {}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long createEntity(Category entity) {
		PersistenceCategory category = new PersistenceCategory();
		category.setName(entity.name());
		category.setDescription(entity.description());
		
		EntityManager em = getEM();
	    try {
	        em.getTransaction().begin();
        	em.persist(category);
	        em.getTransaction().commit();
	    } finally {
	        em.close();
	    }
	    return category.getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateEntity(long id, Category entity) {
		boolean found = false;
		EntityManager em = getEM();
	    try {
	        em.getTransaction().begin();
	        PersistenceCategory category = em.find(getEntityClass(), id);
	        if (category != null) {
	        	category.setName(entity.name());
	        	category.setDescription(entity.description());
	        	found = true;
	        }
	        em.getTransaction().commit();
	    } finally {
	        em.close();
	    }
	    return found;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected long getId(PersistenceCategory v) {
		return v.getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Class<PersistenceCategory> getEntityClass() {
		return PersistenceCategory.class;
	}
	
}
