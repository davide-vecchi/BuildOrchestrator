@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

REM Define the version as a variable :
SET VERSION=2.0.0

SET MavenRepoFolder=C:\Users\Davide\.m2\repository

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
