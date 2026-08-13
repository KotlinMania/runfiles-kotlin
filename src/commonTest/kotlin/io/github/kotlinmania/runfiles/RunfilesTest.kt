// port-lint: source runfiles.rs
package io.github.kotlinmania.runfiles

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun <R> withMockEnv(kvs: Map<String, String?>, closure: (RunfilesSys) -> R): R {
    val sys = FakeRunfilesSys(kvs)
    return closure(sys)
}

class RunfilesTest {
    @Test
    fun testMockEnv() {
        val originalName = "rules_rust"
        val mockedName =
            withMockEnv(mapOf("TEST_WORKSPACE" to "foobar")) { sys ->
                sys.env("TEST_WORKSPACE") ?: ""
            }

        assertEquals("foobar", mockedName)
        assertEquals("rules_rust", originalName)
    }

    private fun makeRunfilesLikeDir(name: String): String {
        val root = testRoot(name)
        val path = "rules_rust/rust/runfiles/data/sample.txt"
        val testPath = Path(root, path)
        SystemFileSystem.createDirectories(parentPath(testPath) ?: Path(root))
        SystemFileSystem.sink(testPath).buffered().use { sink ->
            sink.writeString("Example Text!")
        }
        return root
    }

    @Test
    fun testStandardLookup() {
        if (!supportsHostFileSystem()) return

        val runfilesDir = makeRunfilesLikeDir("test_standard_lookup")
        val r = Runfiles.create(FakeRunfilesSys(mapOf(RUNFILES_DIR_ENV_VAR to runfilesDir))).getOrThrow()

        val f = assertNotNull(r.rlocation("rules_rust/rust/runfiles/data/sample.txt"))
        val buffer = SystemFileSystem.source(Path(f)).buffered().use { source -> source.readString() }

        assertEquals("Example Text!", buffer)
    }

    @Test
    fun testEnvOnlyRunfilesDir() {
        if (!supportsHostFileSystem()) return

        val runfilesDir = makeRunfilesLikeDir("test_env_only_runfiles_dir")

        withMockEnv(
            mapOf(
                MANIFEST_FILE_ENV_VAR to null,
                RUNFILES_DIR_ENV_VAR to runfilesDir,
                TEST_SRCDIR_ENV_VAR to null,
            ),
        ) { sys ->
            val r = Runfiles.create(sys).getOrThrow()

            val d = assertNotNull(r.rlocation("rules_rust"))
            val f = assertNotNull(r.rlocation("rules_rust/rust/runfiles/data/sample.txt"))
            assertEquals(pathJoin(d, "rust/runfiles/data/sample.txt"), f)

            val buffer = SystemFileSystem.source(Path(f)).buffered().use { source -> source.readString() }
            assertEquals("Example Text!", buffer)
        }
    }

    @Test
    fun testRunfilesManifestFileEmpty() {
        if (!supportsHostFileSystem()) return

        val runfilesDir = makeRunfilesLikeDir("test_runfiles_manifest_file_empty")

        withMockEnv(
            mapOf(
                MANIFEST_FILE_ENV_VAR to "",
                RUNFILES_DIR_ENV_VAR to runfilesDir,
                TEST_SRCDIR_ENV_VAR to null,
            ),
        ) { sys ->
            val r = Runfiles.create(sys).getOrThrow()

            val d = assertNotNull(r.rlocation("rules_rust"))
            val f = assertNotNull(r.rlocation("rules_rust/rust/runfiles/data/sample.txt"))
            assertEquals(pathJoin(d, "rust/runfiles/data/sample.txt"), f)

            val buffer = SystemFileSystem.source(Path(f)).buffered().use { source -> source.readString() }
            assertEquals("Example Text!", buffer)
        }
    }

    @Test
    fun testEnvOnlyTestSrcdir() {
        if (!supportsHostFileSystem()) return

        val runfilesDir = makeRunfilesLikeDir("test_env_only_test_srcdir")

        withMockEnv(
            mapOf(
                MANIFEST_FILE_ENV_VAR to null,
                RUNFILES_DIR_ENV_VAR to null,
                TEST_SRCDIR_ENV_VAR to runfilesDir,
            ),
        ) { sys ->
            val r = Runfiles.create(sys).getOrThrow()

            val runfile = assertNotNull(r.rlocation("rules_rust/rust/runfiles/data/sample.txt"))
            val buffer = SystemFileSystem.source(Path(runfile)).buffered().use { source -> source.readString() }

            assertEquals("Example Text!", buffer)
        }
    }

