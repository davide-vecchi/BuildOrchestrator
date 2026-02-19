
REM BuildOrchestrator launch script.

@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :

CHCP 65001 >NUL

ECHO SCRIPT DI LANCIO DEL PROGRAMMA BuildOrchestrator.
PAUSE

SET BasePath=H:\Users\Davide\Davide\Sviluppo\REPOS\Accessori\BuildOrchestrator

java -jar BuildOrchestrator.jar "%BasePath%\src\main\resources\BuildOrchestrator-Config.TXT"

                             REM %BasePath%\src\test\resources\BuildOrchestrator-Config_TestBuildList01.TXT
                             REM %BasePath%\src\test\resources\BuildOrchestrator-Config_TestBuildList01.TXT
                             REM %BasePath%\src\test\resources\BuildOrchestrator-Config_TestBuildList02.TXT
                             REM %BasePath%\src\test\resources\BuildOrchestrator-Config_TestBuildList03.TXT
                             REM %BasePath%\src\test\resources\BuildOrchestrator-Config_TestBuildList04.TXT
                             REM %BasePath%\src\test\resources\BuildOrchestrator-Config_TestBuildList05a.TXT
                             REM %BasePath%\src\test\resources\BuildOrchestrator-Config_TestBuildList05b.TXT
                             REM %BasePath%\src\main\resources\BuildOrchestrator-Config.TXT
                             REM C:\BuildOrchestrator\BuildOrchestrator-Config.TXT
PAUSE

GOTO SkipComment

====================================================
User guide:

----------------------------------------------------

In order to start the BuildOrchestrator program:

1) Adjust the arguments of the command "java -jar BuildOrchestrator.jar ..." above in
   this file, as described in the section below.

2) Save the file (with the same name or with any other name).

3) Double-click it (or right-click it and select the option in bold, which is
   the default and could be called "Open", or "Run", or "Esegui" or similar).

4) The program will show messages to inform about the progress.
   One of the last lines shown should read "... Executed successfully.".

   If instead errors are shown, please preserve the log file. It is in the
   same folder as the SOURCE file, and its name is the same as the SOURCE
   file (first argument, see below) followed by the extension ".LOG"; so if
   the SOURCE file was "C:\AAA\BBB\MySource.epx" then the log file will be
   "MySource.epx.LOG".

----------------------------------------------------

How to adjust the arguments of the command "java -jar BuildOrchestrator.jar ..." :

The program must be started with exactly 1 arguments, in this order:

1) The configuration file.

----------------------------------------------------

All the arguments of the command "java -jar BuildOrchestrator.jar ..." above must be
enclosed in double-quotes ("), but this is not mandatory when the argument
value does not contain any space characters. It's recommended to take the
habit of using the double-quotes for all arguments anyway, in order to avoid
having to remember this.

Each argument must be on a different row; each of these rows must end with
a space followed by a caret (^) except the row with the last argument.

----------------------------------------------------

In the arguments that consist of a file name, it is not mandatory to specify
the file path. If not specified, the current folder is assumed (that is the
folder where this "BuildOrchestrator.BAT" file is).

Specifying the file path is necessary if the file in subject is not in the
same folder as this "BuildOrchestrator.BAT" file.

----------------------------------------------------

====================================================

SkipComment:
