/****************************************************************************
 **			TAU Portable Profiling Package			   **
 **			http://www.cs.uoregon.edu/research/tau	           **
 *****************************************************************************
 **    Copyright 2008  						   	   **
 **    Department of Computer and Information Science, University of Oregon **
 **    Advanced Computing Laboratory, Los Alamos National Laboratory        **
 **    Forschungszentrum Juelich                                            **
 ****************************************************************************/
/****************************************************************************
 **	File 		: TauInit.cpp 			        	   **
 **	Description 	: TAU Profiling Package				   **
 **	Author		: Alan Morris					   **
 **	Contact		: tau-bugs@cs.uoregon.edu               	   **
 **	Documentation	: See http://www.cs.uoregon.edu/research/tau       **
 **                                                                         **
 **      Description     : TAU initialization                               **
 **                                                                         **
 ****************************************************************************/

#ifdef __APPLE__
#define _XOPEN_SOURCE 600 /* Single UNIX Specification, Version 3 */
#endif /* __APPLE__ */

#include <TAU.h>
#include <stdlib.h>
#include <stdio.h>
#include <signal.h>
#ifndef TAU_WINDOWS
#include <ucontext.h>
#endif //TAU_WINDOWS
#include <string.h>

#ifndef TAU_WINDOWS
#include <unistd.h>
#endif

#include <Profile/TauEnv.h>
#include <Profile/TauMetrics.h>
#include <Profile/TauSampling.h>
#include <Profile/TauSnapshot.h>
#include <Profile/TauMetaData.h>
#include <Profile/TauInit.h>
#include <Profile/TauMemory.h>
#include <Profile/TauBacktrace.h>

#ifdef TAU_VAMPIRTRACE 
#include <Profile/TauVampirTrace.h>
#else /* TAU_VAMPIRTRACE */
#ifdef TAU_EPILOG
#include "elg_trc.h"
#endif /* TAU_EPILOG */
#endif /* TAU_VAMPIRTRACE */

#ifdef TAU_SCOREP
#include <Profile/TauSCOREP.h>
#endif

using namespace std;

#ifndef TAU_WINDOWS
typedef void (*tau_sighandler_t)(int, siginfo_t*, void*);
#endif

#if defined(TAU_STRSIGNAL_OK)
extern "C" char *strsignal(int sig);
#endif /* TAU_STRSIGNAL_OK */

extern "C" void Tau_stack_initialization();
extern "C" int Tau_compensate_initialization();
extern "C" int Tau_profiler_initialization();
extern "C" int Tau_profile_exit_all_threads();
extern "C" int Tau_dump_callpaths();
extern "C" int Tau_initialize_collector_api(void);

// True if TAU is fully initialized
int tau_initialized = 0;

// True if TAU is initializing
int initializing = 0;

// Rely on the dl auditor (src/wrapper/taupreload) to set dl_initialized
// if the audit feature is available (GLIBC version 2.4 or greater).
// DO NOT declare static!
#ifdef TAU_TRACK_LD_LOADER
int dl_initialized = 0;
#else
int dl_initialized = 1;
#endif



static void wrap_up(int sig)
{
  void * array[10];
  size_t size;

#ifdef TAU_EXECINFO
  // get void*'s for all entries on the stack
  size = backtrace(array, 10);
#endif /* TAU_EXECINFO = !(_AIX || sun || windows) */

  // print out all the frames to stderr
  fprintf(stderr, "TAU: signal %d on %d - calling TAU_PROFILE_EXIT()...\n", sig, RtsLayer::myNode());

#ifdef TAU_EXECINFO
  backtrace_symbols_fd(array, size, 2);
#endif /* TAU_EXECINFO */
  TAU_PROFILE_EXIT("signal");
  fprintf(stderr, "TAU: done.\n");
  exit(1);
}


#ifndef TAU_WINDOWS
static void tauInitializeKillHandlers()
{
  signal(SIGINT, wrap_up);
  signal(SIGQUIT, wrap_up);
  signal(SIGILL, wrap_up);
  signal(SIGFPE, wrap_up);
  signal(SIGBUS, wrap_up);
  signal(SIGTERM, wrap_up);
  signal(SIGABRT, wrap_up);
  signal(SIGSEGV, wrap_up);
#ifndef TAU_UPC
  signal(SIGCHLD, wrap_up);
#endif
}
#endif

#ifndef TAU_DISABLE_SIGUSR

static void tauSignalHandler(int sig)
{
  // Protect TAU from itself
  TauInternalFunctionGuard protects_this_function;

  if (TauEnv_get_sigusr1_action() == TAU_ACTION_DUMP_CALLPATHS) {
    fprintf(stderr, "Caught SIGUSR1, dumping TAU callpath data\n");
    Tau_dump_callpaths();
  } else if (TauEnv_get_sigusr1_action() == TAU_ACTION_DUMP_BACKTRACES) {
    fprintf(stderr, "Caught SIGUSR1, dumping backtrace data\n");
  } else {
    fprintf(stderr, "Caught SIGUSR1, dumping TAU profile data\n");
    TAU_DB_DUMP_PREFIX("profile");
  }
}

