/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils

import java.nio.file.Path
import java.util.zip.ZipInputStream

import better.files.File

import fs._

object ZipFiles {
  def extractZipFile(filePath: Path, outputDir: Path): Unit = {
    val inputStream = File(filePath).newInputStream
    val zipStream = new ZipInputStream(inputStream)

    extractZipFile(zipStream, outputDir)

    zipStream.close()
  }

  def extractZipFile(
    zipStream: ZipInputStream,
    outputDir: Path,
    filterRegex: Option[String] = None
  ): Unit = {
    File(outputDir).createDirectories()

    Stream
      .continually(zipStream.getNextEntry)
      .takeWhile(_ != null)
      .filter(
        entry => filterRegex.isEmpty || entry.getName.matches(filterRegex.get)
      )
      .foreach(entry => {
        val outputStream = File(outputDir / entry.getName).newOutputStream

        val buffer = new Array[Byte](1024)

        Stream
          .continually(zipStream.read(buffer))
          .takeWhile(_ != -1)
          .foreach(outputStream.write(buffer, 0, _))

        outputStream.close()
      })
  }
}
