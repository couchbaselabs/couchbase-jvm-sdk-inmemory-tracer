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

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.cnc.TracingIdentifiers;
import com.couchbase.client.core.logging.RedactableArgument;
import com.couchbase.client.spans.InMemoryRequestSpan;
import com.couchbase.client.spans.SpansForOperation;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Represents an individual operation, such as a KV upsert or SQL++ query.
 */
@Stability.Volatile
public class Operation {
  private final SpansForOperation spans;

  @Stability.Internal
  public Operation(SpansForOperation spans) {
    this.spans = spans;
  }

  /**
   * An operation can consist of zero or more network calls, depending on whether retries
   * were necessary, whether a TCP connection to the service was available, etc.
   */
  public NetworkCalls networkCalls() {
    return new NetworkCalls(spans.children().stream()
      .filter(c -> c.name().equals(TracingIdentifiers.SPAN_DISPATCH))
      .collect(Collectors.toList()));
  }

  /**
   * The name of the operation, such as "upsert" or "query".
   */
  public String name() {
    return spans.span().name();
  }

  /**
   * The service the operation was sent to, such as "kv" or "query".
   */
  public @Nullable String service() {
    return spans.span().attributeString(TracingIdentifiers.ATTR_SERVICE);
  }

  /**
   * Returns all the underlying OpenTelemetry spans that make up this operation.
   * <p>
   * Users should prefer using the higher-level abstractions instead, such as {@link #networkCalls()} and {@link #requestEncoding()}.
   */
  public SpansForOperation spans() {
    return spans;
  }

  /**
   * Returns the request encoding span, if available.
   */
  public @Nullable RequestEncoding requestEncoding() {
    InMemoryRequestSpan s = spans.children().stream()
      .filter(c -> c.name().equals(TracingIdentifiers.SPAN_REQUEST_ENCODING))
      .findFirst()
      .orElse(null);

    if (s == null) {
      return null;
    }

    return new RequestEncoding(s);
  }

  /**
   * Returns how long the overall operation took, from the SDK's point of view.
   */
  public Duration duration() {
    return spans.duration();
  }

  /**
   * Returns when the operation started, from the SDK's point of view.
   */
  public Instant start() {
    return spans.start();
  }

  /**
   * Returns the SQL++ statement that was executed, if available.  It will only be available
   * on specific operations, including SQL++ and analytics queries.
   * <p>
   * If redaction is enabled, it will be redactable at user-level.
   */
  public @Nullable String statement() {
    String out = spans.span().attributeString(TracingIdentifiers.ATTR_STATEMENT);
    if (out != null) {
      return RedactableArgument.redactUser(out).toString();
    }
    return null;
  }

  /**
   * Returns the document-id of this operation, if available.  It will only be available
   * on specific operations, generally KV ones.
   * <p>
   * If redaction is enabled, it will be redactable at user-level.
   */
  public @Nullable String documentId() {
    String out = spans.span().attributeString(TracingIdentifiers.ATTR_DOCUMENT_ID);
    if (out != null) {
      return RedactableArgument.redactUser(out).toString();
    }
    return null;
  }

  /**
   * Returns the name of the bucket this operation was executed against, if available.
   * It will only be available on specific operations.
   * <p>
   * If redaction is enabled, it will be redactable at user-level.
   */
  public @Nullable String bucket() {
    String out = spans.span().attributeString(TracingIdentifiers.ATTR_NAME);
    if (out != null) {
      return RedactableArgument.redactMeta(out).toString();
    }
    return null;
  }

  /**
   * Returns the name of the scope this operation was executed against, if available.
   * It will only be available on specific operations.
   * <p>
   * If redaction is enabled, it will be redactable at user-level.
   */
  public @Nullable String scope() {
    String out = spans.span().attributeString(TracingIdentifiers.ATTR_SCOPE);
    if (out != null) {
      return RedactableArgument.redactMeta(out).toString();
    }
    return null;
  }

  /**
   * Returns the name of the collection this operation was executed against, if available.
   * It will only be available on specific operations.
   * <p>
   * If redaction is enabled, it will be redactable at user-level.
   */
  public @Nullable String collection() {
    String out = spans.span().attributeString(TracingIdentifiers.ATTR_COLLECTION);
    if (out != null) {
      return RedactableArgument.redactMeta(out).toString();
    }
    return null;
  }

  /**
   * Returns the final exception that was raised to the user, iff this operation failed.
   */
  public @Nullable Throwable exception() {
    return spans.span().exception();
  }

  /**
   * Returns how often the operation was retried, if that info is available.
   */
  public @Nullable Long retries() {
    return spans.span().attributeLong(TracingIdentifiers.ATTR_RETRIES);
  }
}
