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

  /**
   * Returns the minimum duration (or 0, if no durations are present).
   */
  public double min() {
    if (values.length == 0) {
      return 0;
    }

    return values[0];
  }

  /**
   * Returns the maximum duration (or 0, if no durations are present).
   */
  public double max() {
    if (values.length == 0) {
      return 0;
    }

    return values[values.length - 1];
  }

  /**
   * Returns the mean duration (or 0, if no durations are present).
   */
  public double mean() {
    if (values.length == 0) {
      return 0;
    }

    long sum = 0;
    for (long value : values) {
      sum += value;
    }
    return (double) sum / values.length;
  }

  /**
   * Returns the median duration (or 0, if no durations are present).
   */
  public double median() {
    if (values.length == 0) {
      return 0;
    }

    if (values.length % 2 == 0) {
      return (values[values.length / 2 - 1] + values[values.length / 2]) / 2.0;
    } else {
      return values[values.length / 2];
    }
  }

  /**
   * Returns the duration at the given percentile (or 0, if no durations are present).
   * <p>
   * That is, if percentile is 0.9, then 90% of the durations are less than the returned value.
   *
   * @param percentile needs to be between 0 and 1.
   */
  public double percentile(double percentile) {
    if (values.length == 0) {
      return 0;
    }

    int desiredIndex =  (int) Math.ceil(percentile * values.length);
    int index = Math.max(0, Math.min(values.length, desiredIndex));
    return values[index - 1];
  }

  /**
   * Returns the number of durations.
   */
  public long count() {
    return values.length;
  }
}
