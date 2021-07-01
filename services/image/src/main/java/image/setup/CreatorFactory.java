package image.setup;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import image.cache.entry.AbstractEntry;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import utilities.datamodel.Category;
import utilities.datamodel.ImageSize;
import utilities.enumeration.ImageSizePreset;
import image.ImageDB;

/**
 * Helper class creating image generation runnables for image provider setup.
 * @author Norbert Schmitt
 */
public class CreatorFactory {

  private int shapesPerImage = 0;
  private ImageSize imgSize = ImageSizePreset.STD_IMAGE_SIZE;
  private Path workingDir = SetupController.SETUP.getWorkingDir();
  private Map<Category, BufferedImage> categoryImages;
  private List<Long> products;
  private List<Category> categories;
  private ImageDB imgDB;
  private AtomicLong nrOfImagesGenerated;

  private static final Logger LOG = LogManager.getLogger(CreatorFactory.class);

  /**
   * Standard constructor defining all necessary information to create image generation runnables.
   * @param shapesPerImage Number of shapes per generated image.
   * @param imgDB Image database to add image to.
   * @param imgSize Size of the images to generate.
   * @param workingDir Directory to store images.
   * @param products Product IDs for which images will be generated.
   * @param categoryImages Category images that are added after random shapes for the image have been generated.
   * @param nrOfImagesGenerated Current number of images that have been generated by all runnables.
   */
  public CreatorFactory(int shapesPerImage, ImageDB imgDB, ImageSize imgSize, Path workingDir,
      Map<Category, List<Long>> products, Map<Category, BufferedImage> categoryImages,
      AtomicLong nrOfImagesGenerated) {
    if (imgDB == null) {
      LOG.error("Supplied image database is null.");
      throw new NullPointerException("Supplied image database is null.");
    }
    if (products == null) {
      LOG.error("Supplied product map is null.");
      throw new NullPointerException("Supplied product map is null.");
    }
    if (nrOfImagesGenerated == null) {
      LOG.error("Supplied counter for images generated is null.");
      throw new NullPointerException("Supplied counter for images generated is null.");
    }

    if (workingDir == null) {
      LOG.info("Supplied working directory is null. Set to value {}.",
          SetupController.SETUP.getWorkingDir());
    } else {
      this.workingDir = workingDir;
    }
    if (categoryImages == null) {
      LOG.info("Supplied category images are null. Defaulting to not add category images.");
    } else {
      this.categoryImages = categoryImages;
    }
    if (imgSize == null) {
      LOG.info("Supplied image size is null. Defaulting to standard size of {}.",
          ImageSizePreset.STD_IMAGE_SIZE);
    } else {
      this.imgSize = imgSize;
    }
    if (shapesPerImage < 0) {
      LOG.info("Number of shapes per image cannot be below 0, was {}. Set to 0.", shapesPerImage);
    } else {
      this.shapesPerImage = shapesPerImage;
    }
    this.products = products.entrySet().stream().flatMap(e -> e.getValue().stream())
        .collect(Collectors.toList());
    this.categories = products.entrySet().stream()
        .flatMap(e -> e.getValue().stream().map(x -> e.getKey())).collect(Collectors.toList());
    this.imgDB = imgDB;
    this.nrOfImagesGenerated = nrOfImagesGenerated;
  }

  /**
   * Create the image generation runnable.
   * @return Image generation runnable.
   */
  public Runnable newRunnable() {
    return new CreatorRunner(imgDB, imgSize, products.remove(0), shapesPerImage,
        categoryImages.getOrDefault(categories.remove(0), null), workingDir, nrOfImagesGenerated);
  }

}
