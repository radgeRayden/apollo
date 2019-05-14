include (import sokol)
""""#include "include/sokol/sokol_audio.h"

fn fetch-samples (buffer  frame-amount  channel-count)
    ;

fn init () (using sokol)
    saudio_setup
        &
            local saudio_desc
                stream_cb = fetch-samples
    ;
fn update-buffer ()
fn cleanup ()
    sokol.saudio_shutdown;

do
    let
        init
        cleanup
    locals;