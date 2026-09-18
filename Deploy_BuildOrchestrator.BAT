@ECHO OFF

REM =============================================================================
REM NOTE : This script must be located in the Maven module folder, that is, the
REM        folder that contains this module's pom.xml (and its sibling build
REM        script). The script changes to its own directory before doing anything.
REM
REM        This script deploys the built BuildOrchestrator artifact (the
REM        "-jar-with-dependencies" jar) from the local Maven repo to an
REM        installation folder.
REM
REM        - The Maven coordinates (groupId / artifactId / version) are read from
REM          'pom.xml'.
REM        - The installation folder is read from the first line of
REM          'BO-Installation-folder.txt' (in this same folder).
REM =============================================================================

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

REM Enable Delayed Expansion :
SETLOCAL EnableDelayedExpansion

REM Define the install folder file name and the jar name :
SET INSTALL_FOLDER_FILE_NAME=BO-Installation-folder.txt

SET JAR_NAME=BuildOrchestrator.jar

SET MavenRepoFolder=%USERPROFILE%\.m2\repository

REM Change to the folder this script is located in (the module folder, containing pom.xml) :
CD /D "%~dp0"
IF !ERRORLEVEL! NEQ 0 (
	ECHO.
	ECHO CRITICAL ERROR; could not change to this script's folder :
	ECHO %~dp0
	ECHO ERRORLEVEL was !ERRORLEVEL!.
	ECHO.
	PAUSE
	EXIT /B !ERRORLEVEL!
)

REM -----------------------------------------------------------------------------
REM Read the install folder from the first line of 'BO-Installation-folder.txt' :
REM -----------------------------------------------------------------------------

SET INSTALL_FOLDER_FILE_PATH=%~dp0%INSTALL_FOLDER_FILE_NAME%

IF NOT EXIST "%INSTALL_FOLDER_FILE_PATH%" (
	ECHO.
	ECHO CRITICAL ERROR; the following file is missing :
	ECHO %INSTALL_FOLDER_FILE_PATH%
	ECHO It must contain, on its first line, the absolute path of the folder where the
	ECHO built and renamed %JAR_NAME% must be installed.
	ECHO.
	PAUSE
	EXIT /B 1
)

REM Read only the first line, so this works whatever line endings the file uses
REM (SET /P would read the whole file if it uses LF-only endings) :
SET INSTALL_FOLDER=
FOR /F "usebackq delims=" %%i IN (`pwsh -NoProfile -Command "('' + (Get-Content -LiteralPath '%INSTALL_FOLDER_FILE_PATH%' -TotalCount 1)).Trim()"`) DO SET INSTALL_FOLDER=%%i

IF "%INSTALL_FOLDER%"=="" (
	ECHO.
	ECHO CRITICAL ERROR; the first line of the following file is blank :
	ECHO %INSTALL_FOLDER_FILE_PATH%
	ECHO It must contain, on its first line, the absolute path of the folder where the
	ECHO built and renamed %JAR_NAME% must be installed.
	ECHO.
	PAUSE
	EXIT /B 1
)

REM Create the install folder if it does not exist yet :
SET INSTALL_FOLDER_CREATED=no

IF NOT EXIST "%INSTALL_FOLDER%\" (
	MKDIR "%INSTALL_FOLDER%"
	IF !ERRORLEVEL! NEQ 0 (
		ECHO.
		ECHO CRITICAL ERROR; could not create the install folder :
		ECHO %INSTALL_FOLDER%
		ECHO ERRORLEVEL was !ERRORLEVEL!.
		ECHO.
		PAUSE
		EXIT /B !ERRORLEVEL!
	)
	SET INSTALL_FOLDER_CREATED=yes
)

REM -----------------------------------------------------------------------------
REM Derive this module's Maven coordinates (groupId, artifactId) from its pom.xml :
REM -----------------------------------------------------------------------------
FOR /F "usebackq delims=" %%g IN (`mvn -q help:evaluate "-Dexpression=project.groupId" -DforceStdout`) DO SET GROUP_ID=%%g

FOR /F "usebackq delims=" %%a IN (`mvn -q help:evaluate "-Dexpression=project.artifactId" -DforceStdout`) DO SET ARTIFACT_ID=%%a

IF "!GROUP_ID!"=="" (
	ECHO.
	ECHO CRITICAL ERROR; could not read the Maven groupId/artifactId from pom.xml.
	ECHO.
	PAUSE
	EXIT /B 1
)

IF "!ARTIFACT_ID!"=="" (
	ECHO.
	ECHO CRITICAL ERROR; could not read the Maven groupId/artifactId from pom.xml.
	ECHO.
	PAUSE
	EXIT /B 1
)

REM The Maven local-repo path represents the groupId with dots replaced by slashes :
SET GROUP_ID_PATH=!GROUP_ID:.=/!

REM Determine the version to deploy : from the argument if given, otherwise ask the user,
REM offering the pom.xml version as the default.
SET VERSION=%~1

IF "%~1"=="" (
	FOR /F "usebackq delims=" %%v IN (`mvn -q help:evaluate "-Dexpression=project.version" -DforceStdout`) DO SET POM_VERSION=%%v
	SET /P VERSION=Version of BuildOrchestrator to deploy [ press Enter for the default !POM_VERSION! ]: 
	IF "!VERSION!"=="" SET VERSION=!POM_VERSION!
)

IF "!VERSION!"=="" (
	ECHO.
	ECHO CRITICAL ERROR; no version to deploy was provided.
	ECHO.
	PAUSE
	EXIT /B 1
)

REM Full paths used by the deploy, defined once so they stay consistent :
SET ARTIFACT_DIR=%MavenRepoFolder%\%GROUP_ID_PATH%\%ARTIFACT_ID%\%VERSION%
SET PLAIN_JAR=%ARTIFACT_DIR%\%ARTIFACT_ID%-%VERSION%.jar
SET SOURCE_JAR=%ARTIFACT_DIR%\%ARTIFACT_ID%-%VERSION%-jar-with-dependencies.jar
SET DEST_JAR=%INSTALL_FOLDER%\%JAR_NAME%

REM Back up the existing JAR before overwriting it (if there is one) :
IF EXIST "%DEST_JAR%" (
	ECHO.
	ECHO Now will make a backup copy of the JAR before overwriting it;
	ECHO.
	PAUSE

	pwsh -Command "Copy-Item '%DEST_JAR%' ('%DEST_JAR%-' + (Get-Date -Format 'yyyy-MM-dd_HH-mm-ss') + '.jar')"

	DIR "%DEST_JAR%*" /O-D
) ELSE (
	ECHO.
	ECHO No existing %JAR_NAME% to back up; skipping.
)

ECHO.
ECHO Will deploy version : %VERSION%
ECHO Source (Maven repo) :
ECHO   %SOURCE_JAR%
ECHO Destination (install folder) :
ECHO   %DEST_JAR%
ECHO.
PAUSE

@ECHO ON

IF EXIST "%PLAIN_JAR%" DEL "%PLAIN_JAR%"

MOVE "%SOURCE_JAR%" "%DEST_JAR%"

@ECHO OFF

ECHO.
ECHO Done. Moved :
ECHO   From : %SOURCE_JAR%
ECHO   To   : %DEST_JAR%
IF "%INSTALL_FOLDER_CREATED%"=="yes" (
	ECHO.
	ECHO Note : the install folder did not exist and was created :
	ECHO   %INSTALL_FOLDER%
)
ECHO.

PAUSE
