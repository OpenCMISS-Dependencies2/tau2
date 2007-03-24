/****************************************************************************
**			TAU Portable Profiling Package			   **
**			http://www.cs.uoregon.edu/research/tau	           **
*****************************************************************************
**    Copyright 1997  						   	   **
**    Department of Computer and Information Science, University of Oregon **
**    Advanced Computing Laboratory, Los Alamos National Laboratory        **
****************************************************************************/
/***************************************************************************
**	File 		: RtsLayer.cpp					  **
**	Description 	: TAU Profiling Package RTS Layer definitions     **
**	Author		: Sameer Shende					  **
**	Contact		: sameer@cs.uoregon.edu sameer@acl.lanl.gov 	  **
**	Flags		: Compile with				          **
**			  -DPROFILING_ON to enable profiling (ESSENTIAL)  **
**			  -DPROFILE_STATS for Std. Deviation of Excl Time **
**			  -DSGI_HW_COUNTERS for using SGI counters 	  **
**			  -DPROFILE_CALLS  for trace of each invocation   **
**			  -DSGI_TIMERS  for SGI fast nanosecs timer	  **
**			  -DTULIP_TIMERS for non-sgi Platform	 	  **
**			  -DPOOMA_STDSTL for using STD STL in POOMA src   **
**			  -DPOOMA_TFLOP for Intel Teraflop at SNL/NM 	  **
**			  -DPOOMA_KAI for KCC compiler 			  **
**			  -DDEBUG_PROF  for internal debugging messages   **
**                        -DPROFILE_CALLSTACK to enable callstack traces  **
**	Documentation	: See http://www.cs.uoregon.edu/research/tau      **
***************************************************************************/


//////////////////////////////////////////////////////////////////////
// Include Files 
//////////////////////////////////////////////////////////////////////

//#define DEBUG_PROF
#ifdef TAU_AIX
#include "Profile/aix.h" 
#endif /* TAU_AIX */
#ifdef FUJITSU
#include "Profile/fujitsu.h"
#endif /* FUJITSU */
#ifdef TAU_HITACHI
#include "Profile/hitachi.h"
#endif /* HITACHI */
#include "Profile/Profiler.h"
#ifdef TAU_WINDOWS
 #define TAUROOT "root"
 #define TAU_ARCH "win32"
#else
 #include "tauroot.h"
 #include "tauarch.h"
#endif

#if (defined(__QK_USER__) || defined(__LIBCATAMOUNT__ ))
#ifndef TAU_CATAMOUNT
#define TAU_CATAMOUNT 
#endif /* TAU_CATAMOUNT */
#include <catamount/dclock.h>
#endif /* __QK_USER__ || __LIBCATAMOUNT__ */

#ifdef CRAY_TIMERS
#ifndef TAU_CATAMOUNT
/* These header files are for Cray X1 */
#include <intrinsics.h>
#include <sys/param.h>
#endif /* TAU_CATAMOUNT */
#endif // CRAY_TIMERS

#ifdef BGL_TIMERS
/* header files for BlueGene/L */
#include <bglpersonality.h>
#include <rts.h>
#endif // BGL_TIMERS

#ifdef TAU_XLC
#define strcasecmp strcmp
#define strncasecmp strncmp 
#endif /* TAU_XLC */


#ifdef TAU_DOT_H_LESS_HEADERS
#include <iostream>
using namespace std;
#else /* TAU_DOT_H_LESS_HEADERS */
#include <iostream.h>
#endif /* TAU_DOT_H_LESS_HEADERS */

#include <stdio.h> 
#include <fcntl.h>
#include <time.h>
#include <stdlib.h>
#ifdef CPU_TIME
#include <sys/time.h>
#include <sys/resource.h>
#include <unistd.h>
#endif // CPU_TIME

#ifdef JAVA_CPU_TIME
#include "Profile/JavaThreadLayer.h"
#endif // JAVA_CPU_TIME


#ifdef TAU_WINDOWS
//include the header for windows time functions.
#include <Windows.h>	//Various defines needed in Winbase.h.
#include <Winbase.h>	//For QueryPerformanceCounter/Frequency function (down to microsecond
						//resolution depending on the platform. 
#include <sys/timeb.h>	//For _ftime function (millisecond resolution).
//Map strncasecmp and strcasecmp to strnicmp and stricmp.
#define strcasecmp stricmp
#define strncasecmp strnicmp  
#endif //TAU_WINDOWS

#if (!defined(TAU_WINDOWS))
#include <unistd.h>

#if (defined(POOMA_TFLOP) || !defined(TULIP_TIMERS))
#include <sys/time.h>
#else
#ifdef TULIP_TIMERS 
#include "Profile/TulipTimers.h"
#endif //TULIP_TIMERS 
#endif //POOMA_TFLOP

#endif //TAU_WINDOWS

#ifdef TRACING_ON
#define PCXX_EVENT_SRC
#include "Profile/pcxx_events.h"
#endif // TRACING_ON 

#ifdef TAUKTAU
#include <Profile/ktau_timer.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>
#include <asm/unistd.h>
#endif /* TAUKTAU */

/////////////////////////////////////////////////////////////////////////
// Member Function Definitions For class RtsLayer
// Important for Porting to other platforms and frameworks.
/////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////////////
TauGroup_t& RtsLayer::TheProfileMask(void)
{ // to avoid initialization problems of non-local static variables
  static TauGroup_t ProfileMask = TAU_DEFAULT;

  return ProfileMask;
}

/////////////////////////////////////////////////////////////////////////
bool& RtsLayer::TheEnableInstrumentation(void)
{ // to avoid initialization problems of non-local static variables
  static bool EnableInstrumentation = true;

  return EnableInstrumentation;
}

/////////////////////////////////////////////////////////////////////////
long RtsLayer::GenerateUniqueId(void)
{ /* This routine is called in a locked region (RtsLayer::LockDB/UnLockDB)*/
  static long UniqueId = 0;
  return ++UniqueId;
}

/////////////////////////////////////////////////////////////////////////
int& RtsLayer::TheNode(void)
{
#ifdef TAU_SETNODE0
  static int Node = 0;
#else /* TAU_SETNODE0  */
  static int Node =-1;
#endif /* TAU_SETNODE0 */
 
  return Node;
}

