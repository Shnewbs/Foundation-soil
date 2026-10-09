# Native platform scope and GitHub Releases

## Scope decision — 2026-10-09

**Do not touch 1.21.1.** Upstream TFC maintenance stays with its developers.
The existing Soil `main` branch and 1.21.1 assets remain unchanged historical
references, not a branch to keep updating or backport into.

Our active immediate target is **26.1.2** on `mc-26.1.2`. Keep **26.3 → 26.4**
as the separate forward-port track. This checkpoint implements only 26.1.2 changes.
No default-branch change, deletion, force-push or old-release replacement is authorized
by this scope change.

## Release rules

Every delivered native version gets its own GitHub prerelease with the compiled JAR,
sources JAR, full source ZIP, test reports, validation notes and SHA-256 checksums.
Match the platform, mod version, source commit and classfile Java version before
publishing. Build and test before committing the verified implementation and
publishing its tag. Publish through a draft, never overwrite an existing release,
and never promote development ports to stable/latest.

Current checkpoint tag: `mc-26.1.2-v0.1.0-beta-port.2`.
Native compilation/model tests are separate from required TFC/Framework game startup.
Do not drop hard dependencies or claim playability while those gates remain open.
CI artifacts alone do not replace GitHub Releases.

## Forward targets

Verify native dependencies and actual builds for 26.3 and 26.4 independently. Do not
copy version numbers onto a 26.1.2 JAR. This policy does not itself create a scheduled
watcher or prove current 26.4 dependency readiness.
