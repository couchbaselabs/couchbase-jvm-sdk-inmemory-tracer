package com.couchbase.client.util;

import com.couchbase.client.InMemoryRequestTracer;
import com.couchbase.client.InMemoryRequestTracerHandler;
import com.couchbase.client.InMemoryRequestTracerHandlerOperations;
import com.couchbase.client.InMemoryTracerOptions;
import com.couchbase.client.core.util.CbCollections;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TestInMemoryRequestTracer {
  private final InMemoryRequestTracer tracer;
  private final ConcurrentLinkedQueue<InMemoryRequestTracerHandlerOperations> operations = new ConcurrentLinkedQueue<>();

  public TestInMemoryRequestTracer(InMemoryTracerOptions options) {
    InMemoryRequestTracerHandler handler = (ops, sinceLastReport) -> {
      operations.add(ops);
    };
    List<InMemoryRequestTracerHandler> handlers = new ArrayList<>(options.build().handlers());
    handlers.add(handler);
    options.handlers(handlers);
    // Make tests quick
    options.interval(Duration.ofMillis(100));
    tracer = new InMemoryRequestTracer(options);
  }

  public InMemoryRequestTracer tracer() {
    return tracer;
  }

  public InMemoryRequestTracerHandlerOperations waitForNonEmptyOperationsAndClear() {
    while (true) {
      Optional<InMemoryRequestTracerHandlerOperations> first = operations.stream()
        .filter(v -> v.operations().size() > 0)
        .findFirst();

      if (first.isPresent()) {
        operations.clear();
        return first.get();
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
