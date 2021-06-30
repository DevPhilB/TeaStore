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
package utilities.datamodel;

public record ImageSize(
        int width,
        int height
) {
  public static String IMAGE_SIZE_DIVIDER = "x";

  /**
   * Calculates number of pixels of an image.
   * @return number of pixels
   */
  public int getPixelCount() {
    return width * height;
  }

  /**
   * Parses from String.
   * @param str String to pars from
   * @return ImageSize object
   */
  public static ImageSize parseImageSize(String str) {
    if (str == null) {
      throw new NullPointerException("Supplied string is null.");
    }
    if (str.isEmpty()) {
      throw new IllegalArgumentException("Supplied string is empty.");
    }

    String[] tmp = str.trim().split(IMAGE_SIZE_DIVIDER);
    if (tmp.length != 2) {
      throw new IllegalArgumentException("Malformed string supplied. Does not contain exactly two size "
          + "values divided by \"" + IMAGE_SIZE_DIVIDER + "\".");
    }

    int width = 0;
    int height = 0;

    try {
      width = Integer.parseInt(tmp[0].trim());
      height = Integer.parseInt(tmp[1].trim());
    } catch (NumberFormatException parseException) {
      throw new IllegalArgumentException("Malformed string supplied. Cannot parse size values.");
    }

    return new ImageSize(width, height);
  }
}