static void tauToggleInstrumentationHandler(int sig)
{
  // Protect TAU from itself
  TauInternalFunctionGuard protects_this_function;

  fprintf(stderr, "Caught SIGUSR2, toggling TAU instrumentation\n");
  if (RtsLayer::TheEnableInstrumentation()) {
    RtsLayer::TheEnableInstrumentation() = false;
  } else {
    RtsLayer::TheEnableInstrumentation() = true;
  }
}

static void tauBacktraceHandler(int sig, siginfo_t *si, void *context)
{
  // Protect TAU from itself
  TauInternalFunctionGuard protects_this_function;

  // Trigger a context event and record metadata
  char eventname[1024];
  sprintf(eventname, "TAU_SIGNAL (%s)", strsignal(sig));
  TAU_REGISTER_CONTEXT_EVENT(evt, eventname);
  TAU_CONTEXT_EVENT(evt, 1);
  TAU_METADATA("SIGNAL", strsignal(sig));

  Tau_backtrace_exit_with_backtrace(1,
      "TAU: Caught signal %d (%s), dumping profile with stack trace: [rank=%d, pid=%d, tid=%d]... \n",
      sig, strsignal(sig), RtsLayer::myNode(), getpid(), Tau_get_tid());
}

static void tauMemdbgHandler(int sig, siginfo_t *si, void *context)
{
  // Protect TAU from itself
  TauInternalFunctionGuard protects_this_function;

  // Use the backtrace handler if this SIGSEGV wasn't due to invalid memory access
  if (sig == SIGSEGV && si->si_code != SEGV_ACCERR) {
    tauBacktraceHandler(sig, si, context);
    return;
  }

  TAU_REGISTER_CONTEXT_EVENT(evt, "Invalid memory access");

  // Try to find allocation information for the address
  void * ptr = si->si_addr;
  TauAllocation * alloc = TauAllocation::FindContaining(ptr);

  // If allocation info was found, be more informative and maybe attempt to continue
  if (alloc && TauEnv_get_memdbg_attempt_continue()) {

    // Disable the guard page so we can resume
    if (alloc->InUpperGuard(ptr)) {
      alloc->DisableUpperGuard();
    } else if (alloc->InLowerGuard(ptr)) {
      alloc->DisableLowerGuard();
    } else {
      Tau_backtrace_exit_with_backtrace(1,
          "TAU: Memory debugger caught invalid memory access and cannot continue. "
          "Dumping profile with stack trace: [rank=%d, pid=%d, tid=%d]... \n",
          RtsLayer::myNode(), getpid(), Tau_get_tid());
    }

    // Trigger the context event and record a backtrace
    TAU_CONTEXT_EVENT(evt, 1);
    Tau_backtrace_record_backtrace(1);

  } else {
    // Trigger the context event and record a backtrace
    TAU_CONTEXT_EVENT(evt, 1);
    Tau_backtrace_exit_with_backtrace(1,
        "TAU: Memory debugger caught invalid memory access. "
        "Dumping profile with stack trace: [rank=%d, pid=%d, tid=%d]... \n",
        RtsLayer::myNode(), getpid(), Tau_get_tid());
  }

  // Exit the handler and return to the instruction that raised the signal
}


static int tauAddSignal(int sig, tau_sighandler_t handler = tauBacktraceHandler)
{
  int ret = 0;

  struct sigaction act;
  memset(&act, 0, sizeof(struct sigaction));
  ret = sigemptyset(&act.sa_mask);
  if (ret != 0) {
    printf("TAU: Signal error: %s\n", strerror(ret));
    return -1;
  }

  ret = sigaddset(&act.sa_mask, sig);
  if (ret != 0) {
    printf("TAU: Signal error: %s\n", strerror(ret));
    return -1;
  }
  act.sa_sigaction = handler;
#if defined(TAU_BGL) || defined(TAU_BGP) || defined(TAU_BGQ)
  act.sa_flags = SA_SIGINFO;
#else
  act.sa_flags = SA_SIGINFO | SA_ONSTACK;
#endif

  ret = sigaction(sig, &act, NULL);
  if (ret != 0) {
    printf("TAU: error adding signal in sigaction: %s\n", strerror(ret));
    return -1;
  }

  return ret;
}
#endif //TAU_DISABLE_SIGUSR


#ifdef TAU_VAMPIRTRACE
//////////////////////////////////////////////////////////////////////
// Initialize VampirTrace Tracing package
//////////////////////////////////////////////////////////////////////
int Tau_init_vampirTrace(void) {
  vt_open();
  return 0;
}
#endif /* TAU_VAMPIRTRACE */

