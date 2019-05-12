#
    in: console VRAM 
        320x240 palletized pixel data
        64-color (subject to change) pallete in RGB 24-bit format

# global +vertex-shader-source+ : (pointer i8)
# global +fragment-shader-source+ : (pointer i8)

# I wanted the shader code to be on its own scope, but then the private global problem comes back.
using import glsl
using import glm

print "we got here"
#shader constants
let +palette-width+ = 4 
let +palette-height+ = 4

in position : vec4
in uv : vec2
inout screen-uv : vec2

fn vertex-shader ()
    screen-uv.out = uv
    gl_Position = position

+vertex-shader-source+ := (compile-glsl 'vertex (typify vertex-shader))

uniform screen-buffer : sampler2D
uniform palette : sampler2D
out out-color : vec4

fn frag-shader ()
    let texel = (texture screen-buffer screen-uv.in)
    out-color = (vec4 texel.r texel.r texel.r 1.0)
        # vec2
        #     color-index % PALETTE-WIDTH
        #     0
            #color-index // PALETTE-WIDTH
    # out-color = (texelFetch palette (ivec2 0 0) 0) #FIXME: lod should be optional

+fragment-shader-source+ := (compile-glsl 'fragment (typify frag-shader))
print +fragment-shader-source+
print +vertex-shader-source+

run-stage;

include "stdio.h"
include "stdlib.h"
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

global *screen-texture* : sokol.sg_image
global *gfx-bindings* : sokol.sg_bindings
global *gfx-pipeline* : sokol.sg_pipeline

# TODO: change into own module so every module uses the same constants
# vram constants
let +screen-width+ = 320
let +screen-height+ = 240
let +screen-buffer-size+ = (+screen-width+ * +screen-height+ // 2)
print +screen-buffer-size+
fn fill-video-memory (mem)
    for i in (range +screen-buffer-size+) 
        mem @ i = ((rnd.rnd_well_next &*rnd-well*) % 255) as i8
    ;

fn fill-palette (mem)

    

fn init ()
    rnd.rnd_well_seed  &*rnd-well*  0

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

    #TODO: change into union later
    *screen-buffer* = (malloc-array i8 +screen-buffer-size+) 

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
    
    fill-video-memory *screen-buffer*
    ;

fn draw-screen ()
    fill-video-memory *screen-buffer*

    local image-content : sokol.sg_image_content
    (@ image-content.subimage 0 0) =
        local sokol.sg_subimage_content
            ptr = (bitcast (deref *screen-buffer*) voidstar)
            size = +screen-buffer-size+

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

    sokol.sg_update_image  *screen-texture*  &image-content
    sokol.sg_begin_default_pass  &pass-action  (sokol.sapp_width)  (sokol.sapp_height)
    sokol.sg_apply_pipeline  *gfx-pipeline* 
    sokol.sg_apply_bindings  &*gfx-bindings*
    sokol.sg_draw  0  6  1
    sokol.sg_end_pass;
    sokol.sg_commit;

fn cleanup ()
    free *screen-buffer*
    sokol.sg_destroy_image *screen-texture*
    ;

do
    let
        init
        draw-screen
        cleanup
    locals;
