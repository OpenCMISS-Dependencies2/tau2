#ifndef __TAU_VAMPIRTRACE_H__
#define __TAU_VAMPIRTRACE_H__

#ifdef TAU_64BITTYPES_NEEDED
#include <Profile/vt_inttypes.h>
#endif /* TAU_64BITTYPES_NEEDED */

#ifndef VT_NO_ID
#define VT_NO_ID                  0xFFFFFFFF
#endif /* VT_NO_ID */

#ifndef VT_NO_LNO
#define VT_NO_LNO                 0xFFFFFFFF
#endif /* VT_NO_LNO */

#ifndef VT_FUNCTION
#define VT_FUNCTION               1
#endif /* VT_FUNCTION */

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

void vt_open(void);
void vt_close(void);

uint32_t vt_def_region      ( const char* rname,
                                     uint32_t fid,
                                     uint32_t begln,
                                     uint32_t endln,
                                     const char* rdesc,
                                     uint8_t rtype );

void vt_enter(uint64_t* time, uint32_t rid);

void vt_exit(uint64_t* time);
uint64_t vt_pform_wtime(void);


#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __TAU_VAMPIRTRACE_H__ */
