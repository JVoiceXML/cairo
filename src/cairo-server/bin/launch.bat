@echo off

if "%OS%"=="Windows_NT" goto init

echo.
echo ERROR: this script not supported for %OS%.
echo You will need to modify this script for it to work with
echo your operating system.
echo.
goto error

:init

@setlocal enableextensions enabledelayedexpansion
set CAIRO_VERSION=${project.version}

:startValidation

if not "%1" == "" goto chkCairoHome

echo.
echo ERROR: improper call to launch script
echo launch.bat should not be executed directly, please see README for
echo proper application launching instructions.
echo.
goto error

:chkCairoHome

if not "%CAIRO_HOME%"=="" goto valCairoHome

if "%OS%"=="Windows_NT" set CAIRO_HOME=%~dp0..
if not "%CAIRO_HOME%"=="" goto valCairoHome

echo.
echo ERROR: CAIRO_HOME not found in your environment.
echo Please set the CAIRO_HOME variable in your environment to match the
echo location of the Cairo installation
echo.
goto error

:valCairoHome
set CAIRO_JAR=%CAIRO_HOME%\cairo-%CAIRO_VERSION%.jar
if exist "%CAIRO_JAR%" goto chkJavaHome

echo.
echo ERROR: CAIRO_HOME is set to an invalid directory.
echo CAIRO_HOME = %CAIRO_HOME%
echo %CAIRO_JAR% not found!
echo Please set the CAIRO_HOME variable in your environment to match the
echo location of the Cairo installation
echo.
goto error

:chkJavaHome

if not "%JAVA_HOME%" == "" goto valJavaHome

echo.
echo ERROR: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:valJavaHome

if exist "%JAVA_HOME%\bin\java.exe" goto chkJMF

echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = %JAVA_HOME%
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:chkJMF

if exist "%JAVA_HOME%\jre\lib\ext\jmf.jar" goto chkJSAPI
if exist "%JAVA_HOME%\lib\ext\jmf.jar" goto chkJSAPI

echo.
echo ERROR: Java Media Framework (JMF) is not installed.
echo Please download and install JMF from Sun Java web site:
echo http://java.sun.com/products/java-media/jmf/
echo.
goto error

:chkJSAPI

if exist "%JAVA_HOME%\jre\lib\ext\jsapi.jar" goto setClasspath
if exist "%JAVA_HOME%\lib\ext\jsapi.jar" goto setClasspath
if exist "%CAIRO_HOME%\lib\jsapi.jar" goto setClasspath

echo.
echo ERROR: Java Speech API (JSAPI) is not installed.
echo Please run jsapi.exe or jsapi.sh and place the extracted
echo jsapi.jar in %JAVA_HOME%\jre\lib\ext
echo The install file can be downloaded from here:
echo http://www.speechforge.org/downloads/jsapi
echo.
goto error

:setClasspath

set CLASSPATH=%CAIRO_JAR%
for %%b in (%CAIRO_HOME%\lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%b

set CLASSPATH=!CLASSPATH!;%CAIRO_HOME%\config
@REM echo CLASSPATH=%CLASSPATH%

:run
@rem -XX:+UseParallelGC  -XX:+UseConcMarkSweepGC -Xincgc 
@rem -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 
@rem -Xms100m -Xmx200m 
@rem -verbose:gc
"%JAVA_HOME%\bin\java" -Xmx200m  -XX:+UseConcMarkSweepGC -Dlog4j.configuration=log4j.xml -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 %*
goto exit

:error

if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=1
pause
goto eof

:exit

if "%OS%"=="Windows_NT" @endlocal

:eof

@REM === EOF ===
