package site.jokersh.anime.core.navigation

import androidx.navigation3.runtime.NavKey

/** The only mutation surface for the application's four independent navigation stacks. */
public class AppNavigator(
    private val currentRoot: () -> AppRoot,
    private val updateRoot: (AppRoot) -> Unit,
    private val stackFor: (AppRoot) -> MutableList<NavKey>,
) {
    public fun push(route: AppRoute) {
        activeStack().add(route)
    }

    public fun replaceTop(route: AppRoute) {
        val stack = activeStack()
        if (stack.size <= 1) {
            stack.add(route)
        } else {
            stack[stack.lastIndex] = route
        }
    }

    /** Returns true only when a child destination was removed. */
    public fun pop(): Boolean {
        val stack = activeStack()
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    public fun popToRoot() {
        val stack = activeStack()
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    public fun selectRoot(root: AppRoot) {
        updateRoot(root)
    }

    private fun activeStack(): MutableList<NavKey> = stackFor(currentRoot())
}