    @Test
    fun testEnvNothingSet() {
        if (!supportsHostFileSystem()) return

        val runfilesDir = makeRunfilesLikeDir("test_env_nothing_set_binary.runfiles")
        val binary = pathJoin(testRoot("bin"), "test_env_nothing_set_binary")
        val sibling = "$binary.runfiles"
        SystemFileSystem.createDirectories(Path(binary).parent ?: Path("build/tmp"))
        SystemFileSystem.createDirectories(Path(sibling).parent ?: Path("build/tmp"))
        copyRunfilesTree(runfilesDir, sibling)

        withMockEnv(
            mapOf(
                RUNFILES_DIR_ENV_VAR to null,
                TEST_SRCDIR_ENV_VAR to null,
                MANIFEST_FILE_ENV_VAR to null,
            ),
        ) { sys ->
            val fake = (sys as FakeRunfilesSys).copy(args = listOf(binary))
            val r = Runfiles.create(fake).getOrThrow()
            val runfile = assertNotNull(r.rlocation("rules_rust/rust/runfiles/data/sample.txt"))
            val buffer = SystemFileSystem.source(Path(runfile)).buffered().use { source -> source.readString() }
            assertEquals("Example Text!", buffer)
        }
    }

    @Test
    fun testManifestBasedCanReadDataFromRunfiles() {
        val r =
            Runfiles(
                mode = Mode.ManifestBased(mapOf("a/b" to "c/d")),
                repoMapping = RepoMapping.new(),
            )

        assertEquals("c/d", r.rlocation("a/b"))
    }

    @Test
    fun testManifestBasedMissingFile() {
        val r =
            Runfiles(
                mode = Mode.ManifestBased(mapOf("a/b" to "c/d")),
                repoMapping = RepoMapping.new(),
            )

        assertNull(r.rlocation("does/not/exist"))
    }

    private fun dedent(text: String): String =
        text
            .lines()
            .joinToString("\n") { line -> line.trimStart() }

    @Test
    fun testParseRepoMapping() {
        if (!supportsHostFileSystem()) return

        val valid = Path(testRoot("test_parse_repo_mapping"), "test_parse_repo_mapping.txt")
        SystemFileSystem.createDirectories(valid.parent ?: Path("build/tmp"))
        SystemFileSystem.sink(valid).buffered().use { sink ->
            sink.writeString(
                dedent(
                    """
                    ,rules_rust,rules_rust
                    bazel_tools,__main__,rules_rust
                    local_config_cc,rules_rust,rules_rust
                    local_config_sh,rules_rust,rules_rust
                    local_config_xcode,rules_rust,rules_rust
                    platforms,rules_rust,rules_rust
                    rules_rust_tinyjson,rules_rust,rules_rust
                    rust_darwin_aarch64__aarch64-apple-darwin__stable_tools,rules_rust,rules_rust
                    """.trimIndent(),
                ),
            )
        }

        assertEquals(
            RepoMapping(
                prefixes = emptyMap(),
                exact =
                    mapOf(
                        ("local_config_xcode" to "rules_rust") to "rules_rust",
                        ("platforms" to "rules_rust") to "rules_rust",
                        ("rust_darwin_aarch64__aarch64-apple-darwin__stable_tools" to "rules_rust") to "rules_rust",
                        ("rules_rust_tinyjson" to "rules_rust") to "rules_rust",
                        ("local_config_sh" to "rules_rust") to "rules_rust",
                        ("bazel_tools" to "__main__") to "rules_rust",
                        ("local_config_cc" to "rules_rust") to "rules_rust",
                        ("" to "rules_rust") to "rules_rust",
                    ),
            ),
            parseRepoMapping(valid.toString()).getOrThrow(),
        )
    }

    @Test
    fun testParseRepoMappingInvalidFile() {
        if (!supportsHostFileSystem()) return

        val invalid = Path(testRoot("test_parse_repo_mapping_invalid_file"), "invalid.txt")

        assertTrue(parseRepoMapping(invalid.toString()).exceptionOrNull() is RunfilesError.RepoMappingIoError)

        SystemFileSystem.createDirectories(invalid.parent ?: Path("build/tmp"))
        SystemFileSystem.sink(invalid).buffered().use { sink -> sink.writeString("invalid") }

        assertEquals(
            RunfilesError.RepoMappingInvalidFormat,
            parseRepoMapping(invalid.toString()).exceptionOrNull(),
        )
    }

