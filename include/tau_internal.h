/****************************************************************************
**			TAU Portable Profiling Package                     **
**			http://www.cs.uoregon.edu/research/tau             **
*****************************************************************************
**    Copyright 2008  						   	   **
**    Department of Computer and Information Science, University of Oregon **
**    Advanced Computing Laboratory, Los Alamos National Laboratory        **
**    Forschungszentrum Juelich                                            **
****************************************************************************/
/****************************************************************************
**	File 		: tau_internal.h			        	   **
**	Description 	: TAU Profiling Package				   **
**	Contact		: tau-bugs@cs.uoregon.edu               	   **
**	Documentation	: See http://www.cs.uoregon.edu/research/tau       **
**                                                                         **
**      Description     : Include this header in all TAU source files      **
**                        But not user applications                        **
**                                                                         **
****************************************************************************/

#ifndef _TAU_INTERNAL_H_
#define _TAU_INTERNAL_H_


#if (defined(TAU_WINDOWS))
#define TAUDECL __cdecl
#else
#define TAUDECL
#endif /* TAU_WINDOWS */


#include <Profile/tau_types.h>


#ifdef TAU_LARGEFILE
  #define LARGEFILE_OPTION O_LARGEFILE
#else
  #define LARGEFILE_OPTION 0
#endif



#ifdef TAU_WINDOWS
  #include <io.h>
  #include <direct.h> /* for getcwd */
  #define S_IRUSR 0
  #define S_IWUSR 0
  #define S_IRGRP 0
  #define S_IWGRP 0
  #define S_IROTH 0
  #define S_IWOTH 0
#else
  #include <unistd.h>
  #include <sys/time.h>
  #define O_BINARY 0
#endif


#ifdef TAU_WINDOWS
 #define TAUROOT "root"
 #define TAU_ARCH "win32"
#else
 #include "tauroot.h"
 #include "tauarch.h"
#endif



#ifdef __cplusplus

#ifdef HAVE_TR1_HASH_MAP
#include <tr1/unordered_map>
#define TAU_HASH_MAP std::tr1::unordered_map
#else
#include <map>
#define TAU_HASH_MAP std::map
#endif /* HAVE_HASH_MAP */

#endif /* __cplusplus */


#ifdef DEBUG_ASSERT
#ifdef __cplusplus
extern "C" {
#endif // __cplusplus
void Tau_assert_raise_error(const char* msg);
#ifdef __cplusplus
}
#endif // __cplusplus

#define TAU_ASSERT(test, msg) if (! test) Tau_assert_raise_error(msg)

#else /* DEBUG_ASSERT */
#define TAU_ASSERT(test, msg)
#endif /* DEBUG_ASSERT */
 

#endif /* _TAU_INTERNAL_H_ */