/////////////////////////////////////////////////////////////////////////
int& RtsLayer::TheContext(void)
{
  static int Context = 0;
 
  return Context;
}

/////////////////////////////////////////////////////////////////////////

bool& RtsLayer::TheShutdown(void) {
  static bool shutdown = false;
  return shutdown;
}

/////////////////////////////////////////////////////////////////////////

ProfileMap_t& RtsLayer::TheProfileMap(void) {
  static ProfileMap_t *profilemap = new ProfileMap_t;
  
  return *profilemap;
}


/////////////////////////////////////////////////////////////////////////

TauGroup_t RtsLayer::getProfileGroup(char * ProfileGroup) {
  ProfileMap_t::iterator it = TheProfileMap().find(string(ProfileGroup));
  TauGroup_t gr;
  if (it == TheProfileMap().end())
  {
#ifdef DEBUG_PROF
    cout <<ProfileGroup << " not found, adding ... "<<endl;
#endif /* DEBUG_PROF */
    gr = generateProfileGroup();
    TheProfileMap()[string(ProfileGroup)] = gr; // Add
    return gr; 
  }
  else
    return (*it).second; // The group that was found

}

/////////////////////////////////////////////////////////////////////////

TauGroup_t RtsLayer::disableProfileGroupName(char * ProfileGroup) {

  return disableProfileGroup(getProfileGroup(ProfileGroup)); 

}

/////////////////////////////////////////////////////////////////////////

TauGroup_t RtsLayer::enableProfileGroupName(char * ProfileGroup) {

  return enableProfileGroup(getProfileGroup(ProfileGroup));

}

/////////////////////////////////////////////////////////////////////////

TauGroup_t RtsLayer::generateProfileGroup(void) {
  static TauGroup_t key =  0x00000001;
  key = key << 1;
  if (key == 0x0) key = 0x1; // cycle
  return key;
}

/////////////////////////////////////////////////////////////////////////

TauGroup_t RtsLayer::enableProfileGroup(TauGroup_t ProfileGroup) {
  TheProfileMask() |= ProfileGroup; // Add it to the mask
  DEBUGPROFMSG("enableProfileGroup " << ProfileGroup <<" Mask = " 
	<< TheProfileMask() << endl;);
  return TheProfileMask();
}

/////////////////////////////////////////////////////////////////////////

TauGroup_t RtsLayer::enableAllGroups(void) {
  TheProfileMask() = TAU_DEFAULT; // make all bits 1 
  DEBUGPROFMSG("enableAllGroups " << " Mask = " << TheProfileMask() << endl;);
  return TheProfileMask();
}

/////////////////////////////////////////////////////////////////////////

TauGroup_t RtsLayer::disableAllGroups(void) {
  TheProfileMask() = 0; // make all bits 1 
  DEBUGPROFMSG("disableAllGroups " << " Mask = " << TheProfileMask() << endl;);
  return TheProfileMask();
}

/////////////////////////////////////////////////////////////////////////

TauGroup_t RtsLayer::disableProfileGroup(TauGroup_t ProfileGroup) {
  if (TheProfileMask() & ProfileGroup) { // if it is already set 
    TheProfileMask() ^= ProfileGroup; // Delete it from the mask
    DEBUGPROFMSG("disableProfileGroup " << ProfileGroup <<" Mask = " 
	<< TheProfileMask() << endl;);
  } // if it is not in the mask, disableProfileGroup does nothing 
  return TheProfileMask();
}

/////////////////////////////////////////////////////////////////////////

TauGroup_t RtsLayer::resetProfileGroup(void) {
  TheProfileMask() = 0;
  return TheProfileMask();
}

/////////////////////////////////////////////////////////////////////////
int RtsLayer::setMyNode(int NodeId, int tid) {
#if (defined(TRACING_ON) && (TAU_MAX_THREADS != 1))
  int oldid = TheNode();
  int newid = NodeId;
  if ((oldid != -1) && (oldid != newid))
  { /* ie if SET_NODE macro was invoked twice for a threaded program : as 
    in MPI+JAVA where JAVA initializes it with pid and then MPI_INIT is 
    invoked several thousand events later, and TAU computes the process rank
    and invokes the SET_NODE with the correct rank. Handshaking between multiple
    levels of instrumentation. */
    
    TraceReinitialize(oldid, newid, tid); 
  } 
#endif // TRACING WITH THREADS
  TheNode() = NodeId;
// At this stage, we should create the trace file because we know the node id
#ifdef TRACING_ON
#ifdef TAU_VAMPIRTRACE
// Vampirtrace specific function not needed here 
#else
#ifdef TAU_EPILOG
// EPILOG specific function not needed here. 
#else /* TAU_EPILOG */
  TraceEvInit(tid);
#endif /* TAU_EPILOG */
#endif /* TAU_VAMPIRTRACE */
#endif // TRACING_ON
  return TheNode();
}

/////////////////////////////////////////////////////////////////////////
int RtsLayer::setMyContext(int ContextId) {
  TheContext() = ContextId;
  return TheContext();
}

/////////////////////////////////////////////////////////////////////////

bool RtsLayer::isEnabled(TauGroup_t ProfileGroup) {
TauGroup_t res =  ProfileGroup & TheProfileMask() ;

  if (res > 0)
    return true;
  else
    return false;
}

//////////////////////////////////////////////////////////////////////

#ifdef SGI_HW_COUNTERS 
extern "C" {
  int start_counters( int e0, int e1 );
  int read_counters( int e0, long long *c0, int e1, long long *c1);
};
#endif // SGI_HW_COUNTERS

//////////////////////////////////////////////////////////////////////
#ifdef SGI_HW_COUNTERS 
int RtsLayer::SetEventCounter()
{
  int e0, e1;
  int start;


  e0 = 0;
  e1 = 0;


  int x0, x1;
  // 
  // DO NOT remove the following two lines. Otherwise start_counters 
  // crashes with "prioctl PIOCENEVCTRS returns error: Invalid argument"


  x0 = e0; 
  x1 = e1; 


  if((start = start_counters(e0,e1)) < 0) {
    perror("start_counters");
    exit(0);
  }
  return start;
}
#endif // SGI_HW_COUNTERS

