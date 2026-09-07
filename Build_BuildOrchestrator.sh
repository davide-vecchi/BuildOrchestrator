#!/bin/bash

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

REPOSFolder="/Users/pino/Documents/Progetti software/REPOS"

SafetyPathC="/tmp"
mkdir -p "$SafetyPathC"
cd "$SafetyPathC" || exit

echo ""
echo "Build BuildOrchestrator module :"

moduleFolder="$REPOSFolder/BuildTools/BuildOrchestrator"

cd "$moduleFolder" 2>/dev/null || {
    echo ""
    echo "CRITICAL ERROR; the module folder was not found :"
    echo "$moduleFolder"
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
}

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

./Deploy_BuildOrchestrator.sh
