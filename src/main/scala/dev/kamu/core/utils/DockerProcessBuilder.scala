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

import org.slf4j.event.Level

import java.io.{IOException, InputStream, OutputStream}
import java.net.{
  ConnectException,
  InetSocketAddress,
  Socket,
  SocketTimeoutException
}
import java.nio.charset.StandardCharsets
import java.time.Instant
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration
import scala.sys.process.{Process, ProcessBuilder, ProcessIO, ProcessLogger}

class DockerProcessBuilder(
  protected val id: String,
  protected val dockerClient: DockerClient,
  protected val runArgs: DockerRunArgs
) {
  private val logger = LoggerFactory.getLogger(classOf[DockerProcessBuilder])

  def cmd: Seq[String] = {
    dockerClient.makeRunCmd(runArgs)
  }

  def run(processIO: Option[ProcessIO] = None): DockerProcess = {
    val processBuilder = dockerClient.prepare(cmd)
    new DockerProcess(
      id,
      dockerClient,
      runArgs.containerName.get, // TODO: containerName is optional... None.get exception possible
      processBuilder,
      runArgs,
      processIO
    )
  }
}

class DockerProcess(
  val id: String,
  dockerClient: DockerClient,
  val containerName: String,
  processBuilder: ProcessBuilder,
  runArgs: DockerRunArgs,
  ioHandler: Option[ProcessIO] = None
) {
  private val logger = LoggerFactory.getLogger(classOf[DockerProcess])

  val process: Process = processBuilder.run(getIOHandler())

  protected def getIOHandler(): ProcessIO = {
    if (ioHandler.isDefined)
      ioHandler.get
    else if (runArgs.interactive)
      IOHandlerPresets.interactive()
    else
      IOHandlerPresets.redirectOutputTagged(s"[$id] ")
  }

  def join(): Int = {
    process.exitValue()
  }

  def kill(signal: String = "TERM"): Unit = {
    dockerClient.kill(containerName, signal)
  }

  def stop(time: Int = 10): Unit = {
    dockerClient.stop(containerName, time)
  }

  def getHostPort(containerPort: Int): Option[Int] = {
    dockerClient.inspectHostPort(containerName, containerPort)
  }

  def waitForHostPort(containerPort: Int, timeout: Duration): Int = {
    val deadline = Instant.now().plusNanos(timeout.toNanos)

    def waitSome(): Unit = {
      if (Instant.now().compareTo(deadline) >= 0)
        throw new TimeoutException(
          s"Timeout while waiting for container port $containerPort of $id"
        )
      else
        Thread.sleep(500)
    }

    var hostPort = getHostPort(containerPort)

    while (hostPort.isEmpty) {
      waitSome()
      hostPort = getHostPort(containerPort)
    }

    val dockerHost = dockerClient.getDockerHost

    def tryConnect(): Boolean = {
      val timeout = (deadline.toEpochMilli - Instant.now().toEpochMilli).toInt
      if (timeout < 0)
        return false

      try {
        val s = new Socket()
        s.connect(
          new InetSocketAddress(dockerHost, hostPort.get),
          timeout
        )

        // TODO: Due to how docker works it will accept socket connections to the mapped port even when
        // the corresponding port in the container didn't open yet. So here we have to wait for a short time and
        // see if docker's "proxy" will reset the connection when it realizes the container isn't ready yet.
        try {
          s.setSoTimeout(500)
          val read = s.getInputStream.read()
          read >= 0
        } catch {
          case _: SocketTimeoutException =>
            // This means that the remote side is listening for us
            true
        } finally {
          s.close()
        }
      } catch {
        case _: ConnectException       => false
        case _: SocketTimeoutException => false
        case _: IOException            => false
      }
    }

    logger.debug(
      "Waiting for port of {}:{} forwarded to {}:{}",
      containerName,
      Int.box(containerPort),
      dockerHost,
      Int.box(hostPort.get)
    )

    while (!tryConnect()) {
      waitSome()
    }

    hostPort.get
  }
}

object IOHandlerPresets {
  def interactive(): ProcessIO = {
    new ProcessIO(
      in =>
        while (true) {
          val line = scala.io.StdIn.readLine()

          if (line == null)
            in.close()
          else {
            in.write((line + "\n").getBytes(StandardCharsets.UTF_8))
            in.flush()
          }
        },
      out => stream(out, System.out),
      err => stream(err, System.err)
    )
  }

  def redirectOutputTagged(tag: String): ProcessIO = {
    new ProcessIO(
      _ => (),
      out =>
        scala.io.Source
          .fromInputStream(out)
          .getLines
          .foreach(l => System.out.println(tag + l)),
      stderr =>
        scala.io.Source
          .fromInputStream(stderr)
          .getLines()
          .foreach(l => System.err.println(tag + l))
    )
  }

  def blackHoled(): ProcessIO = {
    new ProcessIO(
      _ => (),
      _ => (),
      _ => ()
    )
  }

  def blackHoledLogger(): ProcessLogger = {
    ProcessLogger(_ => (), _ => ())
  }

  def logged(
    logger: org.slf4j.Logger,
    outLevel: Level = Level.DEBUG,
    errLevel: Level = Level.DEBUG
  ): ProcessLogger = {
    ProcessLogger(
      out => log(logger, outLevel, out),
      err => log(logger, errLevel, err)
    )
  }

  def redirectToLogger(
    logger: Logger,
    outLevel: Level = Level.DEBUG,
    errLevel: Level = Level.DEBUG,
    tag: String = ""
  ): ProcessIO = {
    new ProcessIO(
      _ => (),
      out =>
        scala.io.Source
          .fromInputStream(out)
          .getLines
          .foreach(l => log(logger, outLevel, tag + l)),
      stderr =>
        scala.io.Source
          .fromInputStream(stderr)
          .getLines()
          .foreach(l => log(logger, errLevel, tag + l))
    )
  }

  private def stream(from: InputStream, to: OutputStream): Unit = {
    val buf = new Array[Byte](1024)
    while (true) {
      val read = from.read(buf)
      if (read < 0)
        return

      to.write(buf, 0, read)
    }
  }

  // Thanks, SLF4J
  private def log(logger: Logger, level: Level, msg: String): Unit = {
    level match {
      case Level.DEBUG => logger.debug(msg)
      case Level.INFO  => logger.info(msg)
      case Level.WARN  => logger.warn(msg)
      case Level.ERROR => logger.error(msg)
      case Level.TRACE => logger.trace(msg)
    }
  }
}
