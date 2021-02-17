/****************************************************************************
 **			TAU Portable Profiling Package			   **
 **			http://www.cs.uoregon.edu/research/tau	           **
 *****************************************************************************
 **    Copyright 1997  						   	   **
 **    Department of Computer and Information Science, University of Oregon **
 **    Advanced Computing Laboratory, Los Alamos National Laboratory        **
 ****************************************************************************/
/***************************************************************************
 **	File 		: PthreadLayer.cpp				  **
 **	Description 	: TAU Profiling Package RTS Layer definitions     **
 **			  for supporting pthreads 			  **
 **	Contact		: tau-team@cs.uoregon.edu 		 	  **
 **	Documentation	: See http://www.cs.uoregon.edu/research/tau      **
 ***************************************************************************/

#ifdef TAU_DOT_H_LESS_HEADERS
#include <iostream>
#else /* TAU_DOT_H_LESS_HEADERS */
#include <iostream.h>
#endif /* TAU_DOT_H_LESS_HEADERS */
#include <Profile/Profiler.h>
#include <Profile/PthreadLayer.h>
#include <Profile/TauInit.h>
#include <Profile/TauSampling.h>

#include <stdlib.h>
#include <string.h>

#ifdef TAU_GPU
#include <Profile/TauGpu.h>
#endif

// FIXME: Duplicated in pthread_wrap.c
#if !defined(__APPLE__)
#define TAU_PTHREAD_BARRIER_AVAILABLE
#endif

using namespace std;
using namespace tau;

/////////////////////////////////////////////////////////////////////////
// Member Function Definitions For class PthreadLayer
// This allows us to get thread ids from 0..N-1 instead of long nos
// as generated by pthread_self()
/////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////////////
// Define the static private members of PthreadLayer
/////////////////////////////////////////////////////////////////////////

pthread_key_t wrapper_flags_key;

int PthreadLayer::tauThreadCount = 0;
pthread_once_t PthreadLayer::initFlag = PTHREAD_ONCE_INIT;
pthread_key_t PthreadLayer::tauPthreadId;
pthread_mutex_t PthreadLayer::tauThreadcountMutex;
pthread_mutex_t PthreadLayer::tauDBMutex;
pthread_mutex_t PthreadLayer::tauEnvMutex;

////////////////////////////////////////////////////////////////////////
// RegisterThread() should be called before any profiling routines are
// invoked. This routine sets the thread id that is used by the code in
// FunctionInfo and Profiler classes. This should be the first routine a
// thread should invoke from its wrapper. Note: main() thread shouldn't
// call this routine.
////////////////////////////////////////////////////////////////////////
int PthreadLayer::RegisterThread(void)
{
  InitializeThreadData();

  int * id = (int*)pthread_getspecific(tauPthreadId);
  if (!id) {
    id = new int;
    pthread_setspecific(tauPthreadId, id);
    pthread_mutex_lock(&tauThreadcountMutex);
    *id = RtsLayer::_createThread();
    pthread_mutex_unlock(&tauThreadcountMutex);
  }
  return 0;
}

////////////////////////////////////////////////////////////////////////
// GetThreadId returns an id in the range 0..N-1 by looking at the
// thread specific data. Since a getspecific has to be preceeded by a
// setspecific (that all threads besides main do), we get a null for the
// main thread that lets us identify it as thread 0. It is the only
// thread that doesn't do a PthreadLayer::RegisterThread().
////////////////////////////////////////////////////////////////////////
int PthreadLayer::GetThreadId(void)
{
#ifdef TAU_CHARM
  if (RtsLayer::myNode() == -1) return 0;
#endif

  InitializeThreadData();

  int * id = (int*)pthread_getspecific(tauPthreadId);
  if (id) {
    return *id;
  }
  return 0; // main() thread
}

////////////////////////////////////////////////////////////////////////
// InitializeThreadData is called before any thread operations are performed.
// It sets the default values for static private data members of the
// PthreadLayer class.
////////////////////////////////////////////////////////////////////////

void PthreadLayer::delete_wrapper_flags_key(void* wrapped) {
  if (wrapped != NULL) {
    int * tmp = (int*)(wrapped);
    if (TauEnv_get_recycle_threads()) {
        RtsLayer::recycleThread(*tmp);
    }
    delete tmp;
  }
}

