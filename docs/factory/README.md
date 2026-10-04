# The factory as a shareable artifact

`urlshort-factory.rigbundle` is the OpenRig 0.6.3 bundle of `rig/`: the rig spec with its twelve seats, the role specs and guidance, the protocol (`startup/project.md`) and culture (`CULTURE.md`), and the vendored shared skills (93 files). `urlshort-factory.rigbundle.sha256` holds its checksum. The workflow specs are not in the bundle; they live in `rig/workflows/`: the three slice variants (`urlshort-slice`, `urlshort-slice-delegated`, `urlshort-slice-delegated-b`) and `urlshort-drill`.

It contains configuration only: no queue state, transcripts, credentials or product code. Those live in `docs/evidence/`, `missions/` and `src/`.

To bring the factory up elsewhere, with OpenRig 0.6.3 and the Claude Code and Codex runtimes logged in, see `docs/SETUP-FACTORY.md` and the `rig-bundles-and-shareable-artifacts` skill (`rig context get skills/core/rig-bundles-and-shareable-artifacts`).
