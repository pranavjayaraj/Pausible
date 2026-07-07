package com.reset.app.ui

import com.reset.navigation.Screen
import kotlinx.serialization.Serializable

/**
 * The root host's start destination: the parent screen owning the dashboard's tabbed
 * graph and its bottom bar. App-owned — features never navigate here directly; they
 * switch tabs, and the host resolves those onto the dashboard's inner graph.
 */
@Serializable
data object DashboardDestination : Screen
