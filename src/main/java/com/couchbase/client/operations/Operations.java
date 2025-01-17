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
package com.couchbase.client.operations;

import com.couchbase.client.Durations;
import com.couchbase.client.InMemoryRequestTracerHandlerOperations;
import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.cnc.TracingIdentifiers;
import com.couchbase.client.spans.InMemoryRequestSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.couchbase.client.util.DurationUtil.toMicros;

/**
 * Wraps a list of {@link Operation} instances.
 * <p>
 * This is intended to help with aggregating and analyzing multiple operations at once.
 * It allows easily grouping operations, so you can get all the operations for each service type, and then for each of those operation
 * sets, get all the operations of each operation type, etc.
 * <p>
 * See {@link com.couchbase.client.ExampleReports#exampleAggregatedReport(InMemoryRequestTracerHandlerOperations)} for an example of
 * how to use this.
 */
@Stability.Volatile
public class Operations {
  private final List<Operation> operations;

  @Stability.Internal
  public Operations(List<Operation> operations) {
    this.operations = operations;
  }

  /**
   * Groups the operations by operation type ("upsert", "query"), etc.
   * <p>
   * Returns a Map where the key is the operation type, and the value for each key are all the operations of that operation type,
   * that are inside this Operations object.
   */
  public Map<String, Operations> groupByOperationType() {
    return operations.stream()
      .collect(Collectors.groupingBy(
        Operation::name,
        Collectors.collectingAndThen(
          Collectors.toList(),
          Operations::new
        )
      ));
  }

  /**
   * Groups the operations by service ("kv", "query"), etc.
   * <p>
   * Returns a Map where the key is the service, and the value for each key are all the operations for that service,
   * that are inside this Operations object.
   */
  public Map<String, Operations> groupByService() {
    return operations.stream()
      .filter(op -> op.service() != null)
      .collect(Collectors.groupingBy(
        Operation::service,
        Collectors.collectingAndThen(
          Collectors.toList(),
          Operations::new
        )
      ));
  }

  /**
   * Groups the operations by whether they succeeded or not.
   * <p>
   * Here, succeeded is defined by whether an exception was raised to the user.  So a KV get that raised DocumentNotFoundException
   * will be regarded as failed, for example, though in a caching scenario this may not be considered a true failure by the user.
   * <p>
   * Returns a Map where the key is whether the operation succeeded, and the value for each key are all the operations for status,
   * that are inside this Operations object.
   */
  public Map<Boolean, Operations> groupByIfSucceeded() {
    return operations.stream()
      .collect(Collectors.groupingBy(
        op -> op.spans().span().exception() == null,
        Collectors.collectingAndThen(
          Collectors.toList(),
          Operations::new
        )
      ));
  }

  /**
   * Returns a set of all the document IDs that were involved in all operations in this object.
   */
  public Set<String> documentIds() {
    Set<String> documentIds = new HashSet<>();

    operations.forEach(o -> {
      Object documentId = o.spans().span().attribute(TracingIdentifiers.ATTR_DOCUMENT_ID);
      if (documentId instanceof String) {
        documentIds.add((String) documentId);
      }
    });

    return documentIds;
  }

  /**
   * Returns all the durations (from the SDK's point of view) of all operations
   * in this object, in microseconds.
   */
  public Durations durationsMicroseconds() {
    return new Durations(operations.stream().map(o -> toMicros(o.duration())));
  }

  /**
   * Return a map of exception class names to the stats for that exception, across all operations in this object.
   */
  public Map<String, ExceptionStats> exceptionStats() {
    HashMap<String, Integer> exceptionCounts = new HashMap<>();

    operations.forEach(op -> {
      Throwable exception = op.spans().span().exception();
      if (exception != null) {
        String exceptionName = exception.getClass().getSimpleName();
        exceptionCounts.merge(exceptionName, 1, Integer::sum);
      }
    });

    return exceptionCounts.entrySet().stream()
      .map(e -> new ExceptionStats(e.getKey(), e.getValue()))
      .collect(Collectors.toMap(ExceptionStats::exceptionSimpleName, e -> e));
  }

  /**
   * Returns all network calls made by all operations in this object.
   */
  public NetworkCalls networkCalls() {
    List<InMemoryRequestSpan> dispatchToServerSpans = operations.stream()
      .flatMap(o -> o.networkCalls().spans().stream())
      .collect(Collectors.toList());
    return new NetworkCalls(dispatchToServerSpans);
  }


  /**
   * Returns all request encodings made by all operations in this object.
   */
  public RequestEncodings requestEncodings() {
    List<RequestEncoding> spans = operations.stream()
      .map(Operation::requestEncoding)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    return new com.couchbase.client.operations.RequestEncodings(spans);
  }

  /**
   * Returns the underlying list of operations.
   */
  public List<Operation> operations() {
    return new ArrayList<>(operations);
  }

  /**
   * Returns the number of operations in this object.
   */
  public int size() {
    return operations.size();
  }
}
