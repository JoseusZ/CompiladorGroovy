@echo off
REM ============================================================
REM  CompiladorGroovy - launcher
REM  Doble clic para correr el compilador en modo consola.
REM  Argumentos opcionales: archivo.groovy | --file ruta | --demo
REM ============================================================

setlocal

REM Ubicacion de Groovy portable que instalamos (si no existe, intenta el PATH)
set "GROOVY_HOME=%USERPROFILE%\groovy-3.0.25"

if not exist "%GROOVY_HOME%\bin\groovy.bat" (
    set "GROOVY_BIN=groovy"
) else (
    set "GROOVY_BIN=%GROOVY_HOME%\bin\groovy.bat"
)

REM Ir al directorio del proyecto para que los .groovy se vean entre si
cd /d "%~dp0\.."

echo.
echo ====================================================
echo   CompiladorGroovy - ejecutando...
echo ====================================================
echo.

set "ARGS="
:loop
if "%~1"=="" goto :run
set "ARGS=%ARGS% "%~1""
shift
goto :loop

:run
if "%ARGS%"=="" (
    REM Sin argumentos: modo consola interactivo
    "%GROOVY_BIN%" -cp scr scr/Main.groovy
) else (
    "%GROOVY_BIN%" -cp scr scr/Main.groovy %ARGS%
)

echo.
echo ====================================================
echo   Fin de la ejecucion. Codigo de salida: %ERRORLEVEL%
echo ====================================================
echo.

pause
endlocal
