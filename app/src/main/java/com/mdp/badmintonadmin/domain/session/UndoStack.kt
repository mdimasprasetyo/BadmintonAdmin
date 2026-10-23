package com.mdp.badmintonadmin.domain.session

/**
 * A small bounded LIFO stack: the newest entry is undone first and, once [capacity] is reached,
 * the oldest entry is dropped.
 */
class UndoStack<T>(private val capacity: Int = DEFAULT_CAPACITY) {

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    private val entries = ArrayDeque<T>()

    val size: Int get() = entries.size

    fun isEmpty(): Boolean = entries.isEmpty()

    fun push(entry: T) {
        entries.addLast(entry)
        while (entries.size > capacity) entries.removeFirst()
    }

    fun peek(): T? = entries.lastOrNull()

    fun pop(): T? = entries.removeLastOrNull()

    fun clear() = entries.clear()

    companion object {
        const val DEFAULT_CAPACITY = 10
    }
}
