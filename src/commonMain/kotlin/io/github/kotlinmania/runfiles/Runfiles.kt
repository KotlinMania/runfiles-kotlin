// port-lint: source runfiles.rs
package io.github.kotlinmania.runfiles

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

/**
 * Runfiles lookup library for Bazel-built Kotlin binaries and tests.
 *
 * Depend on this runfiles library from a build rule, create a [Runfiles]
 * object, and use [Runfiles.rlocation] or [Runfiles.rlocationFrom] to look up
 * runtime paths for declared data dependencies.
 */

internal const val RUNFILES_DIR_ENV_VAR: String = "RUNFILES_DIR"
internal const val MANIFEST_FILE_ENV_VAR: String = "RUNFILES_MANIFEST_FILE"
internal const val TEST_SRCDIR_ENV_VAR: String = "TEST_SRCDIR"

/** Return the build-time repository name. */
fun repositoryName(repo: String?): String =
    repo ?: throw IllegalStateException(
        "REPOSITORY_NAME was not set at compile time; set it or call rlocationFrom directly",
    )

/**
 * The error type for [Runfiles] construction.
 */
sealed class RunfilesError(
    private val rendered: String,
    private val equalityKey: String = rendered,
    cause: Throwable? = null,
) : Exception(rendered, cause) {
    /** Directory based runfiles could not be found. */
    data object RunfilesDirNotFound : RunfilesError("RunfilesDirNotFound")

    /** An I/O error which occurred during the creation of directory-based runfiles. */
    class RunfilesDirIoError(val error: Throwable) :
        RunfilesError("RunfilesDirIoError: ${error.message ?: error.toString()}", error.toString(), error)

    /** An I/O error which occurred during the creation of manifest-file-based runfiles. */
    class RunfilesManifestIoError(val error: Throwable) :
        RunfilesError("RunfilesManifestIoError: ${error.message ?: error.toString()}", error.toString(), error)

    /** A manifest file could not be parsed. */
    data object RunfilesManifestInvalidFormat : RunfilesError("RepoMappingInvalidFormat")

    /** The bzlmod repo-mapping file could not be found. */
    data object RepoMappingNotFound : RunfilesError("RepoMappingInvalidFormat")

    /** The bzlmod repo-mapping file could not be parsed. */
    data object RepoMappingInvalidFormat : RunfilesError("RepoMappingInvalidFormat")

    /** An I/O error which occurred during the parsing of a repo-mapping file. */
    class RepoMappingIoError(val error: Throwable) :
        RunfilesError("RepoMappingIoError: ${error.message ?: error.toString()}", error.toString(), error)

    /** An error indicating a specific runfile was not found. */
    class RunfileNotFound(val path: String) :
        RunfilesError("RunfileNotFound: $path", path)

    /** An I/O error which occurred when operating with a particular runfile. */
    class RunfileIoError(val error: Throwable) :
        RunfilesError("RunfileIoError: ${error.message ?: error.toString()}", error.toString(), error)

    fun fmt(): String = rendered

    fun eq(other: RunfilesError): Boolean = equals(other)

    override fun toString(): String = rendered

    override fun equals(other: Any?): Boolean =
        other is RunfilesError && this::class == other::class && equalityKey == other.equalityKey

    override fun hashCode(): Int = 31 * this::class.hashCode() + equalityKey.hashCode()
}

/** A specialized result type for runfiles operations. */
typealias Result<T> = kotlin.Result<T>

internal sealed class Mode {
    /**
     * Runfiles located in a directory indicated by the `RUNFILES_DIR`
     * environment variable or a neighboring `.runfiles` directory to the
     * executable.
     */
    data class DirectoryBased(val path: String) : Mode()

    /**
     * Runfiles represented as a mapping of rlocation path to real path
     * indicated by the `RUNFILES_MANIFEST_FILE` environment variable.
     */
    data class ManifestBased(val pathMapping: Map<String, String>) : Mode()
}

/** A pair of source repository and target apparent repository name. */
internal typealias RepoMappingKey = Pair<String, String>

/** The mapping of keys to target canonical directory. */
internal data class RepoMapping(
    val exact: Map<RepoMappingKey, String> = emptyMap(),
    val prefixes: Map<RepoMappingKey, String> = emptyMap(),
) {
    companion object {
        fun new(): RepoMapping = RepoMapping()

        fun default(): RepoMapping = new()
    }

    fun get(key: RepoMappingKey): String? {
        exact[key]?.let { return it }

        val sourceRepo = key.first
        val apparentName = key.second
        for ((storedKey, value) in prefixes) {
            val storedSource = storedKey.first
            val storedApparent = storedKey.second
            if (sourceRepo.startsWith(storedSource) && apparentName == storedApparent) {
                return value
            }
        }

        return null
    }
}

