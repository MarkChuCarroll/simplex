package org.goodmath.simplex.kcsg

import javafx.scene.paint.Color
import kotlin.random.Random


/**
 * A simple property storage.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class PropertyStorage {
    val map: HashMap<String, Any> = HashMap()


    init {
        randomColor(this)
    }

    /**
     * Sets a property. Existing properties are overwritten.
     *
     * @param key key
     * @param property property
     */
    fun set(key: String, property: Any) {
        map[key] = property
    }

    /**
     * Returns a property.
     *
     * @param <T> property type
     * @param key key
     * @return the property; an empty {@link java.util.Optional} will be
     * returned if the property does not exist or the type does not match
     */
    inline fun<reified T> getValue(key: String): T? {
        val value = map[key]
        return value as? T
    }

    /**
     * Deletes the requested property if present. Does nothing otherwise.
     *
     * @param key key
     */
    fun delete(key: String) {
        map.remove(key)
    }

    /**
     * Indicates whether this storage contains the requested property.
     *
     * @param key key
     * @return {@code true} if this storage contains the requested property;
     * {@code false}
     */
    fun contains(key: String): Boolean {
        return map.containsKey(key)
    }

    companion object {

        fun randomColor(storage: PropertyStorage) {
            val c = colors[Random.nextInt(colors.size)]


            storage.set("material:color", "${c.red} ${c.green} ${c.blue}")
        }

        val colors = listOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE,
            Color.MAGENTA, Color.WHITE, Color.BLACK, Color.GRAY, Color.ORANGE)

    }
}
