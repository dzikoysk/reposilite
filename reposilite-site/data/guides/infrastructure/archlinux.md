---
id: archlinux
title: Arch Linux
community: true
---

The Reposilite AUR package can be found at the following sources:
- [GitHub / Reposilite AUR](https://github.com/reposilite-playground/reposilite-aur) -
  releases and official source of package.
- [AUR / Reposilite Package](https://aur.archlinux.org/packages/reposilite) - to pull
  package using AUR helpers

There is a lot of different methods to install on Arch Linux, some of
them are more complicated than others, find a list of methods below:

- [From GitHub](/guide/archlinux#from-github)
- [From AUR](/guide/archlinux#from-aur)
  - [Using an AUR Helper](/guide/archlinux#using-an-aur-helper)
  - [Building Manually](/guide/archlinux#building-manually)

## From GitHub

AUR package builds are updated to the release section on GitHub, see
[Reposilite AUR Releases](https://github.com/reposilite-playground/reposilite-aur/releases).

All install, and updates, must be done manually, using the command
supplied within the release notes, please follow the guide on the latest
version of the release.

**Pros:**
- Officially supported
- Uses a trusted, and well known platform (GitHub)
- Distributed as a built package, fast to install

**Cons:**
- Manual installation, and manual updates
- Download speed is reliant on GitHub

## From AUR

There is two methods of installing from the AUR, please read both before
making your decision on what is best for you.

### Using an AUR Helper

[AUR Helpers](https://wiki.archlinux.org/title/AUR_helpers) are command
line tools which encapsulate the `pacman` package manager, and extends
its functionality to also be able to pull and build packages from the
AUR.

There are multiple AUR helpers to pick from, see
[Comparison Table](https://wiki.archlinux.org/title/AUR_helpers#Comparison_tables)
for more information. For this example we will use Paru.

1. Install [git](https://archlinux.org/packages/extra/x86_64/git/) and
[base-devel](https://archlinux.org/packages/core/any/base-devel/)
packages, these are used to build Arch Linux Packages.
2. Clone the Paru AUR package:

`git clone https://aur.archlinux.org/packages/paru`

3. Build, and install the package: (will require sudo)

`cd paru && makepkg -si`

4. Install Reposilite through Paru:

`paru -Syu reposilite`

This will clone, build and install Reposilite automatically.

Every time you need to update Reposilite, execute:

`paru -Syu`

**NOTE:**
- If you already have Paru installed, you can ignore steps 1 to 3.
- Paru will update itself automatically, you do not need to follow steps
  1 to 3 once you have Paru installed.

**Pros:**
- Easy to install AUR packages (not just Reposilite)
- Automatically updates Reposilite just like `pacman` updates official
  packages.

**Cons:**
- Packages must be built, this takes time and computational resources.

### Building Manually

If you do not wish to use an AUR helper, you can build the package
manually ever release.

1. Install [git](https://archlinux.org/packages/extra/x86_64/git/) and
[base-devel](https://archlinux.org/packages/core/any/base-devel/)
packages, these are used to build Arch Linux Packages.
2. Clone the reposilite AUR package:

`git clone https://aur.archlinux.org/packages/reposilite`

3. Build, and install the package: (will require sudo)

`cd reposilite && makepkg -si`

You will need to follow these steps every time you want to update the
package.

**Pros:**
- Doesn't rely on an AUR helper

**Cons:**
- Packages must be built manually every update, which can be tedious.
- Building packages takes time and computational resources.
