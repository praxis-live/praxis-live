/*
 * Copyright (C) 2013 João Vicente Reis
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.pcgl.netbeans.glsl.lexer;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author João Vicente Reis
 */
public class GlslKeywords {
    private static final String keywordsString1 = "attribute const uniform varying buffer shared coherent volatile restrict readonly writeonly struct layout centroid flat smooth noperspective patch sample break continue do for while switch case default  if else subroutine in out inout true false invariant discard return lowp mediump highp precision";
    private static final String keywordsString2 = "sampler1D sampler2D sampler3D samplerCube sampler1DShadow sampler2DShadow samplerCubeShadow sampler1DArray sampler2DArray sampler1DArrayShadow sampler2DArrayShadow isampler1D isampler2D isampler3D isamplerCube isampler1DArray isampler2DArray usampler1D usampler2D usampler3D usamplerCube usampler1DArray usampler2DArray sampler2DRect sampler2DRectShadow isampler2DRect usampler2DRect samplerBuffer isamplerBuffer usamplerBuffer sampler2DMS isampler2DMS usampler2DMS sampler2DMSArray isampler2DMSArray usampler2DMSArray samplerCubeArray samplerCubeArrayShadow isamplerCubeArray usamplerCubeArray image1D iimage1D uimage1D image2D iimage2D uimage2D image3D iimage3D uimage3D image2DRect iimage2DRect uimage2DRect imageCube iimageCube uimageCube imageBuffer iimageBuffer uimageBuffer image1DArray iimage1DArray uimage1DArray image2DArray iimage2DArray uimage2DArray imageCubeArray iimageCubeArray uimageCubeArray image2DMS iimage2DMS uimage2DMS image2DMSArray iimage2DMSArray uimage2DMSArray atomic_uint mat2 mat3 mat4 dmat2 dmat3 dmat4 mat2x2 mat2x3 mat2x4 dmat2x2 dmat2x3 dmat2x4 mat3x2 mat3x3 mat3x4 dmat3x2 dmat3x3 dmat3x4 mat4x2 mat4x3 mat4x4 dmat4x2 dmat4x3 dmat4x4 vec2 vec3 vec4 ivec2 ivec3 ivec4 bvec2 bvec3 bvec4 dvec2 dvec3 dvec4 float double int void bool uint uvec2 uvec3 uvec4";
    private static final String keywordsString3 = "# #define #undef #if #ifdef #ifndef #else #elif #endif #error #pragma #extension #version #line #include";
    private static final String keywordsString4 = "gl_NumWorkGroups gl_WorkGroupSize gl_WorkGroupID gl_LocalInvocationID gl_GlobalInvocationID gl_LocalInvocationIndex gl_VertexID gl_InstanceID gl_PerVertex gl_Position gl_PointSize gl_ClipDistance gl_in gl_PrimitiveIDIn gl_InvocationID gl_Layer gl_ViewportIndex gl_PatchVerticesIn gl_InvocationID gl_out gl_TessLevelOuter gl_TessLevelInner gl_PatchVerticesIn gl_PrimitiveID gl_TessCoord gl_FragCoord gl_FrontFacing gl_ClipDistance gl_PointCoord gl_PrimitiveID gl_SampleID gl_SamplePosition gl_SampleMaskIn gl_Layer gl_ViewportIndex gl_FragDepth gl_SampleMask";
    private static final String keywordsString5 = "gl_MaxComputeWorkGroupCount gl_MaxComputeWorkGroupSize gl_MaxComputeUniformComponents gl_MaxComputeTextureImageUnits gl_MaxComputeImageUniforms gl_MaxComputeAtomicCounters gl_MaxComputeAtomicCounterBuffers gl_MaxVertexAttribs gl_MaxVertexUniformComponents gl_MaxVaryingComponents gl_MaxVertexOutputComponents gl_MaxGeometryInputComponents gl_MaxGeometryOutputComponents gl_MaxFragmentInputComponents gl_MaxVertexTextureImageUnits gl_MaxCombinedTextureImageUnits gl_MaxTextureImageUnits gl_MaxImageUnits gl_MaxCombinedImageUnitsAndFragmentOutputs gl_MaxImageSamples gl_MaxVertexImageUniforms gl_MaxTessControlImageUniforms gl_MaxTessEvaluationImageUniforms gl_MaxGeometryImageUniforms gl_MaxFragmentImageUniforms gl_MaxCombinedImageUniforms gl_MaxFragmentUniformComponents gl_MaxDrawBuffers gl_MaxClipDistances gl_MaxGeometryTextureImageUnits gl_MaxGeometryOutputVertices gl_MaxGeometryTotalOutputComponents gl_MaxGeometryUniformComponents gl_MaxGeometryVaryingComponents gl_MaxTessControlInputComponents gl_MaxTessControlOutputComponents gl_MaxTessControlTextureImageUnits gl_MaxTessControlUniformComponents gl_MaxTessControlTotalOutputComponents gl_MaxTessEvaluationInputComponents gl_MaxTessEvaluationOutputComponents gl_MaxTessEvaluationTextureImageUnits gl_MaxTessEvaluationUniformComponents gl_MaxTessPatchComponents gl_MaxPatchVertices gl_MaxTessGenLevel gl_MaxViewports gl_MaxVertexUniformVectors gl_MaxFragmentUniformVectors gl_MaxVaryingVectors gl_MaxVertexAtomicCounters gl_MaxTessControlAtomicCounters gl_MaxTessEvaluationAtomicCounters gl_MaxGeometryAtomicCounters gl_MaxFragmentAtomicCounters gl_MaxCombinedAtomicCounters gl_MaxAtomicCounterBindings gl_MaxVertexAtomicCounterBuffers gl_MaxTessControlAtomicCounterBuffers gl_MaxTessEvaluationAtomicCounterBuffers gl_MaxGeometryAtomicCounterBuffers gl_MaxFragmentAtomicCounterBuffers gl_MaxCombinedAtomicCounterBuffers gl_MaxAtomicCounterBufferSize gl_MinProgramTexelOffset gl_MaxProgramTexelOffset";
    private static final String keywordsString6 = "radians degrees sin cos tan asin acos atan sinh cosh tanh asinh acosh atanh pow exp log exp2 log2 sqrt inversqrt abs sign floor trunc round roundEven ceil fract mod modf min max clamp mix step smoothstep isnan isinf floatBitsToInt floatBitsToUInt intBitsToFloat uintBitsToFloat fma frexp ldexp packUnorm2x16 packSnorm2x16 packUnorm4x8 packSnorm4x8 unpackUnorm2x16 unpackSnorm2x16 unpackUnorm4x8 unpackSnorm4x8  packDouble2x32 unpackDouble2x32 packHalf2x16 unpackHalf2x16 length distance dot cross normalize faceforward reflect refract matrixCompMult outerProduct transpose determinant inverse lessThan lessThanEqual greaterThan greaterThanEqual equal notEqual any all not uaddCarry usubBorrow umulExtended imulExtended bitfieldExtract bitfieldInsert bitfieldReverse findLSB bitCount findMSB textureSize textureQueryLod textureQueryLevels texture textureProj textureLod textureOffset texelFetch texelFetchOffset textureProjOffset textureLodOffset textureProjLod textureProjLodOffset textureGrad textureGradOffset textureProjGrad textureProjGradOffset textureGather textureGatherOffset textureGatherOffsets texture1D texture1DProj texture1DLod texture1DProjLod texture2D texture2DProj texture2DLod texture2DProjLod texture3D texture3DProj texture3DLod texture3DProjLod textureCube textureCubeLod shadow1D shadow2D shadow1DProj shadow2DProj shadow1DLod shadow2DLod shadow1DProjLod shadow2DProjLod atomicCounterIncrement atomicCounterDecrement atomicCounter atomicAdd atomicMin atomicMax atomicAnd atomicOr atomicXor atomicExchange atomicCompSwap imageSize imageLoad imageStore imageAtomicAdd imageAtomicMin imageAtomicMax imageAtomicAnd imageAtomicOr imageAtomicXor imageAtomicExchange imageAtomicCompSwap dFdx dFdy fwidth interpolateAtCentroid interpolateAtSample interpolateAtOffset noise1 noise2 noise3 noise4 EmitStreamVertex EndStreamPrimitive EmitVertex EndPrimitive barrier memoryBarriermemoryBarrierAtomicCounter memoryBarrierBuffer memoryBarrierShared memoryBarrierImage groupMemoryBarrier";

