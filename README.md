# Couchbase JVM In-Memory Request Tracer

The Couchbase JVM SDKs offer excellent introspection capabilities, as every individual operation can be output through OpenTelemetry tracing.

Power users may want to send these tracing spans to a system such as Jaeger or Honeycomb for analysis, but for those who wish to keep things simple, this library offers a plugin tracer that captures the OpenTelemetry spans in-memory.

Users can handle the spans however they want, with two example handlers provided that can:

* Write an aggregated summary of the spans to log (this is the out-of-the-box behaviour).
* Write all operations to file, in JSON format.

The library is intended for temporary use, to diagnose issues, rather than permanently instrumenting an application.  Full OpenTelemetry consumers such as Honeycomb are the better option for that.

# Usage
There are just two steps needed.

First, include the library in your project using Maven:

```
<dependency>
    <groupId>com.couchbase.client</groupId>
    <artifactId>couchbase-jvm-sdk-inmemory-tracer</artifactId>
    <version>1.0.1</version>
</dependency>
```

or Gradle:

```
implementation 'com.couchbase.client:couchbase-jvm-sdk-inmemory-tracer:1.0.1'
```

or SBT:

```
libraryDependencies += "com.couchbase.client" % "couchbase-jvm-sdk-inmemory-tracer" % "1.0.1"
```

Second, provide an `InMemoryRequestTracer` when creating your Couchbase JVM SDK `Cluster` object:

```
InMemoryRequestTracer tracer = new InMemoryRequestTracer();

Cluster cluster = Cluster.connect("localhost", ClusterOptions.clusterOptions("username", "password")
    .environment(env -> env.requestTracer(tracer)));
// Use the Cluster as normal
```

By default, it will write an aggregated summary of all operations to SLF4J log, every 10 seconds.

To also make it write all operations to files, in JSON format, use:

```
InMemoryRequestTracer tracer = new InMemoryRequestTracer(InMemoryTracerOptions.inMemoryTracerOptions()
    .handlers(List.of(
      // Write aggregated report to SLF4J log.
      ExampleHandlers::writeAggregatedReport,

      // Write all operations, in JSON format, to files.
      ExampleHandlers::writeAllOperations)));
```

Users are encouraged to use the provided handlers as a starting point for their own custom handlers and reports.

An example of a trivial custom handler:

```
InMemoryRequestTracer tracer = new InMemoryRequestTracer(InMemoryTracerOptions.inMemoryTracerOptions()
  .handlers(List.of(
    (operations, sinceLastReport) -> {
      logger.info("There have been {} operations in last {}", operations.operations().size(), sinceLastReport);
    })));
```

`InMemoryTracerOptions` allows customising other parameters, such as the interval in which the handlers are called (which defaults to 10 seconds).  

# Sample Outputs

The `ExampleHandlers::writeAggregatedReport` handler (which is the default) will output something like (a single KV operation was run for this example, which succeeded):

```
[pool-1-thread-1] INFO com.couchbase.client.ExampleHandlers - Aggregated report for 1 operations over last PT9.999228S: {
  "kv" : {
    "upsert" : {
      "successfulOps" : {
        "counts" : {
          "uniqueDocumentIds" : 1,
          "operations" : 1
        },
        "operationDurationsMicros" : {
          "metrics" : [ {
            "name" : "count",
            "value" : 1
          }, {
            "name" : "min",
            "value" : 18622.0
          }, {
            "name" : "median",
            "value" : 18622.0
          }, {
            "name" : "p95",
            "value" : 18622.0
          }, {
            "name" : "max",
            "value" : 18622.0
          } ]
        },
        "serverDurationsMicros" : {
          "metrics" : [ {
            "name" : "count",
            "value" : 1
          }, {
            "name" : "min",
            "value" : 135.0
          }, {
            "name" : "median",
            "value" : 135.0
          }, {
            "name" : "p95",
            "value" : 135.0
          }, {
            "name" : "max",
            "value" : 135.0
          } ]
        },
        "requestEncodingDurationsMicros" : {
          "metrics" : [ {
            "name" : "count",
            "value" : 1
          }, {
            "name" : "min",
            "value" : 2722.0
          }, {
            "name" : "median",
            "value" : 2722.0
          }, {
            "name" : "p95",
            "value" : 2722.0
          }, {
            "name" : "max",
            "value" : 2722.0
          } ]
        }
      }
    }
  }
}
```

While this is the output from the same handler with a single failed query:

```
{
  "query" : {
    "query" : {
      "failedOps" : {
        "counts" : {
          "uniqueDocumentIds" : 0,
          "operations" : 1
        },
        "exceptions" : {
          "ParsingFailureException" : {
            "count" : 1
          }
        },
        "operationDurationsMicros" : {
          "metrics" : [ {
            "name" : "count",
            "value" : 1
          }, {
            "name" : "min",
            "value" : 56049.0
          }, {
            "name" : "median",
            "value" : 56049.0
          }, {
            "name" : "p95",
            "value" : 56049.0
          }, {
            "name" : "max",
            "value" : 56049.0
          } ]
        }
      }
    }
  }
}
```