/////////////////////////////////////////////////////////////////////////
#ifdef SGI_HW_COUNTERS 
double RtsLayer::GetEventCounter()
{
  static int gen_start = SetEventCounter();
  int gen_read;
  int e0 = 0, e1 = 0;
  long long c0 , c1 ;
  static double accum = 0;

  if ((gen_read = read_counters(e0, &c0, e1, &c1)) < 0) {
    perror("read_counters");
  }

  if (gen_read != gen_start) {
    perror("lost counter! aborting...");
    exit(1);
  }

  accum += c0;
  DEBUGPROFMSG("Read counters e0 " << e0 <<" e1 "<< e1<<" gen_read " 
    << gen_read << " gen_start = " << gen_start << " accum "<< accum 
    << " c0 " << c0 << " c1 " << c1 << endl;);
  gen_start = SetEventCounter(); // Reset the counter

  return accum;
}
#endif //SGI_HW_COUNTERS

///////////////////////////////////////////////////////////////////////////
double getUserTimeInSec(void)
{
  double current_time = 0;
#ifdef CPU_TIME
  
  struct rusage current_usage;

  getrusage (RUSAGE_SELF, &current_usage);
  
/* user time
  current_time = current_usage.ru_utime.tv_sec * 1e6 
	       + current_usage.ru_utime.tv_usec;
*/
  current_time = (current_usage.ru_utime.tv_sec + current_usage.ru_stime.tv_sec)* 1e6 
  + (current_usage.ru_utime.tv_usec + current_usage.ru_stime.tv_usec);
#endif // CPU_TIME
  return current_time; 
}

#ifdef TAU_LINUX_TIMERS

///////////////////////////////////////////////////////////////////////////
int TauReadFullLine(char *line, FILE *fp) {
  int ch, i;
  i = 0; 
  while ( (ch = fgetc(fp)) && ch != EOF && ch != (int) '\n') {
    line[i++] = (unsigned char) ch;
  }
  line[i] = '\0'; 
  if (ch == EOF) {
    return -1;
  }
  return i; 
}

///////////////////////////////////////////////////////////////////////////
double TauGetMHzRatings(void) {
  float ret = 0;
  char line[2048];
  FILE *fp = fopen("/proc/cpuinfo", "r");

  if (fp) {
    while (TauReadFullLine(line, fp) != -1) {
      if (strncmp(line, "cpu MHz", 7) == 0) {
        sscanf(line,"cpu MHz         : %f", &ret);
        return (double) ret; 
      }
      if (strncmp(line, "timebase", 8) == 0) {
        sscanf(line,"timebase        : %f", &ret);
        return (double) ret / 1.0e6; 
      }
    }
  } else {
    perror("/proc/cpuinfo file not found:");
  }
  return (double) ret;
}
  
///////////////////////////////////////////////////////////////////////////
inline double TauGetMHz(void)
{
  static double ratings = TauGetMHzRatings();
  return ratings;
}
///////////////////////////////////////////////////////////////////////////
extern "C" unsigned long long getLinuxHighResolutionTscCounter(void);
// Moved to TauLinuxTimers.c 

#endif /* TAU_LINUX_TIMERS */

#if defined(TAUKTAU) || defined(TAUKTAU_MERGE)
///////////////////////////////////////////////////////////////////////////
double KTauGetMHz(void)
{
#ifdef KTAU_WALLCLOCK
  static double ktau_ratings = 1; //(microsec resolution from kernel)
#else
  static double ktau_ratings = cycles_per_sec()/1000000; //we need ratings per microsec to match tau's reporting
  //static double ktau_ratings = TauGetMHz(); //we need ratings per microsec to match tau's reporting
#endif
  return ktau_ratings;
}
#endif /* TAUKTAU || TAUKTAU_MERGE */

///////////////////////////////////////////////////////////////////////////
double TauWindowsUsecD(void)
{
#ifdef TAU_WINDOWS
  
  //First need to find out whether we have performance
  //clock, and if so, the frequency.
  static bool PerfClockCheckedBefore = false;
  static bool PerformanceClock = false;
  static LARGE_INTEGER Frequency;
  LARGE_INTEGER ClockValue;
  double FinalClockValue = 0;
  static double Multiplier = 0;

  //Intializing!
  ClockValue.HighPart = 0;
  ClockValue.LowPart = 0;
  ClockValue.QuadPart = 0;

  //Testing clock.  This will only be done ONCE!
  if(!PerfClockCheckedBefore)
  {
	  //Intializing!
	  Frequency.HighPart = 0;
	  Frequency.LowPart = 0;
	  Frequency.QuadPart = 0;
	  
	  PerformanceClock = QueryPerformanceFrequency(&Frequency);
	  PerfClockCheckedBefore = true;
	  if(PerformanceClock)
	  {
#ifdef DEBUG_PROF
		  cout << "Frequency high part is: " << Frequency.HighPart << endl;
		  cout << "Frequency low part is: " << Frequency.LowPart << endl;
		  cout << "Frequency quad part is: " << (double) Frequency.QuadPart << endl;			
#endif /* DEBUG_PROF */
		  //Shall be using Frequency.QuadPart and assuming a double as the main TAU
		  //system does.
		  
		  //Checking for zero divide ... should not be one if the clock is working,
		  //but need to be on the safe side!
		  if(Frequency.QuadPart != 0)
		  {
			  Multiplier = (double) 1000000/Frequency.QuadPart;
			  cout << "The value of the multiplier is: " << Multiplier << endl;
		  }
		  else
		  {
			  cout << "There was a problem with the counter ... should not have happened!!" << endl;
			  return -1;
		  }
	  }
	  else
		  cout << "No performace clock available ... using millisecond timers." << endl;
  }

  //Getting clock value.
  if(PerformanceClock)
  {
	  if(QueryPerformanceCounter(&ClockValue))
	  {
		  //As mentioned above, assuming double value.
		  return Multiplier * (double) ClockValue.QuadPart;
	  }
	  else
	  {
		  cout << "There was a problem with the counter ... should not have happened!!" << endl;
		  return -1;
	  }
  }
  else
  {
	  struct _timeb tp;
	  _ftime(&tp);
	  return ( (double) tp.time * 1e6 + tp.millitm * 1e3);
  }
#else  /* TAU_WINDOWS */
  return 0; 
#endif /* TAU_WINDOWS */
}
///////////////////////////////////////////////////////////////////////////

