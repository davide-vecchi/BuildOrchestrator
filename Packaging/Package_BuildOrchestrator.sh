#!/bin/bash

# =============================================================================
# NOTE : Packages BuildOrchestrator for distribution.
#
#        It reads the following files :
#          - BuildOrchestrator.jar         (from this folder; the built
#                                           executable JAR; overridable by
#                                           the first argument)
#          - BuildOrchestrator-Config.TXT  (from this folder)
#          - Build List EXAMPLE.TXT        (from this folder)
#          - BuildOrchestrator.BAT         (from the parent module folder)
#          - BuildOrchestrator.sh          (from the parent module folder)
#
#        and produces a ZIP archive containing a top-level "BuildOrchestrator"
#        folder with all of them, ready to be extracted into the user's chosen
#        BuildOrchestrator installation folder.
#
#        Usage : Package_BuildOrchestrator.sh [jar-path] [version]
#          jar-path : the executable JAR to package (default : this folder's
#                     BuildOrchestrator.jar)
#          version  : optional; when given, the archive is named
#                     "BuildOrchestrator-<version>.zip"
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# The executable JAR to package (default : this folder's BuildOrchestrator.jar) :
JAR_FILE="$SCRIPT_DIR/BuildOrchestrator.jar"
if [ -n "${1:-}" ]; then
    JAR_FILE="$1"
fi

# The folder the packaged JAR comes from (shown in the summary) :
JAR_DIR="$(dirname "$JAR_FILE")/"

# Optional version, used in the archive name :
VERSION="${2:-}"

# The other input files (the launchers are in the parent module folder) :
CONFIG_FILE="$SCRIPT_DIR/BuildOrchestrator-Config.TXT"
LAUNCHER_BAT="$SCRIPT_DIR/../BuildOrchestrator.BAT"
LAUNCHER_SH="$SCRIPT_DIR/../BuildOrchestrator.sh"
BUILD_LIST="$SCRIPT_DIR/Build List EXAMPLE.TXT"

# Name of the top-level folder inside the archive :
PACKAGE_FOLDER="BuildOrchestrator"
STAGING_FOLDER="$SCRIPT_DIR/$PACKAGE_FOLDER"

# Output archive :
if [ -z "$VERSION" ]; then
    ARCHIVE_NAME="BuildOrchestrator.zip"
else
    ARCHIVE_NAME="BuildOrchestrator-$VERSION.zip"
fi
ARCHIVE_FILE="$SCRIPT_DIR/$ARCHIVE_NAME"

# Ensure the required tool is available :
if ! command -v jar >/dev/null 2>&1; then
    echo ""
    echo "CRITICAL ERROR; \"jar\" was not found on the PATH."
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

# Ensure all the input files exist :
MISSING=0

require() {
    if [ ! -f "$1" ]; then
        echo "[FAIL] missing $2 : $1"
        MISSING=1
    fi
}

require "$JAR_FILE"     "executable JAR"
require "$CONFIG_FILE"  "configuration file"
require "$LAUNCHER_BAT" "Windows launcher"
require "$LAUNCHER_SH"  "Unix launcher"
require "$BUILD_LIST"   "example Build List"

if [ "$MISSING" -ne 0 ]; then
    echo ""
    echo "CRITICAL ERROR; one or more input files are missing, see above."
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

# Stage the files into the top-level folder :
rm -rf "$STAGING_FOLDER"
mkdir -p "$STAGING_FOLDER"

cp "$JAR_FILE"     "$STAGING_FOLDER/BuildOrchestrator.jar"
cp "$CONFIG_FILE"  "$STAGING_FOLDER/BuildOrchestrator-Config.TXT"
cp "$LAUNCHER_BAT" "$STAGING_FOLDER/BuildOrchestrator.BAT"
cp "$LAUNCHER_SH"  "$STAGING_FOLDER/BuildOrchestrator.sh"
cp "$BUILD_LIST"   "$STAGING_FOLDER/Build List EXAMPLE.TXT"

# Create the archive :
rm -f "$ARCHIVE_FILE"
jar cfM "$ARCHIVE_FILE" -C "$SCRIPT_DIR" "$PACKAGE_FOLDER"
if [ $? -ne 0 ]; then
    echo ""
    echo "CRITICAL ERROR; could not create the archive :"
    echo "$ARCHIVE_FILE"
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

# Remove the staging folder :
rm -rf "$STAGING_FOLDER"

echo ""
echo "Archive created :"
echo "  $ARCHIVE_FILE"
echo ""
echo "It contains the folder \"$PACKAGE_FOLDER\" with :"
echo "  BuildOrchestrator.jar"
echo "    from \"$JAR_DIR\""
echo "  BuildOrchestrator-Config.TXT"
echo "  BuildOrchestrator.BAT"
echo "  BuildOrchestrator.sh"
echo "  Build List EXAMPLE.TXT"
echo ""

read -r -p "Press Enter to exit..."
