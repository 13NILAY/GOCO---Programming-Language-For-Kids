@echo off
if "%1"=="" (
    echo Usage: run.bat filename.goco
    exit /b 1
)

echo Compiling AST nodes...
javac -d ..\..\ide\target\classes -sourcepath ..\src\main\java ..\src\main\java\ast\nodes\*.java ..\src\main\java\ast\arrays\*.java

echo Compiling parser...
javac -d ..\..\ide\target\classes -cp ..\bin -sourcepath ..\src\main\java ..\src\main\java\parser\*.java

echo Running parser...
java -cp ..\..\ide\target\classes parser.MyLanguageParser %1

pause
