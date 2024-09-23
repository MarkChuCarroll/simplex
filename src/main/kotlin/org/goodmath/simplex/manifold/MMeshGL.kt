package org.goodmath.simplex.manifold

import com.sun.jna.Memory
import com.sun.jna.Pointer

class MMeshGL(val mesh: ManifoldMeshGL): MType(mesh) {
    constructor(mem: Pointer, vertProps: Array<Float>,
        nVerts: Int, nProps: Int,
        triVerts: Array<Int>): this(
        ManifoldC.INSTANCE.manifold_meshgl(
            allocate(),
            vertProps, nVerts, nProps, triVerts, triVerts.size))

    constructor(mem: Pointer, vertProps: Array<Float>,
                nVerts: Int, nProps: Int,
                triVerts: Array<Int>, halfedgeTangent: Array<Float>):
    this(ManifoldC.INSTANCE.manifold_meshgl_w_tangents(
    allocate(),
    vertProps, nVerts, nProps, triVerts, triVerts.size, halfedgeTangent))

    fun copy(): MMeshGL {
        val mem = allocate()
        val c = ManifoldC.INSTANCE.manifold_meshgl_copy(mem, this.mesh)
        return MMeshGL(c)
    }

    /**
     * Updates the mergeFromVert and mergeToVert vectors in order to create a
     * manifold solid. If the MeshGL is already manifold, no change will occur and
     * the function will return false. Otherwise, this will merge verts along open
     * edges within precision (the maximum of the MeshGL precision and the baseline
     * bounding-box precision), keeping any from the existing merge vectors.
     *
     * There is no guarantee the result will be manifold - this is a best-effort
     * helper function designed primarily to aid in the case where a manifold
     * multi-material MeshGL was produced, but its merge vectors were lost due to a
     * round-trip through a file format. Constructing a Manifold from the result
     * will report a Status if it is not manifold.
     */
    fun merge(): MMeshGL {
        val merged = mc.manifold_meshgl_merge(allocate(), this.mesh)
        return MMeshGL(merged)
    }

    fun numProps(): Int {
        return mc.manifold_meshgl_num_prop(mesh)
    }

    fun numTri(): Int {
        return mc.manifold_meshgl_num_tri(mesh)
    }
    fun numVert(): Int {
        return mc.manifold_meshgl_num_vert(mesh)
    }

    fun propLength(): Int {
        return mc.manifold_meshgl_vert_properties_length(mesh)
    }

    fun triLength(): Int {
        return mc.manifold_meshgl_tri_length(mesh)
    }

    fun mergeLength(): Int {
        return mc.manifold_meshgl_merge_length(mesh)
    }

    fun indexLength(): Int {
        return mc.manifold_meshgl_run_index_length(mesh)
    }

    fun originalIdLength(): Int {
        return mc.manifold_meshgl_run_original_id_length(mesh)
    }

    fun transformLength(): Int {
        return mc.manifold_meshgl_run_transform_length(mesh)
    }

    fun faceIdLength(): Int {
        return mc.manifold_meshgl_face_id_length(mesh)
    }

    fun tangentLength(): Int {
        return mc.manifold_meshgl_tangent_length(mesh)
    }

    fun vertProperties(): Array<Float> {
        val mem = Memory((Float.SIZE_BYTES * mc.manifold_meshgl_vert_properties_length(mesh)).toLong())
        return mc.manifold_meshgl_vert_properties(mem, mesh)
    }

    fun export(file: String, options: MExportOptions) {
        mc.manifold_export_meshgl(file, mesh, options.options)
    }

    override fun close() {
        mc.manifold_destruct_meshgl(mesh)
        super.close()
    }

    companion object {
        fun allocate(): Pointer {
            return Memory(ManifoldC.INSTANCE.manifold_meshgl_size())
        }

        fun import(filename: String, forceCleanup: Boolean): MMeshGL {
            return MMeshGL(mc.manifold_import_meshgl(allocate(), filename, forceCleanup.toInt()))
        }

        fun levelSet(sdf: ManifoldC.SdfCallback,
                      bounds: MBox,
                      edgeLength: Float,
                      level: Float,
                      precision: Float,
                      ctx: Pointer): MMeshGL {
            return MMeshGL(ManifoldC.INSTANCE.manifold_level_set(allocate(),
                sdf, bounds.box, edgeLength, level, precision, ctx))
        }
    }

    fun level_set_seq(sdf: ManifoldC.SdfCallback,
                      bounds: MBox,
                      edgeLength: Float, level: Float, precision: Float,
                      ctx: Pointer): MMeshGL {
        return MMeshGL(ManifoldC.INSTANCE.manifold_level_set_seq(allocate(),
            sdf, bounds.box, edgeLength, level, precision, ctx))
    }



}
