#!/bin/bash

# Default version of BuildOrchestrator to deploy, used as the fallback when prompting the user
VERSION="2.3.0-SNAPSHOT"

# If a version was passed as an argument, use it; otherwise prompt the user
if [ -n "${1:-}" ]; then
    VERSION="$1"
else
    read -r -p "Version of BuildOrchestrator to deploy [ press Enter for the default $VERSION ]: " answer
    VERSION="${answer:-$VERSION}"
fi

MavenRepoFolder="$HOME/.m2/repository"

echo ""
echo "Now will move BuildOrchestrator artifact from Maven repo to BuildOrchestrator installation folder;"
echo ""
read -r -p "Press Enter to continue..."

set -x  # Echo commands (equivalent to @ECHO ON)

rm -f "$MavenRepoFolder/DBuildTools/BuildOrchestrator/$VERSION/BuildOrchestrator-$VERSION.jar"

mv "$MavenRepoFolder/DBuildTools/BuildOrchestrator/$VERSION/BuildOrchestrator-$VERSION-jar-with-dependencies.jar" \
   "/Users/pino/Documents/Progetti software/BuildTools/BuildOrchestrator/BuildOrchestrator.jar"

set +x  # Turn off command echoing

read -r -p "Press Enter to exit..."
