package com.github.suusan2go.kotlinfillclass.helper

import com.intellij.util.SystemProperties

object K2SupportHelper {
    fun isK2PluginEnabled(): Boolean = SystemProperties.getBooleanProperty("idea.kotlin.plugin.use.k2", false)
}