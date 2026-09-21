@if "%DEBUG%"=="" @echo off
if "%OS%"=="Windows_NT" setlocal

set "JAVA_HOME=C:\Users\Hari\.jdks\jbr-21.0.11"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

set "DIRNAME=%~dp0"
if "%DIRNAME%"=="" set "DIRNAME=."
set "APP_BASE_NAME=%~n0"
set "APP_HOME=%DIRNAME%"

if exist "%JAVA_EXE%" goto execute

echo ERROR: JAVA_EXE not found at "%JAVA_EXE%"
exit /b 1

:execute
set "CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar"

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" "-Dorg.gradle.java.home=C:\Users\Hari\.jdks\jbr-21.0.11" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
