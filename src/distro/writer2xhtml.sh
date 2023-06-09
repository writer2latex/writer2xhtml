# *Very* simple script to run Writer2xhtml (unix version)
# Created by Henrik Just, october 2003
# Modified december 2003 as suggested by Juan Julian Merelo Guervos
# Modified may 2004 as suggested by J. Wolfgang Kaltz
# Last modified june 2022

# If writer2xhtml.jar is not in the same directory as the script, please
# edit the following line to contain the full path to Writer2xhtml:
W2XPATH=`dirname $0`

# If the required JVM is not in your path, or the path is not set by JAVA_HOME,
# please edit the following line to contain the full path and file name
MYJAVAEXE="java"

if [ "$JAVA_HOME" = "" ]
then
JAVAEXE="$MYJAVAEXE"
else
JAVAEXE="$JAVA_HOME/bin/java"
fi

$JAVAEXE -jar "$W2XPATH/writer2xhtml.jar" "$@"