/** An interface for accessing Bazel runfiles. */
class Runfiles internal constructor(
    private val mode: Mode,
    private val repoMapping: RepoMapping,
) {
    companion object {
        /**
         * Creates a manifest based [Runfiles] object when the manifest file
         * environment variable is present with a non-empty value, or a
         * directory based [Runfiles] object otherwise.
         */
        fun create(): Result<Runfiles> = create(RealRunfilesSys())

        internal fun create(sys: RunfilesSys): Result<Runfiles> {
            val manifestFile = sys.env(MANIFEST_FILE_ENV_VAR)
            val mode = if (!manifestFile.isNullOrEmpty()) {
                createManifestBased(manifestFile)
            } else {
                val dir = findRunfilesDir(sys).getOrElse { return Result.failure(it) }
                val manifestPath = pathJoin(dir, "MANIFEST")
                if (SystemFileSystem.exists(Path(manifestPath))) {
                    createManifestBased(manifestPath)
                } else {
                    Result.success(Mode.DirectoryBased(dir))
                }
            }.getOrElse { return Result.failure(it) }

            val repoMapping = rawRlocation(mode, "_repo_mapping")
                ?.takeIf { SystemFileSystem.exists(Path(it)) }
                ?.let { parseRepoMapping(it).getOrElse { error -> return Result.failure(error) } }
                ?: RepoMapping.default()

            return Result.success(Runfiles(mode, repoMapping))
        }

        private fun createManifestBased(manifestPath: String): Result<Mode> {
            val manifestContent = readText(manifestPath)
                .getOrElse { return Result.failure(RunfilesError.RunfilesManifestIoError(it)) }
            val pathMapping = mutableMapOf<String, String>()
            for (line in rustLines(manifestContent)) {
                val index = line.indexOf(' ')
                if (index < 0) {
                    return Result.failure(RunfilesError.RunfilesManifestInvalidFormat)
                }
                val pair = line.substring(0, index) to line.substring(index + 1)
                pathMapping[pair.first] = pair.second
            }
            return Result.success(Mode.ManifestBased(pathMapping))
        }
    }

    /**
     * Returns the runtime path of a runfile.
     *
     * Runfiles are data dependencies of Bazel-built binaries and tests. The
     * returned path may not be valid. The caller should check that the path
     * exists before opening it.
     */
    fun rlocation(path: String): String? {
        if (isAbsolutePath(path)) {
            return path
        }
        return rawRlocation(mode, path)
    }

    /**
     * Returns the runtime path of a runfile from an explicit source repository.
     */
    fun rlocationFrom(path: String, sourceRepo: String): String? {
        if (isAbsolutePath(path)) {
            return path
        }

        val slash = path.indexOf('/')
        val repoAlias: String
        val repoPath: String?
        if (slash >= 0) {
            repoAlias = path.substring(0, slash)
            repoPath = path.substring(slash + 1)
        } else {
            repoAlias = path
            repoPath = null
        }

        val targetRepoDirectory = repoMapping.get(sourceRepo to repoAlias)
        return if (targetRepoDirectory != null) {
            if (repoPath != null) {
                rawRlocation(mode, "$targetRepoDirectory/$repoPath")
            } else {
                rawRlocation(mode, targetRepoDirectory)
            }
        } else {
            rawRlocation(mode, path)
        }
    }
}

internal fun rawRlocation(mode: Mode, path: String): String? =
    when (mode) {
        is Mode.DirectoryBased -> pathJoin(mode.path, path)
        is Mode.ManifestBased -> mode.pathMapping[path]
    }

internal fun parseRepoMapping(path: String): Result<RepoMapping> {
    val exact = mutableMapOf<RepoMappingKey, String>()
    val prefixes = mutableMapOf<RepoMappingKey, String>()

    val content = readText(path)
        .getOrElse { return Result.failure(RunfilesError.RepoMappingIoError(it)) }
    for (line in rustLines(content)) {
        val parts = line.split(',', limit = 3)
        if (parts.size < 3) {
            return Result.failure(RunfilesError.RepoMappingInvalidFormat)
        }

        val sourceRepo = parts[0]
        val apparentName = parts[1]
        val targetRepo = parts[2]

        val prefix = sourceRepo.removeSuffix("*")
        if (prefix.length != sourceRepo.length) {
            prefixes[prefix to apparentName] = targetRepo
        } else {
            exact[sourceRepo to apparentName] = targetRepo
        }
    }

    return Result.success(RepoMapping(exact = exact, prefixes = prefixes))
}

/** Returns the `.runfiles` directory for the currently executing binary. */
fun findRunfilesDir(): Result<String> = findRunfilesDir(RealRunfilesSys())

