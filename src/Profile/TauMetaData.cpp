/****************************************************************************
**			TAU Portable Profiling Package			   **
**			http://www.cs.uoregon.edu/research/tau	           **
*****************************************************************************
**    Copyright 2007  						   	   **
**    Department of Computer and Information Science, University of Oregon **
**    Advanced Computing Laboratory, Los Alamos National Laboratory        **
****************************************************************************/
/****************************************************************************
**	File 		: MetaData.cpp  				   **
**	Description 	: TAU Profiling Package				   **
**	Author		: Alan Morris					   **
**	Contact		: tau-bugs@cs.uoregon.edu               	   **
**	Documentation	: See http://www.cs.uoregon.edu/research/tau       **
**                                                                         **
**      Description     : This file contains all the Metadata, XML and     **
**                        Snapshot related routines                        **
**                                                                         **
****************************************************************************/

#ifndef TAU_DISABLE_METADATA
#include "tau_config.h"
#if defined(TAU_WINDOWS)
double TauWindowsUsecD(); // from RtsLayer.cpp
#else
#include <sys/utsname.h> // for host identification (uname)
#include <unistd.h>
#include <sys/time.h>
#endif /* TAU_WINDOWS */
#endif /* TAU_DISABLE_METADATA */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include <sstream>

#include "tauarch.h"
#include <Profile/Profiler.h>
#include <Profile/tau_types.h>
#include <Profile/TauMetrics.h>
#include <TauUtil.h>
#include <TauXML.h>
#include <TauMetaData.h>
#if (defined(TAU_FUJITSU) && defined(TAU_MPI))
#include <mpi.h>
#include <mpi-ext.h>
#endif /* TAU_FUJITSU && TAU_MPI */

// Moved from header file
using namespace std;


#ifdef TAU_BGL
#include <rts.h>
#include <bglpersonality.h>
#endif /* TAU_BGL */


/* Re-enabled since we believe this is now working (2009-11-02) */
/* 
   #ifdef TAU_IBM_XLC_BGP
   #undef TAU_BGP
   #endif
*/
/* NOTE: IBM BG/P XLC does not work with metadata when it is compiled with -qpic=large */


#ifdef TAU_BGP
/* header files for BlueGene/P */
#include <bgp_personality.h>
#include <bgp_personality_inlines.h>
#include <kernel_interface.h>
#endif // TAU_BGP

#ifdef TAU_BGQ
#include <firmware/include/personality.h>
#include <spi/include/kernel/process.h>
#include <spi/include/kernel/location.h>
#ifdef __GNUC__
#include <hwi/include/bqc/A2_inlines.h>
#endif // __GNUC__
#include <hwi/include/common/uci.h>

static Personality_t tau_bgq_personality;
#define TAU_BGQ_TORUS_DIM 6
static int tau_torus_size[TAU_BGQ_TORUS_DIM];
static int tau_torus_coord[TAU_BGQ_TORUS_DIM];
static int tau_torus_wraparound[TAU_BGQ_TORUS_DIM];

int tau_bgq_init(void) {
  uint64_t network_options;

  Kernel_GetPersonality(&tau_bgq_personality, sizeof(Personality_t));

  tau_torus_size[0] = tau_bgq_personality.Network_Config.Anodes;
  tau_torus_size[1] = tau_bgq_personality.Network_Config.Bnodes;
  tau_torus_size[2] = tau_bgq_personality.Network_Config.Cnodes;
  tau_torus_size[3] = tau_bgq_personality.Network_Config.Dnodes;
  tau_torus_size[4] = tau_bgq_personality.Network_Config.Enodes;
  tau_torus_size[5] = 64;

  tau_torus_coord[0] = tau_bgq_personality.Network_Config.Anodes;
  tau_torus_coord[1] = tau_bgq_personality.Network_Config.Bnodes;
  tau_torus_coord[2] = tau_bgq_personality.Network_Config.Cnodes;
  tau_torus_coord[3] = tau_bgq_personality.Network_Config.Dnodes;
  tau_torus_coord[4] = tau_bgq_personality.Network_Config.Enodes;
  tau_torus_coord[5] = Kernel_ProcessorID();

  network_options = tau_bgq_personality.Network_Config.NetFlags;

  tau_torus_wraparound[0] = network_options & ND_ENABLE_TORUS_DIM_A;
  tau_torus_wraparound[1] = network_options & ND_ENABLE_TORUS_DIM_B;
  tau_torus_wraparound[2] = network_options & ND_ENABLE_TORUS_DIM_C;
  tau_torus_wraparound[3] = network_options & ND_ENABLE_TORUS_DIM_D;
  tau_torus_wraparound[4] = network_options & ND_ENABLE_TORUS_DIM_E;
  tau_torus_wraparound[5] = 0;

  return 1;
}

#endif /* TAU_BGQ */

#if (defined (TAU_CATAMOUNT) && defined (PTHREADS))
#define _BITS_PTHREADTYPES_H 1
#endif // TAU_CATAMOUNT, PTHREADS

#include <signal.h>
#include <stdarg.h>

