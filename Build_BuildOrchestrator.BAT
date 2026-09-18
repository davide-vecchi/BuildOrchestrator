@ECHO OFF

REM =============================================================================
REM NOTE : This script must be located in the Maven module folder, that is, the
REM        folder that contains this module's pom.xml (and the sibling deploy
REM        script). The script changes to its own directory before doing anything,
REM        so do NOT move or copy this script out of the module folder.
REM =============================================================================

REM Set codepage to UTF-8 (CHCP 65001) :

CHCP 65001 >NUL

REM Enable Delayed Expansion :

SETLOCAL EnableDelayedExpansion

REM Ensure the execution starts on a clean ERRORLEVEL :

IF !ERRORLEVEL! NEQ 0 (
	ECHO.
	ECHO CRITICAL ERROR; not executing the script because ERRORLEVEL
	ECHO was already !ERRORLEVEL! instead of 0 when the script started.
	ECHO.
	PAUSE
	EXIT !ERRORLEVEL!
)

ECHO.
ECHO Build BuildOrchestrator module :

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

ECHO.
ECHO Module folder :
CD
ECHO.

PAUSE

CALL mvn clean install

IF !ERRORLEVEL! NEQ 0 (

	ECHO.
	ECHO CRITICAL ERROR; not executing the deployment script because ERRORLEVEL
	ECHO was !ERRORLEVEL! when the Maven build command returned.
	ECHO.
	PAUSE
	EXIT !ERRORLEVEL!
)

REM Determine the version from the pom.xml, to pass it to the deploy script :
FOR /F "usebackq delims=" %%v IN (`mvn -q help:evaluate "-Dexpression=project.version" -DforceStdout`) DO SET VERSION=%%v

CALL Deploy_BuildOrchestrator.BAT "%VERSION%"
