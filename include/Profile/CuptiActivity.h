#include <Profile/TauGpu.h>
#include <Profile/CuptiLayer.h>
#include <cuda.h>
#include <cupti.h>
#include <math.h>

#if CUPTI_API_VERSION >= 2

#ifdef TAU_BFD
#define HAVE_DECL_BASENAME 1
#  if defined(HAVE_GNU_DEMANGLE) && HAVE_GNU_DEMANGLE
#    include <demangle.h>
#  endif /* HAVE_GNU_DEMANGLE */
#  include <bfd.h>
#endif /* TAU_BFD */

#define CUDA_CHECK_ERROR(err, str) \
	if (err != CUDA_SUCCESS) \
  { \
		fprintf(stderr, str); \
		exit(1); \
	} \

#define CUPTI_CHECK_ERROR(err, str) \
	if (err != CUPTI_SUCCESS) \
  { \
		fprintf(stderr, str); \
		exit(1); \
	} \

#define ACTIVITY_BUFFER_SIZE (4096 * 1024)

extern "C" void Tau_cupti_register_metadata(
						uint32_t deviceId,
						GpuMetadata *metadata,
						int metadata_size);

extern "C" void Tau_cupti_register_calling_site(
						uint32_t correlationId,
						FunctionInfo *current_function);

extern "C" void Tau_cupti_enter_memcpy_event(
						const char *name,
						uint32_t deviceId,
						uint32_t streamId,
						uint32_t contextId,
						uint32_t correlationId,
						int bytes_copied,
						int memcpy_type);

extern "C" void Tau_cupti_exit_memcpy_event(
						const char *name,
						uint32_t deviceId,
						uint32_t streamId,
						uint32_t contextId,
						uint32_t correlationId,
						int bytes_copied,
						int memcpy_type);

extern "C" void Tau_cupti_register_memcpy_event(
						const char *name,
						uint32_t deviceId,
						uint32_t streamId,
						uint32_t contextId,
						uint32_t correlationId,
						double start,
						double stop,
						int bytes_copied,
						int memcpy_type);

extern "C" void Tau_cupti_register_gpu_event(
						const char *name,
						uint32_t deviceId,
						uint32_t streamId,
						uint32_t contextId,
						uint32_t correlationId,
						GpuEventAttributes *gpu_attributes,
						int number_of_attributes,
						double start,
						double stop);

extern "C" void Tau_cupti_register_gpu_atomic_event(
						const char *name,
						uint32_t deviceId,
						uint32_t streamId,
						uint32_t contextId,
						uint32_t correlationId,
						GpuEventAttributes *gpu_attributes,
						int number_of_attributes);

extern "C" void Tau_pure_context_userevent(void **ptr, std::string name);

uint8_t *activityBuffer;
CUpti_SubscriberHandle subscriber;

int number_of_streams;
std::vector<int> streamIds;

void Tau_cupti_register_sync_event(CUcontext c, uint32_t stream);

void Tau_cupti_callback_dispatch(void *ud, CUpti_CallbackDomain domain, CUpti_CallbackId id, const void *params);

void Tau_cupti_record_activity(CUpti_Activity *record);

void __attribute__ ((constructor)) Tau_cupti_onload(void);

void Tau_cupti_subscribe(void);

void __attribute__ ((destructor)) Tau_cupti_onunload(void);

void get_values_from_memcpy(const CUpti_CallbackData *info, CUpti_CallbackId id, CUpti_CallbackDomain domain, int &kind, int &count);

int getMemcpyType(int kind);
const char* demangleName(const char *n);

int getParentFunction(uint32_t id);

bool function_is_sync(CUpti_CallbackId id);
bool function_is_memcpy(CUpti_CallbackId id, CUpti_CallbackDomain domain);
bool function_is_launch(CUpti_CallbackId id);
bool function_is_exit(CUpti_CallbackId id);

bool registered_sync = false;

bool cupti_api_runtime();
bool cupti_api_driver();

typedef std::map<TauContextUserEvent *, TAU_EVENT_DATATYPE> eventMap_t;
eventMap_t eventMap; 

int gpu_occupancy_available(int deviceId);
void record_gpu_occupancy(CUpti_ActivityKernel *k, const char *name, eventMap_t *m);

void form_context_event_name(CUpti_ActivityKernel *kernel, CUpti_ActivitySourceLocator *source, const char *event, std::string *name);

std::map<uint32_t, CUpti_ActivityDevice> deviceMap;
//std::map<uint32_t, CUpti_ActivityGlobalAccess> globalAccessMap;
std::map<uint32_t, CUpti_ActivityKernel> kernelMap;
std::map<uint32_t, CUpti_ActivitySourceLocator> sourceLocatorMap;

#define CAST_TO_RUNTIME_MEMCPY_TYPE_AND_CALL(name, id, info, kind, count) \
	if ((id) == CUPTI_RUNTIME_TRACE_CBID_##name##_v3020) \
	{ \
		kind = ((name##_v3020_params *) info->functionParams)->kind; \
		count = ((name##_v3020_params *) info->functionParams)->count; \
	}

#define S(x) #x
#define SX(x) S(x)
#define RECORD_DEVICE_METADATA(n, device) \
  std::ostringstream str_##n; \
	str_##n << device->n; \
	int string_length_##n = strlen(str_##n.str().c_str()) + 1; \
	char *stored_name_##n = (char*) malloc(sizeof(char)*string_length_##n); \
	strcpy(stored_name_##n, str_##n.str().c_str()); \
	metadata[id].name = "GPU " SX(n); \
	metadata[id].value = stored_name_##n; \
	id++
#endif

	//Tau_metadata("GPU " SX(name), str_##name.str().c_str()); 