    static final Map<String,GlslTokenId> keywords1 = new HashMap<String,GlslTokenId>();
    static final Map<String,GlslTokenId> keywords2 = new HashMap<String,GlslTokenId>();
    static final Map<String,GlslTokenId> keywords3 = new HashMap<String,GlslTokenId>();
    static final Map<String,GlslTokenId> keywords4 = new HashMap<String,GlslTokenId>();
    static final Map<String,GlslTokenId> keywords5 = new HashMap<String,GlslTokenId>();
    static final Map<String,GlslTokenId> keywords6 = new HashMap<String,GlslTokenId>();

    static {
        Scanner scanner;

        scanner = new Scanner(keywordsString1); while(scanner.hasNext()) { keywords1.put(scanner.next(), GlslTokenId.KEYWORD); }
        scanner = new Scanner(keywordsString2); while(scanner.hasNext()) { keywords2.put(scanner.next(), GlslTokenId.BASIC_TYPE); }
        scanner = new Scanner(keywordsString3); while(scanner.hasNext()) { keywords3.put(scanner.next(), GlslTokenId.PREPROCESSOR); }
        scanner = new Scanner(keywordsString4); while(scanner.hasNext()) { keywords4.put(scanner.next(), GlslTokenId.BUILTIN_VAR); }
        scanner = new Scanner(keywordsString5); while(scanner.hasNext()) { keywords5.put(scanner.next(), GlslTokenId.BUILTIN_VAR); }
        scanner = new Scanner(keywordsString6); while(scanner.hasNext()) { keywords6.put(scanner.next(), GlslTokenId.BUILTIN_FUNC); }
    }
}
