package com.arindam.camerax.ui.home.permission

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewbinding.ViewBinding
import com.arindam.camerax.ui.base.BaseFragment
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.REQUIRED_PERMISSIONS

/**
 * The sole purpose of this fragment is to request permissions and, once granted, display the
 * camera fragment to the user.
 *
 * Created by Arindam Karmakar on 9/5/19.
 */

class PermissionsFragment : BaseFragment<ViewBinding>() {

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle Permission granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && !it.value) permissionGranted = false
        }
        if (!permissionGranted) {
            showToast("Permission request denied")
        } else {
            showToast("Permission request granted")
            // Take the user to the success fragment when permission is granted
            navigate(PermissionsFragmentDirections.actionPermissionsToCamera())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasPermissions()) {
            // Request camera-related permissions
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        } else {
            // If permissions have already been granted, proceed
            navigate(PermissionsFragmentDirections.actionPermissionsToCamera())
        }
    }

    override fun setupView(view: View, savedInstanceState: Bundle?) {
        // Empty
    }
}
