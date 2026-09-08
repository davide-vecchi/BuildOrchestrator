#!/bin/bash

# =============================================================================
# NOTE : This script must be located in the Maven module folder, that is, the
#        folder that contains this module's pom.xml (and its sibling build
#        script). The script changes to its own directory before doing anything.
#
#        This script deploys the built BuildOrchestrator artifact (the
#        "-jar-with-dependencies" jar) from the local Maven repo to an
#        installation folder.
#
#        - The Maven coordinates (groupId / artifactId / version) are read from
#          'pom.xml'.
#        - The installation folder is read from the first line of
#          'BO-Installation-folder.txt' (in this same folder).
# =============================================================================

# Change to the folder this script is located in (the module folder, containing pom.xml)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR" 2>/dev/null || {
    echo ""
    echo "CRITICAL ERROR; could not change to this script's folder :"
    echo "$SCRIPT_DIR"
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
}

# -----------------------------------------------------------------------------
# Read the install folder from 'BO-Installation-folder.txt'.
# -----------------------------------------------------------------------------
INSTALL_FOLDER_FILE="$SCRIPT_DIR/BO-Installation-folder.txt"

if [ ! -f "$INSTALL_FOLDER_FILE" ]; then
    echo ""
    echo "CRITICAL ERROR; the following file is missing :"
    echo "$INSTALL_FOLDER_FILE"
    echo "It must contain, on its first line, the absolute path of the folder where the"
    echo "built and renamed BuildOrchestrator.jar must be installed."
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

INSTALL_FOLDER=$(head -n 1 "$INSTALL_FOLDER_FILE" | tr -d '\r' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')

if [ -z "$INSTALL_FOLDER" ]; then
    echo ""
    echo "CRITICAL ERROR; the first line of the following file is blank :"
    echo "$INSTALL_FOLDER_FILE"
    echo "It must contain, on its first line, the absolute path of the folder where the"
    echo "built and renamed BuildOrchestrator.jar must be installed."
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

if [ ! -d "$INSTALL_FOLDER" ]; then
    echo ""
    echo "CRITICAL ERROR; the install folder does not exist :"
    echo "$INSTALL_FOLDER"
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

# -----------------------------------------------------------------------------
# Derive this module's Maven coordinates (groupId, artifactId) from its pom.xml.
# -----------------------------------------------------------------------------
GROUP_ID=$(mvn -q help:evaluate -Dexpression=project.groupId -DforceStdout)

ARTIFACT_ID=$(mvn -q help:evaluate -Dexpression=project.artifactId -DforceStdout)

if [ -z "$GROUP_ID" ] || [ -z "$ARTIFACT_ID" ]; then
    echo ""
    echo "CRITICAL ERROR; could not read the Maven groupId/artifactId from pom.xml."
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

# The Maven local-repo path represents the groupId with dots replaced by slashes.
GROUP_ID_PATH=$(printf '%s' "$GROUP_ID" | tr '.' '/')

# Determine the version to deploy. If it was passed as an argument, use it;
# otherwise ask the user, offering the pom.xml version as the default.
if [ -n "${1:-}" ]; then
    VERSION="$1"
else
    POM_VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)
    read -r -p "Version of BuildOrchestrator to deploy [ press Enter for the default $POM_VERSION ]: " answer
    VERSION="${answer:-$POM_VERSION}"
fi

if [ -z "$VERSION" ]; then
    echo ""
    echo "CRITICAL ERROR; no version to deploy was provided."
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

MavenRepoFolder="$HOME/.m2/repository"

# Full paths used by the deploy, defined once so they stay consistent.
ARTIFACT_DIR="$MavenRepoFolder/$GROUP_ID_PATH/$ARTIFACT_ID/$VERSION"
PLAIN_JAR="$ARTIFACT_DIR/$ARTIFACT_ID-$VERSION.jar"
SOURCE_JAR="$ARTIFACT_DIR/$ARTIFACT_ID-$VERSION-jar-with-dependencies.jar"
DEST_JAR="$INSTALL_FOLDER/BuildOrchestrator.jar"

echo ""
echo "Will deploy version : $VERSION"
echo "Source (Maven repo) :"
echo "  $SOURCE_JAR"
echo "Destination (install folder) :"
echo "  $DEST_JAR"
echo ""
read -r -p "Press Enter to continue..."

set -x  # Echo commands (equivalent to @ECHO ON)

rm -f "$PLAIN_JAR"

mv "$SOURCE_JAR" "$DEST_JAR"

set +x  # Turn off command echoing

echo ""
echo "Done. Moved :"
echo "  From : $SOURCE_JAR"
echo "  To   : $DEST_JAR"
echo ""

read -r -p "Press Enter to exit..."
