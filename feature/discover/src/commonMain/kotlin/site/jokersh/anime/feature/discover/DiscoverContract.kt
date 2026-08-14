package site.jokersh.anime.feature.discover

import androidx.compose.runtime.Immutable
import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.SubjectId

@Immutable
public sealed interface AsyncContent<out T> {
    public data object Initial : AsyncContent<Nothing>

    public data object Loading : AsyncContent<Nothing>

    public data class Content<T>(
        val value: T,
    ) : AsyncContent<T>

    public data object Empty : AsyncContent<Nothing>

    public data class Failure(
        val error: DiscoverErrorUi,
    ) : AsyncContent<Nothing>
}

@Immutable
public data class DiscoverContentUi(
    val heroes: List<SubjectCardUi>,
    val sections: List<DiscoverSectionUi>,
) {
    init {
        require(heroes.isNotEmpty()) { "Discover hero carousel must not be empty" }
    }

    public val hero: SubjectCardUi
        get() = heroes.first()
}

@Immutable
public data class DiscoverSectionUi(
    val id: String,
    val title: String,
    val description: String,
    val subjects: List<SubjectCardUi>,
)

public enum class DiscoverErrorUi {
    Offline,
    RateLimited,
    ServiceUnavailable,
    InvalidData,
    Unknown,
}

@Immutable
public data class DiscoverUiState(
    val content: AsyncContent<DiscoverContentUi> = AsyncContent.Initial,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val lastUpdatedLabel: String? = null,
)

public sealed interface DiscoverIntent {
    public data object Entered : DiscoverIntent

    public data object Refresh : DiscoverIntent

    public data object Retry : DiscoverIntent

    public data class SubjectClicked(
        val subjectId: SubjectId,
    ) : DiscoverIntent

    public data class SectionMoreClicked(
        val sectionId: String,
    ) : DiscoverIntent
}

public sealed interface DiscoverEffect {
    public data class NavigateToSubject(
        val subjectId: SubjectId,
    ) : DiscoverEffect

    public data class NavigateToSection(
        val sectionId: String,
    ) : DiscoverEffect

    public data class ShowMessage(
        val message: DiscoverMessageUi,
    ) : DiscoverEffect
}

public enum class DiscoverMessageUi {
    RefreshFailed,
}
