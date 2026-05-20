// port-lint: ignore
// Platform hooks for the common runfiles port.
package io.github.kotlinmania.runfiles

internal actual class RealRunfilesSys actual constructor() : RunfilesSys {
    actual override fun env(name: String): String? =
        System.getenv(name)?.takeIf { it.isNotEmpty() }

    actual override fun args(): List<String> {
        val command = System.getProperty("sun.java.command") ?: return emptyList()
        return command.split(' ').filter { it.isNotEmpty() }
    }

    actual override fun currentDir(): Result<String> =
        System.getProperty("user.dir")
            ?.takeIf { it.isNotEmpty() }
            ?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("The current working directory is always expected to be set."))

    actual override fun isSymlink(path: String): Result<Boolean> =
        runCatching { java.nio.file.Files.isSymbolicLink(java.nio.file.Path.of(path)) }

    actual override fun readLink(path: String): Result<String> =
        runCatching { java.nio.file.Files.readSymbolicLink(java.nio.file.Path.of(path)).toString() }
}
