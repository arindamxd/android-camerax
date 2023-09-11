package com.arindam.camerax.ui.base

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.arindam.camerax.R
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.REQUIRED_PERMISSIONS
import com.arindam.camerax.util.display.Toaster
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

abstract class BaseFragment<T : ViewBinding> : Fragment() {

    protected lateinit var binding: T

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = provideBinding().let {
        return if (it == null) provideView()
        else {
            binding = it
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view, savedInstanceState)
    }

    open fun provideBinding(): T? = null
    open fun provideView(): View? = View(context)

    abstract fun setupView(view: View, savedInstanceState: Bundle?)

    protected fun navigate(directions: NavDirections) = findNavController().navigate(directions)
    protected fun navigateBack() = findNavController().navigateUp()

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
