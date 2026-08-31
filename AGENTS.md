# Developer Operating Notes

This repository is an Android 11-first project. Read `docs/CURRENT_ARCHITECTURE.md` and `docs/PROJECT_HANDOFF.md` before changing code.

Rules:
- Groovy Gradle is the active build source of truth.
- Preserve `com.aiham.quotes`, targetSdk 30 and the pinned BlackBox revision unless a documented migration is approved.
- Put BlackBox modifications in `blackbox-overrides/Bcore`.
- Preserve the `VirtualEngine` boundary.
- Do not run legacy setup/fix scripts on the active project.
- Treat build success and runtime success as separate facts.
- Never claim WhatsApp or Android 11 runtime success without real device evidence.
- Update `docs/PROJECT_HANDOFF.md` before stopping work.
- Preserve Git history; no force pushes.
