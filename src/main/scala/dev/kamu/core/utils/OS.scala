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

object OS {
  def isWindows: Boolean = {
    System.getProperty("os.name").startsWith("Windows")
  }

  def uid: Long = {
    new com.sun.security.auth.module.UnixSystem().getUid
  }

  def gid: Long = {
    new com.sun.security.auth.module.UnixSystem().getGid
  }
}
