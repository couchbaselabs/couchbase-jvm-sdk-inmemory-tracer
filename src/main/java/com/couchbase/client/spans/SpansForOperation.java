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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Groups all the spans related to a specific operation.
 * <p>
 * Users are encouraged to use the higher-level abstractions instead.
 */
@Stability.Internal
public class SpansForOperation {
  private final InMemoryRequestSpan span;
  private final List<InMemoryRequestSpan> children;

  public SpansForOperation(InMemoryRequestSpan span, List<InMemoryRequestSpan> children) {
    this.span = span;
    this.children = children;
  }

  public InMemoryRequestSpan span() {
    return span;
  }

  public List<InMemoryRequestSpan> children() {
    return children;
  }

  public String name() {
    return span.name();
  }

  public Duration duration() {
    return span.duration();
  }

  public ZonedDateTime start() {
    return span.startLocal();
  }
}
