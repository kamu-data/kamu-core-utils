/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils

import java.util.UUID

import fs._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, date_format, expr}
import org.apache.spark.sql.types.{
  ArrayType,
  DataType,
  NumericType,
  StringType,
  StructType,
  TimestampType
}

import scala.sys.process.Process

trait DataFrameDigest {
  def digest(df: DataFrame): String
}

class DataFrameDigestSHA256 extends DataFrameDigest {
  override def digest(df: DataFrame): String = {
    ensureTimezone(df.sparkSession)

    val tmpDir = new Path(sys.props("java.io.tmpdir"))
      .resolve("kamu-data-" + UUID.randomUUID.toString)

    val fs = tmpDir.getFileSystem(new Configuration())

    normalizeData(df)
      .repartition(1)
      .write
      .csv(tmpDir.toString)

    val output = try {
      Process(Seq("/bin/sh", "-c", s"sort ${tmpDir}/part-*.csv | sha256sum")).!!
    } finally {
      fs.delete(tmpDir, true)
    }

    output.split(' ').head
  }

  protected def normalizeData(df: DataFrame): DataFrame = {
    val cols = df.schema.fields
      .flatMap(f => mapColumnType(f.name, f.dataType))
      .sortBy(_._1)
      .map { case (name, expr) => expr.as(name) }

    df.select(cols: _*)
  }

  protected def ensureTimezone(sparkSession: SparkSession): Unit = {
    val tz = sparkSession.conf.get("spark.sql.session.timeZone")
    if (sparkSession.conf.get("spark.sql.session.timeZone") != "UTC")
      throw new RuntimeException(
        s"Spark timezone is not UTC ($tz) - this will lead to hash differences between timezones"
      )
  }

  // TODO: Horrible hacks to serialize non-CSV-compatible types
  protected def mapColumnType(
    name: String,
    dataType: DataType
  ): Seq[(String, Column)] = {
    dataType match {
      case arr: ArrayType =>
        arr.elementType match {
          case _: NumericType =>
          case _: StringType  =>
          case _ =>
            throw new NotImplementedError(
              s"Arrays with element type ${arr.elementType.simpleString} are not yet supported"
            )
        }
        Seq((name, expr(s"cast(${name} as string)")))
      case struct: StructType =>
        struct.fields.flatMap(f => {
          mapColumnType(s"${name}.${f.name}", f.dataType)
        })
      case _ =>
        Seq((name, mapSimpleType(name, dataType)))
    }
  }

  protected def mapSimpleType(name: String, dataType: DataType): Column = {
    dataType match {
      case _: ArrayType =>
        throw new RuntimeException(
          s"Not a simple type: ${dataType.simpleString}"
        )
      case _: StructType =>
        throw new RuntimeException(
          s"Not a simple type: ${dataType.simpleString}"
        )
      case _: TimestampType =>
        val fmt = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        date_format(col(name), fmt)
      case _ =>
        if (dataType.typeName == "geometry") {
          expr(s"ST_AsText(${name})")
        } else {
          col(name)
        }
    }
  }

}
