/*
 * Copyright 2024 Mark C. Chu-Carroll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("FunctionName", "LocalVariableName")

package org.goodmath.simplex.manifold

// JNA interface to allow us to use Manifold from Kotlin. This is basically
// a Kotlin wrapper around the ManifoldC C bindings, since JNA doesn't
// support C++ bindings.

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure


// Set up a bunch of type aliases for the types set up in manifoldc.h as
// abstract pointer types.
typealias ManifoldManifold = Pointer
typealias ManifoldManifoldVec = Pointer
typealias ManifoldCrossSection = Pointer
typealias ManifoldCrossSectionVec = Pointer
typealias ManifoldSimplePolygon = Pointer
typealias ManifoldPolygons = Pointer
typealias ManifoldMeshGL = Pointer
typealias ManifoldBox = Pointer
typealias ManifoldRect = Pointer
typealias ManifoldExportOptions = Pointer
typealias ManifoldMaterial = Pointer

// Now set up wrappers for the enumeration types.

typealias ManifoldOpType = Int
const val MANIFOLD_ADD = 0
const val MANIFOLD_SUBTRACT = 1
const val MANIFOLD_INTERSECT = 2

typealias ManifoldError = Int
const val MANIFOLD_NO_ERROR = 0
const val MANIFOLD_NON_FINITE_VERTEX = 1
const val MANIFOLD_NOT_MANIFOLD = 2
const val MANIFOLD_VERTEX_INDEX_OUT_OF_BOUNDS = 3
const val MANIFOLD_PROPERTIES_WRONG_LENGTH = 4
const val MANIFOLD_MISSING_POSITION_PROPERTIES = 5
const val MANIFOLD_MERGE_VECTORS_DIFFERENT_LENGTHS = 6
const val MANIFOLD_MERGE_INDEX_OUT_OF_BOUNDS = 7
const val MANIFOLD_TRANSFORM_WRONG_LENGTH = 8
const val MANIFOLD_RUN_INDEX_WRONG_LENGTH = 9
const val MANIFOLD_FACE_ID_WRONG_LENGTH = 10
const val MANIFOLD_INVALID_CONSTRUCTION = 11

typealias ManifoldFillRule = Int
const val MANIFOLD_FILL_RULE_EVEN_ODD = 0
const val MANIFOLD_FILL_RULE_NON_ZERO = 1
const val MANIFOLD_FILL_RULE_POSITIVE = 2
const val MANIFOLD_FILL_RULE_NEGATIVE = 3

typealias ManifoldJoinType = Int
const val MANIFOLD_JOIN_TYPE_SQUARE = 0
const val MANIFOLD_JOIN_TYPE_ROUND = 1
const val MANIFOLD_JOIN_TYPE_MITER = 2


/**
 * The ManifoldC bindings library as a Kotlin JNA interface.
 */
interface ManifoldC: Library {

    class ManifoldManifoldPair(
        val first: ManifoldManifold,
        val second: ManifoldManifold
    ) : Structure()


    // Nested structure types from manifoldc.h
    class ManifoldVec2(val x: Float, val y: Float) : Structure()

    class ManifoldVec3(val x: Float, val y: Float, val z: Float) : Structure()

    class ManifoldVec4(val x: Float, val y: Float, val z: Float, val w: Float) : Structure()

    class ManifoldProperties(surface_area: Float, volume: Float) : Structure()

    // Polygons
    fun manifold_simple_polygon(mem: Pointer, ps: Array<ManifoldVec2>, length: Int): ManifoldSimplePolygon //done

    fun manifold_polygons(mem: Pointer, ps: Array<ManifoldSimplePolygon>, length: Int): ManifoldPolygons //done

    fun manifold_simple_polygon_length(p: ManifoldSimplePolygon): Int  //done

    fun manifold_polygons_length(ps: ManifoldPolygons): Int //done

    fun manifold_polygons_simple_length(ps: ManifoldPolygons, idx: Int): Int  //done

    fun manifold_simple_polygon_get_point(
        p: ManifoldSimplePolygon,
        idx: Int
    ): ManifoldVec2 //

    fun manifold_polygons_get_simple(
        mem: Pointer,
        ps: ManifoldPolygons,
        idx: Int
    ): ManifoldSimplePolygon //done

