package io.alice.platform.api.engine.chat.command.types

import io.alice.platform.api.engine.chat.command.argument.ArgumentProcessor
import io.alice.platform.api.engine.chat.command.option.ValuedOption

private inline fun <T : Comparable<T>> checkRange(
  it: T,
  min: T?,
  max: T?,
  clamp: Boolean,
  fail: (String) -> Unit
): T {
  require(min == null || max == null || min < max) { "min must be less than max" }
  if (clamp) {
    if (min != null && it < min) return min
    if (max != null && it > max) return max
  } else if (min != null && it < min || max != null && it > max) {
    fail(
      when {
        min == null -> "$it is larger than the maximum valid value of $max."
        max == null -> "$it is smaller than the minimum valid value of $min."
        else -> "$it is not in the valid range of $min to $max."
      }
    )
  }
  return it
}

fun <T : Comparable<T>> ArgumentProcessor<T, T>.restrictTo(
  min: T? = null,
  max: T? = null,
  clamp: Boolean = false
): ArgumentProcessor<T, T> =
  clone({ checkRange(transformValue(it), min, max, clamp) { m -> fail(m) } }, transformAll, validator)

fun <T : Comparable<T>> ArgumentProcessor<T, T>.restrictTo(
  range: ClosedRange<T>,
  clamp: Boolean = false
): ArgumentProcessor<T, T> =
  restrictTo(range.start, range.endInclusive, clamp)

fun <T : Comparable<T>> ValuedOption<T?, T, T>.restrictTo(
  min: T? = null,
  max: T? = null,
  clamp: Boolean = false
): ValuedOption<T?, T, T> =
  clone({ checkRange(transformValue(it), min, max, clamp) { m -> fail(m) } }, transformEach, transformAll, validator)

fun <T : Comparable<T>> ValuedOption<T?, T, T>.restrictTo(
  range: ClosedRange<T>,
  clamp: Boolean = false
): ValuedOption<T?, T, T> =
  restrictTo(range.start, range.endInclusive, clamp)