    @Test
    fun testParseRepoMappingWithWildcard() {
        if (!supportsHostFileSystem()) return

        val mappingFile = Path(testRoot("test_parse_repo_mapping_with_wildcard"), "mapping.txt")
        SystemFileSystem.createDirectories(mappingFile.parent ?: Path("build/tmp"))
        SystemFileSystem.sink(mappingFile).buffered().use { sink ->
            sink.writeString(
                dedent(
                    """
                    +deps+*,aaa,_main
                    +deps+*,dep,+deps+dep1
                    +deps+*,dep1,+deps+dep1
                    +deps+*,dep2,+deps+dep2
                    +deps+*,dep3,+deps+dep3
                    +other+exact,foo,bar
                    """.trimIndent(),
                ),
            )
        }

        val repoMapping = parseRepoMapping(mappingFile.toString()).getOrThrow()

        assertEquals("bar", repoMapping.get("+other+exact" to "foo"))
        assertEquals("_main", repoMapping.get("+deps+dep1" to "aaa"))
        assertEquals("+deps+dep1", repoMapping.get("+deps+dep1" to "dep"))
        assertEquals("+deps+dep2", repoMapping.get("+deps+dep2" to "dep2"))
        assertEquals("+deps+dep3", repoMapping.get("+deps+dep3" to "dep3"))
    }

    @Test
    fun testRlocationFromWithWildcard() {
        val runfilesDir = "build/tmp/runfiles-tests/test_rlocation_from_with_wildcard.runfiles"

        val r =
            Runfiles(
                mode = Mode.DirectoryBased(runfilesDir),
                repoMapping =
                    RepoMapping(
                        exact = emptyMap(),
                        prefixes =
                            mapOf(
                                ("+deps+" to "aaa") to "_main",
                                ("+deps+" to "dep") to "+deps+dep1",
                            ),
                    ),
            )

        assertEquals(pathJoin(runfilesDir, "_main/some/path"), r.rlocationFrom("aaa/some/path", "+deps+dep1"))
        assertEquals(pathJoin(runfilesDir, "_main/other/path"), r.rlocationFrom("aaa/other/path", "+deps+dep2"))
        assertEquals(pathJoin(runfilesDir, "+deps+dep1/foo/bar"), r.rlocationFrom("dep/foo/bar", "+deps+dep3"))
        assertEquals(pathJoin(runfilesDir, "aaa/path"), r.rlocationFrom("aaa/path", "+other+repo"))
    }

    private fun testRoot(name: String): String {
        val root = Path("build/tmp/runfiles-tests", name)
        deleteRecursively(root)
        SystemFileSystem.createDirectories(root)
        return root.toString()
    }

    private fun copyRunfilesTree(from: String, to: String) {
        val source = Path(from, "rules_rust/rust/runfiles/data/sample.txt")
        val target = Path(to, "rules_rust/rust/runfiles/data/sample.txt")
        SystemFileSystem.createDirectories(target.parent ?: Path(to))
        val content = SystemFileSystem.source(source).buffered().use { it.readString() }
        SystemFileSystem.sink(target).buffered().use { it.writeString(content) }
    }

    private fun parentPath(path: Path): Path? {
        val text = path.toString()
        val index = text.lastIndexOf('/')
        return if (index <= 0) null else Path(text.substring(0, index))
    }

    private fun deleteRecursively(path: Path) {
        val metadata = SystemFileSystem.metadataOrNull(path) ?: return
        if (metadata.isDirectory) {
            for (child in SystemFileSystem.list(path)) {
                deleteRecursively(child)
            }
        }
        SystemFileSystem.delete(path, mustExist = false)
    }
}

internal expect fun supportsHostFileSystem(): Boolean

private data class FakeRunfilesSys(
    private val env: Map<String, String?>,
    private val cwd: String = ".",
    private val args: List<String> = emptyList(),
    private val symlinks: Map<String, String> = emptyMap(),
) : RunfilesSys {
    override fun env(name: String): String? = env[name]

    override fun args(): List<String> = args

    override fun currentDir(): Result<String> = Result.success(cwd)

    override fun isSymlink(path: String): Result<Boolean> =
        Result.success(symlinks.containsKey(path))

    override fun readLink(path: String): Result<String> =
        symlinks[path]?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("readlink($path): not a symlink"))
}
