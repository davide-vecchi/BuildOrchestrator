
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

SET REPOSFolder=H:\Users\Davide\Davide\Sviluppo\REPOS


SET SafetyPathC=C:\TEMP
IF NOT EXIST "%SafetyPathC%" MKDIR "%SafetyPathC%"
CD /D "%SafetyPathC%"

ECHO.
ECHO Build BuildOrchestrator module :

CD /D "%REPOSFolder%\Accessori\BuildOrchestrator\"

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

CALL Deploy_BuildOrchestrator.BAT
