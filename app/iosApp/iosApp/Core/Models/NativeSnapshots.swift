import Foundation

struct NativeSearchDiscoverySnapshot: Codable {
    let trending: [String]
    let recommendations: [NativeSubjectSummary]
    let personalized: Bool
}

struct NativeSearchResultsSnapshot: Codable {
    let items: [NativeSubjectSummary]
    let nextCursor: String?
    let hasMore: Bool
}

struct NativeCollectionPageSnapshot: Codable {
    let items: [NativeCollectionItemSnapshot]
    let nextCursor: String?
}

struct NativeCollectionItemSnapshot: Codable, Identifiable {
    let subjectId: Int64
    let title: String
    let originalTitle: String
    let posterUrl: String?
    let airDate: String?
    let score: Double
    let status: String
    let userRating: Int
    let comment: String
    let episodeProgress: Int
    let totalEpisodes: Int
    let updatedAt: String

    var id: Int64 { subjectId }
    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

struct NativeProfileRatingPageSnapshot: Codable {
    let items: [NativeProfileRatingSnapshot]
    let nextCursor: String?
}

struct NativeProfileRatingSnapshot: Codable, Identifiable {
    let id: String
    let subjectId: Int64
    let title: String
    let posterUrl: String?
    let score: Int
    let tags: [String]
    let visibility: String
    let updatedAt: String

    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

struct NativeAdminOverviewSnapshot: Codable {
    let role: String
    let usersTotal: Int64
    let usersActive: Int64
    let reviewsPublished: Int64
    let commentsPublished: Int64
    let openCommentReports: Int64
}

struct NativeAdminCommentSnapshot: Codable, Identifiable {
    let id: String
    let subjectId: Int64?
    let subjectTitle: String?
    let authorId: String
    let authorName: String
    let body: String
    let spoiler: Bool
    let moderationStatus: String
    let createdAt: String
    let editedAt: String?
}

struct NativeAdminReportSnapshot: Codable, Identifiable {
    let id: String
    let commentId: String
    let reporterId: String
    let reporterName: String
    let authorId: String
    let authorName: String
    let subjectId: Int64?
    let subjectTitle: String?
    let body: String
    let spoiler: Bool
    let moderationStatus: String
    let reasonCode: String
    let details: String?
    let status: String
    let assignedTo: String?
    let createdAt: String
    let resolvedAt: String?
}

struct NativeActivityPageSnapshot: Codable {
    let items: [NativeActivityItemSnapshot]
    let nextCursor: String?
}

struct NativeActivityItemSnapshot: Codable, Identifiable {
    let id: String
    let actorId: String?
    let actorName: String
    let actorAvatarUrl: String?
    let kind: String
    let subjectId: Int64?
    let subjectTitle: String?
    let posterUrl: String?
    let reviewId: String?
    let listId: String?
    let commentId: String?
    let summary: String
    let occurredAt: String
    let owned: Bool

    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

struct NativeNotificationSnapshot: Codable, Identifiable {
    let id: String
    let kind: String
    let actorId: String?
    let actorName: String?
    let subjectId: Int64?
    let commentId: String?
    let listId: String?
    let readAt: String?
    let createdAt: String
    let reviewId: String?
}

struct NativeProfileSnapshot: Codable {
    let userId: String
    let displayName: String
    let avatarUrl: String?
    let provider: String
    let reviewCount: Int
    let ratingCount: Int
    let listCount: Int
    let wishCount: Int
    let watchingCount: Int
    let completedCount: Int
    let onHoldCount: Int
    let droppedCount: Int
    let syncedAt: String?
    let role: String?

    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
}

struct NativeNetworkContextSnapshot: Codable {
    let countryCode: String?
    let region: String?
    let city: String?
    let displayLocation: String?
    let isDomestic: Bool
    let recommendedRoute: String

    enum CodingKeys: String, CodingKey {
        case countryCode = "country_code"
        case region
        case city
        case displayLocation = "display_location"
        case isDomestic = "is_domestic"
        case recommendedRoute = "recommended_route"
    }
}

struct NativeDiagnosticSnapshot: Codable, Identifiable {
    let endpoint: String
    let statusCode: Int?
    let healthy: Bool
    let body: String
    let latencyMs: Int64?
    let errorMessage: String?

    var id: String { endpoint }
}

struct NativeSubjectCommunitySnapshot: Codable {
    let rating: NativeRatingSnapshot
    let reviews: [NativeReviewSnapshot]
    let comments: [NativeCommentSnapshot]
    let personal: NativeSubjectPersonalStateSnapshot?

    init(
        rating: NativeRatingSnapshot,
        reviews: [NativeReviewSnapshot],
        comments: [NativeCommentSnapshot],
        personal: NativeSubjectPersonalStateSnapshot? = nil,
    ) {
        self.rating = rating
        self.reviews = reviews
        self.comments = comments
        self.personal = personal
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        rating = try container.decode(NativeRatingSnapshot.self, forKey: .rating)
        reviews = try container.decode([NativeReviewSnapshot].self, forKey: .reviews)
        comments = try container.decode([NativeCommentSnapshot].self, forKey: .comments)
        personal = try container.decodeIfPresent(NativeSubjectPersonalStateSnapshot.self, forKey: .personal)
    }
}

struct NativeSubjectPersonalStateSnapshot: Codable {
    let userRating: Int?
    let collectionStatus: String?
    let collectionEpisodeProgress: Int?
    let isCollected: Bool
}

struct NativeCalendarSnapshot: Codable {
    let date: String
    let items: [NativeSubjectSummary]
    let generatedAtEpochSeconds: Int64
}

struct NativeSubjectSectionsSnapshot: Codable {
    let episodes: [NativeEpisodeSnapshot]
    let characters: [NativeCharacterSnapshot]
    let persons: [NativePersonSnapshot]
    let relations: [NativeRelationSnapshot]
}

struct NativeEpisodeSnapshot: Codable, Identifiable {
    let id: Int64
    let number: Double?
    let title: String?
    let originalTitle: String?
    let type: String
    let airDate: String?
    let airStatus: String
}

struct NativeCharacterSnapshot: Codable, Identifiable {
    let id: Int64
    let name: String
    let imageUrl: String?
    let relation: String
    let actors: [NativePersonSnapshot]

