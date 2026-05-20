// port-lint: ignore
// Platform hooks for the common runfiles port.
package io.github.kotlinmania.runfiles

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.S_IFMT
import platform.posix.S_IFLNK
import platform.posix.errno
import platform.posix.getenv
import platform.posix.lstat
import platform.posix.stat
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class)
internal actual class RealRunfilesSys actual constructor() : RunfilesSys {
    actual override fun env(name: String): String? =
        getenv(name)?.toKString()?.takeIf { it.isNotEmpty() }

    actual override fun args(): List<String> =
        listOfNotNull(env("_"))

    actual override fun currentDir(): Result<String> = platformCurrentDir()

    actual override fun isSymlink(path: String): Result<Boolean> = memScoped {
        val sb = alloc<stat>()
        val rc = lstat(path, sb.ptr)
        if (rc != 0) {
            Result.failure(lastIoError("lstat($path)"))
        } else {
            val mode = sb.st_mode.toInt() and S_IFMT
            Result.success(mode == S_IFLNK)
        }
    }

    actual override fun readLink(path: String): Result<String> = platformReadLink(path)

    private fun lastIoError(context: String): Throwable {
        val code = errno
        val message = strerror(code)?.toKString() ?: "errno=$code"
        return IllegalStateException("$context: $message ($code)")
    }
}

internal expect fun platformCurrentDir(): Result<String>

internal expect fun platformReadLink(path: String): Result<String>
