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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.persistence.sessions.server.ServerSession;
import org.eclipse.persistence.tools.schemaframework.SchemaManager;
import org.mindrot.jbcrypt.BCrypt;

import  utilities.datamodel.Category;
import  utilities.datamodel.Order;
import  utilities.datamodel.OrderItem;
import  utilities.datamodel.Product;
import  utilities.datamodel.User;

/**
 * Class for generating data in the database.
 *
 * @author Joakim von Kistowski
 *
 */
public final class DataGenerator {

	/**
	 * Status code for maintenance mode.
	 */
	public static final int MAINTENANCE_STATUS_CODE = 503;

	/**
	 * Default category count for small database.
	 */
	public static final int SMALL_DB_CATEGORIES = 5;
	/**
	 * Default product count per category for small database.
	 */
	public static final int SMALL_DB_PRODUCTS_PER_CATEGORY = 100;
	/**
	 * Default user count for small database.
	 */
	public static final int SMALL_DB_USERS = 100;
	/**
	 * Default max order per user for small database.
	 */
	public static final int SMALL_DB_MAX_ORDERS_PER_USER = 5;

	/**
	 * Default category count for tiny database.
	 */
	public static final int TINY_DB_CATEGORIES = 2;
	/**
	 * Default product count per category for tiny database.
	 */
	public static final int TINY_DB_PRODUCTS_PER_CATEGORY = 20;
	/**
	 * Default user count for tiny database.
	 */
	public static final int TINY_DB_USERS = 5;
	/**
	 * Default max order per user for tiny database.
	 */
	public static final int TINY_DB_MAX_ORDERS_PER_USER = 2;

	private Random random = new Random(5);

	private static final String PASSWORD = "password";
	private static final String[] CATEGORYNAMES = { "Black Tea", "Green Tea", "Herbal Tea", "Rooibos", "White Tea",
			"Tea Cups", "Tea Pots", "Filters", "Infusers" };
	private static final String[] CATEGORYDESCRIPTIONS = { "Pure black tea and blends", "From China and Japan",
			"Helps when you feel sick", "In many variations", "If green tea doesn't agree with you",
			"Cups and glasses", "Classy and useful", "For extremely fine grained tea",
			"No metal for green tea" };

