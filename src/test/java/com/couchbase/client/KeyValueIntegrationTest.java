package com.couchbase.client;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.operations.Operation;
import com.couchbase.client.operations.Operations;
import com.couchbase.client.util.CouchbaseResources;
import com.couchbase.client.util.TestInMemoryRequestTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.couchbase.client.InMemoryTracerOptions.inMemoryTracerOptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Timeout(30)
public class KeyValueIntegrationTest {
  private static final TestInMemoryRequestTracer testTracer = new TestInMemoryRequestTracer(inMemoryTracerOptions());
  private static final CouchbaseResources test = new CouchbaseResources(testTracer.tracer());

  @BeforeAll
  public static void setup() {
    test.cluster().waitUntilReady(Duration.ofSeconds(10));
  }

  @AfterAll
  public static void teardown() {
    test.close();
  }

  @Test
  public void upsert() {
    Collection collection = test.bucket().defaultCollection();
    collection.upsert("id", JsonObject.create());
    InMemoryRequestTracerHandlerOperations ops = testTracer.waitForNonEmptyOperationsAndClear();
    assertEquals(1, ops.operations().size());
    List<Operation> operations = ops.operations().operations();
    assertEquals(1, operations.size());
    Operation op = operations.get(0);
    assertEquals("upsert", op.name());
    assertEquals(test.bucket().name(), op.bucket());
    assertEquals(collection.scopeName(), op.scope());
    assertEquals(collection.name(), op.collection());
    assertEquals(0, op.retries());
    assertNull(op.statement());
    assertEquals("id", op.documentId());

    // Just check nothing is thrown
    ExampleReports.exampleAggregatedReport(ops);
    ExampleReports.exampleOperationsOutput(ops);
  }

  @Test
  public void mixedKvOperations() {
    Collection collection = test.bucket().defaultCollection();
    collection.upsert("id1", JsonObject.create());
    collection.upsert("id2", JsonObject.create());
    collection.remove("id1");
    collection.get("id2");

    InMemoryRequestTracerHandlerOperations ops = testTracer.waitForNonEmptyOperationsAndClear();
    assertEquals(4, ops.operations().size());

    Map<String, Operations> groupedByOpType = ops.operations().groupByOperationType();

    assertEquals(3, groupedByOpType.size());
    assertEquals(2, groupedByOpType.get("upsert").operations().size());
    assertEquals(1, groupedByOpType.get("remove").operations().size());
    assertEquals(1, groupedByOpType.get("get").operations().size());

    Map<String, Operations> groupedByService = ops.operations().groupByService();

    assertEquals(1, groupedByService.size());
    assertEquals(4, groupedByService.get("kv").operations().size());

    // Just check nothing is thrown
    ExampleReports.exampleAggregatedReport(ops);
    ExampleReports.exampleOperationsOutput(ops);
  }
}
