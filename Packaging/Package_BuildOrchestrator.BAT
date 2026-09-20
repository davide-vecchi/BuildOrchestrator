@ECHO OFF

REM =============================================================================
REM NOTE : Packages BuildOrchestrator for distribution.
REM
REM        It reads the following files :
REM
REM          - BuildOrchestrator.jar         (from this folder; the built
REM                                           executable JAR; overridable by
REM                                           the first argument)
REM          - BuildOrchestrator-Config.TXT  (from this folder)
REM          - Build List EXAMPLE.TXT        (from this folder)
REM          - BuildOrchestrator.BAT         (from the parent module folder)
REM          - BuildOrchestrator.sh          (from the parent module folder)
REM
REM        and produces a ZIP archive containing a top-level "BuildOrchestrator"
REM        folder with all of them, ready to be extracted into the user's chosen
REM        BuildOrchestrator installation folder.
REM
REM        Usage : Package_BuildOrchestrator.BAT [jar-path] [version]
REM          jar-path : the executable JAR to package (default : this folder's
REM                     BuildOrchestrator.jar)
REM          version  : optional; when given, the archive is named
REM                     "BuildOrchestrator-<version>.zip"
REM =============================================================================

SETLOCAL EnableDelayedExpansion

REM The executable JAR to package (default : this folder's BuildOrchestrator.jar) :
SET JAR_FILE=%~dp0BuildOrchestrator.jar
IF NOT "%~1"=="" SET JAR_FILE=%~1

REM Optional version, used in the archive name :
SET VERSION=%~2

REM The other input files (the launchers are in the parent module folder) :
SET CONFIG_FILE=%~dp0BuildOrchestrator-Config.TXT
SET LAUNCHER_BAT=%~dp0..\BuildOrchestrator.BAT
SET LAUNCHER_SH=%~dp0..\BuildOrchestrator.sh
SET BUILD_LIST=%~dp0Build List EXAMPLE.TXT

REM Name of the top-level folder inside the archive :
SET PACKAGE_FOLDER=BuildOrchestrator
SET STAGING_FOLDER=%~dp0%PACKAGE_FOLDER%

REM Output archive :
IF "!VERSION!"=="" (
	SET ARCHIVE_NAME=BuildOrchestrator.zip
) ELSE (
	SET ARCHIVE_NAME=BuildOrchestrator-!VERSION!.zip
)
SET ARCHIVE_FILE=%~dp0!ARCHIVE_NAME!

REM Ensure the required tools are available :
WHERE pwsh >NUL 2>NUL
IF !ERRORLEVEL! NEQ 0 (
	ECHO.
	ECHO CRITICAL ERROR; "pwsh" was not found on the PATH.
	ECHO.
	PAUSE
	EXIT /B 1
)

REM Ensure all the input files exist :
SET MISSING=0
CALL :Require "!JAR_FILE!"     "executable JAR"
CALL :Require "!CONFIG_FILE!"  "configuration file"
CALL :Require "!LAUNCHER_BAT!" "Windows launcher"
CALL :Require "!LAUNCHER_SH!"  "Unix launcher"
CALL :Require "!BUILD_LIST!"   "example Build List"

IF !MISSING! NEQ 0 (
	ECHO.
	ECHO CRITICAL ERROR; one or more input files are missing, see above.
	ECHO.
	PAUSE
	EXIT /B 1
)

REM Stage the files into the top-level folder :
IF EXIST "!STAGING_FOLDER!" RMDIR /S /Q "!STAGING_FOLDER!"
MKDIR "!STAGING_FOLDER!"

COPY /Y "!JAR_FILE!"     "!STAGING_FOLDER!\BuildOrchestrator.jar"        >NUL
COPY /Y "!CONFIG_FILE!"  "!STAGING_FOLDER!\BuildOrchestrator-Config.TXT" >NUL
COPY /Y "!LAUNCHER_BAT!" "!STAGING_FOLDER!\BuildOrchestrator.BAT"        >NUL
COPY /Y "!LAUNCHER_SH!"  "!STAGING_FOLDER!\BuildOrchestrator.sh"         >NUL
COPY /Y "!BUILD_LIST!"   "!STAGING_FOLDER!\Build List EXAMPLE.TXT"       >NUL

REM Create the archive :
IF EXIST "!ARCHIVE_FILE!" DEL /Q "!ARCHIVE_FILE!"
pwsh -NoProfile -Command "Compress-Archive -Path '%STAGING_FOLDER%' -DestinationPath '%ARCHIVE_FILE%' -Force"
IF !ERRORLEVEL! NEQ 0 (
	ECHO.
	ECHO CRITICAL ERROR; could not create the archive :
	ECHO !ARCHIVE_FILE!
	ECHO ERRORLEVEL was !ERRORLEVEL!.
	ECHO.
	PAUSE
	EXIT /B !ERRORLEVEL!
)

REM Remove the staging folder :
RMDIR /S /Q "!STAGING_FOLDER!"

ECHO.
ECHO Archive created :
ECHO   !ARCHIVE_FILE!
ECHO.
ECHO It contains the folder "!PACKAGE_FOLDER!" with :
ECHO   BuildOrchestrator.jar
ECHO   BuildOrchestrator-Config.TXT
ECHO   BuildOrchestrator.BAT
ECHO   BuildOrchestrator.sh
ECHO   Build List EXAMPLE.TXT
ECHO.
PAUSE
EXIT /B 0

REM -----------------------------------------------------------------------------
REM Require that a file exists : %1 = path, %2 = description
REM -----------------------------------------------------------------------------
:Require
IF NOT EXIST "%~1" (
	ECHO [FAIL] missing %~2 : %~1
	SET MISSING=1
)
EXIT /B 0
