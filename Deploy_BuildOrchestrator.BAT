@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

REM Define the version as a variable :
SET VERSION=2.3.0-SNAPSHOT

SET MavenRepoFolder=C:\Users\Davide\.m2\repository

REM Read the install folder from the first line of 'BO-Installation-folder.txt' :

IF NOT EXIST "%~dp0BO-Installation-folder.txt" (
	ECHO.
	ECHO CRITICAL ERROR; the following file is missing :
	ECHO %~dp0BO-Installation-folder.txt
	ECHO It must contain, on its first line, the absolute path of the folder where the
	ECHO built and renamed BuildOrchestrator.jar must be installed.
	ECHO.
	PAUSE
	EXIT /B 1
)

SET /P INSTALL_FOLDER=<"%~dp0BO-Installation-folder.txt"

IF "%INSTALL_FOLDER%"=="" (
	ECHO.
	ECHO CRITICAL ERROR; the first line of the following file is blank :
	ECHO %~dp0BO-Installation-folder.txt
	ECHO It must contain, on its first line, the absolute path of the folder where the
	ECHO built and renamed BuildOrchestrator.jar must be installed.
	ECHO.
	PAUSE
	EXIT /B 1
)

ECHO.
ECHO Now will make a backup copy of the JAR before overwriting it;
ECHO.
PAUSE

pwsh -Command "Copy-Item '%INSTALL_FOLDER%\BuildOrchestrator.jar' ('%INSTALL_FOLDER%\BuildOrchestrator.jar-' + (Get-Date -Format 'yyyy-MM-dd_HH-mm-ss') + '.jar')"

DIR "%INSTALL_FOLDER%\BuildOrchestrator*.jar" /O-D


ECHO.
ECHO.
ECHO Now will move BuildOrchestrator artifact from Maven repo to BuildOrchestrator installation folder;
ECHO.
PAUSE

@ECHO ON

DEL %MavenRepoFolder%\DBuildTools\BuildOrchestrator\%VERSION%\BuildOrchestrator-%VERSION%.jar

MOVE %MavenRepoFolder%\DBuildTools\BuildOrchestrator\%VERSION%\BuildOrchestrator-%VERSION%-jar-with-dependencies.jar ^
     "%INSTALL_FOLDER%\BuildOrchestrator.jar"

@ECHO OFF

PAUSE