class MetaDataRepo : public map<Tau_metadata_key,Tau_metadata_value_t*,Tau_Metadata_Compare> {
public :
  virtual ~MetaDataRepo() {
    Tau_destructor_trigger();
  }
};


// These come from Tau_metadata_register calls
map<Tau_metadata_key,Tau_metadata_value_t*,Tau_Metadata_Compare> &Tau_metadata_getMetaData(int tid) {
  static MetaDataRepo metadata[TAU_MAX_THREADS];
  return metadata[tid];
}

extern "C" void Tau_metadata_create_value(Tau_metadata_value_t** tmv, const Tau_metadata_type_t type) {
  // allocate a new value, and set the type
  (*tmv) = (Tau_metadata_value_t*)malloc(sizeof(Tau_metadata_value_t));
  (*tmv)->type = type;
  return ;
}

extern "C" void Tau_metadata_create_object(Tau_metadata_object_t** tmo, const char *name, Tau_metadata_value_t* value) {
  // allocate an object with one name and one value, and store the name and value
  (*tmo) = (Tau_metadata_object_t*)malloc(sizeof(Tau_metadata_object_t));
  (*tmo)->count = 1;
  (*tmo)->names = (char**)malloc(sizeof(char*)*1);
  (*tmo)->names[0] = strdup(name);
  (*tmo)->values = (Tau_metadata_value_t**)malloc(sizeof(Tau_metadata_value_t*)*1);
  (*tmo)->values[0] = value;
  return ;
}

extern "C" void Tau_metadata_create_array(Tau_metadata_array_t** tma, const int length) {
  // allocate an array of declared size
  (*tma) = (Tau_metadata_array_t*)malloc(sizeof(Tau_metadata_array_t));
  (*tma)->length = length;
  (*tma)->values = (Tau_metadata_value_t**)malloc(sizeof(Tau_metadata_value_t*) * length);
  return ;
}

extern "C" void Tau_metadata_array_put(Tau_metadata_value_t* tmv, const int index, Tau_metadata_value_t *value) {
  // put a value in the array. If the array is too small, reallocate it.
  Tau_metadata_array_t *tma = tmv->data.aval;
  if (tma->length <= index) {
    // issue a warning!
	TAU_VERBOSE("WARNING! Reallocating metadata array due to access beyond declared length!\n");
	tma->length = index+1;
    tma->values = (Tau_metadata_value_t**)realloc(tma->values, sizeof(Tau_metadata_value_t*) * tma->length);
  }
  tma->values[index] = value;
  return;
}

extern "C" void Tau_metadata_object_put(Tau_metadata_value_t* tmv, const char *name, Tau_metadata_value_t *value) {
  // get the object
  Tau_metadata_object_t *tmo = tmv->data.oval;
  // save the old count as the index
  int index = tmo->count;
  // increment the count
  tmo->count++;
  // reallocate the arrays
  tmo->names = (char**)realloc(tmo->names, sizeof(Tau_metadata_value_t*) * tmo->count);
  tmo->values = (Tau_metadata_value_t**)realloc(tmo->values, sizeof(Tau_metadata_value_t*) * tmo->count);
  // store the new tuple
  tmo->names[index] = strdup(name);
  tmo->values[index] = value;
  return;
}

extern "C" void Tau_metadata(const char *name, const char *value) {
#ifdef TAU_DISABLE_METADATA
  return;
#endif // TAU_DISABLE_METADATA
  // make the key
  Tau_metadata_key *key = new Tau_metadata_key();
  key->name = strdup(name);
  // make the value
  Tau_metadata_value_t* tmv = NULL;
  Tau_metadata_create_value(&tmv, TAU_METADATA_TYPE_STRING);
  tmv->data.cval = strdup(value);
  RtsLayer::LockDB();
  Tau_metadata_getMetaData(RtsLayer::myThread())[*key] = tmv;
  RtsLayer::UnLockDB();
}

void Tau_metadata_register(const char *name, int value) {
  char buf[256];
  sprintf (buf, "%d", value);
  Tau_metadata(name, buf);
}

void Tau_metadata_register(const char *name, const char *value) {
  Tau_global_incr_insideTAU();
  Tau_metadata(name, value);
  Tau_global_decr_insideTAU();
}


