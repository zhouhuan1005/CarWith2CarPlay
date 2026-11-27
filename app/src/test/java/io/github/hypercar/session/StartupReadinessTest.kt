package io.github.hypercar.session

import org.junit.Assert.*
import org.junit.Test

class StartupReadinessTest {
    /** v1.1.0.0 起恒定 CarWith：source 只剩 "carwith"/null（null 与 carwith 同判）。 */
    private val ready = StartupReadiness(
        source = "carwith", shizukuReady = false, carWithInstalled = true, overlayAllowed = true,
    )

    @Test fun unknownSourceFallsBackToCarWithRequirement() {
        // source 为 null（历史遗留）也按 CarWith 要求判。
        assertTrue(ready.copy(source = null).manualReady)
        assertTrue(ready.copy(source = "unknown").manualReady)
    }

    @Test fun carWithRequiresItsOwnPackageButNotShizuku() {
        assertTrue(ready.manualReady)
        assertTrue(ready.automaticReady)
        // Shizuku 未授权不再阻塞（CarWith 模式不需要）。
        assertFalse(ready.copy(carWithInstalled = false).manualReady)
        assertTrue(
            ready.copy(carWithInstalled = false, shizukuReady = true).manualIssues
                .contains("安装并启用系统 CarWith（com.miui.carlink）"),
        )
    }

    @Test fun requiredPermissionRevocationBlocksBothEntries() {
        for (permission in listOf("附近设备", "通知", "定位")) {
            val blocked = ready.copy(missingPermissions = listOf(permission))
            assertFalse(blocked.manualReady)
            assertFalse(blocked.automaticReady)
            assertTrue(blocked.manualIssues.contains(permission))
        }
    }

    @Test fun radiosAndRequiredLocationServiceMustBeEnabled() {
        assertFalse(ready.copy(bluetoothEnabled = false).manualReady)
        assertFalse(ready.copy(wifiEnabled = false).automaticReady)
        assertFalse(ready.copy(locationEnabled = false).manualReady)
    }

    @Test fun coldStandbyCanWaitForBluetoothButStillChecksOtherRequirements() {
        val radioOff = ready.copy(bluetoothEnabled = false)
        assertTrue(radioOff.automaticStandbyIssues.isEmpty())
        assertFalse(radioOff.automaticReady)
        assertFalse(radioOff.copy(missingPermissions = listOf("附近设备")).automaticStandbyIssues.isEmpty())
        assertFalse(radioOff.copy(wifiEnabled = false).automaticStandbyIssues.isEmpty())
    }

    @Test fun missingOverlayBlocksAutomaticOnly() {
        val foregroundOnly = ready.copy(overlayAllowed = false)
        assertTrue(foregroundOnly.manualReady)
        assertFalse(foregroundOnly.automaticReady)
        assertEquals(1, foregroundOnly.automaticIssues.size)
    }
}
