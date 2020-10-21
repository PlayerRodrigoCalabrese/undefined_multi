@echo off
:loop
title Undefined EMU - MULTI
"C:\Program Files\BellSoft\LibericaJRE-15\bin\java.exe" -Xmx1024M -jar multi.jar -o true
goto loop
PAUSE
