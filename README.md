# BuildOrchestrator

A Maven build orchestrator for building Java projects. It reads a **Build List** file that describes which modules to build and how, then runs each module's build command in sequence, optionally moving the produced artifact to a destination folder. It is meant to replace hand-written Batch / Bash Maven build scripts.

## Overview

`BuildOrchestrator` is a Java console application. Given a Build List file, it:

- runs the list's *Initialization* commands first,
- then, for each listed *module*, executes that module's build command (typically `mvn clean install`),
- optionally moves each module's built artifact to a configured destination folder.

Builds run with a configurable timeout, and their output can be captured (and optionally still shown to the user). On Windows it can also run the commands through a temporary `.BAT` script when the build requires preliminary shell-environment changes that must persist.

The application writes its messages to the console and to log files.

## Requirements

- **JDK 21** — the project is compiled for Java release 21.
- **Apache Maven** — `mvn` must be on `PATH`.
- **Bash** (Linux/macOS) for the `.sh` scripts, or **Windows** for the `.BAT` scripts.

## Dev env setup

1. Clone this repository.
2. Make the scripts executable (git does not preserve the executable bit):

   ```bash
   chmod +x Build_BuildOrchestrator.sh Deploy_BuildOrchestrator.sh BuildOrchestrator.sh Packaging/Package_BuildOrchestrator.sh
   ```

3. Tell the deploy step where to install the built jar. The folder path is read from the first line of `BO-Installation-folder.txt` (in this folder, git-ignored). A template is provided as `BO-Installation-folder.example.txt`:

   ```bash
   cp BO-Installation-folder.example.txt BO-Installation-folder.txt
   # then edit the first line of BO-Installation-folder.txt to the real path
   ```

   If the folder does not exist yet, the deploy script creates it.

The `mvn clean install` command downloads the project's dependencies (including the `djavalibraries:*` modules it depends on), so no further setup is normally required for the first build.

## Building and deploying

Run, from this module folder:

```bash
./Build_BuildOrchestrator.sh
```

This:
1. runs `mvn clean install` on this module,
2. reads the produced version from `pom.xml`,
3. runs `Deploy_BuildOrchestrator.sh`, which moves the built `...-jar-with-dependencies.jar` from the local Maven repo into the install folder (as `BuildOrchestrator.jar`).

## Running the application

```bash
./BuildOrchestrator.sh [path-to-a-Build-List-file]
```

- With **0 arguments**, the program asks you for the path of the Build List file to use.
- With **1 argument**, that argument is the path of the Build List file to use.

Progress is printed while it runs; the last lines should show **"... Executed successfully."**.

## The Build List format

A Build List file is made of sections introduced by headers in square brackets:

- **`[Initialization]`** — commands to run before any module is built (e.g. to back up an existing jar). One command per line.
- **`[Options]`** — optional flags, currently `NoPause` (do not pause between builds).
- **`[Modules]`** — the modules to build. Each module is a block of 2 or 3 lines:
  1. the filesystem path of the module's folder (where its `pom.xml` is),
  2. the build command to run for it (with any arguments),
  3. *optional* — the destination folder where the module's built artifact must be moved (omit to not move it).

Blocks are separated by at least one blank line. Lines starting with `#` are comments.

See `src/main/resources/Build List BuildOrchestrator.TXT` for a working example (it builds BuildOrchestrator itself).

## Scripts in this repository

| Script | Purpose | Platform |
|--------|---------|----------|
| `BuildOrchestrator.sh` / `.BAT` | Launch the application (`java -jar BuildOrchestrator.jar ...`) | Linux/macOS / Windows |
| `Build_BuildOrchestrator.sh` / `.BAT` | Build this module (`mvn clean install`) then deploy it | Linux/macOS / Windows |
| `Deploy_BuildOrchestrator.sh` / `.BAT` | Move the built jar from the Maven repo to the install folder | Linux/macOS / Windows |
| `Packaging/Package_BuildOrchestrator.sh` / `.BAT` | Bundle the built jar and the config/launcher files into a distributable ZIP | Linux/macOS / Windows |

The `.sh` scripts are location-relative: they operate on the folder they are located in, so the repository can live anywhere on disk.
The only machine-specific value is the install folder, configured through `BO-Installation-folder.txt`.
