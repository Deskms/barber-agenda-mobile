@echo off
rem Compila la app (debug) con el JDK de Android Studio y deja el resultado en build-log.txt.
cd /d %~dp0..
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
echo JAVA_HOME=%JAVA_HOME% > build-log.txt
call gradlew.bat :app:assembleDebug --console=plain >> build-log.txt 2>&1
echo FIN %ERRORLEVEL% >> build-log.txt
