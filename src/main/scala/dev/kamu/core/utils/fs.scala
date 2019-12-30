/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils

import org.apache.hadoop.fs.{FileSystem, Path}

package object fs {

  implicit class PathExt(val p: Path) {
    def resolve(children: String*): Path = {
      if (children.isEmpty)
        p
      else
        new Path(p, children.head).resolve(children.tail: _*)
    }

    def resolve(children: Path*): Path = {
      if (children.isEmpty)
        p
      else
        new Path(p, children.head).resolve(children.tail: _*)
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

}
