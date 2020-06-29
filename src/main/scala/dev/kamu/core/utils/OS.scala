/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
