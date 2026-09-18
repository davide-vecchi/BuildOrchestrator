@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

REM Define the install folder file name and the jar name :
SET INSTALL_FOLDER_FILE_NAME=BO-Installation-folder.txt

SET JAR_NAME=BuildOrchestrator.jar

REM Define the version as a variable :
SET VERSION=2.3.0-SNAPSHOT

SET MavenRepoFolder=C:\Users\Davide\.m2\repository

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

DEL %MavenRepoFolder%\DBuildTools\BuildOrchestrator\%VERSION%\BuildOrchestrator-%VERSION%.jar

MOVE %MavenRepoFolder%\DBuildTools\BuildOrchestrator\%VERSION%\BuildOrchestrator-%VERSION%-jar-with-dependencies.jar ^
     "%INSTALL_FOLDER%\%JAR_NAME%"

@ECHO OFF

PAUSE
