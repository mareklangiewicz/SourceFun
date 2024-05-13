package pl.mareklangiewicz.sourcefun

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import pl.mareklangiewicz.ulog.ULog
import kotlinx.coroutines.*
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import okio.Path.Companion.toOkioPath
import pl.mareklangiewicz.annotations.*
import pl.mareklangiewicz.gradle.ulog.asULog
import pl.mareklangiewicz.kground.io.*
import pl.mareklangiewicz.kground.plusIfNN
import pl.mareklangiewicz.kommand.CLI
import pl.mareklangiewicz.kommand.getSysCLI
import pl.mareklangiewicz.uctx.uctx
import pl.mareklangiewicz.usubmit.USubmit

fun <T: Task> T.runWithUCtxForTask(action: suspend T.() -> Unit) = runBlocking { uctxForTask { action() } }
fun <T: Task> T.doLastWithUCtxForTask(action: suspend T.() -> Unit) = doLast { runWithUCtxForTask(action) }
fun <T: Task> T.doFirstWithUCtxForTask(action: suspend T.() -> Unit) = doFirst { runWithUCtxForTask(action) }

/** Setting some param explicitly to null means we don't add any (even default) to context. */
@OptIn(NotPortableApi::class)
suspend inline fun <T: Task, R> T.uctxForTask(
  context: CoroutineContext = EmptyCoroutineContext,
  coroutineName: String? = name,
  dispatcher: CoroutineDispatcher? = getSysDispatcherForIO(),
  fs: UFileSys? = getSysUFileSys(),
  cwd: UCWD? = fs?.getSysWorkingDir()?.let(::UCWD),
  cli: CLI? = getSysCLI(),
  log: ULog? = logger.asULog(),
  submit: USubmit? = null,
  // TODO_someday_maybe: some awesome configurable default (TUI?) Supervisor/USubmit for gradle?
  // BTW ZenitySupervisor as default USubmit in gradle is bad idea. It has to work on different systems/CIs.
  noinline block: suspend CoroutineScope.() -> R,
): R = uctx(
  context = context plusIfNN dispatcher plusIfNN fs plusIfNN cwd plusIfNN cli plusIfNN log plusIfNN submit,
  name = coroutineName,
  block = block,
)


@UntrackedTask(because = "Downloaded file is external state and can't be tracked.")
abstract class DownloadFile : DefaultTask() { // TODO_later: nice task for downloading multiple files.

  @get:Input
  abstract val inputUrl: Property<String>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @OptIn(DelicateApi::class)
  @TaskAction
  fun execute() = runWithUCtxForTask {
    download(inputUrl.get(), outputFile.get().asFile.toOkioPath())
  }
}

