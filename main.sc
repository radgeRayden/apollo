import .video
import .audio

include 
    (import sokol) 
""""#define SOKOL_NO_ENTRY
    #define SOKOL_IMPL
    #define SOKOL_GLCORE33
    #include "include/sokol/sokol_app.h"
    #include "include/sokol/sokol_gfx.h"
    #include "include/sokol/sokol_time.h"
    #include "include/sokol/sokol_audio.h"

include 
    (import C) 
""""#include <stdlib.h>
    #include <stdio.h>

SCREEN-WIDTH := 640
SCREEN-HEIGHT := 480

fn game-init ()
    sokol.sg_setup (&(local sokol.sg_desc))
    sokol.stm_setup;
    audio.init;
    video.init;
    ;

fn game-frame ()
    video.draw-screen; #implicit pass to default framebuffer
    ;

fn game-cleanup ()
    video.cleanup;
    audio.cleanup;

fn game-event (event)

fn main(argc argv)
    local app_desc : sokol.sapp_desc
        width = SCREEN-WIDTH
        height = SCREEN-HEIGHT
        init_cb = (static-typify game-init)
        frame_cb = (static-typify game-frame)
        cleanup_cb = (static-typify game-cleanup)
        event_cb = (static-typify game-event (pointer sokol.sapp_event))
        window_title = "game!!!!"
    (sokol.sapp_run &app_desc)
    return 0

# (main 0 0)

compile-object "main.o"
    do
        let main = 
            static-typify main (mutable (@ (mutable (@ i8))))
        locals;
    # 'no-debug-info
    # 'O2

# change this according to your environment. #works_on_my_machine
(C.system
    (.. "x86_64-w64-mingw32-gcc -g "
        \ "main.o sokol.c rnd.c "
        \ "-lkernel32 "
        \ "-lgdi32 "
        \ "-luser32 "
        \ "-lole32 "
        \ "-o game.exe "))