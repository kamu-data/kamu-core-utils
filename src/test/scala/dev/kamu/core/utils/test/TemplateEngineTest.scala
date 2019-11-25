/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils.test

import dev.kamu.core.utils.TemplateEngine
import org.scalatest.FunSuite

class TemplateEngineTest extends FunSuite {
  test("value missing") {
    val engine = new TemplateEngine()
    val actual = engine.render(
      """SELECT * FROM {{ table }}""",
      Map.empty
    )
    assert(actual == "SELECT * FROM ")
  }

  test("value present") {
    val engine = new TemplateEngine()
    val actual = engine.render(
      """SELECT * FROM {{ table }}""",
      Map("table" -> "foo")
    )
    assert(actual == "SELECT * FROM foo")
  }
}
