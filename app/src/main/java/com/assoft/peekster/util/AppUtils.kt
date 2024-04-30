package com.assoft.peekster.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assoft.peekster.R
import com.assoft.peekster.activity.MainViewModel
import com.assoft.peekster.activity.shareable.dialog.RationalePermissionRequest
import com.assoft.peekster.domain.NetworkDevice
import java.util.*

object AppUtils {

    private var uniqueNumber = 0

    fun getUniqueNumber(): Int {
        return (System.currentTimeMillis() / 1000).toInt() + ++uniqueNumber
    }

    fun startForegroundService(
        context: Context,
        intent: Intent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent)
        else context.startService(
            intent
        )
    }

    fun checkRunningConditions(context: Context): Boolean {
        for (request in getRequiredPermissions(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    request.permissionRequest
                ) != PackageManager.PERMISSION_GRANTED
            ) return false
        }
        return true
    }

    fun getRequiredPermissions(context: Context): List<RationalePermissionRequest.PermissionRequest> {
        val permissionRequests: MutableList<RationalePermissionRequest.PermissionRequest> =
            mutableListOf()
        permissionRequests.add(
            RationalePermissionRequest.PermissionRequest(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                R.string.request_permission_storage,
                R.string.request_permission_storage_summary
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionRequests.add(
                RationalePermissionRequest.PermissionRequest(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                    R.string.request_permission_read_phone_state,
                    R.string.request_permission_read_phone_state_summary
                )
            )
        }
        return permissionRequests
    }

    fun getLocalDeviceName(viewModel: MainViewModel): String {
        val name = viewModel.getDeviceName() ?: Build.MODEL.toUpperCase(Locale.getDefault())
        viewModel.saveDeviceName(name)
        return name
    }

    private fun getDeviceSerial(context: Context?): String? {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> String.format(
                "{%s}",
                UUID.randomUUID().toString()
            )
            ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String.format(
                    "{%s}",
                    UUID.randomUUID().toString()
                )
            } else {
                null
            }
            else -> null
        }
    }

    fun getLocalDevice(context: Context, viewModel: MainViewModel): NetworkDevice? {
        val device = NetworkDevice(device_id = getDeviceSerial(context)).apply {
            brand = Build.BRAND
            model = Build.MODEL
            nick_name = getLocalDeviceName(viewModel)
        }
        try {
            val packageInfo = context.packageManager
                .getPackageInfo(context.applicationInfo.packageName, 0)
            device.version_number = packageInfo.versionCode
            device.version_name = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return device
    }

    @AnyRes
    fun getReference(context: Context, @AttrRes refId: Int): Int {
        val typedValue = TypedValue()
        if (!context.theme.resolveAttribute(refId, typedValue, true)) {
            val values = context.theme
                .obtainStyledAttributes(context.applicationInfo.theme, intArrayOf(refId))
            return if (values.length() > 0) values.getResourceId(0, 0) else 0
        }
        return typedValue.resourceId
    }

    fun getDefaultIconBuilder(context: Context): TextDrawable.IShapeBuilder? {
        val builder: TextDrawable.IShapeBuilder = TextDrawable.builder()
        builder.beginConfig()
            .firstLettersOnly(true)
            .textMaxLength(1)
            .textColor(
                ContextCompat.getColor(
                    context,
                    getReference(context, R.attr.colorSurface)
                )
            )
            .shapeColor(
                ContextCompat.getColor(
                    context,
                    getReference(context, R.attr.colorPassive)
                )
            )
        return builder
    }
}