# port-lint Proposed Changes

**Generated:** 2026-10-16
**Source:** tmp/runfiles/rust/runfiles/runfiles.rs
**Target:** src/commonMain/kotlin/io/github/kotlinmania/runfiles/Runfiles.kt

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/runfiles/Runfiles.kt` | `// port-lint: source runfiles.rs` | `// port-lint: source tmp/runfiles/rust/runfiles/runfiles.rs` | `tmp/runfiles/rust/runfiles/runfiles.rs` | `port-lint provenance header matched only by basename: 'runfiles.rs' vs expected 'tmp/runfiles/rust/runfiles/runfiles.rs'` |
