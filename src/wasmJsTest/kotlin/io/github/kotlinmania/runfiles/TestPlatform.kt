// Test host capability check for filesystem-backed runfiles cases.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.kotlinmania.runfiles

internal actual fun supportsHostFileSystem(): Boolean =
    wasmJsHasNodeFileSystem()

@JsFun("() => typeof process !== 'undefined' && !!(process.versions && process.versions.node)")
private external fun wasmJsHasNodeFileSystem(): Boolean
