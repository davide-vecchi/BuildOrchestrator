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
REM          - README.md                     (from the parent module folder)
REM          - LICENSE                       (from the parent module folder)
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

REM The application name, the base for the file names below :
SET APP_NAME=BuildOrchestrator

REM The file names (used both to locate the inputs and as the staged names) :
SET JAR_NAME=%APP_NAME%.jar
SET CONFIG_NAME=%APP_NAME%-Config.TXT
SET LAUNCHER_BAT_NAME=%APP_NAME%.BAT
SET LAUNCHER_SH_NAME=%APP_NAME%.sh
SET BUILD_LIST_NAME=Build List EXAMPLE.TXT
SET README_NAME=README.md
SET LICENSE_NAME=LICENSE

REM The executable JAR to package (default : this folder's %JAR_NAME%) :
SET JAR_FILE=%~dp0%JAR_NAME%
IF NOT "%~1"=="" SET JAR_FILE=%~1

REM The folder the packaged JAR comes from (shown in the summary) :
FOR %%F IN ("!JAR_FILE!") DO SET JAR_DIR=%%~dpF

REM Optional version, used in the archive name :
SET VERSION=%~2

REM The other input files (the launchers, README and LICENSE are in the parent module folder) :
SET CONFIG_FILE=%~dp0%CONFIG_NAME%
SET LAUNCHER_BAT=%~dp0..\%LAUNCHER_BAT_NAME%
SET LAUNCHER_SH=%~dp0..\%LAUNCHER_SH_NAME%
SET BUILD_LIST=%~dp0%BUILD_LIST_NAME%
SET README_FILE=%~dp0..\%README_NAME%
SET LICENSE_FILE=%~dp0..\%LICENSE_NAME%

REM Name of the top-level folder inside the archive :
SET PACKAGE_FOLDER=%APP_NAME%
SET STAGING_FOLDER=%~dp0%PACKAGE_FOLDER%

REM Output archive :
IF "!VERSION!"=="" (
	SET ARCHIVE_NAME=%APP_NAME%.zip
) ELSE (
	SET ARCHIVE_NAME=%APP_NAME%-!VERSION!.zip
)
SET ARCHIVE_FILE=%~dp0!ARCHIVE_NAME!

REM Locate the PowerShell executable (prefer pwsh, fall back to Windows PowerShell) :
SET PS_EXE=
WHERE pwsh >NUL 2>NUL
IF !ERRORLEVEL! EQU 0 (
	SET PS_EXE=pwsh
) ELSE (
	WHERE powershell >NUL 2>NUL
	IF !ERRORLEVEL! EQU 0 SET PS_EXE=powershell
)

IF "!PS_EXE!"=="" (
	ECHO.
	ECHO CRITICAL ERROR; PowerShell was not found on the PATH.
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
CALL :Require "!README_FILE!"  "README"
CALL :Require "!LICENSE_FILE!" "license"

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

COPY /Y "!JAR_FILE!"     "!STAGING_FOLDER!\!JAR_NAME!"          >NUL
COPY /Y "!CONFIG_FILE!"  "!STAGING_FOLDER!\!CONFIG_NAME!"       >NUL
COPY /Y "!LAUNCHER_BAT!" "!STAGING_FOLDER!\!LAUNCHER_BAT_NAME!" >NUL
COPY /Y "!LAUNCHER_SH!"  "!STAGING_FOLDER!\!LAUNCHER_SH_NAME!"  >NUL
COPY /Y "!BUILD_LIST!"   "!STAGING_FOLDER!\!BUILD_LIST_NAME!"   >NUL
COPY /Y "!README_FILE!"  "!STAGING_FOLDER!\!README_NAME!"       >NUL
COPY /Y "!LICENSE_FILE!" "!STAGING_FOLDER!\!LICENSE_NAME!"      >NUL

REM Create the archive :
IF EXIST "!ARCHIVE_FILE!" DEL /Q "!ARCHIVE_FILE!"
!PS_EXE! -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%STAGING_FOLDER%' -DestinationPath '%ARCHIVE_FILE%' -Force"
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
ECHO   !JAR_NAME!
ECHO     from "!JAR_DIR!"
ECHO   !CONFIG_NAME!
ECHO   !LAUNCHER_BAT_NAME!
ECHO   !LAUNCHER_SH_NAME!
ECHO   !BUILD_LIST_NAME!
ECHO   !README_NAME!
ECHO   !LICENSE_NAME!
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
