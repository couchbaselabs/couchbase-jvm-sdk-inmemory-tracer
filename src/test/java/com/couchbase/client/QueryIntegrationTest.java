package com.couchbase.client;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.operations.Operation;
import com.couchbase.client.util.CouchbaseResources;
import com.couchbase.client.util.TestInMemoryRequestTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.List;

import static com.couchbase.client.InMemoryTracerOptions.inMemoryTracerOptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Timeout(30)
public class QueryIntegrationTest {
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
  public void query() {
    String statement = "SELECT 'hello' AS GREETING";
    test.cluster().query(statement);
    InMemoryRequestTracerHandlerOperations ops = testTracer.waitForNonEmptyOperationsAndClear();
    assertEquals(1, ops.operations().size());
    List<Operation> operations = ops.operations().operations();
    assertEquals(1, operations.size());
    Operation op = operations.get(0);
    assertEquals("query", op.name());
    assertNull(op.bucket());
    assertNull(op.scope());
    assertNull(op.collection());
    assertEquals(0, op.retries());
    assertEquals(statement, op.statement());
    assertNull(op.documentId());

    // Just check nothing is thrown
    ExampleReports.exampleAggregatedReport(ops);
    ExampleReports.exampleOperationsOutput(ops);
  }
}
