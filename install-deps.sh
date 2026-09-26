#!/bin/bash

# =============================================================================
# install-deps.sh
#
# Installs the DLibs dependencies required by BuildOrchestrator from the
# Libs-JARs repository.
#
# This script checks for Libs-JARs in ../Libs-JARs/ by default. If not found,
# it prompts the user to either clone it, provide a different path, or abort.
#
# This script is the Unix counterpart to install-deps.ps1. It contains the
# installation logic directly, without needing a launcher, because Unix-like
# systems do not have a PowerShell execution policy.
#
# Usage (from any terminal):
#   - Linux / macOS / Git Bash on Windows: ./install-deps.sh
# =============================================================================

set -e

# =============================================================================
# Configuration
# =============================================================================
DLIBS_DIR="dlibs"
LJ_REPO_URL="https://github.com/davide-vecchi/Libs-JARs.git"

# Color definitions
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# BuildOrchestrator version that this script is for
BO_VERSION="2.3.0-SNAPSHOT"

# List of DLibs required by BuildOrchestrator (direct + transitive)
# Format: groupId/artifactId/version
declare -a DEPS=(
    "djavalibraries/dapplication/2.3.0"
    "djavalibraries/dutil/2.3.0"
    "djavalibraries/dlog/2.3.0"
    "djavalibraries/duserinputoutput/2.3.0"
    "djavalibraries/dfile/2.4.0"
    "djavalibraries/dparam/2.2.2"
    "djavalibraries/dmaven/2.3.0"
    "djavalibraries/dtest/2.2.2"
    "djavalibraries/dsax/2.2.2"
    "djavalibraries/dxml/2.2.2"
    "javalibraries3rdparty/threadsafenumberformat/2.2.1"
)

echo ""
echo "========================================"
echo "  Installing dependencies for BuildOrchestrator version $BO_VERSION"
echo "========================================"
echo ""

# Determine the location of Libs-JARs
LJ_PATH="../Libs-JARs"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Check if LJ exists at the default location
if [ -d "$SCRIPT_DIR/$LJ_PATH/$DLIBS_DIR" ]; then
    LJ_ABSOLUTE_PATH="$(cd "$SCRIPT_DIR/$LJ_PATH" && pwd)"
    echo -e "${GREEN}Found Libs-JARs at: $LJ_ABSOLUTE_PATH${NC}"
else
    echo -e "${YELLOW}Libs-JARs not found at default location: $SCRIPT_DIR/$LJ_PATH${NC}"
    echo ""
    echo "Options:"
    echo "  1. Press Enter to clone Libs-JARs into $LJ_PATH"
    echo "  2. Enter the path to an existing Libs-JARs folder"
    echo "  3. Enter / to terminate"
    echo ""
    read -r -p "Your choice: " user_input

    if [[ "$user_input" == "/" ]]; then
        echo "Aborted."
        exit 0
    fi

    if [[ -z "$user_input" ]]; then
        # Clone the repository
        echo "Cloning Libs-JARs into $LJ_PATH..."
        git clone "$LJ_REPO_URL" "$SCRIPT_DIR/$LJ_PATH"
        if [ $? -ne 0 ]; then
            echo -e "${RED}Failed to clone Libs-JARs.${NC}"
            exit 1
        fi
        LJ_ABSOLUTE_PATH="$(cd "$SCRIPT_DIR/$LJ_PATH" && pwd)"
        echo -e "${GREEN}Cloned Libs-JARs to: $LJ_ABSOLUTE_PATH${NC}"
    else
        # User provided a path
        if [[ "$user_input" = /* ]]; then
            LJ_PATH="$user_input"
        else
            LJ_PATH="$SCRIPT_DIR/$user_input"
        fi

        if [ ! -d "$LJ_PATH/$DLIBS_DIR" ]; then
            echo -e "${RED}Error: $LJ_PATH/$DLIBS_DIR does not exist.${NC}"
            echo "Please ensure the path points to the root of the Libs-JARs repository."
            exit 1
        fi
        LJ_ABSOLUTE_PATH="$(cd "$LJ_PATH" && pwd)"
        echo -e "${GREEN}Using Libs-JARs at: $LJ_ABSOLUTE_PATH${NC}"
    fi
fi

echo ""
echo "Installing dependencies..."

for dep in "${DEPS[@]}"; do
    # Split the entry into groupId, artifactId, version
    IFS='/' read -r group_id artifact_id version <<< "$dep"

    jar_file="$LJ_ABSOLUTE_PATH/$DLIBS_DIR/$dep/$artifact_id-$version.jar"
    pom_file="$LJ_ABSOLUTE_PATH/$DLIBS_DIR/$dep/$artifact_id-$version.pom"

    if [ ! -f "$jar_file" ]; then
        echo -e "${YELLOW}WARNING: JAR not found for $dep. Skipping.${NC}"
        continue
    fi

    if [ ! -f "$pom_file" ]; then
        echo -e "${YELLOW}WARNING: POM not found for $dep. Skipping.${NC}"
        continue
    fi

    echo -e "Installing ${CYAN}$artifact_id${NC} ($version)..."
    if mvn install:install-file -Dfile="$jar_file" -DpomFile="$pom_file"; then
        echo -e "${GREEN}  Success${NC}"
    else
        echo -e "${RED}  Failed${NC}"
        exit 1
    fi
    echo ""
done

echo -e "${GREEN}Installation complete.${NC}"