	private static final String[][] PRODUCTNAMES = {
			{ "Earl Grey (loose)", "Assam (loose)", "Darjeeling (loose)", "Frisian Black Tee (loose)",
				"Anatolian Assam (loose)", "Earl Grey (20 bags)", "Assam (20 bags)", "Darjeeling (20 bags)",
				"Ceylon (loose)", "Ceylon (20 bags)", "House blend (20 bags)", "Assam with Ginger (20 bags)"},
			{ "Sencha (loose)", "Sencha (15 bags)", "Sencha (25 bags)", "Earl Grey Green (loose)",
					"Earl Grey Green (15 bags)", "Earl Grey Green (25 bags)", "Matcha 30 g", "Matcha 50 g",
					"Matcha 100 g", "Gunpowder Tea (loose)", "Gunpowder Tea (15 bags)", "Gunpowder Tea (25 bags)" },
			{ "Camomile (loose)", "Camomile (15 bags)", "Peepermint (loose)", "Peppermint (15 bags)",
					"Peppermint (15 bags)", "Sweet Mint (loose)", "Sweet Mint (15 bags)", "Sweet Mint (25 bags)",
					"Lemongrass (loose)", "Lemongrass (20 bags)", "Chai Mate (15 bags)", "Chai Mate (25 bags)",
					"Stomach Soothing Tea (15 bags)", "Headache Soothing Tea (15 bags)" },
			{ "Rooibos Pure (loose)", "Rooibos Pure (20 bags)", "Rooibos Orange (loose)", "Rooibos Orange (20 bags)",
					"Rooibos Coconut (loose)", "Rooibos Coconut (20 bags)", "Rooibos Vanilla (loose)",
					"Rooibos Pure (20 bags)", "Rooibos Ginger (loose)", "Rooibos Pure (20 bags)",
					"Rooibos Grapefruit (loose)", "Rooibos Pure (20 bags)" },
			{ "White Tea (loose)", "White Tea (15 bags)", "White Tea (25 bags)", "White Chai (loose)",
					"White Chai (15 bags)", "White Chai (25 bags)", "Pai Mu Tan White (loose)",
					"Pai Mu Tan White (15 bags)", "Pai Mu Tan White (25 bags)", "White Apricot (loose)",
					"White Apricot (15 bags)", "White Apricot (25 bags)" },
			{ "Ceramic Cup White", "Ceramic Cup Blue", "Ceramic Cup Green", "Ceramic Cup Black",
					"Percelain Cup White", "Porcelain Cup with Flowers", "Poercelain Cup with Dog Picture",
					"Small Glass Cup", "Large Glass Cup", "Small Glass Cup with Glass Infuser",
					"Large Glass Cup with Glass Infuser", "Small Glass Cup with Plastic Infuser",
					"Large Glass Cup with Plastic Infuser" },
			{ "Porcelain Teapot White, 2 Cups", "Porcelain Teapot White, 5 Cups",
					"Porcelain Teapot with Flowers, 2 Cups", "Porcelain Teapot with Flowers, 5 Cups",
					"Persian Teapot, 3 Cups", "Large Teapot with Glass Infuser, 7 Cups",
					"Small Teapot with Glass Infuser, 3 Cups", "Medium Teapot with Glass Infuser, 5 Cups",
					"Large Glass Teapot with Steel Infuser, 7 Cups", "Small Glass Teapot with Steel Infuser, 3 Cups",
					"Medium Glass Teapot with Steel Infuser, 5 Cups", "Glass Teapot Warmer" },
			{ "Filters with Drawstring, 100 pcs.", "Filters with Drawstring, 250 pcs.",
					"Filters with Drawstring, 500 pcs.", "Tea Sack, 50 pcs.", "Tea Sack, 125 pcs.",
					"Tea Sack, 500 pcs.", "Reusible Cotton Tea Sack, 10 pcs.", "Reusible Cotton Tea Sack, 35 pcs.",
					"Reusable Cotton Tea Sack, 50 pcs.", "Pyramid-shaped Tea Filter, 10 pcs.",
					"Pyramid-shaped Tea Filter, 25 pcs.", "Mr. Tea Filter, 10 pcs." },
			{ "Medium Mesh Ball with Chain", "Medium Snap Mesh Ball", "Large Ball with Chain",
						"Small Mesh Ball with Chain", "Small Snap Mesh Ball", "Large Snap Mesh Ball",
						"Medium Silicone Ball Infuser", "Small Silicone Ball Infuser",
					"Large Silicone Ball Infuser", "Small Mesh Ball with Panda Look", "Heart-shaped Infuser" } };

	private static final String[] FIRSTNAMES = {"James", "John", "Robert", "Michael", "William", "David",
			"Richard", "Charles", "Jospeph", "Thomas", "Christopher", "Daniel", "Paul", "Mark", "Donald",
			"George", "Kenneth", "Steven", "Edward", "Brian", "Ronald", "Anthony", "Kevin", "Jason",
			"Matthew", "Gary", "Timothy", "Jose", "Larry", "Jeffrey", "Frank", "Scott", "Eric", "Stephen",
			"Andrew", "Raymond", "Gregory", "Joshua", "Jerry", "Dennis", "Walter", "Patrick", "Peter",
			"Mary", "Patricia", "Barbara", "Elizabeth", "Jennifer", "Maria", "Susan", "Margaret", "Dorothy",
			"Lisa", "Nancy", "Karen", "Betty", "Helen", "Sandra", "Donna", "Carol", "Ruth", "Sharon",
			"Michelle", "Laura", "Sarah", "Kimberly", "Deborah", "Jessica", "Shirley", "Cynthia"};
	private static final String[] LASTNAMES = {"Smith", "Johnson", "Williams", "Jones", "Brown", "Davis",
			"Miller", "Wilson", "Moorse", "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris",
			"Martin", "Thompson", "Garcia", "Martinez", "Robinson", "Clark", "Rodriguez", "Lewis", "Lee",
			"Walker", "Hall", "Allen", "Young", "Hernandez", "King", "Wright", "Lopez", "Hill", "Scoot"};

