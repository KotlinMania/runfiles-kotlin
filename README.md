# runfiles-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Frunfiles--kotlin-blue.svg)](https://github.com/KotlinMania/runfiles-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/runfiles-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/runfiles-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/runfiles-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/runfiles-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`bazelbuild/rules_rust`](https://github.com/bazelbuild/rules_rust/tree/main/rust/runfiles).

**Original Project:** This port is based on [`bazelbuild/rules_rust`](https://github.com/bazelbuild/rules_rust/tree/main/rust/runfiles). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `bazelbuild/rules_rust`

> The text below is reproduced and lightly edited from [`https://github.com/bazelbuild/rules_rust/tree/main/rust/runfiles`](https://github.com/bazelbuild/rules_rust/tree/main/rust/runfiles). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## Rust Rules

* Postsubmit [![Build status](https://badge.buildkite.com/76523cc666caab9ca91c2a08d9ac8f84af28cb25a92f387293.svg?branch=main)](https://buildkite.com/bazel/rustlang-rules-rust-postsubmit?branch=main)

## Overview

This repository provides rules for building [Rust](https://www.rust-lang.org/) projects with [Bazel](https://bazel.build/).

### Starter repo

The fastest way to try this in an empty project is to click the green "Use this template" button on https://github.com/bazel-starters/rust.

## Community

General discussions and announcements take place in the [GitHub Discussions](https://github.com/bazelbuild/rules_rust/discussions), but there are
additional places where community members gather to discuss `rules_rust`.

* Chat: [#rust](https://bazelbuild.slack.com/archives/CSV56UT0F) channel on Bazel Slack: [https://slack.bazel.build/](https://slack.bazel.build/)
* **Archived** Developer mailing list: [groups.google.com/g/rules_rust](https://groups.google.com/g/rules_rust)

## Documentation

<!-- TODO: Render generated docs on the github pages site again, https://bazelbuild.github.io/rules_rust/ -->

Please refer to [the full documentation](https://bazelbuild.github.io/rules_rust).

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:runfiles-kotlin:0.1.0-SNAPSHOT")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same Apache-2.0 license as the upstream [`bazelbuild/rules_rust`](https://github.com/bazelbuild/rules_rust/tree/main/rust/runfiles). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the rules_rust authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`bazelbuild/rules_rust`](https://github.com/bazelbuild/rules_rust/tree/main/rust/runfiles) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
