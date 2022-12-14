package com.onelogin.oidc.demo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ViewAnimator
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.onelogin.oidc.Callback
import com.onelogin.oidc.OneLoginOIDC.getClient
import com.onelogin.oidc.userInfo.UserInfo
import com.onelogin.oidc.userInfo.UserInfoError
import java.text.SimpleDateFormat
import java.util.*

class UserInfoFragment : Fragment() {

    private val userId: TextView by lazy { requireView().findViewById<TextView>(R.id.user_id) }
    private val email: TextView by lazy { requireView().findViewById<TextView>(R.id.email) }
    private val preferredName: TextView by lazy { requireView().findViewById<TextView>(R.id.username) }
    private val updatedAt: TextView by lazy { requireView().findViewById<TextView>(R.id.updated_at) }
    private val animator: ViewAnimator by lazy { requireView() as ViewAnimator }
    private val format = SimpleDateFormat("EEE, MMM d, ''yy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_info, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        animator.displayedChild = 0
        getClient()
            .getUserInfo(object : Callback<UserInfo, UserInfoError> {
                override fun onSuccess(success: UserInfo) {
                    userId.text = success.sub
                    email.text = success.email
                    preferredName.text =
                        if (success.preferredUsername != null) success.preferredUsername else "Empty"
                    val lastUpdateTimestamp = success.updatedAt
                    updatedAt.text = if (lastUpdateTimestamp != null) {
                        format.format(Date(lastUpdateTimestamp * 1000))
                    } else {
                        "Empty"
                    }
                    animator.displayedChild = 1
                }

                override fun onError(error: UserInfoError) {
                    Snackbar.make(
                        requireView(),
                        error.message!!,
                        Snackbar.LENGTH_LONG
                    ).show()
                    Log.e(
                        DemoOIDCApp.LOG_TAG,
                        "Error getting user info",
                        error
                    )
                }
            })
    }
}