extern "C"
void Tau_pthread_fork_before(void) {
  // Temporarily disable the sampling timer when a fork occurs.
  // Otherwise the signal can interfere with the fork.
  if(TauEnv_get_ebs_enabled()) {
    Tau_sampling_timer_pause();
  }
}

extern "C"
void Tau_pthread_fork_after(void) {
  // Re-enable the sampling timer
  if(TauEnv_get_ebs_enabled()) {
    Tau_sampling_timer_resume();
  }
}

extern "C"
void pthread_init_once(void)
{
  pthread_key_create(&PthreadLayer::tauPthreadId, &PthreadLayer::delete_wrapper_flags_key);
  pthread_mutex_init(&PthreadLayer::tauThreadcountMutex, NULL);
  pthread_mutex_init(&PthreadLayer::tauDBMutex, NULL);
  pthread_mutex_init(&PthreadLayer::tauEnvMutex, NULL);
  // FIXME: This is completely unrelated to PthreadLayer
  pthread_key_create(&wrapper_flags_key, NULL);
  // Register handler to disable sample processing during fork
  pthread_atfork(Tau_pthread_fork_before, Tau_pthread_fork_after, NULL);
}

int PthreadLayer::InitializeThreadData(void)
{
  // Do this exactly once.  Checking a static flag is a race condition so
  // use pthread_once with a callback friend function.
  pthread_once(&initFlag, pthread_init_once);
  return 0;
}

////////////////////////////////////////////////////////////////////////
int PthreadLayer::InitializeDBMutexData(void)
{
  // Initialized in pthread_init_once
  return 1;
}

////////////////////////////////////////////////////////////////////////
// LockDB locks the mutex protecting TheFunctionDB() global database of
// functions. This is required to ensure that push_back() operation
// performed on this is atomic (and in the case of tracing this is
// followed by a GetFunctionID() ). This is used in
// FunctionInfo::FunctionInfoInit().
////////////////////////////////////////////////////////////////////////
int PthreadLayer::LockDB(void)
{
  InitializeThreadData();
  pthread_mutex_lock(&tauDBMutex);
  return 1;
}

////////////////////////////////////////////////////////////////////////
// UnLockDB() unlocks the mutex tauDBMutex used by the above lock operation
////////////////////////////////////////////////////////////////////////
int PthreadLayer::UnLockDB(void)
{
  pthread_mutex_unlock(&tauDBMutex);
  return 1;
}

////////////////////////////////////////////////////////////////////////
int PthreadLayer::InitializeEnvMutexData(void)
{
  // Initialized in pthread_init_once
  return 1;
}

////////////////////////////////////////////////////////////////////////
// LockEnv locks the mutex protecting TheFunctionEnv() global database of
// functions. This is required to ensure that push_back() operation
// performed on this is atomic (and in the case of tracing this is
// followed by a GetFunctionID() ). This is used in
// FunctionInfo::FunctionInfoInit().
////////////////////////////////////////////////////////////////////////
int PthreadLayer::LockEnv(void)
{
  InitializeThreadData();
  pthread_mutex_lock(&tauEnvMutex);
  return 1;
}

////////////////////////////////////////////////////////////////////////
// UnLockEnv() unlocks the mutex tauEnvMutex used by the above lock operation
////////////////////////////////////////////////////////////////////////
int PthreadLayer::UnLockEnv(void)
{
  pthread_mutex_unlock(&tauEnvMutex);
  return 1;
}


///////////////////////////////////////////////////////////////////////////////
// Below is the pthread_create wrapper
// TODO: This should probably be in its own file.
///////////////////////////////////////////////////////////////////////////////

// FIXME: Duplicated in pthread_wrap.c
typedef void * (*start_routine_p)(void *);
typedef int (*pthread_create_p)(pthread_t *, const pthread_attr_t *, start_routine_p, void *arg);
typedef void (*pthread_exit_p)(void *);
typedef int (*pthread_join_p)(pthread_t, void **);
#ifdef TAU_PTHREAD_BARRIER_AVAILABLE
typedef int (*pthread_barrier_wait_p)(pthread_barrier_t *);
#endif

