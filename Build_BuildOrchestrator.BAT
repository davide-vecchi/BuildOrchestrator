
@ECHO OFF

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
