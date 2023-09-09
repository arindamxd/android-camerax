package com.arindam.camerax.ui.home.permission

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import com.arindam.camerax.R
import com.arindam.camerax.ui.base.BaseFragment
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.PERMISSIONS_REQUEST_CODE
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.PERMISSIONS_REQUIRED

/**
 * The sole purpose of this fragment is to request permissions and, once granted, display the
 * camera fragment to the user.
 *
 * Created by Arindam Karmakar on 9/5/19.
 */

class PermissionsFragment : BaseFragment() {

    override fun provideLayout(): Int = 0
    override fun provideView(): View? = View(context)

    override fun setupView(view: View, savedInstanceState: Bundle?) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasPermissions()) {
            // Request camera-related permissions
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        } else {
            // If permissions have already been granted, proceed
            navigate(PermissionsFragmentDirections.actionPermissionsToCamera())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Permission request granted")

                // Take the user to the success fragment when permission is granted
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    PermissionsFragmentDirections.actionPermissionsToCamera()
                )
            } else {
                showToast("Permission request denied")
            }
        }
    }
}
