#!/bin/sh

# Define variables 
makefile_specified=no
options_specified=no

# depending on the options, we might invoke the regular compiler, TAU_COMPILER, or both
invoke_without_tau=no
invoke_with_tau=yes

if [ $# = 0 ] ; then
  echo "Usage $0 [-tau_makefile=<tau_stub_makefile>] [-tau_options=<tau_compiler_opts>] <opts> <file>"
  echo "If -tau_makefile option is not used, "
  echo "TAU uses the file specified in the TAU_MAKEFILE environment variable"
  echo "e.g., "
  echo "% tau_f90.sh -tau_makefile=/usr/local/tau-2.x/ia64/lib/Makefile.tau-mpi-pdt  -tau_options=-optVerbose -c foo.f90"
  echo " 	or"
  echo "% setenv TAU_MAKEFILE /usr/local/tau-2.x/include/Makefile"
  echo "% setenv TAU_OPTIONS -optVerbose -optTauSelectFile=select.tau"
  echo "% tau_f90.sh -c foo.f90"
  exit 1
fi

TAUARGS=
NON_TAUARGS=

for arg in "$@" ; do
  # Thanks to Bernd Mohr for the following that handles quotes and spaces (see configure for explanation)
  modarg=`echo "x$arg" | sed -e 's/^x//' -e 's/"/\\\"/g' -e s,\',%@%\',g -e 's/%@%/\\\/g' -e 's/ /\\\ /g'`
  case $arg in 
    -tau_makefile=*)
      MAKEFILE=`echo $arg | sed -e 's/-tau_makefile=//'`
      makefile_specified=yes
      ;;
    -tau_options=*)
      TAUCOMPILER_OPTIONS=`echo $arg | sed -e 's/-tau_options=//'`
      options_specified=yes
      ;;
    -E)
      invoke_without_tau=yes
      invoke_with_tau=no
      NON_TAUARGS="$NON_TAUARGS $modarg"
      ;;
    -MD | -MMD)
      # if either of these are specified, we invoke the regular compiler
      # and TAU_COMPILER, unless -E or another disabling option is specified
      invoke_without_tau=yes
      NON_TAUARGS="$NON_TAUARGS $modarg"
      ;;
    -MF* | -MT* | -MQ* | -MP | -MG)
      # these arguments should only go to the non-tau invocation
      NON_TAUARGS="$NON_TAUARGS $modarg"
      ;;
    -M | -MM | -V | -v | --version | -print-prog-name=ld | -print-search-dirs | -dumpversion)
      # if any of these are specified, we invoke the regular compiler only
      invoke_without_tau=yes
      invoke_with_tau=no
      NON_TAUARGS="$NON_TAUARGS $modarg"
      ;;
    *)
      TAUARGS="$TAUARGS $modarg"
      NON_TAUARGS="$NON_TAUARGS $modarg"
      ;;
  esac
done

if [ $makefile_specified = no ] ; then
    MAKEFILE=$TAU_MAKEFILE
    if [ "x$MAKEFILE" != "x" ] ; then
	if [ ! -r $MAKEFILE ] ; then
	    echo "ERROR: environment variable TAU_MAKEFILE is set but the file is not readable"
	    exit 1
        fi
    else
	echo $0: "ERROR: please set the environment variable TAU_MAKEFILE"
	exit 1
    fi
fi

if [ $options_specified = no ] ; then
    TAUCOMPILER_OPTIONS=$TAU_OPTIONS
    if [ "x$TAUCOMPILER_OPTIONS" = "x" ] ; then
	TAUCOMPILER_OPTIONS=-optVerbose 
    fi
fi

if [ $invoke_without_tau = yes ] ; then
cat <<EOF > /tmp/makefile.tau$$
  include $MAKEFILE
  all:
	@if [ "x\$(TAU_F90)" = "x" ] ; then \
	echo "Error, no fortran compiler specified in TAU configure (use -fortran=<>)" ; \
	else \
	\$(TAU_F90) $NON_TAUARGS ; \
	fi
EOF
make -s -f /tmp/makefile.tau.$USER.$$
/bin/rm -f /tmp/makefile.tau.$USER.$$
fi


if [ $invoke_with_tau = yes ] ; then
cat <<EOF > /tmp/makefile.tau.$USER.$$
include $MAKEFILE
all:
	@if [ "x\$(TAU_F90)" = "x" ] ; then \
	echo "Error, no fortran compiler specified in TAU configure (use -fortran=<>)" ; \
	else \
	\$(TAU_COMPILER) $TAUCOMPILER_OPTIONS \$(TAU_F90) $TAUARGS ; \
	fi

EOF
make -s -f /tmp/makefile.tau.$USER.$$
/bin/rm -f /tmp/makefile.tau.$USER.$$
fi