#ifdef TAUKTAU_MERGE
  //declare the sys_ktau_gettimeofday syscall
  //#define __NR_ktau_gettimeofday ???
  //_syscall2(int,ktau_gettimeofday,struct timeval *,tv,struct timezone *,tz);
  extern "C" int ktau_gettimeofday(struct timeval *tv, struct timezone *tz);
#endif // TAUKTAU_MERGE 

#ifdef TAU_MULTIPLE_COUNTERS
void RtsLayer::getUSecD (int tid, double *values){
#if ((defined(TAU_EPILOG) && !defined(PROFILING_ON)) || (defined(TAU_VAMPIRTRACE) && !defined(PROFILING_ON)))
  return;
#endif /* TAU_EPILOG/VAMPIRTRACE, PROFILING_ON */
  MultipleCounterLayer::getCounters(tid, values);
}
#else //TAU_MULTIPLE_COUNTERS
double RtsLayer::getUSecD (int tid) {

#if ((defined(TAU_EPILOG) && !defined(PROFILING_ON)) || (defined(TAU_VAMPIRTRACE) && !defined(PROFILING_ON)))
  return 0;
#endif /* TAU_EPILOG/VAMPIRTRACE, PROFILING_ON */


#ifdef TAU_PCL
  return PCL_Layer::getCounters(tid);
#else  // TAU_PCL
#ifdef TAU_PAPI
  static const char *papi_env = getenv("PAPI_EVENT");
  if (papi_env != NULL)
    return PapiLayer::getSingleCounter(tid);
#ifdef TAU_PAPI_WALLCLOCKTIME
  return PapiLayer::getWallClockTime();
#else /* TAU_PAPI_WALLCLOCKTIME */
#ifdef TAU_PAPI_VIRTUAL
  return PapiLayer::getVirtualTime();
#else  /* TAU_PAPI_VIRTUAL */
  return PapiLayer::getSingleCounter(tid);
#endif /* TAU_PAPI_VIRTUAL */
#endif /* TAU_PAPI_WALLCLOCKTIME */
#else  // TAU_PAPI
#ifdef CPU_TIME
  return getUserTimeInSec();
#else // CPU_TIME
#ifdef JAVA_CPU_TIME
  return JavaThreadLayer::getCurrentThreadCpuTime();
#else // JAVA_CPU_TIME
#ifdef TAUKTAU_MERGE
  struct timeval tp;
  static double last_timestamp = 0.0;
  double timestamp;
  ktau_gettimeofday (&tp, 0);
  timestamp = (double) tp.tv_sec * 1e6 + tp.tv_usec;
  if (timestamp < last_timestamp)
  {
     DEBUGPROFMSG("RtsLayer::getUSecD(): ktau_gettimeofday() goes back in time. Fixing ...."<<endl;);
     timestamp = last_timestamp;
  }
  last_timestamp = timestamp;
  return timestamp;
#else // TAUKTAU_MERGE
#ifdef BGL_TIMERS
  static double bgl_clockspeed = 0.0;

  if (bgl_clockspeed == 0.0)
  {
    BGLPersonality mybgl;
    rts_get_personality(&mybgl, sizeof(BGLPersonality));
    bgl_clockspeed = 1.0e6/(double)BGLPersonality_clockHz(&mybgl);
  }
  return (rts_get_timebase() * bgl_clockspeed);
#else // BGL_TIMERS
#ifdef SGI_HW_COUNTERS
  return RtsLayer::GetEventCounter();
#else  //SGI_HW_COUNTERS

#ifdef SGI_TIMERS
  struct timespec tp;
  clock_gettime(CLOCK_SGI_CYCLE,&tp);
  return (tp.tv_sec * 1e6 + (tp.tv_nsec * 1e-3)) ;

#else  // SGI_TIMERS
#ifdef CRAY_TIMERS
#ifdef TAU_CATAMOUNT /* for Cray XT3 */
  return dclock()*1.0e6; 
#else /* for Cray X1 */
  long long tick = _rtc();
  return (double) tick/HZ;
#endif /* TAU_CATAMOUNT */
#endif // CRAY_TIMERS
#ifdef TAU_ALPHA_TIMERS
  struct timespec currenttime;
  clock_gettime(CLOCK_REALTIME, &currenttime);
  return (currenttime.tv_sec * 1e6 + (currenttime.tv_nsec * 1e-3));
#endif /* TAU_ALPHA_TIMERS */
#ifdef TAU_LINUX_TIMERS
  return (double) getLinuxHighResolutionTscCounter()/TauGetMHz();
#else /* TAU_LINUX_TIMERS */
#if (defined(POOMA_TFLOP) || !defined(TULIP_TIMERS)) 
#if (defined(TAU_WINDOWS))
  return TauWindowsUsecD();
#else // TAU_WINDOWS 
#ifdef TAU_MUSE
#ifdef DEBUG_PROF
  // TO CHECK IF THE VALUE IS MONOTONICALLY INCREASING
  double queryValue = 0.0;
  static double lastQueryValue = 0.0;
  queryValue = TauMuseQuery();
  char msg[200];
  if(queryValue < lastQueryValue){
        if(queryValue < 0){
		DEBUGPROFMSG("TauMuseQuery() came out negative!!!!!."<<endl;);
        }else{
		DEBUGPROFMSG("TauMuseQuery() less than lastQueryValue.!!!!!"<<endl;);
		sprintf(msg,"TauMuseQuery() lastQueryValue=%f\n",lastQueryValue);
		DEBUGPROFMSG(msg);
		sprintf(msg,"TauMuseQuery() queryValue=%f\n",queryValue);
		DEBUGPROFMSG(msg);
        }
        queryValue = lastQueryValue;
  }
  lastQueryValue = queryValue;
  return queryValue;
#else //DEBUG_PROF
  
  return TauMuseQuery();

#endif //DEBUG_PROF
#else /* TAU_MUSE */
#ifdef TAU_MUSE_MULTIPLE
#ifdef DEBUG_PROF
  // TO CHECK IF THE VALUE IS MONOTONICALLY INCREASING
  double queryValue = 0.0;
  static double lastQueryValue = 0.0;
  char msg[200];
  double data[10];
  int size = 10;
  queryValue = TauMuseMultipleQuery(data,size);
  if(queryValue < lastQueryValue){
        if(queryValue < 0){
		DEBUGPROFMSG("TauMuseQuery() came out negative!!!!!."<<endl;);
        }else{
		DEBUGPROFMSG("TauMuseQuery() less than lastQueryValue.!!!!!"<<endl;);
		sprintf(msg,"TauMuseQuery() lastQueryValue=%f\n",lastQueryValue);
		DEBUGPROFMSG(msg);
		sprintf(msg,"TauMuseQuery() queryValue=%f\n",queryValue);
		DEBUGPROFMSG(msg);
        }
        queryValue = lastQueryValue;
  }
  lastQueryValue = queryValue;
  return queryValue;
#else //DEBUG_PROF
 
  double data[10];
  int size=10; 
  return TauMuseMultipleQuery(data,size);

#endif //DEBUG_PROF
#else //TAU_MUSE_MULTIPLE
  struct timeval tp;
  static double last_timestamp = 0.0;
  double timestamp;
  gettimeofday (&tp, 0);
  timestamp = (double) tp.tv_sec * 1e6 + tp.tv_usec;
  if (timestamp < last_timestamp)
  {
     DEBUGPROFMSG("RtsLayer::getUSecD(): gettimeofday() goes back in time. Fixing ...."<<endl;);
     timestamp = last_timestamp;
  }
  last_timestamp = timestamp;
  return timestamp;
#endif // TAU_MUSE_EVENT
#endif // TAU_MUSE 
#endif // TAU_WINDOWS
#else  // TULIP_TIMERS by default.  
  return pcxx_GetUSecD();
#endif // POOMA_TFLOP
#endif // TAU_LINUX_TIMERS
#endif // SGI_TIMERS

#endif // SGI_HW_COUNTERS
#endif // BGL_TIMERS
#endif // TAUKTAU_MERGE
#endif // JAVA_CPU_TIME
#endif // CPU_TIME
#endif // TAU_PAPI
#endif // TAU_PCL
}
#endif //TAU_MULTIPLE_COUNTERS