int Tau_metadata_fillMetaData() 
{
#ifdef TAU_DISABLE_METADATA
  return 0;
#else

  static int filled = 0;
  if (filled) {
    return 0;
  }
  filled = 1;


#ifdef TAU_WINDOWS
  const char *timeFormat = "%I64d";
#else
  const char *timeFormat = "%lld";
#endif // TAU_WINDOWS


  char tmpstr[4096];
  sprintf (tmpstr, timeFormat, TauMetrics_getInitialTimeStamp());
  Tau_metadata_register("Starting Timestamp", tmpstr);


  time_t theTime = time(NULL);
  struct tm *thisTime = gmtime(&theTime);
  strftime(tmpstr,4096,"%Y-%m-%dT%H:%M:%SZ", thisTime);
  Tau_metadata_register("UTC Time", tmpstr);


  thisTime = localtime(&theTime);
  char buf[4096];
  strftime (buf,4096,"%Y-%m-%dT%H:%M:%S", thisTime);

  char tzone[7];
  strftime (tzone, 7, "%z", thisTime);
  if (strlen(tzone) == 5) {
    tzone[6] = 0;
    tzone[5] = tzone[4];
    tzone[4] = tzone[3];
    tzone[3] = ':';
  }
  sprintf (tmpstr, "%s%s", buf, tzone);

  Tau_metadata_register("Local Time", tmpstr);

  // write out the timestamp (number of microseconds since epoch (unsigned long long)
  sprintf (tmpstr, timeFormat, TauMetrics_getTimeOfDay());
  Tau_metadata_register("Timestamp", tmpstr);


#ifndef TAU_WINDOWS
  // try to grab meta-data
  char hostname[4096];
  gethostname(hostname,4096);
  Tau_metadata_register("Hostname", hostname);

  struct utsname archinfo;

  uname (&archinfo);
  Tau_metadata_register("OS Name", archinfo.sysname);
  Tau_metadata_register("OS Version", archinfo.version);
  Tau_metadata_register("OS Release", archinfo.release);
  Tau_metadata_register("OS Machine", archinfo.machine);
  Tau_metadata_register("Node Name", archinfo.nodename);

  Tau_metadata_register("TAU Architecture", TAU_ARCH);
  Tau_metadata_register("TAU Config", TAU_CONFIG);
  Tau_metadata_register("TAU Makefile", TAU_MAKEFILE);
  Tau_metadata_register("TAU Version", TAU_VERSION);

  Tau_metadata_register("pid", (int)getpid());
#endif // windows

#ifdef TAU_BGL
  char bglbuffer[4096];
  char location[BGLPERSONALITY_MAX_LOCATION];
  BGLPersonality personality;

  rts_get_personality(&personality, sizeof(personality));
  BGLPersonality_getLocationString(&personality, location);

  sprintf (bglbuffer, "(%d,%d,%d)", BGLPersonality_xCoord(&personality),
      BGLPersonality_yCoord(&personality),
      BGLPersonality_zCoord(&personality));
  Tau_metadata_register("BGL Coords", bglbuffer);

  Tau_metadata_register("BGL Processor ID", rts_get_processor_id());

  sprintf (bglbuffer, "(%d,%d,%d)", BGLPersonality_xSize(&personality),
      BGLPersonality_ySize(&personality),
      BGLPersonality_zSize(&personality));
  Tau_metadata_register("BGL Size", bglbuffer);


  if (BGLPersonality_virtualNodeMode(&personality)) {
    Tau_metadata_register("BGL Node Mode", "Virtual");
  } else {
    Tau_metadata_register("BGL Node Mode", "Coprocessor");
  }

  sprintf (bglbuffer, "(%d,%d,%d)", BGLPersonality_isTorusX(&personality),
      BGLPersonality_isTorusY(&personality),
      BGLPersonality_isTorusZ(&personality));
  Tau_metadata_register("BGL isTorus", bglbuffer);

  Tau_metadata_register("BGL DDRSize", BGLPersonality_DDRSize(&personality));
  Tau_metadata_register("BGL DDRModuleType", personality.DDRModuleType);
  Tau_metadata_register("BGL Location", location);

  Tau_metadata_register("BGL rankInPset", BGLPersonality_rankInPset(&personality));
  Tau_metadata_register("BGL numNodesInPset", BGLPersonality_numNodesInPset(&personality));
  Tau_metadata_register("BGL psetNum", BGLPersonality_psetNum(&personality));
  Tau_metadata_register("BGL numPsets", BGLPersonality_numPsets(&personality));

  sprintf (bglbuffer, "(%d,%d,%d)", BGLPersonality_xPsetSize(&personality),
      BGLPersonality_yPsetSize(&personality),
      BGLPersonality_zPsetSize(&personality));
  Tau_metadata_register("BGL PsetSize", bglbuffer);

  sprintf (bglbuffer, "(%d,%d,%d)", BGLPersonality_xPsetOrigin(&personality),
      BGLPersonality_yPsetOrigin(&personality),
      BGLPersonality_zPsetOrigin(&personality));
  Tau_metadata_register("BGL PsetOrigin", bglbuffer);

  sprintf (bglbuffer, "(%d,%d,%d)", BGLPersonality_xPsetCoord(&personality),
      BGLPersonality_yPsetCoord(&personality),
      BGLPersonality_zPsetCoord(&personality));
  Tau_metadata_register("BGL PsetCoord", bglbuffer);
#endif /* TAU_BGL */

#ifdef TAU_BGP
  char bgpbuffer[4096];
  char location[BGPPERSONALITY_MAX_LOCATION];
  _BGP_Personality_t personality;

  Kernel_GetPersonality(&personality, sizeof(_BGP_Personality_t));
  BGP_Personality_getLocationString(&personality, location);

  sprintf (bgpbuffer, "(%d,%d,%d)", BGP_Personality_xCoord(&personality),
      BGP_Personality_yCoord(&personality),
      BGP_Personality_zCoord(&personality));
  Tau_metadata_register("BGP Coords", bgpbuffer);

  Tau_metadata_register("BGP Processor ID", Kernel_PhysicalProcessorID());

  sprintf (bgpbuffer, "(%d,%d,%d)", BGP_Personality_xSize(&personality),
      BGP_Personality_ySize(&personality),
      BGP_Personality_zSize(&personality));
  Tau_metadata_register("BGP Size", bgpbuffer);


  if (Kernel_ProcessCount() > 1) {
    Tau_metadata_register("BGP Node Mode", "Virtual");
  } else {
    sprintf(bgpbuffer, "Coprocessor (%d)", Kernel_ProcessCount());
    Tau_metadata_register("BGP Node Mode", bgpbuffer);
  }

  sprintf (bgpbuffer, "(%d,%d,%d)", BGP_Personality_isTorusX(&personality),
      BGP_Personality_isTorusY(&personality),
      BGP_Personality_isTorusZ(&personality));
  Tau_metadata_register("BGP isTorus", bgpbuffer);

  Tau_metadata_register("BGP DDRSize (MB)", BGP_Personality_DDRSizeMB(&personality));
  /* CHECK: 
     Tau_metadata_register("BGP DDRModuleType", personality.DDRModuleType);
   */
  Tau_metadata_register("BGP Location", location);

  Tau_metadata_register("BGP rankInPset", BGP_Personality_rankInPset(&personality));
  /*
     Tau_metadata_register("BGP numNodesInPset", Kernel_ProcessCount());
   */
  Tau_metadata_register("BGP psetSize", BGP_Personality_psetSize(&personality));
  Tau_metadata_register("BGP psetNum", BGP_Personality_psetNum(&personality));
  Tau_metadata_register("BGP numPsets", BGP_Personality_numComputeNodes(&personality));

  /* CHECK: 
     sprintf (bgpbuffer, "(%d,%d,%d)", BGP_Personality_xPsetSize(&personality),
     BGP_Personality_yPsetSize(&personality),
     BGP_Personality_zPsetSize(&personality));
     Tau_metadata_register("BGP PsetSize", bgpbuffer);

     sprintf (bgpbuffer, "(%d,%d,%d)", BGP_Personality_xPsetOrigin(&personality),
     BGP_Personality_yPsetOrigin(&personality),
     BGP_Personality_zPsetOrigin(&personality));
     Tau_metadata_register("BGP PsetOrigin", bgpbuffer);

     sprintf (bgpbuffer, "(%d,%d,%d)", BGP_Personality_xPsetCoord(&personality),
     BGP_Personality_yPsetCoord(&personality),
     BGP_Personality_zPsetCoord(&personality));
     Tau_metadata_register("BGP PsetCoord", bgpbuffer);
   */

#endif /* TAU_BGP */

#if (defined(TAU_FUJITSU) && defined(TAU_MPI))
  int xrank, yrank, zrank, xshape, yshape, zshape; 
  int retcode, dim; 
  char fbuffer[4096]; 


  retcode  = FJMPI_Topology_get_dimension(&dim);  
  if (retcode != MPI_SUCCESS) {
    fprintf(stderr, "FJMPI_Topology_get_dimension ERROR in TauMetaData.cpp\n");
    return 0;
  }

  switch (dim) {
  case 1: 
    retcode = FJMPI_Topology_rank2x(RtsLayer::mynode(), &xrank); 
    sprintf (fbuffer, "(%d)", xrank);
    break;
  case 2:
    retcode = FJMPI_Topology_rank2xy(RtsLayer::mynode(), &xrank, &yrank); 
    sprintf (fbuffer, "(%d,%d)", xrank, yrank);
    break;
  case 3:
    retcode = FJMPI_Topology_rank2xyz(RtsLayer::mynode(), &xrank, &yrank, &zrank); 
    sprintf (fbuffer, "(%d,%d,%d)", xrank, yrank, zrank);
    break;
  default:
    fprintf(stderr, "FJMPI_Topology_get_dimension ERROR in switch TauMetaData.cpp\n");
    return 0;
  }
  if (retcode != MPI_SUCCESS) {
    fprintf(stderr, "FJMPI_Topology_rank2x ERROR in switch TauMetaData.cpp\n");
    return 0;
  }

  Tau_metadata_register("FUJITSU Coords", fbuffer);
  retcode = FJMPI_Topology_get_shape(&xshape, &yshape, &zshape); 
  if (retcode != MPI_SUCCESS) {
    fprintf(stderr, "FJMPI_Topology_get_shape ERROR in TauMetaData.cpp\n");
    return 0;
  }


  sprintf (fbuffer, "(%d,%d,%d)", xshape, yshape, zshape);
  Tau_metadata_register("FUJITSU Size", fbuffer);

  sprintf (fbuffer, "%d", dim);
  Tau_metadata_register("FUJITSU Dimension", fbuffer);

#endif /* TAU_FUJITSU && TAU_MPI */

#ifdef TAU_BGQ
  /* NOTE: Please refer to Scalasca's elg_pform_bgq.c [www.scalasca.org] for 
     details on IBM BGQ Axis mapping. */
  static int bgq_init = tau_bgq_init(); 
  char bgqbuffer[4096];
  static char tau_axis_map[] = "EFABCD";  
  /* EF -> x, AB -> y, CD -> z */

#define TAU_BGQ_IDX(i) tau_axis_map[i] - 'A'

  int x = tau_torus_coord[TAU_BGQ_IDX(0)] * tau_torus_size[TAU_BGQ_IDX(1)] 	
    + tau_torus_coord[TAU_BGQ_IDX(1)]; 
  int y = tau_torus_coord[TAU_BGQ_IDX(2)] * tau_torus_size[TAU_BGQ_IDX(3)] 
    + tau_torus_coord[TAU_BGQ_IDX(3)]; 
  int z = tau_torus_coord[TAU_BGQ_IDX(4)] * tau_torus_size[TAU_BGQ_IDX(5)]
    + tau_torus_coord[TAU_BGQ_IDX(5)]; 

  sprintf(bgqbuffer, "(%d,%d,%d)", x,y,z);
  Tau_metadata_register("BGQ Coords", bgqbuffer);

  int size_x = tau_torus_size[TAU_BGQ_IDX(0)] * tau_torus_size[TAU_BGQ_IDX(1)];
  int size_y = tau_torus_size[TAU_BGQ_IDX(2)] * tau_torus_size[TAU_BGQ_IDX(3)];
  int size_z = tau_torus_size[TAU_BGQ_IDX(4)] * tau_torus_size[TAU_BGQ_IDX(5)];

  sprintf(bgqbuffer, "(%d,%d,%d,%d,%d,%d)", tau_torus_size[0], tau_torus_size[1], tau_torus_size[2],
      tau_torus_size[3], tau_torus_size[4], tau_torus_size[5]);
  Tau_metadata_register("BGQ Size", bgqbuffer);

  int wrap_x = tau_torus_wraparound[TAU_BGQ_IDX(0)] && tau_torus_wraparound[TAU_BGQ_IDX(1)]; 
  int wrap_y = tau_torus_wraparound[TAU_BGQ_IDX(2)] && tau_torus_wraparound[TAU_BGQ_IDX(3)]; 
  int wrap_z = tau_torus_wraparound[TAU_BGQ_IDX(4)] && tau_torus_wraparound[TAU_BGQ_IDX(5)]; 

  sprintf(bgqbuffer, "(%d,%d,%d)", wrap_x,wrap_y,wrap_z);
  Tau_metadata_register("BGQ Period", bgqbuffer);

  BG_UniversalComponentIdentifier uci = tau_bgq_personality.Kernel_Config.UCI;
  unsigned int row, col, mp, nb, cc;
  bg_decodeComputeCardOnNodeBoardUCI(uci, &row, &col, &mp, &nb, &cc);
  sprintf(bgqbuffer, "R%x%x-M%d-N%02x-J%02x <%d,%d,%d,%d,%d>", row, col, mp, nb, cc,
      tau_torus_coord[0], tau_torus_coord[1], tau_torus_coord[2],
      tau_torus_coord[3], tau_torus_coord[4]);
  Tau_metadata_register("BGQ Node Name", bgqbuffer);

  sprintf(bgqbuffer, "%ld", ((uci>>38)&0xFFFFF)); /* encode row,col,mp,nb,cc*/
  Tau_metadata_register("BGQ Node ID", bgqbuffer);

  sprintf(bgqbuffer, "%ld", Kernel_PhysicalProcessorID());
  Tau_metadata_register("BGQ Physical Processor ID", bgqbuffer);

  sprintf(bgqbuffer, "%d", tau_bgq_personality.Kernel_Config.FreqMHz);
  Tau_metadata_register("CPU MHz", bgqbuffer);

  sprintf(bgqbuffer, "%d", Kernel_GetJobID());
  Tau_metadata_register("BGQ Job ID", bgqbuffer);

  sprintf(bgqbuffer, "%d", Kernel_ProcessorID());
  Tau_metadata_register("BGQ Processor ID", bgqbuffer);

  sprintf(bgqbuffer, "%d", Kernel_PhysicalHWThreadID());
  Tau_metadata_register("BGQ Physical HW Thread ID", bgqbuffer);

  sprintf(bgqbuffer, "%d", Kernel_ProcessCount());
  Tau_metadata_register("BGQ Process Count", bgqbuffer);

  sprintf(bgqbuffer, "%d", Kernel_ProcessorCount());
  Tau_metadata_register("BGQ Processor Count", bgqbuffer);

  sprintf(bgqbuffer, "%d", Kernel_MyTcoord());
  Tau_metadata_register("BGQ tCoord", bgqbuffer);

  sprintf(bgqbuffer, "%d", Kernel_ProcessorCoreID());
  Tau_metadata_register("BGQ Processor Core ID", bgqbuffer);

  sprintf(bgqbuffer, "%d", Kernel_ProcessorThreadID());
  Tau_metadata_register("BGQ Processor Thread ID", bgqbuffer);

  sprintf(bgqbuffer, "%d", Kernel_BlockThreadId());
  Tau_metadata_register("BGQ Block Thread ID", bgqbuffer);

  // Returns the Rank associated with the current process
  sprintf(bgqbuffer, "%d", Kernel_GetRank());
  Tau_metadata_register("BGQ Rank", bgqbuffer);

  sprintf(bgqbuffer, "%d", tau_bgq_personality.DDR_Config.DDRSizeMB);
  Tau_metadata_register("BGQ DDR Size (MB)", bgqbuffer);


#endif /* TAU_BGQ */

#ifdef __linux__
  // doesn't work on ia64 for some reason
  //Tau_util_output (out, "\t<linux_tid>%d</linux_tid>\n", gettid());

  // try to grab CPU info
  FILE *f = fopen("/proc/cpuinfo", "r");
  if (f) {
    char line[4096];
    while (Tau_util_readFullLine(line, f)) {
      char const * value = strstr(line,":");
      if (!value) {
        break;
      } else {
        /* skip over colon */
        value += 2;
      }

      // Allocates a string
      value = Tau_util_removeRuns(value);

      if (strncmp(line, "vendor_id", 9) == 0) {
        Tau_metadata_register("CPU Vendor", value);
      }
      if (strncmp(line, "vendor", 6) == 0) {
        Tau_metadata_register("CPU Vendor", value);
      }
      if (strncmp(line, "cpu MHz", 7) == 0) {
        Tau_metadata_register("CPU MHz", value);
      }
      if (strncmp(line, "clock", 5) == 0) {
        Tau_metadata_register("CPU MHz", value);
      }
      if (strncmp(line, "model name", 10) == 0) {
        Tau_metadata_register("CPU Type", value);
      }
      if (strncmp(line, "family", 6) == 0) {
        Tau_metadata_register("CPU Type", value);
      }
      if (strncmp(line, "cpu\t", 4) == 0) {
        Tau_metadata_register("CPU Type", value);
      }
      if (strncmp(line, "cache size", 10) == 0) {
        Tau_metadata_register("Cache Size", value);
      }
      if (strncmp(line, "cpu cores", 9) == 0) {
        Tau_metadata_register("CPU Cores", value);
      }

      // Deallocates the string
      free((void*)value);
    }
    fclose(f);
  }

  f = fopen("/proc/meminfo", "r");
  if (f) {
    char line[4096];
    while (Tau_util_readFullLine(line, f)) {
      char const * value = strstr(line,":");

      if (!value) {
        break;
      } else {
        value += 2;
      }

      // Allocates a string
      value = Tau_util_removeRuns(value);

      if (strncmp(line, "MemTotal", 8) == 0) {
        Tau_metadata_register("Memory Size", value);
      }

      free((void*)value);
    }
    fclose(f);
  }

  char buffer[4096];
  bzero(buffer, 4096);
  int rc = readlink("/proc/self/exe", buffer, 4096);
  if (rc != -1) {
    Tau_metadata_register("Executable", buffer);
  }
  bzero(buffer, 4096);
  rc = readlink("/proc/self/cwd", buffer, 4096);
  if (rc != -1) {
    Tau_metadata_register("CWD", buffer);
  }


  f = fopen("/proc/self/cmdline", "r");
  if (f) {
    char line[4096];

    /* *CWL* - STL cannot be used in PGI init sections???
       std::ostringstream os;

       while (Tau_util_readFullLine(line, f)) {
       if (os.str().length() != 0) {
       os << " ";
       }
       os << line;
       }
       Tau_metadata_register("Command Line", os.str().c_str());
     */
    string os;
    // *CWL* - The following loop performs newline to space conversions
    while (Tau_util_readFullLine(line, f)) {
      if (os.length() != 0) {
        os.append(" ");
      }
      os.append(string(line));
    }    
    Tau_metadata_register("Command Line", os.c_str());
    fclose(f);
  }
#endif /* __linux__ */

  char *user = getenv("USER");
  if (user != NULL) {
    Tau_metadata_register("username", user);
  }

  return 0;
#endif // TAU_DISABLE_METADATA

}