internal fun findRunfilesDir(sys: RunfilesSys): Result<String> {
    sys.env(MANIFEST_FILE_ENV_VAR)?.let { value ->
        check(value.isEmpty()) { "Unexpected call when $MANIFEST_FILE_ENV_VAR exists" }
    }

    sys.env(RUNFILES_DIR_ENV_VAR)?.let { runfilesDir ->
        if (isDirectory(runfilesDir)) {
            return Result.success(runfilesDir)
        }
    }
    sys.env(TEST_SRCDIR_ENV_VAR)?.let { testSrcdir ->
        if (isDirectory(testSrcdir)) {
            return Result.success(testSrcdir)
        }
    }

    val execPath = sys.args().firstOrNull()
        ?: return Result.failure(RunfilesError.RunfilesDirNotFound)
    val currentDir = sys.currentDir()
        .getOrElse { return Result.failure(RunfilesError.RunfilesDirIoError(it)) }

    var binaryPath = execPath
    while (true) {
        val runfilesName = fileName(binaryPath) + ".runfiles"
        val runfilesPath = withFileName(binaryPath, runfilesName)
        if (isDirectory(runfilesPath)) {
            return Result.success(runfilesPath)
        }

        var next = parentPath(binaryPath)
        while (next != null) {
            if (fileName(next).endsWith(".runfiles")) {
                return Result.success(next)
            }
            next = parentPath(next)
        }

        val isSymlink = sys.isSymlink(binaryPath)
            .getOrElse { return Result.failure(RunfilesError.RunfilesDirIoError(it)) }
        if (!isSymlink) {
            break
        }

        val linkTarget = sys.readLink(binaryPath)
            .getOrElse { return Result.failure(RunfilesError.RunfilesDirIoError(it)) }
        binaryPath = if (isAbsolutePath(linkTarget)) {
            linkTarget
        } else {
            val linkDir = parentPath(binaryPath) ?: ""
            pathJoin(pathJoin(currentDir, linkDir), linkTarget)
        }
    }

    return Result.failure(RunfilesError.RunfilesDirNotFound)
}

internal interface RunfilesSys {
    fun env(name: String): String?
    fun args(): List<String>
    fun currentDir(): Result<String>
    fun isSymlink(path: String): Result<Boolean>
    fun readLink(path: String): Result<String>
}

internal expect class RealRunfilesSys() : RunfilesSys {
    override fun env(name: String): String?
    override fun args(): List<String>
    override fun currentDir(): Result<String>
    override fun isSymlink(path: String): Result<Boolean>
    override fun readLink(path: String): Result<String>
}

private fun readText(path: String): Result<String> =
    runCatching {
        SystemFileSystem.source(Path(path)).buffered().use { source ->
            source.readString()
        }
    }

private fun rustLines(text: String): List<String> {
    if (text.isEmpty()) {
        return emptyList()
    }
    val withoutFinalTerminator = when {
        text.endsWith("\r\n") -> text.dropLast(2)
        text.endsWith('\n') || text.endsWith('\r') -> text.dropLast(1)
        else -> text
    }
    if (withoutFinalTerminator.isEmpty()) {
        return emptyList()
    }
    return withoutFinalTerminator.split('\n').map { line -> line.removeSuffix("\r") }
}

private fun isDirectory(path: String): Boolean =
    SystemFileSystem.metadataOrNull(Path(path))?.isDirectory == true

internal fun pathJoin(left: String, right: String): String {
    if (left.isEmpty()) return right
    if (right.isEmpty()) return left
    if (isAbsolutePath(right)) return right
    val separator = if (left.contains('\\') && !left.contains('/')) "\\" else "/"
    return left.trimEnd('/', '\\') + separator + right.trimStart('/', '\\')
}

private fun isAbsolutePath(path: String): Boolean =
    path.startsWith('/') ||
        path.startsWith("\\\\") ||
        (path.length >= 3 && path[1] == ':' && (path[2] == '\\' || path[2] == '/'))

private fun fileName(path: String): String {
    val trimmed = path.trimEnd('/', '\\')
    val slash = maxOf(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'))
    return if (slash >= 0) trimmed.substring(slash + 1) else trimmed
}

private fun parentPath(path: String): String? {
    val trimmed = path.trimEnd('/', '\\')
    val slash = maxOf(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'))
    return when {
        slash < 0 -> null
        slash == 0 -> trimmed.substring(0, 1)
        slash == 2 && trimmed.getOrNull(1) == ':' -> trimmed.substring(0, 3)
        else -> trimmed.substring(0, slash)
    }
}

private fun withFileName(path: String, name: String): String {
    val parent = parentPath(path)
    return if (parent == null) name else pathJoin(parent, name)
}
