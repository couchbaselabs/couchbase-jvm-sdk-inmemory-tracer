# Couchbase JVM In-Memory Request Tracer

The Couchbase JVM SDKs offer excellent introspection capabilities, as every individual operation can be output through OpenTelemetry tracing.

Power users may want to send these tracing spans to a system such as Jaeger or Honeycomb for analysis, but for those who wish to keep things simple, this library offers a plugin tracer that captures the OpenTelemetry spans in-memory.

Users can handle the spans however they want, with two example handlers provided that can:

* Write an aggregated summary of the spans to log (this is the out-of-the-box behaviour).
* Write all operations to file, in JSON format.

The library is intended for temporary use, to diagnose issues, rather than permanently instrumenting an application.  Full OpenTelemetry consumers such as Honeycomb are the better option for that.

# Usage
Include the library in your project.

Provide an `InMemoryRequestTracer` when creating your Couchbase JVM SDK `Cluster` object:

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

`InMemoryTracerOptions` allows customising other parameters, such as the interval in which the handlers are called (which defaults to 10 seconds).  

# Compatibility
This is provided as a separate library, to make it easier to use against various versions of the Couchbase SDKs.

It should be compatible with the Couchbase Java, Kotlin and Scala SDKs.  The library is written in JDK 8 to maximise compatibility.

The SDK interfaces this library relies on have been stable from core-io 2.3.4 onwards (core-io is the library shared by all Couchbase JVM SDKs), and so this library is expected to work without issue against Java SDK 3.3.4 onwards, Scala SDK 1.3.4 onwards, and Kotlin SDK 1.0.4 onwards.

# Support
The library is not a fully supported Couchbase product, but is provided as-is, with best-effort support.  

Contributions are welcome, and users are encouraged to use the provided handlers as a starting point for their own custom handlers: please feel free to copy them and modify to your own needs.

The intent is that the library be used to temporarily diagnose issues, rather than permanently instrumenting an application.  Everything in this library should be regarded as volatile.

# Performance Impact
The library is not expected to dramatically impact performance.

The library retains spans in memory, so there will be an increase in memory usage.

Spans are removed from memory when they are passed to the handlers, which is on a customisable interval defaulting to 10 seconds.

Handling the spans themselves may have some small performance impact, as numbers get crunched and operations get written to file.
The impact of course will depend on the handlers used.

The handlers are called on a separate thread, to reduce impact on the SDK.

# Limitations
Any simple operations such as KV upserts or SQL++ queries should work, but results may vary for more complex compound operations such as ACID transactions.
