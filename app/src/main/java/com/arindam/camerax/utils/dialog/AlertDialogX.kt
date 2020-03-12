package com.arindam.camerax.utils.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.arindam.camerax.R
import com.arindam.camerax.utils.FLAGS_FULLSCREEN

/**
 * Created by Arindam Karmakar on 13/03/20.
 */

class AlertDialogX {

    open class Builder(private val context: Context) {

        var title: String? = null
        var message: String? = null
        var positiveBtnText: String? = null
        var negativeBtnText: String? = null
        var pListener: AlertDialogXListener? = null
        var nListener: AlertDialogXListener? = null
        var icon = 0
        var visibility: Icon? = null
        var animation: Animation? = null
        var pBtnColor = 0
        var nBtnColor = 0
        var bgColor = 0
        var cancel = false

        fun setTitle(title: String?): Builder = apply {
            this.title = title
        }

        fun setMessage(message: String?): Builder = apply {
            this.message = message
        }

        fun setBackgroundColor(bgColor: Int): Builder = apply {
            this.bgColor = bgColor
        }

        fun setPositiveBtnText(positiveBtnText: String?): Builder = apply {
            this.positiveBtnText = positiveBtnText
        }

        fun setPositiveBtnBackground(pBtnColor: Int): Builder = apply {
            this.pBtnColor = pBtnColor
        }

        fun setNegativeBtnText(negativeBtnText: String?): Builder = apply {
            this.negativeBtnText = negativeBtnText
        }

        fun setNegativeBtnBackground(nBtnColor: Int): Builder = apply {
            this.nBtnColor = nBtnColor
        }

        fun setIcon(icon: Int, visibility: Icon?): Builder = apply {
            this.icon = icon
            this.visibility = visibility
        }

        fun setAnimation(animation: Animation?): Builder = apply {
            this.animation = animation
        }

        // Set Positive Listener
        fun onPositiveClicked(pListener: AlertDialogXListener?): Builder = apply {
            this.pListener = pListener
        }

        // Set Negative Listener
        fun onNegativeClicked(nListener: AlertDialogXListener?): Builder = apply {
            this.nListener = nListener
        }

        fun isCancellable(cancel: Boolean): Builder = apply {
            this.cancel = cancel
        }

        fun build() {

            val dialog = when {
                animation === Animation.POP -> Dialog(context, R.style.PopTheme)
                animation === Animation.SIDE -> Dialog(context, R.style.SideTheme)
                animation === Animation.SLIDE -> Dialog(context, R.style.SlideTheme)
                else -> Dialog(context)
            }

            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setCancelable(cancel)
            dialog.setContentView(R.layout.layout_dialogx)

            // Getting Resources
            val view = dialog.findViewById(R.id.background) as View
            val titleView = dialog.findViewById(R.id.title) as TextView
            val messageView = dialog.findViewById(R.id.message) as TextView
            val iconImg = dialog.findViewById(R.id.icon) as ImageView
            val nBtn = dialog.findViewById(R.id.negativeBtn) as Button
            val pBtn = dialog.findViewById(R.id.positiveBtn) as Button

            titleView.text = title
            messageView.text = message

            if (positiveBtnText != null) pBtn.text = positiveBtnText
            if (negativeBtnText != null) nBtn.text = negativeBtnText

            if (pBtnColor != 0) {
                val bgShape = pBtn.background as GradientDrawable
                bgShape.setColor(pBtnColor)
            }
            if (nBtnColor != 0) {
                val bgShape = nBtn.background as GradientDrawable
                bgShape.setColor(nBtnColor)
            }

            iconImg.setImageResource(icon)
            iconImg.visibility = if (visibility === Icon.VISIBLE) View.VISIBLE else View.GONE
            if (bgColor != 0) view.setBackgroundColor(bgColor)

            pBtn.setOnClickListener {
                pListener?.onClick()
                dialog.dismiss()
            }
            if (nListener != null) {
                nBtn.visibility = View.VISIBLE
                nBtn.setOnClickListener {
                    nListener?.onClick()
                    dialog.onBackPressed()
                    dialog.dismiss()
                }
            }

            // Set the dialog to not focusable
            dialog.window?.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )

            // Make sure that the dialog's window is in full screen
            dialog.window?.decorView?.systemUiVisibility = FLAGS_FULLSCREEN

            // Show the dialog while still in immersive mode
            dialog.show()

            // Set the dialog to focusable again
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
    }
}