The `ExampleHandlers::writeAllOperations` handler, which can be enabled to run (see above), outputs a new file every interval period containing something like (a single SQL++ query was run for this example):

```
[ {
  "name" : "query",
  "service" : "query",
  "start" : "2025-01-13T14:34:57.397123Z",
  "durationUs" : 33504,
  "statement" : "SELECT 'hello' AS GREETING",
  "retries" : 0,
  "networkCalls" : [ {
    "durationUs" : 7372,
    "start" : "2025-01-13T14:34:57.402123Z",
    "host" : "192.168.0.99",
    "port" : 8093
  } ]
} ]
```

While this is the output from a single failed query:

```
[ {
  "name" : "query",
  "service" : "query",
  "start" : "2025-01-13T14:55:03.375123Z",
  "durationUs" : 54476,
  "statement" : "BAD SQL++ TO FORCE A FAILURE",
  "exception" : "com.couchbase.client.core.error.ParsingFailureException: Parsing of the input failed {\"completed\":true,\"coreId\":\"0x5685c00c00000001\",\"errors\":[{\"additional\":{\"line\":1,\"column\":5},\"code\":3000,\"message\":\"syntax error - line 1, column 5, near 'BAD ', at: SQL\",\"retry\":false}],\"httpStatus\":400,\"idempotent\":false,\"lastDispatchedFrom\":\"192.168.1.120:58521\",\"lastDispatchedTo\":\"192.168.0.99:8093\",\"requestId\":5,\"requestType\":\"QueryRequest\",\"retried\":0,\"service\":{\"operationId\":\"null\",\"statement\":\"BAD SQL++ TO FORCE A FAILURE\",\"type\":\"query\"},\"timeoutMs\":75000,\"timings\":{\"dispatchMicros\":9378,\"totalDispatchMicros\":9378,\"totalMicros\":52305}}",
  "retries" : 0,
  "networkCalls" : [ {
    "durationUs" : 9371,
    "start" : "2025-01-13T14:55:03.382123Z",
    "host" : "192.168.0.99",
    "port" : 8093
  } ]
} ]
```

And the output from a single successful KV operation:
```
[ {
  "name" : "upsert",
  "service" : "kv",
  "start" : "2025-01-13T14:59:25.275123Z",
  "durationUs" : 27169,
  "documentId" : "id",
  "bucket" : "default",
  "scope" : "_default",
  "collection" : "_default",
  "retries" : 0,
  "requestEncoding" : {
    "durationUs" : 3315,
    "start" : "2025-01-13T14:59:25.275123Z"
  },
  "networkCalls" : [ {
    "durationUs" : 13081,
    "start" : "2025-01-13T14:59:25.286123Z",
    "host" : "192.168.0.99",
    "port" : 11210,
    "durability" : "NONE",
    "serverDurationUs" : 126
  } ]
} ]
```

Users should feel free to add their own handlers outputting metrics or JSON to their desired specification.

# Compatibility
This is provided as a separate library, to make it easier to use against various versions of the Couchbase SDKs.

It should be compatible with the Couchbase Java, Kotlin and Scala SDKs.  Building the library from source requires JDK 8 or later.  At runtime, Java 8 or later is required.

The SDK interfaces this library relies on have been stable from core-io 2.3.4 onwards (core-io is the library shared by all Couchbase JVM SDKs), and so this library is expected to work without issue against Java SDK 3.3.4 onwards, Scala SDK 1.3.4 onwards, and Kotlin SDK 1.0.4 onwards.

# Support
The library is not a fully supported Couchbase product, but is provided as-is, with best-effort support.  

Contributions are welcome, and users are encouraged to use the provided handlers as a starting point for their own custom handlers.
Please feel free to copy them and modify to your own needs.

The intent is that the library be used to temporarily diagnose issues, rather than permanently instrumenting an application.  Everything in this library should be regarded as potentially volatile and subject to change.

# Performance Impact
The library is not expected to dramatically impact performance.

The library retains spans in memory, so there will be an increase in memory usage.

Spans are removed from memory when they are passed to the handlers, which is on a customisable interval defaulting to 10 seconds.

Handling the spans themselves may have some small performance impact, as numbers get crunched and operations get written to file.
The impact of course will depend on the handlers used.

The handlers are called on a separate thread, to reduce impact on the SDK.

# Limitations
Any simple operations such as KV upserts or SQL++ queries should work, but results may vary for more complex compound operations such as ACID transactions.

# For Maintainers
Before running tests, edit `src/test/resources/com/couchbase/client/integration.properties` to point at your cluster.

Deployment (after initial one-off Sonatype setup):
```
mvn clean deploy
```
