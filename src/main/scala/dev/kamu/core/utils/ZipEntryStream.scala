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

import java.io.{FilterInputStream, InputStream}
import java.util.zip.{ZipEntry, ZipInputStream}

object ZipEntryStream {
  def first(inputStream: InputStream): ZipEntryStream = {
    val zipStream = new ZipInputStream(inputStream)
    new ZipEntryStream(zipStream, zipStream.getNextEntry)
  }

  def findFirst(
    inputStream: InputStream,
    regex: String
  ): Option[ZipEntryStream] = {
    val zipStream = new ZipInputStream(inputStream)
    var entry = zipStream.getNextEntry
    while (entry != null) {
      if (entry.getName.matches(regex))
        return Some(new ZipEntryStream(zipStream, entry))
      entry = zipStream.getNextEntry
    }
    None
  }
}

class ZipEntryStream private (
  val zipStream: ZipInputStream,
  val entry: ZipEntry
) extends FilterInputStream(zipStream)
