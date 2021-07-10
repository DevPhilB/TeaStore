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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import utilities.datamodel.Category;
import utilities.datamodel.ImageSize;
import utilities.enumeration.ImageSizePreset;
import utilities.datamodel.Product;
import image.ImageDB;
import image.ImageProvider;
import image.StoreImage;
import image.cache.FirstInFirstOut;
import image.cache.IDataCache;
import image.cache.LastInFirstOut;
import image.cache.LeastFrequentlyUsed;
import image.cache.LeastRecentlyUsed;
import image.cache.MostRecentlyUsed;
import image.cache.RandomReplacement;
import image.cache.rules.CacheAll;
import image.storage.DriveStorage;
import image.storage.IDataStorage;
import image.storage.rules.StoreAll;
import image.storage.rules.StoreLargeImages;
import utilities.rest.client.HttpClient;
import utilities.rest.client.HttpClientHandler;

import static utilities.rest.api.API.PERSISTENCE_ENDPOINT;

/**
 * Image provider setup class. Connects to the persistence service to collect all available products and generates
 * images from the received products and their category. Searches for existing images to be used in the web interface
 * and adds them to the storage / cache.
 * @author Norbert Schmitt
 */
public enum SetupController {

  /**
   * Instance of the setup controller.
   */
  SETUP;

  /**
   * Constants used during image provider setup.
   * @author Norbert Schmitt
   */
  private interface SetupControllerConstants {

	/**
	 * Standard working directory in which the images are stored.
	 */
    public static final Path STD_WORKING_DIR = Paths.get("images");

    /**
     * Longest wait period before querying the persistence again if it is finished creating entries.
     */
    public final int PERSISTENCE_CREATION_MAX_WAIT_TIME = 120000;

    /**
     * Wait time in ms before checking again for an existing persistence service.
     */
    public static final List<Integer> PERSISTENCE_CREATION_WAIT_TIME = Arrays.asList(1000, 2000,
        5000, 10000, 30000, 60000);

    /**
     * Number of available logical cpus for image creation.
     */
    public static final int CREATION_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * Wait time in ms for the image creation thread pool to terminate all threads.
     */
    public static final long CREATION_THREAD_POOL_WAIT = 500;

    /**
     * Wait time in ms (per image to generate) before an image provider service is registered if there is another
     * image provider service registered.
     */
    public static final long CREATION_THREAD_POOL_WAIT_PER_IMG_NR = 70;
  }
  // HTTP client
  private String scheme;
  private String gatewayHost;
  private Integer persistencePort;
  private HttpRequest request;
  private HttpClient httpClient;
  private HttpClientHandler handler;
  private ObjectMapper mapper;

  private StorageRule storageRule = StorageRule.STD_STORAGE_RULE;
  private CachingRule cachingRule = CachingRule.STD_CACHING_RULE;
  private Path workingDir = SetupControllerConstants.STD_WORKING_DIR;
  private long cacheSize = IDataCache.STD_MAX_CACHE_SIZE;
  private StorageMode storageMode = StorageMode.STD_STORAGE_MODE;
  private CachingMode cachingMode = CachingMode.STD_CACHING_MODE;
  private long nrOfImagesToGenerate = 0;
  private long nrOfImagesExisting = 0;
  private long nrOfImagesForCategory = 0;
  private AtomicLong nrOfImagesGenerated = new AtomicLong();
  private HashMap<String, BufferedImage> categoryImages = new HashMap<>();
  private ImageDB imgDB = new ImageDB();
  private IDataStorage<StoreImage> storage = null;
  private IDataCache<StoreImage> cache = null;
  private ScheduledThreadPoolExecutor imgCreationPool = new ScheduledThreadPoolExecutor(
      SetupControllerConstants.CREATION_THREAD_POOL_SIZE
  );
  private static final Logger LOG = LogManager.getLogger(SetupController.class);
  private AtomicBoolean isFinished = new AtomicBoolean();