///////////////////////////////////////////////////////////////////////////
//Note: This is similar to Tulip event classes during tracing
///////////////////////////////////////////////////////////////////////////
int RtsLayer::setAndParseProfileGroups(char *prog, char *str)
{
  char *end;
  
  if ( str )
  { 
    while (str && *str) 
    {
      if ( ( end = strchr (str, '+')) != NULL) *end = '\0';
 
      switch ( str[0] )
      {
	      /*
        case 'a' :
	case 'A' : // Assign Expression Evaluation Group
	  if (strncasecmp(str,"ac", 2) == 0) {
	    RtsLayer::enableProfileGroup(TAU_ACLMPL); 
	    // ACLMPL enabled 
	  } 
	  else 
	    RtsLayer::enableProfileGroup(TAU_ASSIGN);
	  break;
	case 'b' : 
	case 'B' : // Blitz++ profile group
	  RtsLayer::enableProfileGroup(TAU_BLITZ);
	  break; // Blitz++ enabled
        case 'f' :
	case 'F' : // Field Group
	  if (strncasecmp(str, "ff", 2) == 0) {
	    RtsLayer::enableProfileGroup(TAU_FFT);
	    // FFT enabled 
	  }
	  else 
	    RtsLayer::enableProfileGroup(TAU_FIELD);
	    // Field enabled 
	  break;
	case 'c' :
	case 'C' : 
	  RtsLayer::enableProfileGroup(TAU_COMMUNICATION);
	  break;
 	case 'h' :
	case 'H' :
	  RtsLayer::enableProfileGroup(TAU_HPCXX);
	  break;
        case 'i' :
	case 'I' : // DiskIO, Other IO 
	  RtsLayer::enableProfileGroup(TAU_IO);
	  break;
        case 'l' :
	case 'L' : // Field Layout Group
	  RtsLayer::enableProfileGroup(TAU_LAYOUT);
	  break;
	case 'm' : 
	case 'M' : 
          if (strncasecmp(str,"mesh", 4) == 0) {
  	    RtsLayer::enableProfileGroup(TAU_MESHES);
	    // Meshes enabled
 	  } 
 	  else 
	    RtsLayer::enableProfileGroup(TAU_MESSAGE);
	    // Message Profile Group enabled 
  	  break;
        case 'p' :
	case 'P' : 
          if (strncasecmp(str, "paws1", 5) == 0) {
	    RtsLayer::enableProfileGroup(TAU_PAWS1); 
	  } 
	  else {
	    if (strncasecmp(str, "paws2", 5) == 0) {
	      RtsLayer::enableProfileGroup(TAU_PAWS2); 
	    } 
	    else {
	      if (strncasecmp(str, "paws3", 5) == 0) {
	        RtsLayer::enableProfileGroup(TAU_PAWS3); 
	      } 
	      else {
	        if (strncasecmp(str,"pa",2) == 0) {
	          RtsLayer::enableProfileGroup(TAU_PARTICLE);
	          // Particle enabled 
	        } 
		else {
	          RtsLayer::enableProfileGroup(TAU_PETE);
	    	  // PETE Profile Group enabled 
	 	}
	      }
	    } 
 	  } 
	  
	  break;
  	case 'r' : 
	case 'R' : // Region Group 
	  RtsLayer::enableProfileGroup(TAU_REGION);
	  break;
        case 's' :
	case 'S' : 
	  if (strncasecmp(str,"su",2) == 0) {
	    RtsLayer::enableProfileGroup(TAU_SUBFIELD);
	    // SubField enabled 
	  } 
 	  else
	    RtsLayer::enableProfileGroup(TAU_SPARSE);
	    // Sparse Index Group
	  break;
        case 'd' :
	case 'D' : // Domainmap Group
	  if (strncasecmp(str,"de",2) == 0) {
	    RtsLayer::enableProfileGroup(TAU_DESCRIPTOR_OVERHEAD);
	  } else  
	     RtsLayer::enableProfileGroup(TAU_DOMAINMAP);
	  break;
 	case 'u' :
        case 'U' : // User or Utility 
          if (strncasecmp(str,"ut", 2) == 0) { 
	    RtsLayer::enableProfileGroup(TAU_UTILITY);
	  }
	  else // default - for u is USER 
 	    RtsLayer::enableProfileGroup(TAU_USER);
	  break;
        case 'v' :
	case 'V' : // ACLVIZ Group
	  RtsLayer::enableProfileGroup(TAU_VIZ);
	  break;
	  // Old stuff. Delete it!
	  */
 	case '0' :
	  RtsLayer::enableProfileGroup(TAU_GROUP_0);
	  printf("ENABLING 0!\n");
	  break;
	case '1' : // User1
	  switch (str[1])
	  {
	    case '0':
	      RtsLayer::enableProfileGroup(TAU_GROUP_10);
	      break; 
	    case '1':
	      RtsLayer::enableProfileGroup(TAU_GROUP_11);
	      break; 
	    case '2':
	      RtsLayer::enableProfileGroup(TAU_GROUP_12);
	      break; 
	    case '3':
	      RtsLayer::enableProfileGroup(TAU_GROUP_13);
	      break; 
	    case '4':
	      RtsLayer::enableProfileGroup(TAU_GROUP_14);
	      break; 
	    case '5':
	      RtsLayer::enableProfileGroup(TAU_GROUP_15);
	      break; 
	    case '6':
	      RtsLayer::enableProfileGroup(TAU_GROUP_16);
	      break; 
	    case '7':
	      RtsLayer::enableProfileGroup(TAU_GROUP_17);
	      break; 
	    case '8':
	      RtsLayer::enableProfileGroup(TAU_GROUP_18);
	      break; 
	    case '9':
	      RtsLayer::enableProfileGroup(TAU_GROUP_19);
	      break; 
	    default :
	      RtsLayer::enableProfileGroup(TAU_GROUP_1);
	      break; 
	  }
	  break;
	 
	case '2' : // User2
          switch (str[1])
          {
            case '0':
              RtsLayer::enableProfileGroup(TAU_GROUP_20);
              break;
            case '1':
              RtsLayer::enableProfileGroup(TAU_GROUP_21);
              break;
            case '2':
              RtsLayer::enableProfileGroup(TAU_GROUP_22);
              break;
            case '3':
              RtsLayer::enableProfileGroup(TAU_GROUP_23);
              break;
            case '4':
              RtsLayer::enableProfileGroup(TAU_GROUP_24);
              break;
            case '5':
              RtsLayer::enableProfileGroup(TAU_GROUP_25);
              break;
            case '6':
              RtsLayer::enableProfileGroup(TAU_GROUP_26);
              break; 
            case '7':
              RtsLayer::enableProfileGroup(TAU_GROUP_27);
              break;
            case '8':
              RtsLayer::enableProfileGroup(TAU_GROUP_28);
              break;
            case '9':
              RtsLayer::enableProfileGroup(TAU_GROUP_29);
              break;
            default :
              RtsLayer::enableProfileGroup(TAU_GROUP_2);
              break;
	  }
	  break;
	case '3' : // User3
          switch (str[1])
          {
            case '0':
              RtsLayer::enableProfileGroup(TAU_GROUP_30);
              break;
            case '1':
              RtsLayer::enableProfileGroup(TAU_GROUP_31);
              break;
            default :
              RtsLayer::enableProfileGroup(TAU_GROUP_3);
              break;
	  }
	  break;
	case '4' : // User4
	  RtsLayer::enableProfileGroup(TAU_GROUP_4);
	  break;
	case '5' : 
	  RtsLayer::enableProfileGroup(TAU_GROUP_5);
	  break;
	case '6' : 
	  RtsLayer::enableProfileGroup(TAU_GROUP_6);
	  break;
	case '7' : 
	  RtsLayer::enableProfileGroup(TAU_GROUP_7);
	  break;
	case '8' : 
	  RtsLayer::enableProfileGroup(TAU_GROUP_8);
	  break;
	case '9' : 
	  RtsLayer::enableProfileGroup(TAU_GROUP_9);
	  break;

	default  :
	  RtsLayer::enableProfileGroupName(str);
	  break; 
      } 
      if (( str = end) != NULL) *str++ = '+';
    }
  }
  else 
    enableProfileGroup(TAU_DEFAULT); // Enable everything 
  return 1;
}

