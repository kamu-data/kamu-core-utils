/*
 * Copyright (c) 2018 kamu.dev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package dev.kamu.core.utils

import java.net.URI
import java.nio.file.Path

import org.apache.logging.log4j.LogManager

import scala.sys.process.{Process, ProcessBuilder, ProcessLogger}
import scala.util.Try

case class DockerRunArgs(
  image: String,
  args: List[String] = List.empty,
  containerName: Option[String] = None,
  hostname: Option[String] = None,
  network: Option[String] = None,
  exposeAllPorts: Boolean = false,
  exposePorts: List[Int] = List.empty,
  exposePortMap: Map[Int, Int] = Map.empty,
  volumeMap: Map[Path, Path] = Map.empty,
  workDir: Option[String] = None,
  environmentVars: Map[String, String] = Map.empty,
  entryPoint: Option[String] = None,
  remove: Boolean = true,
  tty: Boolean = false,
  interactive: Boolean = false,
  detached: Boolean = false
)

case class ExecArgs(
  tty: Boolean = false,
  interactive: Boolean = false
)

class DockerClient() {
  protected val logger = LogManager.getLogger(getClass.getName)

  def run(
    runArgs: DockerRunArgs
  ): Unit = {
    val cmd = makeRunCmd(runArgs)
    val process = prepare(cmd)
    val exitCode = process.!
    if (exitCode != 0)
      throw new RuntimeException(
        s"Command failed with exit code $exitCode: ${cmd.mkString(" ")}"
      )
  }

  def runShell(
    runArgs: DockerRunArgs,
    shellCommand: Seq[String]
  ): Unit = {
    run(
      runArgs.copy(
        entryPoint = Some("bash"),
        args = List("-c", shellCommand.mkString(" "))
      )
    )
  }

  def exec(
    execArgs: ExecArgs,
    container: String,
    command: Seq[String]
  ): ProcessBuilder = {
    prepare(
      List(
        "docker",
        "exec"
      ) ++ (
        if (execArgs.interactive) List("-i") else List.empty
      ) ++ (
        if (execArgs.tty) List("-t") else List.empty
      ) ++ List(container) ++ command
    )
  }

  def makeRunCmd(runArgs: DockerRunArgs): Seq[String] = {
    List(
      List(
        "docker",
        "run"
      ),
      if (runArgs.remove) List("--rm") else List.empty,
      if (runArgs.tty) List("-t") else List.empty,
      if (runArgs.interactive) List("-i") else List.empty,
      if (runArgs.detached) List("-d") else List.empty,
      runArgs.containerName.map(v => List("--name", v)).getOrElse(List.empty),
      runArgs.hostname.map(v => List("--hostname", v)).getOrElse(List.empty),
      runArgs.network.map(v => List("--network", v)).getOrElse(List.empty),
      if (runArgs.exposeAllPorts) List("-P") else List.empty,
      runArgs.exposePorts
        .map(p => List("-p", p.toString))
        .reduceOption(_ ++ _)
        .getOrElse(List.empty),
      runArgs.exposePortMap
        .map { case (h, c) => List("-p", s"$h:$c") }
        .reduceOption(_ ++ _)
        .getOrElse(List.empty),
      runArgs.volumeMap
        .map { case (h, c) => List("-v", formatVolume(h, c)) }
        .reduceOption(_ ++ _)
        .getOrElse(List.empty),
      runArgs.workDir.map(v => List("--workdir", v)).getOrElse(List.empty),
      runArgs.environmentVars
        .map { case (n, v) => List("-e", s"$n=$v") }
        .reduceOption(_ ++ _)
        .getOrElse(List.empty),
      runArgs.entryPoint
        .map(v => List("--entrypoint", v))
        .getOrElse(List.empty),
      List(runArgs.image),
      runArgs.args
    ).reduce(_ ++ _)
  }

  def prepare(cmd: Seq[String]): ProcessBuilder = {
    logger.debug("Docker run: " + cmd.mkString(" "))
    Process(cmd)
  }

  // QA: be aware of '--sig-proxy' and '--stop-signal' options. 'docker kill' default signal is 'kill'
  def kill(container: String, signal: String = "TERM"): Unit = {
    val processBuilder = prepare(
      Seq("docker", "kill", s"--signal=$signal", container)
    )
    processBuilder
      .run(IOHandlerPresets.logged(logger))
      .exitValue()
  }

  // give a container t seconds to terminate, kill otherwise. 10 seconds is docker's default
  def stop(container: String, time: Int = 10): Unit = {
    val processBuilder = prepare(
      Seq("docker", "stop", s"--time=$time", container)
    )
    processBuilder
      .run(IOHandlerPresets.logged(logger))
      .exitValue()
  }

  def pull(image: String): Unit = {
    prepare(Seq("docker", "pull", image)).!
  }

  def inspectHostPort(container: String, port: Int): Option[Int] = {
    val format = "--format={{ (index (index .NetworkSettings.Ports \"" + port + "/tcp\") 0).HostPort }}"

    val formatEscaped =
      if (!OS.isWindows) format else format.replace("\"", "\\\"")

    val processBuilder = prepare(
      Seq("docker", "inspect", formatEscaped, container)
    )

    try {
      Some(
        processBuilder
          .!!(IOHandlerPresets.logged(logger))
          .stripLineEnd
          .toInt
      )
    } catch {
      case _: RuntimeException => None
    }
  }

  def withNetwork[T](network: String, logger: Option[ProcessLogger] = None)(
    body: => T
  ): T = {
    val processLogger = logger.getOrElse(IOHandlerPresets.blackHoledLogger())

    prepare(Seq("docker", "network", "create", network)).!(processLogger)

    try {
      body
    } finally {
      prepare(Seq("docker", "network", "rm", network)).!(processLogger)
    }
  }

  def getDockerHost: String = {
    sys.env
      .get("DOCKER_HOST")
      .map(s => URI.create(s).getHost)
      .orElse(
        Try(Process(Seq("docker-machine", "ip", "default")).!!).toOption
      )
      .getOrElse("localhost")
  }

  // TODO: Windows sadness territory :'(
  private def formatVolume(src: Path, dst: Path): String = {
    val ssrc =
      if (!OS.isWindows) src.toAbsolutePath.toString else asBoot2DockerPath(src)

    val sdst =
      if (!OS.isWindows) dst.toAbsolutePath.toString
      else dst.toAbsolutePath.toString.substring(2).replace('\\', '/')

    s"$ssrc:$sdst"
  }

  private def asBoot2DockerPath(p: Path): String = {
    val drivePattern = "([a-zA-Z]):(.*)".r
    p.toAbsolutePath.toString.replace('\\', '/') match {
      case drivePattern(drive, rest) => s"/${drive.toLowerCase}${rest}"
      case _                         => throw new Exception(s"Unexpected path without drive letter: $p")
    }
  }

  implicit class ProcessEx(val p: Process) {

    def raiseForExitValue(id: String): Unit = {
      val exitValue = p.exitValue()
      if (exitValue != 0)
        throw new Exception(
          f"Process $id exited abnormally with code: $exitValue"
        )
    }

  }
}