	private static final int MAX_ITEMS_PER_ORDER = 10;
	private static final double PREFFERED_CATEGORY_CHANCE = 0.825;


	/**
	 * The data generator singleton.
	 */
	public static final DataGenerator GENERATOR = new DataGenerator();

	private boolean maintenanceMode = false;

	private DataGenerator() {

	}

	/**
	 * Checks if the database is empty.
	 *
	 * @return True if the database is empty.
	 */
	public boolean isDatabaseEmpty() {
		// every other entity requires a valid category or user
		return (CategoryRepository.REPOSITORY.getAllEntities(-1, 1).size() == 0
				&& UserRepository.REPOSITORY.getAllEntities(-1, 1).size() == 0);
	}

	/**
	 * Generates data for the database. Uses a fixed random seed.
	 *
	 * @param categories
	 *            Number of categories.
	 * @param productsPerCategory
	 *            Number of products per category.
	 * @param users
	 *            Number of users. Password is always "password".
	 * @param maxOrdersPerUser
	 *            Maximum order per user.
	 */
	public void generateDatabaseContent(int categories, int productsPerCategory,
			int users, int maxOrdersPerUser) {
		setGenerationFinishedFlag(false);
		CacheManager.MANAGER.clearAllCaches();
		random = new Random(5);
		generateCategories(categories);
		generateProducts(productsPerCategory);
		generateUsers(users);
		generateOrders(maxOrdersPerUser, productsPerCategory);
		setGenerationFinishedFlag(true);
		CacheManager.MANAGER.clearAllCaches();
	}

	private void generateCategories(int categories) {
		for (int i = 0; i < categories; i++) {
			String description = null;
			String name = null;

			if (i < CATEGORYDESCRIPTIONS.length) {
				description = CATEGORYDESCRIPTIONS[i];
			} else {
				int version = i / CATEGORYDESCRIPTIONS.length;
				description = CATEGORYDESCRIPTIONS[i % CATEGORYDESCRIPTIONS.length] + ", v" + version;
			}
			if (i < CATEGORYNAMES.length) {
				name = CATEGORYNAMES[i];
			} else {
				int version = i / CATEGORYNAMES.length;
				name = CATEGORYNAMES[i % CATEGORYNAMES.length] + ", v" + version;
			}
			Category category = new Category(
					1L + i,
					description,
					name
			);
			CategoryRepository.REPOSITORY.createEntity(category);
		}
	}

	private void generateProducts(int productsPerCategory) {
		int categoryIndex = 0;
		for (PersistenceCategory category : CategoryRepository.REPOSITORY.getAllEntities()) {
			int productTypeIndex = categoryIndex % PRODUCTNAMES.length;
			for (int i = 0; i < productsPerCategory; i++) {
				int productIndex = i % PRODUCTNAMES[productTypeIndex].length;
				int version = i / PRODUCTNAMES[productTypeIndex].length;
				String name = null;
				String description = null;
				Long listPriceInCents = null;
				Long categoryId = null;
				if (version == 0) {
					name = PRODUCTNAMES[productTypeIndex][productIndex];
				} else {
					name = PRODUCTNAMES[productTypeIndex][productIndex] + ", v" + version;
				}
				description= "Great " + category.getName() + ": " + PRODUCTNAMES[productTypeIndex][productIndex];
				listPriceInCents = 95L + random.nextInt(12000);
				categoryId = category.getId();
				Product product = new Product(
						(long) i,
						categoryId,
						name,
						description,
						listPriceInCents
				);
				ProductRepository.REPOSITORY.createEntity(product);
			}
			categoryIndex++;
		}
	}

