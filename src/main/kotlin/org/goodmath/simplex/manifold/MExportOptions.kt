package org.goodmath.simplex.manifold

import com.sun.jna.Memory
import com.sun.jna.Pointer

class MExportOptions(val options: ManifoldExportOptions): MType(options) {
    constructor(): this(mc.manifold_export_options(allocate()))

    fun setFaceted(faceted: Int) {
        mc.manifold_export_options_set_faceted(options, faceted)
    }

    fun setMaterial(material: MMaterial) {
        mc.manifold_export_options_set_material(options, material.material)
    }

    override fun close() {
        mc.manifold_destruct_export_options(options)
        super.close()
    }

    companion object {
        fun allocate(): Pointer {
            return Memory(mc.manifold_export_options_size())
        }
    }
}
