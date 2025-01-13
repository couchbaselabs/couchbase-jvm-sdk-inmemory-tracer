/*
 * Copyright (c) 2025 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.spans;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.cnc.RequestSpan;
import com.couchbase.client.core.msg.RequestContext;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

/**
 * Used to store everything that would normally be provided to OpenTelemetry for an individual
 * span, in-memory instead.
 * <p>
 * It is recommended that users use the higher-level abstractions instead, as it is more likely that changes will be made to these
 * underlying spans and attributes, which will if possible be abstracted away by the higher-level classes.
 */
@Stability.Internal
public class InMemoryRequestSpan implements RequestSpan {
  private final String name;
  private final InMemoryRequestSpan parent;
  private final long startNanos = System.nanoTime();
  private long endNanos = System.nanoTime();
  private final Instant startInstant = Instant.now();
  private final HashMap<String, Object> attributes = new HashMap<>();
  private @Nullable Throwable exception = null;
  private RequestSpan.@Nullable StatusCode status;

  public InMemoryRequestSpan(String name, InMemoryRequestSpan parent) {
    this.name = name;
    this.parent = parent;
  }

  @Override
  public void attribute(String key, String value) {
    attributes.put(key, value);
  }

  @Override
  public void attribute(String key, boolean value) {
    attributes.put(key, value);
  }

  @Override
  public void attribute(String key, long value) {
    // This will box, but isn't used much
    attributes.put(key, value);
  }

  @Override
  public void event(String name, Instant timestamp) {
    // Not currently recorded
  }

  @Override
  public void status(RequestSpan.StatusCode status) {
    this.status = status;
  }

  @Override
  public void end() {
    endNanos = System.nanoTime();
  }

  @Override
  public void recordException(Throwable err) {
    exception = err;
  }

  @Override
  public void requestContext(RequestContext requestContext) {
  }

  public String name() {
    return name;
  }

  public InMemoryRequestSpan parent() {
    return parent;
  }

  public long startNanos() {
    return startNanos;
  }

  public Instant startInstant() {
    return startInstant;
  }

  public long endNanos() {
    return endNanos;
  }

  public HashMap<String, Object> attributes() {
    return new HashMap<>(attributes);
  }

  public @Nullable String attributeString(String key) {
    Object out = attributes.get(key);
    if (out instanceof String) {
      return (String) out;
    }
    return null;
  }

  public @Nullable Long attributeLong(String key) {
    Object out = attributes.get(key);
    if (out instanceof Long) {
      return (Long) out;
    }
    return null;
  }

  public @Nullable Object attribute(String key) {
    return attributes.get(key);
  }

  @Nullable
  public Throwable exception() {
    return exception;
  }

  public Duration duration() {
    return Duration.ofNanos(endNanos - startNanos);
  }
}