	private void generateUsers(int users) {
		IntStream.range(0, users).parallel().forEach(i -> {
			User user = new User(
					(long) i,
					"user" + i,
					BCrypt.hashpw(PASSWORD, BCrypt.gensalt(6)),
					FIRSTNAMES[random.nextInt(FIRSTNAMES.length)]
							+ " " + LASTNAMES[random.nextInt(LASTNAMES.length)],
					"user" + i + "@teastore.com"
			);
			UserRepository.REPOSITORY.createEntity(user);
		});
	}

	private void generateOrders(int maxOrdersPerUser, int productsPerCategory) {
		UserRepository.REPOSITORY.getAllEntities().parallelStream().forEach(user -> {
			for (int i = 0; i < random.nextInt(maxOrdersPerUser + 1); i++) {
				String addressName = user.getRealName();
				String eastWest = " East ";
				if (random.nextDouble() > 0.5) {
					eastWest = " West ";
				}
				String northSouth = " North";
				if (random.nextDouble() > 0.5) {
					northSouth = " South";
				}
				String address1 = random.nextInt(9000) + eastWest + random.nextInt(9000) + northSouth;
				String address2 = "District " + random.nextInt(500) + ", Utopia, "
						+ (10000 + random.nextInt(40000));
				String creditCardType = "MasterCard";
				if (random.nextDouble() > 0.5) {
					creditCardType = "Visa";
				}
				String creditCardExpiryDate = LocalDate.ofYearDay(LocalDateTime.now().getYear() + 1
						+ random.nextInt(10), 1
						+ random.nextInt(363)).format(DateTimeFormatter.ISO_LOCAL_DATE);
				String time = LocalDateTime.of(LocalDateTime.now().getYear() - random.nextInt(10),
						1 + random.nextInt(10), 1 + random.nextInt(24),
						random.nextInt(23), random.nextInt(59))
						.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
				Long userId = user.getId();
				String creditCardNumber = fourDigits() + " " + fourDigits() + " " + fourDigits() + " " + fourDigits();
				long totalPriceInCents = 0;
				List<PersistenceCategory> categories = CategoryRepository.REPOSITORY.getAllEntities();
				PersistenceCategory preferred = categories.get(random.nextInt(categories.size()));
				for (int j = 0; j < 1 + random.nextInt(MAX_ITEMS_PER_ORDER); j++) {
					OrderItem item = generateOrderItem((long) i, preferred, productsPerCategory);
					totalPriceInCents += item.quantity() * item.unitPriceInCents();
					OrderItemRepository.REPOSITORY.createEntity(item);
				}
				Order order = new Order(
						(long) i,
						userId,
						time,
						totalPriceInCents,
						addressName,
						address1,
						address2,
						creditCardType,
						creditCardNumber,
						creditCardExpiryDate
				);
				OrderRepository.REPOSITORY.createEntity(order);
			}
		});
	}

	// Order and preferred category must have a valid id!
	private OrderItem generateOrderItem(Long orderId, PersistenceCategory preferred, int productsPerCategory) {
		Integer quantity = random.nextInt(7);
		PersistenceCategory itemCategory = preferred;
		if (random.nextDouble() > PREFFERED_CATEGORY_CHANCE) {
			List<PersistenceCategory> categories = CategoryRepository.REPOSITORY.getAllEntities();
			itemCategory = categories.get(random.nextInt(categories.size()));
		}
		PersistenceProduct product = ProductRepository.REPOSITORY.getAllEntities(
				itemCategory.getId(), random.nextInt(productsPerCategory), 1).get(0);
		OrderItem item = new OrderItem(
				random.nextLong(),
				product.getId(),
				orderId,
				quantity,
				product.getListPriceInCents()
		);
		return item;
	}

	private String fourDigits() {
		return String.valueOf(1000 + random.nextInt(8999));
	}

