package com.arindam.camerax.ui.home

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import com.arindam.camerax.R
import com.arindam.camerax.databinding.ActivityHomeBinding
import com.arindam.camerax.ui.base.BaseActivity
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.REQUIRED_PERMISSIONS

/**
 * Presentation: main entry. Single-activity host; camera, gallery, and permissions are fragments.
 */
class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    override fun provideBinding(): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

    override fun setupView(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return
        val navHost = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHost.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(
            if (hasPermissions()) R.id.cameraFragment else R.id.permissionsFragment
        )
        navController.graph = graph
    }

    private fun hasPermissions(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}