//////////////////////////////////////////////////////////////////////
void RtsLayer::ProfileInit(int& argc, char**& argv)
{
  int i;
  int ret_argc;
  char **ret_argv;

#ifdef TAU_COMPENSATE
#ifndef TAU_MULTIPLE_COUNTERS
  double tover = TauGetTimerOverhead(TauNullTimerOverhead);
  if (tover < 0) tover = 0;
#else /* TAU_MULTIPLE_COUNTERS */
  double* tover = TauGetTimerOverhead(TauNullTimerOverhead);
  for (i = 0; i < MAX_TAU_COUNTERS; i++)
  { /* iterate through all counters and reset null overhead to zero 
       if necessary */
    if (tover[i] < 0) tover[i] = 0;
  }
#endif /* TAU_MULTIPLE_COUNTERS */
#endif /* TAU_COMPENSATE */
  
  ret_argc = 1;
  ret_argv = new char *[argc];
  ret_argv[0] = argv[0]; // The program name 

  for(i=1; i < argc; i++) {
    if ( ( strcasecmp(argv[i], "--profile") == 0 ) ) {
        // Enable the profile groups
        if ( (i + 1) < argc && argv[i+1][0] != '-' )  { // options follow
           RtsLayer::resetProfileGroup(); // set it to blank
           RtsLayer::setAndParseProfileGroups(argv[0], argv[i+1]);
	   i++; // ignore the argv after --profile 
        }
    }
    else
    {
	ret_argv[ret_argc++] = argv[i];
    }
  }
  argc = ret_argc;
  argv = ret_argv;
  return;
}


//////////////////////////////////////////////////////////////////////
bool RtsLayer::isCtorDtor(const char *name)
{
  return false;
  // For now, we always return false. So, no matter what, the profile file
  // is always created! 
}