    fun manifold_polygons_get_point(
        ps: ManifoldPolygons, simple_idx: Int,
        pt_idx: Int
    ): ManifoldVec2 // done

    // Mesh Construction
    fun manifold_meshgl(
        mem: Pointer, vert_props: Array<Float>, n_verts: Int,
        n_props: Int, tri_verts: Array<Int>,
        n_tris: Int
    ): ManifoldMeshGL //done

    fun manifold_meshgl_w_tangents(
        mem: Pointer, vert_props: Array<Float>,
        n_verts: Int, n_props: Int,
        tri_verts: Array<Int>, n_tris: Int,
        halfedge_tangent: Array<Float>
    ): ManifoldMeshGL // done

    fun manifold_get_meshgl(mem: Pointer, m: ManifoldManifold): ManifoldMeshGL // MManifold

    fun manifold_meshgl_copy(mem: Pointer, m: ManifoldMeshGL): ManifoldMeshGL // done

    fun manifold_meshgl_merge(mem: Pointer, m: ManifoldMeshGL): ManifoldMeshGL // done


    // SDF
    // By default, the execution policy (sequential or parallel) of
    // manifold_level_set will be chosen automatically depending on the size of the
    // job and whether Manifold has been compiled with a PAR backend. If you are
    // using these bindings from a language that has a runtime lock preventing the
    // parallel execution of closures, then you should use manifold_level_set_seq to
    // force sequential execution.
    interface SdfCallback: Callback {
        fun invoke(a: Float, b: Float, c: Float, mem: Pointer): Float
    }

    fun manifold_level_set(
        mem: Pointer,
        sdf: SdfCallback,
        bounds: ManifoldBox,
        edge_length: Float,
        level: Float,
        precision: Float,
        ctx: Pointer
    ): ManifoldMeshGL // MMeshGL

    fun manifold_level_set_seq(
        mem: Pointer, sdf: SdfCallback,
        bounds: ManifoldBox,
        edge_length: Float, level: Float, precision: Float, ctx: Pointer
    ): ManifoldMeshGL // MMeshGL


    // Manifold Vectors
    // In the high level wrapper, these are not exposed; instead, they're transparent converted to/from
    // List<MManifold>.
    fun manifold_manifold_empty_vec(mem: Pointer): ManifoldManifoldVec

    fun manifold_manifold_vec(mem: Pointer, sz: Int): ManifoldManifoldVec

    fun manifold_manifold_vec_reserve(ms: ManifoldManifoldVec, sz: Int)

    fun manifold_manifold_vec_length(ms: ManifoldManifoldVec): Int

    fun manifold_manifold_vec_get(
        mem: Pointer, ms: ManifoldManifoldVec,
        idx: Int
    ): ManifoldManifold

    fun manifold_manifold_vec_set(
        ms: ManifoldManifoldVec,
        idx: Int,
        m: ManifoldManifold
    );

    fun manifold_manifold_vec_push_back(
        ms: ManifoldManifoldVec,
        m: ManifoldManifold
    )

    // Manifold Booleans
    fun manifold_boolean(
        mem: Pointer, a: ManifoldManifold,
        b: ManifoldManifold, op: ManifoldOpType
    ): ManifoldManifold // MManifold

    fun manifold_batch_boolean(
        mem: Pointer, ms: ManifoldManifoldVec,
        op: ManifoldOpType
    ): ManifoldManifold // MManifold

    fun manifold_union(
        mem: Pointer, a: ManifoldManifold,
        b: ManifoldManifold
    ): ManifoldManifold // MManifold

    fun manifold_difference(
        mem: Pointer, a: ManifoldManifold,
        b: ManifoldManifold
    ): ManifoldManifold // MManifold

    fun manifold_intersection(
        mem: Pointer, a: ManifoldManifold,
        b: ManifoldManifold
    ): ManifoldManifold // MManifold

    fun manifold_split(
        mem_first: Pointer, mem_second: Pointer,
        a: ManifoldManifold, b: ManifoldManifold
    ): ManifoldManifoldPair  // MManifold

    fun manifold_split_by_plane(
        mem_first: Pointer, mem_second: Pointer,
        m: ManifoldManifold,
        normal_x: Float, normal_y: Float,
        normal_z: Float, offset: Float
    ): ManifoldManifoldPair // MManifold

