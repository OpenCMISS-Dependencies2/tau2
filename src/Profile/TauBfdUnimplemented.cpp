/* *CWL* Temporary fix. An appropriate reconciliation of the semantics of the two versions
   will need to be explored and implemented at some point */

/***********************************
 * ******* TAU MINGW VERSION *******
 ***********************************/
#ifdef TAU_MINGW

// *CWL* This code is used for two (rather different) purposes.
//
//    1. When a shared library for BFD is not available and -optShared
//       is required as an option to build the application.
//
//    2. When BFD is not available at all (the interface is still required).

/* Not exactly empty. Support for address maps are independent of BFD */
#include <TAU.h>
#include <Profile/TauBfd.h>

#include <stdio.h>
#include <dirent.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <vector>
using namespace std;

#define TAU_BFD_UNIMPLEMENTED_HANDLE -1337
#define TAU_BFD_UNIMPLEMENTED_MODULE_HANDLE -11337

// Empty interface functions
static void Tau_bfd_internal_issueBfdWarningIfNecessary() {
  static bool warningIssued = false;
  if (!warningIssued) {
    fprintf(stderr,"TAU Warning: BFD is not available in at least one part of this TAU-instrumented application! Please check to see if BFD is not shared or not present. Expect some missing BFD functionality.\n");
    warningIssued = true;
  }
}

void Tau_bfd_initializeBfdIfNecessary() {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
}

tau_bfd_handle_t Tau_bfd_registerUnit(int flag) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  addressMaps = new vector<TauBfdAddrMap>();
  return TAU_BFD_UNIMPLEMENTED_HANDLE;
}

bool Tau_bfd_checkHandle(tau_bfd_handle_t handle) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (handle == TAU_BFD_UNIMPLEMENTED_HANDLE) {
    return true;
  }
  return false;
}

void Tau_bfd_updateAddressMaps(tau_bfd_handle_t handle) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (!Tau_bfd_checkHandle(handle)) {
    return;
  }
}

vector<TauBfdAddrMap> *Tau_bfd_getAddressMaps(tau_bfd_handle_t handle) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (!Tau_bfd_checkHandle(handle)) {
    return NULL;
  }
  return addressMaps;
}

tau_bfd_module_handle_t Tau_bfd_getModuleHandle(tau_bfd_handle_t handle,
		uintptr_t probeAddr) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (!Tau_bfd_checkHandle(handle)) {
    return TAU_BFD_NULL_MODULE_HANDLE;
  }
  return TAU_BFD_UNIMPLEMENTED_MODULE_HANDLE;
}

bool Tau_bfd_resolveBfdInfo(tau_bfd_handle_t handle,
		uintptr_t probe_addr, TauBfdInfo & info)
{
	Tau_bfd_internal_issueBfdWarningIfNecessary();
	return false;
}

bool Tau_bfd_resolveBfdExecInfo(tau_bfd_handle_t handle,
		uintptr_t probe_addr, TauBfdInfo & info) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  return false;
}

int Tau_bfd_processBfdModuleInfo(tau_bfd_handle_t handle,
		tau_bfd_module_handle_t moduleHandle, TauBfdIterFn fn)
{
	Tau_bfd_internal_issueBfdWarningIfNecessary();
	if (!Tau_bfd_checkHandle(handle)) {
		return TAU_BFD_SYMTAB_LOAD_FAILED;
	}
	return TAU_BFD_SYMTAB_LOAD_UNRESOLVED;
}

int Tau_bfd_processBfdExecInfo(tau_bfd_handle_t handle, TauBfdIterFn fn)
{
	Tau_bfd_internal_issueBfdWarningIfNecessary();
	if (!Tau_bfd_checkHandle(handle)) {
		return TAU_BFD_SYMTAB_LOAD_FAILED;
	}
	return TAU_BFD_SYMTAB_LOAD_UNRESOLVED;
}

/******************************************
 * ******* END OF TAU MINGW VERSION *******
 ******************************************/

#else /* TAU_MINGW */

/**************************************
 * ******* ORIGINAL BFD VERSION *******
 **************************************/

// *CWL* This code is used for two (rather different) purposes.
//
//    1. When a shared library for BFD is not available and -optShared
//       is required as an option to build the application.
//
//    2. When BFD is not available at all (the interface is still required).

/* Not exactly empty. Support for address maps are independent of BFD */
#include <TAU.h>
#include <Profile/TauBfd.h>

#include <stdio.h>
#include <dirent.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <vector>
using namespace std;

