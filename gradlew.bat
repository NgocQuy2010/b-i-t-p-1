@echo off
setlocal
set "ROOT=%~dp0"
if not exist "%ROOT%gradle\wrapper\gradle-wrapper.jar" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%ROOT%setup-wrapper.ps1"
  if errorlevel 1 exit /b 1
)
java -cp "%ROOT%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
