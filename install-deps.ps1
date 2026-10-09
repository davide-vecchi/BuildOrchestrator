# =============================================================================
# install-deps.ps1
#
# Installs the DLibs dependencies required by BuildOrchestrator from the
# Libs-JARs repository.
#
# This script checks for Libs-JARs in ../Libs-JARs/ by default. If not found,
# it prompts the user to either clone it, provide a different path, or abort.
#
# IMPORTANT: If you are on Windows and your PowerShell execution policy is set
# to Restricted (the default on many systems), this script cannot be run
# directly. Use the launcher script install-deps.bat instead, which automatically
# bypasses the execution policy for this script.
#
# If you run this script directly on Windows and your PowerShell execution policy
# is set to Restricted, you must either:
#   - Run it with: powershell -ExecutionPolicy Bypass -File install-deps.ps1
#   - Change your execution policy (not recommended for security reasons)
#
# Usage:
#   - From Windows GUI (recommended): double-click install-deps.bat
#   - From Windows PowerShell (if policy allows): .\install-deps.ps1
#   - From Windows GUI (if policy allows): right-click install-deps.ps1 and
#     choose "Run with PowerShell".
# =============================================================================

# =============================================================================
# Configuration
# =============================================================================
$DLIBS_DIR = "dlibs"
$LJ_REPO_URL = "https://github.com/davide-vecchi/Libs-JARs.git"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$POM_FILE = "$SCRIPT_DIR\pom.xml"

# Color definitions
function Write-Success { Write-Host $args[0] -ForegroundColor Green }
function Write-Warning { Write-Host $args[0] -ForegroundColor Yellow }
function Write-Error { Write-Host $args[0] -ForegroundColor Red }
function Write-Info { Write-Host $args[0] -ForegroundColor Cyan }
function Write-Normal { Write-Host $args[0] -ForegroundColor White }


# BuildOrchestrator (BO) version that this script is for
$BO_VERSION = "2.3.1-SNAPSHOT"

# Dependencies required by BuildOrchestrator (direct + transitive)
# Format: groupId/artifactId/version
$DEPS = @(
    "djavalibraries/dapplication/2.3.0",
    "djavalibraries/dutil/2.4.0-SNAPSHOT",
    "djavalibraries/dfile/2.4.0",
    "djavalibraries/dlog/2.3.0",
    "djavalibraries/duserinputoutput/2.3.0",
    "djavalibraries/dparam/2.2.2",
    "djavalibraries/dmaven/2.3.0",
    "djavalibraries/dtest/2.2.2",
    "djavalibraries/dsax/2.2.2",
    "djavalibraries/dxml/2.2.2",
    "javalibraries3rdparty/threadsafenumberformat/2.2.1"
)

Write-Normal ""
Write-Normal "=================================================================="
Write-Normal "  Installing dependencies for BuildOrchestrator version $BO_VERSION"
Write-Normal "=================================================================="
Write-Normal ""

# =============================================================================
# Drift guard
#
# The DEPS list and the BO_VERSION constant are hand-maintained. Before
# installing, warn (and wait for a key, so the message does not scroll away) if
# they no longer match pom.xml :
#   (a) BO_VERSION vs. the pom's project.version;
#   (b) the DLibs declared in the pom vs. the DEPS list.
# Note that (b) is intentionally partial: the transitive DEPS entries (such as
# dsax, dxml and threadsafenumberformat) are not declared in the pom, so they
# cannot be checked this way. The snapshot guard below has no such limitation:
# it reads snapshot-ness from the DEPS list itself.
# =============================================================================

# Pause so that a warning does not scroll away unnoticed :
function Pause-ForReview {
    Read-Host "Press Enter to continue" | Out-Null
}

$POM_VERSION = (@(& mvn -q help:evaluate -f $POM_FILE "-Dexpression=project.version" -DforceStdout 2>$null) | Where-Object { $_ -ne "" } | Select-Object -Last 1)
$POM_DEPS_RAW = (@(& mvn -q help:evaluate -f $POM_FILE "-Dexpression=project.dependencies" -DforceStdout 2>$null)) -join "`n"

# (a) The validated-for constant vs. the pom version :
if ($POM_VERSION -and $POM_VERSION -ne $BO_VERSION) {
    Write-Warning "WARNING: the DEPS list was validated for BuildOrchestrator $BO_VERSION, but the project is now $POM_VERSION. Please review the list and update BO_VERSION."
    Pause-ForReview
}

# (b) The DLibs declared in the pom vs. the DEPS list :
$depTokens = [regex]::Matches($POM_DEPS_RAW, '<(groupId|artifactId|version)>([^<]+)</\1>') | ForEach-Object { $_.Groups[2].Value }

