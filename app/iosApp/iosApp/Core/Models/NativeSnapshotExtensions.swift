import Foundation

extension NativeSubjectSummary {
    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }

    static func placeholder(id: Int64, title: String) -> NativeSubjectSummary {
        NativeSubjectSummary(
            id: id,
            title: title,
            originalTitle: nil,
            posterUrl: nil,
            year: nil,
            type: "Other",
            airingStatus: "Unknown",
            rating: nil,
            ratingVotes: 0,
        )
    }
}

extension NativeCollectionItemSnapshot {
    var subjectSummary: NativeSubjectSummary {
        NativeSubjectSummary(
            id: subjectId,
            title: title,
            originalTitle: originalTitle.isEmpty ? nil : originalTitle,
            posterUrl: posterUrl,
            year: nil,
            type: "Other",
            airingStatus: status == "Completed" ? "Finished" : "Unknown",
            rating: score > 0 ? score : nil,
            ratingVotes: 0,
        )
    }
}

extension NativeActivityItemSnapshot {
    var subjectSummary: NativeSubjectSummary? {
        guard let subjectId, let subjectTitle else { return nil }
        return NativeSubjectSummary(
            id: subjectId,
            title: subjectTitle,
            originalTitle: nil,
            posterUrl: posterUrl,
            year: nil,
            type: "Other",
            airingStatus: "Unknown",
            rating: nil,
            ratingVotes: 0,
        )
    }
}

extension NativeNotificationSnapshot {
    var title: String {
        let actor = actorName ?? AnimeL10n.string(.someone)
        switch kind {
        case "review_liked": return AnimeL10n.string(.notificationReviewLiked, actor)
        case "comment_liked": return AnimeL10n.string(.notificationCommentLiked, actor)
        case "comment_replied": return AnimeL10n.string(.notificationCommentReplied, actor)
        case "followed": return AnimeL10n.string(.notificationFollowed, actor)
        default: return AnimeL10n.string(.notificationDefault, actor)
        }
    }
}

extension NativeSessionSnapshot {
    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
}

extension String {
    var collectionDisplayName: String {
        switch self {
        case "Wish", "wish": return AnimeL10n.string(.statusWish)
        case "Watching", "watching": return AnimeL10n.string(.statusWatching)
        case "Completed", "completed": return AnimeL10n.string(.statusCompleted)
        case "OnHold", "on_hold", "onhold": return AnimeL10n.string(.statusOnHold)
        case "Dropped", "dropped": return AnimeL10n.string(.statusDropped)
        default: return self
        }
    }

    var feedDisplayName: String {
        switch self {
        case "following": return AnimeL10n.string(.feedFollowing)
        case "popular": return AnimeL10n.string(.feedPopular)
        case "public": return AnimeL10n.string(.feedPublic)
        default: return self
        }
    }

    var adminDisplayName: String {
        switch self {
        case "published": return AnimeL10n.string(.moderationPublished)
        case "hidden": return AnimeL10n.string(.moderationHidden)
        case "deleted": return AnimeL10n.string(.moderationDeleted)
        default: return self
        }
    }

    var reportDisplayName: String {
        switch self {
        case "spam": return AnimeL10n.string(.reportSpam)
        case "harassment": return AnimeL10n.string(.reportHarassment)
        case "spoiler": return AnimeL10n.string(.reportSpoiler)
        case "illegal": return AnimeL10n.string(.reportIllegal)
        case "other": return AnimeL10n.string(.reportOther)
        default: return self
        }
    }

    var adminReportDisplayName: String {
        switch self {
        case "open": return AnimeL10n.string(.reportOpen)
        case "claimed": return AnimeL10n.string(.reportClaimed)
        case "resolved": return AnimeL10n.string(.reportResolved)
        case "dismissed": return AnimeL10n.string(.reportDismissed)
        default: return self
        }
    }

    var searchTypeName: String {
        switch self {
        case "tv": return "TV"
        case "web": return "Web"
        case "ova": return "OVA"
        case "movie": return AnimeL10n.string(.subjectTypeMovie)
        case "other": return AnimeL10n.string(.subjectTypeOther)
        default: return self
        }
    }

    var searchAiringName: String {
        switch self {
        case "announced": return AnimeL10n.string(.subjectAiringAnnounced)
        case "airing": return AnimeL10n.string(.subjectAiring)
        case "finished": return AnimeL10n.string(.subjectFinished)
        default: return self
        }
    }

    var searchSortName: String {
        switch self {
        case "rating": return AnimeL10n.string(.searchSortRating)
        case "updated": return AnimeL10n.string(.searchSortUpdated)
        default: return AnimeL10n.string(.searchSortRelevance)
        }
    }
}

extension String {
    var displayName: String {
        switch self {
        case "Tv": return AnimeL10n.string(.subjectTypeTv)
        case "Web": return AnimeL10n.string(.subjectTypeWeb)
        case "Ova": return AnimeL10n.string(.subjectTypeOva)
        case "Movie": return AnimeL10n.string(.subjectTypeMovie)
        case "Airing": return AnimeL10n.string(.subjectAiring)
        case "Finished": return AnimeL10n.string(.subjectFinished)
        case "Announced": return AnimeL10n.string(.subjectAiringAnnounced)
        default: return self
        }
    }
}
