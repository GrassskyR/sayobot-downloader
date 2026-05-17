package com.example.sayobotdownloader

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.sayobotdownloader.ui.search.SearchScreen
import com.example.sayobotdownloader.ui.detail.DetailScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(SearchRoute)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<SearchRoute> {
          SearchScreen(
            onItemClick = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<DetailRoute> { route ->
          DetailScreen(
            sid = route.sid,
            title = route.title,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}