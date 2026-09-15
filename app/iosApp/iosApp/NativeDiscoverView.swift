import Foundation
import SwiftUI
import AnimeShared

enum NativeLanguagePreference: String, CaseIterable, Identifiable {
    case system = "System"
    case simplifiedChinese = "SimplifiedChinese"
    case traditionalChinese = "TraditionalChinese"
    case english = "English"
    case japanese = "Japanese"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .system: return "跟随系统"
        case .simplifiedChinese: return "简体中文"
        case .traditionalChinese: return "繁体中文"
        case .english: return "English"
        case .japanese: return "日本語"
        }
    }

    var localeIdentifier: String {
        switch self {
        case .system:
            let identifier = Locale.preferredLanguages.first ?? Locale.current.identifier
            let normalized = identifier.lowercased()
            if normalized.hasPrefix("ja") { return "ja" }
            if normalized.hasPrefix("en") { return "en" }
            if normalized.hasPrefix("zh") {
                return normalized.contains("tw") || normalized.contains("hk") || normalized.contains("mo") || normalized.contains("hant")
                    ? "zh-Hant"
                    : "zh-Hans"
            }
            return "zh-Hans"
        case .simplifiedChinese: return "zh-Hans"
        case .traditionalChinese: return "zh-Hant"
        case .english: return "en"
        case .japanese: return "ja"
        }
    }

    var locale: Locale {
        Locale(identifier: localeIdentifier)
    }

    static func from(rawValue: String?) -> NativeLanguagePreference {
        guard let rawValue, let preference = NativeLanguagePreference(rawValue: rawValue) else { return .system }
        return preference
    }
}

@MainActor
final class NativeAppModel: ObservableObject {
    @Published private(set) var discovery: NativeDiscoverySnapshot?
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?
    @Published private(set) var session: NativeSessionSnapshot
    @Published private(set) var isSessionReady = false
    @Published private(set) var searchDiscovery: NativeSearchDiscoverySnapshot?
    @Published private(set) var collectionPage: NativeCollectionPageSnapshot?
    @Published private(set) var isLoadingCollection = false
    @Published private(set) var myRatingsPage: NativeProfileRatingPageSnapshot?
    @Published private(set) var adminOverview: NativeAdminOverviewSnapshot?
    @Published private(set) var adminComments: [NativeAdminCommentSnapshot] = []
    @Published private(set) var adminReports: [NativeAdminReportSnapshot] = []
    @Published private(set) var activityPage: NativeActivityPageSnapshot?
    @Published private(set) var notifications: [NativeNotificationSnapshot] = []
    @Published private(set) var profile: NativeProfileSnapshot?
    @Published private(set) var networkContext: NativeNetworkContextSnapshot?
    @Published private(set) var subjectCommunity: [Int64: NativeSubjectCommunitySnapshot] = [:]
    @Published private(set) var diagnostics: [NativeDiagnosticSnapshot] = []
    @Published private(set) var isLoadingDiagnostics = false
    @Published private(set) var calendar: NativeCalendarSnapshot?
    @Published private(set) var subjectSections: [Int64: NativeSubjectSectionsSnapshot] = [:]
    @Published private(set) var userProfiles: [String: NativeUserProfileSnapshot] = [:]
    @Published private(set) var userReviews: [String: NativeReviewPageSnapshot] = [:]
    @Published private(set) var userLists: [String: [NativeListSummarySnapshot]] = [:]
    @Published private(set) var lists: [NativeListSummarySnapshot] = []
    @Published private(set) var listDetails: [String: NativeListDetailSnapshot] = [:]
    @Published private(set) var syncStatus: NativeSyncStatusSnapshot?
    @Published private(set) var syncConflicts: [NativeSyncConflictSnapshot] = []
    @Published var pendingSubject: NativeSubjectSummary?
    @Published private(set) var appearanceTheme: String
    @Published private(set) var glassEnabled: Bool
    @Published private(set) var reduceMotionEnabled: Bool
    @Published private(set) var languagePreference: NativeLanguagePreference

    private var facade: IosNativeAppFacade?
    private var hasStarted = false
    private var hasAuthenticatedSession = false
    private var collectionRequestID = 0

    private static let sessionCacheKey = "anime.ios.session.snapshot"
    private static let profileCacheKey = "anime.ios.profile.snapshot"
    private static let ratingCachePrefix = "anime.ios.personal-rating"
    private static let appearanceThemeKey = "anime.ios.appearance.theme"
    private static let glassEnabledKey = "anime.ios.appearance.glass"
    private static let reduceMotionKey = "anime.ios.appearance.reduce-motion"
    // Keep the language key identical to the KMP SettingsStore so native iOS and
    // shared settings do not create competing preferences.
    private static let languageKey = "anime.settings.language"
    private static let calendarCachePrefix = "anime.ios.calendar."

    var isAuthenticated: Bool {
        isSessionReady && session.status == "authenticated"
    }

    var isRestoringSession: Bool {
        !isSessionReady || session.status == "restoring"
    }

