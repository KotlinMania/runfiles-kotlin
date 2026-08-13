// Platform hooks for the common runfiles port.
package io.github.kotlinmania.runfiles

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.getcwd
import platform.posix.getenv
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class)
internal actual class RealRunfilesSys actual constructor() : RunfilesSys {
    actual override fun env(name: String): String? =
        getenv(name)?.toKString()?.takeIf { it.isNotEmpty() }

    actual override fun args(): List<String> =
        listOfNotNull(env("_"))

    actual override fun currentDir(): Result<String> =
        memScoped {
            val buf = allocArray<ByteVar>(PATH_MAX_BYTES)
            val result = getcwd(buf, PATH_MAX_BYTES.convert())
            if (result == null) {
                Result.failure(lastIoError("getcwd"))
            } else {
                Result.success(result.toKString())
            }
        }

    actual override fun isSymlink(path: String): Result<Boolean> =
        Result.success(false)

    actual override fun readLink(path: String): Result<String> =
        Result.failure(lastIoError("readlink($path)"))

    private fun lastIoError(context: String): Throwable {
        val code = errno
        val message = strerror(code)?.toKString() ?: "errno=$code"
        return IllegalStateException("$context: $message ($code)")
    }
}

private const val PATH_MAX_BYTES: Int = 4096
