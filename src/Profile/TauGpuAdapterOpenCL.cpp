//if callbacks are supported make sure do locking in the callbacks.
#ifdef TAU_ENABLE_CL_CALLBACK
#define TAU_OPENCL_LOCKING
#endif
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <queue>
#include "TauGpuAdapterOpenCL.h"
#ifdef TAU_OPENCL_LOCKING
#include <pthread.h>
#else
#pragma message ("Warning: Compiling without pthread support, callbacks may give \
overlapping timers errors.")
#endif
void __attribute__ ((constructor)) Tau_opencl_init(void);
//void __attribute__ ((destructor)) Tau_opencl_exit(void);

// HACK need to not profile clGetEventProfilingInfo inside a callback.
extern cl_int clGetEventProfilingInfo_noinst(cl_event a1, cl_profiling_info a2, size_t
a3, void * a4, size_t * a5);

extern cl_mem clCreateBuffer_noinst(cl_context a1, cl_mem_flags a2, size_t a3, void *
a4, cl_int * a5);

extern cl_int clEnqueueWriteBuffer_noinst(cl_command_queue a1, cl_mem a2, cl_bool a3,
size_t a4, size_t a5, const void * a6, cl_uint a7, const cl_event * a8, cl_event
* a9);

extern cl_int clWaitForEvents_noinst(cl_uint a1, const cl_event * a2);

extern cl_int clReleaseEvent_noinst(const cl_event a1);

static queue<callback_data*> KernelBuffer;

static map<cl_command_queue, openCLGpuId*> IdentityMap;

double Tau_opencl_sync_clocks(cl_command_queue commandQueue, cl_context
context);

char* openCLGpuId::printId() 
{	
		//printf("in printId, id: %d.\n", id);
		char r[20];
		sprintf(r, "%d", id);
		return r;
}

double openCLGpuId::syncOffset()
{
	return sync_offset;
}
/* CUDA Event are uniquely identified as the pair of two other ids:
 * context and call (API).
 */
class openCLEventId : public eventId
{
	int id;
	public:
	
	// for use in STL Maps	
	bool operator<(const openCLEventId& A) const
	{ 
			return id<A.id; 
	}
};

openCLGpuId::openCLGpuId(cl_device_id i, double sync)
{
	id = i;
	sync_offset = sync;
}

openCLGpuId *Tau_opencl_retrive_gpu(cl_command_queue q)
{
	//printf("Adapter: command queue is: %d.\n", q);
	if (q == NULL)
	{	printf("NULL command queue passed. exiting.\n");
		exit(1); }
	//map<cl_command_queue, openCLGpuId*>::iterator it = IdentityMap.end();
	map<cl_command_queue, openCLGpuId*>::iterator it = IdentityMap.find(q);
	if (it != IdentityMap.end())
	{
		//printf("found GPU returning.\n");
		return it->second;
	}
	else
	{	
	  cl_device_id id;
		cl_context context;
		cl_int err;
		cl_uint vendor;
		err = clGetCommandQueueInfo(q, CL_QUEUE_DEVICE, sizeof(cl_device_id), &id, NULL);
		if (err != CL_SUCCESS)
		{	
			printf("error in clGetCommandQueueInfo DEVICE.\n"); 
			if (err == CL_INVALID_COMMAND_QUEUE)
				printf("invalid command queue.\n");
		}
		//err = clGetDeviceInfo(id, CL_DEVICE_VENDOR_ID, sizeof(cl_uint), &vendor, NULL);


		//printf("device id: %d.\n", id);
		//printf("vendor id: %d.\n", vendor);
		clGetCommandQueueInfo(q, CL_QUEUE_CONTEXT, sizeof(cl_context), &context, NULL);
		if (err != CL_SUCCESS)
		{	printf("error in clGetCommandQueueInfo CONTEXT.\n"); }
	
		double sync_offset;
		sync_offset = Tau_opencl_sync_clocks(q, context);
	
		openCLGpuId *gId = new openCLGpuId(id, sync_offset);
		IdentityMap[q] = gId;
		
		return gId;
		//printf("New device id found: %d.\n", id);
	}
}

callback_data::callback_data(char* n, openCLGpuId *i, FunctionInfo* cs, cl_event* ev)
{
	name  = n;
	id = i->getCopy();
	callingSite = cs;
	event = ev;
	memcpy_type = -1;
}
callback_data::callback_data(char* n, openCLGpuId *i, FunctionInfo* cs, cl_event* ev, int
memtype)
{
	name  = n;
	id = i->getCopy();
	callingSite = cs;
	event = ev;
	memcpy_type = memtype;
}
callback_data::~callback_data()
{
	//clReleaseEvent_noinst(event);
}

bool callback_data::isMemcpy()
{
	return memcpy_type != -1;
}

//Lock for the callbacks
pthread_mutex_t callback_lock;

