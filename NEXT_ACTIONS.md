# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 1/1 (100.0%)
- **Function parity:** 25/28 matched (target 50) — 89.3%
- **Class/type parity:** 6/6 matched (target 21) — 100.0%
- **Combined symbol parity:** 31/34 matched (target 71) — 91.2%
- **Average inline-code cosine:** 0.58 (function body across 1 matched files)
- **Average documentation cosine:** 0.89 (doc text across 1 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 1 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. runfiles

- **Target:** `runfiles.Runfiles`
- **Similarity:** 0.58
- **Dependents:** 0
- **Priority Score:** 33404.2
- **Functions:** 25/28 matched (target 50)
- **Missing functions:** `with_mock_env`, `make_runfiles_like_dir`, `dedent`
- **Types:** 6/6 matched (target 21)
- **Missing types:** _none_
- **Tests:** 12/15 matched

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Initialize task queue for systematic porting
cd tools/ast_distance
./ast_distance --init-tasks ../../tmp/runfiles/rust/runfiles rust ../../src kotlin tasks.json ../../AGENTS.md

# Get next high-priority task
./ast_distance --assign tasks.json <agent-id>
```
