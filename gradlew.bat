@echo off
setlocal
set "APP_HOME=%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%APP_HOME%scripts\bootstrap-wrapper.ps1"
if errorlevel 1 exit /b 1
set "JAVA_EXE=java.exe"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