static int writeMetaData(Tau_util_outputDevice *out, bool newline, int counter, int tid) {
  const char *endl = "";
  //newline = true;
  if (newline) {
    endl = "\n";
  }
  Tau_util_output (out, "<metadata>%s", endl);

  if (counter != -1) {
    Tau_XML_writeAttribute(out, "Metric Name", RtsLayer::getCounterName(counter), newline);
  }

  /*
   * In order to support thread-specific metadata, we will need to aggregate
   * the metadata which is common to all threads in this process (thread 0
   * metadata, basically) with the thread-specific metata. If the current
   * thread is 0, we have no aggregation to do.
   */
  map<Tau_metadata_key,Tau_metadata_value_t*,Tau_Metadata_Compare> *localRepo = NULL;
  if (tid == 0) {
    // just get a reference to thread 0 metadata
    localRepo = &(Tau_metadata_getMetaData(tid));
  } else {
    // create a new aggregator
    localRepo = new MetaDataRepo();
	// copy all metadata from thread 0
    for (map<Tau_metadata_key,Tau_metadata_value_t*,Tau_Metadata_Compare>::iterator it = Tau_metadata_getMetaData(0).begin(); it != Tau_metadata_getMetaData(0).end(); it++) {
	  // DON'T copy the context metadata fields
	  if (it->first.timer_context == NULL) {
        (*localRepo)[it->first] = it->second;
	  }
	}
 	// overwrite with thread-specific data
    for (map<Tau_metadata_key,Tau_metadata_value_t*,Tau_Metadata_Compare>::iterator it = Tau_metadata_getMetaData(tid).begin(); it != Tau_metadata_getMetaData(tid).end(); it++) {
      (*localRepo)[it->first] = it->second;
	}
  }
  int thread0 = Tau_metadata_getMetaData(0).size();
  int local = localRepo->size();
  int threadi = Tau_metadata_getMetaData(tid).size();

  // write out the user-specified (some from TAU) attributes
  int i = 0;
  for (map<Tau_metadata_key,Tau_metadata_value_t*,Tau_Metadata_Compare>::iterator it = (*localRepo).begin(); it != (*localRepo).end(); it++) {
  /*
	if (it->first.timer_context == NULL) {
      const char *name = it->first.name;
      const char *value = it->second->data.cval;
      Tau_XML_writeAttribute(out, name, value, newline);
	} else {
	*/
      Tau_XML_writeAttribute(out, &(it->first), it->second, newline);
	//}
	i++;
  }

  Tau_util_output (out, "</metadata>%s", endl);
  return 0;
}

