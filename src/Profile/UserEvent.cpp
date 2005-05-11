/****************************************************************************
**			TAU Portable Profiling Package			   **
**			http://www.acl.lanl.gov/tau		           **
*****************************************************************************
**    Copyright 1997  						   	   **
**    Department of Computer and Information Science, University of Oregon **
**    Advanced Computing Laboratory, Los Alamos National Laboratory        **
****************************************************************************/
/***************************************************************************
**	File 		: UserEvent.cpp					  **
**	Description 	: TAU Profiling Package				  **
**	Author		: Sameer Shende					  **
**	Contact		: sameer@cs.uoregon.edu sameer@acl.lanl.gov 	  **
**	Flags		: Compile with				          **
**			  -DPROFILING_ON to enable profiling (ESSENTIAL)  **
**			  -DPROFILE_STATS for Std. Deviation of Excl Time **
**			  -DSGI_HW_COUNTERS for using SGI counters 	  **
**			  -DPROFILE_CALLS  for trace of each invocation   **
**                        -DSGI_TIMERS  for SGI fast nanosecs timer       **
**			  -DTULIP_TIMERS for non-sgi Platform	 	  **
**			  -DPOOMA_STDSTL for using STD STL in POOMA src   **
**			  -DPOOMA_TFLOP for Intel Teraflop at SNL/NM 	  **
**			  -DPOOMA_KAI for KCC compiler 			  **
**			  -DDEBUG_PROF  for internal debugging messages   **
**                        -DPROFILE_CALLSTACK to enable callstack traces  **
**	Documentation	: See http://www.acl.lanl.gov/tau	          **
***************************************************************************/

//////////////////////////////////////////////////////////////////////
// Note: The default behavior of this library is to calculate all the
// statistics (min, max, mean, stddev, etc.) If the user wishes to 
// override these settings, SetDisableXXX routines can be used to do so
//////////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////////////////////////
// Include Files 
//////////////////////////////////////////////////////////////////////

//#define DEBUG_PROF

#include "Profile/Profiler.h"


#ifdef TAU_WINDOWS
  typedef __int64 x_int64;
  typedef unsigned __int64 x_uint64;
#else
  typedef long long x_int64;
  typedef unsigned long long x_uint64;
#endif


#include <stdio.h>
#include <fcntl.h>

//#include <math.h>
#ifdef TAU_DOT_H_LESS_HEADERS
#include <iostream>
using namespace std;
#else /* TAU_DOT_H_LESS_HEADERS */
#include <iostream.h>
#endif /* TAU_DOT_H_LESS_HEADERS */

#ifdef TRACING_ON
#ifdef TAU_EPILOG
#include "elg_trc.h"
#else /* TAU_EPILOG */
#define PCXX_EVENT_SRC
#include "Profile/pcxx_events.h"
#endif /* TAU_EPILOG */
#endif // TRACING_ON 

#ifdef PGI
template void vector<TauUserEvent *>::insert_aux(vector<TauUserEvent *>::iterator, TauUserEvent *const &);
template TauUserEvent** copy_backward(TauUserEvent**,TauUserEvent**,TauUserEvent**);
template TauUserEvent** uninitialized_copy(TauUserEvent**,TauUserEvent**,TauUserEvent**);
#endif // PGI

vector<TauUserEvent*>& TheEventDB(void)
{
  static vector<TauUserEvent*> EventDB;

  return EventDB;
}

// Add User Event to the EventDB
void TauUserEvent::AddEventToDB()
{
  RtsLayer::LockDB();
  TheEventDB().push_back(this);
  DEBUGPROFMSG("Successfully registered event " << GetEventName() << endl;);
  DEBUGPROFMSG("Size of eventDB is " << TheEventDB().size() <<endl);
  /* Set user event id */
  EventId = RtsLayer::GenerateUniqueId();
  RtsLayer::UnLockDB();
  return;
}

// Constructor 
long TauUserEvent::GetEventId(void) 
{
  return EventId;
}

// Constructor 
TauUserEvent::TauUserEvent(const char * EName, bool increasing)
{
  DEBUGPROFMSG("Inside ctor of TauUserEvent EName = "<< EName << endl;);

  EventName 	= EName;
  // Assign event name and then set the default values
  DisableMin 	= false; 	// Min 	    is calculated 
  DisableMax 	= false; 	// Max      is calculated 
  DisableMean 	= false; 	// Mean     is calculated 
  DisableStdDev = false; 	// StdDev   is calculated
  MonotonicallyIncreasing = increasing; // By default it is false 

  for(int i=0; i < TAU_MAX_THREADS; i++) 
  {
    LastValueRecorded[i] = 0;  	// null to start with
    NumEvents[i] = 0L; 		// initialize
    MinValue[i]  = 9999999;  	// Least -ve value? limits.h
    MaxValue[i]  = -9999999;		// Greatest  +ve value?    "
    SumSqrValue[i]  = 0;		// initialize
    SumValue[i]     = 0; 		// initialize
  }

  AddEventToDB();
  // Register this event in the main event database 
}