int init_callback() 
{
#ifdef TAU_OPENCL_LOCKING
	printf("initalize pthread locking.\n");
	pthread_mutexattr_t lock_attr;
	pthread_mutexattr_init(&lock_attr);
	pthread_mutex_init(&callback_lock, &lock_attr);
	return 1;
#endif
}

void lock_callback()
{
#ifdef TAU_OPENCL_LOCKING
  static int initflag = init_callback();
	pthread_mutex_lock(&callback_lock);
#endif
}

void release_callback()
{
#ifdef TAU_OPENCL_LOCKING
	pthread_mutex_unlock(&callback_lock);
#endif
}

//The time in microseconds that the GPU is ahead of the CPU clock.
double sync_offset = 0;

double Tau_opencl_sync_clocks(cl_command_queue commandQueue, cl_context context)
{
	int d = 0;
	void *data = &d;
	cl_mem buffer;
	cl_int err;
	buffer = clCreateBuffer_noinst(context, CL_MEM_READ_WRITE |
	CL_MEM_ALLOC_HOST_PTR, sizeof(void *), NULL, &err);
	if (err != CL_SUCCESS)
	{
		printf("Cannot Create Sync Buffer: %d.\n", err);
		if (err == CL_INVALID_CONTEXT)
		{
			printf("Invalid context.\n");
		}
	  exit(1);	
	}

	double cpu_timestamp;	
  struct timeval tp;
	cl_ulong gpu_timestamp;

	cl_event sync_event;
	err = clEnqueueWriteBuffer_noinst(commandQueue, buffer, CL_TRUE, 0, sizeof(void*), data,  0, NULL, &sync_event);
	if (err != CL_SUCCESS)
	{
		printf("Cannot Enqueue Sync Kernel: %d.\n", err);
	  exit(1);	
	}

	//clWaitForEvents_noinst(1, &sync_event);
	//get CPU timestamp.
  gettimeofday(&tp, 0);
  cpu_timestamp = ((double)tp.tv_sec * 1e6 + tp.tv_usec);
	//get GPU timestamp for finish.
  err = clGetEventProfilingInfo_noinst(sync_event, CL_PROFILING_COMMAND_END,
															 sizeof(cl_ulong), &gpu_timestamp, NULL);
	if (err != CL_SUCCESS)
	{
		printf("Cannot get end time for Sync event.\n");
	  exit(1);	
	}

	//printf("SYNC: CPU= %f GPU= %f.\n", cpu_timestamp, ((double)gpu_timestamp/1e3)); 
	sync_offset = cpu_timestamp - (((double) gpu_timestamp)/1e3);

	return sync_offset;
}


void Tau_opencl_init()
{
	//printf("in Tau_opencl_init().\n");
	Tau_gpu_init();
}

void Tau_opencl_exit()
{
	//printf("in Tau_opencl_exit().\n");
	//delete &KernelBuffer;
	Tau_gpu_exit();
}

void Tau_opencl_enter_memcpy_event(const char *name, openCLGpuId *id, int size, int MemcpyType)
{
	Tau_gpu_enter_memcpy_event(name, id->getCopy(), size, MemcpyType);
}

void Tau_opencl_exit_memcpy_event(const char *name, openCLGpuId *id, int MemcpyType)
{
	Tau_gpu_exit_memcpy_event(name, id->getCopy(), MemcpyType);
}

void Tau_opencl_register_gpu_event(const char *name, openCLGpuId *gId, double start,
double stop, FunctionInfo* parent)
{
	lock_callback();
	//printf("locked for: %s.\n", name);
	eventId evId = Tau_gpu_create_gpu_event(name, gId, parent);
	Tau_gpu_register_gpu_event(evId, start/1e3, stop/1e3);
	//printf("released for: %s.\n", name);
	release_callback();
}

void Tau_opencl_register_memcpy_event(const char *name, openCLGpuId *gId, double start, double stop, int
transferSize, int MemcpyType, FunctionInfo* parent)
{
	//printf("in Tau_open.\n");
	//printf("Memcpy type is %d.\n", MemcpyType);
	lock_callback();
	//printf("locked for: %s.\n", name);
	FunctionInfo* p;
	eventId evId = Tau_gpu_create_gpu_event(name, gId, parent);
	Tau_gpu_register_memcpy_event(evId, start/1e3, stop/1e3, transferSize, MemcpyType);
	//printf("released for: %s.\n", name);
	release_callback();

}


void Tau_opencl_enqueue_event(callback_data* new_data)
{
	if (new_data->event == NULL)
	{
		//printf("null event in constructor!\n");
	}
	else
	{
		//printf("[TAU (opencl): adding kernel to buffer...");
		if (new_data->isMemcpy())
		{
			//printf("isMemcpy kind: %d.\n", new_data->memcpy_type);
		}
		else
		{
			//printf("isKernel.\n");
		}
		KernelBuffer.push(new_data);	
	}
}