    private func applySessionSnapshot(_ snapshot: NativeSessionSnapshot) {
        // A background restore may still finish after an interactive login. Once iOS has
        // received an authenticated session, never let that intermediate restoring state
        // replace the visible account screen and cause a flash.
        if snapshot.status == "restoring" {
            guard !hasAuthenticatedSession else { return }
            session = snapshot
            return
        }

        if snapshot.status == "failed", hasAuthenticatedSession {
            // A temporary network failure is not a logout. Keep the cached account
            // visible and let the next refresh retry instead of flashing to guest.
            isSessionReady = true
            return
        }

        if snapshot.status == "authenticated" {
            if session.userId != snapshot.userId {
                profile = Self.loadCachedProfile(for: snapshot.userId)
                collectionRequestID += 1
                collectionPage = nil
                myRatingsPage = nil
            }
            hasAuthenticatedSession = true
            session = snapshot
            isSessionReady = true
            Self.persist(snapshot)
            return
        }

        hasAuthenticatedSession = snapshot.status == "authenticated"
        session = snapshot
        isSessionReady = true
        if snapshot.status == "guest" || snapshot.status == "expired" {
            profile = nil
            collectionRequestID += 1
            collectionPage = nil
            myRatingsPage = nil
            isLoadingCollection = false
            Self.clearPersistedUserSnapshot()
        }
    }

    init() {
        let cachedSession = Self.loadCachedSession()
        session = cachedSession ?? NativeSessionSnapshot(status: "restoring")
        hasAuthenticatedSession = cachedSession != nil
        // A cached snapshot is only a rendering hint. Authenticated API requests must
        // wait until the KMP repository has validated or refreshed the Keychain token.
        isSessionReady = false
        profile = cachedSession.flatMap { Self.loadCachedProfile(for: $0.userId) }
        appearanceTheme = UserDefaults.standard.string(forKey: Self.appearanceThemeKey) ?? "system"
        glassEnabled = UserDefaults.standard.object(forKey: Self.glassEnabledKey) as? Bool ?? true
        reduceMotionEnabled = UserDefaults.standard.bool(forKey: Self.reduceMotionKey)
        languagePreference = NativeLanguagePreference.from(
            rawValue: UserDefaults.standard.string(forKey: Self.languageKey),
        )
        pendingSubject = nil
    }

    func setAppearanceTheme(_ value: String) {
        let normalized = ["system", "light", "dark"].contains(value) ? value : "system"
        appearanceTheme = normalized
        UserDefaults.standard.set(normalized, forKey: Self.appearanceThemeKey)
    }

    func setGlassEnabled(_ value: Bool) {
        glassEnabled = value
        UserDefaults.standard.set(value, forKey: Self.glassEnabledKey)
    }

    func setReduceMotionEnabled(_ value: Bool) {
        reduceMotionEnabled = value
        UserDefaults.standard.set(value, forKey: Self.reduceMotionKey)
    }

    func setLanguage(_ rawValue: String) {
        let preference = NativeLanguagePreference.from(rawValue: rawValue)
        languagePreference = preference
        UserDefaults.standard.set(preference.rawValue, forKey: Self.languageKey)
    }

    @discardableResult
    func handleExternalUrl(_ url: URL) -> Bool {
        if IosBridge.shared.handleOpenUrl(rawUrl: url.absoluteString) {
            return true
        }
        guard
            url.scheme?.lowercased() == "https",
            url.host?.lowercased() == "anime.jokersh.site"
        else { return false }
        let components = url.pathComponents
        guard
            components.count >= 3,
            components[1].lowercased() == "subjects",
            let subjectId = Int64(components[2]),
            subjectId > 0
        else { return false }
        pendingSubject = NativeSubjectSummary(
            id: subjectId,
            title: "作品详情",
            originalTitle: nil,
            posterUrl: nil,
            year: nil,
            type: "Other",
            airingStatus: "Unknown",
            rating: nil,
            ratingVotes: 0,
        )
        return true
    }

