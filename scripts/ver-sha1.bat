@echo off
rem Muestra la huella SHA-1 de la llave de depuracion (la pide Google Cloud para el cliente OAuth de Android).
cd /d %~dp0..
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
call gradlew.bat -q :app:signingReport > huella-sha1.txt 2>&1
echo FIN >> huella-sha1.txt
