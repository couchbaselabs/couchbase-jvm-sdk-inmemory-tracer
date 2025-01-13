package com.couchbase.client.util;

import com.couchbase.client.InMemoryRequestTracer;
import com.couchbase.client.InMemoryRequestTracerHandler;
import com.couchbase.client.InMemoryRequestTracerHandlerOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.util.Collections.singletonList;

public class TestInMemoryRequestTracer {
  private final InMemoryRequestTracer tracer;
  private final ConcurrentLinkedQueue<InMemoryRequestTracerHandlerOperations> operations = new ConcurrentLinkedQueue<>();

  public TestInMemoryRequestTracer() {
    InMemoryRequestTracerHandler handler = (ops, sinceLastReport) -> {
      operations.add(ops);
    };
    tracer = InMemoryRequestTracer.builder()
      .handlers(singletonList(handler))
      .interval(Duration.ofMillis(100)) // Make tests quick
      .build();
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
