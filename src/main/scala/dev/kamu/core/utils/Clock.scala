/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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

class ManualClock(clock: Option[java.time.Clock] = None) extends Clock {
  protected val _clock: java.time.Clock =
    clock.getOrElse(java.time.Clock.systemUTC())

  protected var _current: Instant = Instant.MIN

  def advance(): Unit = {
    val next = _clock.instant()
    if (next.compareTo(_current) <= 0)
      throw new RuntimeException("Non-monotonic clock behavior detected")
    _current = next
  }

  def instant(): Instant = {
    _current
  }

  def timestamp(): Timestamp = {
    Timestamp.from(instant())
  }
}