//////////////////////////////////////////////////////////////////////
// PrimaryGroup returns the first group that the function belongs to.
// This is needed in tracing as Vampir can handle only one group per
// function. PrimaryGroup("TAU_FIELD | TAU_USER") should return "TAU_FIELD"
//////////////////////////////////////////////////////////////////////
string RtsLayer::PrimaryGroup(const char *ProfileGroupName) 
{
  string groups = ProfileGroupName;
  string primary; 
  string separators = " |"; 
  int start, stop, n;

  start = groups.find_first_not_of(separators, 0);
  n = groups.length();
  stop = groups.find_first_of(separators, start); 

  if ((stop < 0) || (stop > n)) stop = n;

  primary = groups.substr(start, stop - start) ;
  return primary;

}

//////////////////////////////////////////////////////////////////////
// TraceSendMsg traces the message send
//////////////////////////////////////////////////////////////////////
void RtsLayer::TraceSendMsg(int type, int destination, int length)
{
#ifdef TRACING_ON 
  x_int64 parameter;
  x_uint64 xother, xtype, xlength, xcomm;

  if (RtsLayer::isEnabled(TAU_MESSAGE))
  {
    parameter = 0;
    /* for send, othernode is receiver or destination */
    xtype = type;
    xlength = length;
    xother = destination;
    xcomm = 0;

    /* Format for parameter is
       63 ..... 56 55 ..... 48 47............. 32
          other       type          length

       These are the high order bits, below are the low order bits

       31 ..... 24 23 ..... 16 15..............0
          other       type          length       

       e.g.

       xtype = 0xAABB;
       xother = 0xCCDD;
       xlength = 0xDEADBEEF;
       result = 0xccaaDEADdddbbBEEF

     parameter = ((xlength >> 16) << 32) | 
       ((xtype >> 8 & 0xFF) << 48) |
       ((xother >> 8 & 0xFF) << 56) |
       (xlength & 0xFFFF) | 
       ((xtype & 0xFF)  << 16) | 
       ((xother & 0xFF) << 24);

     */

    parameter = (xlength >> 16 << 54 >> 22) |
      ((xtype >> 8 & 0xFF) << 48) |
      ((xother >> 8 & 0xFF) << 56) |
      (xlength & 0xFFFF) | 
      ((xtype & 0xFF)  << 16) | 
      ((xother & 0xFF) << 24) |
      (xcomm << 58 >> 16);


    pcxx_Event(TAU_MESSAGE_SEND, parameter); 
#ifdef DEBUG_PROF
    printf("Node %d TraceSendMsg, type %x dest %x len %x par %lx \n", 
  	RtsLayer::myNode(), type, destination, length, parameter);
#endif //DEBUG_PROF
  } 
#endif //TRACING_ON
}

  
//////////////////////////////////////////////////////////////////////
// TraceRecvMsg traces the message recv
//////////////////////////////////////////////////////////////////////
void RtsLayer::TraceRecvMsg(int type, int source, int length)
{
#ifdef TRACING_ON
  x_int64 parameter;
  x_uint64 xother, xtype, xlength, xcomm;


  if (RtsLayer::isEnabled(TAU_MESSAGE)) 
  {
    parameter = 0;
    /* for recv, othernode is sender or source*/
    xtype = type;
    xlength = length;
    xother = source;
    xcomm = 0;

    // see TraceSendMsg for documentation

    parameter = (xlength >> 16 << 54 >> 22) |
      ((xtype >> 8 & 0xFF) << 48) |
      ((xother >> 8 & 0xFF) << 56) |
      (xlength & 0xFFFF) | 
      ((xtype & 0xFF)  << 16) | 
      ((xother & 0xFF) << 24) |
      (xcomm << 58 >> 16);


    pcxx_Event(TAU_MESSAGE_RECV, parameter); 
  
#ifdef DEBUG_PROF
    printf("Node %d TraceRecvMsg, type %x src %x len %x par %lx \n", 
  	RtsLayer::myNode(), type, source, length, parameter);
#endif //DEBUG_PROF
  }
#endif //TRACING_ON
}

//////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////
// DumpEDF() writes the function information in the edf.<node> file
// The function info consists of functionId, group, name, type, parameters
//////////////////////////////////////////////////////////////////////
int RtsLayer::DumpEDF(int tid)
{
#ifdef TRACING_ON 
  	vector<FunctionInfo*>::iterator it;
  	vector<TauUserEvent*>::iterator uit;
	char filename[1024], errormsg[1024];
	char *dirname;
	FILE* fp;
	int  numEvents, numExtra;


	if (tid != 0) 
	{ 
#ifdef DEBUG_PROF
	  printf("DumpEDF: FlushEvents = %d\n",GetFlushEvents(tid));
#endif /* DEBUG_PROF */
	  if (GetFlushEvents(tid) == 0)
	    return 1; 
	}
	RtsLayer::LockDB();
	// Only thread 0 on a node should write the edf files.
	if ((dirname = getenv("TRACEDIR")) == NULL) {
	// Use default directory name .
	   dirname  = new char[8];
	   strcpy (dirname,".");
	}

	sprintf(filename,"%s/events.%d.edf",dirname, RtsLayer::myNode());
	DEBUGPROFMSG("Creating " << filename << endl;);
	if ((fp = fopen (filename, "w+")) == NULL) {
		sprintf(errormsg,"Error: Could not create %s",filename);
		perror(errormsg);
		return 0;
	}
	
	// Data Format 
	// <no.> events
	// # or \n ignored
	// %s %s %d "%s %s" %s 
	// id group tag "name type" parameters

	numExtra = 9; // Number of extra events
	/* OLD
	numEvents = TheFunctionDB().size();
	*/
	numEvents = TheFunctionDB().size() + TheEventDB().size();

	numEvents += numExtra;

	fprintf(fp,"%d dynamic_trace_events\n", numEvents);

	fprintf(fp,"# FunctionId Group Tag \"Name Type\" Parameters\n");

 	for (it = TheFunctionDB().begin(); 
	  it != TheFunctionDB().end(); it++)
	{
  	  DEBUGPROFMSG("Node: "<< RtsLayer::myNode() <<  " Dumping EDF Id : " 
	    << (*it)->GetFunctionId() << " " << (*it)->GetPrimaryGroup() 
	    << " 0 " << (*it)->GetName() << " " << (*it)->GetType() 
	    << " EntryExit" << endl;); 
	
	  fprintf(fp, "%ld %s 0 \"%s %s\" EntryExit\n", (*it)->GetFunctionId(),
	    (*it)->GetPrimaryGroup(), (*it)->GetName(), (*it)->GetType() );
	}

	/* Now write the user defined event */
 	for (uit = TheEventDB().begin(); 
	  uit != TheEventDB().end(); uit++)
	{
	  int monoinc = 0; 
	  if ((*uit)->GetMonotonicallyIncreasing())
	  { /* if it is true */
	    monoinc = 1;
	  }
  	  DEBUGPROFMSG("Node: "<< RtsLayer::myNode() <<  " Dumping EDF Id : " 
	    << (*uit)->GetEventId() << " " 
	    << monoinc<<" " << (*uit)->GetEventName() << " " 
	    << " TriggerValue" << endl;); 
	
	  fprintf(fp, "%ld TAUEVENT %d \"%s\" TriggerValue\n", (*uit)->GetEventId(), monoinc, (*uit)->GetEventName());
	}
	// Now add the nine extra events 
	fprintf(fp,"%ld TRACER 0 \"EV_INIT\" none\n", (long) PCXX_EV_INIT); 
	fprintf(fp,"%ld TRACER 0 \"FLUSH_ENTER\" none\n", (long) PCXX_EV_FLUSH_ENTER); 
	fprintf(fp,"%ld TRACER 0 \"FLUSH_EXIT\" none\n", (long) PCXX_EV_FLUSH_EXIT); 
	fprintf(fp,"%ld TRACER 0 \"FLUSH_CLOSE\" none\n", (long) PCXX_EV_CLOSE); 
	fprintf(fp,"%ld TRACER 0 \"FLUSH_INITM\" none\n", (long) PCXX_EV_INITM); 
	fprintf(fp,"%ld TRACER 0 \"WALL_CLOCK\" none\n", (long) PCXX_EV_WALL_CLOCK); 
	fprintf(fp,"%ld TRACER 0 \"CONT_EVENT\" none\n", (long) PCXX_EV_CONT_EVENT); 
	fprintf(fp,"%ld TAU_MESSAGE -7 \"MESSAGE_SEND\" par\n", (long) TAU_MESSAGE_SEND); 
	fprintf(fp,"%ld TAU_MESSAGE -8 \"MESSAGE_RECV\" par\n", (long) TAU_MESSAGE_RECV); 

  
	fclose(fp);
	RtsLayer::UnLockDB();
#endif //TRACING_ON
	return 1;
}

