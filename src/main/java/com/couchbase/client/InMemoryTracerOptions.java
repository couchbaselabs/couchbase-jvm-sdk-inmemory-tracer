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
package com.couchbase.client;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.util.CbCollections;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows customizing options for the {@link InMemoryRequestTracer}.
 */
@Stability.Volatile
public class InMemoryTracerOptions {
  private static final List<InMemoryRequestTracerHandler> DEFAULT_HANDLERS = CbCollections.listOf(ExampleHandlers::writeAggregatedReport);
  private static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(10);

  private List<InMemoryRequestTracerHandler> handlers = DEFAULT_HANDLERS;
  private Duration interval = DEFAULT_INTERVAL;

  /**
   * The options should only be instantiated through the {@link #inMemoryTracerOptions()} static method.
   */
  protected InMemoryTracerOptions() {
  }

  /**
   * Returns an options builder.
   */
  public static InMemoryTracerOptions inMemoryTracerOptions() {
    return new InMemoryTracerOptions();
  }

  /**
   * Sets the handlers, which will be called on a periodic basic ({@link this#interval(Duration)}) with the operations
   * that have occurred since the last time it was called.
   * <p>
   * The handlers will be called in the order they are provided.
   * <p>
   * The default is to call {@link ExampleHandlers#writeAggregatedReport(InMemoryRequestTracerHandlerOperations, Duration)}}.
   *
   * @return the same {@link InMemoryTracerOptions} for chaining purposes.
   */
  public InMemoryTracerOptions handlers(List<InMemoryRequestTracerHandler> handlers) {
    this.handlers = new ArrayList<>(handlers);
    return this;
  }

  /**
   * Sets how often the handlers are called.  Defaults to every 10 seconds.
   *
   * @return the same {@link InMemoryTracerOptions} for chaining purposes.
   */
  public InMemoryTracerOptions interval(final Duration interval) {
    this.interval = interval;
    return this;
  }

  @Stability.Internal
  public Built build() {
    return new Built();
  }

  @Stability.Internal
  public class Built {
    public List<InMemoryRequestTracerHandler> handlers() {
      return handlers;
    }

    public Duration interval() {
      return interval;
    }
  }
} 
