::
:: set TAU_ROOT below and make sure java is in your path
::
@echo off
set TAU_ROOT=..


set JAR_ROOT=%TAU_ROOT%/bin
set CONFIG_FILE=%HOME%/.ParaProf/perfdmf.cfg
set PERFDMF_JAR=%JAR_ROOT%/perfdmf.jar
set JARGS_JAR=%JAR_ROOT%/jargs.jar
set JDBC_JAR=%JAR_ROOT%/postgresql.jar;%JAR_ROOT%/mysql.jar;%JAR_ROOT%/oracle.jar
set COMMON_JAR=%JAR_ROOT%/tau-common.jar
set JARS=%JAR_ROOT%/paraprof.jar;%JAR_ROOT%/vis.jar;%PERFDMF_JAR%;%JAR_ROOT%/jogl.jar;%JAR_ROOT%/jgraph.jar;%JDBC_JAR%;%JAR_ROOT%/jargs.jar;%JAR_ROOT%/epsgraphics.jar;%JAR_ROOT%/batik-combined.jar;%JAR_ROOT%/tau-common.jar

java -Xmx500m -Djava.library.path=%JAR_ROOT% -cp %JARS% edu/uoregon/tau/paraprof/ParaProf %1 %2 %3 %4 %5