bool buffer_front_is_complete()
{
  callback_data* front = KernelBuffer.front();
  cl_event* event = front->event;

  cl_int status, err;

	err = clGetEventInfo(*event, CL_EVENT_COMMAND_EXECUTION_STATUS, sizeof(cl_int),
	&status, NULL);
	if (err != CL_SUCCESS)
	{
		printf("Fatel error: calling clGetEventInfo, exitiing.\n");
		exit(1);
	}
	//printf("[TAU (opencl): event ready? %d.\n", status == CL_COMPLETE);

	return status == CL_COMPLETE;
}

void Tau_opencl_register_sync_event()
{
	//printf("[TAU (opencl): registering sync.\n");
	//printf("[TAU (opencl): empty buffer? %d.\n", KernelBuffer.empty());	
	//printf("[TAU (opencl): size of buffer: %d.\n", KernelBuffer.size());	
 while(!KernelBuffer.empty() 
 	 && buffer_front_is_complete())
 {
 	 cl_ulong startTime, endTime;

	 callback_data* kernel_data = KernelBuffer.front();
	 cl_int err;
	 err = clGetEventProfilingInfo_noinst(*kernel_data->event, CL_PROFILING_COMMAND_START,
																	 sizeof(cl_ulong), &startTime, NULL);
		if (err != CL_SUCCESS)
		{
			printf("Cannot get start time for Kernel event.\n");
			exit(1);	
		}

		err = clGetEventProfilingInfo_noinst(*kernel_data->event, CL_PROFILING_COMMAND_END,
																	 sizeof(cl_ulong), &endTime, NULL);
		if (err != CL_SUCCESS)
		{
			printf("Cannot get end time for Kernel event.\n");
			exit(1);	
		}

		if (kernel_data->isMemcpy())
		{
			//printf("TAU (opencl): isMemcpy kind: %d.\n", kernel_data->memcpy_type);
			Tau_opencl_register_memcpy_event(kernel_data->name, kernel_data->id, (double) startTime,
			(double) endTime, TAU_GPU_UNKNOW_TRANSFER_SIZE, kernel_data->memcpy_type,
			kernel_data->callingSite);
		}
		else
		{
			//printf("TAU (opencl): isKernel.\n");
			Tau_opencl_register_gpu_event(kernel_data->name, kernel_data->id, (double) startTime,
			(double) endTime, kernel_data->callingSite);
		}
		KernelBuffer.pop();

	}
}

void CL_CALLBACK Tau_opencl_memcpy_callback(cl_event event, cl_int command_stat, void
*data)
{
	callback_data *memcpy_data = (callback_data*) malloc(memcpy_data_size);
	memcpy(memcpy_data, data, memcpy_data_size);
	//printf("in memcpy callback!\n");
	//printf("in TauGpuAdapt, name: %s", memcpy_data->name);
	cl_ulong startTime, endTime;
	cl_int err;
  err = clGetEventProfilingInfo_noinst(event, CL_PROFILING_COMMAND_START,
															 sizeof(cl_ulong), &startTime, NULL);
	if (err != CL_SUCCESS)
	{
		printf("Cannot get start time for Memcpy event.\n");
	  exit(1);	
	}
  err = clGetEventProfilingInfo_noinst(event, CL_PROFILING_COMMAND_END,
															 sizeof(cl_ulong), &endTime, NULL);
	if (err != CL_SUCCESS)
	{
		printf("Cannot get end time for Memcpy event.\n");
	  exit(1);	
	}
	//printf("DtoH calling Tau_open.\n");
	Tau_opencl_register_memcpy_event(memcpy_data->name, 0, (double) startTime,
	(double) endTime, TAU_GPU_UNKNOW_TRANSFER_SIZE, memcpy_data->memcpy_type,
	memcpy_data->callingSite);
	
	free(data);
}


void CL_CALLBACK Tau_opencl_kernel_callback(cl_event event, cl_int command_stat, void
*data)
{
	callback_data *kernel_data = (callback_data*) malloc(kernel_data_size);
	//printf("memcpy size  %d.\n", kernel_data_size);
	memcpy(kernel_data, data, kernel_data_size);
	//printf("in kernel callback!\n");
	cl_ulong startTime, endTime;
	cl_int err;
  err = clGetEventProfilingInfo_noinst(event, CL_PROFILING_COMMAND_START,
															 sizeof(cl_ulong), &startTime, NULL);
	if (err != CL_SUCCESS)
	{
		printf("Cannot get start time for Kernel event.\n");
	  exit(1);	
	}
  err = clGetEventProfilingInfo_noinst(event, CL_PROFILING_COMMAND_END,
															 sizeof(cl_ulong), &endTime, NULL);
	if (err != CL_SUCCESS)
	{
		printf("Cannot get end time for Kernel event.\n");
	  exit(1);	
	}
		//printf("OpenCL.cpp: start timestamp: %.7f stop time %.7f.", (double) startTime, (double)endTime);
	  //printf("in TauGpuAdapt name: %s.\n", kernel_data->name);
		Tau_opencl_register_gpu_event(kernel_data->name, 0, (double) startTime,
		(double) endTime, kernel_data->callingSite);
	free(data);
}