    fun manifold_trim_by_plane(
        mem: Pointer, m: ManifoldManifold,
        normal_x: Float, normal_y: Float,
        normal_z: Float, offset: Float
    ): ManifoldManifold // MManifold


    // 3D to 2D
    fun manifold_slice(
        mem: Pointer, m: ManifoldManifold,
        height: Float
    ): ManifoldPolygons // MManifold

    fun manifold_project(mem: Pointer, m: ManifoldManifold): ManifoldPolygons // MManifold

    // Convex Hulls
    fun manifold_hull(mem: Pointer, m: ManifoldManifold): ManifoldManifold // MManifold

    fun manifold_batch_hull(mem: Pointer, ms: ManifoldManifoldVec): ManifoldManifold // MManifold

    fun manifold_hull_pts(mem: Pointer, ps: Array<ManifoldVec3>, length: Int): ManifoldManifold // MManifold

    // Manifold Transformations
    fun manifold_translate(
        mem: Pointer, m: ManifoldManifold, x: Float,
        y: Float, z: Float
    ): ManifoldManifold// MManifold

    fun manifold_rotate(
        mem: Pointer, m: ManifoldManifold, x: Float,
        y: Float, z: Float
    ): ManifoldManifold  // MManifold

    fun manifold_scale(
        mem: Pointer, m: ManifoldManifold, x: Float,
        y: Float, z: Float
    ): ManifoldManifold // MManifold

    fun manifold_transform(
        mem: Pointer, m: ManifoldManifold, x1: Float,
        y1: Float, z1: Float, x2: Float, y2: Float,
        z2: Float, x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float
    ): ManifoldManifold // MManifold

    fun manifold_mirror(
        mem: Pointer, m: ManifoldManifold, nx: Float,
        ny: Float, nz: Float
    ): ManifoldManifold // MManifold

    interface WarpCallback: Callback {
        fun invoke(x: Float, y: Float, z: Float, mem: Pointer): ManifoldVec3
    }

    fun manifold_warp(
        mem: Pointer, m: ManifoldManifold,
        `fun`: WarpCallback,
        ctx: Pointer
    ): ManifoldManifold // MManifold

    fun manifold_smooth_by_normals(
        mem: Pointer, m: ManifoldManifold,
        normalIdx: Int
    ): ManifoldManifold // MManifold

    fun manifold_smooth_out(
        mem: Pointer, m: ManifoldManifold,
        minSharpAngle: Float, minSmoothness: Float
    ): ManifoldManifold // MManifold

    fun manifold_refine(mem: Pointer, m: ManifoldManifold, refine: Int): ManifoldManifold // MManifold

    fun manifold_refine_to_length(
        mem: Pointer, m: ManifoldManifold,
        length: Float
    ): ManifoldManifold // MManifold

    // Manifold Shapes / Constructors
    fun manifold_empty(mem: Pointer): ManifoldManifold // MManifold

    fun manifold_copy(mem: Pointer, m: ManifoldManifold): ManifoldManifold // MManifold

    fun manifold_tetrahedron(mem: Pointer): ManifoldManifold // MManifold

    fun manifold_cube(
        mem: Pointer, x: Float, y: Float, z: Float,
        center: Int
    ): ManifoldManifold // MManifold

    fun manifold_cylinder(
        mem: Pointer, height: Float, radius_low: Float,
        radius_high: Float, circular_segments: Int,
        center: Int
    ): ManifoldManifold //MManifold

    fun manifold_sphere(
        mem: Pointer, radius: Float,
        circular_segments: Int
    ): ManifoldManifold  // MManifold

    fun manifold_of_meshgl(mem: Pointer, mesh: ManifoldMeshGL): ManifoldManifold

    fun manifold_smooth(
        mem: Pointer, mesh: ManifoldMeshGL,
        half_edges: Array<Int>, smoothness: Array<Float>,
        n_idxs: Int
    ): ManifoldManifold //MManifold

    fun manifold_extrude(
        mem: Pointer, cs: ManifoldPolygons,
        height: Float, slices: Int,
        twist_degrees: Float, scale_x: Float,
        scale_y: Float
    ): ManifoldManifold  // MManifold

    fun manifold_revolve(
        mem: Pointer, cs: ManifoldPolygons,
        circular_segments: Int
    ): ManifoldManifold // MManifold

