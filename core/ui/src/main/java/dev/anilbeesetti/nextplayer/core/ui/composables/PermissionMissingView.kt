package dev.anilbeesetti.nextplayer.core.ui.composables

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.anilbeesetti.nextplayer.core.common.isAllFilesAccessGranted
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun PermissionMissingView(
    isGranted: Boolean,
    showRationale: Boolean,
    permission: String,
    launchPermissionRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val effectiveGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.isAllFilesAccessGranted()
    } else {
        isGranted
    }

    if (effectiveGranted) {
        content()
    } else if (showRationale) {
        PermissionRationaleDialog(
            text = stringResource(
                id = R.string.permission_info,
                permission,
            ),
            onConfirmButtonClick = launchPermissionRequest,
        )
    } else {
        PermissionDetailView(
            text = stringResource(
                id = R.string.permission_settings,
                permission,
            ),
        )
    }
}
