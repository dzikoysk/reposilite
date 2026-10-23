# Java certificate blocklist

`blocked.certs` is the unmodified non-Windows blocklist published by Azul for
the eMudhra certificate compromise (CVE-2026-91403), retrieved on 2026-10-23.
It uses OpenJDK's SHA-256 blocklist format, also supported by Temurin.

- Guidance: https://docs.azul.com/core/emudhra-workaround
- Source: https://docs.azul.com/assets/2026-10/emudhra-block-unix.certs
- Published SHA-256: `2596bb6dda83c66d96c5a252d7b2ae1f24cc1ae3f1890223b4f3591a8d52bf10`
- Upstream mitigation: https://ops.java/emudhra/

Both Docker stages merge this file with the runtime's existing blocklist before
running Java. Merging preserves certificates blocked by future base-image
updates. Keeping the file in the repository makes builds independent of the
vendor download service.

Remove this mitigation only after verifying that both Temurin base images reject
the compromised certificates, including alternative ECDSA signature encodings.