    func start() {
        guard !hasStarted else { return }
        hasStarted = true
        NSLog("[Anime iOS] native model startup begin")

        let facade = IosBridge.shared.nativeAppFacade(
            openExternalUrl: { rawUrl in
                IosAuthSessionCoordinator.shared.start(rawUrl: rawUrl)
            },
            readSecret: { account in
                IosKeychain.shared.read(account: account)
            },
            writeSecret: { account, value in
                IosKeychain.shared.write(account: account, value: value)
            },
            removeSecret: { account in
                IosKeychain.shared.remove(account: account)
            },
        )
        self.facade = facade
        NSLog("[Anime iOS] native facade created")
        facade.startSessionObservation { [weak self] rawSnapshot in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = Self.decode(rawSnapshot, as: NativeSessionSnapshot.self) ?? NativeSessionSnapshot(status: "failed")
                self.applySessionSnapshot(snapshot)
            }
        }
        NSLog("[Anime iOS] session observation installed")
        facade.start()
        NSLog("[Anime iOS] native facade start requested")
    }

    func refresh(force: Bool = false) {
        start()
        guard let facade else {
            errorMessage = "iOS 原生页面尚未准备完成"
            return
        }
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil
        facade.loadDiscovery(force: force) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.isLoading = false
                if let rawSnapshot, let snapshot = Self.decode(rawSnapshot, as: NativeDiscoverySnapshot.self) {
                    self.discovery = snapshot
                }
                self.errorMessage = error
            }
        }
    }

    func loadSubject(
        id: Int64,
        force: Bool = false,
        completion: @escaping (NativeSubjectDetailSnapshot?, String?) -> Void,
    ) {
        start()
        guard let facade else {
            completion(nil, "iOS 原生页面尚未准备完成")
            return
        }
        facade.loadSubject(subjectId: id, force: force) { rawSnapshot, error in
            Task { @MainActor in
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeSubjectDetailSnapshot.self) }
                completion(snapshot, error)
            }
        }
    }

    func loadCalendar(date: String, completion: ((NativeCalendarSnapshot?, String?) -> Void)? = nil) {
        start()
        if let cached = Self.loadCachedCalendar(for: date) {
            calendar = cached
            completion?(cached, nil)
        }
        facade?.loadCalendar(date: date) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeCalendarSnapshot.self) }
                if let snapshot {
                    self.calendar = snapshot
                    Self.persistCalendar(snapshot)
                }
                completion?(snapshot, error)
            }
        }
    }

    func loadSubjectSections(id: Int64, force: Bool = false, completion: ((NativeSubjectSectionsSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadSubjectSections(subjectId: id, force: force) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeSubjectSectionsSnapshot.self) }
                if let snapshot { self.subjectSections[id] = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func loadSearchDiscovery(completion: ((String?) -> Void)? = nil) {
        start()
        facade?.loadSearchDiscovery { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let rawSnapshot {
                    self.searchDiscovery = Self.decode(rawSnapshot, as: NativeSearchDiscoverySnapshot.self)
                }
                completion?(error)
            }
        }
    }

    func search(
        query: String,
        cursor: String? = nil,
        typesCsv: String? = nil,
        yearStart: Int? = nil,
        yearEnd: Int? = nil,
        airingCsv: String? = nil,
        sort: String = "relevance",
        completion: ((NativeSearchResultsSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        let filtersCsv = [
            typesCsv ?? "",
            yearStart.map(String.init) ?? "",
            yearEnd.map(String.init) ?? "",
            airingCsv ?? "",
            sort,
        ].joined(separator: "|")
        facade?.searchSubjectsFiltered(query: query, cursor: cursor, filtersCsv: filtersCsv) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeSearchResultsSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func saveSearchHistory(query: String) {
        start()
        facade?.saveSearchHistory(query: query)
    }

    func loadCollection(status: String?, cursor: String? = nil, append: Bool = false, completion: ((NativeCollectionPageSnapshot?, String?) -> Void)? = nil) {
        start()
        guard isSessionReady else {
            completion?(nil, nil)
            return
        }
        guard session.status == "authenticated" else {
            completion?(nil, "请先登录 Anime")
            return
        }
        guard let facade else {
            completion?(nil, "iOS 原生页面尚未准备完成")
            return
        }
        collectionRequestID += 1
        let requestID = collectionRequestID
        if !append {
            collectionPage = nil
        }
        isLoadingCollection = true
        facade.loadCollection(status: status, cursor: cursor) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                guard requestID == self.collectionRequestID else { return }
                self.isLoadingCollection = false
                let page = rawSnapshot.flatMap { Self.decode($0, as: NativeCollectionPageSnapshot.self) }
                let resolvedError =
                    error ??
                    (rawSnapshot != nil && page == nil ? "片库数据格式异常，请重试" : nil)
                let snapshot: NativeCollectionPageSnapshot?
                if append, let page, let current = self.collectionPage {
                    snapshot = NativeCollectionPageSnapshot(items: current.items + page.items, nextCursor: page.nextCursor)
                } else {
                    snapshot = page
                }
                if let snapshot { self.collectionPage = snapshot }
                completion?(snapshot, resolvedError)
            }
        }
    }

    func loadActivity(
        feed: String,
        cursor: String? = nil,
        append: Bool = false,
        completion: ((NativeActivityPageSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        facade?.loadActivity(feed: feed, cursor: cursor) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let page = rawSnapshot.flatMap { Self.decode($0, as: NativeActivityPageSnapshot.self) }
                let snapshot: NativeActivityPageSnapshot?
                if append, let page, let current = self.activityPage {
                    snapshot = NativeActivityPageSnapshot(items: current.items + page.items, nextCursor: page.nextCursor)
                } else {
                    snapshot = page
                }
                if let snapshot { self.activityPage = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func loadNotifications(completion: (([NativeNotificationSnapshot]?, String?) -> Void)? = nil) {
        start()
        facade?.loadNotifications { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: [NativeNotificationSnapshot].self) }
                self.notifications = snapshot ?? []
                completion?(snapshot, error)
            }
        }
    }

    func markNotificationRead(id: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.markNotificationRead(id: id) { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil { self.loadNotifications() }
                completion?(error)
            }
        }
    }

    func loadProfile(completion: ((NativeProfileSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadProfile { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeProfileSnapshot.self) }
                if let snapshot {
                    self.profile = snapshot
                    Self.persist(snapshot)
                }
                completion?(self.profile, error)
            }
        }
    }

    func loadMyRatings(cursor: String? = nil, append: Bool = false, completion: ((NativeProfileRatingPageSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadMyRatings(cursor: cursor) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let page = rawSnapshot.flatMap { Self.decode($0, as: NativeProfileRatingPageSnapshot.self) }
                if let page {
                    if append, let current = self.myRatingsPage {
                        self.myRatingsPage = NativeProfileRatingPageSnapshot(items: current.items + page.items, nextCursor: page.nextCursor)
                    } else {
                        self.myRatingsPage = page
                    }
                }
                completion?(self.myRatingsPage, error)
            }
        }
    }

    func loadAdminOverview(completion: ((NativeAdminOverviewSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadAdminOverview { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeAdminOverviewSnapshot.self) }
                if let snapshot { self.adminOverview = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func loadAdminComments(status: String? = nil, completion: (([NativeAdminCommentSnapshot], String?) -> Void)? = nil) {
        start()
        facade?.loadAdminComments(status: status) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let comments = rawSnapshot.flatMap { Self.decode($0, as: [NativeAdminCommentSnapshot].self) } ?? []
                if error == nil { self.adminComments = comments }
                completion?(self.adminComments, error)
            }
        }
    }

    func loadAdminReports(status: String? = "open", completion: (([NativeAdminReportSnapshot], String?) -> Void)? = nil) {
        start()
        facade?.loadAdminReports(status: status) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let reports = rawSnapshot.flatMap { Self.decode($0, as: [NativeAdminReportSnapshot].self) } ?? []
                if error == nil { self.adminReports = reports }
                completion?(self.adminReports, error)
            }
        }
    }

    func adminReportAction(id: String, action: String, contentAction: String? = nil, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.adminReportAction(id: id, action: action, reason: nil, contentAction: contentAction) { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil {
                    self.adminReports.removeAll { $0.id == id }
                    self.loadAdminOverview()
                }
                completion?(error)
            }
        }
    }

    func moderateComment(id: String, action: String, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.moderateComment(id: id, action: action, reason: nil) { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil { self.adminComments.removeAll { $0.id == id } }
                completion?(error)
            }
        }
    }

    func withdrawActivity(id: String, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.withdrawActivity(id: id) { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil { self.activityPage = nil }
                completion?(error)
            }
        }
    }

    func loadNetworkContext(completion: ((NativeNetworkContextSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadNetworkContext { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeNetworkContextSnapshot.self) }
                if let snapshot { self.networkContext = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func loadSubjectCommunity(subjectId: Int64, completion: ((NativeSubjectCommunitySnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadSubjectCommunity(subjectId: subjectId) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeSubjectCommunitySnapshot.self) }
                if let snapshot { self.subjectCommunity[subjectId] = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func loadReview(id: String, completion: ((NativeReviewSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadReview(id: id) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                completion?(rawSnapshot.flatMap { Self.decode($0, as: NativeReviewSnapshot.self) }, error)
            }
        }
    }

    func updateReview(id: String, title: String?, body: String?, spoiler: Bool, visibility: String, completion: ((NativeReviewSnapshot?, String?) -> Void)? = nil) {
        start()
        if rejectValueWrite(completion) { return }
        facade?.updateReview(id: id, title: title, body: body, spoiler: spoiler, visibility: visibility) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                completion?(rawSnapshot.flatMap { Self.decode($0, as: NativeReviewSnapshot.self) }, error)
            }
        }
    }

    func deleteReview(id: String, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.deleteReview(id: id) { error in Task { @MainActor in completion?(error) } }
    }

    func updateComment(id: String, body: String, spoiler: Bool, completion: ((NativeCommentSnapshot?, String?) -> Void)? = nil) {
        start()
        if rejectValueWrite(completion) { return }
        facade?.updateComment(id: id, body: body, spoiler: spoiler) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                completion?(rawSnapshot.flatMap { Self.decode($0, as: NativeCommentSnapshot.self) }, error)
            }
        }
    }

    func deleteComment(id: String, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.deleteComment(id: id) { error in Task { @MainActor in completion?(error) } }
    }

    func deleteRating(subjectId: Int64, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.deleteRating(subjectId: subjectId) { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil {
                    self.myRatingsPage = nil
                    self.loadProfile()
                }
                completion?(error)
            }
        }
    }

    func reportComment(id: String, reasonCode: String = "other", details: String? = nil, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.reportComment(id: id, reasonCode: reasonCode, details: details) { error in Task { @MainActor in completion?(error) } }
    }

    func loadUserProfile(id: String, completion: ((NativeUserProfileSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadUserProfile(id: id) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeUserProfileSnapshot.self) }
                if let snapshot { self.userProfiles[id] = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func loadUserReviews(id: String, cursor: String? = nil, oldestFirst: Bool = false, completion: ((NativeReviewPageSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadUserReviews(id: id, cursor: cursor, oldestFirst: oldestFirst) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeReviewPageSnapshot.self) }
                if let snapshot { self.userReviews[id] = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func loadUserLists(id: String, completion: (([NativeListSummarySnapshot]?, String?) -> Void)? = nil) {
        start()
        facade?.loadUserLists(id: id) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: [NativeListSummarySnapshot].self) }
                if let snapshot { self.userLists[id] = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func loadLists(completion: (([NativeListSummarySnapshot]?, String?) -> Void)? = nil) {
        start()
        facade?.loadLists { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: [NativeListSummarySnapshot].self) }
                if let snapshot { self.lists = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func loadList(id: String, completion: ((NativeListDetailSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadList(id: id) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeListDetailSnapshot.self) }
                if let snapshot { self.listDetails[id] = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func createList(title: String, description: String, visibility: String, subjectIds: [Int64], completion: ((NativeListSummarySnapshot?, String?) -> Void)? = nil) {
        start()
        let subjectIdsCsv = subjectIds.map(String.init).joined(separator: ",")
        facade?.createList(title: title, description: description, visibility: visibility, subjectIdsCsv: subjectIdsCsv) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                completion?(rawSnapshot.flatMap { Self.decode($0, as: NativeListSummarySnapshot.self) }, error)
            }
        }
    }

    func updateList(id: String, title: String, description: String, visibility: String, subjectIds: [Int64], completion: ((NativeListSummarySnapshot?, String?) -> Void)? = nil) {
        start()
        let subjectIdsCsv = subjectIds.map(String.init).joined(separator: ",")
        facade?.updateList(id: id, title: title, description: description, visibility: visibility, subjectIdsCsv: subjectIdsCsv) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                completion?(rawSnapshot.flatMap { Self.decode($0, as: NativeListSummarySnapshot.self) }, error)
            }
        }
    }

    func deleteList(id: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.deleteList(id: id) { error in Task { @MainActor in completion?(error) } }
    }

    func followUser(id: String, following: Bool, completion: ((Bool?, String?) -> Void)? = nil) {
        start()
        facade?.followUser(id: id, following: following) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeFollowSnapshot.self) }
                completion?(snapshot?.following, error)
            }
        }
    }

    func followList(id: String, following: Bool, completion: ((Bool?, String?) -> Void)? = nil) {
        start()
        facade?.followList(id: id, following: following) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeFollowSnapshot.self) }
                completion?(snapshot?.following, error)
            }
        }
    }

    func saveRating(subjectId: Int64, score: Int, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.saveRating(subjectId: subjectId, score: Int32(score)) { error in
            Task { @MainActor [weak self] in
                if error == nil { self?.persistUserRating(score, subjectId: subjectId) }
                completion?(error)
            }
        }
    }

    func setCollection(subjectId: Int64, status: String?, episodeProgress: Int? = nil, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.setCollectionWithProgress(
            subjectId: subjectId,
            status: status,
            episodeProgress: Int32(episodeProgress ?? -1),
        ) { error in
            Task { @MainActor in completion?(error) }
        }
    }

    func createComment(subjectId: Int64, body: String, completion: ((NativeCommentSnapshot?, String?) -> Void)? = nil) {
        start()
        if rejectValueWrite(completion) { return }
        facade?.createComment(subjectId: subjectId, body: body) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeCommentSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func createCommentAdvanced(subjectId: Int64, body: String, spoiler: Bool, parentId: String?, completion: ((NativeCommentSnapshot?, String?) -> Void)? = nil) {
        start()
        if rejectValueWrite(completion) { return }
        facade?.createCommentAdvanced(subjectId: subjectId, body: body, spoiler: spoiler, parentId: parentId) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                completion?(rawSnapshot.flatMap { Self.decode($0, as: NativeCommentSnapshot.self) }, error)
            }
        }
    }

    func createReview(
        subjectId: Int64,
        title: String?,
        body: String,
        completion: ((NativeReviewSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        if rejectValueWrite(completion) { return }
        facade?.createReview(subjectId: subjectId, title: title, body: body) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeReviewSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func createReviewAdvanced(subjectId: Int64, kind: String, title: String?, body: String, spoiler: Bool, visibility: String, completion: ((NativeReviewSnapshot?, String?) -> Void)? = nil) {
        start()
        if rejectValueWrite(completion) { return }
        // R1 deliberately has one review shape. Keep legacy callers from
        // accidentally re-introducing the long-review/title contract.
        facade?.createReviewAdvanced(subjectId: subjectId, kind: "short", title: nil, body: body, spoiler: spoiler, visibility: visibility) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                completion?(rawSnapshot.flatMap { Self.decode($0, as: NativeReviewSnapshot.self) }, error)
            }
        }
    }

    func reactComment(
        id: String,
        reaction: String,
        active: Bool,
        completion: ((NativeReactionSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        if rejectValueWrite(completion) { return }
        facade?.reactComment(id: id, reaction: reaction, active: active) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeReactionSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func reactReview(
        id: String,
        reaction: String,
        active: Bool,
        completion: ((NativeReactionSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        if rejectValueWrite(completion) { return }
        facade?.reactReview(id: id, reaction: reaction, active: active) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeReactionSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func beginBangumiLogin(completion: ((String?) -> Void)? = nil) {
        start()
        facade?.beginBangumiLogin { _, error in
            Task { @MainActor in completion?(error) }
        }
    }

    func loginWithAnime(username: String, password: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.loginWithAnime(username: username, password: password) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let rawSnapshot {
                    let snapshot = Self.decode(rawSnapshot, as: NativeSessionSnapshot.self) ?? self.session
                    self.applySessionSnapshot(snapshot)
                }
                completion?(error)
            }
        }
    }

    func registerAnime(username: String, password: String, displayName: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.registerAnime(username: username, password: password, displayName: displayName) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let rawSnapshot {
                    let snapshot = Self.decode(rawSnapshot, as: NativeSessionSnapshot.self) ?? self.session
                    self.applySessionSnapshot(snapshot)
                }
                completion?(error)
            }
        }
    }

    func updateProfile(displayName: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.updateProfile(displayName: displayName) { error in
            Task { @MainActor in completion?(error) }
        }
    }

    func uploadAvatar(base64: String, contentType: String, completion: ((String?) -> Void)? = nil) {
        start()
        if rejectSimpleWrite(completion) { return }
        facade?.uploadAvatar(base64: base64, contentType: contentType) { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil { self.loadProfile() }
                completion?(error)
            }
        }
    }

    func changePassword(currentPassword: String, newPassword: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.changePassword(currentPassword: currentPassword, newPassword: newPassword) { error in
            Task { @MainActor in completion?(error) }
        }
    }

    func exportMyData(completion: ((String?, String?) -> Void)? = nil) {
        start()
        facade?.exportMyData { rawData, error in
            Task { @MainActor in completion?(rawData, error) }
        }
    }

    func deleteAccount(completion: ((String?) -> Void)? = nil) {
        start()
        facade?.deleteAccount { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil {
                    self.profile = nil
                    self.collectionRequestID += 1
                    self.collectionPage = nil
                    self.myRatingsPage = nil
                    self.isLoadingCollection = false
                    Self.clearPersistedUserSnapshot()
                }
                completion?(error)
            }
        }
    }

    func loadSyncStatus(completion: ((NativeSyncStatusSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadSyncStatus { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeSyncStatusSnapshot.self) }
                if let snapshot { self.syncStatus = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func startSync(completion: ((String?) -> Void)? = nil) {
        start()
        facade?.startSync { error in Task { @MainActor in completion?(error) } }
    }

    func loadSyncConflicts(completion: (([NativeSyncConflictSnapshot]?, String?) -> Void)? = nil) {
        start()
        facade?.loadSyncConflicts { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: [NativeSyncConflictSnapshot].self) }
                self.syncConflicts = snapshot ?? []
                completion?(snapshot, error)
            }
        }
    }

    func resolveSyncConflict(id: String, expectedVersion: Int64, choice: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.resolveSyncConflict(id: id, expectedVersion: expectedVersion, choice: choice) { error in
            Task { @MainActor in completion?(error) }
        }
    }

    func logout(completion: ((String?) -> Void)? = nil) {
        start()
        facade?.logout { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil {
                    self.profile = nil
                    self.collectionRequestID += 1
                    self.collectionPage = nil
                    self.isLoadingCollection = false
                    Self.clearPersistedUserSnapshot()
                }
                completion?(error)
            }
        }
    }

    func loadDiagnostics(completion: (([NativeDiagnosticSnapshot]?, String?) -> Void)? = nil) {
        start()
        isLoadingDiagnostics = true
        facade?.diagnostics { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: [NativeDiagnosticSnapshot].self) }
                self.diagnostics = snapshot ?? []
                self.isLoadingDiagnostics = false
                completion?(snapshot, error)
            }
        }
    }

    fileprivate static func decode<T: Decodable>(_ raw: String, as type: T.Type) -> T? {
        guard let data = raw.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }

    func cachedUserRating(subjectId: Int64) -> Int? {
        guard let userId = session.userId else { return nil }
        let value = UserDefaults.standard.integer(forKey: Self.ratingCacheKey(userId: userId, subjectId: subjectId))
        return (1...10).contains(value) ? value : nil
    }

    private func persistUserRating(_ score: Int, subjectId: Int64) {
        guard let userId = session.userId, (1...10).contains(score) else { return }
        UserDefaults.standard.set(score, forKey: Self.ratingCacheKey(userId: userId, subjectId: subjectId))
    }

    private static func ratingCacheKey(userId: String, subjectId: Int64) -> String {
        "\(ratingCachePrefix).\(userId).\(subjectId)"
    }

    private func writeErrorMessage() -> String? {
        if !isSessionReady { return "正在恢复登录状态，请稍候" }
        if session.status != "authenticated" { return "请先登录 Anime" }
        return nil
    }

    @discardableResult
    private func rejectSimpleWrite(_ completion: ((String?) -> Void)?) -> Bool {
        guard let error = writeErrorMessage() else { return false }
        completion?(error)
        return true
    }

    @discardableResult
    private func rejectValueWrite<Value>(_ completion: ((Value?, String?) -> Void)?) -> Bool {
        guard let error = writeErrorMessage() else { return false }
        completion?(nil, error)
        return true
    }

    private static func loadCachedSession() -> NativeSessionSnapshot? {
        guard
            let data = UserDefaults.standard.data(forKey: sessionCacheKey),
            let snapshot = try? JSONDecoder().decode(NativeSessionSnapshot.self, from: data),
            snapshot.status == "authenticated",
            let userId = snapshot.userId,
            !userId.isEmpty
        else { return nil }
        return snapshot
    }

    private static func loadCachedProfile(for userId: String?) -> NativeProfileSnapshot? {
        guard
            let userId,
            let data = UserDefaults.standard.data(forKey: profileCacheKey),
            let snapshot = try? JSONDecoder().decode(NativeProfileSnapshot.self, from: data),
            snapshot.userId == userId
        else { return nil }
        return snapshot
    }

    private static func persist(_ snapshot: NativeSessionSnapshot) {
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        UserDefaults.standard.set(data, forKey: sessionCacheKey)
    }

    private static func persist(_ snapshot: NativeProfileSnapshot) {
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        UserDefaults.standard.set(data, forKey: profileCacheKey)
    }

    private static func calendarCacheKey(for date: String) -> String {
        "\(calendarCachePrefix)\(date)"
    }

    private static func loadCachedCalendar(for date: String) -> NativeCalendarSnapshot? {
        guard
            let data = UserDefaults.standard.data(forKey: calendarCacheKey(for: date)),
            let snapshot = try? JSONDecoder().decode(NativeCalendarSnapshot.self, from: data),
            snapshot.date == date
        else { return nil }
        return snapshot
    }

    private static func persistCalendar(_ snapshot: NativeCalendarSnapshot) {
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        UserDefaults.standard.set(data, forKey: calendarCacheKey(for: snapshot.date))
    }

    private static func clearPersistedUserSnapshot() {
        UserDefaults.standard.removeObject(forKey: sessionCacheKey)
        UserDefaults.standard.removeObject(forKey: profileCacheKey)
    }
}

