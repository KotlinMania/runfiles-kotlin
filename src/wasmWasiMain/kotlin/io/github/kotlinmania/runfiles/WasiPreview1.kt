// Platform hooks for the common runfiles port.
@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class, kotlin.wasm.unsafe.UnsafeWasmMemoryApi::class)

package io.github.kotlinmania.runfiles

import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.withScopedMemoryAllocator

internal object WasiPreview1 {
    fun getenv(name: String): String? {
        val prefix = "$name="
        return environ().firstOrNull { it.startsWith(prefix) }?.substring(prefix.length)
    }

    fun args(): List<String> =
        withScopedMemoryAllocator { allocator ->
            val countPtr = allocator.allocate(Int.SIZE_BYTES)
            val sizePtr = allocator.allocate(Int.SIZE_BYTES)
            val sizesErrno = argsSizesGet(countPtr.address.toInt(), sizePtr.address.toInt())
            if (sizesErrno != WASI_ERRNO_SUCCESS) return@withScopedMemoryAllocator emptyList()

            val count = countPtr.loadInt()
            val bufferSize = sizePtr.loadInt()
            if (count <= 0 || bufferSize <= 0) return@withScopedMemoryAllocator emptyList()

            val argvPtr = allocator.allocate(count * Int.SIZE_BYTES)
            val bufferPtr = allocator.allocate(bufferSize)
            val getErrno = argsGet(argvPtr.address.toInt(), bufferPtr.address.toInt())
            if (getErrno != WASI_ERRNO_SUCCESS) return@withScopedMemoryAllocator emptyList()

            buildList(count) {
                repeat(count) { index ->
                    val entryPtr = Pointer((argvPtr + index * Int.SIZE_BYTES).loadInt().toUInt())
                    add(entryPtr.readNullTerminatedString())
                }
            }
        }

    private fun environ(): List<String> =
        withScopedMemoryAllocator { allocator ->
            val countPtr = allocator.allocate(Int.SIZE_BYTES)
            val sizePtr = allocator.allocate(Int.SIZE_BYTES)
            val sizesErrno = environSizesGet(countPtr.address.toInt(), sizePtr.address.toInt())
            if (sizesErrno != WASI_ERRNO_SUCCESS) return@withScopedMemoryAllocator emptyList()

            val count = countPtr.loadInt()
            val bufferSize = sizePtr.loadInt()
            if (count <= 0 || bufferSize <= 0) return@withScopedMemoryAllocator emptyList()

            val environPtr = allocator.allocate(count * Int.SIZE_BYTES)
            val bufferPtr = allocator.allocate(bufferSize)
            val getErrno = environGet(environPtr.address.toInt(), bufferPtr.address.toInt())
            if (getErrno != WASI_ERRNO_SUCCESS) return@withScopedMemoryAllocator emptyList()

            buildList(count) {
                repeat(count) { index ->
                    val entryPtr = Pointer((environPtr + index * Int.SIZE_BYTES).loadInt().toUInt())
                    add(entryPtr.readNullTerminatedString())
                }
            }
        }
}

private fun Pointer.readNullTerminatedString(): String {
    var length = 0
    while ((this + length).loadByte().toInt() != 0) {
        length += 1
    }
    val bytes = ByteArray(length)
    for (index in 0 until length) {
        bytes[index] = (this + index).loadByte()
    }
    return bytes.decodeToString()
}

private const val WASI_ERRNO_SUCCESS = 0

@WasmImport("wasi_snapshot_preview1", "args_sizes_get")
private external fun argsSizesGet(argumentCount: Int, argumentBufferSize: Int): Int

@WasmImport("wasi_snapshot_preview1", "args_get")
private external fun argsGet(arguments: Int, argumentBuffer: Int): Int

@WasmImport("wasi_snapshot_preview1", "environ_sizes_get")
private external fun environSizesGet(environCount: Int, environBufferSize: Int): Int

@WasmImport("wasi_snapshot_preview1", "environ_get")
private external fun environGet(environ: Int, environBuffer: Int): Int
