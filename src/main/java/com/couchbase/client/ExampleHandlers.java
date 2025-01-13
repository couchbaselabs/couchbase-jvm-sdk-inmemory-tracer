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
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.SerializationFeature;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.node.ArrayNode;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.ZonedDateTime;

import static com.couchbase.client.util.OperationsToJson.FORMATTER;

/**
 * Example handlers for the in-memory request tracer.
 * <p>
 * Users are encouraged to implement their own handlers based on the provided interfaces.  Feel free to copy the handlers here as starting
 * point.
 */
@Stability.Volatile
public class ExampleHandlers {
  private static final Logger logger = LoggerFactory.getLogger(ExampleHandlers.class);

  private ExampleHandlers() {
  }

  /**
   * Writes an aggregated JSON-based report of all operations to an SLF4J logger.
   */
  public static void writeAggregatedReport(InMemoryRequestTracerHandlerOperations operations, Duration sinceLastReport) {
    ObjectNode aggregatedReport = ExampleReports.exampleAggregatedReport(operations);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    try {
      String repAsStr = mapper.writeValueAsString(aggregatedReport);
      logger.info("Aggregated report for {} operations over last {}: {}", operations.operations().size(), sinceLastReport, repAsStr);
    } catch (Exception e) {
      logger.error("Failed to pretty print JSON", e);
    }
  }

  /**
   * Writes all operations in JSON form into a file in the current working directory.
   * <p>
   * These files can be large when there are many operations.
   */
  public static void writeAllOperations(InMemoryRequestTracerHandlerOperations operations, Duration sinceLastReport) {
    ArrayNode ops = ExampleReports.exampleOperationsOutput(operations);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    try {
      String opsAsStr = mapper.writeValueAsString(ops);
      ZonedDateTime now = ZonedDateTime.now();
      String formattedDate = now.format(FORMATTER);
      Path filename = Paths.get(formattedDate + "_ops_report.json");
      String currentWorkingDirectory = System.getProperty("user.dir");
      logger.info("Writing aggregated report for {} ops over last {} to file {}/{}", operations.operations().size(), sinceLastReport, filename, currentWorkingDirectory);
      Files.write(filename, opsAsStr.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Exception e) {
      logger.error("Failed to pretty print JSON", e);
    }
  }
}
