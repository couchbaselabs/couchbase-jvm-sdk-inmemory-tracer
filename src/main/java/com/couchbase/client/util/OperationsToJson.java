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
package com.couchbase.client.util;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.node.ArrayNode;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.node.ObjectNode;
import com.couchbase.client.core.json.Mapper;
import com.couchbase.client.operations.NetworkCall;
import com.couchbase.client.operations.NetworkCalls;
import com.couchbase.client.operations.Operation;
import com.couchbase.client.operations.Operations;
import com.couchbase.client.operations.RequestEncoding;

import java.time.Duration;
import java.time.Instant;

import static com.couchbase.client.util.DurationUtil.toMicros;

@Stability.Internal
public class OperationsToJson {
  private OperationsToJson() {
  }

  public static String toJson(Instant instant) {
    return instant.toString();
  }

  public static ArrayNode toJson(Operations operation) {
    ArrayNode out = Mapper.createArrayNode();
    for (Operation call : operation.operations()) {
      out.add(toJson(call));
    }
    return out;
  }

  public static ObjectNode toJson(Operation operation) {
    ObjectNode out = Mapper.createObjectNode();
    out.put("name", operation.name())
      .put("service", operation.service())
      .put("start", toJson(operation.start()))
      .put("durationUs", toMicros(operation.duration()));
    String statement = operation.statement();
    String documentId = operation.documentId();
    String bucket = operation.bucket();
    String scope = operation.scope();
    String collection = operation.collection();
    if (statement != null) {
      out.put("statement", statement);
    }
    if (documentId != null) {
      out.put("documentId", documentId);
    }
    if (bucket != null) {
      out.put("bucket", bucket);
    }
    if (scope != null) {
      out.put("scope", scope);
    }
    if (collection != null) {
      out.put("collection", collection);
    }
    Throwable exception = operation.exception();
    if (exception != null) {
      out.put("exception", exception.toString());
    }
    out.put("retries", operation.retries());
    RequestEncoding re = operation.requestEncoding();
    if (re != null) {
      out.set("requestEncoding", toJson(re));
    }
    out.set("networkCalls", toJson(operation.networkCalls()));
    return out;
  }

  public static ObjectNode toJson(RequestEncoding op) {
    ObjectNode out = Mapper.createObjectNode();
    out.put("durationUs", toMicros(op.duration()))
      .put("start", toJson(op.start()));
    return out;
  }

  public static ObjectNode toJson(NetworkCall call) {
    ObjectNode out = Mapper.createObjectNode();
    out.put("durationUs", toMicros(call.duration()))
      .put("start", toJson(call.start()))
      .put("host", call.remoteHost())
      .put("port", call.remotePort());
    String durability = call.durability();
    if (durability != null) {
      out.put("durability", call.durability());
    }
    Duration serverDuration = call.serverDuration();
    if (serverDuration != null) {
      out.put("serverDurationUs", toMicros(serverDuration));
    }
    return out;
  }

  public static ArrayNode toJson(NetworkCalls calls) {
    ArrayNode out = Mapper.createArrayNode();
    for (NetworkCall call : calls.networkCalls()) {
      out.add(toJson(call));
    }
    return out;
  }
}
