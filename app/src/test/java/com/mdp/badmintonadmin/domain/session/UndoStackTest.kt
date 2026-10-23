package com.mdp.badmintonadmin.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoStackTest {

    @Test
    fun `pops newest first`() {
        val stack = UndoStack<String>()
        stack.push("a"); stack.push("b"); stack.push("c")
        assertEquals("c", stack.peek())
        assertEquals("c", stack.pop())
        assertEquals("b", stack.pop())
        assertEquals("a", stack.pop())
        assertNull(stack.pop())
        assertTrue(stack.isEmpty())
    }

    @Test
    fun `drops the oldest entry beyond capacity`() {
        val stack = UndoStack<Int>(capacity = 3)
        (1..5).forEach { stack.push(it) }
        assertEquals(3, stack.size)
        assertEquals(listOf(5, 4, 3), listOf(stack.pop(), stack.pop(), stack.pop()))
        assertNull(stack.pop())
    }

    @Test
    fun `clear empties the stack`() {
        val stack = UndoStack<Int>()
        stack.push(1)
        stack.clear()
        assertNull(stack.peek())
        assertEquals(0, stack.size)
    }
}
