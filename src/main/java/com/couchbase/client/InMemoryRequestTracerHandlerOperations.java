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
import com.couchbase.client.spans.SpansForOperation;
import com.couchbase.client.operations.Operation;
import com.couchbase.client.operations.Operations;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provided to the {@link InMemoryRequestTracerHandler}, and contains all operations since the last time the handler was called.
 */
@Stability.Volatile
public class InMemoryRequestTracerHandlerOperations {
  private final Operations operations;

  @Stability.Internal
  public InMemoryRequestTracerHandlerOperations(List<SpansForOperation> spans) {
    this.operations = new Operations(spans.stream()
      .map(Operation::new)
      .collect(Collectors.toList()));
  }

  /**
   * The operations that have happened since the last time the handler was called.
   */
  public Operations operations() {
    return operations;
  }
}