    fun manifold_compose(mem: Pointer, ms: ManifoldManifoldVec): ManifoldManifold // MManifold

    fun manifold_decompose(mem: Pointer, m: ManifoldManifold): ManifoldManifoldVec // MManifold

    fun manifold_as_original(mem: Pointer, m: ManifoldManifold): ManifoldManifold // MManifold

    // Manifold Info
    fun manifold_is_empty(m: ManifoldManifold): Int // MManifold

    fun manifold_status(m: ManifoldManifold): ManifoldError // MManifold

    fun manifold_num_vert(m: ManifoldManifold): Int// MManifold

    fun manifold_num_edge(m: ManifoldManifold): Int // MManifold

    fun manifold_num_tri(m: ManifoldManifold): Int // MManifold

    fun manifold_bounding_box(mem: Pointer, m: ManifoldManifold): ManifoldBox // MManifold

    fun manifold_precision(m: ManifoldManifold): Float // MManifold

    fun manifold_genus(m: ManifoldManifold): Int // MManifold

    fun manifold_get_properties(m: ManifoldManifold): ManifoldProperties // MManifold

    fun manifold_get_circular_segments(radius: Float): Int // MManifold

    fun manifold_original_id(m: ManifoldManifold): Int // MManifold

    fun manifold_reserve_ids(n: Int): Int // MManifold

    interface PropertySetCallback: Callback {
        fun invoke(new_prop: Array<Float>, position: ManifoldVec3,
            old_prop: Array<Float>, ctx: Pointer)// MManifold
    }

    fun manifold_set_properties(
        mem: Pointer, m: ManifoldManifold, num_prop: Int,
        `fun`: PropertySetCallback,
        ctx: Pointer
    ): ManifoldManifold// MManifold

    fun manifold_calculate_curvature(
        mem: Pointer, m: ManifoldManifold,
        gaussian_idx: Int, mean_idx: Int
    ): ManifoldManifold // MManifold

    fun manifold_min_gap(
        m: ManifoldManifold, other: ManifoldManifold,
        searchLength: Float
    ): Float // MManifold

    fun manifold_calculate_normals(
        mem: Pointer, m: ManifoldManifold,
        normal_idx: Int,
        min_sharp_angle: Int
    ): ManifoldManifold // MManifold

    // CrossSection Shapes/Constructors

    fun manifold_cross_section_empty(mem: Pointer): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_copy(
        mem: Pointer,
        cs: ManifoldCrossSection
    ): ManifoldCrossSection // MCRossSection

    fun manifold_cross_section_of_simple_polygon(
        mem: Pointer, p: ManifoldSimplePolygon, fr: ManifoldFillRule
    ): ManifoldCrossSection // MPolygon

    fun manifold_cross_section_of_polygons(
        mem: Pointer,
        p: ManifoldPolygons,
        fr: ManifoldFillRule
    ): ManifoldCrossSection // MPolygon

    fun manifold_cross_section_square(
        mem: Pointer, x: Float, y: Float,
        center: Int
    ): ManifoldCrossSection // MCRossSection


