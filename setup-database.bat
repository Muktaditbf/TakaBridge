@echo off
REM ===========================================================================
REM  TakaBridge - create the database
REM  ---------------------------------------------------------------------
REM  Double click this file ONCE, after Oracle XE is installed.
REM
REM  It will ask for a password. Type the password you set while installing
REM  Oracle XE (the one for SYS / SYSTEM), then press Enter. Nothing appears
REM  on screen while you type - that is normal.
REM ===========================================================================

setlocal

set "PROJECT=%~dp0"
set "SQLPLUS=sqlplus"

REM --- Find SQL*Plus. It is normally on the PATH after installing Oracle XE,
REM     but a command window opened before the install will not have it yet.
where sqlplus >nul 2>nul
if errorlevel 1 (
    if exist "C:\app\%USERNAME%\product\21c\dbhomeXE\bin\sqlplus.exe" (
        set "SQLPLUS=C:\app\%USERNAME%\product\21c\dbhomeXE\bin\sqlplus.exe"
    ) else (
        echo.
        echo  ERROR: SQL*Plus was not found.
        echo         Is Oracle XE installed? If you just installed it, restart
        echo         the computer and run this file again.
        echo.
        pause
        exit /b 1
    )
)

echo.
echo  ===========================================================
echo   TakaBridge - creating the database
echo.
echo   When it asks for a password, type the one you set while
echo   installing Oracle XE. Nothing shows while you type.
echo  ===========================================================
echo.

"%SQLPLUS%" system@localhost:1521/XE "@%PROJECT%schema.sql"

echo.
echo  ===========================================================
echo   If the number above is 23, the database is ready.
echo.
echo   Next: run build.bat, then start-tomcat.bat
echo  ===========================================================
echo.
pause
