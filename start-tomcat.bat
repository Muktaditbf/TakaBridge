@echo off
REM ===========================================================================
REM  TakaBridge - start the server
REM  ---------------------------------------------------------------------
REM  Double click this file, then open http://localhost:8080/TakaBridge/
REM
REM  Keep the window that appears OPEN - that window IS the server.
REM  Close it (or press Ctrl+C inside it) to stop.
REM
REM  This starter is used instead of Tomcat's own startup.bat because it does
REM  not need a JAVA_HOME system variable. Nothing on your computer is changed.
REM ===========================================================================

setlocal

set "PROJECT=%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo.
    echo  ERROR: Java was not found.
    echo         Install a JDK from https://adoptium.net, open a NEW command
    echo         window, then try again.
    echo.
    pause
    exit /b 1
)

REM --- Find Tomcat, the same way build.bat does -------------------------------
if not "%CATALINA_HOME%"=="" if exist "%CATALINA_HOME%\bin\bootstrap.jar" goto :tomcat_ok
set "CATALINA_HOME=%PROJECT%tomcat11"
if exist "%CATALINA_HOME%\bin\bootstrap.jar" goto :tomcat_ok
set "CATALINA_HOME=%PROJECT%..\tomcat11"
if exist "%CATALINA_HOME%\bin\bootstrap.jar" goto :tomcat_ok
set "CATALINA_HOME=C:\tomcat11"
if exist "%CATALINA_HOME%\bin\bootstrap.jar" goto :tomcat_ok

echo.
echo  ERROR: Tomcat 11 was not found.
echo         Unzip it to C:\tomcat11, or put the tomcat11 folder next to
echo         this project folder.
echo.
pause
exit /b 1

:tomcat_ok
for %%I in ("%CATALINA_HOME%") do set "CATALINA_HOME=%%~fI"

if not exist "%CATALINA_HOME%\webapps\TakaBridge\WEB-INF\web.xml" (
    echo.
    echo  ERROR: TakaBridge has not been deployed yet.
    echo         Run build.bat first.
    echo.
    pause
    exit /b 1
)

echo.
echo  ===========================================================
echo   TakaBridge is starting...
echo.
echo   Open your browser at:   http://localhost:8080/TakaBridge/
echo.
echo   KEEP THIS WINDOW OPEN. Closing it stops the server.
echo  ===========================================================
echo.

cd /d "%CATALINA_HOME%\bin"

java -Dcatalina.home="%CATALINA_HOME%" ^
     -Dcatalina.base="%CATALINA_HOME%" ^
     -Djava.io.tmpdir="%CATALINA_HOME%\temp" ^
     -Djava.util.logging.config.file="%CATALINA_HOME%\conf\logging.properties" ^
     -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager ^
     -cp "%CATALINA_HOME%\bin\bootstrap.jar;%CATALINA_HOME%\bin\tomcat-juli.jar" ^
     org.apache.catalina.startup.Bootstrap start

echo.
echo  The server has stopped.
pause