    fun manifold_cross_section_circle(
        mem: Pointer, radius: Float,
        circular_segments: Int
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_compose(
        mem: Pointer, csv: ManifoldCrossSectionVec
    ): ManifoldCrossSection // McrossSection

    fun manifold_cross_section_decompose(mem: Pointer, cs: ManifoldCrossSection): ManifoldCrossSectionVec // MCrossSection

    // CrossSection Vectors.
    // Again, not exposed by the high level wrapper.
    fun manifold_cross_section_empty_vec(mem: Pointer): ManifoldCrossSectionVec

    fun manifold_cross_section_vec(mem: Pointer, sz: Int): ManifoldCrossSectionVec

    fun manifold_cross_section_vec_reserve(
        csv: ManifoldCrossSectionVec,
        sz: Int
    )

    fun manifold_cross_section_vec_length(csv: ManifoldCrossSectionVec): Int

    fun manifold_cross_section_vec_get(
        mem: Pointer, csv: ManifoldCrossSectionVec, idx: Int
    ): ManifoldCrossSection

    fun manifold_cross_section_vec_set(
        csv: ManifoldCrossSectionVec, idx: Int,
        cs: ManifoldCrossSection
    )

    fun manifold_cross_section_vec_push_back(
        csv: ManifoldCrossSectionVec,
        cs: ManifoldCrossSection
    )

    // CrossSection Booleans
    fun manifold_cross_section_boolean(
        mem: Pointer,
        a: ManifoldCrossSection,
        b: ManifoldCrossSection,
        op: ManifoldOpType
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_batch_boolean(
        mem: Pointer,
        csv: ManifoldCrossSectionVec, op: ManifoldOpType
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_union(
        mem: Pointer,
        a: ManifoldCrossSection,
        b: ManifoldCrossSection
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_difference(
        mem: Pointer, a: ManifoldCrossSection, b: ManifoldCrossSection
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_intersection(
        mem: Pointer, a: ManifoldCrossSection, b: ManifoldCrossSection
    ): ManifoldCrossSection // MCrossSection


    // CrossSection Convex Hulls
    fun manifold_cross_section_hull(
        mem: Pointer,
        cs: ManifoldCrossSection
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_batch_hull(
        mem: Pointer, css: ManifoldCrossSectionVec
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_hull_simple_polygon(
        mem: Pointer, ps: ManifoldSimplePolygon
    ): ManifoldCrossSection // MPolygon

    fun manifold_cross_section_hull_polygons(
        mem: Pointer, ps: ManifoldPolygons
    ): ManifoldCrossSection // MPolygon

    // CrossSection Transformation
    fun manifold_cross_section_translate(
        mem: Pointer,
        cs: ManifoldCrossSection,
        x: Float, y: Float
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_rotate(
        mem: Pointer,
        cs: ManifoldCrossSection,
        deg: Float
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_scale(
        mem: Pointer,
        cs: ManifoldCrossSection,
        x: Float, y: Float
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_mirror(
        mem: Pointer,
        cs: ManifoldCrossSection,
        ax_x: Float, ax_y: Float
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_transform(
        mem: Pointer,
        cs: ManifoldCrossSection,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float
    ): ManifoldCrossSection // MCrossSection

    interface CrossSectionWarpCallback: Callback {
        fun invoke(x: Float, y: Float): ManifoldVec2
    }

    interface CrossSectionWarpContextCallback: Callback {
        fun invoke(x: Float, y: Float, ctx: Pointer): ManifoldVec2
    }

    fun manifold_cross_section_warp(
        mem: Pointer, cs: ManifoldCrossSection,
        `fun`: CrossSectionWarpCallback
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_warp_context(
        mem: Pointer,
        cs: ManifoldCrossSection,
        `fun`: CrossSectionWarpContextCallback,
        ctx: Pointer
    ): ManifoldCrossSection // MCrossSection

    fun manifold_cross_section_simplify(
        mem: Pointer,
        cs: ManifoldCrossSection,
        epsilon: Float
    ): ManifoldCrossSection// MCrossSection

    fun manifold_cross_section_offset(
        mem: Pointer, cs: ManifoldCrossSection,
        delta: Double, jt: ManifoldJoinType,
        miter_limit: Double, circular_segments: Int
    ): ManifoldCrossSection// MCrossSection

    // CrossSection Info
    fun manifold_cross_section_area(cs: ManifoldCrossSection): Double // MCrossSection

    fun manifold_cross_section_num_vert(cs: ManifoldCrossSection): Int // MCrossSection

    fun manifold_cross_section_num_contour(cs: ManifoldCrossSection): Int // MCrossSection

    fun manifold_cross_section_is_empty(cs: ManifoldCrossSection): Int // MCrossSection

    fun manifold_cross_section_bounds(
        mem: Pointer,
        cs: ManifoldCrossSection
    ): ManifoldRect  // MCrossSection

    fun manifold_cross_section_to_polygons(
        mem: Pointer,
        cs: ManifoldCrossSection
    ): ManifoldPolygons // MCrossSection

    // Rectangle
    fun manifold_rect(mem: Pointer, x1: Float, y1: Float, x2: Float, y2: Float): ManifoldRect // MRect

    fun manifold_rect_min(r: ManifoldRect): ManifoldVec2 // MRect

    fun manifold_rect_max(r: ManifoldRect): ManifoldVec2 // MRect

    fun manifold_rect_dimensions(r: ManifoldRect): ManifoldVec2 // MRect

    fun manifold_rect_center(r: ManifoldRect): ManifoldVec2 // MRect

    fun manifold_rect_scale(r: ManifoldRect): Float // MRect

    fun manifold_rect_contains_pt(r: ManifoldRect, x: Float, y: Float): Int  // MRect

    fun manifold_rect_contains_rect(a: ManifoldRect, b: ManifoldRect): Int // MRect

    fun manifold_rect_include_pt(r: ManifoldRect, x: Float, y: Float)  //  MRect

    fun manifold_rect_union(mem: Pointer, a: ManifoldRect, b: ManifoldRect): ManifoldRect  //  MRect

    fun manifold_rect_transform(
        mem: Pointer, r: ManifoldRect, x1: Float,
        y1: Float, x2: Float, y2: Float, x3: Float,
        y3: Float
    ): ManifoldRect  //  MRect

    fun manifold_rect_translate(
        mem: Pointer, r: ManifoldRect, x: Float,
        y: Float
    ): ManifoldRect  //  MRect

    fun manifold_rect_mul(mem: Pointer, r: ManifoldRect, x: Float, y: Float): ManifoldRect   //  MRect

    fun manifold_rect_does_overlap_rect(a: ManifoldRect, r: ManifoldRect): Int   //  MRect

    fun manifold_rect_is_empty(r: ManifoldRect): Int   //  MRect

    fun manifold_rect_is_finite(r: ManifoldRect): Int   //  MRect

    // Bounding Box
    fun manifold_box(
        mem: Pointer, x1: Float, y1: Float, z1: Float, x2: Float,
        y2: Float, z2: Float
    ): ManifoldBox // MBox

    fun manifold_box_min(b: ManifoldBox): ManifoldVec3 // MBOx

    fun manifold_box_max(b: ManifoldBox): ManifoldVec3 // MBox

    fun manifold_box_dimensions(b: ManifoldBox): ManifoldVec3 // MBox

    fun manifold_box_center(b: ManifoldBox): ManifoldVec3// MBox

    fun manifold_box_scale(b: ManifoldBox): Float // MBox

    fun manifold_box_contains_pt(b: ManifoldBox, x: Float, y: Float, z: Float): Int // MBox

    fun manifold_box_contains_box(a: ManifoldBox, b: ManifoldBox): Int // MBox

    fun manifold_box_include_pt(b: ManifoldBox, x: Float, y: Float, z: Float) // MBox

    fun manifold_box_union(mem: Pointer, a: ManifoldBox, b: ManifoldBox): ManifoldBox // mbox

    fun manifold_box_transform(
        mem: Pointer, b: ManifoldBox, x1: Float,
        y1: Float, z1: Float, x2: Float, y2: Float,
        z2: Float, x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float
    ): ManifoldBox //mbox

    fun manifold_box_translate(
        mem: Pointer, b: ManifoldBox, x: Float, y: Float,
        z: Float
    ): ManifoldBox //mbox

    fun manifold_box_mul(
        mem: Pointer, b: ManifoldBox, x: Float, y: Float,
        z: Float
    ): ManifoldBox //mbox

    fun manifold_box_does_overlap_pt(b: ManifoldBox, x: Float, y: Float, z: Float): Int // mbox

    fun manifold_box_does_overlap_box(a: ManifoldBox, b: ManifoldBox): Int //mbox

    fun manifold_box_is_finite(b: ManifoldBox): Int //mbox

   // Static Quality Globals
    fun manifold_set_min_circular_angle(degrees: Float) // ManifoldLibrary

    fun manifold_set_min_circular_edge_length(length: Float) // ManifoldLibrary

    fun manifold_set_circular_segments(number: Int)  // ManifoldLibary

    // Manifold Mesh Extraction
    fun manifold_meshgl_num_prop(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_num_vert(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_num_tri(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_vert_properties_length(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_tri_length(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_merge_length(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_run_index_length(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_run_original_id_length(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_run_transform_length(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_face_id_length(m: ManifoldMeshGL): Int // MMeshGL
    fun manifold_meshgl_tangent_length(m: ManifoldMeshGL) :Int // MMeshGL
    fun manifold_meshgl_vert_properties(mem: Pointer, m: ManifoldMeshGL): Array<Float>

    fun manifold_meshgl_tri_verts(mem: Pointer, m: ManifoldMeshGL): Array<Int>

    fun manifold_meshgl_merge_from_vert(mem: Pointer, m: ManifoldMeshGL): Array<Int>

    fun manifold_meshgl_merge_to_vert(mem: Pointer, m: ManifoldMeshGL): Array<Int>
    fun manifold_meshgl_run_index(mem: Pointer, m: ManifoldMeshGL): Array<Int>
    fun manifold_meshgl_run_original_id(mem: Pointer, m: ManifoldMeshGL): Array<Int>
    fun manifold_meshgl_run_transform(mem: Pointer, m: ManifoldMeshGL): Array<Float>
    fun manifold_meshgl_face_id(mem: Pointer, m: ManifoldMeshGL): Array<Int>
    fun manifold_meshgl_halfedge_tangent(mem: Pointer, m: ManifoldMeshGL): Array<Float>

    // memory size
    fun manifold_manifold_size(): Long
    fun manifold_manifold_vec_size(): Long
    fun manifold_cross_section_size(): Long
    fun manifold_cross_section_vec_size(): Long
    fun manifold_simple_polygon_size(): Long
    fun manifold_polygons_size(): Long
    fun manifold_manifold_pair_size(): Long
    fun manifold_meshgl_size(): Long
    fun manifold_box_size(): Long
    fun manifold_rect_size(): Long
    fun manifold_curvature_size(): Long

    // destruction
    fun manifold_destruct_manifold(m: ManifoldManifold)
    fun manifold_destruct_manifold_vec(ms: ManifoldManifoldVec)
    fun manifold_destruct_cross_section(m: ManifoldCrossSection)
    fun manifold_destruct_cross_section_vec(csv: ManifoldCrossSectionVec)
    fun manifold_destruct_simple_polygon(p: ManifoldSimplePolygon)
    fun manifold_destruct_polygons(p: ManifoldPolygons)
    fun manifold_destruct_meshgl(m: ManifoldMeshGL)
    fun manifold_destruct_box(b: ManifoldBox)
    fun manifold_destruct_rect(b: ManifoldRect)

    // pointer free + destruction
    fun manifold_delete_manifold(m: ManifoldManifold)
    fun manifold_delete_manifold_vec(ms: ManifoldManifoldVec)
    fun manifold_delete_cross_section(cs: ManifoldCrossSection)
    fun manifold_delete_cross_section_vec(csv: ManifoldCrossSectionVec)
    fun manifold_delete_simple_polygon(p: ManifoldSimplePolygon)
    fun manifold_delete_polygons(p: ManifoldPolygons)
    fun manifold_delete_meshgl(m: ManifoldMeshGL)
    fun manifold_delete_box(b: ManifoldBox)
    fun manifold_delete_rect(b: ManifoldRect)

    // MeshIO / Export
    fun manifold_material(mem: Pointer): ManifoldMaterial

    fun manifold_material_set_roughness(mat: ManifoldMaterial, roughness: Float)

    fun manifold_material_set_metalness(mat: ManifoldMaterial, metalness: Float)

    fun manifold_material_set_color(mat: ManifoldMaterial, color: ManifoldVec4)

    fun manifold_material_set_vert_color(mat: ManifoldMaterial,
                                         vert_color: ManifoldVec4, n_vert: Int)

    fun manifold_export_options(mem: Pointer): ManifoldExportOptions

    fun manifold_export_options_set_faceted(options: ManifoldExportOptions,
                                            faceted: Int)

    fun manifold_export_options_set_material(options: ManifoldExportOptions,
                                             mat: ManifoldMaterial)

    fun manifold_export_meshgl(filename: String, mesh: ManifoldMeshGL,
                               options: ManifoldExportOptions)

    fun manifold_import_meshgl(mem: Pointer, filename: String,
                               force_cleanup: Int): ManifoldMeshGL

    fun manifold_material_size(): Long

    fun manifold_export_options_size(): Long

    fun manifold_destruct_material(m: ManifoldMaterial)
    fun manifold_destruct_export_options(options: ManifoldExportOptions)

    fun manifold_delete_material(m : ManifoldMaterial)

    fun manifold_delete_export_options(options: ManifoldExportOptions)

    companion object {
        val INSTANCE: ManifoldC by lazy {
            Native.load(
                "manifoldc",
                ManifoldC::class.java
            ) as ManifoldC
        }
    }
}


