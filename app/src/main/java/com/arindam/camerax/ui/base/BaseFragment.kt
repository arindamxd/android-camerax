package com.arindam.camerax.ui.base

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.arindam.camerax.R
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.REQUIRED_PERMISSIONS
import com.arindam.camerax.util.display.Toaster
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

abstract class BaseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = provideView() ?: inflater.inflate(provideLayout(), container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view, savedInstanceState)
    }

    @LayoutRes
    abstract fun provideLayout(): Int
    abstract fun setupView(view: View, savedInstanceState: Bundle?)

    open fun provideView(): View? = null

    protected fun navigateBack(@IdRes viewId: Int) {
        Navigation.findNavController(requireActivity(), viewId).navigateUp()
    }

    protected fun navigate(navDirections: NavDirections) {
        findNavController().navigate(navDirections)
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    protected fun hasPermissions() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    /** Use external media if it is available, our app's file directory otherwise */
    protected fun getOutputFileDirectory(): File {
        val appContext = requireContext().applicationContext
        val mediaDir = requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
    }

    protected fun isDirectoryNotEmpty(): Boolean = getOutputFileDirectory().listFiles()?.isNotEmpty() == true

    /** Helper function used to create a timestamped file */
    protected fun createFile(baseFolder: File, format: String, extension: String) = File(
        baseFolder, SimpleDateFormat(format, Locale.US).format(System.currentTimeMillis()) + extension
    )

    protected fun createFileName(format: String): String {
        return SimpleDateFormat(format, Locale.US).format(System.currentTimeMillis())
    }

    protected fun showToast(@StringRes resId: Int) {
        showToast(requireContext().getString(resId))
    }

    protected fun showToast(message: String) {
        Toaster.show(requireContext(), message)
    }
}
