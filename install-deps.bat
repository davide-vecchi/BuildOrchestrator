@ECHO OFF
REM ============================================================================
REM install-deps.bat
REM
REM Launcher for install-deps.ps1 of BuildOrchestrator.
REM
REM Usage: Double-click this file, or run from Command Prompt.
REM ============================================================================

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-deps.ps1"
PAUSE