for ($tokenIndex = 0; $tokenIndex + 2 -lt $depTokens.Count; $tokenIndex += 3) {
    $groupId = $depTokens[$tokenIndex]
    $artifactId = $depTokens[$tokenIndex + 1]
    $version = $depTokens[$tokenIndex + 2]

    if ($groupId -ne 'djavalibraries' -and $groupId -ne 'javalibraries3rdparty') {
        continue
    }

    $expectedEntry = "$groupId/$artifactId/$version"
    if ($DEPS -notcontains $expectedEntry) {
        Write-Warning "WARNING: the pom declares $expectedEntry, which is missing from (or at a different version in) the DEPS list. Please review the list."
        Pause-ForReview
    }
}

# (c) Snapshot guard: a snapshot DEPS list requires a snapshot consumer :
$depsHasSnapshot = $false
foreach ($dep in $DEPS) {
    if ($dep -like '*-SNAPSHOT') {
        $depsHasSnapshot = $true
        break
    }
}

if ($depsHasSnapshot -and $BO_VERSION -notlike '*-SNAPSHOT') {
    Write-Warning "WARNING: the DEPS list contains snapshots, but BuildOrchestrator $BO_VERSION is not a snapshot. A released consumer must not depend on snapshot DLibs."
    Pause-ForReview
}

# Determine the location of Libs-JARs
$LJ_PATH = "..\Libs-JARs"

# Check if LJ exists at the default location
if (Test-Path "$SCRIPT_DIR\$LJ_PATH\$DLIBS_DIR") {
    $LJ_ABSOLUTE_PATH = Resolve-Path "$SCRIPT_DIR\$LJ_PATH"
    Write-Success "Found Libs-JARs at: $LJ_ABSOLUTE_PATH"
} else {
    Write-Warning "Libs-JARs not found at default location: $SCRIPT_DIR\$LJ_PATH"
    Write-Normal ""
    Write-Normal "Options:"
    Write-Normal "  1. Press Enter to clone Libs-JARs into $LJ_PATH"
    Write-Normal "  2. Enter the path to an existing Libs-JARs folder"
    Write-Normal "  3. Enter / to terminate"
    Write-Normal ""
    $user_input = Read-Host "Your choice"

    if ($user_input -eq "/") {
        Write-Normal "Aborted."
        exit 0
    }

    if ([string]::IsNullOrWhiteSpace($user_input)) {
        # Clone the repository
        Write-Normal "Cloning Libs-JARs into $LJ_PATH..."
        git clone $LJ_REPO_URL "$SCRIPT_DIR\$LJ_PATH"
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to clone Libs-JARs."
            Read-Host "Press Enter to exit"
            exit 1
        }
        $LJ_ABSOLUTE_PATH = Resolve-Path "$SCRIPT_DIR\$LJ_PATH"
        Write-Success "Cloned Libs-JARs to: $LJ_ABSOLUTE_PATH"
    } else {
        # User provided a path
        if ($user_input -match "^[A-Za-z]:") {
            $LJ_PATH = $user_input
        } else {
            $LJ_PATH = "$SCRIPT_DIR\$user_input"
        }

        if (-not (Test-Path "$LJ_PATH\$DLIBS_DIR")) {
            Write-Error ("Error: " + $LJ_PATH + "\" + $DLIBS_DIR + " does not exist.")
            Write-Normal "Please ensure the path points to the root of the Libs-JARs repository."
            Read-Host "Press Enter to exit"
            exit 1
        }
        $LJ_ABSOLUTE_PATH = Resolve-Path $LJ_PATH
        Write-Success "Using Libs-JARs at: $LJ_ABSOLUTE_PATH"
    }
}

Write-Normal ""
Write-Normal "Installing dependencies..."

$successCount = 0
$failCount = 0

foreach ($dep in $DEPS) {
    # Split the entry into groupId, artifactId, version
    $parts = $dep -split '/'
    $group_id = $parts[0]
    $artifact_id = $parts[1]
    $version = $parts[2]

    $jarFile = "$LJ_ABSOLUTE_PATH\$DLIBS_DIR\$dep\$artifact_id-$version.jar"
    $pomFile = "$LJ_ABSOLUTE_PATH\$DLIBS_DIR\$dep\$artifact_id-$version.pom"

    if (-not (Test-Path $jarFile)) {
        Write-Warning ("WARNING: JAR not found for " + $dep + ". Skipping.")
        $failCount++
        continue
    }

    if (-not (Test-Path $pomFile)) {
        Write-Warning ("WARNING: POM not found for " + $dep + ". Skipping.")
        $failCount++
        continue
    }

    Write-Info ("Installing " + $artifact_id + " (" + $version + ")...")
    & mvn install:install-file -Dfile="$jarFile" -DpomFile="$pomFile"

    if ($LASTEXITCODE -eq 0) {
        Write-Success "  Success"
        $successCount++
    } else {
        Write-Error "  Failed"
        $failCount++
        Read-Host "Press Enter to exit"
        exit 1
    }
    Write-Normal ""
}

Write-Normal ""
Write-Success "Installation complete."
Write-Normal "  Successful: $successCount"
Write-Error "  Failed: $failCount"
Read-Host "Press Enter to exit"