//////////////////////////////////////////////////////////////////////
// MergeAndConvertTracesIfNecessary does just that!
//////////////////////////////////////////////////////////////////////

int RtsLayer::MergeAndConvertTracesIfNecessary(void)
{ 
  char *outfile;
  /* Get environment variables */
  if ((outfile = getenv("TAU_TRACEFILE")) != NULL)
  { /* output file is defined. We need to merge the traces */
    /* Now, who does the merge and conversion? */
    if ((myNode() == 0) && (myThread() == 0))
    {
      char *outdir;
      char *keepfiles;
      char cmd[1024];
      char rmcmd[256]; 
      char cdcmd[1024];
      char *tauroot=TAUROOT;
      char *tauarch=TAU_ARCH;
      char *conv="tau2vtf";
      char converter[1024] = {0}; 
      FILE *in;
  
      /* If we can't find tau2vtf, use tau_convert! */
      sprintf(converter, "%s/%s/bin/%s",tauroot, tauarch, conv);
      if ((in = fopen(converter, "r")) == NULL)
      {
#ifdef DEBUG_PROF
        printf("Couldn't open %s\n", converter);
#endif /* DEBUG_PROF */
        sprintf(converter, "%s/%s/bin/tau_convert", tauroot, tauarch);
      }
      else
      { /* close it */
        fclose(in);
      }

      /* Should we get rid of intermediate trace files? */
      if((keepfiles = getenv("TAU_KEEP_TRACEFILES")) == NULL)
      {
	strcpy(rmcmd, "/bin/rm -f app12345678.trc tautrace.*.trc tau.edf events.*.edf");
      }
      else
      { 
	strcpy(rmcmd," "); /* NOOP */
      }

      /* Next, look for trace directory */
      if ((outdir = getenv("TRACEDIR")) != NULL)
      { /* change directory to outdir */
        sprintf(cdcmd, "cd %s;", outdir);
      }
      else
      {
	strcpy(cdcmd, " ");
      }

      /* create the command */
      sprintf(cmd, "%s /bin/rm -f app12345678.trc; %s/%s/bin/tau_merge tautrace.*.trc app12345678.trc; %s app12345678.trc tau.edf %s; %s", cdcmd,tauroot, tauarch, converter, outfile, rmcmd);
#ifdef DEBUG_PROF
      printf("The merge/convert cmd is: %s\n", cmd);
#endif /* DEBUG_PROF */

      /* and execute it */
#ifndef TAU_CATAMOUNT
/* NOTE: BGL will not execute this code as well because the compute node 
   kernels cannot fork tasks. So, on BGL, nothing will happen when the 
   following system command executes */
      system(cmd);
#endif /* TAU_CATAMOUNT */
    } /* on node 0, thread 0 */
  } /* if output file is defined */
  else 
  { /* output file not defined, just exit normally */
    return 0;
  }
  return 1;
}

#ifdef __GNUC__
#ifndef NO_RTTI
#include <cxxabi.h>
#endif /* NO_RTTI */
#endif /* __GNUC__ */

/////////////////////////////////////////////////////////////////////////
std::string RtsLayer::GetRTTI(const char *name)
{
#ifdef __GNUC__
#ifndef NO_RTTI
  std::size_t len;
  int stat;
  char *ptr = NULL;
  const std::string mangled = name;
  return abi::__cxa_demangle(mangled.c_str(), ptr, &len, &stat);
#else /* NO_RTTI */
  return string(name);
#endif /* NO_RTTI */
#else
  return string(CheckNotNull(name));
#endif /* GNUC */
}

/***************************************************************************
 * $RCSfile: RtsLayer.cpp,v $   $Author: sameer $
 * $Revision: 1.84 $   $Date: 2007/03/24 01:04:02 $
 * POOMA_VERSION_ID: $Id: RtsLayer.cpp,v 1.84 2007/03/24 01:04:02 sameer Exp $ 
 ***************************************************************************/
