# BuildOrchestrator

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)

A Maven build orchestrator for building Java projects. It reads a **Build List** file that describes which modules to build and how, then runs each module's build command in sequence, optionally moving the produced artifact to a destination folder. It is meant to replace hand-written Batch / Bash Maven build scripts.

If you just want to run it without building from source, see the [Quick Start](#quick-start-using-the-distributable-zip).

## Overview

`BuildOrchestrator` is a Java console application. Given a Build List file, it:

- runs the list's *Initialization* commands first,
- then, for each listed *module*, executes that module's build command (typically `mvn clean install`),
- optionally moves each module's built artifact to a configured destination folder.

Builds run with a configurable timeout, and their output can be captured (and optionally still shown to the user). On Windows it can also run the commands through a temporary `.BAT` script when the build requires preliminary shell-environment changes that must persist.

The application writes its messages to the console and to log files.

## Quick Start (using the distributable ZIP)

If you just want to use BuildOrchestrator without building from source:

1. Download the distributable ZIP from the [Releases](https://github.com/davide-vecchi/BuildOrchestrator/releases) page and unzip it into a convenient folder (e.g. `C:\BuildOrchestrator\`).

2. Run `BuildOrchestrator.BAT` (Windows) or `BuildOrchestrator.sh` (Linux/macOS).

   The ZIP contains the executable jar, the launcher scripts, an example configuration file (`BuildOrchestrator-Config.TXT`), an example Build List (`Build List EXAMPLE.TXT`), the `README.md` and the `LICENSE`. The configuration file must be kept next to the jar (see [Configuration](#configuration)); Java 21 or higher is required.

## Prerequisites (for building from source)

- Java 21 or higher installed and configured.
- Maven 3.6 or higher (tested with 3.9).
- Git (to clone the required repositories).
- Bash (Linux/macOS) for the `.sh` scripts, or Windows for the `.BAT` scripts.
- TestNG is used to run the tests (Maven downloads it automatically).

## Dev env setup

1. Clone this repository.
2. Make the scripts executable (git tracks the executable bit, but a Windows checkout does not apply it):

   ```bash
   chmod +x Build_BuildOrchestrator.sh Deploy_BuildOrchestrator.sh BuildOrchestrator.sh Packaging/Package_BuildOrchestrator.sh install-deps.sh
   ```

3. Tell the deploy step where to install the built jar. The folder path is read from the first line of `BO-Installation-folder.txt` (in this folder, git-ignored). A template is provided as `BO-Installation-folder.example.txt`:

   ```bash
   cp BO-Installation-folder.example.txt BO-Installation-folder.txt
   # then edit the first line of BO-Installation-folder.txt to the real path
   ```

   If the folder does not exist yet, the deploy script creates it.

4. Install the DLibs dependencies — see [Installing the required libraries (DLibs)](#installing-the-required-libraries-dlibs).

## Installing the required libraries (DLibs)

*This section is only needed if you are building from source.*

`BuildOrchestrator` depends on several libraries (`DLibs`) that are distributed as pre-compiled JARs in
the [Libs-JARs](https://github.com/davide-vecchi/Libs-JARs) repository. The steps below explain how to install these
libraries in your local Maven repo.

Alternatively, if you have access to the GitHub Packages repository declared in `pom.xml` (and to the repositories
that publish these libraries), you can configure your credentials for it and let `mvn clean install` resolve the DLibs
directly, skipping the steps below.

**Step 1: Clone the `Libs-JARs` repository**

```bash
git clone https://github.com/davide-vecchi/Libs-JARs.git ../Libs-JARs
```

**Step 2: Install the DLibs libraries**

From the `Libs-JARs` folder, run the installation script:

```bash
cd ../Libs-JARs
```

```bash
./install-all-dlibs.sh        # Linux, macOS, or Git Bash on Windows
```

or

```bash
install-all-dlibs.bat         # Windows Command Prompt or PowerShell
```

Alternatively, you can go inside the `BuildOrchestrator` folder and run the provided `install-deps.sh` or
`install-deps.bat` script to install only the libraries needed by this project.

Either way, the script will install the JARs into your local Maven repository (`~/.m2/repository`).

When you build the project, e.g. using

```bash
mvn clean install
```

, Maven will resolve these dependencies from your local repository.

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

Progress is printed while it runs; the last lines should show **"... executed successfully in <N> ms."**.

### Configuration

The program reads its settings from a configuration file named `<jar-name-without-extension>-Config.TXT` and located next to the JAR (for the shipped JAR: `BuildOrchestrator-Config.TXT`); the file must exist. All of its parameters are optional and fall back to defaults, and they are documented, with their defaults and examples, inside the file itself: `BuildListFile`, `MavenFolder`, `MavenRepoFolder`, `CommandTimeoutMs`, `CaptureBuildOutput`, `ShowCapturedOutput` and the `RunInScriptIfWinN` list. A copy is kept in `src/main/resources/` and shipped as `Packaging/BuildOrchestrator-Config.TXT`.

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
| `install-deps.sh` / `.ps1` / `.bat` | Install the DLibs dependencies from `Libs-JARs` into the local Maven repo | Linux/macOS / Windows |

The `.sh` scripts are location-relative: they operate on the folder they are located in, so the repository can live anywhere on disk.

## Project Status

- Active and in use.

## Changelog

See the [CHANGELOG](CHANGELOG.md) for the list of changes.

## Contributing

See the [CONTRIBUTING](CONTRIBUTING.md) page.

## Contacts

You can reach me in two ways:

- **Open an issue:** For bug reports, feature requests, or general questions,
  please [open a new issue](https://github.com/davide-vecchi/BuildOrchestrator/issues/new) on GitHub.
- **Mention me:** If you need to communicate with me, mention my username (`@davide-vecchi`) in a comment.
  I will be notified.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
