package internals

import java.util.*

/**
 * A class defining the union of two maps
 */
internal data class Union<K, V>(private val m1: Map<K, V>, private val m2: Map<K, V>) : Map<K, V> {

    override val size: Int get() = m1.keys.asSequence().filter { it !in m2 }.count() + m2.keys.size

    override fun isEmpty(): Boolean = m1.isEmpty() && m2.isEmpty()

    override fun containsValue(value: V): Boolean = value in valueSequence

    override fun containsKey(key: K): Boolean = key in m1 || key in m2

    override fun get(key: K): V? = m2[key] ?: m1[key]

    override val keys: Set<K> = object : Set<K> {
        override val size: Int get() = this@Union.size

        override fun isEmpty(): Boolean = this@Union.isEmpty()

        override fun iterator(): Iterator<K> = iterator {
            for (elem in m1.keys) {
                if (elem !in m2) {
                    yield(elem)
                }
            }
            yieldAll(m2.keys)
        }

        override fun containsAll(elements: Collection<K>): Boolean = elements.asSequence()
            .filter { it !in m2 }
            .all { it in m1 }

        override fun contains(element: K): Boolean = element in this@Union
    }

    override val values: Collection<V> get() = valueSequence.toCollection(LinkedList())

    override val entries: Set<Map.Entry<K, V>> = object : Set<Map.Entry<K, V>> {

        override val size: Int get() = this@Union.size

        override fun isEmpty(): Boolean = this@Union.isEmpty()

        override fun iterator(): Iterator<Map.Entry<K, V>> = iterator {
            for (elem in m1) {
                if (elem.key !in m2) {
                    yield(elem)
                }
            }
            yieldAll(m2.entries)
        }

        override fun containsAll(elements: Collection<Map.Entry<K, V>>): Boolean = elements.asSequence()
            .filter { it.key !in m2 && it !in m2.entries }
            .all { it in m1.entries }

        override fun contains(element: Map.Entry<K, V>): Boolean =
            element in m2.entries || (element.key !in m2 && element in m1.entries)
    }

    private val valueSequence: Sequence<V> = sequence {
        for ((key, value) in m1) {
            if (key !in m2) {
                yield(value)
            }
        }
        yieldAll(m2.values)
    }.distinct()
}

internal operator fun <K, V> Map<K, V>.plus(other: Map<K, V>): Map<K, V> = Union(this, other)

/**
 * Other extensions
 */
internal operator fun <E> MutableList<E>.plus(elem: E?): MutableList<E> = apply { elem?.let { addLast(it) } }

internal val <T> List<T>.headTail: Pair<T, List<T>> get() = first() to subList(1, size)

internal inline fun <K, V, R> List<Pair<K, V>>.asMap(transform: (Pair<K, V>) -> R): Map<K, R> {
    return associateByTo(LinkedHashMap(size), { it.first }, { transform(it) })
}