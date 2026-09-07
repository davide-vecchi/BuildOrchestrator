#!/bin/bash

# Define the version as a variable
VERSION="2.0.0"

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
