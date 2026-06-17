#!/bin/bash

# Ensure the execution starts on a clean exit code
if [ $? -ne 0 ]; then
    echo ""
    echo "CRITICAL ERROR; not executing the script because exit code"
    echo "was already $? instead of 0 when the script started."
    echo ""
    read -p "Press Enter to exit..."
    exit $?
fi

REPOSFolder="/Users/pino/Documents/Progetti software/REPOS"

SafetyPathC="/tmp"
mkdir -p "$SafetyPathC"
cd "$SafetyPathC" || exit

echo ""
echo "Build BuildOrchestrator module :"

cd "$REPOSFolder/BuildTools/BuildOrchestrator" || exit

echo ""
echo "Module folder :"
pwd
echo ""

read -p "Press Enter to start Maven build..."

mvn clean install

if [ $? -ne 0 ]; then
    echo ""
    echo "CRITICAL ERROR; not executing the deployment script because exit code"
    echo "was $? when the Maven build command returned."
    echo ""
    read -p "Press Enter to exit..."
    exit $?
fi

./Deploy_BuildOrchestrator.sh
