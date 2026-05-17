package com.example.sayobotdownloader

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object SearchRoute : NavKey
@Serializable data class DetailRoute(val sid: Int, val title: String) : NavKey