struct NativeDiscoverView: View {
    @ObservedObject var model: NativeAppModel
    @State private var discoverTitleOpacity = 1.0

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 28) {
                    if model.isLoading && model.discovery == nil {
                        ProgressView()
                            .frame(maxWidth: .infinity, minHeight: 240)
                    } else if let discovery = model.discovery {
                        let heroSubjects = carouselSubjects(from: discovery)
                        if !heroSubjects.isEmpty {
                            NativeDiscoveryCarousel(subjects: heroSubjects)
                        }

                        ForEach(discovery.sections) { section in
                            NativeDiscoverySectionView(section: section, model: model)
                        }
                    } else {
                        NativeEmptyState(
                            title: "暂时没有内容",
                            message: model.errorMessage ?? "下拉重新加载发现内容。",
                            actionTitle: "重试",
                            action: { model.refresh(force: true) },
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .refreshable {
                model.refresh(force: true)
            }
            .onScrollGeometryChange(for: CGFloat.self) { geometry in
                geometry.contentOffset.y + geometry.contentInsets.top
            } action: { _, offset in
                let fadeDistance: CGFloat = 56
                let nextOpacity = 1 - min(max(offset / fadeDistance, 0), 1)
                if abs(nextOpacity - discoverTitleOpacity) > 0.01 {
                    discoverTitleOpacity = nextOpacity
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Text("发现")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .fixedSize(horizontal: true, vertical: false)
                        .layoutPriority(1)
                        .opacity(discoverTitleOpacity)
                        .accessibilityAddTraits(.isHeader)
                }
                .sharedBackgroundVisibility(.hidden)

                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeCalendarView(model: model)
                    } label: {
                        Image(systemName: "calendar")
                    }
                    .accessibilityLabel("播出日历")
                }
            }
            .navigationDestination(for: NativeSubjectSummary.self) { subject in
                NativeSubjectDetailView(summary: subject, model: model)
            }
        }
        .task {
            model.start()
            if model.discovery == nil {
                model.refresh()
            }
        }
    }

    private func carouselSubjects(from discovery: NativeDiscoverySnapshot) -> [NativeSubjectSummary] {
        var seen = Set<Int64>()
        return Array(
            discovery.sections
                .flatMap(\.subjects)
                .filter { seen.insert($0.id).inserted }
                .prefix(5)
        )
    }
}

