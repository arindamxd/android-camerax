---
name: bump-version
description: >-
  Increments versionCodeOffset for another Play upload of the same versionName,
  updates RELEASE.md shipping numbers, and prints Play Console release notes
  from git log since the previous upload. Use when the user says bump version,
  version bump, or bump the version.
---

# Bump version (Play upload)

Trigger phrases: **bump version**, **version bump**, **bump the version**. Run immediately. Do not ask offset vs name unless they named major/minor/patch.

Same workflow as [`.cursor/rules/bump-version.mdc`](../../../.cursor/rules/bump-version.mdc).

## 1. Bump `versionCodeOffset`

Same `versionName` (another Play upload of `X.Y.Z`):

1. In root [`build.gradle.kts`](../../../build.gradle.kts), increment `ext["versionCodeOffset"]` by 1.
2. `versionCode` = `major * 10000 + minor * 100 + patch + offset` (example: 1.7.0 + offset 4 → **10704**).
3. Update [`RELEASE.md`](../../../RELEASE.md) **Current shipping target** and Play Console **Version** lines. Do not rewrite “Last local verification” as if those checks already ran on the new code.

Only bump `versionMajor` / `versionMinor` / `versionPatch` (and reset offset to 0) if the user explicitly asks for a new `X.Y.Z`.

Do not commit unless asked.

## 2. Changelog range

Previous Play upload = the most recent commit whose subject is exactly `version bumped` **before** this bump (if HEAD is already that commit, use the one before it). Fallback: newest git tag (`git tag --sort=-v:refname`).

```sh
git log --format='%s' <previous>..HEAD
```

Skip subjects: `version bumped`, merge commits, docs-only, agent/DI/build-only churn.

## 3. Print Play Console notes

After the bump, reply with:

1. One line: `versionName` / new `versionCode` / new offset.
2. Raw bullets: remaining git subjects (changelog source).
3. Paste-ready Play notes:

```
Release name: {versionName}

<en-US>
What's new in CameraX {versionName}

• {user-facing highlight}
• {user-facing highlight}
• Stability and performance improvements
</en-US>
```

User-facing highlights only (4–6 bullets: modes, capture, gallery, Settings, UI). Skip architecture / DI / agent docs / build-only churn. End with a stability bullet when the range includes fixes. Do not invent features. Format details: [AGENTS.md](../../../AGENTS.md) and [`.cursor/rules/play-console-release-notes.mdc`](../../../.cursor/rules/play-console-release-notes.mdc).
