@ECHO OFF

SET _CLASSPATHCOMPONENT=%1
:argCheck
IF %2a==a goto gotAllArgs
SHIFT
SET _CLASSPATHCOMPONENT=%_CLASSPATHCOMPONENT% %1
GOTO argCheck
:gotAllArgs
SET JAVACLASSPATH=%JAVACLASSPATH%%_CLASSPATHCOMPONENT%;
