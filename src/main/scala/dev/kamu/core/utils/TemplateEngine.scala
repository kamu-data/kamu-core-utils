/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils

class TemplateEngine {
  def render(template: String, context: Map[String, String]): String = {
    var rendered = template

    for ((name, value) <- context) {
      rendered = rendered.replaceAll("\\{\\{\\s*" + name + "\\s*\\}\\}", value)
    }

    rendered.replaceAll("\\{\\{[^}]*\\}\\}", "")
  }
}
