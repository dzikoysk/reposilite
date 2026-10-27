# Java certificate security patch

Both Docker stages apply `patch.sh` before running Java. It merges `blocked.certs`
with the runtime's existing blocklist and removes the four trust roots listed in
[Oracle's current guidance](https://ops.java/emudhra/):

- emSign Root CA - G1
- emSign Root CA - G2
- emSign ECC Root CA - G3
- AAA Certificate Services

The original blocklist addresses CVE-2026-91403. Azul now warns that the blocklist
alone does not cover the related bypass, CVE-2026-114820. Removing the trust roots
also rejects chains using alternative certificate signature encodings. Connections
that depend on these roots will no longer be trusted. Roots are selected by their
certificate subject because aliases differ between JDK distributions.

`blocked.certs` is the unmodified non-Windows blocklist published by Azul,
retrieved on 2026-10-23. It uses OpenJDK's SHA-256 blocklist format.

- Guidance: https://docs.azul.com/core/emudhra-workaround
- Source: https://docs.azul.com/assets/2026-10/emudhra-block-unix.certs
- Published SHA-256: `2596bb6dda83c66d96c5a252d7b2ae1f24cc1ae3f1890223b4f3591a8d52bf10`
- Bypass advisory: https://docs.azul.com/core/release/october-2026-24-respin/release-notes

Merging preserves certificates blocked by future base-image updates. Keeping the
patch in the repository makes builds independent of vendor download services.
Remove it only after verifying that both Temurin base images reject the affected
chains, including alternative certificate signature encodings.
