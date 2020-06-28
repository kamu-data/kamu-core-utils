/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils

import java.io.OutputStream
import java.nio.file.{Path, Paths}
import java.util.UUID

import better.files.File

object Temp {
  def systemTempDir: Path =
    Paths.get(System.getProperty("java.io.tmpdir"))

  def getRandomTempName(prefix: String): Path =
    systemTempDir.resolve(prefix + UUID.randomUUID.toString)

  def withRandomTempDir[T](prefix: String)(func: Path => T): T = {
    val tempDir = File(getRandomTempName(prefix))
    tempDir.createDirectories()

    try {
      func(tempDir.path)
    } finally {
      tempDir.delete()
    }
  }

  def withTempFile[T](prefix: String, writeFun: OutputStream => Unit)(
    body: Path => T
  ): T = {
    val file = File(getRandomTempName(prefix))
    val outputStream = file.newOutputStream
    try {
      writeFun(outputStream)
      outputStream.close()
      body(file.path)
    } finally {
      outputStream.close()
      file.delete()
    }
  }
}
