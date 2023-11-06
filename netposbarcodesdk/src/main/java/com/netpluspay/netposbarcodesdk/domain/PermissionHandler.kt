package com.netpluspay.netposbarcodesdk.domain

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import pub.devrel.easypermissions.EasyPermissions

interface PermissionHandler : EasyPermissions.PermissionCallbacks {
    fun checkForPermission(context: Context, perms: String): Boolean
    fun requestForPermission(
        host: Activity,
        requestCode: Int,
        permissionRationale: String,
        permissionToRequest: String,
    )

    fun permissionHandler(
        host: Activity,
        context: Context,
        permissionToRequest: String,
        permRequestCode: Int,
        @StringRes permRationaleStringId: Int,
        fn: () -> Unit,
    )
}
