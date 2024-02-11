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

import java.nio.file.{Files, Path, Paths}
import java.util.zip.ZipInputStream
import fs._

import java.io.{FileInputStream, FileOutputStream}

object ZipFiles {
  def extractZipFile(filePath: Path, outputDir: Path): Unit = {
    val zipStream = new ZipInputStream(new FileInputStream(filePath.toFile))

    extractZipFile(zipStream, outputDir)

    zipStream.close()
  }

  def extractZipFile(
    zipStream: ZipInputStream,
    outputDir: Path,
    filterRegex: Option[String] = None
  ): Unit = {
    Files.createDirectories(outputDir)

    Stream
      .continually(zipStream.getNextEntry)
      .takeWhile(_ != null)
      .filter(
        entry => filterRegex.isEmpty || entry.getName.matches(filterRegex.get)
      )
      .foreach(entry => {
        val outputPath = outputDir / Paths.get(entry.getName)
        val parent = outputPath.getParent
        if (!parent.toFile.exists()) {
          Files.createDirectories(parent)
        }

        val outputStream = new FileOutputStream(outputPath.toFile)

        val buffer = new Array[Byte](1024)

        Stream
          .continually(zipStream.read(buffer))
          .takeWhile(_ != -1)
          .foreach(outputStream.write(buffer, 0, _))

        outputStream.close()
      })
  }
}
