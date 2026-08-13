// Platform hooks for the common runfiles port.
package io.github.kotlinmania.runfiles

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

internal actual class RealRunfilesSys actual constructor() : RunfilesSys {
    actual override fun env(name: String): String? =
        WasiPreview1.getenv(name)?.takeIf { it.isNotEmpty() }

    actual override fun args(): List<String> = WasiPreview1.args()

    actual override fun currentDir(): Result<String> =
        runCatching { SystemFileSystem.resolve(Path(".")).toString() }

    actual override fun isSymlink(path: String): Result<Boolean> =
        Result.success(false)

    actual override fun readLink(path: String): Result<String> =
        Result.failure(IllegalStateException("readlink($path): symlink targets are unavailable on Wasm-WASI"))
}
