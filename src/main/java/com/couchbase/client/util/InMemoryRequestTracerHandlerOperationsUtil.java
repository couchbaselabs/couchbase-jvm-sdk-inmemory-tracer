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
import com.couchbase.client.spans.InMemoryRequestSpan;
import com.couchbase.client.spans.SpansForOperation;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Stability.Internal
public class InMemoryRequestTracerHandlerOperationsUtil {
  private InMemoryRequestTracerHandlerOperationsUtil() {
  }

  public static Tuple2<List<SpansForOperation>, List<InMemoryRequestSpan>> associateTopLevelSpansWithChildren(List<InMemoryRequestSpan> spans) {
    HashMap<InMemoryRequestSpan, List<InMemoryRequestSpan>> operationSpans = new HashMap<>();

    // First-pass: look for the top-level spans (operations)
    for (InMemoryRequestSpan span : spans) {
      if (span.parent() == null) {
        operationSpans.put(span, new ArrayList<>());
      }
    }

    // Can have e.g. dispatch_to_server spans that have been recorded when their parent operation span has not yet been completed.
    List<InMemoryRequestSpan> doNotHaveTopLevelSpansYet = new ArrayList<>();

    // Second-pass: associate children with their parents.
    // The SDK (currently) only has two levels of span, so this is sufficient.
    for (InMemoryRequestSpan span : spans) {
      if (span.parent() != null) {
        List<InMemoryRequestSpan> operationSpan = operationSpans.get(span.parent());
        if (operationSpan != null) {
          operationSpan.add(span);
        } else {
          doNotHaveTopLevelSpansYet.add(span);
        }
      }
    }

    List<SpansForOperation> ret = operationSpans.entrySet().stream()
      .map(k -> new SpansForOperation(k.getKey(), k.getValue()))
      .collect(Collectors.toList());

    return Tuples.of(ret, doNotHaveTopLevelSpansYet);
  }
}
