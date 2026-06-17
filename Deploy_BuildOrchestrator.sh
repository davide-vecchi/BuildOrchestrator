#!/bin/bash

MavenRepoFolder="$HOME/.m2/repository"

echo ""
echo "Now will move BuildOrchestrator artifact from Maven repo to BuildOrchestrator installation folder;"
echo ""
read -p "Press Enter to continue..."

set -x  # Echo commands (equivalent to @ECHO ON)

rm -f "$MavenRepoFolder/DBuildTools/BuildOrchestrator/2.0.0/BuildOrchestrator-2.0.0.jar"

mv "$MavenRepoFolder/DBuildTools/BuildOrchestrator/2.0.0/BuildOrchestrator-2.0.0-jar-with-dependencies.jar" \
   "/Users/pino/Documents/Progetti software/BuildTools/BuildOrchestrator/BuildOrchestrator.jar"

set +x  # Turn off command echoing

read -p "Press Enter to exit..."
