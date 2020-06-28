/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils

import java.nio.file.Path

package object fs {

  implicit class PathExt(val p: Path) {

    def /(basename: String): Path = p.resolve(basename)
    def /(basename: Path): Path = p.resolve(basename)

    def resolve(child: String, more: String*): Path = {
      if (more.isEmpty)
        p.resolve(child)
      else
        p.resolve(child).resolve(more.head, more.tail: _*)
    }

    def resolve(child: Path, more: Path*): Path = {
      if (more.isEmpty)
        p.resolve(child)
      else
        p.resolve(child).resolve(more.head, more.tail: _*)
    }

  }

}
