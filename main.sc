import .video
import .audio

include (options "-v") (import sokol) "sokol.c"

include 
    (import C) 
""""#include <stdlib.h>
    #include <stdio.h>

SCREEN-WIDTH := 640
SCREEN-HEIGHT := 480

fn game-init ()
    sokol.sg_setup (&(local sokol.sg_desc))
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

(main 0 0)

compile-object 
    default-target-triple
    compiler-file-kind-object
    "main.o"
    do
        let main = 
            static-typify main (mutable (@ (mutable (@ i8))))
        locals;
    'no-debug-info
    'O2

# change this according to your environment. #works_on_my_machine
(C.system
    (.. "x86_64-w64-mingw32-gcc "
        \ "main.o sokol.c rnd.c "
        \ "-O2 "
        \ "-lkernel32 "
        \ "-lgdi32 "
        \ "-luser32 "
        \ "-lole32 "
        \ "-o game.exe "))