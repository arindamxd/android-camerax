package com.arindam.camerax.ui.base

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections

/**
 * Presentation: whether [NavController.navigate] would find a current destination and a matching
 * action. Call before navigate so a missing graph cannot crash after process restore.
 */
fun canNavigate(currentDestination: NavDestination?, actionId: Int): Boolean =
    currentDestination?.getAction(actionId) != null

/** Presentation: [canNavigate] against this controller's current destination. */
fun NavController.canNavigate(directions: NavDirections): Boolean =
    canNavigate(currentDestination, directions.actionId)
