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
import com.couchbase.client.spans.InMemoryRequestSpan;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * A {@link Operation} can consist of zero or more underlying network calls.  Each is represented by an instance of this class.
 */
@Stability.Volatile
public class NetworkCall {
  private final InMemoryRequestSpan span;

  @Stability.Internal
  public NetworkCall(InMemoryRequestSpan span) {
    this.span = span;
  }

  /**
   * Returns how long the server reported this operation took.
   * <p>
   * This is only available for KV operations, and only if the server reported it.
   * <p>
   * This metric should not be taken completely literally.  For performance reasons, the server will only start its internal timer
   * for this operation when it takes the item off a network queue, and will stop the timer when it returns the item to the queue.
   * So there will be cases where the item was waiting in a full network queue to be processed, and this is not reflected in this
   * time.
   */
  public @Nullable Duration serverDuration() {
    Object serverDuration = span.attribute(TracingIdentifiers.ATTR_SERVER_DURATION);
    if (serverDuration instanceof Long) {
      return Duration.ofNanos((Long) serverDuration * 1000);
    }
    return null;
  }

  /**
   * How long this network call took, from the SDK's point of view.
   */
  public Duration duration() {
    return span.duration();
  }

  /**
   * When this network call started, from the SDK's point of view.
   */
  public ZonedDateTime start() {
    return span.startLocal();
  }

  /**
   * Returns, if available, the remote host this network call was sent to.  This will usually be a hostname or IP address.
   */
  public @Nullable String remoteHost() {
    return span.attributeString(TracingIdentifiers.ATTR_REMOTE_HOSTNAME);
  }

  /**
   * Returns, if available, the remote port this network call was sent to.
   */
  public @Nullable Long remotePort() {
    return span.attributeLong(TracingIdentifiers.ATTR_REMOTE_PORT);
  }

  /**
   * Returns, if available, the durability level requested for this operation.
   * This is generally available only for KV operations.
   */
  public @Nullable String durability() {
    return span.attributeString(TracingIdentifiers.ATTR_DURABILITY);
  }
}
