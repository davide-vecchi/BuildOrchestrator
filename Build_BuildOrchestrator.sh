#!/bin/bash

# =============================================================================
# NOTE : This script must be located in the Maven module folder, that is, the
#        folder that contains this module's pom.xml (and the sibling deploy
#        script). The script changes to its own directory before doing anything,
#        so do NOT move or copy this script out of the module folder.
# =============================================================================

# Ensure the execution starts on a clean exit code
startExitCode=$?

if [ $startExitCode -ne 0 ]; then
    echo ""
    echo "CRITICAL ERROR; not executing the script because exit code"
    echo "was already $startExitCode instead of 0 when the script started."
    echo ""
    read -r -p "Press Enter to exit..."
    exit $startExitCode
fi

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

echo ""
echo "Build BuildOrchestrator module :"

echo ""
echo "Module folder :"
pwd
echo ""

read -r -p "Press Enter to start Maven build..."

mvn clean install
mvnExitCode=$?

if [ $mvnExitCode -ne 0 ]; then
    echo ""
    echo "CRITICAL ERROR; not executing the deployment script because exit code"
    echo "was $mvnExitCode instead of 0 when the Maven build command returned."
    echo ""
    read -r -p "Press Enter to exit..."
    exit $mvnExitCode
fi

# Determine the version from the pom.xml, to pass it to the deploy script
VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)

./Deploy_BuildOrchestrator.sh "$VERSION"