// Copy Constructor 
TauUserEvent::TauUserEvent(TauUserEvent& X)
{
  DEBUGPROFMSG("Inside copy ctor TauUserEvent::TauUserEvent()" << endl;);

  EventName 	= X.EventName;
  DisableMin	= X.DisableMin;
  DisableMax 	= X.DisableMax;
  DisableMean	= X.DisableMean;
  DisableStdDev = X.DisableStdDev;
  MonotonicallyIncreasing = X.MonotonicallyIncreasing;
/* Do we really need these? 
  LastValueRecorded = X.LastValueRecorded;
  NumEvents	= X.NumEvents;
  MinValue	= X.MinValue;
  MaxValue	= X.MaxValue;
  SumSqrValue	= X.SumSqrValue;
  SumValue	= X.SumValue;
 */

  AddEventToDB(); 
  //Register this event
}

// Default constructor
TauUserEvent::TauUserEvent()
{
  EventName 	= string("No Name");
  DisableMin 	= false; 	// Min 	    is calculated 
  DisableMax 	= false; 	// Max      is calculated 
  DisableMean 	= false; 	// Mean     is calculated 
  DisableStdDev = false; 	// StdDev   is calculated
  MonotonicallyIncreasing = false; // By default it does not have any constraints

  for (int i=0; i < TAU_MAX_THREADS; i++)
  {
    LastValueRecorded[i] = 0;  	// null to start with
    NumEvents[i] = 0L; 		// initialize
    MinValue[i]  = 9999999;  	// Least -ve value? limits.h
    MaxValue[i]  = -9999999;		// Greatest  +ve value?    "
    SumSqrValue[i]  = 0;		// initialize
    SumValue[i]     = 0; 		// initialize
  } 

  AddEventToDB();
  // Register this event in the main event database 
}

// Assignment operator
TauUserEvent& TauUserEvent::operator= (const TauUserEvent& X)
{

  DEBUGPROFMSG("Inside TauUserEvent::operator= (const TauUserEvent& X)" << endl;);

  EventName 	= X.EventName;
  DisableMin	= X.DisableMin;
  DisableMax 	= X.DisableMax;
  DisableMean	= X.DisableMean;
  DisableStdDev = X.DisableStdDev;
/* do we really need these? 
  LastValueRecorded = X.LastValueRecorded;
  NumEvents	= X.NumEvents;
  MinValue	= X.MinValue;
  MaxValue	= X.MaxValue;
  SumSqrValue	= X.SumSqrValue;
  SumValue	= X.SumValue;
*/

  return *this;
}

///////////////////////////////////////////////////////////
// GetMonotonicallyIncreasing
///////////////////////////////////////////////////////////
bool TauUserEvent::GetMonotonicallyIncreasing(void)
{
  return MonotonicallyIncreasing; 
}

///////////////////////////////////////////////////////////
// SetMonotonicallyIncreasing
///////////////////////////////////////////////////////////
void TauUserEvent::SetMonotonicallyIncreasing(bool value)
{
  MonotonicallyIncreasing = value; 
}

///////////////////////////////////////////////////////////
// TriggerEvent records the value of data in the UserEvent
///////////////////////////////////////////////////////////

void TauUserEvent::TriggerEvent(TAU_EVENT_DATATYPE data, int tid)
{ 
#ifdef TRACING_ON
#ifndef TAU_EPILOG
  TraceEvent(GetEventId(), (x_uint64) 0, tid, 0, 0); 
  TraceEvent(GetEventId(), (x_uint64) data, tid, 0, 0); 
  TraceEvent(GetEventId(), (x_uint64) 0, tid, 0, 0); 
#endif /* TAU_EPILOG */
  /* Timestamp is 0, and use_ts is 0, so tracing layer gets timestamp */
#endif /* TRACING_ON */

#ifdef PROFILING_ON
  // Record this value  
  LastValueRecorded[tid] = data;

  // Increment number of events
  NumEvents[tid] ++;

  // Compute relevant statistics for the data 
  if (!GetDisableMin()) 
  {  // Min is not disabled
     if (NumEvents[tid] > 1) {
     	MinValue[tid] = data < MinValue[tid] ? data : MinValue[tid];
     } else
	MinValue[tid] = data;
  }
  
  if (!GetDisableMax())
  {  // Max is not disabled
     if (NumEvents[tid] > 1) {
       MaxValue[tid] = MaxValue[tid] < data ? data : MaxValue[tid];
     } else
       MaxValue[tid] = data;
  }

  if (!GetDisableMean())
  {  // Mean is not disabled 
     SumValue[tid] += data; 
  }
     
  if (!GetDisableStdDev())
  {  // Standard Deviation is not disabled
     SumSqrValue[tid] += data*data; 
  }

#endif /* PROFILING_ON */
  return; // completed calculating statistics for this event
}

