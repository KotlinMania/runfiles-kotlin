// Platform hooks for the common runfiles port.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.kotlinmania.runfiles

internal actual class RealRunfilesSys actual constructor() : RunfilesSys {
    actual override fun env(name: String): String? =
        jsEnv(name)?.takeIf { it.isNotEmpty() }

    actual override fun args(): List<String> {
        val joined = jsArgs()
        return if (joined.isEmpty()) emptyList() else joined.split('\u0000')
    }

    actual override fun currentDir(): Result<String> =
        runCatching { jsCwd().takeIf { it.isNotEmpty() } ?: error("cwd unavailable") }

    actual override fun isSymlink(path: String): Result<Boolean> =
        runCatching { jsIsSymlink(path) }

    actual override fun readLink(path: String): Result<String> =
        runCatching { jsReadLink(path) }
}

@JsFun("(name) => { try { if (typeof process === 'undefined') return null; const v = process.env[name]; return v === undefined ? null : v; } catch (e) { return null; } }")
private external fun jsEnv(name: String): String?

@JsFun("() => { try { if (typeof process === 'undefined' || !process.argv) return ''; return process.argv.join('\\u0000'); } catch (e) { return ''; } }")
private external fun jsArgs(): String

@JsFun("() => { try { return (typeof process !== 'undefined' && process.cwd) ? process.cwd() : ''; } catch (e) { return ''; } }")
private external fun jsCwd(): String

@JsFun("(p) => { try { var r = eval('typeof require === \"function\" ? require : null'); return r ? r('fs').lstatSync(p).isSymbolicLink() : false; } catch (e) { return false; } }")
private external fun jsIsSymlink(path: String): Boolean

@JsFun("(p) => { var r = eval('typeof require === \"function\" ? require : null'); if (!r) throw new Error('require unavailable'); return r('fs').readlinkSync(p); }")
private external fun jsReadLink(path: String): String