#ifdef TAU_EPILOG 
//////////////////////////////////////////////////////////////////////
// Initialize EPILOG Tracing package
//////////////////////////////////////////////////////////////////////
int Tau_init_epilog(void) {
  esd_open();
  return 0;
}
#endif /* TAU_EPILOG */

extern "C"
int Tau_init_check_initialized()
{
  return tau_initialized;
}


extern "C"
int Tau_init_initializingTAU()
{
  return initializing - tau_initialized;
}

extern "C"
void Tau_init_dl_initialized()
{
  dl_initialized = 1;
}

extern "C"
int Tau_init_check_dl_initialized()
{
  return dl_initialized;
}


//////////////////////////////////////////////////////////////////////
// Initialize signal handling routines
//////////////////////////////////////////////////////////////////////
extern "C"
int Tau_signal_initialization()
{
#ifndef TAU_DISABLE_SIGUSR
  // Protect TAU from itself
  TauInternalFunctionGuard protects_this_function;

  if (TauEnv_get_track_signals()) {
    TAU_VERBOSE("TAU: Enable signal tracking\n");

    tauAddSignal(SIGILL);
    tauAddSignal(SIGINT);
    tauAddSignal(SIGQUIT);
    tauAddSignal(SIGTERM);
    tauAddSignal(SIGPIPE);
    tauAddSignal(SIGABRT);
    tauAddSignal(SIGFPE);
    if (TauEnv_get_memdbg()) {
      tauAddSignal(SIGBUS, tauMemdbgHandler);
      tauAddSignal(SIGSEGV, tauMemdbgHandler);
    } else {
      tauAddSignal(SIGBUS);
      tauAddSignal(SIGSEGV);
    }
  }

#endif // TAU_DISABLE_SIGUSR
  return 0;
}

extern "C" int Tau_init_initializeTAU()
{
  //protect against reentrancy
  if (initializing) return 0;
  initializing = 1;

  // Protect TAU from itself
  TauInternalFunctionGuard protects_this_function;

  /* initialize the memory debugger */
  Tau_memory_initialize();

  /* initialize the Profiler stack */
  Tau_stack_initialization();

  /* initialize environment variables */
  TauEnv_initialize();

#ifdef TAU_EPILOG
  /* no more initialization necessary if using epilog/scalasca */
  initializing = 1;
  Tau_init_epilog();
  return 0;
#endif

#ifdef TAU_SCOREP
  /* no more initialization necessary if using SCOREP */
  initializing = 1;
  SCOREP_Tau_InitMeasurement();
  SCOREP_Tau_RegisterExitCallback(Tau_profile_exit_all_threads);
  return 0;
#endif

#ifdef TAU_VAMPIRTRACE
  /* no more initialization necessary if using vampirtrace */
  initializing = 1;
  Tau_init_vampirTrace();
  return 0;
#endif

  /* we need the timestamp of the "start" */
  Tau_snapshot_initialization();

#ifndef TAU_DISABLE_SIGUSR
  if (signal(SIGUSR1, tauSignalHandler) == SIG_ERR) {
    perror("failed to register TAU profile dump signal handler");
  }

  if (signal(SIGUSR2, tauToggleInstrumentationHandler) == SIG_ERR) {
    perror("failed to register TAU instrumentation toggle signal handler");
  }
#endif

  Tau_profiler_initialization();

  /* initialize the metrics we will be counting */
  TauMetrics_init();

  Tau_signal_initialization();

  /* initialize compensation */
  if (TauEnv_get_compensate()) {
    Tau_compensate_initialization();
  }
#ifndef TAU_WINDOWS
  /* initialize signal handlers to flush the trace buffer */
  if (TauEnv_get_tracing()) {
    tauInitializeKillHandlers();
  }
#endif
  /* initialize sampling if requested */
#if !defined(TAU_MPI) && !defined(TAU_WINDOWS)
  if (TauEnv_get_ebs_enabled()) {
    // Work-around for MVAPHICH 2 to move sampling initialization to after MPI_Init()
    Tau_sampling_init_if_necessary();
  }
#endif /* TAU_MPI && TAU_WINDOWS */

#ifdef TAU_PGI
  sbrk(102400);
#endif /* TAU_PGI */

#ifndef TAU_DISABLE_METADATA
  Tau_metadata_fillMetaData();
#endif

#ifdef TAU_OPENMP
  Tau_initialize_collector_api();
#endif

  tau_initialized = 1;

#ifdef __MIC__
  if (TauEnv_get_mic_offload())
  {
    TAU_PROFILE_SET_NODE(0);
    Tau_create_top_level_timer_if_necessary();
  }
#endif

  //Initialize locks.
  RtsLayer::Initialize();

  // FIXME: No so sure this is a good idea...
  Tau_memory_wrapper_enable();

  return 0;
}
