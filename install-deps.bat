@ECHO OFF
REM ============================================================================
REM install-deps.bat
REM
REM Launcher for install-deps.ps1 of BuildOrchestrator.
REM
REM Usage: Double-click this file, or run from Command Prompt.
REM ============================================================================

SETLOCAL

REM Locate the PowerShell executable (prefer pwsh, fall back to Windows PowerShell) :
SET PS_EXE=
WHERE pwsh >NUL 2>NUL
IF %ERRORLEVEL% EQU 0 SET PS_EXE=pwsh
IF NOT DEFINED PS_EXE WHERE powershell >NUL 2>NUL
IF NOT DEFINED PS_EXE IF %ERRORLEVEL% EQU 0 SET PS_EXE=powershell

IF NOT DEFINED PS_EXE (
	ECHO.
	ECHO CRITICAL ERROR; PowerShell was not found on the PATH.
	ECHO.
	PAUSE
	EXIT /B 1
)

%PS_EXE% -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-deps.ps1"
PAUSE
