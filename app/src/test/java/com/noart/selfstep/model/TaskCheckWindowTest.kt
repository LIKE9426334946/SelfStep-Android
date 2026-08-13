package com.noart.selfstep.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TaskCheckWindowTest {
    @Test
    fun opensAtTenFortyFivePm() {
        assertFalse(TaskCheckWindow.isOpen(LocalTime.of(22, 44, 59)))
        assertTrue(TaskCheckWindow.isOpen(LocalTime.of(22, 45)))
    }

    @Test
    fun includesTheEntireElevenThirtyMinute() {
        assertTrue(TaskCheckWindow.isOpen(LocalTime.of(23, 30, 59, 999_999_999)))
        assertFalse(TaskCheckWindow.isOpen(LocalTime.of(23, 31)))
    }
}