  private SetupController() {}

  /**
   * Set up HTTP client for persistence queries
   */
  public void setupHttpClient(
          HttpVersion version,
          String scheme,
          String gatewayHost,
          Integer persistencePort
  ) {
    this.mapper = new ObjectMapper();
    this.scheme = scheme;
    this.gatewayHost = gatewayHost.isEmpty() ? "localhost" : gatewayHost;
    this.persistencePort = persistencePort;
    this.request = new DefaultFullHttpRequest(
            version,
            HttpMethod.GET,
            "",
            Unpooled.EMPTY_BUFFER
    );
    this.request.headers().set(HttpHeaderNames.HOST, this.gatewayHost);
    this.request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    this.request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
  }

  private void fetchProductsForCategory(Category category, HashMap<Category, List<Long>> products) {
    List<Product> productList = null;
    // GET api/persistence/products
    String persistenceEndpointProducts = PERSISTENCE_ENDPOINT +
            "/products?categoryid=" + category.id() + "&" + "start=0&max=-1";
    request.setUri(persistenceEndpointProducts);
    httpClient = new HttpClient(gatewayHost, persistencePort, request);
    handler = new HttpClientHandler();
    try {
      httpClient.sendRequest(handler);
      if (!handler.jsonContent.isEmpty()) {
          productList = mapper.readValue(
                  handler.jsonContent,
                  new TypeReference<List<Product>>() {}
          );
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    //
    if(productList == null) {
      products.put(category, new ArrayList<>());
      LOG.info("No products for category {} ({}) found.", category.name(), category.id());
    } else {
      List<Long> ids = productList.stream().map(Product::id).collect(Collectors.toList());
      products.put(category, ids);
      LOG.info("Category {} ({}) contains {} products.", category.name(), category.id(), ids.size());
    }
  }

  private List<Category> fetchCategories() {
    List<Category> categories = null;
    // GET api/persistence/categories
    String persistenceEndpointCategories = PERSISTENCE_ENDPOINT + "/categories";
    request.setUri(persistenceEndpointCategories);
    httpClient = new HttpClient(gatewayHost, persistencePort, request);
    handler = new HttpClientHandler();
    try {
      httpClient.sendRequest(handler);
      if (!handler.jsonContent.isEmpty()) {
        categories = mapper.readValue(
                handler.jsonContent,
                new TypeReference<List<Category>>() {}
        );
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (categories == null) {
      return new ArrayList<Category>();
    }
    return categories;
  }

  private HashMap<Category, BufferedImage> matchCategoriesToImage(List<Category> categories) {
    HashMap<Category, BufferedImage> result = new HashMap<>();

    List<String> imageNames = categoryImages.entrySet().stream().map(e -> e.getKey())
        .collect(Collectors.toList());
    for (String name : imageNames) {
      for (Category category : categories) {
        String[] tmp = category.name().split(",");
        if (tmp[0].toLowerCase().replace(" ", "-").equals(name)) {
          LOG.info("Found matching category {} ({}) for image {}.", category.name(),
              category.id(), name + "." + StoreImage.STORE_IMAGE_FORMAT);
          result.put(category, categoryImages.get(name));
        }
      }
    }
    return result;
  }

  /**
   * Generates images for the product IDs and categories received from the persistence service.
   */
  public void generateImages() {
    List<Category> categories = fetchCategories();
    HashMap<Category, List<Long>> products = new HashMap<>();
    categories.forEach(cat -> fetchProductsForCategory(cat, products));

    generateImages(products, matchCategoriesToImage(categories));
  }

  /**
   * Generates images for the given product IDs and categories.
   * @param products Map of categories and the corresponding products.
   * @param categoryImages Category image representing a specific category.
   */
  public void generateImages(Map<Category, List<Long>> products,
      Map<Category, BufferedImage> categoryImages) {
    nrOfImagesToGenerate = products.entrySet().stream().flatMap(e -> e.getValue().stream()).count();

    CreatorFactory factory = new CreatorFactory(ImageCreator.STD_NR_OF_SHAPES_PER_IMAGE, imgDB,
        ImageSizePreset.STD_IMAGE_SIZE, workingDir, products, categoryImages, nrOfImagesGenerated);

    // Schedule all image creation tasks
    for (long i = 0; i < nrOfImagesToGenerate; i++) {
      imgCreationPool.execute(factory.newRunnable());
    }

    LOG.info("Image creator thread started. {} {} sized images to generate using {} threads.",
        nrOfImagesToGenerate, ImageSizePreset.STD_IMAGE_SIZE.toString(),
        SetupControllerConstants.CREATION_THREAD_POOL_SIZE);
  }

  /**
   * Search for category images in the resource folder.
   */
  public void detectCategoryImages() {
    LOG.info("Trying to find images that indicate categories in generated images.");

    String resPath = "categoryimg" + File.separator + "black-tea.png";
    File dir = getPathToResource(resPath).toFile();

    if (dir != null) {
      LOG.info("Found resource directory with category images at {}.",
          dir.toPath().toAbsolutePath().toString());
    } else {
      LOG.info("Resource path {} not found.", resPath);
      return;
    }

    nrOfImagesForCategory = 0;
    if (dir.exists() && dir.isDirectory()) {
      File[] fileList = dir.listFiles();
      if (fileList == null) {
    	  return;
      }
      for (File file : fileList) {
        if (file.isFile() && file.getName().endsWith(StoreImage.STORE_IMAGE_FORMAT)) {
          try {
            categoryImages.put(file.getName().substring(0, file.getName().length() - 4),
                ImageIO.read(file));
            nrOfImagesForCategory++;
          } catch (IOException ioException) {
            LOG.warn(
                "An IOException occured while reading image file " + file.getAbsolutePath() + ".",
                ioException);
          }
        }
      }
    }
    LOG.info("Found {} images for categories.", nrOfImagesForCategory);
  }

  /**
   * Create the working directory in which all generated images are stored if it is not existing.
   */
  public void createWorkingDir() {
    if (!workingDir.toFile().exists()) {
      if (!workingDir.toFile().mkdir()) {
        LOG.error("Standard working directory \"" + workingDir.toAbsolutePath()
            + "\" could not be created.");
        throw new IllegalArgumentException("Standard working directory \""
            + workingDir.toAbsolutePath() + "\" could not be created.");
      } else {
        LOG.info("Working directory {} created.", workingDir.toAbsolutePath().toString());
      }
    } else {
      LOG.info("Working directory {} already existed.", workingDir.toAbsolutePath().toString());
    }
  }

  /**
   * Returns the path to a given resource, category image or web interface image.
   * @param resource Resource to find path.
   * @return Path to the given resource or NULL if the resource could not be found.
   */
  public Path getPathToResource(String resource) {
    // Rework the code piece fetching the existing images until the next
    // comment
    URL url = this.getClass().getResource(resource);
    Path dir = null;
    String path = "";
    try {
      path = URLDecoder.decode(url.getPath(), "UTF-8");
      if (path.contains(":")) {
        path = path.substring(3);
      }
      dir = Paths.get(path).getParent();
    } catch (UnsupportedEncodingException e) {
      LOG.warn("The resource path \"" + path + "\" could not be decoded with UTF-8.");
    }
    // End of rework
    return dir;
  }

  /**
   * Search for web interface images and add them to the existing image database.
   */
  public void detectExistingImages() {
    detectExistingImages(imgDB);
  }

  /**
   * Search for web interface images and add them to the given image database.
   * @param db Image database found web interface images will be added to.
   */
  public void detectExistingImages(ImageDB db) {
    if (db == null) {
      LOG.error("The supplied image database is null.");
      throw new NullPointerException("The supplied image database is null.");
    }

    String resPath = "existingimg" + File.separator + "front.png";
    Path dir = getPathToResource(resPath);

    if (dir != null) {
      LOG.info("Found resource directory with existing images at {}.",
          dir.toAbsolutePath().toString());
    } else {
      LOG.info("Resource path {} not found.", resPath);
      return;
    }

    File currentDir = dir.toFile();

    if (currentDir.exists() && currentDir.isDirectory()) {
      File[] fileList = currentDir.listFiles();
      if (fileList == null) {
    	  return;
      }
      for (File file : fileList) {
        if (file.isFile() && file.getName().endsWith(StoreImage.STORE_IMAGE_FORMAT)) {
          long imageID = ImageIDFactory.ID.getNextImageID();

          BufferedImage buffImg = null;
          // Copy files to correct file with the image id number
          try {
            buffImg = ImageIO.read(file);

          } catch (IOException ioException) {
            LOG.warn("An IOException occured while reading the file " + file.getAbsolutePath()
                + " from disk.", ioException.getMessage());
          } finally {
            if (buffImg == null) {
              LOG.warn("The file \"" + file.toPath().toAbsolutePath() + "\" could not be read.");
              continue;
            }
          }

          db.setImageMapping(
              file.getName().substring(0,
                  file.getName().length() - StoreImage.STORE_IMAGE_FORMAT.length() - 1),
              imageID, new ImageSize(buffImg.getWidth(), buffImg.getHeight()));
          StoreImage img = new StoreImage(imageID, buffImg, ImageSizePreset.FULL.getSize());

          try {
            Files.write(workingDir.resolve(String.valueOf(imageID)), img.getByteArray(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
          } catch (IOException ioException) {
            LOG.warn("An IOException occured while writing the image with ID "
                + String.valueOf(imageID) + " to the file "
                + workingDir.resolve(String.valueOf(imageID)).toAbsolutePath() + ".",
                ioException.getMessage());
          }
          // Increment to have correct number of images for the limited drive storage
          nrOfImagesExisting++;
        }
      }
    }

    LOG.info("Scanned path {} for existing images. {} images found.",
        dir.toAbsolutePath().toString(), nrOfImagesExisting);
  }

  /**
   * Sets the cache size of the specific implementation.
   * @param cacheSize Positive cache size in bytes.
   * @return True if the cache size was set successfully, otherwise false.
   */
  public boolean setCacheSize(long cacheSize) {
    if (cacheSize < 0) {
      LOG.info("Tried to set cache size to a value below zero. Keeping old value");
      return false;
    }
    if (cache == null) {
      LOG.info("No cache defined.");
      return false;
    }
    return cache.setMaxCacheSize(cacheSize);
  }

  /**
   * Delete all images from the current working directory.
   */
  public void deleteImages() {
    deleteUnusedImages(new ArrayList<>());
  }

  /**
   * Delete all images from the current working directory, except the images with the IDs given.
   * @param imagesToKeep List of images to keep.
   */
  public void deleteUnusedImages(List<Long> imagesToKeep) {
    File currentDir = workingDir.toFile();
    int nrOfImagesDeleted = 0;

    if (currentDir.exists() && currentDir.isDirectory()) {
      File[] fileList = currentDir.listFiles();
      if (fileList == null) {
        return;
      }
      for (File file : fileList) {
        if (file.isFile() && !imagesToKeep.contains(Long.parseLong(file.getName()))) {
          boolean isDeleted = file.delete();
          if (isDeleted) {
            nrOfImagesDeleted++;
          }
        }
      }
    }

    LOG.info("Deleted images in working directory {}. {} images deleted.",
        workingDir.toAbsolutePath().toString(), nrOfImagesDeleted);
  }

  /**
   * Deletes the current working directory.
   */
  public void deleteWorkingDir() {
    File currentDir = workingDir.toFile();
    boolean isDeleted = false;

    if (currentDir.exists() && currentDir.isDirectory()) {
      isDeleted = currentDir.delete();
    }

    if (isDeleted) {
      LOG.info("Deleted working directory {}.", workingDir.toAbsolutePath().toString());
    } else {
      LOG.info("Working directory {} not deleted.", workingDir.toAbsolutePath().toString());
    }
  }

  /**
   * Sets up the storage, storage rule, cache implementation and caching rule according to the configuration.
   */
  public void setupStorage() {
    Predicate<StoreImage> storagePredicate = new StoreAll<StoreImage>();
    switch (storageRule) {
    case ALL:
      break;
    case FULL_SIZE_IMG:
      storagePredicate = new StoreLargeImages();
      break;
    default:
      break;
    }

    // Only support Drive Storage at this moment
    storage = new DriveStorage(workingDir, imgDB, storagePredicate);

    Predicate<StoreImage> cachePredicate = null;
    if (cachingRule == CachingRule.ALL) {
      cachePredicate = new CacheAll<StoreImage>();
    } else {
      cachePredicate = new CacheAll<StoreImage>();
    }

    cache = null;
    switch (cachingMode) {
    case FIFO:
      cache = new FirstInFirstOut<StoreImage>(storage, cacheSize, cachePredicate);
      break;
    case LIFO:
      cache = new LastInFirstOut<StoreImage>(storage, cacheSize, cachePredicate);
      break;
    case RR:
      cache = new RandomReplacement<StoreImage>(storage, cacheSize, cachePredicate);
      break;
    case LFU:
      cache = new LeastFrequentlyUsed<StoreImage>(storage, cacheSize, cachePredicate);
      break;
    case LRU:
      cache = new LeastRecentlyUsed<StoreImage>(storage, cacheSize, cachePredicate);
      break;
    case MRU:
      cache = new MostRecentlyUsed<StoreImage>(storage, cacheSize, cachePredicate);
      break;
    default:
      break;
    }

    LOG.info("Storage setup done.");
  }

  /**
   * Give the image provider the configured image database and cache / storage object containing all images referenced
   * in the image database.
   */
  public void configureImageProvider() {
    ImageProvider.IP.setImageDB(imgDB);
    if (cache == null) {
    	ImageProvider.IP.setStorage(storage);
    } else {
    	ImageProvider.IP.setStorage(cache);
    }

    LOG.info("Storage and image database handed over to image provider");
  }

  /**
   * Returns the current working directory.
   * @return Current working directory.
   */
  public Path getWorkingDir() {
    return workingDir;
  }

  /**
   * Checks whether the setup is finished and complete or not.
   * @return True if the setup is finished and complete, otherwise false.
   */
  public boolean isFinished() {
    if (storage == null) {
      return false;
    }
    if (imgCreationPool.getQueue().size() != 0) {
      return false;
    }
    return isFinished.get();
  }

  /**
   * Returns a string containing the current state of the image provider setup and configuration settings.
   * @return A string containing the current state of the image provider setup and configuration settings.
   */
  public String getState() {
    StringBuilder sb = new StringBuilder();

    sb.append("Image Provider State:").append(System.lineSeparator());
    sb.append("---------------------").append(System.lineSeparator());
    sb.append("Working Directory: ").append(workingDir.toAbsolutePath().toString())
        .append(System.lineSeparator());
    sb.append("Storage Mode: ").append(storageMode.getStrRepresentation())
        .append(System.lineSeparator());
    sb.append("Storage Rule: ").append(storageRule.getStrRepresentation())
        .append(System.lineSeparator());
    sb.append("Caching Mode: ").append(cachingMode.getStrRepresentation())
        .append(System.lineSeparator());
    sb.append("Caching Rule: ").append(cachingRule.getStrRepresentation())
        .append(System.lineSeparator());
    String poolState = "Running";
    if (imgCreationPool.getQueue().size() == 0) {
    	poolState = "Finished";
    }
    sb.append("Creator Thread: ").append(poolState)
        .append(System.lineSeparator());
    sb.append("Images Created: ").append(String.valueOf(nrOfImagesGenerated.get())).append(" / ")
        .append(String.valueOf(nrOfImagesToGenerate)).append(System.lineSeparator());
    sb.append("Pre-Existing Images Found: ").append(String.valueOf(nrOfImagesExisting))
        .append(System.lineSeparator());
    sb.append("Category Images Found: ").append(String.valueOf(nrOfImagesForCategory))
        .append(System.lineSeparator());

    return sb.toString();
  }

  /*
   * Convenience methods
   */

  /**
   * Deletes all images and the current working directory.
   */
  public void teardown() {
    deleteImages();
    deleteWorkingDir();
  }

  /**
   * Deletes all images and the current working directory and starts the setup by generating product images and
   * adding web interface images to the image database. The final cache / storage and image database is then handed
   * over to the image provider instance. If this image provider service is the not the first image provider and other
   * image provider services are registered, the registration is delayed until all images are generated.
   */
  public void startup() {
    // Delete all images in case the image provider was not shutdown gracefully last
    // time, leaving images on disk
    isFinished.set(false);
    deleteImages();
    deleteWorkingDir();
    createWorkingDir();
    detectExistingImages();
    detectCategoryImages();
    generateImages();
    setupStorage();
    configureImageProvider();
    // Wait for completion
    long waitingTime = (
            (nrOfImagesToGenerate - nrOfImagesGenerated.get())
            / SetupControllerConstants.CREATION_THREAD_POOL_SIZE
            * SetupControllerConstants.CREATION_THREAD_POOL_WAIT_PER_IMG_NR
    );
    try {
      imgCreationPool.shutdown();
      if (imgCreationPool.awaitTermination(waitingTime, TimeUnit.MILLISECONDS)) {
        LOG.info("Image creation stopped.");
      } else {
        LOG.warn("Image creation thread pool not terminating after {}ms. Stop waiting.", waitingTime);
      }
    } catch (InterruptedException interruptedException) {
      LOG.warn("Waiting for image creation thread pool termination interrupted by exception.",
              interruptedException);
    }
    // Maybe we need to keep a reference to the old thread pool if it has not finished properly yet.
    imgCreationPool = new ScheduledThreadPoolExecutor(SetupControllerConstants.CREATION_THREAD_POOL_SIZE);
    isFinished.set(true);
  }

  /**
   * Deletes all images and the current working directory and starts the setup by generating product images and
   * adding web interface images to the image database. The final cache / storage and image database is then handed
   * over to the image provider instance. The reconfiguration and image generation takes place in a background thread.
   * This service remains registered and might receive request from other services.
   */
  public void reconfiguration() {
    Thread x = new Thread() {

      @Override
      public void run() {
        try {
          imgCreationPool.shutdownNow();
          if (imgCreationPool.awaitTermination(
                  SetupControllerConstants.CREATION_THREAD_POOL_WAIT,
                  TimeUnit.MILLISECONDS
            ))
          {
            LOG.info("Image creation stopped.");
          } else {
            LOG.warn("Image creation thread pool not terminating after {}ms. Stop waiting.",
                    SetupControllerConstants.CREATION_THREAD_POOL_WAIT);
          }
        } catch (InterruptedException interruptedException) {
          LOG.warn("Waiting for image creation thread pool termination interrupted by exception.",
                  interruptedException);
        }
        // Maybe we need to keep a reference to the old thread pool if it has not finished properly yet.
        imgCreationPool = new ScheduledThreadPoolExecutor(SetupControllerConstants.CREATION_THREAD_POOL_SIZE);

        // Create new image database
        imgDB = new ImageDB();

        isFinished.set(false);
        deleteImages();
        detectExistingImages();
        detectCategoryImages();
        generateImages();
        setupStorage();
        configureImageProvider();
        isFinished.set(true);
      }
    };
    x.start();
  }

}
