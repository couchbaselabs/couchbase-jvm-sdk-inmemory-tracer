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
import com.couchbase.client.core.cnc.RequestSpan;
import com.couchbase.client.core.cnc.RequestTracer;
import com.couchbase.client.core.cnc.tracing.NoopRequestSpan;
import com.couchbase.client.spans.InMemoryRequestSpan;
import com.couchbase.client.spans.SpansForOperation;
import com.couchbase.client.util.InMemoryRequestTracerHandlerOperationsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A RequestTracer is called whenever the SDK wants to create a new OpenTelemetry span.
 * This RequestTracer implementation holds on to those spans in memory, and periodically calls a handler with those spans.
 */
@Stability.Volatile
public class InMemoryRequestTracer implements RequestTracer {
  private static final Logger logger = LoggerFactory.getLogger(InMemoryRequestTracer.class);

  private List<InMemoryRequestSpan> spans = new ArrayList<>();
  private final InMemoryTracerOptions.Built options;
  private final AtomicReference<LocalDateTime> lastUpdate = new AtomicReference<>(LocalDateTime.now());
  private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

  public InMemoryRequestTracer() {
    this(InMemoryTracerOptions.inMemoryTracerOptions());
  }

  public InMemoryRequestTracer(InMemoryTracerOptions options) {
    this.options = options.build();

    cleanupExecutor.scheduleAtFixedRate(this::callHandler, 0, this.options.interval().toMillis(), TimeUnit.MILLISECONDS);
  }

  @Stability.Internal
  @Override
  public synchronized RequestSpan requestSpan(String name, RequestSpan parent) {
    // Intentionally discarding non-DebugRequestSpan parents here.  This means we don't need to worry about app-provided parent spans
    // on operation spans, and can simplify the logic.
    try {
      InMemoryRequestSpan out = new InMemoryRequestSpan(name, parent instanceof InMemoryRequestSpan ? (InMemoryRequestSpan) parent : null);
      spans.add(out);
      return out;
    } catch (Exception e) {
      logger.warn("Error creating DebugRequestSpan: ", e);
      return NoopRequestSpan.INSTANCE;
    }
  }

  @Stability.Internal
  @Override
  public Mono<Void> start() {
    return Mono.empty();
  }

  @Stability.Internal
  @Override
  public Mono<Void> stop(Duration timeout) {
    cleanupExecutor.shutdown();
    try {
      cleanupExecutor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return Mono.empty();
  }

  private synchronized void callHandler() {
    LocalDateTime now = LocalDateTime.now();
    Duration sinceLastUpdate = Duration.between(lastUpdate.get(), now);
    Tuple2<List<SpansForOperation>, List<InMemoryRequestSpan>> ops =
      InMemoryRequestTracerHandlerOperationsUtil.associateTopLevelSpansWithChildren(spans);

    options.handlers().forEach(handler -> {
      try {
        handler.evaluate(new InMemoryRequestTracerHandlerOperations(ops.getT1()), sinceLastUpdate);
      } catch (Exception e) {
        logger.error("Handler failed: ", e);
      }
    });

    // As a precaution against bugs, kill any old dangling spans.
    ZonedDateTime tooOld = ZonedDateTime.now().minus(sinceLastUpdate.multipliedBy(2));
    spans = ops.getT2()
      .stream()
      .filter(v -> v.startLocal().isAfter(tooOld))
      .collect(Collectors.toList());
    lastUpdate.set(now);
  }

  /**
   * Returns a list of all spans currently stored in memory.
   * <p>
   * Generally users should prefer to use the handler mechanism to access spans
   */
  public synchronized List<InMemoryRequestSpan> spans() {
    // defensive copy
    return new ArrayList<>(spans);
  }
}
