// port-lint: ignore
// Platform hooks for the common runfiles port.
package io.github.kotlinmania.runfiles

internal actual class RealRunfilesSys actual constructor() : RunfilesSys {
    actual override fun env(name: String): String? {
        val value = jsEnv(name)
        return if (value == undefined() || value == null) null else value.unsafeCast<String>().takeIf { it.isNotEmpty() }
    }

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

private fun jsProcess(): dynamic = js("typeof process === 'undefined' ? null : process")

private fun jsEnv(name: String): dynamic {
    val process = jsProcess()
    return if (process == null) null else process.env[name]
}

private fun jsArgs(): String = js(
    "(function(){ try { if (typeof process === 'undefined' || !process.argv) return ''; return process.argv.join('\\u0000'); } catch (e) { return ''; } })()",
)

private fun jsCwd(): String = js(
    "(function(){ try { return (typeof process !== 'undefined' && process.cwd) ? process.cwd() : ''; } catch (e) { return ''; } })()",
)

private fun jsIsSymlink(path: String): Boolean = js(
    "(function(p){ try { var r = eval('typeof require === \"function\" ? require : null'); return r ? r('fs').lstatSync(p).isSymbolicLink() : false; } catch (e) { return false; } })(path)",
)

private fun jsReadLink(path: String): String = js(
    "(function(p){ var r = eval('typeof require === \"function\" ? require : null'); if (!r) throw new Error('require unavailable'); return r('fs').readlinkSync(p); })(path)",
)

private fun undefined(): dynamic = js("undefined")
