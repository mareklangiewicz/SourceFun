package pl.mareklangiewicz.gradle.ulog

import org.gradle.api.Project
import org.gradle.api.Script
import org.gradle.api.Task
import org.slf4j.LoggerFactory
import pl.mareklangiewicz.udata.str
import pl.mareklangiewicz.ulog.ULog
import pl.mareklangiewicz.ulog.ULogLevel
import pl.mareklangiewicz.ulog.ULogLevel.*

private fun defaultLogLine(level: ULogLevel, data: Any?) = "L ${level.symbol} ${data.str(maxLength = 256)}"

class SLF4JULog(
  val logger: org.slf4j.Logger,
  var minLevel: ULogLevel = VERBOSE,
  val toLogLine: (ULogLevel, Any?) -> String = ::defaultLogLine,
): ULog {
  override fun invoke(level: ULogLevel, data: Any?) {
    if (level < minLevel) return
    val line = toLogLine(level, data)
    when (level) {
      NONE -> Unit
      QUIET, VERBOSE -> logger.trace(line)
      DEBUG -> logger.debug(line)
      INFO -> logger.info(line)
      WARN -> logger.warn(line)
      ERROR, ASSERT -> logger.error(line)
    }
  }
}

fun org.slf4j.Logger.asULog(
  minLevel: ULogLevel = VERBOSE,
  toLogLine: (ULogLevel, Any?) -> String = ::defaultLogLine,
) = SLF4JULog(this, minLevel, toLogLine)

fun Project.getULogForProject(
  minLevel: ULogLevel = VERBOSE,
  toLogLine: (ULogLevel, Any?) -> String = ::defaultLogLine,
) = logger.asULog(minLevel, toLogLine)

fun Task.getULogForTask(
  minLevel: ULogLevel = VERBOSE,
  toLogLine: (ULogLevel, Any?) -> String = ::defaultLogLine,
) = logger.asULog(minLevel, toLogLine)

fun Script.getULogForScript(
  minLevel: ULogLevel = VERBOSE,
  toLogLine: (ULogLevel, Any?) -> String = ::defaultLogLine,
) = logger.asULog(minLevel, toLogLine)

fun getSLF4JULog(
  loggerName: String,
  minLevel: ULogLevel = VERBOSE,
  toLogLine: (ULogLevel, Any?) -> String = ::defaultLogLine,
) = LoggerFactory.getLogger(loggerName).asULog(minLevel, toLogLine)