// Return the data stored in the class
TAU_EVENT_DATATYPE TauUserEvent::GetMin(int tid)
{ 
  if (NumEvents[tid] != 0L)
  { 
    return MinValue[tid];
  }
  else
    return 0;
}

TAU_EVENT_DATATYPE TauUserEvent::GetMax(int tid)
{
  if (NumEvents[tid] != 0L)
  {
    return MaxValue[tid];
  }
  else
    return 0;
}

TAU_EVENT_DATATYPE TauUserEvent::GetSumValue(int tid)
{  
  if (NumEvents[tid] != 0L)
  {
    return SumValue[tid];
  }
  else
    return 0;
}

TAU_EVENT_DATATYPE TauUserEvent::GetMean(int tid)
{
  if (NumEvents[tid] != 0L) 
  {
    return (SumValue[tid]/NumEvents[tid]);
  } 
  else
    return 0;
}

double TauUserEvent::GetSumSqr(int tid)
{
  return (SumSqrValue[tid]);
}

long TauUserEvent::GetNumEvents(int tid)
{
  return NumEvents[tid];
}

// Get the event name
const char * TauUserEvent::GetEventName (void) const
{
  return EventName.c_str();
}

// Set the event name
void TauUserEvent::SetEventName (const char *newname)
{
  EventName = newname;
}

// Set the event name
void TauUserEvent::SetEventName (string newname)
{
  EventName = newname;
}

bool TauUserEvent::GetDisableMin(void)
{ 
  return DisableMin;
}

bool TauUserEvent::GetDisableMax(void)
{
  return DisableMax;
}

bool TauUserEvent::GetDisableMean(void)
{
  return DisableMean;
}

bool TauUserEvent::GetDisableStdDev(void)
{
  return DisableStdDev;
}

// Set Routines
void TauUserEvent::SetDisableMin(bool value)
{
  DisableMin = value;
  return;
}

void TauUserEvent::SetDisableMax(bool value)
{
  DisableMax = value;
  return;
}

void TauUserEvent::SetDisableMean(bool value)
{
  DisableMean = value;
  return;
}

void TauUserEvent::SetDisableStdDev(bool value)
{
  DisableStdDev = value;
  return;
}

TauUserEvent::~TauUserEvent(void)
{
  DEBUGPROFMSG(" DTOR CALLED for " << GetEventName() << endl;); 
}

