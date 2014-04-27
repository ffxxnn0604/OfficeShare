LOCAL_PATH := $(call my-dir)
LOCAL_CPP_EXTENSION := .cpp

include $(CLEAR_VARS)
LOCAL_MODULE := udt
LOCAL_SRC_FILES := udt/md5.cpp udt/common.cpp udt/window.cpp udt/list.cpp udt/buffer.cpp udt/packet.cpp udt/channel.cpp udt/queue.cpp udt/ccc.cpp udt/cache.cpp udt/core.cpp udt/epoll.cpp udt/api.cpp 
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/udt
LOCAL_CPP_FEATURES += exceptions
#LOCAL_CFLAGS += -fexceptions
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := barchart-udt-core-2.3.0-SNAPSHOT
LOCAL_SRC_FILES := barchart_udt/com_barchart_udt_CCC.cpp barchart_udt/com_barchart_udt_SocketUDT.cpp barchart_udt/JNICCC.cpp barchart_udt/JNICCCFactory.cpp barchart_udt/JNIHelpers.cpp
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/barchart_udt
LOCAL_CPP_FEATURES += exceptions
LOCAL_CFLAGS += -fpermissive
LOCAL_SHARED_LIBRARIES := udt
include $(BUILD_SHARED_LIBRARY)