#define TAU_BFD_UNIMPLEMENTED_HANDLE -1337
#define TAU_BFD_UNIMPLEMENTED_MODULE_HANDLE -11337

vector<TauBfdAddrMap> *addressMaps;

// Empty interface functions
static void Tau_bfd_internal_issueBfdWarningIfNecessary() {
  static bool warningIssued = false;
  if (!warningIssued) {
    fprintf(stderr,"TAU Warning: BFD is not available in at least one part of this TAU-instrumented application! Please check to see if BFD is not shared or not present. Expect some missing BFD functionality.\n");
    warningIssued = true;
  }
}

void Tau_bfd_initializeBfdIfNecessary() {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
}

tau_bfd_handle_t Tau_bfd_registerUnit(int flag) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  addressMaps = new vector<TauBfdAddrMap>();
  return TAU_BFD_UNIMPLEMENTED_HANDLE;
}

bool Tau_bfd_checkHandle(tau_bfd_handle_t handle) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (handle == TAU_BFD_UNIMPLEMENTED_HANDLE) {
    return true;
  }
  return false;
}

void Tau_bfd_updateAddressMaps(tau_bfd_handle_t handle) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (!Tau_bfd_checkHandle(handle)) {
    return;
  }
  TAU_VERBOSE("Tau_bfd_updateAddressMaps: Updating object maps\n");

  addressMaps->clear();
  // Note: Linux systems only.
  FILE *mapsfile = fopen("/proc/self/maps", "r");
  if (mapsfile == NULL) {
    return;
  }
  char line[4096];
  while (!feof(mapsfile)) {
    fgets(line, 4096, mapsfile);
    unsigned long start, end, offset;
    char module[4096];
    char perms[5];
    module[0] = 0;
    
    sscanf(line, "%lx-%lx %s %lx %*s %*u %[^\n]",
	   &start, &end, perms, &offset, module);
    if (*module && ((strcmp(perms, "r-xp") == 0) ||
		    (strcmp(perms, "rwxp") == 0))) {
      TAU_VERBOSE("got %s, %p-%p (%d)\n", module, start, end, offset);
      TauBfdAddrMap map;
      map.start = start;
      map.end = end;
      map.offset = offset;
      sprintf(map.name, "%s", module);
      addressMaps->push_back(map);
    }
  }
  fclose(mapsfile);
}

vector<TauBfdAddrMap> *Tau_bfd_getAddressMaps(tau_bfd_handle_t handle) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (!Tau_bfd_checkHandle(handle)) {
    return NULL;
  }
  return addressMaps;
}

tau_bfd_module_handle_t Tau_bfd_getModuleHandle(tau_bfd_handle_t handle,
						unsigned long probe_addr) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (!Tau_bfd_checkHandle(handle)) {
    return TAU_BFD_NULL_MODULE_HANDLE;
  }
  return TAU_BFD_UNIMPLEMENTED_MODULE_HANDLE;
}

// Probe for BFD information given a single address.
TauBfdInfo *Tau_bfd_resolveBfdInfo(tau_bfd_handle_t handle,
				   unsigned long probe_addr) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  return NULL;
}

TauBfdInfo *Tau_bfd_resolveBfdExecInfo(tau_bfd_handle_t handle,
				       unsigned long probe_addr) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  return NULL;
}

// Run a unit-defined iterator through symbols discovered in a Bfd module.
//   If there are too many symbols in the module, we will avoid resolving
//   the symbols but allow the iterator to perform its work.
int Tau_bfd_processBfdModuleInfo(tau_bfd_handle_t handle,
				 tau_bfd_module_handle_t moduleHandle,
				 int symbolLimit,
				 TauBfdIterFn fn) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (!Tau_bfd_checkHandle(handle)) {
    return TAU_BFD_SYMTAB_LOAD_FAILED;
  }
  return TAU_BFD_SYMTAB_LOAD_UNRESOLVED;
}

int Tau_bfd_processBfdExecInfo(tau_bfd_handle_t handle,
				int symbolLimit,
				TauBfdIterFn fn) {
  Tau_bfd_internal_issueBfdWarningIfNecessary();
  if (!Tau_bfd_checkHandle(handle)) {
    return TAU_BFD_SYMTAB_LOAD_FAILED;
  }
  return TAU_BFD_SYMTAB_LOAD_UNRESOLVED;
}

/*********************************************
 * ******* END OF ORIGINAL BFD VERSION *******
 *********************************************/

#endif /* TAU_MINGW */
