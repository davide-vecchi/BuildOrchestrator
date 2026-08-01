REM 20:51 2026-02-04

@ECHO OFF

CD /D "%~dp0"

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

@ECHO ON

CD /D "H:\Users\Davide\Davide\Sviluppo\REPOS\BuildTools\BuildOrchestrator\"

DEL /S /Q "Build*-log*.LOG.2*"

DEL /S /Q "*.DUMP"

DEL /S /Q "*.bak"


RMDIR /S /Q "test-output\"

PAUSE

CD /D "C:\BuildOrchestrator\"

DEL /S /Q "Build*-log*.LOG.2*"

DEL /S /Q "*.DUMP"

DEL /S /Q "*.bak"


PAUSE
