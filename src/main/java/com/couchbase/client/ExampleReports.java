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
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.node.ArrayNode;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.node.ObjectNode;
import com.couchbase.client.core.json.Mapper;
import com.couchbase.client.operations.ExceptionStats;
import com.couchbase.client.operations.NetworkCalls;
import com.couchbase.client.operations.RequestEncodings;
import com.couchbase.client.util.OperationsToJson;

import java.util.Map;

/**
 * Example reports, for use by {@link ExampleHandlers}.
 * <p>
 * There are many many ways to aggregate metrics, and users are strongly encouraged to copy these reports and use them as a starting point for
 * their own custom reports.
 */
@Stability.Volatile
public class ExampleReports {
  private ExampleReports() {
  }

  public static ArrayNode exampleOperationsOutput(InMemoryRequestTracerHandlerOperations operations) {
    return OperationsToJson.toJson(operations.operations());
  }

  /**
   * All of this library is at a volatile status, and this report should be regarded as particularly volatile.
   * If a stable interface is required, users should copy this method and modify it to suit their needs.
   */
  public static ObjectNode exampleAggregatedReport(InMemoryRequestTracerHandlerOperations operations) {
    ObjectNode out = Mapper.createObjectNode();

    operations.operations().groupByService().forEach((service, operationsForService) -> {
      ObjectNode jsonForService = Mapper.createObjectNode();

      operationsForService.groupByOperationType().forEach((operationType, operationsForOperationType) -> {
        ObjectNode jsonForOperation = Mapper.createObjectNode();

        operationsForService.groupByIfSucceeded().forEach((didSucceed, operationsForIfSucceeded) -> {

          Durations durations = operationsForIfSucceeded.durationsMicroseconds();
          NetworkCalls networkCalls = operationsForIfSucceeded.networkCalls();
          RequestEncodings requestEncodings = operationsForIfSucceeded.requestEncodings();

          ObjectNode leaf = Mapper.createObjectNode()
            .set("counts", Mapper.createObjectNode()
              .put("uniqueDocumentIds", operationsForIfSucceeded.documentIds().size())
              .put("operations", durations.count()));

          if (!didSucceed) {
            Map<String, ExceptionStats> exceptionCounts = operationsForIfSucceeded.exceptionStats();

            ObjectNode exceptionsJson = Mapper.createObjectNode();
            exceptionCounts.forEach((k, v) -> exceptionsJson.set(k,
              Mapper.createObjectNode().put("count", v.count())));
            leaf.set("exceptions", exceptionsJson);
          }

          leaf.set("operationDurationsMicros", Mapper.createObjectNode().set("metrics", metricsFrom(durations)));
          jsonForOperation.set(didSucceed ? "successfulOps" : "failedOps", leaf);

          Durations serverDurations = networkCalls.serverDurationsMicroseconds();
          if (serverDurations.count() > 0) {
            leaf.set("serverDurationsMicros", Mapper.createObjectNode().set("metrics", metricsFrom(serverDurations)));
          }

          if (requestEncodings.size() > 0) {
            Durations requestEncodingsDurations = requestEncodings.durationsMicroseconds();
            leaf.set("requestEncodingDurationsMicros", Mapper.createObjectNode().set("metrics", metricsFrom(requestEncodingsDurations)));
          }
        });

        jsonForService.set(operationType, jsonForOperation);
      });

      out.set(service, jsonForService);
    });

    return out;
  }

  public static ArrayNode metricsFrom(Durations durations) {
    return Mapper.createArrayNode()
      .add(Mapper.createObjectNode().put("name", "count").put("value", durations.count()))
      .add(Mapper.createObjectNode().put("name", "min").put("value", durations.min()))
      .add(Mapper.createObjectNode().put("name", "median").put("value", durations.median()))
      .add(Mapper.createObjectNode().put("name", "p95").put("value", durations.percentile(0.95)))
      .add(Mapper.createObjectNode().put("name", "max").put("value", durations.max()));
  }
}
