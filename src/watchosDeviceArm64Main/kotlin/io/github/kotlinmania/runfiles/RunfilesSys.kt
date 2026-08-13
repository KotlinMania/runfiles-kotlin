// Platform hooks for the common runfiles port.
package io.github.kotlinmania.runfiles

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.getcwd
import platform.posix.readlink
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformCurrentDir(): Result<String> =
    memScoped {
        val buf = allocArray<ByteVar>(PATH_MAX_BYTES)
        val result = getcwd(buf, PATH_MAX_BYTES.convert())
        if (result == null) {
            Result.failure(platformIoError("getcwd"))
        } else {
            Result.success(result.toKString())
        }
    }

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformReadLink(path: String): Result<String> =
    memScoped {
        val buf = allocArray<ByteVar>(PATH_MAX_BYTES)
        val count = readlink(path, buf, (PATH_MAX_BYTES - 1).convert()).convert<Int>()
        if (count < 0) {
            Result.failure(platformIoError("readlink($path)"))
        } else {
            Result.success(buf.readBytes(count).decodeToString())
        }
    }

@OptIn(ExperimentalForeignApi::class)
private fun platformIoError(context: String): Throwable {
    val code = errno
    val message = strerror(code)?.toKString() ?: "errno=$code"
    return IllegalStateException("$context: $message ($code)")
}

private const val PATH_MAX_BYTES: Int = 4096
