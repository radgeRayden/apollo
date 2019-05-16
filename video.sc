#
    in: console VRAM 
        320x240 palletized pixel data
        64-color (subject to change) pallete in RGB 24-bit format

# I wanted the shader code to be on its own scope, but then the private global problem comes back.
using import glsl
using import glm

#shader constants
let +palette-width+ = 4 
let +palette-height+ = 4
let +screen-width+ = 320
let +screen-height+ = 240

in position : vec4
in uv : vec2
inout screen-uv : vec2

fn vertex-shader ()
    screen-uv.out = uv
    gl_Position = position

+vertex-shader-source+ := (compile-glsl 330 'vertex (typify vertex-shader))

uniform screen-buffer : sampler2D
uniform palette : sampler2D
out out-color : vec4

fn frag-shader ()
    let screen-column = ((screen-uv.in.x * +screen-width+) as i32)
    let color-pair-byte = ((. ((texture screen-buffer screen-uv.in) * 255) r) as i32)

    let color-index =
        if ((screen-column % 2) == 0)
            (color-pair-byte & 0x0f)
        else
            (color-pair-byte >> 4)
    
    let pcolor = 
        texelFetch 
            palette 
            ivec2 
                (color-index % +palette-width+) 
                (color-index // +palette-width+) 
            0

    out-color = (vec4 pcolor.rgb 1.0)

+fragment-shader-source+ := (compile-glsl 330 'fragment (typify frag-shader))
run-stage;

include "stdio.h"
include "stdlib.h"
include "time.h"
include 
    (import rnd) 
""""#define RND_IMPLEMENTATION
    #include "include/rnd.h"

include 
    (import sokol)
""""#include "include/sokol/sokol_gfx.h"
    #include "include/sokol/sokol_app.h"

#rng stuff
global *rnd-well* : rnd.rnd_well_t

#VRAM state globals
global *screen-buffer* : (mutable (pointer i8))
global *palette-buffer* : (mutable (pointer i8))

global *screen-texture* : sokol.sg_image
global *palette-texture* : sokol.sg_image
global *gfx-bindings* : sokol.sg_bindings
global *gfx-pipeline* : sokol.sg_pipeline

# TODO: change into own module so every module uses the same constants
# vram constants
let +screen-buffer-size+ = (+screen-width+ * +screen-height+ // 2)
let +palette-buffer-size+ = (+palette-width+ * +palette-height+ * (sizeof i32))

using import enum
enum memfill plain
    incremental = 0
    random
    incremental-4bit
    random-4bit
    zero


fn fill-buffer (mem size method)
    switch method
    case memfill.incremental
        for i in (range size)
            if (size < 256)
                #can't express the whole range, so we subdivide it
                mem @ i = (i * ((256 / size) as i32) % 256) as i8
            else
                mem @ i = (i % 256) as i8
    pass memfill.random-4bit
    case memfill.random
        for i in (range size)
            mem @ i = ((rnd.rnd_well_next &*rnd-well*) % 256) as i8
    case memfill.incremental-4bit
        for i in (range size)
            let high-nibble = (((i % 8) * 2) << 4)
            let low-nibble = (((i % 8) * 2) + 1)
            mem @ i = (high-nibble | low-nibble) as i8
    case memfill.zero
        for i in (range size)
            mem @ i = 0:i8
    default ();
    ;

fn init ()
    rnd.rnd_well_seed  &*rnd-well*  ((clock) as u32)

    using sokol

    local screen-quad =
        #TODO: change this to account for screen scaling using a projection matrix
        arrayof f32
            #   position         uv
            \ -1.0  1.0  0.5  0.0  1.0   # top-left
            \ -1.0 -1.0  0.5  0.0  0.0    # bottom-left
            \  1.0 -1.0  0.5  1.0  0.0    # bottom-right
            \ -1.0  1.0  0.5  0.0  1.0    # top-left
            \  1.0 -1.0  0.5  1.0  0.0    # bottom-right
            \  1.0  1.0  0.5  1.0  1.0    # top-right
    

    *gfx-bindings*.vertex_buffers @ 0 = 
        sg_make_buffer
            &
                local sg_buffer_desc
                    size = (sizeof screen-quad)
                    type = SG_BUFFERTYPE_VERTEXBUFFER
                    usage = _SG_USAGE_DEFAULT
                    content = &screen-quad
                    label = "screen-quad"

    
    local shader-desc : sg_shader_desc
        vs = (local sg_shader_stage_desc (source = +vertex-shader-source+))
        fs = 
            local sg_shader_stage_desc 
                source = +fragment-shader-source+
        label = "console-screen-shader"
    (. (shader-desc.attrs @ 0) name) = "position"
    (. (shader-desc.attrs @ 0) sem_name) = "POS"
    (. (shader-desc.attrs @ 1) name) = "uv"
    (. (shader-desc.attrs @ 1) sem_name) = "UV"
    (shader-desc.fs.images @ 0) =
        local sg_shader_image_desc
            name = "screen_buffer"
            type = SG_IMAGETYPE_2D
    (shader-desc.fs.images @ 1) =
        local sg_shader_image_desc
            name = "palette"
            type = SG_IMAGETYPE_2D

    local console-screen-shader = (sg_make_shader &shader-desc)

    local def-stencil-state : sg_stencil_state
        fail_op = _SG_STENCILOP_DEFAULT
        depth_fail_op = _SG_STENCILOP_DEFAULT
        pass_op = _SG_STENCILOP_DEFAULT
        compare_func = _SG_COMPAREFUNC_DEFAULT
    
    local pip-desc : sg_pipeline_desc
        shader = console-screen-shader
        primitive_type = _SG_PRIMITIVETYPE_DEFAULT
        index_type = _SG_INDEXTYPE_DEFAULT
        depth_stencil =
            local sg_depth_stencil_state
                stencil_front = def-stencil-state
                stencil_back = def-stencil-state
                depth_compare_func = _SG_COMPAREFUNC_DEFAULT
        blend =
            local sg_blend_state
                src_factor_rgb = _SG_BLENDFACTOR_DEFAULT
                dst_factor_rgb = _SG_BLENDFACTOR_DEFAULT
                op_rgb = _SG_BLENDOP_DEFAULT
                src_factor_alpha = _SG_BLENDFACTOR_DEFAULT
                dst_factor_alpha = _SG_BLENDFACTOR_DEFAULT
                op_alpha = _SG_BLENDOP_DEFAULT
                color_format = _SG_PIXELFORMAT_DEFAULT
                depth_format = _SG_PIXELFORMAT_DEFAULT
        rasterizer =
            local sg_rasterizer_state
                cull_mode = _SG_CULLMODE_DEFAULT
                face_winding = _SG_FACEWINDING_DEFAULT
        label = "console-screen-pipeline"

    pip-desc.layout.attrs @ 0 = (local sg_vertex_attr_desc (format = SG_VERTEXFORMAT_FLOAT3))
    pip-desc.layout.attrs @ 1 = (local sg_vertex_attr_desc (format = SG_VERTEXFORMAT_FLOAT2))

    *gfx-pipeline* = (sg_make_pipeline  &pip-desc)

    *screen-buffer* = (malloc-array i8 +screen-buffer-size+) 
    *palette-buffer* = (malloc-array i8 +palette-buffer-size+)

    *screen-texture* =
        sg_make_image
            &
                local sg_image_desc
                    type = SG_IMAGETYPE_2D
                    width = (+screen-width+ // 2) #FIXME: we need better names to convey why this is necessary
                    height = +screen-height+
                    usage = SG_USAGE_STREAM
                    pixel_format = SG_PIXELFORMAT_L8
                    min_filter = SG_FILTER_NEAREST
                    mag_filter = SG_FILTER_NEAREST
                    wrap_u = SG_WRAP_REPEAT
                    wrap_v = SG_WRAP_REPEAT
                    wrap_w = SG_WRAP_REPEAT
    
    *gfx-bindings*.fs_images @ 0 = *screen-texture*

    *palette-texture* =
        sg_make_image
            &
                local sg_image_desc
                    type = SG_IMAGETYPE_2D
                    width = +palette-width+
                    height = +palette-height+
                    usage = SG_USAGE_STREAM
                    pixel_format = SG_PIXELFORMAT_RGBA8
                    min_filter = SG_FILTER_NEAREST
                    mag_filter = SG_FILTER_NEAREST
                    wrap_u = SG_WRAP_REPEAT
                    wrap_v = SG_WRAP_REPEAT
                    wrap_w = SG_WRAP_REPEAT
    
    *gfx-bindings*.fs_images @ 1 = *palette-texture*

    fill-buffer  *palette-buffer*  +palette-buffer-size+  memfill.random
    ;

fn draw-screen ()
    fill-buffer *screen-buffer* +screen-buffer-size+ memfill.incremental-4bit
    # fill-buffer  *palette-buffer*  +palette-buffer-size+  memfill.random

    local screen-content : sokol.sg_image_content
    (@ screen-content.subimage 0 0) =
        local sokol.sg_subimage_content
            ptr = (bitcast (deref *screen-buffer*) voidstar)
            size = +screen-buffer-size+
    local palette-content : sokol.sg_image_content
    (@ palette-content.subimage 0 0) =
        local sokol.sg_subimage_content
            ptr = (bitcast (deref *palette-buffer*) voidstar)
            size = +palette-buffer-size+

    local pass-action : sokol.sg_pass_action
        depth =
            local sokol.sg_depth_attachment_action
                action = sokol._SG_ACTION_DEFAULT
        stencil =
            local sokol.sg_stencil_attachment_action
                action = sokol._SG_ACTION_DEFAULT
    pass-action.colors @ 0 = 
        local sokol.sg_color_attachment_action
            action = sokol.SG_ACTION_CLEAR
            val = (arrayof f32 0.14 0.14 0.14 1.0)

    sokol.sg_update_image  *screen-texture*  &screen-content
    sokol.sg_update_image  *palette-texture*  &palette-content
    sokol.sg_begin_default_pass  &pass-action  (sokol.sapp_width)  (sokol.sapp_height)
    sokol.sg_apply_pipeline  *gfx-pipeline* 
    sokol.sg_apply_bindings  &*gfx-bindings*
    sokol.sg_draw  0  6  1
    sokol.sg_end_pass;
    sokol.sg_commit;

fn cleanup ()
    free *screen-buffer*
    free *palette-buffer*
    sokol.sg_destroy_image *screen-texture*
    sokol.sg_destroy_image *palette-texture*
    ;

do
    let
        init
        draw-screen
        cleanup
    locals;