private struct NativeDiscoveryCarousel: View {
    let subjects: [NativeSubjectSummary]

    var body: some View {
        TabView {
            ForEach(subjects) { subject in
                NavigationLink(value: subject) {
                    NativeHeroCard(subject: subject)
                }
                .buttonStyle(.plain)
            }
        }
        .frame(height: 250)
        .tabViewStyle(.page(indexDisplayMode: .automatic))
        .indexViewStyle(.page(backgroundDisplayMode: .interactive))
    }
}

private struct NativeDiscoverySectionView: View {
    let section: NativeDiscoverySection
    @ObservedObject var model: NativeAppModel

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(section.title)
                        .font(.title2.weight(.bold))
                    Text(description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                NavigationLink("查看全部") {
                    NativeDiscoverySectionListView(section: section, model: model)
                }
                .font(.subheadline.weight(.semibold))
            }

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 12) {
                    ForEach(section.subjects) { subject in
                        NavigationLink(value: subject) {
                            NativeSubjectCard(subject: subject)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
        }
    }

    private var description: String {
        switch section.id {
        case "continue": return "从上次停下的地方继续"
        case "airing": return "这一刻，大家正在追"
        case "top-rated": return "来自 Bangumi 的高口碑作品"
        case "upcoming": return "值得提前留意的新作"
        default: return "为你整理的作品"
        }
    }
}

