@ECHO OFF

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

REM Read the install folder from the first line of 'BO-Installation-folder.txt' :

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

SET /P INSTALL_FOLDER=<"%INSTALL_FOLDER_FILE_PATH%"

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

REM Derive this module's Maven coordinates (groupId, artifactId) from its pom.xml :
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

ECHO.
ECHO Now will make a backup copy of the JAR before overwriting it;
ECHO.
PAUSE

pwsh -Command "Copy-Item '%INSTALL_FOLDER%\%JAR_NAME%' ('%INSTALL_FOLDER%\%JAR_NAME%-' + (Get-Date -Format 'yyyy-MM-dd_HH-mm-ss') + '.jar')"

DIR "%INSTALL_FOLDER%\%JAR_NAME%*" /O-D


ECHO.
ECHO.
ECHO Now will move BuildOrchestrator artifact from Maven repo to BuildOrchestrator installation folder;
ECHO.
PAUSE

@ECHO ON

DEL %MavenRepoFolder%\%GROUP_ID_PATH%\%ARTIFACT_ID%\%VERSION%\BuildOrchestrator-%VERSION%.jar

MOVE %MavenRepoFolder%\%GROUP_ID_PATH%\%ARTIFACT_ID%\%VERSION%\BuildOrchestrator-%VERSION%-jar-with-dependencies.jar ^
     "%INSTALL_FOLDER%\%JAR_NAME%"

@ECHO OFF

PAUSE
