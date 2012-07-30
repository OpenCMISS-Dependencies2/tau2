#include <Profile/TauGpu.h>
#include <stdlib.h>

extern "C" void Tau_metadata_task(char *name, const char* value, int tid);

#define uint32_t unsigned int

struct {
	GpuMetadata *list;
	int length;
	} typedef metadata_struct;

map<uint32_t, FunctionInfo*> functionInfoMap;

map<uint32_t, metadata_struct> deviceInfoMap;

class CuptiGpuEvent : public GpuEvent
{
public:
	uint32_t streamId;
	uint32_t contextId;
	uint32_t deviceId;
	uint32_t correlationId;

	//This event is tied to the entire deivce not a particular stream or context.
	//Used for recording device metadata.
	bool deviceContainer;
	
	const char *name;
	//FunctionInfo *callingSite;
	GpuEventAttributes *gpu_event_attributes;
	int number_of_gpu_attributes;

	/*CuptiGpuEvent(uint32_t s, uint32_t cn, uint32_t c) { streamId = s; contextId = cn ; correlationId = c; };*/
	CuptiGpuEvent *getCopy() const { 
		CuptiGpuEvent *c = new CuptiGpuEvent(*this);
		return c; 
	};
	CuptiGpuEvent(const char* n, uint32_t device, GpuEventAttributes *m, int m_size) : name(n), deviceId(device), gpu_event_attributes(m), number_of_gpu_attributes(m_size) {
		deviceContainer = true;
		streamId = 0;
		contextId = 0;
		correlationId = -1;
	};
	CuptiGpuEvent(const char* n, uint32_t device, uint32_t stream, uint32_t context, uint32_t correlation, GpuEventAttributes *m, int m_size) : name(n), deviceId(device), streamId(stream), contextId(context), correlationId(correlation), gpu_event_attributes(m), number_of_gpu_attributes(m_size) {
		deviceContainer = false;
	};

	const char* getName() const { return name; }

	const char* gpuIdentifier() const {
		char *rtn = (char*) malloc(50*sizeof(char));
		sprintf(rtn, "%d/%d/%d/%d", deviceId, streamId, contextId, correlationId);
		return rtn;
	};
	x_uint64 id_p1() const {
		return correlationId;
	};
	x_uint64 id_p2() const { 
		return RtsLayer::myNode(); 
	};
	bool less_than(const GpuEvent *other) const
	{	
		if (deviceContainer || ((CuptiGpuEvent *)other)->deviceContainer) {
			return deviceId < ((CuptiGpuEvent *)other)->deviceId;
		}
		else {
			if (contextId == ((CuptiGpuEvent *)other)->context()) {
				return streamId < ((CuptiGpuEvent *)other)->stream();
			} else {
				return contextId < ((CuptiGpuEvent *)other)->context();
			}
		}
		/*
		if (ret) { printf("%s equals %s.\n", printId(), ((CuptiGpuEvent *)other)->printId()); }
		else { printf("%s does not equal %s.\n", printId(), ((CuptiGpuEvent *)other)->printId());}
		return ret;
		*/
	};

	void getAttributes(GpuEventAttributes *&gA, int &num) const
	{
		num = number_of_gpu_attributes;
		gA = gpu_event_attributes;
	}

	void recordMetadata(int id) const
	{
		map<uint32_t, metadata_struct>::iterator it = deviceInfoMap.find(deviceId);
		if (it != deviceInfoMap.end())
		{
			GpuMetadata *gpu_metadata = it->second.list;
			int number_of_gpu_metadata = it->second.length;
			//printf("recording %d.\n", number_of_gpu_metadata);
			for (int i=0;i<number_of_gpu_metadata;i++)
			{
				Tau_metadata_task(gpu_metadata[i].name, gpu_metadata[i].value, id);
			}
		}
	}

	double syncOffset() const { return 0; };
	uint32_t stream() { return streamId; };
	uint32_t context() { return contextId; };
	FunctionInfo* getCallingSite() const
	{
		FunctionInfo *funcInfo = NULL;
		map<uint32_t, FunctionInfo*>::iterator it = functionInfoMap.find(correlationId);
		if (it != functionInfoMap.end())
		{
			funcInfo = it->second;
		}
		return funcInfo;
	};

	~CuptiGpuEvent()
	{
		free(gpu_event_attributes);
	}

};
