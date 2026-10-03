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

# Color definitions
function Write-Success { Write-Host $args[0] -ForegroundColor Green }
function Write-Warning { Write-Host $args[0] -ForegroundColor Yellow }
function Write-Error { Write-Host $args[0] -ForegroundColor Red }
function Write-Info { Write-Host $args[0] -ForegroundColor Cyan }
function Write-Normal { Write-Host $args[0] -ForegroundColor White }


# BuildOrchestrator version that this script is for
$BO_VERSION = "2.3.0-SNAPSHOT"

# Dependencies required by BuildOrchestrator (direct + transitive)
# Format: groupId/artifactId/version
$DEPS = @(
    "djavalibraries/dapplication/2.3.0",
    "djavalibraries/dutil/2.3.0",
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

# Determine the location of Libs-JARs
$LJ_PATH = "..\Libs-JARs"
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path

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