private struct NativeDiscoverySectionListView: View {
    let section: NativeDiscoverySection
    @ObservedObject var model: NativeAppModel

    var body: some View {
        ScrollView {
            LazyVGrid(columns: posterGridColumns, spacing: 20) {
                ForEach(section.subjects) { subject in
                    NavigationLink(value: subject) {
                        NativeSubjectCard(subject: subject, fixedWidth: nil)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(section.title)
        .navigationBarTitleDisplayMode(.large)
        .navigationDestination(for: NativeSubjectSummary.self) { subject in
            NativeSubjectDetailView(summary: subject, model: model)
        }
    }

    private let posterGridColumns = [GridItem(.flexible()), GridItem(.flexible())]
}

private struct NativeHeroCard: View {
    let subject: NativeSubjectSummary

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            NativeRemoteImage(url: subject.posterURL, contentMode: .fill)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .clipped()
            LinearGradient(
                colors: [.clear, .black.opacity(0.82)],
                startPoint: .center,
                endPoint: .bottom,
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            VStack(alignment: .leading, spacing: 8) {
                Text("本季口碑新作")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.82))
                Text(subject.title)
                    .font(.title.weight(.bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                if let rating = subject.rating {
                    Label(String(format: "%.1f  Bangumi", rating), systemImage: "star.fill")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.white)
                }
            }
            .padding(20)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 250)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}

private struct NativeSubjectCard: View {
    let subject: NativeSubjectSummary
    let fixedWidth: CGFloat?

    init(subject: NativeSubjectSummary, fixedWidth: CGFloat? = 156) {
        self.subject = subject
        self.fixedWidth = fixedWidth
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let fixedWidth {
                NativePosterImage(
                    url: subject.posterURL,
                    width: fixedWidth,
                    height: fixedWidth * 4 / 3,
                    cornerRadius: 16,
                )
            } else {
                NativePosterImage(
                    url: subject.posterURL,
                    aspectRatio: 3 / 4,
                    cornerRadius: 16,
                )
            }

            Text(subject.title)
                .font(.headline)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            HStack(spacing: 5) {
                if let rating = subject.rating {
                    Image(systemName: "star.fill")
                        .foregroundStyle(.orange)
                    Text(String(format: "%.1f", rating))
                } else {
                    Text("暂无评分")
                }
            }
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
        }
        .frame(width: fixedWidth, alignment: .leading)
        .frame(maxWidth: fixedWidth == nil ? .infinity : nil, alignment: .leading)
    }
}

struct NativeSubjectDetailView: View {
    let summary: NativeSubjectSummary
    @ObservedObject var model: NativeAppModel
    let focusCommentId: String?
    @State private var detail: NativeSubjectDetailSnapshot?
    @State private var community: NativeSubjectCommunitySnapshot?
    @State private var errorMessage: String?
    @State private var communityError: String?
    @State private var isLoading = true

    init(
        summary: NativeSubjectSummary,
        model: NativeAppModel,
        focusCommentId: String? = nil,
    ) {
        self.summary = summary
        self.model = model
        self.focusCommentId = focusCommentId
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 22) {
                    NativeDetailHero(summary: detail?.summary ?? summary)

                    if isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    }

                    if let detail {
                        detailBody(detail)
                    } else if let errorMessage {
                        NativeEmptyState(
                            title: "详情加载失败",
                            message: errorMessage,
                            actionTitle: "重试",
                            action: { load(force: true) },
                        )
                    }
                    NativeSubjectCommunityView(
                        subjectId: summary.id,
                        community: community,
                        errorMessage: communityError,
                        totalEpisodes: detail?.totalEpisodes,
                        model: model,
                        onReload: loadCommunity,
                    )
                }
                .padding(16)
                .padding(.bottom, 32)
            }
            .onChange(of: community?.comments.map(\.id) ?? []) { _, _ in
                scrollToFocusedComment(using: proxy)
            }
            .task {
                load()
                loadCommunity()
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .navigationTitle(summary.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeSubjectSectionsView(subjectId: summary.id, model: model)
                    } label: {
                        Image(systemName: "list.bullet.rectangle")
                    }
                    .accessibilityLabel("作品资料")
                }
            }
        }
    }

    private func scrollToFocusedComment(using proxy: ScrollViewProxy) {
        guard let focusCommentId else { return }
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(120))
            withAnimation(.easeInOut(duration: 0.35)) {
                proxy.scrollTo("comment-\(focusCommentId)", anchor: .center)
            }
        }
    }

    @ViewBuilder
    private func detailBody(_ detail: NativeSubjectDetailSnapshot) -> some View {
        if let summaryText = detail.summaryText, !summaryText.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                Text("简介")
                    .font(.title3.weight(.bold))
                Text(summaryText)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }

        if !detail.tags.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 8) {
                    ForEach(detail.tags, id: \.self) { tag in
                        Text(tag)
                            .font(.caption.weight(.medium))
                            .padding(.horizontal, 11)
                            .padding(.vertical, 7)
                            .background(.thinMaterial, in: Capsule())
                    }
                }
            }
        }

        if let url = URL(string: detail.sourceUrl) {
            Link(destination: url) {
                Label("查看来源", systemImage: "arrow.up.right.square")
                    .font(.subheadline.weight(.semibold))
            }
        }
    }

    private func load(force: Bool = false) {
        isLoading = true
        errorMessage = nil
        model.loadSubject(id: summary.id, force: force) { value, error in
            detail = value
            errorMessage = error
            isLoading = false
        }
    }

    private func loadCommunity() {
        communityError = nil
        if let cached = model.subjectCommunity[summary.id] {
            community = cached
        }
        model.loadSubjectCommunity(subjectId: summary.id) { value, error in
            community = value
            communityError = error
        }
    }
}

