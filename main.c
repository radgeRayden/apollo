
#define SOKOL_IMPL
#define SOKOL_GLCORE33
#define SOKOL_NO_ENTRY

#include "include/sokol/sokol_app.h"
#include "include/sokol/sokol_audio.h"

void fetch_samples(float* buffer, int num_frames, int channel_count) {

}

void init() {
    saudio_desc audio_desc = {
        .stream_cb = fetch_samples
    };
    saudio_setup(&audio_desc);

}

void frame() {

}

void cleanup() {
    saudio_shutdown();
}

void event(const sapp_event* event) {
}

int main(int argc, char** argv) {
    sapp_desc app_desc = {
        .width = 640,
        .height = 480,
        .init_cb = init,
        .frame_cb = frame,
        .cleanup_cb = cleanup,
        .event_cb = event,
        .window_title = "C version"
    };
    sapp_run(&app_desc);
}