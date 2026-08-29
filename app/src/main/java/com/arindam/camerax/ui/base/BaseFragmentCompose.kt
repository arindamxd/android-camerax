package com.arindam.camerax.ui.base

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.REQUIRED_PERMISSIONS
import com.arindam.camerax.util.display.Toaster

/** Compose [Fragment] host: navigation and permission check. */
abstract class BaseFragmentCompose : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setComposeView(this)
    }

    abstract fun setComposeView(view: ComposeView)

    protected fun navigate(directions: NavDirections, fromDestinationId: Int? = null) {
        val controller = findNavController()
        if (fromDestinationId != null && controller.currentDestination?.id != fromDestinationId) {
            return
        }
        if (!controller.canNavigate(directions)) return
        controller.navigate(directions)
    }

    protected fun navigateBack() {
        val controller = findNavController()
        if (controller.currentDestination == null) return
        controller.navigateUp()
    }

    protected fun hasPermissions() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    protected fun showToast(@StringRes resId: Int) {
        showToast(requireContext().getString(resId))
    }

    protected fun showToast(message: String) {
        Toaster.show(requireContext(), message)
    }
}
