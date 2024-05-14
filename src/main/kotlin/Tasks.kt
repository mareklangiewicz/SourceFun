package pl.mareklangiewicz.sourcefun

import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import pl.mareklangiewicz.ulog.ULog
import kotlinx.coroutines.*
import okio.Path
import okio.FileSystem.Companion.SYSTEM
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import okio.Path.Companion.toOkioPath
import pl.mareklangiewicz.annotations.*
import pl.mareklangiewicz.gradle.ulog.asULog
import pl.mareklangiewicz.io.readUtf8
import pl.mareklangiewicz.io.writeUtf8
import pl.mareklangiewicz.kground.io.*
import pl.mareklangiewicz.kground.plusIfNN
import pl.mareklangiewicz.kommand.CLI
import pl.mareklangiewicz.kommand.ax
import pl.mareklangiewicz.kommand.core.curlDownload
import pl.mareklangiewicz.kommand.getSysCLI
import pl.mareklangiewicz.kommand.git.gitHash
import pl.mareklangiewicz.uctx.uctx
import pl.mareklangiewicz.ure.core.Ure
import pl.mareklangiewicz.ure.replaceAll
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
  log: ULog? = logger.asULog(alsoPrintLn = true),
  submit: USubmit? = null,
  // TODO_someday_maybe: some awesome configurable default (TUI?) Supervisor/USubmit for gradle?
  // BTW ZenitySupervisor as default USubmit in gradle is bad idea. It has to work on different systems/CIs.
  noinline block: suspend CoroutineScope.() -> R,
): R = uctx(
  context = context plusIfNN dispatcher plusIfNN fs plusIfNN cwd plusIfNN cli plusIfNN log plusIfNN submit,
  name = coroutineName,
  block = block,
)

abstract class SourceFunTask : SourceTask() {

  @get:OutputDirectory
  protected abstract val outputDirProperty: DirectoryProperty

  @get:Internal
  protected abstract val taskActionProperty: Property<(source: FileTree, output: Directory) -> Unit>

  fun setOutput(path: Path) = outputDirProperty.set(path.toFile())

  fun setTaskAction(action: (srcTree: FileTree, outDir: Directory) -> Unit) {
    taskActionProperty.set(action)
    taskActionProperty.finalizeValue()
  }

  @TaskAction
  fun taskAction() = taskActionProperty.get()(source, outputDirProperty.get())
}

fun SourceTask.addSource(path: Path) {
  source(path.toFile())
}

var SourceFunTask.src: Path
  get() = error("src is write only")
  set(value) = setSource(value.toFile())

var SourceFunTask.out: Path
  get() = error("out is write only")
  set(value) = setOutput(value)


fun SourceFunTask.setVisitFun(action: FileVisitDetails.(outDir: Directory) -> Unit) {
  setTaskAction { srcTree, outDir -> srcTree.visit { it.action(outDir) } }
}

/** Note: this version uses the same file relative path/name for both input and output */
fun SourceFunTask.setVisitPathFun(action: (inPath: Path, outPath: Path) -> Unit) {
  setVisitFun { outDir ->
    if (isDirectory) return@setVisitFun
    val inPath = file.toOkioPath()
    val outPath = outDir.file(path).asFile.toOkioPath()
    logger.info("src: $inPath") // printing absolute path is good because it's then clickable in the IDE
    logger.info("out: $outPath")
    action(inPath, outPath)
  }
}

fun SourceFunTask.setTransformPathFun(transform: (Path) -> String?) = setVisitPathFun { inPath, outPath ->
  transform(inPath)?.let { SYSTEM.writeUtf8(outPath, it, createParentDir = true) }
}

fun SourceFunTask.setTransformFun(transform: Path.(String) -> String?) {
  setTransformPathFun { it.transform(SYSTEM.readUtf8(it)) }
}



abstract class SourceRegexTask : SourceFunTask() {
  @get:Input
  abstract val match: Property<String>

  @get:Input
  abstract val replace: Property<String>

  init {
    setTransformFun { it.replace(Regex(match.get()), replace.get()) }
  }
}


abstract class SourceUreTask : SourceFunTask() {
  @get:Input
  abstract val match: Property<Ure>

  @get:Input
  abstract val replace: Property<String>

  init {
    setTransformFun { it.replaceAll(match.get(), replace.get()) }
  }
}

@UntrackedTask(because = "Git version and build time is external state and can't be tracked.")
abstract class VersionDetailsTask : DefaultTask() {

  @get:OutputDirectory
  abstract val generatedAssetsDir: DirectoryProperty

  @OptIn(DelicateApi::class)
  @TaskAction
  fun execute() = runBlocking {
    val commit = gitHash().ax().single()
    val time = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
    generatedAssetsDir.dir("version-details").get().run {
      project.mkdir(this)
      file("commit").asFile.writeText(commit)
      file("buildtime").asFile.writeText(time)
    }
  }
}

@UntrackedTask(because = "Downloaded file is external state and can't be tracked.")
abstract class DownloadFileTask : DefaultTask() { // TODO_later: nice task for downloading multiple files.

  @get:Input
  abstract val inputUrl: Property<String>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @OptIn(DelicateApi::class)
  @TaskAction
  fun execute() = runWithUCtxForTask {
    curlDownload(inputUrl.get(), outputFile.get().asFile.toOkioPath())
  }
}


