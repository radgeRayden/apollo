include (import saudio) "include/sokol/sokol_audio.h"

global *sample-rate* : i32
global *frame-acc* : u64 = 0

fn fetch-samples (buffer  frame-amount  channel-count)
    let sample-amount = frame-amount * channel-count
    for i in (range sample-amount)
        let t = (((*frame-acc* + i) as f32) * (1 / *sample-rate*)) #seconds
        let frequency = 261.63 #Hz
        buffer @ i = (sin (2 * pi * t * frequency))
        
    *frame-acc* += frame-amount
    ;

include "stdio.h"
fn init ()
    saudio.saudio_setup
        &
            local saudio.saudio_desc 
                stream_cb = (static-typify fetch-samples (mutable (pointer f32)) i32 i32)
    *sample-rate* = (saudio.saudio_sample_rate)
    ;


fn update-buffer ()
fn cleanup ()
    saudio.saudio_shutdown;

do
    let
        init
        cleanup
    locals;