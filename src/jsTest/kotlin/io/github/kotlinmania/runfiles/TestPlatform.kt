// port-lint: ignore
// Test host capability check for filesystem-backed runfiles cases.
package io.github.kotlinmania.runfiles

internal actual fun supportsHostFileSystem(): Boolean =
    jsHasNodeFileSystem()

private fun jsHasNodeFileSystem(): Boolean = js(
    "(function(){ return typeof process !== 'undefined' && !!(process.versions && process.versions.node); })()",
)
