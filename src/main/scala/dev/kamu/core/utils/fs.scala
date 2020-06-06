/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils

import java.io.OutputStream
import java.util.UUID

import org.apache.hadoop.fs.{FileSystem, Path}

package object fs {

  implicit class PathExt(val p: Path) {
    def resolve(child: String, more: String*): Path = {
      val pp = ensureHasPath()
      if (more.isEmpty)
        new Path(pp, child)
      else
        new Path(pp, child).resolve(more.head, more.tail: _*)
    }

    def resolve(child: Path, more: Path*): Path = {
      val pp = ensureHasPath()
      if (more.isEmpty)
        new Path(pp, child)
      else
        new Path(pp, child).resolve(more.head, more.tail: _*)
    }

    private def ensureHasPath(): Path = {
      if (p.toUri.isAbsolute && p.toUri.getPath.isEmpty)
        new Path(p, "/")
      else
        p
    }
  }

  implicit class FileSystemExt(val fs: FileSystem) {
    def toAbsolute(p: Path): Path = {
      if (p.isAbsolute)
        p
      else
        fs.getWorkingDirectory.resolve(p)
    }
  }

  object Temp {
    def systemTempDir: Path =
      new Path(System.getProperty("java.io.tmpdir"))

    def getRandomTempDir(prefix: String): Path =
      systemTempDir.resolve(prefix + UUID.randomUUID.toString)

    def withRandomTempDir[T](
      fileSystem: FileSystem,
      prefix: String
    )(
      func: Path => T
    ): T = {
      val tempDir = getRandomTempDir(prefix)
      fileSystem.mkdirs(tempDir)

      try {
        func(tempDir)
      } finally {
        fileSystem.delete(tempDir, true)
      }
    }

    def withTempFile[T](
      fileSystem: FileSystem,
      prefix: String,
      writeFun: OutputStream => Unit
    )(
      body: Path => T
    ): T = {
      val path = systemTempDir.resolve(prefix + UUID.randomUUID().toString)
      val outputStream = fileSystem.create(path, false)
      try {
        writeFun(outputStream)
        outputStream.close()
        body(path)
      } finally {
        outputStream.close()
        fileSystem.delete(path, false)
      }
    }
  }

}
