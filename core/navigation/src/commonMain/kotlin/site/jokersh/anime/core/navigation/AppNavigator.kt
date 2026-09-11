package site.jokersh.anime.core.navigation

import androidx.navigation3.runtime.NavKey

public enum class RootSelectionResult {
    Switched,
    ReturnedToRoot,
    Unchanged,
}

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
        } else if (stack.lastOrNull() != route) {
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

    /**
     * Selects a root destination and describes the user-visible navigation operation.
     *
     * The explicit result keeps platform shells from having to infer intent from a Boolean:
     * switching tabs and returning to the active tab's root are both root-navigation actions,
     * while tapping an already-visible root is a no-op.
     */
    public fun selectRoot(root: AppRoot): RootSelectionResult {
        if (currentRoot() == root) {
            // Match the platform tab-bar convention: tapping the active tab returns
            // to that tab's root instead of leaving a deep child route on screen.
            val changed = activeStack().size > 1
            popToRoot()
            return if (changed) RootSelectionResult.ReturnedToRoot else RootSelectionResult.Unchanged
        } else {
            updateRoot(root)
            return RootSelectionResult.Switched
        }
    }

    private fun activeStack(): MutableList<NavKey> = stackFor(currentRoot())
}
