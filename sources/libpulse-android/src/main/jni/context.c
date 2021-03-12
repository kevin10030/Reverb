/*******************************************************************************
*Copyright (c) 2012 Harrison Chapman.
*
*This file is part of Libpulse-android.
*
*    Reverb is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    Reverb is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with Reverb.  If not, see <http://www.gnu.org/licenses/>.
*
*Contributors:
*    Harrison Chapman - initial API and implementation
*******************************************************************************/

#include <jni.h>
#include <pulse/pulseaudio.h>

#include "jni_core.h"
#include "context.h"
#include "logging.h"

#include "context_util.h"

int created_num = 0;

JNIEXPORT jlong JNICALL
Java_com_harrcharr_pulse_PulseContext_JNICreate(
		JNIEnv *jenv, jclass jcls, pa_threaded_mainloop *m) {
	dlog(0, "creating pulse context for the %d th time", created_num++);
	dlog(0, "%d", m);
	pa_mainloop_api *api = pa_threaded_mainloop_get_api(m);
	pa_context *c = pa_context_new(api, "primary");

	return c;
}

JNIEXPORT jint JNICALL
Java_com_harrcharr_pulse_PulseContext_connect(
		JNIEnv *jenv, jobject jobj, jstring server) {
	pa_context *c = (pa_context *)get_obj_ptr(jenv, jobj);
    const jbyte *srv;
    srv = (*jenv)->GetStringUTFChars(jenv, server, NULL);
    if (srv == NULL) {
        return NULL; /* OutOfMemoryError already thrown */
    }
	int result = pa_context_connect(c, srv, PA_CONTEXT_NOFAIL, NULL);
    (*jenv)->ReleaseStringUTFChars(jenv, server, srv);

    if (result < 0) {
    	// An error occurred during server connection
    	throw_exception(jenv, "java/lang/Exception", pa_strerror(pa_context_errno(c)));
    }
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_disconnect(
		JNIEnv *jenv, jobject jobj) {
	pa_context *c = (pa_context *)get_obj_ptr(jenv, jobj);
	pa_context_disconnect(c);
	pa_context_unref(c);
}

JNIEXPORT jint JNICALL
Java_com_harrcharr_pulse_PulseContext_getStatus(
		JNIEnv *jenv, jobject jobj) {
	pa_context *c = (pa_context *)get_obj_ptr(jenv, jobj);
	return pa_context_get_state(c);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setStateCallback(
		JNIEnv *jenv, jobject jobj, jobject runnable) {
	pa_context *c = (pa_context *)get_obj_ptr(jenv, jobj);

	jni_pa_cb_info_t *cbinfo = (jni_pa_cb_info_t*)malloc(sizeof(jni_pa_cb_info_t));
	cbinfo->cb_runnable = (*jenv)->NewGlobalRef(jenv, runnable);
	cbinfo->m = NULL;
	cbinfo->to_free = NULL;

	pa_context_set_state_callback(c, context_state_cb, cbinfo);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getSinkInfo(
		JNIEnv *jenv, jobject jcontext, jint idx, jobject runnable) {
	context_synchronized_info_call(
			jenv, jcontext, runnable,
			&pa_context_get_sink_info_by_index, (uint32_t)idx,
			info_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getSinkInfoList(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	context_synchronized_info_list_call(
			jenv, jcontext, runnable,
			&pa_context_get_sink_info_list,
			info_cb);
}


JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setSinkMute(
		JNIEnv *jenv, jobject jcontext, jint idx, jboolean mute, jobject runnable) {
	context_synchronized_mute_call(
			jenv, jcontext, runnable,
			&pa_context_set_sink_mute_by_index, (uint32_t)idx,
			(int) mute, success_cb);
}


JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setSinkVolume(
		JNIEnv *jenv, jobject jcontext,
		jint idx, jintArray volumes,
		jobject runnable) {
	context_synchronized_volume_call(
			jenv, jcontext, runnable,
			&pa_context_set_sink_volume_by_index, (uint32_t)idx,
			volumes, success_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getSinkInputInfoList(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	context_synchronized_info_list_call(
			jenv, jcontext, runnable,
			&pa_context_get_sink_input_info_list,
			info_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getSinkInputInfo(
		JNIEnv *jenv, jobject jcontext, jint idx,
		jobject runnable) {
	context_synchronized_info_call(
			jenv, jcontext, runnable,
			&pa_context_get_sink_input_info, (uint32_t)idx,
			info_cb);
}


JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setSinkInputMute(
		JNIEnv *jenv, jobject jcontext, jint idx, jboolean mute,
		jobject runnable) {
	context_synchronized_mute_call(
			jenv, jcontext, runnable,
			&pa_context_set_sink_input_mute, (uint32_t)idx,
			(int) mute, success_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setSinkInputVolume(
		JNIEnv *jenv, jobject jcontext,
		jint idx, jintArray volumes,
		jobject runnable) {
	context_synchronized_volume_call(
			jenv, jcontext, runnable,
			&pa_context_set_sink_input_volume, (uint32_t)idx,
			volumes, success_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_moveSinkInput(
		JNIEnv *jenv, jobject jcontext,
		jint idx, jint sink_idx,
		jobject runnable) {
	context_synchronized_move_call(
			jenv, jcontext, runnable,
			&pa_context_move_sink_input_by_index, (uint32_t)idx,
			(uint32_t)sink_idx,
			success_cb);
}


JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getSourceInfo(
		JNIEnv *jenv, jobject jcontext, jint idx, jobject runnable) {
	context_synchronized_info_call(
			jenv, jcontext, runnable,
			&pa_context_get_source_info_by_index, (uint32_t)idx,
			info_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getSourceInfoList(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	context_synchronized_info_list_call(
			jenv, jcontext, runnable,
			&pa_context_get_source_info_list,
			info_cb);
}


JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setSourceMute(
		JNIEnv *jenv, jobject jcontext, jint idx, jboolean mute, jobject runnable) {
	context_synchronized_mute_call(
			jenv, jcontext, runnable,
			&pa_context_set_source_mute_by_index, (uint32_t)idx,
			(int) mute, success_cb);
}


JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setSourceVolume(
		JNIEnv *jenv, jobject jcontext,
		jint idx, jintArray volumes,
		jobject runnable) {
	context_synchronized_volume_call(
			jenv, jcontext, runnable,
			&pa_context_set_source_volume_by_index, (uint32_t)idx,
			volumes, success_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getSourceOutputInfoList(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	context_synchronized_info_list_call(
			jenv, jcontext, runnable,
			&pa_context_get_source_output_info_list,
			info_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getSourceOutputInfo(
		JNIEnv *jenv, jobject jcontext, jint idx,
		jobject runnable) {
	context_synchronized_info_call(
			jenv, jcontext, runnable,
			&pa_context_get_source_output_info, (uint32_t)idx,
			info_cb);
}


JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setSourceOutputMute(
		JNIEnv *jenv, jobject jcontext, jint idx, jboolean mute,
		jobject runnable) {
	context_synchronized_mute_call(
			jenv, jcontext, runnable,
			&pa_context_set_source_output_mute, (uint32_t)idx,
			(int) mute, success_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setSourceOutputVolume(
		JNIEnv *jenv, jobject jcontext,
		jint idx, jintArray volumes,
		jobject runnable) {
	context_synchronized_volume_call(
			jenv, jcontext, runnable,
			&pa_context_set_source_output_volume, (uint32_t)idx,
			volumes, success_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_moveSourceOutput(
		JNIEnv *jenv, jobject jcontext,
		jint idx, jint source_idx,
		jobject runnable) {
	context_synchronized_move_call(
			jenv, jcontext, runnable,
			&pa_context_move_source_output_by_index, (uint32_t)idx,
			(uint32_t)source_idx,
			success_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getClientInfo(
		JNIEnv *jenv, jobject jcontext, jint idx, jobject runnable) {
	context_synchronized_info_call(
			jenv, jcontext, runnable,
			&pa_context_get_client_info, (uint32_t)idx,
			info_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_getClientInfoList(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	context_synchronized_info_list_call(
			jenv, jcontext, runnable,
			&pa_context_get_client_info_list,
			info_cb);
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setConnectionReadyCallback(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	pa_context *c = get_context_ptr(jenv, jcontext);
	jni_pa_state_cbs_t *cbs = get_state_cbs_ptr(jenv, jcontext);

	if (cbs == NULL && runnable != NULL) {
		cbs = new_state_cbs();
		set_state_cbs_ptr(jenv, jcontext, cbs);
		pa_context_set_state_callback(c, context_state_cb, cbs);
	}
	if (cbs != NULL && cbs->ready_cbo != NULL) {
		del_cb_globalref(jenv, cbs->ready_cbo);
	}
	if (runnable != NULL) {
		cbs->ready_cbo = get_cb_globalref(jenv, jcontext, runnable);
	}
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_setConnectionFailedCallback(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	pa_context *c = get_context_ptr(jenv, jcontext);
	jni_pa_state_cbs_t *cbs = get_state_cbs_ptr(jenv, jcontext);

	if (cbs == NULL && runnable != NULL) {
		cbs = new_state_cbs();
		set_state_cbs_ptr(jenv, jcontext, cbs);
		pa_context_set_state_callback(c, context_state_cb, cbs);
	}
	if (cbs != NULL && cbs->failed_cbo != NULL) {
		del_cb_globalref(jenv, cbs->failed_cbo);
	}
	if (runnable != NULL) {
		cbs->failed_cbo = get_cb_globalref(jenv, jcontext, runnable);
	}
}


JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_JNISubscribeSinkInput(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	pa_context *c = get_context_ptr(jenv, jcontext);
	jni_pa_event_cbs_t *cbs = get_event_cbs_ptr(jenv, jcontext);

	if (cbs == NULL && runnable != NULL) {
		cbs = new_event_cbs();
		set_event_cbs_ptr(jenv, jcontext, cbs);
		pa_context_set_subscribe_callback(c, context_subscription_cb, cbs);
	}
	if (cbs != NULL && cbs->sink_input_cbo != NULL) {
		del_cb_globalref(jenv, cbs->sink_input_cbo);
		cbs->sink_input_cbo = NULL;
	}
	if (runnable != NULL) {
		cbs->sink_input_cbo = get_cb_globalref(jenv, jcontext, runnable);
	}
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_JNISubscribeSourceOutput(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	pa_context *c = get_context_ptr(jenv, jcontext);
	jni_pa_event_cbs_t *cbs = get_event_cbs_ptr(jenv, jcontext);

	if (cbs == NULL && runnable != NULL) {
		cbs = new_event_cbs();
		set_event_cbs_ptr(jenv, jcontext, cbs);
		pa_context_set_subscribe_callback(c, context_subscription_cb, cbs);
	}
	if (cbs != NULL && cbs->source_output_cbo != NULL) {
		del_cb_globalref(jenv, cbs->source_output_cbo);
		cbs->source_output_cbo = NULL;
	}
	if (runnable != NULL) {
		cbs->source_output_cbo = get_cb_globalref(jenv, jcontext, runnable);
	}
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_JNISubscribeSink(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	pa_context *c = get_context_ptr(jenv, jcontext);
	jni_pa_event_cbs_t *cbs = get_event_cbs_ptr(jenv, jcontext);

	if (cbs == NULL && runnable != NULL) {
		cbs = new_event_cbs();
		set_event_cbs_ptr(jenv, jcontext, cbs);
		pa_context_set_subscribe_callback(c, context_subscription_cb, cbs);
	}
	if (cbs != NULL && cbs->sink_cbo != NULL) {
		del_cb_globalref(jenv, cbs->sink_cbo);
		cbs->sink_cbo = NULL;
	}
	if (runnable != NULL) {
		cbs->sink_cbo = get_cb_globalref(jenv, jcontext, runnable);
	}
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_JNISubscribeSource(
		JNIEnv *jenv, jobject jcontext, jobject runnable) {
	pa_context *c = get_context_ptr(jenv, jcontext);
	jni_pa_event_cbs_t *cbs = get_event_cbs_ptr(jenv, jcontext);

	if (cbs == NULL && runnable != NULL) {
		cbs = new_event_cbs();
		set_event_cbs_ptr(jenv, jcontext, cbs);
		pa_context_set_subscribe_callback(c, context_subscription_cb, cbs);
	}
	if (cbs != NULL && cbs->source_cbo != NULL) {
		del_cb_globalref(jenv, cbs->source_cbo);
		cbs->source_cbo = NULL;
	}
	if (runnable != NULL) {
		cbs->source_cbo = get_cb_globalref(jenv, jcontext, runnable);
	}
}

JNIEXPORT void JNICALL
Java_com_harrcharr_pulse_PulseContext_JNISubscribe(
		JNIEnv *jenv, jobject jcontext) {
	pa_context *c = get_context_ptr(jenv, jcontext);
	pa_threaded_mainloop *m = get_mainloop_ptr(jenv, jcontext);

	LOGE("Subscribing, pulse state %d", pa_context_get_state(c));
	LOGE("Subscribing, pulse state %d", pa_context_get_state(c));
	LOGE("Subscribing, pulse state %d", pa_context_get_state(c));

	pa_threaded_mainloop_lock(m);

	pa_operation *o;
	o = pa_context_subscribe(c, PA_SUBSCRIPTION_MASK_ALL, NULL, NULL);
	assert(o);

	pa_operation_unref(o);
	pa_threaded_mainloop_unlock(m);
}
