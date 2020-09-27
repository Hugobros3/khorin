/** Mutates an immutable map */
fun <K, V> Map<K, V>.addOrReplaceKey(key: K, value: V): Map<K, V> {
    val newMap = this.toMutableMap()
    newMap[key] = value
    return newMap
}