extern "C" void Tau_context_metadata(const char *name, const char *value) {

#ifdef TAU_DISABLE_METADATA
  return;
#endif

  //printf("%s, %s\n", name, value);
  RtsLayer::LockDB();
  // get the current calling context
  Profiler *current = TauInternal_CurrentProfiler(RtsLayer::myThread());
  Tau_metadata_key *key = new Tau_metadata_key();
  // it IS possible to request metadata with no active timer.
  if (current != NULL) {
    FunctionInfo *fi = current->ThisFunction;
    char *fname = (char*)(malloc(sizeof(char)*(strlen(fi->GetName()) + strlen(fi->GetType()) + 2)));
    sprintf(fname, "%s %s", fi->GetName(), fi->GetType());
	key->timer_context = fname;
	key->call_number = fi->GetCalls(RtsLayer::myThread());
	key->timestamp = (x_uint64)current->StartTime[0];
  }
  key->name = strdup(name);
  Tau_metadata_value_t* tmv = NULL;
  Tau_metadata_create_value(&tmv, TAU_METADATA_TYPE_STRING);
  tmv->data.cval = strdup(value);
  //printf("%p  %s:%s:%d:%d:%llu = %s\n", &(Tau_metadata_getMetaData(RtsLayer::myThread())), key->name, key->timer_context, RtsLayer::myThread(), key->call_number, key->timestamp, tmv->data.cval);
  int before = Tau_metadata_getMetaData(RtsLayer::myThread()).size();
  Tau_metadata_getMetaData(RtsLayer::myThread())[*key] = tmv;
  int after = Tau_metadata_getMetaData(RtsLayer::myThread()).size();
  //printf("before: %d items, after %d items\n", before, after);
  RtsLayer::UnLockDB();
}

