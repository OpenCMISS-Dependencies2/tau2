/****************************************************************************
**			TAU Portable Profiling Package			   **
**			http://www.cs.uoregon.edu/research/tau	           **
*****************************************************************************
**    Copyright 1997  						   	   **
**    Department of Computer and Information Science, University of Oregon **
**    Advanced Computing Laboratory, Los Alamos National Laboratory        **
****************************************************************************/
/***************************************************************************
**	File 		: TauBeacon.cpp					  **
**	Description 	: TAU's publish-subscribe interface to Beacon **
**	Contact		: tau-team@cs.uoregon.edu 		 	  **
**	Documentation	: See http://www.cs.uoregon.edu/research/tau      **
***************************************************************************/


//////////////////////////////////////////////////////////////////////
// Include Files 
//////////////////////////////////////////////////////////////////////

//#define DEBUG_PROF 1

#include <tau_internal.h>
#include <Profile/TauMetrics.h>
#ifdef TAU_BEACON
#include <Profile/TauBeacon.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>


#endif /* TAU_BEACON */

#include <stdio.h>
#ifndef TAU_BEACON_BUFFER_SIZE
#define TAU_BEACON_BUFFER_SIZE  1024
#endif /* TAU_BEACON_BUFFER_SIZE */

//////////////////////////////////////////////////////////////////////
// For Initialization
//////////////////////////////////////////////////////////////////////
int TauBeaconInit(void) {
#ifdef DEBUG_PROF
  printf("Called TauBeaconInit\n");
#endif /* DEBUG_PROF */
  return 1;
}

//////////////////////////////////////////////////////////////////////
// Publish an event
//////////////////////////////////////////////////////////////////////
int TauBeaconPublish(double value, char *units, char *topic, char *additional_info) {

   static BEACON_beep_t binfo;
   static BEACON_beep_handle_t handle; 
   static BEACON_topic_info_t *topic_info;
   char data_buf[TAU_BEACON_BUFFER_SIZE]; 
   char beep_name[TAU_BEACON_BUFFER_SIZE];
   static char hostname[TAU_BEACON_BUFFER_SIZE];
   static bool first_time = true; 
   static BEACON_topic_properties_t *eprop; 
   static int jobid; 
   int ret = 0;
   if (first_time) {
     first_time = false; 

     ret = gethostname(hostname, TAU_BEACON_BUFFER_SIZE); 
     if (ret == -1) {
       fprintf(stderr, "Error returned by gethostname, ret = %d\n", ret); 
     }
     // we could use an Enclave id here 
     jobid = getpid();
     // First allocate the eprop object
     eprop = (BEACON_topic_properties_t *) malloc(sizeof(BEACON_topic_properties_t));
     if (eprop == NULL) {
       fprintf(stderr, "Malloc error for eprop!\n");
       exit(1);
     }

     // Next allocate the topic_info
     topic_info = (BEACON_topic_info_t *)malloc(sizeof(BEACON_topic_info_t) ); 
     if (topic_info == NULL) {
       fprintf(stderr, "Malloc error for topic_info!\n");
       exit(1);
     }

     strcpy(topic_info->topic_name, topic);
     sprintf(topic_info->severity, "INFO"); 
     // initialize data structures 
     memset(&binfo, 0, sizeof(binfo));
     strcpy(binfo.beep_version, "1.0");
     strcpy(binfo.beep_name, "TAU_BEACON_BEEP");
     ret = BEACON_Connect(&binfo, &handle); 
     if (ret != BEACON_SUCCESS) {
       fprintf(stderr, "BEACON_Connect failed. ret = %d\n", ret);
       exit(1); 
     }
     strcpy(eprop->topic_scope, "node");
   }
   
   sprintf(eprop->topic_payload, "data=%g; units=%s; name=%s; node=%s; jobid=%d", value, units, additional_info, hostname, jobid); 
   ret = BEACON_Publish(handle, topic_info->topic_name, eprop); 
   if (ret != BEACON_SUCCESS) {
     fprintf(stderr, "BEACON_Publish failed. ret = %d\n", ret);
     exit(1); 
   }
#ifdef DEBUG_PROF
   printf("TauBeaconPublish: TOPIC = %s;\n", topic);
   printf("TauBeaconPublish: %s\n", eprop->topic_payload);
#endif /* DEBUG_PROF */
   return 1;
}