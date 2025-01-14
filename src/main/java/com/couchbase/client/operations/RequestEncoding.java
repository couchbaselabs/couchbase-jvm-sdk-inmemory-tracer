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
import com.couchbase.client.spans.InMemoryRequestSpan;

import java.time.Duration;
import java.time.Instant;

/**
 * An operation generally needs to encode the request before sending it over the wire, which is represented by this class.
 */
@Stability.Volatile
public class RequestEncoding {
  private final InMemoryRequestSpan requestEncodingSpan;

  @Stability.Internal
  public RequestEncoding(InMemoryRequestSpan requestEncodingSpan) {
    this.requestEncodingSpan = requestEncodingSpan;
  }

  /**
   * Returns the underlying span, though users should prefer this higher-level abstraction where possible.
   */
  public InMemoryRequestSpan span() {
    return requestEncodingSpan;
  }

  /**
   * How long this network call took, from the SDK's point of view.
   */
  public Duration duration() {
    return span().duration();
  }

  /**
   * When this network call started, from the SDK's point of view.
   */
  public Instant start() {
    return span().startInstant();
  }
}
