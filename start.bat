@echo off
setlocal
cd /d "%~dp0"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto run
for /d %%D in ("%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-*" "%ProgramFiles%\Eclipse Adoptium\jdk-*" "%ProgramFiles%\Java\jdk-*" "%USERPROFILE%\.jdks\*") do if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto run
echo JDK not found. Install JDK 17 or newer and set JAVA_HOME to its folder.
pause
exit /b 1
:run
set "PATH=%JAVA_HOME%\bin;%SystemRoot%\System32;%SystemRoot%\System32\WindowsPowerShell\v1.0;%PATH%"
echo Starting Student Attendance at http://localhost:8080
echo Keep this window open. Press Ctrl+C to stop.
echo If port 8080 is in use, stop your previous copy of the application first.
if exist "target\student-attendance-1.0.0.jar" (
  java -jar "target\student-attendance-1.0.0.jar"
) else (
  call mvnw.cmd spring-boot:run
)
if errorlevel 1 pause
endlocal
