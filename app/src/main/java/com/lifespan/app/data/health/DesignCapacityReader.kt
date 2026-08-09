package com.lifespan.app.data.health

import android.content.Context

/**
 * Reads the battery's design capacity (mAh) from the hidden framework
 * `PowerProfile`, the same source Settings uses. Reflection is required
 * because the class is not part of the public SDK; returns null when the
 * value is unavailable or implausible.
 */
class DesignCapacityReader(private val context: Context) {

    val designCapacityMah: Double? by lazy {
        runCatching {
            val clazz = Class.forName("com.android.internal.os.PowerProfile")
            val profile = clazz.getConstructor(Context::class.java).newInstance(context)
            val capacity = clazz.getMethod("getBatteryCapacity").invoke(profile) as Double
            capacity.takeIf { it > 100.0 }
        }.getOrNull()
    }
}