extern "C" void Tau_structured_metadata(const Tau_metadata_object_t *object, bool context) {

#ifdef TAU_DISABLE_METADATA
  return;
#else

  Tau_metadata_key *key = new Tau_metadata_key();
  if (context) {
    RtsLayer::LockDB();
    // get the current calling context
    Profiler *current = TauInternal_CurrentProfiler(RtsLayer::myThread());
    // it IS possible to request metadata with no active timer.
    if (current != NULL) {
      FunctionInfo *fi = current->ThisFunction;
      char *fname = (char*)(malloc(sizeof(char)*(strlen(fi->GetName()) + strlen(fi->GetType()) + 2)));
      sprintf(fname, "%s %s", fi->GetName(), fi->GetType());
	  key->timer_context = fname;
	  key->call_number = fi->GetCalls(RtsLayer::myThread());
	  key->timestamp = (x_uint64)current->StartTime[0];
    }
  }
  int i;
  for (i = 0 ; i < object->count ; i++) {
    key->name = strdup(object->names[i]);
    Tau_metadata_value_t* tmv = object->values[i];
    //printf("%p  %s:%s:%d:%llu = %s\n", &(Tau_metadata_getMetaData(RtsLayer::myThread())), key->name, key->timer_context, key->call_number, key->timestamp, tmv->data.cval);
    Tau_metadata_getMetaData(RtsLayer::myThread())[*key] = tmv;
  }
  RtsLayer::UnLockDB();
#endif
}

