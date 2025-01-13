package com.couchbase.client;

import com.couchbase.client.core.annotation.Stability;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Stability.Volatile
public class Durations {
  // These are sorted
  private final long[] values;

  @Stability.Internal
  public Durations(Stream<Long> values) {
    List<Long> collected = values.collect(Collectors.toList());
    long[] arr = new long[collected.size()];
    for (int i = 0; i < collected.size(); i++) {
      arr[i] = collected.get(i);
    }
    Arrays.sort(arr);
    this.values = arr;
  }

  @Stability.Internal
  public Durations(long[] values) {
    Arrays.sort(values);
    this.values = values;
  }

  public double min() {
    return values[0];
  }

  public double max() {
    return values[values.length - 1];
  }

  public double mean() {
    long sum = 0;
    for (long value : values) {
      sum += value;
    }
    return (double) sum / values.length;
  }

  public double median() {
    if (values.length % 2 == 0) {
      return (values[values.length / 2 - 1] + values[values.length / 2]) / 2.0;
    } else {
      return values[values.length / 2];
    }
  }

  public double percentile(double percentile) {
    int desiredIndex =  (int) Math.ceil(percentile * values.length);
    int index = Math.max(0, Math.min(values.length, desiredIndex));
    return values[index - 1];
  }

  public long count() {
    return values.length;
  }
}
