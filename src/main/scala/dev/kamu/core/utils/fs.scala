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

import java.nio.file.{Files, Path}
import java.util.Comparator

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

    def deleteRecursive() = {
      Files
        .walk(p)
        .sorted(Comparator.reverseOrder())
        .forEach(_.toFile.delete)
    }

  }

}
