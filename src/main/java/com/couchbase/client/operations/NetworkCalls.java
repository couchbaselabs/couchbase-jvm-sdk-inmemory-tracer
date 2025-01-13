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
import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.cnc.TracingIdentifiers;
import com.couchbase.client.spans.InMemoryRequestSpan;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A {@link Operation} can consist of zero or more underlying network calls, which are represented by this class.
 */
@Stability.Volatile
public class NetworkCalls {
  private final List<InMemoryRequestSpan> dispatchToServerSpans;

  @Stability.Internal
  public NetworkCalls(List<InMemoryRequestSpan> dispatchToServerSpans) {
    this.dispatchToServerSpans = dispatchToServerSpans;
  }

  /**
   * Returns all server durations reported for all underlying network calls, in microseconds.
   * <p>
   * See {@link NetworkCall#serverDuration} for more details.
   */
  public Durations serverDurationsMicroseconds() {
    return new Durations(dispatchToServerSpans.stream()
      .map((o -> o.attributeLong(TracingIdentifiers.ATTR_SERVER_DURATION)))
      .filter(Objects::nonNull));
  }

  /**
   * Returns the underlying spans, though users should prefer this higher-level abstraction where possible e.g. {@link #networkCalls()}.
   */
  public List<InMemoryRequestSpan> spans() {
    return dispatchToServerSpans;
  }

  /**
   * Returns a list of {@link NetworkCall}s, one for each underlying network call.
   */
  public List<NetworkCall> networkCalls() {
    return dispatchToServerSpans.stream()
      .map(NetworkCall::new)
      .collect(Collectors.toList());
  }
}


