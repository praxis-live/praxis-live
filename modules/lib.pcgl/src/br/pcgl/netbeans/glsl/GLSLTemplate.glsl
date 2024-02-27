/*
 * Sample dummy shader to check the highlighter plugin.
 */

#version 430 core
#extension GL_ARB_shading_language_include : require

#include "my-shader.glsl"

uniform vec3 position;
uniform uint elementCount;
uniform usampler3D textureSampler;

layout(binding = 0, r32ui) readonly uniform uimage3D myImage3D;

layout (local_size_x = 256) in;
void main() {
    if (gl_GlobalInvocationID.x >= elementCount) { return; }

    int number = 5 + 3;
    float alpha = 0.5f;    // line comment

    ivec3 texCoord = ivec3(0,0,0);
    uint value = imageLoad(myImage3D, texCoord);

    @$@$ // Invalid characters! They're only allowed inside comments: @$@$
}
