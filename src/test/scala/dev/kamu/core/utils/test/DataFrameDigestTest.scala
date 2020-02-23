/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils.test

import dev.kamu.core.utils.DataFrameDigestSHA1
import org.scalatest.FunSuite

class DataFrameDigestTest extends FunSuite with KamuDataFrameSuite {
  import spark.implicits._

  test("Computes stable digest - simple types") {
    val df = sc
      .parallelize(
        Seq(
          (
            ts(0),
            1,
            "a"
          ),
          (
            ts(1000),
            2,
            "b"
          ),
          (
            ts(2000),
            3,
            "c"
          )
        )
      )
      .toDF("system_time", "id", "name")

    val actual = new DataFrameDigestSHA1().digest(df)

    assert(actual == "f5bb5b05ee0b172c784ec4ac71c4559744a1cd3b")
  }

  test("Computes stable digest - geometry") {
    val df = sc
      .parallelize(
        Seq(
          (
            ts(0),
            1,
            "POLYGON ((0 0, 20 0, 20 20, 0 20, 0 0))"
          ),
          (
            ts(1000),
            2,
            "POLYGON ((0 0, 20 0, 20 20, 0 20, 0 0))"
          ),
          (
            ts(2000),
            3,
            "POLYGON ((0 0, 20 0, 20 20, 0 20, 0 0))"
          )
        )
      )
      .toDF("system_time", "id", "geom")
      .selectExpr(
        "system_time",
        "id",
        "ST_GeomFromWKT(geom) as geom"
      )

    val actual = new DataFrameDigestSHA1().digest(df)

    assert(actual == "fe40ea58c253619c54efbc66a62a6d1d594953e4")
  }

  test("Computes stable digest - array of simple types") {
    val df = sc
      .parallelize(
        Seq(
          (
            ts(0),
            1,
            Array("a", "b", "c"),
            Array(3, 2, 1)
          ),
          (
            ts(1000),
            2,
            Array("x", "y"),
            Array(3, 2)
          )
        )
      )
      .toDF("system_time", "id", "tags", "nums")

    val actual = new DataFrameDigestSHA1().digest(df)

    assert(actual == "8a892bad282f3a61bfd608e07e4da37e70f03104")
  }

  test("Computes stable digest - array of timestamps") {
    val df = sc
      .parallelize(
        Seq(
          (
            ts(0),
            1,
            Array(ts(0), ts(1000))
          ),
          (
            ts(1000),
            2,
            Array(ts(2000))
          )
        )
      )
      .toDF("system_time", "id", "times")

    assertThrows[NotImplementedError] {
      new DataFrameDigestSHA1().digest(df)
    }
  }

  test("Computes stable digest - struct of simple types") {
    val df = sc
      .parallelize(
        Seq(
          (
            ts(0),
            1,
            "a",
            1,
            1.5,
            Array("x", "y")
          ),
          (
            ts(1000),
            2,
            "b",
            2,
            3.14,
            Array("z")
          )
        )
      )
      .toDF("system_time", "id", "name", "num", "share", "tags")
      .selectExpr(
        "system_time",
        "struct(id, name) as identity",
        "struct(num, share, tags) as info"
      )

    val actual = new DataFrameDigestSHA1().digest(df)

    assert(actual == "ce29a331f66914495de4b2f34a54f696191c8578")
  }

}
