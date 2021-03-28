package com.beancounter.common.utils;

import com.beancounter.common.exception.BusinessException;
import java.nio.ByteBuffer;
import java.util.UUID;

public final class KeyGenUtils {
  private static final char[] CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
  private static final int[] i256 = new int[256];

  static {
    for (int i = 0; i < CHARS.length; i++) {
      i256[CHARS[i]] = i;
    }
  }

  private KeyGenUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Given a UUID instance, return a short (22-character) string
   * representation of it.
   *
   * @param uuid a UUID instance.
   * @return a short string representation of the UUID.
   * @throws BusinessException  if the UUID instance is null.
   * @throws IllegalArgumentException if the underlying UUID implementation is not 16 bytes.
   */
  public static String format(UUID uuid) {
    if (uuid == null) {
      throw new BusinessException("Null UUID");
    }

    byte[] bytes = toByteArray(uuid);
    return encodeBase64(bytes);
  }

  /**
   * Given a UUID representation (either a short or long form), return a
   * UUID from it.
   *
   * <p>If the uuidString is longer than our short, 22-character form (or 24 with padding),
   * it is assumed to be a full-length 36-character UUID string.
   *
   * @param uuidString a string representation of a UUID.
   * @return a UUID instance
   * @throws IllegalArgumentException if the uuidString is not a valid UUID representation.
   * @throws BusinessException     if the uuidString is null.
   */
  public static UUID parse(String uuidString) {
    if (uuidString == null || uuidString.isEmpty()) {
      throw new BusinessException("Invalid UUID string");
    }

    if (uuidString.length() > 24) {
      return UUID.fromString(uuidString);
    }

    if (uuidString.length() < 22) {
      throw new BusinessException("Short UUID must be 22 characters: " + uuidString);
    }

    byte[] bytes = decodeBase64(uuidString);
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.put(bytes, 0, 16);
    bb.clear();
    return new UUID(bb.getLong(), bb.getLong());
  }

  /**
   * Extracts the bytes from a UUID instance in MSB, LSB order.
   *
   * @param uuid a UUID instance.
   * @return the bytes from the UUID instance.
   */
  private static byte[] toByteArray(UUID uuid) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  /**
   * Accepts a UUID byte array (of exactly 16 bytes) and base64 encodes it, using a URL-safe
   * encoding scheme.  The resulting string will be 22 characters in length with no extra
   * padding on the end (e.g. no "==" on the end).
   *
   * <p>Base64 encoding takes each three bytes from the array and converts them into
   * four characters.  This implementation, not using padding, converts the last byte into two
   * characters.
   *
   * @param bytes a UUID byte array.
   * @return a URL-safe base64-encoded string.
   */
  private static String encodeBase64(byte[] bytes) {

    // Output is always 22 characters.
    char[] chars = new char[22];

    int i = 0;
    int j = 0;

    while (i < 15) {
      // Get the next three bytes.
      int d = (bytes[i++] & 0xff) << 16 | (bytes[i++] & 0xff) << 8 | (bytes[i++] & 0xff);

      // Put them in these four characters
      chars[j++] = CHARS[(d >>> 18) & 0x3f];
      chars[j++] = CHARS[(d >>> 12) & 0x3f];
      chars[j++] = CHARS[(d >>> 6) & 0x3f];
      chars[j++] = CHARS[d & 0x3f];
    }

    // The last byte of the input gets put into two characters at the end of the string.
    int d = (bytes[i] & 0xff) << 10;
    chars[j++] = CHARS[d >> 12];
    chars[j] = CHARS[(d >>> 6) & 0x3f];
    return new String(chars);
  }

  /**
   * Base64 decodes a short, 22-character UUID string (or 24-characters with padding)
   * into a byte array. The resulting byte array contains 16 bytes.
   *
   * <p>Base64 decoding essentially takes each four characters from the string and converts
   * them into three bytes. This implementation, not using padding, converts the final
   * two characters into one byte.
   *
   * @param s key
   * @return bytes
   */
  private static byte[] decodeBase64(String s) {

    // Output is always 16 bytes (UUID).
    byte[] bytes = new byte[16];
    int i = 0;
    int j = 0;

    while (i < 15) {
      // Get the next four characters.
      int d = i256[s.charAt(j++)] << 18
          | i256[s.charAt(j++)] << 12
          | i256[s.charAt(j++)] << 6
          | i256[s.charAt(j++)];

      // Put them in these three bytes.
      bytes[i++] = (byte) (d >> 16);
      bytes[i++] = (byte) (d >> 8);
      bytes[i++] = (byte) d;
    }

    // Add the last two characters from the string into the last byte.
    bytes[i] = (byte) ((i256[s.charAt(j++)] << 18 | i256[s.charAt(j)] << 12) >> 16);
    return bytes;
  }

  public static String getId() {
    return format(UUID.randomUUID());
  }
}
