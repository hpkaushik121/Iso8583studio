package `in`.aicortex.iso8583studio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A custom navigation controller that wraps Voyager's Navigator to provide a
 * simplified and centralized navigation API.
 *
 * @param navigator The underlying Voyager Navigator instance that manages the screen stack.
 */
class NavigationController(private val navigator: Navigator) {

    /**
     * Exposes the current screen at the top of the stack as a Compose State.
     * The UI will automatically recompose whenever this value changes.
     */
    val currentScreen: State<Screen> = derivedStateOf {
        // We cast to AppScreen, assuming all screens in the stack are of this sealed type.
        navigator.lastItem
    }

    /**
     * Navigates to a screen safely:
     *   1. Already at top   → no-op (avoids redundant recomposition)
     *   2. Exists in stack  → pop back to it  (prevents duplicate-key crash when the same
     *                         singleton Screen object, e.g. Destination.Home, appears twice)
     *   3. Not in stack     → push normally
     *
     * Voyager's SaveableStateHolder keys screens by their [Screen.key], which is the
     * fully-qualified class name for `object` screens.  Pushing the same object twice
     * creates two SaveableStateProviders with identical keys, causing:
     *   "IllegalArgumentException: Key … was used multiple times"
     */
    fun navigateTo(screen: Screen) {
        when {
            navigator.lastItem == screen        -> return
            navigator.items.contains(screen)    -> navigator.popUntil { it == screen }
            else                                -> navigator.push(screen)
        }
    }

    /**
     * Navigates back to the previous screen by popping the current one from the stack.
     * Does nothing if there is only one screen on the stack.
     */
    fun goBack() {
        if (navigator.canPop) {
            navigator.pop()
        }
    }
}

/**
 * A Composable function to create and remember an instance of our custom NavigationController.
 * This ensures the controller is stable across recompositions and is tied to the lifecycle
 * of the Navigator.
 *
 * @param navigator The Voyager Navigator instance.
 * @return A remembered instance of [NavigationController].
 */
@Composable
fun rememberNavigationController(navigator: Navigator): NavigationController {
    return remember(navigator) {
        NavigationController(navigator)
    }
}
