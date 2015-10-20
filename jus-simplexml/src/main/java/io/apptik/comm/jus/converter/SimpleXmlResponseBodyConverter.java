/*
 * Copyright (C) 2015 AppTik Project
 * Copyright (C) 2015 Square, Inc.
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
package io.apptik.comm.jus.converter;


import org.simpleframework.xml.Serializer;

import java.io.IOException;
import java.io.InputStream;

import io.apptik.comm.jus.Converter;
import io.apptik.comm.jus.NetworkResponse;

public final class SimpleXmlResponseBodyConverter<T> implements Converter<NetworkResponse, T> {
  private final Class<T> cls;
  private final Serializer serializer;
  private final boolean strict;

  public SimpleXmlResponseBodyConverter(Class<T> cls, Serializer serializer, boolean strict) {
    this.cls = cls;
    this.serializer = serializer;
    this.strict = strict;
  }

  @Override public T convert(NetworkResponse value) throws IOException {
    InputStream is = value.getByteStream();
    try {
      T read = serializer.read(cls, is, strict);
      if (read == null) {
        throw new IllegalStateException("Could not deserialize body as " + cls);
      }
      return read;
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
  }
}
