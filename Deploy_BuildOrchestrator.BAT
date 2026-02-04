
@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

SET MavenRepoFolder=C:\Users\Davide\.m2\repository


ECHO.
ECHO Now will move BuildOrchestrator artifact from Maven repo to BuildOrchestrator installation folder;
ECHO.
PAUSE

@ECHO ON

DEL %MavenRepoFolder%\DAccessori\BuildOrchestrator\1.0-SNAPSHOT\BuildOrchestrator-1.0-SNAPSHOT.jar

MOVE %MavenRepoFolder%\DAccessori\BuildOrchestrator\1.0-SNAPSHOT\BuildOrchestrator-1.0-SNAPSHOT-jar-with-dependencies.jar ^
     C:\BuildOrchestrator\BuildOrchestrator.jar

@ECHO OFF

PAUSE
