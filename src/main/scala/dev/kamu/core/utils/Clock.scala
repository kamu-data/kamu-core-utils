/*
 * Copyright 2018 kamu.dev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.kamu.core.utils

import java.sql.Timestamp
import java.time.Instant

trait Clock {
  def instant(): Instant
  def timestamp(): Timestamp
}

class AutoClock(clock: Option[java.time.Clock] = None) extends Clock {
  protected val _clock: java.time.Clock =
    clock.getOrElse(java.time.Clock.systemUTC())

  def instant(): Instant = {
    _clock.instant()
  }

  def timestamp(): Timestamp = {
    Timestamp.from(instant())
  }
}

class ManualClock(
  now: Option[Instant] = None,
  clock: Option[java.time.Clock] = None
) extends Clock {
  protected val _clock: java.time.Clock =
    clock.getOrElse(java.time.Clock.systemUTC())

  protected var _current: Instant = Instant.MIN

  def set(next: Instant): Unit = {
    if (next.compareTo(_current) <= 0)
      throw new RuntimeException("Non-monotonic clock behavior detected")
    _current = next
  }

  def set(next: Timestamp): Unit = {
    set(next.toInstant)
  }

  def advance(): Unit = {
    set(_clock.instant())
  }

  def instant(): Instant = {
    _current
  }

  def timestamp(): Timestamp = {
    Timestamp.from(instant())
  }
}
