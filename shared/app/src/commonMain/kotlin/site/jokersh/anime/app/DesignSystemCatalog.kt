package site.jokersh.anime.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import site.jokersh.anime.app.generated.resources.Res
import site.jokersh.anime.app.generated.resources.f1_bangumi_accessibility
import site.jokersh.anime.app.generated.resources.f1_bangumi_empty
import site.jokersh.anime.app.generated.resources.f1_bangumi_source
import site.jokersh.anime.app.generated.resources.f1_bangumi_votes
import site.jokersh.anime.app.generated.resources.f1_catalog_description
import site.jokersh.anime.app.generated.resources.f1_catalog_title
import site.jokersh.anime.app.generated.resources.f1_subject_primary
import site.jokersh.anime.app.generated.resources.f1_subject_primary_accessibility
import site.jokersh.anime.app.generated.resources.f1_subject_primary_metadata
import site.jokersh.anime.app.generated.resources.f1_subject_secondary
import site.jokersh.anime.app.generated.resources.f1_subject_secondary_accessibility
import site.jokersh.anime.app.generated.resources.f1_subject_secondary_metadata
import site.jokersh.anime.core.designsystem.AnimePosterCard
import site.jokersh.anime.core.designsystem.AnimeRatingBadge
import site.jokersh.anime.core.designsystem.AnimeSectionHeader
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.BangumiRatingUi
import site.jokersh.anime.core.designsystem.GlassRole
import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.SubjectId

@Composable
internal fun DesignSystemCatalog() {
    val primary =
        SubjectCardUi(
            id = SubjectId(1001),
            title = stringResource(Res.string.f1_subject_primary),
            originalTitle = null,
            poster = null,
            metadata = stringResource(Res.string.f1_subject_primary_metadata),
            rating = "8.6 Bangumi",
            collectionLabel = null,
            accessibilityLabel = stringResource(Res.string.f1_subject_primary_accessibility),
        )
    val secondary =
        SubjectCardUi(
            id = SubjectId(1003),
            title = stringResource(Res.string.f1_subject_secondary),
            originalTitle = null,
            poster = null,
            metadata = stringResource(Res.string.f1_subject_secondary_metadata),
            rating = null,
            collectionLabel = null,
            accessibilityLabel = stringResource(Res.string.f1_subject_secondary_accessibility),
        )

    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg)) {
        AnimeSectionHeader(
            title = stringResource(Res.string.f1_catalog_title),
            description = stringResource(Res.string.f1_catalog_description),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
        ) {
            AnimePosterCard(model = primary, onClick = {})
            AnimePosterCard(model = secondary, onClick = {})
        }
        AnimeRatingBadge(
            rating =
                BangumiRatingUi(
                    score = "8.6",
                    votesLabel = stringResource(Res.string.f1_bangumi_votes),
                    updatedAtLabel = null,
                    sourceLabel = stringResource(Res.string.f1_bangumi_source),
                    emptyLabel = stringResource(Res.string.f1_bangumi_empty),
                    accessibilityLabel = stringResource(Res.string.f1_bangumi_accessibility),
                ),
            role = GlassRole.StaticHero,
        )
    }
}
