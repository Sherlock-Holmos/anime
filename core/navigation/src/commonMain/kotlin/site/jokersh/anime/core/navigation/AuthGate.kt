package site.jokersh.anime.core.navigation

import kotlinx.serialization.Serializable
import site.jokersh.anime.core.model.SessionState

/** A serializable user intent that must survive authentication before it is executed. */
@Serializable
public sealed interface PendingAuthAction {
    public val actionId: String

    @Serializable
    public data class SetCollection(
        val subjectId: Long,
        val status: String = "wish",
    ) : PendingAuthAction {
        override val actionId: String = "collection:$subjectId:$status"
    }

    @Serializable
    public data class OpenRating(
        val subjectId: Long,
    ) : PendingAuthAction {
        override val actionId: String = "rating:$subjectId"
    }

    @Serializable
    public data class OpenComments(
        val subjectId: Long,
    ) : PendingAuthAction {
        override val actionId: String = "comments:$subjectId"
    }
}

public sealed interface AuthGateDecision {
    public data class Execute(
        val action: PendingAuthAction,
    ) : AuthGateDecision

    public data class Authenticate(
        val action: PendingAuthAction,
    ) : AuthGateDecision
}

/**
 * Single-slot authentication gate. A protected intent is consumed at most once after a successful
 * session transition; cancellation explicitly discards it.
 */
public class AuthGate(
    initialAction: PendingAuthAction? = null,
) {
    public var pendingAction: PendingAuthAction? = initialAction
        private set

    public fun request(
        action: PendingAuthAction,
        sessionState: SessionState,
    ): AuthGateDecision =
        if (sessionState is SessionState.Authenticated) {
            pendingAction = null
            AuthGateDecision.Execute(action)
        } else {
            pendingAction = action
            AuthGateDecision.Authenticate(action)
        }

    public fun replayAfterAuthentication(sessionState: SessionState): PendingAuthAction? {
        if (sessionState !is SessionState.Authenticated) return null
        return pendingAction.also { pendingAction = null }
    }

    public fun cancel() {
        pendingAction = null
    }
}