void TauUserEvent::ReportStatistics(bool ForEachThread)
{
  TAU_EVENT_DATATYPE TotalNumEvents, TotalSumValue, Minima, Maxima ;
  vector<TauUserEvent*>::iterator it;

  Maxima = Minima = 0;
  cout << "TAU Runtime Statistics" <<endl;
  cout << "*************************************************************" << endl;

  for(it  = TheEventDB().begin(); it != TheEventDB().end(); it++)
  {
    DEBUGPROFMSG("TauUserEvent "<< 
      (*it)->GetEventName() << "\n Min " << (*it)->GetMin() << "\n Max " <<
      (*it)->GetMax() << "\n Mean " << (*it)->GetMean() << "\n Sum Sqr " <<
      (*it)->GetSumSqr() << "\n NumEvents " << (*it)->GetNumEvents()<< endl;);
      
    TotalNumEvents = TotalSumValue = 0;

    for (int tid = 0; tid < TAU_MAX_THREADS; tid++)
    { 
      if ((*it)->GetNumEvents(tid) > 0)
      { // There were some events on this thread 
        TotalNumEvents += (*it)->GetNumEvents(tid); 
	TotalSumValue  += (*it)->GetSumValue(tid);

        if (!(*it)->GetDisableMin())
        { // Min is not disabled
	  // take the lesser of Minima and the min on that thread
	  if (tid > 0) 
	  { // more than one thread
	    Minima = (*it)->GetMin(tid) < Minima ? (*it)->GetMin(tid) : Minima;
	  } 
	  else 
	  { // this is the first thread. Initialize Minima to the min on it.
	    Minima = (*it)->GetMin(tid);
	  }
	} 

	if (!(*it)->GetDisableMax())
	{ // Max is not disabled
	  // take the maximum of Maxima and max on that thread
	  if (tid > 0)
	  { // more than one thread 
	    Maxima = (*it)->GetMax(tid) > Maxima ? (*it)->GetMax(tid) : Maxima;
	  } 
	  else
	  { // this is the first thread. Initialize Maxima to the max on it.
	    Maxima = (*it)->GetMax(tid);
	  }
	}   
	  

	if (ForEachThread) 
	{ // true, print statistics for this thread
	  cout <<  "n,c,t "<<RtsLayer::myNode() <<"," <<RtsLayer::myContext()
	       <<  "," << tid << " : Event : "<< (*it)->GetEventName() << endl
	       <<  " Number : " << (*it)->GetNumEvents(tid) <<endl
	       <<  " Min    : " << (*it)->GetMin(tid) << endl
	       <<  " Max    : " << (*it)->GetMax(tid) << endl
	       <<  " Mean   : " << (*it)->GetMean(tid) << endl
	       <<  " Sum    : " << (*it)->GetSumValue(tid) << endl << endl;
	}
	
      } // there were no events on this thread 
    } // for all threads 
    


    cout << "*************************************************************" << endl;
    cout << "Cumulative Statistics over all threads for Node: "
	 << RtsLayer::myNode() << " Context: " << RtsLayer::myContext() << endl;
    cout << "*************************************************************" << endl;
    cout << "Event Name     = " << (*it)->GetEventName() << endl;
	        
    cout << "Total Number   = " << TotalNumEvents << endl;
    cout << "Total Value    = " << TotalSumValue << endl; 
    cout << "Minimum Value  = " << Minima << endl;
    cout << "Maximum Value  = " << Maxima << endl;
    cout << "-------------------------------------------------------------" <<endl;
    cout << endl;
 
  } // For all events
}

////////////////////////////////////////////////////////////////////////////
// We now implement support for user defined events that link with callpaths
////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////
// The datatypes and routines for maintaining a context map
////////////////////////////////////////////////////////////////////////////
#define TAU_CONTEXT_MAP_TYPE long *, TauUserEvent *, TaultUserEventLong

/////////////////////////////////////////////////////////////////////////
/* The comparison function for callpath requires the TaultUserEventLong struct
 * to be defined. The operator() method in this struct compares two callpaths.
 * Since it only compares two arrays of longs (containing addresses), we can
 * look at the callpath depth as the first index in the two arrays and see if
 * they're equal. If they two arrays have the same depth, then we iterate
 * through the array and compare each array element till the end */
/////////////////////////////////////////////////////////////////////////
struct TaultUserEventLong
{
  bool operator() (const long *l1, const long *l2) const
 {
   int i;
   /* first check 0th index (size) */
   if (l1[0] != l2[0]) return (l1[0] < l2[0]);
   /* they're equal, see the size and iterate */
   for (i = 0; i < l1[0] ; i++)
   {
     if (l1[i] != l2[i]) return l1[i] < l2[i];
   }
   return (l1[i] < l2[i]);
 }
};


/////////////////////////////////////////////////////////////////////////
// We use one global map to store the callpath information
/////////////////////////////////////////////////////////////////////////
map<TAU_CONTEXT_MAP_TYPE >& TheContextMap(void)
{ // to avoid initialization problems of non-local static variables
  static map<TAU_CONTEXT_MAP_TYPE > contextmap;

  return contextmap;
}

//////////////////////////////////////////////////////////////////////
#define TAU_DEFAULT_CONTEXT_CALLPATH_DEPTH 2

//////////////////////////////////////////////////////////////////////
// How deep should the callpath be? The default value is 2
//////////////////////////////////////////////////////////////////////
int& TauGetContextCallPathDepth(void)
{
  char *depth;
  static int value = 0;

  
  if (value == 0)
  {
    if ((depth = getenv("TAU_CALLPATH_DEPTH")) != NULL)
    {
      value = atoi(depth);
      if (value > 1)
      {
        return value;
      }
      else
      {
        value = TAU_DEFAULT_CONTEXT_CALLPATH_DEPTH;
        return value; /* default value */
      }
    }
    else
    {
      value = TAU_DEFAULT_CONTEXT_CALLPATH_DEPTH;
      return value;
    }
  }
  else
    return value;
}


    