struct tau_pthread_pack
{
  start_routine_p start_routine;
  std::vector<FunctionInfo*> timer_context_stack;
  void * arg;
};

extern "C" char* Tau_ompt_resolve_callsite_eagerly(unsigned long addr, char * resolved_address);

struct tau_pthread_wrapper_args_t {
    void * handle; // TAU timer handle
    tau_pthread_pack * pack; // Argument to tau_pthread_function
};

// If a thread exits through pthread_exit(), we will never return
// to the tau_ptrhead_function() wrapper and could end up not stopping
// the timer. We use a pthread cleanup function to handle stopping
// the timer in that case.
extern "C"
void tau_pthread_function_cleanup_handler(void * args) {
  tau_pthread_wrapper_args_t * wrapper_args = (tau_pthread_wrapper_args_t *)args;
  TAU_PROFILER_STOP(wrapper_args->handle);
  /* iterate over the stack and stop the timer context */
  if (TauEnv_get_threadContext() && wrapper_args->pack->timer_context_stack.size() > 0) {
    for (std::vector<FunctionInfo*>::iterator iter = wrapper_args->pack->timer_context_stack.end() ;
        iter != wrapper_args->pack->timer_context_stack.begin() ; iter--) {
  	    Tau_stop_timer(*iter, Tau_get_thread());
    }
  }
#ifndef TAU_TBB_SUPPORT
  // Thread 0 in TBB will not wait for the other threads to finish
  // (it does not join). DO NOT stop the timer for this thread, but
  // let thread 0 do that when it shuts down all threads on exit.
  // See src/wrappers/taupreload/taupreload.c:taupreload_fini() for details
  Tau_stop_top_level_timer_if_necessary();
#endif
  delete wrapper_args->pack;
}

// TAU replaces the function which is to be launched on a new pthread
// with this wrapper function, which ensures that the thread is registered
// and starts a timer on it before calling the function that was actually
// provided to pthread_create.
extern "C"
void * tau_pthread_function(void *arg)
{   
  void * ret = NULL;
  tau_pthread_pack * pack = (tau_pthread_pack*)arg;

  TAU_REGISTER_THREAD();
  Tau_create_top_level_timer_if_necessary();
  
  /* iterate over the stack and create a timer context */
  if (TauEnv_get_threadContext() && pack->timer_context_stack.size() > 0) {
    for (std::vector<FunctionInfo*>::iterator iter = pack->timer_context_stack.begin() ;
        iter != pack->timer_context_stack.end() ; iter++) {
  	    Tau_start_timer(*iter, 0, Tau_get_thread());
    }
  }

  /* Create a timer that will measure this spawned thread */
  char timerName[1024] = {0};
  // Give the timer name a prefix of [PTHREAD] to distinguish it from
  // the timer started within the function if it is instrumented.
  const char timerPrefix[] = "[PTHREAD] ";
  strncpy(timerName, timerPrefix, sizeof(timerName));
  Tau_ompt_resolve_callsite_eagerly((unsigned long)(pack->start_routine), timerName + sizeof(timerPrefix) - 1);
  void *handle = NULL;
  TAU_PROFILER_CREATE(handle, timerName, "", TAU_DEFAULT);

  TAU_PROFILER_START(handle);
  // Stopping this timer is handled by a pthread cleanup function,
  // which accepts a single argument. We pass the timer handle
  // and pack through to the cleanup function through wrapper_args.
  tau_pthread_wrapper_args_t wrapper_args;
  wrapper_args.handle = handle;
  wrapper_args.pack = pack;
  // Register the cleanup function.
  pthread_cleanup_push(tau_pthread_function_cleanup_handler, &wrapper_args);
  
  // Call the function that we are wrapping.
  ret = pack->start_routine(pack->arg);

  // If we return normally, we pop the cleanup function.
  // Non-zero argument causes the cleanup function to execute when popped,
  // so that we will stop the timer through the cleanup function even
  // when returning normally.
  pthread_cleanup_pop(1);
  return ret;
}

