# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 0/1 (0.0%)
- **Function parity:** 0/28 matched — 0.0%
- **Class/type parity:** 0/6 matched — 0.0%
- **Combined symbol parity:** 0/34 matched — 0.0%
- **Average inline-code cosine:** 0.00 (function body across 0 matched files)
- **Average documentation cosine:** 0.00 (doc text across 0 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 0 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Refresh the parity inventory with the shared workspace ast_distance binary
/Volumes/stuff/Projects/kotlinmania/bin/ast_distance --deep tmp/runfiles/rust/runfiles rust src/commonMain/kotlin/io/github/kotlinmania/runfiles kotlin

# The only source file currently missing is tmp/runfiles/rust/runfiles/runfiles.rs.
```