extern "C" void Tau_phase_metadata(const char *name, const char *value) {
#ifdef TAU_DISABLE_METADATA
  return;
#else
#ifdef TAU_PROFILEPHASE
  // get the current calling context
  Profiler *current = TauInternal_CurrentProfiler(RtsLayer::myThread());
  Tau_metadata_key *key = new Tau_metadata_key();
  key->name = strdup(name);
  while (current != NULL) {
    if (current->GetPhase()) {
      FunctionInfo *fi = current->ThisFunction;
      char *fname = (char*)(malloc(sizeof(char)*(strlen(fi->GetName()) + strlen(fi->GetType()) + 2)));
      sprintf(fname, "%s %s", fi->GetName(), fi->GetType());
	  key->timer_context = fname;
	  key->call_number = fi->GetCalls(RtsLayer::myThread());
	  key->timestamp = (x_uint64)current->StartTime[0];
	  break;
    }    
    current = current->ParentProfiler;
  }
  Tau_metadata_value_t* tmv = NULL;
  Tau_create_metadata_value(&tmv, TAU_METADATA_TYPE_STRING);
  tmv->data.cval = strdup(value);
  RtsLayer::LockDB();
  Tau_metadata_getMetaData(RtsLayer::myThread())[*key] = tmv;
  RtsLayer::UnLockDB();
#else
  Tau_context_metadata(name, value);
#endif
#endif
}