    var imageURL: URL? { imageUrl.flatMap(URL.init(string:)) }
}

struct NativePersonSnapshot: Codable, Identifiable {
    let id: Int64
    let name: String
    let imageUrl: String?
    let role: String?

    var imageURL: URL? { imageUrl.flatMap(URL.init(string:)) }
}

struct NativeRelationSnapshot: Codable, Identifiable {
    let subject: NativeSubjectSummary
    let kind: String
    let label: String

    var id: Int64 { subject.id }
}

struct NativeRatingSnapshot: Codable {
    let score: Double?
    let votes: Int64
}

struct NativeReviewSnapshot: Codable, Identifiable {
    let id: String
    let subjectId: Int64
    let authorId: String
    let kind: String
    let title: String?
    let body: String
    let spoiler: Bool
    let likeCount: Int64
    let createdAt: String
    let owned: Bool
    let bookmarkCount: Int64
    let editedAt: String?
    let visibility: String
}

struct NativeCommentSnapshot: Codable, Identifiable {
    let id: String
    let parentId: String?
    let authorId: String
    let authorName: String
    let body: String
    let spoiler: Bool
    let createdAt: String
    let owned: Bool
    let likeCount: Int64
    let bookmarkCount: Int64
}

struct NativeReviewPageSnapshot: Codable {
    let items: [NativeReviewSnapshot]
    let nextCursor: String?
}

struct NativeUserProfileSnapshot: Codable {
    let id: String
    let displayName: String
    let avatarUrl: String?
    let createdAt: String
    let reviewCount: Int64
    let ratingCount: Int64
    let listCount: Int64
    let followerCount: Int64
    let followingCount: Int64
    let following: Bool

    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
}

struct NativeListSummarySnapshot: Codable, Identifiable {
    let id: String
    let ownerId: String?
    let ownerName: String
    let title: String
    let description: String
    let itemCount: Int64
    let followerCount: Int64
    let updatedAt: String
    let owned: Bool
    let following: Bool
}

struct NativeListItemSnapshot: Codable, Identifiable {
    let subjectId: Int64
    let title: String
    let posterUrl: String?
    let note: String?
    let position: Int
    let score: Double?

    var id: Int64 { subjectId }
    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

struct NativeListDetailSnapshot: Codable {
    let summary: NativeListSummarySnapshot
    let items: [NativeListItemSnapshot]
}

struct NativeFollowSnapshot: Codable {
    let following: Bool
}

struct NativeSyncStatusSnapshot: Codable {
    let pendingCount: Int64
    let failedCount: Int64
    let conflictCount: Int64
    let lastSuccessfulAt: String?
    let bangumiLinked: Bool
}

struct NativeSyncConflictSnapshot: Codable, Identifiable {
    let id: String
    let subjectId: Int64
    let localVersion: Int64
    let fieldName: String
    let localValue: String
    let remoteValue: String
    let detectedAt: String
}

struct NativeReactionSnapshot: Codable {
    let reaction: String
    let active: Bool
    let likeCount: Int64
    let bookmarkCount: Int64
}

struct NativeSessionSnapshot: Codable {
    let status: String
    let userId: String?
    let displayName: String?
    let avatarUrl: String?
    let expiresAtEpochSeconds: Int64?
    let message: String?

    init(
        status: String,
        userId: String? = nil,
        displayName: String? = nil,
        avatarUrl: String? = nil,
        expiresAtEpochSeconds: Int64? = nil,
        message: String? = nil,
    ) {
        self.status = status
        self.userId = userId
        self.displayName = displayName
        self.avatarUrl = avatarUrl
        self.expiresAtEpochSeconds = expiresAtEpochSeconds
        self.message = message
    }
}

struct NativeDiscoverySnapshot: Codable {
    let sections: [NativeDiscoverySection]
    let generatedAtEpochSeconds: Int64
}

struct NativeDiscoverySection: Codable, Identifiable {
    let id: String
    let title: String
    let subjects: [NativeSubjectSummary]
}

struct NativeSubjectSummary: Codable, Identifiable, Hashable {
    let id: Int64
    let title: String
    let originalTitle: String?
    let posterUrl: String?
    let year: Int?
    let type: String
    let airingStatus: String
    let rating: Double?
    let ratingVotes: Int
}

struct NativeSubjectDetailSnapshot: Codable {
    let summary: NativeSubjectSummary
    let summaryText: String?
    let airDate: String?
    let endDate: String?
    let totalEpisodes: Int?
    let tags: [String]
    let backdropUrl: String?
    let sourceUrl: String
    let dataUpdatedAtEpochSeconds: Int64
}
