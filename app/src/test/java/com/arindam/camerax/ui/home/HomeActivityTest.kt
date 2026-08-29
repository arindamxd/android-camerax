package com.arindam.camerax.ui.home

import androidx.navigation.fragment.NavHostFragment
import com.arindam.camerax.R
import com.arindam.camerax.testing.RobolectricPermissions
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeActivityTest {

    @Test
    fun startDestination_isCameraWhenPermissionGranted() {
        RobolectricPermissions.grantCamera()
        val activity = Robolectric.buildActivity(HomeActivity::class.java).setup().get()
        val navHost = activity.supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        assertEquals(R.id.cameraFragment, navHost.navController.currentDestination?.id)
    }

    @Test
    fun startDestination_isPermissionsWhenCameraDenied() {
        RobolectricPermissions.denyCamera()
        val activity = Robolectric.buildActivity(HomeActivity::class.java).setup().get()
        val navHost = activity.supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        assertEquals(R.id.permissionsFragment, navHost.navController.currentDestination?.id)
    }
}
