package com.dreamjournal.app.ui.navigation

sealed class AppDestination(val route: String, val label: String) {
    data object Home : AppDestination("home", "记录")
    data object Calendar : AppDestination("calendar", "日历")
    data object Settings : AppDestination("settings", "我的")
    data object Detail : AppDestination("detail/{entryId}", "详情") {
        fun createRoute(entryId: Long): String = "detail/$entryId"
    }
}