/* Forward declaration to see if TAU thinks GPU initialization is done */
bool& Tau_gpu_initialized(void);
Profiler * Tau_get_timer_at_stack_depth(int pos);

extern "C"
int tau_pthread_create_wrapper(pthread_create_p pthread_create_call,
    pthread_t * threadp, const pthread_attr_t * attr,
    start_routine_p start_routine, void * arg)
{
  TauInternalFunctionGuard protects_this_function;

  bool * wrapped = (bool*)pthread_getspecific(wrapper_flags_key);
  if (!wrapped) {
    wrapped = new bool;
    pthread_setspecific(wrapper_flags_key, (void*)wrapped);
    *wrapped = false;
  }

  size_t stackSize = TauEnv_get_pthread_stack_size();
  pthread_attr_t tmp;
  bool destroy_attr = false;
  if (stackSize) {
    if (attr == NULL) {
	  destroy_attr = true;
	  pthread_attr_init(&tmp);
	  attr = &tmp;
	}
    size_t defaultSize;
    if (pthread_attr_getstacksize(attr, &defaultSize)) {
      TAU_VERBOSE("TAU: ERROR - failed to get default pthread stack size.\n");
      defaultSize = 0;
    }
    if(pthread_attr_setstacksize(const_cast<pthread_attr_t*>(attr), stackSize)) {
      TAU_VERBOSE("TAU: ERROR - failed to change pthread stack size from %d to %d.\n", defaultSize, stackSize);
    } else {
      TAU_VERBOSE("TAU: changed pthread stack size from %d to %d\n", defaultSize, stackSize);
    }
  }

  int retval;
  bool ignore_thread = false;
#ifdef CUPTI
/* This is needed so that TauGpu.cpp can let the rest of TAU know that
 * it has been initialized.  PthreadLayer.cpp needs to know whether
 * CUDA/CUPTI has been initialized (for GPU timestamps) - if it starts before
 * TAU is initialized, then we could have problems.  This prevents that. */
  ignore_thread = !Tau_gpu_initialized();
#endif
  if(*wrapped || Tau_global_getLightsOut() ||
     !Tau_init_check_initialized() || ignore_thread) {
    // Another wrapper has already intercepted the call so just pass through
    retval = pthread_create_call(threadp, attr, start_routine, arg);
  } else {
    *wrapped = true;
    tau_pthread_pack * pack = new tau_pthread_pack;
    pack->start_routine = start_routine;
    pack->arg = arg;

    // CUDA or OpenMP could spawn a thread during startup, so make
    // sure that we have a top level timer so that when
    // pthread_create exits, we don't write a profile!
    Tau_init_initializeTAU();
    if (Tau_get_node() == -1) { Tau_set_node(0); }
    Tau_create_top_level_timer_if_necessary();

	/* set up some context for the spawned thread */
    if (TauEnv_get_threadContext()) {
        int depth = Tau_get_current_stack_depth(Tau_get_thread());
        for (int i = 1 ; i <= depth ; i++) {
            tau::Profiler *profiler = Tau_get_timer_at_stack_depth(i);
            //printf("Pushing timer: %s\n", profiler->ThisFunction->GetName());
            pack->timer_context_stack.push_back(profiler->ThisFunction);
        }
    }

    TAU_PROFILE_TIMER(timer, "pthread_create", "", TAU_DEFAULT);
    TAU_PROFILE_START(timer);
    retval = pthread_create_call(threadp, attr, tau_pthread_function, (void*)pack); // 0
    TAU_PROFILE_STOP(timer);
    *wrapped = false;
	if (destroy_attr) {
	    pthread_attr_destroy(&tmp);
	}
  }
  return retval;
}

extern "C"
int tau_pthread_join_wrapper(pthread_join_p pthread_join_call,
    pthread_t thread, void ** retval)
{
  TauInternalFunctionGuard protects_this_function;

  bool * wrapped = (bool*)pthread_getspecific(wrapper_flags_key);
  if (!wrapped) {
    wrapped = new bool;
    pthread_setspecific(wrapper_flags_key, (void*)wrapped);
    *wrapped = false;
  }

  int ret;
  if(*wrapped || Tau_global_getLightsOut()) {
    // Another wrapper has already intercepted the call so just pass through
    ret = pthread_join_call(thread, retval);
  } else {
    *wrapped = true;
    TAU_PROFILE_TIMER(timer, "pthread_join", "", TAU_DEFAULT);
    TAU_PROFILE_START(timer);
    ret = pthread_join_call(thread, retval);
    TAU_PROFILE_STOP(timer);
    *wrapped = false;
  }
  return ret;
}

