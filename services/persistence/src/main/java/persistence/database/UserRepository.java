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

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import utilities.datamodel.User;

/**
 * Repository that performs transactional CRUD operations for users on database.
 * @author Joakim von Kistowski
 *
 */
public final class UserRepository extends AbstractPersistenceRepository<User, PersistenceUser> {

	/**
	 * Singleton for the ProductRepository.
	 */
	public static final UserRepository REPOSITORY = new UserRepository();
	
	//Private constructor.
	private UserRepository() {
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long createEntity(User entity) {
		PersistenceUser user = new PersistenceUser();
		user.setUserName(entity.userName());
		user.setPassword(entity.password());
		user.setRealName(entity.realName());
		user.setEmail(entity.email());
		EntityManager em = getEM();
	    try {
	        em.getTransaction().begin();
	        PersistenceUser existing = getUserByName(entity.userName());
	        if (existing == null) {
	        	em.persist(user);
	        } else {
	        	user.setId(-1L);
	        }
	        em.getTransaction().commit();
	    } finally {
	        em.close();
	    }
	    return user.getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateEntity(long id, User entity) {
		boolean found = false;
		EntityManager em = getEM();
	    try {
	        em.getTransaction().begin();
	        PersistenceUser user = em.find(getEntityClass(), id);
	        if (user != null) {
	        	user.setUserName(entity.userName());
	    		user.setPassword(entity.password());
	    		user.setRealName(entity.realName());
	    		user.setEmail(entity.email());
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
	protected long getId(PersistenceUser v) {
		return v.getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Class<PersistenceUser> getEntityClass() {
		return PersistenceUser.class;
	}
	
	/**
	 * Return the user with the name.
	 * @param userName The user name.
	 * @return User or null if the user doesn't exist.
	 */
	public PersistenceUser getUserByName(String userName) {
		EntityManager em = getEM();
		TypedQuery<PersistenceUser> allMatchesQuery =
				em.createQuery("SELECT u FROM " + getEntityClass().getName()
						+ " u WHERE u.userName = :name", getEntityClass())
				.setMaxResults(1);
		allMatchesQuery.setParameter("name", userName);
		List<PersistenceUser> entities = allMatchesQuery.getResultList();
		if (entities == null || entities.isEmpty()) {
			return null;
		}
		return entities.get(0);
	}
	
}
