TAUROOT=
include $(TAUROOT)/include/Makefile
CC=$(TAU_CC) 
ARFLAGS=rcv 
CFLAGS=$(TAU_DEFS) $(TAU_INTERNAL_FLAG1) $(TAU_INCLUDE) 
INSTALLDEST = $(TAU_PREFIX_INSTALL_DIR)/$(CONFIG_ARCH)/lib





LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's PTHREAD DSO" 
#PTHREAD_AVAILABLE#LDPRELOAD_INSTALL_CMD=cp $(PTHREAD_LDPRELOAD_LIB) $(INSTALLDEST)/shared$(TAU_CONFIG) #ENDIF#
#NOSHARED#LDPRELOAD_INSTALL_CMD= #ENDIF#



#CRAYXMT#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#BGL#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#CATAMOUNT#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#CRAYX1CC#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#SUNCC#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#APPLEPGI#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#HITACHI#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#CRAYXMT#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#CRAYCC#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#OPEN64ORC#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#USE_NECCXX#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#NOSHARED#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#
#TAU_WINDOWS#LDPRELOAD_INSTALL_CMD=@echo "NOTE: Not building TAU's preload DSO" #ENDIF#


INSTALL_LD_LIB=install-ld-lib
#CRAYXMT#INSTALL_LD_LIB= #ENDIF#
#TAU_WINDOWS#INSTALL_LD_LIB= #ENDIF#
#BGL#INSTALL_LD_LIB= #ENDIF#
#CATAMOUNT#INSTALL_LD_LIB= #ENDIF#
#CRAYX1CC#INSTALL_LD_LIB= #ENDIF#
#SUNCC#INSTALL_LD_LIB= #ENDIF#
#APPLEPGI#INSTALL_LD_LIB= #ENDIF#
#HITACHI#INSTALL_LD_LIB= #ENDIF#
#CRAYXMT#INSTALL_LD_LIB= #ENDIF#
#CRAYCC#INSTALL_LD_LIB= #ENDIF#
#OPEN64ORC#INSTALL_LD_LIB= #ENDIF#
#USE_NECCXX#INSTALL_LD_LIB= #ENDIF#
#NOSHARED#INSTALL_LD_LIB= #ENDIF#


#PTHREAD_AVAILABLE#PTHREAD_LD_LIB=libTauPthreadWrap.a #ENDIF#
#CRAYXMT#PTHREAD_LD_LIB= #ENDIF#
#TAU_WINDOWS#PTHREAD_LD_LIB= #ENDIF#
#BGL#PTHREAD_LD_LIB= #ENDIF#
#CATAMOUNT#PTHREAD_LD_LIB= #ENDIF#
#CRAYX1CC#PTHREAD_LD_LIB= #ENDIF#
#SUNCC#PTHREAD_LD_LIB= #ENDIF#
#APPLEPGI#PTHREAD_LD_LIB= #ENDIF#
#HITACHI#PTHREAD_LD_LIB= #ENDIF#
#CRAYXMT#PTHREAD_LD_LIB= #ENDIF#
#CRAYCC#PTHREAD_LD_LIB= #ENDIF#
#OPEN64ORC#PTHREAD_LD_LIB= #ENDIF#
#USE_NECCXX#PTHREAD_LD_LIB= #ENDIF#
#NOSHARED#PTHREAD_LD_LIB= #ENDIF#

#PTHREAD_AVAILABLE#PTHREAD_LDPRELOAD_LIB=libTAU-pthread$(TAU_SHLIBX) #ENDIF#
#CRAYXMT#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#TAU_WINDOWS#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#BGL#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#CATAMOUNT#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#CRAYX1CC#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#SUNCC#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#APPLEPGI#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#HITACHI#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#CRAYXMT#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#CRAYCC#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#OPEN64ORC#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#USE_NECCXX#PTHREAD_LDPRELOAD_LIB= #ENDIF#
#NOSHARED#PTHREAD_LDPRELOAD_LIB= #ENDIF#

install: $(INSTALL_LD_LIB) install-preload-lib

$(INSTALL_LD_LIB): $(PTHREAD_LDPRELOAD_LIB)
	mkdir -p $(INSTALLDEST)/wrappers/pthread_wrapper
	cp link_options.tau $(INSTALLDEST)/wrappers/pthread_wrapper
	$(TAU_SHARED_INSTALL_CMD)

install-preload-lib: $(PTHREAD_LD_LIB)
	$(TAU_STATIC_INSTALL_CMD)

$(PTHREAD_LD_LIB): pthread_wrap_ld.o
	$(TAU_AR) $(ARFLAGS) $@ $<

pthread_wrap_ld.o: pthread_wrap.c
	$(CC) -DTAU_LIBRARY_SOURCE $(CFLAGS) -g -c $<  -o $@

$(PTHREAD_LDPRELOAD_LIB): pthread_wrap_preload.o
	$(CC) $(TAU_SHFLAGS) $@ $(TAU_SHLIBS) -L$(INSTALLDEST) $< -ldl

pthread_wrap_preload.o: pthread_wrap.c
	$(CC) -DTAU_LIBRARY_SOURCE -DTAU_PRELOAD_LIB $(CFLAGS) -g -c $< -o $@

# *CWL* This is REQUIRED for any component of TAU for a successful new build!
clean:
	/bin/rm -f pthread_wrap_preload.o pthread_wrap_ld.o $(PTHREAD_LD_LIB) $(PTHREAD_LDPRELOAD_LIB)