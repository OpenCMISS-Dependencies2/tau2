/************************************************************************************************
 * *   Plugin Testing
 * *   Tests basic functionality of a plugin for atomic event registration event
 * *
 * *********************************************************************************************/

#include <iostream>
#include <stdio.h>
#include <stdlib.h>

#include <Profile/Profiler.h>
#include <Profile/TauSampling.h>
#include <Profile/TauMetrics.h>
#include <Profile/TauAPI.h>
#include <Profile/TauPlugin.h>


int Tau_plugin_test_event_atomic_event_registration_complete(Tau_plugin_event_atomic_event_registration_data data) {
  
  std::cout << "TAU PLUGIN: Event with name " << ((tau::TauUserEvent *)data.user_event_ptr)->GetName() << " has been registered" << std::endl;

  return 0;
}

/*This is the init function that gets invoked by the plugin mechanism inside TAU.
 * Every plugin MUST implement this function to register callbacks for various events 
 * that the plugin is interested in listening to*/
extern "C" int Tau_plugin_init_func(PluginManager* plugin_manager) {
  Tau_plugin_callbacks * cb = (Tau_plugin_callbacks*)malloc(sizeof(Tau_plugin_callbacks));
  Tau_util_init_tau_plugin_callbacks(cb);
  cb->AtomicEventRegistrationComplete = Tau_plugin_test_event_atomic_event_registration_complete;
  Tau_util_plugin_register_callbacks(cb);

  return 0;
}