private struct NativeDetailHero: View {
    let summary: NativeSubjectSummary

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            NativePosterImage(url: summary.posterURL, width: 132, height: 176, cornerRadius: 18)

            VStack(alignment: .leading, spacing: 10) {
                Text(summary.title)
                    .font(.title2.weight(.bold))
                if let originalTitle = summary.originalTitle, !originalTitle.isEmpty {
                    Text(originalTitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                if let rating = summary.rating {
                    Label(String(format: "%.1f", rating), systemImage: "star.fill")
                        .font(.headline.weight(.semibold))
                        .foregroundStyle(.orange)
                }
                Text(metadata)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var metadata: String {
        [summary.year.map(String.init), summary.type.displayName, summary.airingStatus.displayName]
            .compactMap { $0 }
            .joined(separator: " · ")
    }
}

private struct NativeEmptyState: View {
    let title: String
    let message: String
    let actionTitle: String
    let action: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: "sparkles")
        } description: {
            Text(message)
        } actions: {
            Button(actionTitle, action: action)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, minHeight: 220)
    }
}

private extension NativeSubjectSummary {
    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

private extension String {
    var displayName: String {
        switch self {
        case "Tv": return "TV"
        case "Web": return "Web"
        case "Ova": return "OVA"
        case "Movie": return "剧场版"
        case "Airing": return "连载中"
        case "Finished": return "已完结"
        case "Announced": return "未开播"
        default: return self
        }
    }
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
