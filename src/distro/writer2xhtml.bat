@echo off
rem *Very* simple batch file to run Writer2xhtml
rem Last modified june 2022

rem If the Java executable is not in your path, please edit the following
rem line to contain the full path and file name

set JAVAEXE="java"

%JAVAEXE% -jar "%~dp0\writer2xhtml.jar" %1 %2 %3 %4 %5 %6 %7 %8 %9

set JAVAEXE=