	/**
	 * Drops database and recreates all tables.<br/>
	 * Attention: Does not reset foreign persistence contexts.
	 * Best practice is to call CacheManager.MANAGER.resetAllEMFs() after dropping and then recreating the DB.
	 */
	public void dropAndCreateTables() {
		CacheManager.MANAGER.clearLocalCacheOnly();
		ServerSession session = CategoryRepository.REPOSITORY.getEM().unwrap(ServerSession.class);
		SchemaManager schemaManager = new SchemaManager(session);
		schemaManager.replaceDefaultTables(true, true);
		CacheManager.MANAGER.clearLocalCacheOnly();
		CacheManager.MANAGER.resetLocalEMF();
		setGenerationFinishedFlag(false);
		CacheManager.MANAGER.clearAllCaches();
	}

	private void setGenerationFinishedFlag(boolean flag) {
		EntityManager em = CategoryRepository.REPOSITORY.getEM();
		try {
			em.getTransaction().begin();
			List<DatabaseManagementEntity> entities =
					em.createQuery("SELECT u FROM "
							+ DatabaseManagementEntity.class.getName()
							+ " u", DatabaseManagementEntity.class)
					.getResultList();
			if (entities == null || entities.isEmpty()) {
				DatabaseManagementEntity entity = new DatabaseManagementEntity();
				entity.setFinishedGenerating(flag);
				em.persist(entity);
			} else {
				DatabaseManagementEntity entity = entities.get(0);
				entity.setFinishedGenerating(flag);
			}
			em.getTransaction().commit();
		} finally {
			em.close();
		}
	}

	/**
	 * Returns true if the database has finished generating.
	 * False if it is currently generating.
	 * @return False if the database is generating.
	 */
	public boolean getGenerationFinishedFlag() {
		if (isMaintenanceMode()) {
			return false;
		}
		boolean finishedGenerating = false;
		EntityManager em = CategoryRepository.REPOSITORY.getEM();
		try {
			List<DatabaseManagementEntity> entities =
					em.createQuery("SELECT u FROM "
							+ DatabaseManagementEntity.class.getName()
							+ " u", DatabaseManagementEntity.class)
					.getResultList();
			if (entities != null && !entities.isEmpty()) {
				finishedGenerating = entities.get(0).isFinishedGenerating();
			}
		} finally {
			em.close();
		}
		return finishedGenerating;
	}

	/**
	 * Returns if the current persistence is in maintenance mode.
	 * Will return 503 on pretty much every external call in this mode.
	 * @return True if in maintenance, false otherwise.
	 */
	public boolean isMaintenanceMode() {
		return maintenanceMode;
	}

	/**
	 * Put the current persistence into maintenance mode.
	 * Will return 503 on pretty much every external call in this mode.
	 * @param maintenanceMode The maintenance flag.
	 */
	public synchronized void setMaintenanceModeInternal(boolean maintenanceMode) {
		this.maintenanceMode = maintenanceMode;
	}

	/**
	 * Puts all persistences into maintenance mode.
	 * Will return 503 on pretty much every external call once in this mode.
	 * @param maintenanceMode The maintenance flag.
	 */
	public void setMaintenanceModeGlobal(boolean maintenanceMode) {
		setMaintenanceModeInternal(maintenanceMode);
		/*
		List<Response> rs = ServiceLoadBalancer.multicastRESTToOtherServiceInstances(
				"generatedb", String.class, client -> setMaintenanceModeExternal(client, maintenanceMode));
		rs.forEach(r -> {
				if (r != null) {
					r.bufferEntity();
					r.close();
				}
			});
		*/

	}

	private Response setMaintenanceModeExternal() {//RESTClient<String> client, final Boolean maintenanceMode) {
		//Response r = client.getEndpointTarget().path("maintenance")
		//.request(MediaType.TEXT_PLAIN).post(Entity.entity(String.valueOf(maintenanceMode), MediaType.TEXT_PLAIN));
		//return r;
		return null;
	}
}
