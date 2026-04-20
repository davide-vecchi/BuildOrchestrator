
REM Beginning of BuildOrchestrator launch script.

@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :

CHCP 65001 >NUL

ECHO SCRIPT DI LANCIO DEL PROGRAMMA BuildOrchestrator.
PAUSE

REM java -jar BuildOrchestrator.jar "H:\Users\Davide\Davide\Sviluppo\REPOS\Accessori\BuildOrchestrator\src\test\resources\TestBuildList01-BuildList.TXT"
REM java -jar BuildOrchestrator.jar "H:\Users\Davide\Davide\Sviluppo\REPOS\Accessori\BuildOrchestrator\src\test\resources\TestBuildList02-BuildList.TXT"
REM java -jar BuildOrchestrator.jar "H:\Users\Davide\Davide\Sviluppo\REPOS\Accessori\BuildOrchestrator\src\test\resources\TestBuildList03-BuildList.TXT"
REM java -jar BuildOrchestrator.jar "H:\Users\Davide\Davide\Sviluppo\REPOS\Accessori\BuildOrchestrator\src\test\resources\TestBuildList04-BuildList.TXT"
REM java -jar BuildOrchestrator.jar "H:\Users\Davide\Davide\Sviluppo\REPOS\Accessori\BuildOrchestrator\src\test\resources\TestBuildList05a-BuildList.TXT"
REM java -jar BuildOrchestrator.jar "H:\Users\Davide\Davide\Sviluppo\REPOS\Accessori\BuildOrchestrator\src\test\resources\TestBuildList05b-BuildList.TXT"
REM
REM java -jar BuildOrchestrator.jar "H:\Users\Davide\Davide\Sviluppo\REPOS\Accessori\BuildOrchestrator\src\main\resources\Build List BuildOrchestrator ONLY.TXT"

java -jar BuildOrchestrator.jar

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

----------------------------------------------------

How to adjust the arguments of the command "java -jar BuildOrchestrator.jar ..." :

The program must be started with either 0 or 1 arguments.
If started with 1 argument, that argument must be the path to the Build List to use.

----------------------------------------------------

All the arguments of the command "java -jar BuildOrchestrator.jar ..." above must be
enclosed in double-quotes ("), but this is not mandatory when the argument
value does not contain any space characters. It's recommended to take the
habit of using the double-quotes for all arguments anyway, in order to avoid
having to remember this.

----------------------------------------------------

In the arguments that consist of a file name, it is not mandatory to specify
the file path. If not specified, the current folder is assumed (that is the
folder where this "BuildOrchestrator.BAT" file is).

Specifying the file path is necessary if the file in subject is not in the
same folder as this "BuildOrchestrator.BAT" file.

----------------------------------------------------

====================================================

SkipComment:

REM End of BuildOrchestrator launch script.
