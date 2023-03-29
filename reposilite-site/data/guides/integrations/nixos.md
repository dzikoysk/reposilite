---
id: nixos
title: NixOS
---

You can install Reposilite on NixOS using the following configuration.

Create a package expression in `/etc/nixos/reposilite-bin.nix` file (adjust JDK package 
and JVM properties to your needs):

```nix
{ pkgs, ... }:
let
  jdk = pkgs.openjdk17_headless;
  stdenv = pkgs.stdenv;
in
stdenv.mkDerivation rec {
  pname = "reposilite-bin";
  version = "3.3.2";

  jar = builtins.fetchurl {
    url="https://maven.reposilite.com/releases/com/reposilite/reposilite/${version}/reposilite-${version}-all.jar";
    sha256="369345847c98033ff2546d76e74702b859b41d05135997b4740d8e925f361a85";
  };

  dontUnpack = true;

  nativeBuildInputs = [ pkgs.makeWrapper ];
  installPhase = ''
    runHook preInstall
    makeWrapper ${jdk}/bin/java $out/bin/reposilite \
      --add-flags "-Xmx40m -jar $jar" \
      --set JAVA_HOME ${jdk}
    runHook postInstall
  '';
}
```

Put the Reposilite configuration in `/etc/nixos/reposilite.nix` (adjust `cfg.user`, `cfg.group` 
etc. to your needs):

```nix
{ config, pkgs, ... }:
let 
  reposilite = (import ./reposilite-bin.nix { inherit pkgs; }); 
  cfg = { 
    user = "reposilite"; 
    group = "reposilite"; 
    home = "/var/lib/reposilite"; 
    pkg = reposilite; 
    port = 8084;
  };
in
{
  environment.systemPackages = [
    cfg.pkg
  ];

  users.groups.${cfg.group} = {
    name = cfg.group;
  };

  users.users.${cfg.user} = {
    isSystemUser = true;
    group = cfg.group;
    home = cfg.home;
    createHome = true;
  };

  systemd.services."reposilite" = {
    description = "Reposilite - Maven repository";

    wantedBy = [ "multi-user.target" ];

    script = "${cfg.pkg}/bin/reposilite --working-directory ${cfg.home} --port ${toString cfg.port}";

    serviceConfig = {
      User = cfg.user;
      Group = cfg.group;
    };
  };
}
```
Add Reposilite to NixOS e.g. in `/etc/nixos/configuration.nix`:

```nix
{ config, pkgs, ... }:
{
  imports = [
    # ...
    ./reposilite.nix
  ];
  # ...
}
```

### Reposilite CLI

The configuration above adds Reposilite to the system path, which may be needed to configure 
Reposilite. For example:

```bash
systemctl stop reposilite.service
runuser -u reposilite -g reposilite -- reposilite --working-directory /var/lib/reposilite --port 8084
```

When started from a terminal, Reposilite's console can be used to add users and tokens.
