package site.jokersh.anime.app

import site.jokersh.anime.data.catalog.CatalogRepository
import site.jokersh.anime.data.catalog.FixtureCatalogRepository
import site.jokersh.anime.data.catalog.FixtureSearchRepository
import site.jokersh.anime.data.catalog.SearchRepository
import site.jokersh.anime.data.collection.CollectionRepository
import site.jokersh.anime.data.collection.InMemoryCollectionStore
import site.jokersh.anime.data.collection.OfflineFirstCollectionRepository
import site.jokersh.anime.data.comment.CommentRepository
import site.jokersh.anime.data.comment.CommunityRepository
import site.jokersh.anime.data.comment.EmptyCommunityRepository
import site.jokersh.anime.data.comment.InMemoryCommentDraftStore
import site.jokersh.anime.data.comment.RemoteCommentRepository
import site.jokersh.anime.data.session.FixtureSessionRepository
import site.jokersh.anime.data.session.SessionRepository
import site.jokersh.anime.data.settings.InMemorySettingsStore
import site.jokersh.anime.data.settings.PersistentSettingsRepository
import site.jokersh.anime.data.settings.SettingsRepository

class AppContainer internal constructor(
    val profile: BuildProfile,
    val catalogRepository: CatalogRepository,
    val searchRepository: SearchRepository,
    val sessionRepository: SessionRepository,
    val communityRepository: CommunityRepository,
    val settingsRepository: SettingsRepository,
    val collectionRepository: CollectionRepository,
    val commentRepository: CommentRepository,
)

fun createAppContainer(
    profile: BuildProfile,
    catalogRepository: CatalogRepository = FixtureCatalogRepository(),
    searchRepository: SearchRepository = FixtureSearchRepository(),
    sessionRepository: SessionRepository = FixtureSessionRepository(),
    communityRepository: CommunityRepository = EmptyCommunityRepository(),
    settingsRepository: SettingsRepository = PersistentSettingsRepository(InMemorySettingsStore()),
    collectionRepository: CollectionRepository =
        OfflineFirstCollectionRepository(
            InMemoryCollectionStore(),
        ) { _, _, _ -> Result.failure(IllegalStateException("Collection service is unavailable")) },
    commentRepository: CommentRepository =
        RemoteCommentRepository(communityRepository, InMemoryCommentDraftStore(), currentUserId = { null }),
): AppContainer =
    AppContainer(
        profile = profile,
        catalogRepository = catalogRepository,
        searchRepository = searchRepository,
        sessionRepository = sessionRepository,
        communityRepository = communityRepository,
        settingsRepository = settingsRepository,
        collectionRepository = collectionRepository,
        commentRepository = commentRepository,
    )
