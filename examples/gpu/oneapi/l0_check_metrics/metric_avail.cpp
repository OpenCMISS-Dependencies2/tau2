//==============================================================
// Copyright (C) Intel Corporation
//
// SPDX-License-Identifier: MIT
// =============================================================


#include <iostream>
#include "ze_utils.h"


static void PrintMetricsInfo(ze_device_handle_t device) {
  PTI_ASSERT(device != nullptr);
  ze_result_t status = ZE_RESULT_SUCCESS;

  std::cout << "Target device: " <<
    utils::ze::GetDeviceName(device) << std::endl;

  uint32_t group_count = 0;
  status = zetMetricGroupGet(device, &group_count, nullptr);
  if (status != ZE_RESULT_SUCCESS || group_count == 0) {
    std::cout << "[WARNING] No metrics found" << std::endl;
    return;
  }

  std::vector<zet_metric_group_handle_t> group_list(group_count, nullptr);
  status = zetMetricGroupGet(device, &group_count, group_list.data());
  PTI_ASSERT(status == ZE_RESULT_SUCCESS);
  
  std::cout << "Printing EBS Metric Groups." << std::endl;

  for (uint32_t gid = 0; gid < group_count; ++gid) {
    zet_metric_group_properties_t group_props{};
    group_props.stype = ZET_STRUCTURE_TYPE_METRIC_GROUP_PROPERTIES;
    status = zetMetricGroupGetProperties(group_list[gid], &group_props);
    PTI_ASSERT(status == ZE_RESULT_SUCCESS);

    bool is_ebs = (group_props.samplingType &
                   ZET_METRIC_GROUP_SAMPLING_TYPE_FLAG_EVENT_BASED);
    bool is_tbs = (group_props.samplingType &
                   ZET_METRIC_GROUP_SAMPLING_TYPE_FLAG_TIME_BASED);
    PTI_ASSERT(is_ebs || is_tbs);
    std::string sampling_type;
    if (is_ebs && is_tbs) {
      sampling_type = "EBS, TBS";
      continue;
    } else if (is_ebs) {
      sampling_type = "EBS";
    } else {
      sampling_type = "TBS";
      continue;
    }

    std::cout << "Metric Group " << gid << ": D" << group_props.domain <<
      " / " << sampling_type << " / " << group_props.name << " (" <<
      group_props.description << ")" << std::endl;

    uint32_t metric_count = group_props.metricCount;
    std::vector<zet_metric_handle_t> metric_list(metric_count, nullptr);
    status = zetMetricGet(group_list[gid], &metric_count, metric_list.data());
    PTI_ASSERT(status == ZE_RESULT_SUCCESS);
    PTI_ASSERT(metric_count == group_props.metricCount);

    for (uint32_t mid = 0; mid < metric_count; ++mid) {
      zet_metric_properties_t metric_props{};
      metric_props.stype = ZET_STRUCTURE_TYPE_METRIC_PROPERTIES;
      status = zetMetricGetProperties(metric_list[mid], &metric_props);
      PTI_ASSERT(status == ZE_RESULT_SUCCESS);

      std::cout << "\tMetric " << mid << ": D" << group_props.domain <<
        " / " << sampling_type << " / " << group_props.name << " / " <<
        metric_props.name << " (" << metric_props.description << ") [" <<
        utils::ze::GetResultType(metric_props.resultType) << ", " <<
        utils::ze::GetMetricType(metric_props.metricType) << "]" << std::endl;
    }
  }
}



int main()
{

  utils::SetEnv("ZET_ENABLE_METRICS", "1");
  
	ze_result_t status = ZE_RESULT_SUCCESS;
	status = zeInit(ZE_INIT_FLAG_GPU_ONLY);
	assert(status == ZE_RESULT_SUCCESS);
 
 
  ze_device_handle_t device = utils::ze::GetGpuDevice();
  if (device == nullptr) {
    std::cout << "Unable to find target device" << std::endl;
    return 0;
  }
 

  
   PrintMetricsInfo(device);
 
 
  return 0;
}
