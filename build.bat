@echo off
REM ===========================================================================
REM  TakaBridge - build and deploy
REM  ---------------------------------------------------------------------
REM  Compiles every Java class, copies the web pages next to them, and puts
REM  the finished application inside Tomcat.
REM
REM  Just double click this file, or run it from a command prompt.
REM ===========================================================================

setlocal enabledelayedexpansion

set "PROJECT=%~dp0"
set "BUILD=%PROJECT%build"
set "WEBAPP=%PROJECT%src\main\webapp"
set "LIB=%WEBAPP%\WEB-INF\lib"
set "SRCLIST=%TEMP%\takabridge-sources.txt"

echo.
echo  TakaBridge
echo  ----------

REM --- Check that Java is available -------------------------------------------
where javac >nul 2>nul
if errorlevel 1 (
    echo.
    echo  ERROR: javac was not found.
    echo         Install a JDK from https://adoptium.net, open a NEW command
    echo         window, then try again.
    echo.
    pause
    exit /b 1
)

REM --- Find Tomcat ------------------------------------------------------------
REM  Looked for in this order, so the project can be copied to another
REM  computer without editing anything:
REM     1. the CATALINA_HOME variable, if it is already set
REM     2. a tomcat11 folder inside this project folder
REM     3. a tomcat11 folder next to this project folder
REM     4. C:\tomcat11
if not "%CATALINA_HOME%"=="" if exist "%CATALINA_HOME%\lib\servlet-api.jar" goto :tomcat_ok
set "CATALINA_HOME=%PROJECT%tomcat11"
if exist "%CATALINA_HOME%\lib\servlet-api.jar" goto :tomcat_ok
set "CATALINA_HOME=%PROJECT%..\tomcat11"
if exist "%CATALINA_HOME%\lib\servlet-api.jar" goto :tomcat_ok
set "CATALINA_HOME=C:\tomcat11"
if exist "%CATALINA_HOME%\lib\servlet-api.jar" goto :tomcat_ok

echo.
echo  ERROR: Tomcat 11 was not found.
echo         Unzip it to C:\tomcat11, or put the tomcat11 folder next to
echo         this project folder.
echo.
pause
exit /b 1

:tomcat_ok
for %%I in ("%CATALINA_HOME%") do set "CATALINA_HOME=%%~fI"
echo  Tomcat: %CATALINA_HOME%

REM --- Check the Oracle driver ------------------------------------------------
if not exist "%LIB%\ojdbc17.jar" (
    echo.
    echo  ERROR: ojdbc17.jar is missing.
    echo         Copy it into: %LIB%
    echo.
    pause
    exit /b 1
)

REM --- Clean -------------------------------------------------------------------
if exist "%BUILD%" rmdir /s /q "%BUILD%"
mkdir "%BUILD%\WEB-INF\classes"

REM --- Copy the pages, the stylesheet, web.xml and the driver ------------------
xcopy /e /i /y /q "%WEBAPP%\*" "%BUILD%\" >nul

REM --- List the sources -------------------------------------------------------
REM  Each path is quoted and written with forward slashes, so a folder name
REM  containing a space (like "New folder") cannot break the compiler.
if exist "%SRCLIST%" del /q "%SRCLIST%"
for /r "%PROJECT%src\main\java" %%f in (*.java) do (
    set "SRCFILE=%%f"
    echo "!SRCFILE:\=/!">> "%SRCLIST%"
)

REM --- Compile -----------------------------------------------------------------
echo  Compiling Java classes...

javac -encoding UTF-8 ^
      -cp "%CATALINA_HOME%\lib\servlet-api.jar;%CATALINA_HOME%\lib\jsp-api.jar;%LIB%\ojdbc17.jar" ^
      -d "%BUILD%\WEB-INF\classes" ^
      "@%SRCLIST%"

if errorlevel 1 (
    echo.
    echo  BUILD FAILED - read the messages above.
    echo.
    pause
    exit /b 1
)

REM --- Deploy -------------------------------------------------------------------
echo  Deploying to Tomcat...
if exist "%CATALINA_HOME%\webapps\TakaBridge" rmdir /s /q "%CATALINA_HOME%\webapps\TakaBridge"
if exist "%CATALINA_HOME%\webapps\TakaBridge.war" del /q "%CATALINA_HOME%\webapps\TakaBridge.war"
xcopy /e /i /y /q "%BUILD%" "%CATALINA_HOME%\webapps\TakaBridge\" >nul

echo.
echo  BUILD OK
echo.
echo  Now double click:   start-tomcat.bat
echo  Then open:          http://localhost:8080/TakaBridge/
echo.
pause
