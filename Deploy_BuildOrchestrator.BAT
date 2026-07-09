@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

REM Define the version as a variable :
SET VERSION=2.1.0-SNAPSHOT

SET MavenRepoFolder=C:\Users\Davide\.m2\repository

ECHO.
ECHO Now will make a backup copy of the JAR before overwriting it;
ECHO.
PAUSE

pwsh -Command "Copy-Item C:\BuildOrchestrator\BuildOrchestrator.jar ('C:\BuildOrchestrator\BuildOrchestrator.jar-' + (Get-Date -Format 'yyyy-MM-dd_HH-mm-ss') + '.jar')"

DIR C:\BuildOrchestrator\BuildOrchestrator*.jar /O-D


ECHO.
ECHO.
ECHO Now will move BuildOrchestrator artifact from Maven repo to BuildOrchestrator installation folder;
ECHO.
PAUSE

@ECHO ON

DEL %MavenRepoFolder%\DBuildTools\BuildOrchestrator\%VERSION%\BuildOrchestrator-%VERSION%.jar

MOVE %MavenRepoFolder%\DBuildTools\BuildOrchestrator\%VERSION%\BuildOrchestrator-%VERSION%-jar-with-dependencies.jar ^
     C:\BuildOrchestrator\BuildOrchestrator.jar

@ECHO OFF

PAUSE