////////////////////////////////////////////////////////////////////////////
// Formulate Context Comparison Array
//////////////////////////////////////////////////////////////////////
long* TauFormulateContextComparisonArray(Profiler *p)
{
  int depth = TauGetContextCallPathDepth();
  /* Create a long array with size depth+1. We need to put the depth
   * in it as the 0th index */
  long *ary = new long [depth+1];

  int i = 0;
  int j;
  Profiler *current = p; /* argument */

  /* initialize the array */
  for (j = 0; j < depth+1; j++)
  {
    ary[j] = 0L;
  }
  /* use the clean array now */

  if (ary)
  {
    ary[0] = depth; /* this tells us how deep it is */
    while (current != NULL && depth != 0)
    {
      i++; /* increment i */
      ary[i] = (long) current->ThisFunction;
      depth --;
      current = current->ParentProfiler;
    }
  }
  return ary;
}

////////////////////////////////////////////////////////////////////////////
// Formulate Context Callpath name string
////////////////////////////////////////////////////////////////////////////
string * TauFormulateContextNameString(Profiler *p)
{
  DEBUGPROFMSG("Inside TauFormulateContextNameString()"<<endl;);
  int depth = TauGetContextCallPathDepth();
  Profiler *current = p;
  string delimiter(" => ");
  string *name = new string("");

  while (current != NULL && depth != 0)
  {
    if (current != p)
      *name =  current->ThisFunction->GetName() + string(" ") +
               current->ThisFunction->GetType() + delimiter + *name;
    else
      *name =  current->ThisFunction->GetName() + string (" ") +
               current->ThisFunction->GetType();
    current = current->ParentProfiler;
    depth --;
  }
  DEBUGPROFMSG("TauFormulateContextNameString:Name: "<<*name <<endl;);
  return name;
}



////////////////////////////////////////////////////////////////////////////
// Ctor for TauContextUserEvent 
////////////////////////////////////////////////////////////////////////////
TauContextUserEvent::TauContextUserEvent(const char *EName, bool MonoIncr)
{
  /* create the event */
  uevent = new TauUserEvent(EName, MonoIncr);
  DisableContext = false; /* context tracking is enabled by default */
  MonotonicallyIncreasing = MonoIncr;
}

////////////////////////////////////////////////////////////////////////////
// Dtor for TauContextUserEvent 
////////////////////////////////////////////////////////////////////////////
TauContextUserEvent::~TauContextUserEvent()
{
  delete uevent; 
}

////////////////////////////////////////////////////////////////////////////
// SetDisableContext for TauContextUserEvent 
////////////////////////////////////////////////////////////////////////////
void TauContextUserEvent::SetDisableContext(bool value)
{
  /* set it */
  DisableContext = value;
}

////////////////////////////////////////////////////////////////////////////
// Trigger the context event
////////////////////////////////////////////////////////////////////////////
void TauContextUserEvent::TriggerEvent( TAU_EVENT_DATATYPE data, int tid)
{
  if (!DisableContext)
  {
    long *comparison = 0;
    TauUserEvent *ue;
    /* context tracking is enabled */
    Profiler *current = Profiler::CurrentProfiler[tid];
    comparison = TauFormulateContextComparisonArray(current); 

    map<TAU_CONTEXT_MAP_TYPE>::iterator it = TheContextMap().find(comparison);
    if (it == TheContextMap().end())
    {
      string contextname(uevent->EventName  + " : " + *TauFormulateContextNameString(current));
      DEBUGPROFMSG("Couldn't find string in map: "<<*comparison<<endl; );
      ue = new TauUserEvent((const char *)(contextname.c_str()), MonotonicallyIncreasing);
      TheContextMap().insert(map<TAU_CONTEXT_MAP_TYPE>::value_type(comparison, ue));
    }
    else
    {
      /* found it! Get the user defined event from the map */
      ue = (*it).second;
      delete comparison; // free up memory when name is found
    }
    /* Now we trigger this event */
    if (ue)
     ue->TriggerEvent(data, tid);
  }
  uevent->TriggerEvent(data, tid);
}

/***************************************************************************
 * $RCSfile: UserEvent.cpp,v $   $Author: sameer $
 * $Revision: 1.16 $   $Date: 2005/05/11 19:56:26 $
 * POOMA_VERSION_ID: $Id: UserEvent.cpp,v 1.16 2005/05/11 19:56:26 sameer Exp $ 
 ***************************************************************************/
