---
name: mes-test-verification
description: Use when deciding or running verification for badminton MES changes, including Maven tests, frontend builds, schema/entity consistency checks, smoke tests, or documenting validation results before commit.
---

# MES Test Verification

## Choose Verification by Change Type

| Change | Minimum verification |
|---|---|
| Backend Java | Maven test from `workspace/idea/ems` |
| Mapper/entity/schema | Maven test plus schema/entity check |
| PC frontend | `npm run build` from `workspace/vscode/ems-forword` |
| API integration | backend test plus frontend build or smoke request |
| Docs only | Markdown path/link/table review |
| Git hook or workflow | dry-run relevant Git command when safe |

## Backend Command

From `workspace/idea/ems`:

```powershell
java "-Dmaven.multiModuleProjectDirectory=D:\soft\XiaoXueQiworkspace\workspace\idea\ems" -cp .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain test
```

## Frontend Command

From `workspace/vscode/ems-forword`:

```powershell
npm run build
```

## Reporting Format

Record:

- command;
- working directory;
- pass/fail;
- important warnings;
- reason if not run.

## Must Not

- Do not claim tests passed if only compilation or file review was done.
- Do not skip validation silently.
- Do not treat unrelated warnings as fixed unless actually addressed.
