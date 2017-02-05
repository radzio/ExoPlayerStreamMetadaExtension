/*
 * Copyright (C) 2017 Radosław Piekarz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.droidlabs.exoplayer.extensions.streammetadata;

import android.util.Log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This is an InputStream which allows to fetch Icecast/Shoutcast metadata from.
 */
public class IcyInputStream extends FilterInputStream {

  private static final String UTF_8_ENCODING = "UTF-8";
  private static final String TAG = IcyInputStream.class.getSimpleName();

  private int period;
  private int remaining;
  private byte[] buffer;
  private MetadataListener metadataListener;
  private String characterEncoding;

  /**
   * Creates a new input stream.
   *
   * @param in the underlying input stream
   * @param period the period of metadata frame is repeating (in bytes)
   */
  public IcyInputStream(InputStream in, int period) {
    this(in, period, null);
  }

  /**
   * Creates a new input stream.
   *
   * @param in the underlying input stream
   * @param period the period of metadata frame is repeating (in bytes)
   * @param metadataListener the callback - may be null
   */
  public IcyInputStream(InputStream in, int period, MetadataListener metadataListener) {
    this(in, period, metadataListener, null);
  }

  /**
   * Creates a new input stream.
   *
   * @param in the underlying input stream
   * @param period the period of metadata frame is repeating (in bytes)
   * @param metadataListener the callback - may be null
   * @param characterEncoding the encoding used for metadata strings - may be null = default is
   * UTF-8
   */
  public IcyInputStream(InputStream in, int period, MetadataListener metadataListener,
      String characterEncoding) {
    super(in);
    this.period = period;
    this.metadataListener = metadataListener;
    this.characterEncoding = characterEncoding != null ? characterEncoding : UTF_8_ENCODING;

    remaining = period;
    buffer = new byte[128];
  }

  @Override
  public int read() throws IOException {
    int readByte = super.read();

    if (--remaining == 0) {
      fetchMetadata();
    }

    return readByte;
  }

  @Override
  public int read(byte[] buffer, int offset, int len) throws IOException {
    int ret = in.read(buffer, offset, remaining < len ? remaining : len);

    if (remaining == ret) {
      fetchMetadata();
    } else {
      remaining -= ret;
    }

    return ret;
  }

  public String getCharacterEncoding() {
    return characterEncoding;
  }

  /**
   * Sets the character encoding used for the metadata strings.
   * By default it is set to UTF-8.
   */
  public void setCharacterEncoding(String characterEncoding) {
    this.characterEncoding = characterEncoding;
  }

  /**
   * This method reads the metadata string.
   * Actually it calls the method parseMetadata().
   */
  private void fetchMetadata() throws IOException {
    remaining = period;

    int size = in.read();

    // either no metadata or eof:
    if (size < 1) {
      return;
    }

    // size *= 16:
    size <<= 4;

    if (buffer.length < size) {
      buffer = null;
      buffer = new byte[size];
      Log.d(TAG, "Enlarged metadata buffer to " + size + " bytes");
    }

    size = readFully(buffer, 0, size);

    // find the string end:
    for (int i = 0; i < size; i++) {
      if (buffer[i] == 0) {
        size = i;
        break;
      }
    }

    String s;

    try {
      s = new String(buffer, 0, size, characterEncoding);
    } catch (Exception e) {
      Log.e(TAG, "Cannot convert bytes to String");
      return;
    }

    Log.d(TAG, "Metadata string: " + s);

    parseMetadata(s);
  }

  /**
   * Parses the metadata and sends them to PlayerCallback.
   *
   * @param metadata the metadata string like: StreamTitle='...';StreamUrl='...';
   */
  private void parseMetadata(String metadata) {
    String[] metadataKeyValues = metadata.split(";");

    for (String keyValue : metadataKeyValues) {
      int n = keyValue.indexOf('=');
      if (n < 1) {
        continue;
      }

      boolean isString = n + 1 < keyValue.length()
          && keyValue.charAt(keyValue.length() - 1) == '\''
          && keyValue.charAt(n + 1) == '\'';

      String key = keyValue.substring(0, n);
      String value = isString ?
          keyValue.substring(n + 2, keyValue.length() - 1) :
          n + 1 < keyValue.length() ?
              keyValue.substring(n + 1) : "";

      if (metadataListener != null) {
        metadataListener.onMetadataRetrieved(key, value);
      }
    }
  }

  /**
   * Tries to read all bytes into the target buffer.
   *
   * @param size the requested size
   * @return the number of really bytes read; if less than requested, then eof detected
   */
  private int readFully(byte[] buffer, int offset, int size) throws IOException {
    int readBytes;
    int initialOffset = offset;

    while (size > 0 && (readBytes = in.read(buffer, offset, size)) != -1) {
      offset += readBytes;
      size -= readBytes;
    }

    return offset - initialOffset;
  }
}