struct tau_pthread_exit_wrapper_cleanup_args_t {
    void * timer;
    bool * wrapped;
};

// In order to time pthread_exit, we have to use a cleanup handler,
// since the exit call will otherwise prevent the timer stop from
// being reached.
extern "C"
void tau_pthread_exit_wrapper_cleanup(void * args) {
    tau_pthread_exit_wrapper_cleanup_args_t * exit_args = (tau_pthread_exit_wrapper_cleanup_args_t *)args;
    TAU_PROFILER_STOP(exit_args->timer);
    *(exit_args->wrapped) = false;
}

extern "C"
void tau_pthread_exit_wrapper(pthread_exit_p pthread_exit_call, void * value_ptr)
{
  TauInternalFunctionGuard protects_this_function;

  bool * wrapped = (bool*)pthread_getspecific(wrapper_flags_key);
  if (!wrapped) {
    wrapped = new bool;
    pthread_setspecific(wrapper_flags_key, (void*)wrapped);
    *wrapped = false;
  }

  if(*wrapped || Tau_global_getLightsOut()) {
    // Another wrapper has already intercepted the call so just pass through
    pthread_exit_call(value_ptr);
  } else {
    *wrapped = true;
    void * handle;
    TAU_PROFILER_CREATE(handle, "pthread_exit", "", TAU_DEFAULT);
    TAU_PROFILER_START(handle);
    tau_pthread_exit_wrapper_cleanup_args_t cleanup_args;
    cleanup_args.timer = handle;
    cleanup_args.wrapped = wrapped;
    pthread_cleanup_push(tau_pthread_exit_wrapper_cleanup, &cleanup_args);
    pthread_exit_call(value_ptr);
    pthread_cleanup_pop(1);
  }
}

#ifdef TAU_PTHREAD_BARRIER_AVAILABLE
extern "C"
int tau_pthread_barrier_wait_wrapper(pthread_barrier_wait_p pthread_barrier_wait_call,
    pthread_barrier_t * barrier)
{
  TauInternalFunctionGuard protects_this_function;

  bool * wrapped = (bool*)pthread_getspecific(wrapper_flags_key);
  if (!wrapped) {
    wrapped = new bool;
    pthread_setspecific(wrapper_flags_key, (void*)wrapped);
    *wrapped = false;
  }

  int retval;
  if(*wrapped || Tau_global_getLightsOut()) {
    // Another wrapper has already intercepted the call so just pass through
    retval = pthread_barrier_wait_call(barrier);
  } else {
    *wrapped = true;
    TAU_PROFILE_TIMER(timer, "pthread_barrier_wait", "", TAU_DEFAULT);
    TAU_PROFILE_START(timer);
    retval = pthread_barrier_wait_call(barrier);
    TAU_PROFILE_STOP(timer);
    *wrapped = false;
  }
  return retval;
}
#endif

extern "C"
int tau_pthread_create(pthread_t * threadp, const pthread_attr_t * attr,
    start_routine_p start_routine, void * arg)
{
  return tau_pthread_create_wrapper(pthread_create, threadp, attr, start_routine, arg);
}

extern "C"
int tau_pthread_join(pthread_t thread, void ** retval)
{
  return tau_pthread_join_wrapper(pthread_join, thread, retval);
}

extern "C"
void tau_pthread_exit(void * value_ptr)
{
  tau_pthread_exit_wrapper(pthread_exit, value_ptr);
}

#ifdef TAU_PTHREAD_BARRIER_AVAILABLE
extern "C"
int tau_pthread_barrier_wait(pthread_barrier_t * barrier)
{
  return tau_pthread_barrier_wait_wrapper(pthread_barrier_wait, barrier);
}

#endif /* TAU_PTHREAD_BARRIER_AVAILABLE */