int Tau_metadata_writeMetaData(Tau_util_outputDevice *out, int tid) {

#ifdef TAU_DISABLE_METADATA
  return 0;
#endif

  //Tau_metadata_fillMetaData();
  return writeMetaData(out, true, -1, tid);
}

int Tau_metadata_writeMetaData(Tau_util_outputDevice *out) {
  return writeMetaData(out, true, -1, 0);
}

int Tau_metadata_writeMetaData(Tau_util_outputDevice *out, int counter, int tid) {
#ifdef TAU_DISABLE_METADATA
  return 0;
#endif

  //Tau_metadata_fillMetaData();
  int retval;
  retval = writeMetaData(out, false, counter, tid);
  return retval;
}

/* helper function to write to already established file pointer */
int Tau_metadata_writeMetaData(FILE *fp, int counter, int tid) {
  Tau_util_outputDevice out;
  out.fp = fp;
  out.type = TAU_UTIL_OUTPUT_FILE;
  return Tau_metadata_writeMetaData(&out, counter, tid);
}


Tau_util_outputDevice *Tau_metadata_generateMergeBuffer() {
  Tau_util_outputDevice *out = Tau_util_createBufferOutputDevice();

  Tau_util_output(out,"%d%c", Tau_metadata_getMetaData(RtsLayer::myThread()).size(), '\0');

  for (map<Tau_metadata_key,Tau_metadata_value_t*,Tau_Metadata_Compare>::iterator it = Tau_metadata_getMetaData(RtsLayer::myThread()).begin(); it != Tau_metadata_getMetaData(RtsLayer::myThread()).end(); ++it) {
    const char *name = it->first.name;
    Tau_util_output(out,"%s%c", name, '\0');
	switch (it->second->type) {
	  case TAU_METADATA_TYPE_STRING:
        Tau_util_output(out,"%s%c", it->second->data.cval, '\0');
		break;
	  case TAU_METADATA_TYPE_INTEGER:
        Tau_util_output(out,"%d%c", it->second->data.ival, '\0');
		break;
	  case TAU_METADATA_TYPE_DOUBLE:
        Tau_util_output(out,"%f%c", it->second->data.dval, '\0');
		break;
	  case TAU_METADATA_TYPE_NULL:
        Tau_util_output(out,"NULL%c", '\0');
		break;
	  case TAU_METADATA_TYPE_FALSE:
        Tau_util_output(out,"FALSE%c", '\0');
		break;
	  case TAU_METADATA_TYPE_TRUE:
        Tau_util_output(out,"TRUE%c", '\0');
		break;
	  default:
        Tau_util_output(out,"%c", '\0');
	    break;
	}
  }
  return out;
}


void Tau_metadata_removeDuplicates(char *buffer, int buflen) {
  //printf ("************* REMOVING DUPLICATES ************* \n");
  // read the number of items and allocate arrays
  int numItems;
  sscanf(buffer,"%d", &numItems);
  buffer = strchr(buffer, '\0')+1;

  char **attributes = (char **) malloc(sizeof(char*) * numItems);
  char **values = (char **) malloc(sizeof(char*) * numItems);

  // assign char pointers to the values inside the buffer
  for (int i=0; i<numItems; i++) {
    const char *attribute = buffer;
    buffer = strchr(buffer, '\0')+1;
    const char *value = buffer;
    buffer = strchr(buffer, '\0')+1;

    Tau_metadata_key *key = new Tau_metadata_key();
	key->name = strdup(attribute);
    map<Tau_metadata_key,Tau_metadata_value_t*,Tau_Metadata_Compare>::iterator iter = Tau_metadata_getMetaData(RtsLayer::myThread()).find(*key);
    if (iter != Tau_metadata_getMetaData(RtsLayer::myThread()).end()) {
	  if (iter->second->type == TAU_METADATA_TYPE_STRING) {
        const char *my_value = iter->second->data.cval;
        if (0 == strcmp(value, my_value)) {
          Tau_metadata_getMetaData(RtsLayer::myThread()).erase(*key);
        }
	  }
    }
  }
}


