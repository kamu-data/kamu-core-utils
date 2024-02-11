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

import fs._
import java.io.{FileOutputStream, OutputStream}
import java.nio.file.{Files, Path}

object Temp {
  def withRandomTempDir[T](prefix: String)(func: Path => T): T = {
    val path = Files.createTempDirectory(prefix)

    try {
      func(path)
    } finally {
      path.deleteRecursive()
    }
  }

  def withTempFile[T](prefix: String, writeFun: OutputStream => Unit)(
    body: Path => T
  ): T = {
    val path = Files.createTempFile(prefix, "")
    val outputStream = new FileOutputStream(path.toFile)
    try {
      writeFun(outputStream)
      outputStream.close()
      body(path)
    } finally {
      outputStream.close()
      path.toFile.delete()
    }
  }
}
