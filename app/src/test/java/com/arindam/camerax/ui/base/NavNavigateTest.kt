package com.arindam.camerax.ui.base

import androidx.navigation.NavAction
import androidx.navigation.NavDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavNavigateTest {

    @Test
    fun canNavigate_falseWhenDestinationMissing() {
        assertFalse(canNavigate(null, actionId = 2))
    }

    @Test
    fun canNavigate_falseWhenActionMissing() {
        val destination = NavDestination("fragment").apply { id = 1 }
        assertFalse(canNavigate(destination, actionId = 2))
    }

    @Test
    fun canNavigate_trueWhenActionExists() {
        val destination = NavDestination("fragment").apply {
            id = 1
            putAction(2, NavAction(3))
        }
        assertTrue(canNavigate(destination, actionId = 2))
    }
}
