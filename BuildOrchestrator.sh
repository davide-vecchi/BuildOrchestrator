#!/bin/bash

# Beginning of BuildOrchestrator launch script.

# Change to the folder this script is located in, so that the JAR and the Build List
# are resolved relative to it, wherever the script is launched from :
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Set terminal to UTF-8 (macOS Terminal does this by default, but setting explicitly helps)
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

echo "SCRIPT DI LANCIO DEL PROGRAMMA BuildOrchestrator."
read -r -p "Press Enter to continue..."

# Note: On macOS, the JAR name is case-sensitive. Ensure BuildOrchestrator.jar exists.
# Examples :
# java -jar BuildOrchestrator.jar
# java -Dfile.encoding=UTF-8 -jar BuildOrchestrator.jar "/Users/pino/Documents/Progetti software/BuildTools/BuildOrchestrator/src/main/resources/Build List BuildOrchestrator ONLY.TXT"

java -jar BuildOrchestrator.jar

read -r -p "Press Enter to exit..."

# The comment block below is for reference. It will not be executed.
# ====================================================
# User guide:
# ----------------------------------------------------
# In order to start the BuildOrchestrator program:
# 1) Adjust the arguments of the command "java -jar BuildOrchestrator.jar ..." above in
#    this file, as described in the section below.
# 2) Save the file (with the same name or with any other name).
# 3) Make the script executable (chmod +x BuildOrchestrator.sh) and run it from Terminal.
# 4) The program will show messages to inform about the progress.
#    One of the last lines shown should read "... Executed successfully.".
# ----------------------------------------------------
# How to adjust the arguments of the command "java -jar BuildOrchestrator.jar ..." :
# The program must be started with either 0 or 1 arguments.
# If started with 1 argument, that argument must be the path to the Build List to use.
# ----------------------------------------------------
# All the arguments of the command "java -jar BuildOrchestrator.jar ..." above must be
# enclosed in double-quotes ("), but this is not mandatory when the argument
# value does not contain any space characters. It's recommended to take the
# habit of using the double-quotes for all arguments anyway, in order to avoid
# having to remember this.
# ----------------------------------------------------
# In the arguments that consist of a file name, it is not mandatory to specify
# the file path. If not specified, the current folder is assumed (that is the
# folder where this script file is).
# Specifying the file path is necessary if the file in subject is not in the
# same folder as this script file.
# ----------------------------------------------------
# ====================================================
