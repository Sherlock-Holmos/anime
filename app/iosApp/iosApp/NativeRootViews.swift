import SwiftUI
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
    let summary: String
    let occurredAt: String

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

    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
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

struct NativeLibraryView: View {
    @ObservedObject var model: NativeAppModel
    @State private var libraryTitleOpacity = 1.0
    @State private var query = ""
    @State private var results: [NativeSubjectSummary] = []
    @State private var nextCursor: String?
    @State private var isSearching = false
    @State private var errorMessage: String?
    @State private var selectedType = "all"
    @State private var selectedAiring = "all"
    @State private var selectedSort = "relevance"
    @State private var yearStartText = ""
    @State private var yearEndText = ""
    @State private var showingFilters = false

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 24) {
                    if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        recommendationContent
                    } else if isSearching && results.isEmpty {
                        ProgressView("正在搜索")
                            .frame(maxWidth: .infinity, minHeight: 180)
                    } else if results.isEmpty {
                        ContentUnavailableView.search(text: query)
                            .frame(maxWidth: .infinity, minHeight: 220)
                    } else {
                        resultGrid
                    }
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .onScrollGeometryChange(for: CGFloat.self) { geometry in
                geometry.contentOffset.y + geometry.contentInsets.top
            } action: { _, offset in
                let fadeDistance: CGFloat = 56
                let nextOpacity = 1 - min(max(offset / fadeDistance, 0), 1)
                if abs(nextOpacity - libraryTitleOpacity) > 0.01 {
                    libraryTitleOpacity = nextOpacity
                }
            }
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "搜索作品")
            .onSubmit(of: .search, submitSearch)
            .refreshable { await refreshRecommendations() }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Text("资料库")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .fixedSize(horizontal: true, vertical: false)
                        .layoutPriority(1)
                        .opacity(libraryTitleOpacity)
                        .accessibilityAddTraits(.isHeader)
                }
                .sharedBackgroundVisibility(.hidden)

                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showingFilters = true
                    } label: {
                        Image(systemName: hasActiveFilters ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                    }
                    .accessibilityLabel("搜索筛选")
                }
            }
            .navigationDestination(for: NativeSubjectSummary.self) { subject in
                NativeSubjectDetailView(summary: subject, model: model)
            }
        }
        .task {
            model.start()
            if model.searchDiscovery == nil { model.loadSearchDiscovery() }
        }
        .sheet(isPresented: $showingFilters) {
            NativeSearchFiltersSheet(
                selectedType: $selectedType,
                selectedAiring: $selectedAiring,
                selectedSort: $selectedSort,
                yearStartText: $yearStartText,
                yearEndText: $yearEndText,
            ) {
                if !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    submitSearch()
                }
            }
            .presentationDetents([.medium, .large])
        }
    }

    @ViewBuilder
    private var recommendationContent: some View {
        if let discovery = model.searchDiscovery {
            if !discovery.trending.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text("大家正在搜")
                        .font(.title3.weight(.bold))
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(discovery.trending, id: \.self) { term in
                                Button(term) {
                                    query = term
                                    submitSearch()
                                }
                                .buttonStyle(.bordered)
                                .tint(.accentColor)
                            }
                        }
                    }
                }
            }
            if !discovery.recommendations.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text(discovery.personalized ? "为你推荐" : "精选作品")
                        .font(.title3.weight(.bold))
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 145), spacing: 12)], spacing: 18) {
                        ForEach(discovery.recommendations) { subject in
                            NavigationLink(value: subject) {
                                NativeCatalogCard(subject: subject)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        } else {
            ProgressView()
                .frame(maxWidth: .infinity, minHeight: 180)
        }
    }

    private var resultGrid: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text("搜索结果")
                    .font(.title3.weight(.bold))
                Spacer()
                if hasActiveFilters {
                    Text(activeFilterSummary)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                }
            }
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 145), spacing: 12)], spacing: 18) {
                ForEach(results) { subject in
                    NavigationLink(value: subject) {
                        NativeCatalogCard(subject: subject)
                    }
                    .buttonStyle(.plain)
                }
            }
            if let nextCursor {
                Button("加载更多") { loadMore(cursor: nextCursor) }
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.bordered)
            }
        }
    }

    private func submitSearch() {
        let normalized = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { return }
        isSearching = true
        errorMessage = nil
        model.search(
            query: normalized,
            typesCsv: typeQueryValue,
            yearStart: Int(yearStartText),
            yearEnd: Int(yearEndText),
            airingCsv: airingQueryValue,
            sort: selectedSort,
        ) { snapshot, error in
            results = snapshot?.items ?? []
            nextCursor = snapshot?.nextCursor
            errorMessage = error
            isSearching = false
            if error == nil { model.saveSearchHistory(query: normalized) }
        }
    }

    private func loadMore(cursor: String) {
        let normalized = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { return }
        isSearching = true
        model.search(
            query: normalized,
            cursor: cursor,
            typesCsv: typeQueryValue,
            yearStart: Int(yearStartText),
            yearEnd: Int(yearEndText),
            airingCsv: airingQueryValue,
            sort: selectedSort,
        ) { snapshot, error in
            if let snapshot {
                results.append(contentsOf: snapshot.items)
                nextCursor = snapshot.nextCursor
            }
            errorMessage = error
            isSearching = false
        }
    }

    private func refreshRecommendations() async {
        await withCheckedContinuation { continuation in
            model.loadSearchDiscovery { _ in continuation.resume() }
        }
    }

    private var typeQueryValue: String? { selectedType == "all" ? nil : selectedType }
    private var airingQueryValue: String? { selectedAiring == "all" ? nil : selectedAiring }
    private var hasActiveFilters: Bool {
        selectedType != "all" || selectedAiring != "all" || selectedSort != "relevance" ||
            !yearStartText.isEmpty || !yearEndText.isEmpty
    }

    private var activeFilterSummary: String {
        var values: [String] = []
        if selectedType != "all" { values.append(selectedType.searchTypeName) }
        if selectedAiring != "all" { values.append(selectedAiring.searchAiringName) }
        if selectedSort != "relevance" { values.append(selectedSort.searchSortName) }
        if !yearStartText.isEmpty || !yearEndText.isEmpty {
            values.append("年份")
        }
        return values.joined(separator: " · ")
    }
}

private struct NativeSearchFiltersSheet: View {
    @Binding var selectedType: String
    @Binding var selectedAiring: String
    @Binding var selectedSort: String
    @Binding var yearStartText: String
    @Binding var yearEndText: String
    let onApply: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var validationMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("筛选") {
                    Picker("类型", selection: $selectedType) {
                        Text("全部类型").tag("all")
                        Text("TV").tag("tv")
                        Text("Web").tag("web")
                        Text("OVA").tag("ova")
                        Text("剧场版").tag("movie")
                        Text("其他").tag("other")
                    }
                    Picker("播出状态", selection: $selectedAiring) {
                        Text("全部状态").tag("all")
                        Text("未开播").tag("announced")
                        Text("连载中").tag("airing")
                        Text("已完结").tag("finished")
                    }
                    Picker("排序", selection: $selectedSort) {
                        Text("相关度").tag("relevance")
                        Text("评分优先").tag("rating")
                        Text("最近更新").tag("updated")
                    }
                }

                Section("年份范围") {
                    TextField("起始年份（可选）", text: $yearStartText)
                        .keyboardType(.numberPad)
                    TextField("结束年份（可选）", text: $yearEndText)
                        .keyboardType(.numberPad)
                    if let validationMessage {
                        Text(validationMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    Button("清除筛选") {
                        selectedType = "all"
                        selectedAiring = "all"
                        selectedSort = "relevance"
                        yearStartText = ""
                        yearEndText = ""
                        validationMessage = nil
                    }
                }
            }
            .navigationTitle("搜索筛选")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("应用") { apply() }
                }
            }
        }
    }

    private func apply() {
        let start = yearStartText.trimmingCharacters(in: .whitespacesAndNewlines)
        let end = yearEndText.trimmingCharacters(in: .whitespacesAndNewlines)
        let startValue = start.isEmpty ? nil : Int(start)
        let endValue = end.isEmpty ? nil : Int(end)
        guard (start.isEmpty || startValue != nil), (end.isEmpty || endValue != nil) else {
            validationMessage = "年份必须是数字"
            return
        }
        guard (startValue == nil || startValue! >= 1900), (endValue == nil || endValue! >= 1900) else {
            validationMessage = "年份应不早于 1900 年"
            return
        }
        guard startValue == nil || endValue == nil || startValue! <= endValue! else {
            validationMessage = "起始年份不能晚于结束年份"
            return
        }
        onApply()
        dismiss()
    }
}

struct NativeCollectionView: View {
    @ObservedObject var model: NativeAppModel
    @State private var selectedStatus = "Watching"
    @State private var errorMessage: String?

    private let filters = ["Watching", "Wish", "Completed", "OnHold", "Dropped"]

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 20) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(filters, id: \.self) { filter in
                                Button(filter.displayName) {
                                    selectedStatus = filter
                                    loadCollection(filter)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(selectedStatus == filter ? .accentColor : .gray.opacity(0.35))
                            }
                        }
                    }
                    if let errorMessage {
                        NativeInlineError(message: errorMessage) { loadCollection(selectedStatus) }
                    }
                    if let page = model.collectionPage, !page.items.isEmpty {
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 155), spacing: 12)], spacing: 18) {
                            ForEach(page.items) { item in
                                NavigationLink {
                                    NativeSubjectDetailView(summary: item.subjectSummary, model: model)
                                } label: {
                                    NativeCollectionCard(item: item)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        if let cursor = page.nextCursor {
                            Button("加载更多") {
                                model.loadCollection(status: selectedStatus, cursor: cursor, append: true) { _, error in
                                    errorMessage = error
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .buttonStyle(.bordered)
                        }
                    } else if !model.isSessionReady || model.isLoadingCollection {
                        ProgressView("正在恢复片库")
                            .frame(maxWidth: .infinity, minHeight: 220)
                    } else {
                        ContentUnavailableView("还没有作品", systemImage: "books.vertical", description: Text("在作品详情中收藏后，会显示在这里。"))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .navigationTitle("片库")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await withCheckedContinuation { continuation in
                    model.loadCollection(status: selectedStatus) { _, error in
                        errorMessage = error
                        continuation.resume()
                    }
                }
            }
            .task {
                model.start()
                if model.isSessionReady, model.session.status == "authenticated", model.collectionPage == nil {
                    loadCollection(selectedStatus)
                }
            }
            .onChange(of: model.isSessionReady) { _, ready in
                if ready, model.session.status == "authenticated", model.collectionPage == nil {
                    loadCollection(selectedStatus)
                }
            }
            .onChange(of: model.session.status) { _, status in
                if status == "authenticated", model.isSessionReady, model.collectionPage == nil {
                    loadCollection(selectedStatus)
                }
            }
        }
    }

    private func loadCollection(_ status: String) {
        errorMessage = nil
        model.loadCollection(status: status) { _, error in
            errorMessage = error
        }
    }
}

struct NativeCalendarView: View {
    @ObservedObject var model: NativeAppModel
    @State private var selectedDate = Date()
    @State private var errorMessage: String?

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                DatePicker("播出日期", selection: $selectedDate, displayedComponents: .date)
                    .datePickerStyle(.compact)
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))

                if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                } else if let calendar = model.calendar, calendar.date == dateString {
                    if calendar.items.isEmpty {
                        ContentUnavailableView("当天没有播出", systemImage: "calendar.badge.clock", description: Text("可以切换到其他日期查看。"))
                            .frame(maxWidth: .infinity, minHeight: 180)
                    } else {
                        ForEach(calendar.items) { subject in
                            NavigationLink {
                                NativeSubjectDetailView(summary: subject, model: model)
                            } label: {
                                NativeCalendarRow(subject: subject)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                } else {
                    ProgressView("正在加载日历")
                        .frame(maxWidth: .infinity, minHeight: 180)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("播出日历")
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
        .onChange(of: selectedDate) { _, _ in load() }
        .refreshable { await refresh() }
    }

    private var dateString: String { Self.dateFormatter.string(from: selectedDate) }

    private func load() {
        errorMessage = nil
        model.loadCalendar(date: dateString) { _, error in errorMessage = error }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadCalendar(date: dateString) { _, error in
                errorMessage = error
                continuation.resume()
            }
        }
    }
}

private struct NativeCalendarRow: View {
    let subject: NativeSubjectSummary

    var body: some View {
        HStack(spacing: 14) {
            NativeRemoteImage(url: subject.posterURL)
                .frame(width: 58, height: 78)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            VStack(alignment: .leading, spacing: 5) {
                Text(subject.title)
                    .font(.headline)
                    .lineLimit(2)
                Text(subject.originalTitle ?? subject.type.displayName)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                if let rating = subject.rating {
                    Label(String(format: "%.1f", rating), systemImage: "star.fill")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.orange)
                }
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(.tertiary)
        }
        .padding(14)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

struct NativeSubjectSectionsView: View {
    let subjectId: Int64
    @ObservedObject var model: NativeAppModel
    @State private var selectedSection = "episodes"
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 14) {
                Picker("资料类型", selection: $selectedSection) {
                    Text("分集").tag("episodes")
                    Text("角色").tag("characters")
                    Text("关联").tag("relations")
                }
                .pickerStyle(.segmented)

                if let errorMessage {
                    NativeInlineError(message: errorMessage) { load(force: true) }
                } else if let sections = model.subjectSections[subjectId] {
                    sectionContent(sections)
                } else {
                    ProgressView("正在加载作品资料")
                        .frame(maxWidth: .infinity, minHeight: 220)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("作品资料")
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
        .refreshable { await refresh() }
    }

    @ViewBuilder
    private func sectionContent(_ sections: NativeSubjectSectionsSnapshot) -> some View {
        switch selectedSection {
        case "characters":
            if sections.characters.isEmpty {
                ContentUnavailableView("暂无角色资料", systemImage: "person.2")
            } else {
                ForEach(sections.characters) { character in
                    HStack(spacing: 12) {
                        NativeRemoteImage(url: character.imageURL)
                            .frame(width: 54, height: 54)
                            .clipShape(Circle())
                        VStack(alignment: .leading, spacing: 4) {
                            Text(character.name).font(.headline)
                            Text(character.relation)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                            if !character.actors.isEmpty {
                                Text("配音：" + character.actors.map(\.name).joined(separator: "、"))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }
                        }
                        Spacer()
                    }
                    .padding(14)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
            }
        case "relations":
            if sections.relations.isEmpty {
                ContentUnavailableView("暂无关联作品", systemImage: "link")
            } else {
                ForEach(sections.relations) { relation in
                    NavigationLink {
                        NativeSubjectDetailView(summary: relation.subject, model: model)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(relation.label.isEmpty ? relation.kind : relation.label)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.tint)
                                Text(relation.subject.title)
                                    .font(.headline)
                                    .foregroundStyle(.primary)
                                    .lineLimit(2)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(.tertiary)
                        }
                        .padding(14)
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        default:
            if sections.episodes.isEmpty {
                ContentUnavailableView("暂无分集资料", systemImage: "list.number")
            } else {
                ForEach(sections.episodes) { episode in
                    HStack(spacing: 12) {
                        Text(episode.number.map { String(format: "%g", $0) } ?? "—")
                            .font(.headline.monospacedDigit())
                            .frame(width: 42)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(episode.title ?? "未命名分集")
                                .font(.headline)
                                .lineLimit(2)
                            Text([episode.airDate, episode.airStatus.displayName].compactMap { $0 }.joined(separator: " · "))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .padding(14)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
            }
        }
    }

    private func load(force: Bool = false) {
        errorMessage = nil
        model.loadSubjectSections(id: subjectId, force: force) { _, error in errorMessage = error }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadSubjectSections(id: subjectId, force: true) { _, error in
                errorMessage = error
                continuation.resume()
            }
        }
    }
}

struct NativeActivityView: View {
    @ObservedObject var model: NativeAppModel
    @State private var activityTitleOpacity = 1.0
    @State private var selectedMode = "动态"
    @State private var selectedFeed = "public"
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    Picker("内容", selection: $selectedMode) {
                        Text("动态").tag("动态")
                        Text("通知").tag("通知")
                    }
                    .pickerStyle(.segmented)
                    if selectedMode == "动态" {
                        feedPicker
                        activityContent
                    } else {
                        notificationContent
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .onScrollGeometryChange(for: CGFloat.self) { geometry in
                geometry.contentOffset.y + geometry.contentInsets.top
            } action: { _, offset in
                let fadeDistance: CGFloat = 56
                let nextOpacity = 1 - min(max(offset / fadeDistance, 0), 1)
                if abs(nextOpacity - activityTitleOpacity) > 0.01 {
                    activityTitleOpacity = nextOpacity
                }
            }
            .refreshable { await refresh() }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Text("动态")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .fixedSize(horizontal: true, vertical: false)
                        .layoutPriority(1)
                        .opacity(activityTitleOpacity)
                        .accessibilityAddTraits(.isHeader)
                }
                .sharedBackgroundVisibility(.hidden)

                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeListsView(model: model)
                    } label: {
                        Image(systemName: "list.bullet.rectangle")
                    }
                    .accessibilityLabel("片单")
                }
            }
            .navigationDestination(for: NativeSubjectSummary.self) { subject in
                NativeSubjectDetailView(summary: subject, model: model)
            }
        }
        .task {
            model.start()
            loadCurrentMode()
        }
        .onChange(of: selectedMode) { _, _ in loadCurrentMode() }
        .onChange(of: selectedFeed) { _, _ in loadCurrentMode() }
    }

    private var feedPicker: some View {
        HStack(spacing: 8) {
            ForEach(["following", "popular", "public"], id: \.self) { feed in
                Button(feed.feedDisplayName) { selectedFeed = feed }
                    .buttonStyle(.borderedProminent)
                    .tint(selectedFeed == feed ? .accentColor : .gray.opacity(0.35))
            }
        }
    }

    @ViewBuilder
    private var activityContent: some View {
        if let errorMessage {
            NativeInlineError(message: errorMessage) { loadCurrentMode() }
        } else if let page = model.activityPage, !page.items.isEmpty {
            ForEach(page.items) { item in
                NativeActivityCard(item: item, destination: activityDestination(for: item))
            }
            if page.nextCursor != nil {
                Button("加载更多") {
                    model.loadActivity(feed: selectedFeed, cursor: page.nextCursor, append: true)
                }
                .frame(maxWidth: .infinity)
                .buttonStyle(.bordered)
            }
        } else {
            ContentUnavailableView("还没有动态", systemImage: "bubble.left.and.bubble.right", description: Text("完成一次评分、评价或讨论后，内容会显示在这里。"))
                .frame(maxWidth: .infinity, minHeight: 220)
        }
    }

    @ViewBuilder
    private var notificationContent: some View {
        if model.notifications.isEmpty {
            ContentUnavailableView("没有新通知", systemImage: "bell", description: Text("新的互动会在这里提醒你。"))
                .frame(maxWidth: .infinity, minHeight: 220)
        } else {
            ForEach(model.notifications) { notification in
                NativeNotificationRow(notification: notification, destination: notificationDestination(for: notification)) {
                    model.markNotificationRead(id: notification.id)
                }
            }
        }
    }

    private func loadCurrentMode() {
        errorMessage = nil
        if selectedMode == "通知" {
            model.loadNotifications { _, error in errorMessage = error }
        } else {
            model.loadActivity(feed: selectedFeed) { _, error in errorMessage = error }
        }
    }

    private func activityDestination(for item: NativeActivityItemSnapshot) -> AnyView? {
        if let subject = item.subjectSummary {
            return AnyView(NativeSubjectDetailView(summary: subject, model: model))
        }
        if let reviewId = item.reviewId {
            return AnyView(NativeReviewDetailView(reviewId: reviewId, model: model))
        }
        if let listId = item.listId {
            return AnyView(NativeListDetailView(listId: listId, model: model))
        }
        if let actorId = item.actorId {
            return AnyView(NativeUserProfileView(userId: actorId, model: model))
        }
        return nil
    }

    private func notificationDestination(for notification: NativeNotificationSnapshot) -> AnyView? {
        if let reviewId = notification.reviewId {
            return AnyView(NativeReviewDetailView(reviewId: reviewId, model: model))
        }
        if let commentId = notification.commentId, let subjectId = notification.subjectId {
            return AnyView(
                NativeSubjectDetailView(
                    summary: NativeSubjectSummary.placeholder(id: subjectId, title: "讨论"),
                    model: model,
                    focusCommentId: commentId,
                )
            )
        }
        if let listId = notification.listId {
            return AnyView(NativeListDetailView(listId: listId, model: model))
        }
        if let actorId = notification.actorId {
            return AnyView(NativeUserProfileView(userId: actorId, model: model))
        }
        return nil
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            if selectedMode == "通知" {
                model.loadNotifications { _, error in errorMessage = error; continuation.resume() }
            } else {
                model.loadActivity(feed: selectedFeed) { _, error in errorMessage = error; continuation.resume() }
            }
        }
    }
}

struct NativeProfileView: View {
    @ObservedObject var model: NativeAppModel
    @State private var profileTitleOpacity = 1.0
    @State private var showingAccount = false
    @State private var message: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    if model.session.status == "authenticated" {
                        authenticatedContent
                    } else if model.session.status == "restoring" {
                        ProgressView("正在恢复账号")
                            .frame(maxWidth: .infinity, minHeight: 180)
                    } else {
                        guestContent
                    }
                    if let message {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(message.contains("失败") ? .red : .secondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .onScrollGeometryChange(for: CGFloat.self) { geometry in
                geometry.contentOffset.y + geometry.contentInsets.top
            } action: { _, offset in
                let fadeDistance: CGFloat = 56
                let nextOpacity = 1 - min(max(offset / fadeDistance, 0), 1)
                if abs(nextOpacity - profileTitleOpacity) > 0.01 {
                    profileTitleOpacity = nextOpacity
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Text("我的")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .fixedSize(horizontal: true, vertical: false)
                        .layoutPriority(1)
                        .opacity(profileTitleOpacity)
                        .accessibilityAddTraits(.isHeader)
                }
                .sharedBackgroundVisibility(.hidden)

                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeSettingsView(model: model)
                    } label: {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel("设置")
                }
            }
            .sheet(isPresented: $showingAccount) {
                NativeAccountSheet(model: model)
                    .presentationDetents([.medium, .large])
            }
        }
        .task {
            model.start()
            if model.session.status == "authenticated" { model.loadProfile() }
        }
        .onChange(of: model.session.status) { _, status in
            if status == "authenticated" { model.loadProfile() }
        }
    }

    @ViewBuilder
    private var authenticatedContent: some View {
        HStack(spacing: 14) {
            NativeAvatar(url: model.profile?.avatarURL ?? model.session.avatarURL, name: model.profile?.displayName ?? model.session.displayName)
            VStack(alignment: .leading, spacing: 4) {
                Text(model.profile?.displayName ?? model.session.displayName ?? "Anime 用户")
                    .font(.title3.weight(.bold))
                Text("已登录 · \(model.profile?.provider ?? "Anime")")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(18)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))

        if let profile = model.profile {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                NativeMetric(value: profile.watchingCount + profile.completedCount, title: "收藏")
                NativeMetric(value: profile.ratingCount, title: "评分")
                NativeMetric(value: profile.reviewCount, title: "评价")
            }
        }

        NavigationLink {
            NativeCollectionView(model: model)
        } label: {
            NativeActionRow(title: "我的片库", subtitle: "浏览正在追、想看和已完成的作品", systemImage: "books.vertical")
        }
        .buttonStyle(.plain)

        Button {
            model.logout { error in message = error ?? "已退出登录" }
        } label: {
            NativeActionRow(title: "退出登录", subtitle: "清除本机凭据并返回访客状态", systemImage: "rectangle.portrait.and.arrow.right", tint: .red)
        }
        .buttonStyle(.plain)
    }

    private var guestContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.system(size: 42))
                .foregroundStyle(.tint)
            Text("登录 Anime")
                .font(.title2.weight(.bold))
            Text("登录后可同步片库、评分、评价和社区互动。")
                .foregroundStyle(.secondary)
            Button("登录或注册") { showingAccount = true }
                .buttonStyle(.borderedProminent)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

struct NativeReviewDetailView: View {
    let reviewId: String
    @ObservedObject var model: NativeAppModel
    @State private var review: NativeReviewSnapshot?
    @State private var errorMessage: String?
    @State private var isLoading = true
    @State private var isLiked = false
    @State private var isBookmarked = false
    @State private var message: String?
    @State private var showingEditor = false
    @State private var showingDeleteConfirmation = false
    @State private var showingAccount = false
    @State private var isReacting = false
    @State private var isDeleting = false

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                if isLoading && review == nil {
                    ProgressView("正在加载评价")
                        .frame(maxWidth: .infinity, minHeight: 220)
                } else if let review {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 5) {
                                Text(review.title ?? "作品评价")
                                    .font(.title2.weight(.bold))
                                Text(review.createdAt.replacingOccurrences(of: "T", with: " ").prefix(16))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if review.spoiler {
                                Label("剧透", systemImage: "eye.slash")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.orange)
                            }
                        }
                        Text(review.body)
                            .font(.body)
                            .fixedSize(horizontal: false, vertical: true)
                        HStack(spacing: 18) {
                            Button {
                                guard authorizeWrite() else { return }
                                react("like")
                            } label: {
                                Label("\(review.likeCount)", systemImage: isLiked ? "heart.fill" : "heart")
                            }
                            .disabled(model.isRestoringSession || isReacting)
                            Button {
                                guard authorizeWrite() else { return }
                                react("bookmark")
                            } label: {
                                Label("\(review.bookmarkCount)", systemImage: isBookmarked ? "bookmark.fill" : "bookmark")
                            }
                            .disabled(model.isRestoringSession || isReacting)
                        }
                        .foregroundStyle(.tint)
                    }
                    .padding(18)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))

                    NavigationLink {
                        NativeSubjectDetailView(summary: NativeSubjectSummary.placeholder(id: review.subjectId, title: "作品"), model: model)
                    } label: {
                        NativeActionRow(title: "查看所属作品", subtitle: "打开作品详情和社区讨论", systemImage: "film")
                    }
                    .buttonStyle(.plain)

                    if let message {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(message.hasPrefix("操作失败") ? .red : .secondary)
                    }
                } else if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("评价")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let review, review.owned, review.kind == "short" {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("编辑") {
                            guard authorizeWrite() else { return }
                            showingEditor = true
                        }
                        Button("删除评价", role: .destructive) {
                            guard authorizeWrite() else { return }
                            showingDeleteConfirmation = true
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .task { load() }
        .refreshable { await refresh() }
        .sheet(isPresented: $showingEditor) {
            if let review {
                NativeReviewEditor(review: review, model: model) { updated, error in
                    if let updated { self.review = updated; message = "评价已更新" }
                    if let error { message = "更新失败：\(error)" }
                }
            }
        }
        .sheet(isPresented: $showingAccount) {
            NativeAccountSheet(model: model)
        }
        .confirmationDialog("确定删除这条评价吗？", isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button("删除评价", role: .destructive) {
                guard authorizeWrite(), !isDeleting else { return }
                isDeleting = true
                model.deleteReview(id: reviewId) { error in
                    isDeleting = false
                    if let error { message = "删除失败：\(error)" }
                    else { message = "评价已删除"; review = nil }
                }
            }
        }
    }

    private func load() {
        isLoading = true
        errorMessage = nil
        model.loadReview(id: reviewId) { value, error in
            review = value
            errorMessage = error
            isLiked = false
            isBookmarked = false
            isLoading = false
        }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadReview(id: reviewId) { value, error in
                review = value
                errorMessage = error
                continuation.resume()
            }
        }
    }

    private func react(_ reaction: String) {
        guard !isReacting else { return }
        let active = reaction == "like" ? !isLiked : !isBookmarked
        isReacting = true
        model.reactReview(id: reviewId, reaction: reaction, active: active) { value, error in
            isReacting = false
            if let value {
                if reaction == "like" { isLiked = value.active }
                if reaction == "bookmark" { isBookmarked = value.active }
                message = "操作已完成"
            } else if let error {
                message = "操作失败：\(error)"
            }
        }
    }

    private func authorizeWrite() -> Bool {
        if model.isRestoringSession {
            message = "正在恢复登录状态，请稍候"
            return false
        }
        guard model.isAuthenticated else {
            showingAccount = true
            return false
        }
        return true
    }
}

private struct NativeReviewEditor: View {
    let review: NativeReviewSnapshot
    @ObservedObject var model: NativeAppModel
    let onComplete: (NativeReviewSnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var reviewText: String
    @State private var spoiler: Bool
    @State private var visibility: String
    @State private var isSaving = false

    init(review: NativeReviewSnapshot, model: NativeAppModel, onComplete: @escaping (NativeReviewSnapshot?, String?) -> Void) {
        self.review = review
        self.model = model
        self.onComplete = onComplete
        _reviewText = State(initialValue: review.body)
        _spoiler = State(initialValue: review.spoiler)
        _visibility = State(initialValue: review.visibility)
    }

    var body: some View {
        NavigationStack {
            Form {
                TextEditor(text: $reviewText).frame(minHeight: 170)
                Toggle("包含剧透", isOn: $spoiler)
                Picker("可见性", selection: $visibility) {
                    Text("公开").tag("public")
                    Text("仅自己").tag("private")
                }
                Button(isSaving ? "保存中…" : "保存修改") { save() }
                    .disabled(isSaving || reviewText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle("编辑评价")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
            }
        }
    }

    private func save() {
        guard !isSaving else { return }
        let body = reviewText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty else { return }
        isSaving = true
        model.updateReview(
            id: review.id,
            title: nil,
            body: body,
            spoiler: spoiler,
            visibility: visibility,
        ) { updated, error in
            isSaving = false
            onComplete(updated, error)
            if updated != nil { dismiss() }
        }
    }
}

struct NativeUserProfileView: View {
    let userId: String
    @ObservedObject var model: NativeAppModel
    @State private var errorMessage: String?
    @State private var isLoading = true
    @State private var isFollowing = false
    @State private var message: String?

    private var profile: NativeUserProfileSnapshot? { model.userProfiles[userId] }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                if let profile {
                    HStack(spacing: 14) {
                        NativeAvatar(url: profile.avatarURL, name: profile.displayName)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(profile.displayName).font(.title3.weight(.bold))
                            Text("加入于 \(profile.createdAt.prefix(10))")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button(isFollowing ? "已关注" : "关注") {
                            model.followUser(id: userId, following: !isFollowing) { value, error in
                                if let value { isFollowing = value }
                                message = error.map { "操作失败：\($0)" }
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))

                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                        NativeMetric(value: Int(profile.reviewCount), title: "评价")
                        NativeMetric(value: Int(profile.ratingCount), title: "评分")
                        NativeMetric(value: Int(profile.followerCount), title: "粉丝")
                    }

                    if let reviews = model.userReviews[userId]?.items, !reviews.isEmpty {
                        Text("评价").font(.title3.weight(.bold))
                        ForEach(reviews) { review in
                            NavigationLink {
                                NativeReviewDetailView(reviewId: review.id, model: model)
                            } label: {
                                NativeReviewRow(review: review)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    if let lists = model.userLists[userId], !lists.isEmpty {
                        Text("片单").font(.title3.weight(.bold))
                        ForEach(lists) { list in
                            NavigationLink {
                                NativeListDetailView(listId: list.id, model: model)
                            } label: {
                                NativeListSummaryRow(list: list)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    if let message {
                        Text(message).font(.footnote).foregroundStyle(.secondary)
                    }
                } else if isLoading {
                    ProgressView("正在加载用户")
                        .frame(maxWidth: .infinity, minHeight: 220)
                } else if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("用户")
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
        .refreshable { await refresh() }
    }

    private func load() {
        isLoading = true
        errorMessage = nil
        var remaining = 2
        func finish() {
            remaining -= 1
            if remaining == 0 { isLoading = false }
        }
        model.loadUserProfile(id: userId) { value, error in
            if let value { isFollowing = value.following }
            errorMessage = error
            finish()
        }
        model.loadUserReviews(id: userId) { _, error in
            if errorMessage == nil { errorMessage = error }
            finish()
        }
        model.loadUserLists(id: userId) { _, error in
            if errorMessage == nil { errorMessage = error }
        }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadUserProfile(id: userId) { value, error in
                if let value { isFollowing = value.following }
                errorMessage = error
                continuation.resume()
            }
            model.loadUserReviews(id: userId)
            model.loadUserLists(id: userId)
        }
    }
}

private struct NativeReviewRow: View {
    let review: NativeReviewSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(review.title ?? "作品评价").font(.headline)
            Text(review.body).font(.subheadline).foregroundStyle(.secondary).lineLimit(3)
            Text(review.createdAt.replacingOccurrences(of: "T", with: " ").prefix(10))
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct NativeListSummaryRow: View {
    let list: NativeListSummarySnapshot

    var body: some View {
        HStack {
            Image(systemName: "list.bullet.rectangle.portrait")
                .font(.title2)
                .foregroundStyle(.tint)
            VStack(alignment: .leading, spacing: 4) {
                Text(list.title).font(.headline)
                Text("\(list.itemCount) 部作品 · \(list.followerCount) 人关注")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(.tertiary)
        }
        .padding(14)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct NativeListsView: View {
    @ObservedObject var model: NativeAppModel
    @State private var showingEditor = false
    @State private var errorMessage: String?

    var body: some View {
        List {
            if let errorMessage {
                Section { Text(errorMessage).foregroundStyle(.red) }
            }
            if model.lists.isEmpty {
                ContentUnavailableView("还没有片单", systemImage: "list.bullet.rectangle", description: Text("创建片单来整理喜欢的作品。"))
            } else {
                ForEach(model.lists) { list in
                    NavigationLink {
                        NativeListDetailView(listId: list.id, model: model)
                    } label: {
                        NativeListSummaryRow(list: list)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollIndicators(.hidden)
        .navigationTitle("片单")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showingEditor = true } label: { Image(systemName: "plus") }
            }
        }
        .task { load() }
        .refreshable { await refresh() }
        .sheet(isPresented: $showingEditor) {
            NativeListEditorView(model: model) { _, error in errorMessage = error }
        }
    }

    private func load() {
        model.loadLists { _, error in errorMessage = error }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadLists { _, error in errorMessage = error; continuation.resume() }
        }
    }
}

struct NativeListDetailView: View {
    let listId: String
    @ObservedObject var model: NativeAppModel
    @State private var errorMessage: String?
    @State private var isFollowing = false
    @State private var showingEditor = false
    @State private var showingDeleteConfirmation = false
    @State private var message: String?

    private var detail: NativeListDetailSnapshot? { model.listDetails[listId] }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 14) {
                if let detail {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(detail.summary.title).font(.title2.weight(.bold))
                        Text(detail.summary.description.isEmpty ? "暂无描述" : detail.summary.description)
                            .foregroundStyle(.secondary)
                        Text("创建者：\(detail.summary.ownerName) · \(detail.summary.itemCount) 部作品")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                        HStack {
                            Button(isFollowing ? "已关注" : "关注片单") {
                                model.followList(id: listId, following: !isFollowing) { value, error in
                                    if let value { isFollowing = value }
                                    message = error.map { "操作失败：\($0)" }
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            if let ownerId = detail.summary.ownerId {
                                NavigationLink("查看创建者") {
                                    NativeUserProfileView(userId: ownerId, model: model)
                                }
                                .buttonStyle(.bordered)
                            }
                        }
                    }
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))

                    if detail.items.isEmpty {
                        ContentUnavailableView("片单暂无作品", systemImage: "books.vertical")
                    } else {
                        ForEach(detail.items) { item in
                            NavigationLink {
                                NativeSubjectDetailView(summary: NativeSubjectSummary.placeholder(id: item.subjectId, title: item.title), model: model)
                            } label: {
                                HStack(spacing: 12) {
                                    NativeRemoteImage(url: item.posterURL)
                                        .frame(width: 54, height: 74)
                                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(item.title).font(.headline).lineLimit(2)
                                        if let note = item.note, !note.isEmpty {
                                            Text(note).font(.caption).foregroundStyle(.secondary).lineLimit(2)
                                        }
                                    }
                                    Spacer()
                                    if let score = item.score { Text(String(format: "%.1f", score)).font(.caption.weight(.semibold)) }
                                }
                                .padding(12)
                                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    if let message { Text(message).font(.footnote).foregroundStyle(.secondary) }
                } else if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                } else {
                    ProgressView("正在加载片单").frame(maxWidth: .infinity, minHeight: 220)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("片单")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if detail?.summary.owned == true {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("编辑") { showingEditor = true }
                        Button("删除片单", role: .destructive) { showingDeleteConfirmation = true }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .task { load() }
        .refreshable { await refresh() }
        .sheet(isPresented: $showingEditor) {
            if let detail {
                NativeListEditorView(model: model, existing: detail) { _, error in
                    errorMessage = error
                    if error == nil { load() }
                }
            }
        }
        .confirmationDialog("确定删除这个片单吗？", isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button("删除片单", role: .destructive) {
                model.deleteList(id: listId) { error in
                    message = error.map { "删除失败：\($0)" } ?? "片单已删除"
                }
            }
        }
    }

    private func load() {
        errorMessage = nil
        model.loadList(id: listId) { value, error in
            if let value { isFollowing = value.summary.following }
            errorMessage = error
        }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadList(id: listId) { value, error in
                if let value { isFollowing = value.summary.following }
                errorMessage = error
                continuation.resume()
            }
        }
    }
}

private struct NativeListEditorView: View {
    @ObservedObject var model: NativeAppModel
    let existing: NativeListDetailSnapshot?
    let onComplete: (NativeListSummarySnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var title: String
    @State private var description: String
    @State private var visibility: String
    @State private var subjectIdsText: String
    @State private var isSaving = false

    init(model: NativeAppModel, existing: NativeListDetailSnapshot? = nil, onComplete: @escaping (NativeListSummarySnapshot?, String?) -> Void) {
        self.model = model
        self.existing = existing
        self.onComplete = onComplete
        _title = State(initialValue: existing?.summary.title ?? "")
        _description = State(initialValue: existing?.summary.description ?? "")
        _visibility = State(initialValue: "public")
        _subjectIdsText = State(initialValue: existing?.items.map { String($0.subjectId) }.joined(separator: ", ") ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("片单名称", text: $title)
                TextField("描述（可选）", text: $description, axis: .vertical)
                    .lineLimit(2...5)
                Picker("可见性", selection: $visibility) {
                    Text("公开").tag("public")
                    Text("仅自己").tag("private")
                }
                Section("作品") {
                    TextField("作品 ID，逗号分隔", text: $subjectIdsText, axis: .vertical)
                        .keyboardType(.numbersAndPunctuation)
                    Text("可以在作品详情或 Bangumi 中查看作品 ID。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Button(isSaving ? "保存中…" : (existing == nil ? "创建片单" : "保存修改")) { save() }
                    .disabled(isSaving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle(existing == nil ? "新建片单" : "编辑片单")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
            }
        }
    }

    private func save() {
        let ids = subjectIdsText.split { $0 == "," || $0 == "，" || $0 == " " || $0 == "\n" }.compactMap { Int64($0) }
        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanDescription = description.trimmingCharacters(in: .whitespacesAndNewlines)
        isSaving = true
        let finish: (NativeListSummarySnapshot?, String?) -> Void = { value, error in
            isSaving = false
            onComplete(value, error)
            if value != nil { dismiss() }
        }
        if let existing {
            model.updateList(id: existing.summary.id, title: cleanTitle, description: cleanDescription, visibility: visibility, subjectIds: ids, completion: finish)
        } else {
            model.createList(title: cleanTitle, description: cleanDescription, visibility: visibility, subjectIds: ids, completion: finish)
        }
    }
}

struct NativeSubjectCommunityView: View {
    let subjectId: Int64
    let community: NativeSubjectCommunitySnapshot?
    let errorMessage: String?
    let totalEpisodes: Int?
    @ObservedObject var model: NativeAppModel
    let onReload: () -> Void

    @State private var selectedScore = 0
    @State private var selectedCollectionStatus: String?
    @State private var selectedCollectionProgress = 0
    @State private var commentText = ""
    @State private var commentSpoiler = false
    @State private var isSubmitting = false
    @State private var isSavingRating = false
    @State private var isSavingCollection = false
    @State private var localComments: [NativeCommentSnapshot] = []
    @State private var localReviews: [NativeReviewSnapshot] = []
    @State private var commentReactions: [String: NativeReactionSnapshot] = [:]
    @State private var reviewReactions: [String: NativeReactionSnapshot] = [:]
    @State private var pendingReactionKeys: Set<String> = []
    @State private var pendingReportIds: Set<String> = []
    @State private var message: String?
    @State private var showingReviewComposer = false
    @State private var showingAccount = false
    @State private var editingComment: NativeCommentSnapshot?
    @State private var deletingCommentId: String?
    @State private var replyTargetId: String?
    @State private var didLoadCommentDraft = false
    @FocusState private var commentEditorFocused: Bool

    private let collectionStatuses = [
        ("Watching", "在看"),
        ("Wish", "想看"),
        ("Completed", "看过"),
        ("OnHold", "搁置"),
        ("Dropped", "抛弃"),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("社区")
                        .font(.title2.weight(.bold))
                    Text("评分、评价和讨论")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button(action: onReload) {
                    Image(systemName: "arrow.clockwise")
                }
                .accessibilityLabel("刷新社区内容")
            }

            if model.isRestoringSession {
                Label("正在恢复账号状态…", systemImage: "arrow.triangle.2.circlepath")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(.secondary)
            } else if !model.isAuthenticated {
                Label("登录后可评分、收藏、写评价和参与讨论", systemImage: "lock.open")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(.secondary)
            }

            if let errorMessage {
                NativeInlineError(message: errorMessage, retry: onReload)
            } else if let community {
                ratingAndCollection(community.rating)
                reviewSection(mergedReviews(community.reviews))
                commentSection(community.comments)
            } else {
                ProgressView("正在加载社区内容")
                    .frame(maxWidth: .infinity, minHeight: 100)
            }

            if let message {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(message.contains("失败") ? .red : .secondary)
            }
        }
        .onAppear {
            restorePersonalState()
            loadCommentDraft()
        }
        .onChange(of: community?.comments.map(\.id) ?? []) { _, ids in
            // Keep an optimistic local insertion until the server returns it, then
            // reconcile by id instead of clearing all local content on every refresh.
            localComments.removeAll { ids.contains($0.id) }
        }
        .onChange(of: community?.reviews.map(\.id) ?? []) { _, ids in
            localReviews.removeAll { ids.contains($0.id) }
        }
        .onChange(of: community?.personal?.userRating) { _, _ in
            restorePersonalState()
        }
        .onChange(of: community?.personal?.collectionStatus) { _, _ in
            restorePersonalState()
        }
        .onChange(of: model.isSessionReady) { _, ready in
            if ready, model.isAuthenticated {
                restorePersonalState()
                onReload()
            }
        }
        .onChange(of: model.session.status) { _, status in
            if status != "authenticated" {
                selectedScore = 0
                selectedCollectionStatus = nil
                replyTargetId = nil
            }
        }
        .sheet(isPresented: $showingReviewComposer) {
            NativeReviewComposer(subjectId: subjectId, model: model) { review, error in
                if let review {
                    localReviews.insert(review, at: 0)
                    message = "评价已发布"
                } else if let error {
                    message = "发布失败：\(error)"
                }
            }
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $showingAccount) {
            NativeAccountSheet(model: model)
        }
        .sheet(item: $editingComment) { comment in
            NativeCommentEditor(comment: comment, model: model) { updated, error in
                if let error {
                    message = "更新失败：\(error)"
                } else {
                    if let updated, let index = localComments.firstIndex(where: { $0.id == updated.id }) {
                        localComments[index] = updated
                    }
                    message = "讨论已更新"
                    onReload()
                }
            }
        }
        .confirmationDialog("确定删除这条讨论吗？", isPresented: Binding(
            get: { deletingCommentId != nil },
            set: { if !$0 { deletingCommentId = nil } },
        ), titleVisibility: .visible) {
            Button("删除讨论", role: .destructive) {
                guard let deletingCommentId else { return }
                model.deleteComment(id: deletingCommentId) { error in
                    message = error.map { "删除失败：\($0)" } ?? "讨论已删除"
                    if error == nil {
                        localComments.removeAll { $0.id == deletingCommentId }
                        if replyTargetId == deletingCommentId { cancelReply() }
                    }
                    self.deletingCommentId = nil
                    if error == nil { onReload() }
                }
            }
        }
    }

    private func ratingAndCollection(_ rating: NativeRatingSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline) {
                Image(systemName: "star.fill")
                    .foregroundStyle(.orange)
                VStack(alignment: .leading, spacing: 2) {
                    Text("社区平均分")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                    Text(rating.score.map { String(format: "%.1f", $0) } ?? "暂无评分")
                        .font(.title3.weight(.bold))
                }
                Text("· \(rating.votes) 人评分")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
            }

            HStack(spacing: 10) {
                Menu {
                    ForEach(1...10, id: \.self) { score in
                        Button(score == selectedScore ? "✓ 我的评分 \(score)" : "我的评分 \(score)") {
                            guard authorizeWrite() else { return }
                            let previousScore = selectedScore
                            selectedScore = score
                            saveRating(score, previousScore: previousScore)
                        }
                    }
                } label: {
                    Label(
                        selectedScore == 0 ? "我的评分" : "我的评分 \(selectedScore)/10",
                        systemImage: selectedScore == 0 ? "star" : "star.fill",
                    )
                }
                .buttonStyle(.borderedProminent)
                .disabled(model.isRestoringSession || isSavingRating)

                Menu {
                    ForEach(collectionStatuses, id: \.0) { status in
                        Button(status.1) {
                            guard authorizeWrite() else { return }
                            saveCollection(status.0)
                        }
                    }
                    Divider()
                    Button("移出片库", role: .destructive) {
                        guard authorizeWrite() else { return }
                        saveCollection(nil)
                    }
                } label: {
                    Label(selectedCollectionStatus?.displayName ?? "收藏状态", systemImage: selectedCollectionStatus == nil ? "plus.circle" : "checkmark.circle.fill")
                }
                .buttonStyle(.bordered)
                .disabled(model.isRestoringSession || isSavingCollection)
            }

            if selectedCollectionStatus != nil {
                Stepper(value: Binding(
                    get: { selectedCollectionProgress },
                    set: { saveCollectionProgress($0) },
                ), in: 0...max(0, totalEpisodes ?? 999)) {
                    HStack {
                        Label("观看进度", systemImage: "play.rectangle")
                        Spacer()
                        Text(progressLabel)
                            .foregroundStyle(.secondary)
                    }
                }
                .disabled(model.isRestoringSession || isSavingCollection)
            }
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    @ViewBuilder
    private func reviewSection(_ reviews: [NativeReviewSnapshot]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("评价")
                    .font(.title3.weight(.bold))
                Spacer()
                Button("写短评价") {
                    guard authorizeWrite() else { return }
                    showingReviewComposer = true
                }
                .disabled(model.isRestoringSession)
                    .font(.subheadline.weight(.semibold))
                Text("\(reviews.count)")
                    .foregroundStyle(.secondary)
            }

            if reviews.isEmpty {
                Text("还没有评价，成为第一个分享感受的人吧。")
                    .foregroundStyle(.secondary)
                    .padding(.vertical, 8)
            } else {
                ForEach(Array(reviews.prefix(5))) { review in
                    reviewCard(review)
                }
            }
        }
    }

    private func reviewCard(_ review: NativeReviewSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(alignment: .firstTextBaseline) {
                Text(review.title ?? "作品评价")
                    .font(.headline)
                Spacer()
                Text(review.createdAt.replacingOccurrences(of: "T", with: " ").prefix(10))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            if review.spoiler {
                Label("包含剧透", systemImage: "eye.slash")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.orange)
            }
            Text(review.body)
                .font(.body)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 16) {
                reactionButton(
                    title: "\(reviewReaction(review.id, kind: "like")?.likeCount ?? review.likeCount)",
                    systemImage: reviewReaction(review.id, kind: "like")?.active == true ? "heart.fill" : "heart",
                    tint: reviewReaction(review.id, kind: "like")?.active == true ? .pink : .secondary,
                    disabled: model.isRestoringSession || pendingReactionKeys.contains(reactionKey(review.id, kind: "like")),
                ) {
                    guard authorizeWrite() else { return }
                    let key = reactionKey(review.id, kind: "like")
                    guard !pendingReactionKeys.contains(key) else { return }
                    let active = reviewReactions[key]?.active != true
                    pendingReactionKeys.insert(key)
                    model.reactReview(id: review.id, reaction: "like", active: active) { reaction, error in
                        pendingReactionKeys.remove(key)
                        if let reaction { reviewReactions[key] = reaction }
                        message = error.map { "操作失败：\($0)" }
                    }
                }
                reactionButton(
                    title: "\(reviewReaction(review.id, kind: "bookmark")?.bookmarkCount ?? review.bookmarkCount)",
                    systemImage: reviewReaction(review.id, kind: "bookmark")?.active == true ? "bookmark.fill" : "bookmark",
                    tint: reviewReaction(review.id, kind: "bookmark")?.active == true ? .blue : .secondary,
                    disabled: model.isRestoringSession || pendingReactionKeys.contains(reactionKey(review.id, kind: "bookmark")),
                ) {
                    guard authorizeWrite() else { return }
                    let key = reactionKey(review.id, kind: "bookmark")
                    guard !pendingReactionKeys.contains(key) else { return }
                    let active = reviewReactions[key]?.active != true
                    pendingReactionKeys.insert(key)
                    model.reactReview(id: review.id, reaction: "bookmark", active: active) { reaction, error in
                        pendingReactionKeys.remove(key)
                        if let reaction { reviewReactions[key] = reaction }
                        message = error.map { "操作失败：\($0)" }
                    }
                }
            }
            .font(.caption.weight(.medium))
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    @ViewBuilder
    private func commentSection(_ comments: [NativeCommentSnapshot]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("讨论")
                    .font(.title3.weight(.bold))
                Spacer()
                Text("\(mergedComments(comments).count)")
                    .foregroundStyle(.secondary)
            }

            VStack(spacing: 10) {
                if let replyTarget {
                    HStack(spacing: 8) {
                        Image(systemName: "arrowshape.turn.up.left")
                        Text("回复 \(replyTarget.authorName)")
                            .font(.caption.weight(.medium))
                        Spacer()
                        Button("取消") { cancelReply() }
                            .font(.caption.weight(.semibold))
                    }
                    .foregroundStyle(.secondary)
                }
                TextEditor(text: $commentText)
                    .frame(minHeight: 84)
                    .padding(8)
                    .scrollContentBackground(.hidden)
                    .focused($commentEditorFocused)
                    .disabled(model.isRestoringSession)
                    .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(alignment: .topLeading) {
                        if commentText.isEmpty {
                            Text("说点什么…")
                                .foregroundStyle(.tertiary)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 16)
                                .allowsHitTesting(false)
                        }
                    }
                HStack {
                    Toggle("剧透", isOn: $commentSpoiler)
                        .font(.caption)
                    Spacer()
                    Button(isSubmitting ? "发布中…" : "发布") { submitComment() }
                        .buttonStyle(.borderedProminent)
                        .disabled(model.isRestoringSession || isSubmitting || commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }

            let visibleComments = mergedComments(comments)
            if visibleComments.isEmpty {
                Text("还没有讨论，欢迎开启话题。")
                    .foregroundStyle(.secondary)
                    .padding(.vertical, 8)
            } else {
                let commentIndex = Dictionary(uniqueKeysWithValues: visibleComments.map { ($0.id, $0) })
                let rootComments = visibleComments.filter { comment in
                    guard let parentId = comment.parentId else { return true }
                    // Only one reply level is rendered. Unexpected deeper replies
                    // are shown as standalone comments and cannot start another reply.
                    return commentIndex[parentId]?.parentId != nil || commentIndex[parentId] == nil
                }
                ForEach(rootComments) { comment in
                    commentCard(comment, isReply: false)
                    ForEach(visibleComments.filter { $0.parentId == comment.id }) { reply in
                        commentCard(reply, isReply: true)
                            .padding(.leading, 20)
                    }
                }
            }
        }
    }

    private func commentCard(_ comment: NativeCommentSnapshot, isReply: Bool) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(comment.authorName)
                    .font(.headline)
                if comment.owned {
                    Text("我")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tint)
                }
                Spacer()
                Text(comment.createdAt.replacingOccurrences(of: "T", with: " ").prefix(10))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Menu {
                    if comment.owned {
                        Button("编辑") {
                            guard authorizeWrite() else { return }
                            editingComment = comment
                        }
                        Button("删除", role: .destructive) {
                            guard authorizeWrite() else { return }
                            deletingCommentId = comment.id
                        }
                    } else {
                        Button("举报") {
                            guard authorizeWrite() else { return }
                            guard !pendingReportIds.contains(comment.id) else { return }
                            pendingReportIds.insert(comment.id)
                            model.reportComment(id: comment.id) { error in
                                pendingReportIds.remove(comment.id)
                                message = error.map { "举报失败：\($0)" } ?? "举报已提交"
                            }
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .foregroundStyle(.secondary)
                }
            }
            Text(comment.body)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 16) {
                if !isReply, comment.parentId == nil {
                    Button("回复") { beginReply(to: comment) }
                        .foregroundStyle(.tint)
                }
                reactionButton(
                    title: "\(commentReaction(comment.id, kind: "like")?.likeCount ?? comment.likeCount)",
                    systemImage: commentReaction(comment.id, kind: "like")?.active == true ? "heart.fill" : "heart",
                    tint: commentReaction(comment.id, kind: "like")?.active == true ? .pink : .secondary,
                    disabled: model.isRestoringSession || pendingReactionKeys.contains(reactionKey(comment.id, kind: "like")),
                ) {
                    guard authorizeWrite() else { return }
                    let key = reactionKey(comment.id, kind: "like")
                    guard !pendingReactionKeys.contains(key) else { return }
                    let active = commentReactions[key]?.active != true
                    pendingReactionKeys.insert(key)
                    model.reactComment(id: comment.id, reaction: "like", active: active) { reaction, error in
                        pendingReactionKeys.remove(key)
                        if let reaction { commentReactions[key] = reaction }
                        message = error.map { "操作失败：\($0)" }
                    }
                }
                reactionButton(
                    title: "\(commentReaction(comment.id, kind: "bookmark")?.bookmarkCount ?? comment.bookmarkCount)",
                    systemImage: commentReaction(comment.id, kind: "bookmark")?.active == true ? "bookmark.fill" : "bookmark",
                    tint: commentReaction(comment.id, kind: "bookmark")?.active == true ? .blue : .secondary,
                    disabled: model.isRestoringSession || pendingReactionKeys.contains(reactionKey(comment.id, kind: "bookmark")),
                ) {
                    guard authorizeWrite() else { return }
                    let key = reactionKey(comment.id, kind: "bookmark")
                    guard !pendingReactionKeys.contains(key) else { return }
                    let active = commentReactions[key]?.active != true
                    pendingReactionKeys.insert(key)
                    model.reactComment(id: comment.id, reaction: "bookmark", active: active) { reaction, error in
                        pendingReactionKeys.remove(key)
                        if let reaction { commentReactions[key] = reaction }
                        message = error.map { "操作失败：\($0)" }
                    }
                }
            }
            .font(.caption.weight(.medium))
        }
        .padding(14)
        .id("comment-\(comment.id)")
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var replyTarget: NativeCommentSnapshot? {
        guard let replyTargetId else { return nil }
        let comments = mergedComments(community?.comments ?? [])
        return comments.first { $0.id == replyTargetId && $0.parentId == nil }
    }

    private func mergedComments(_ comments: [NativeCommentSnapshot]) -> [NativeCommentSnapshot] {
        var result = comments
        let ids = Set(comments.map(\.id))
        result.append(contentsOf: localComments.filter { !ids.contains($0.id) })
        return result
    }

    private func mergedReviews(_ reviews: [NativeReviewSnapshot]) -> [NativeReviewSnapshot] {
        var result = reviews
        let ids = Set(reviews.map(\.id))
        result.append(contentsOf: localReviews.filter { !ids.contains($0.id) })
        return result
    }

    private func reactionKey(_ id: String, kind: String) -> String {
        "\(id):\(kind)"
    }

    private func reviewReaction(_ id: String, kind: String) -> NativeReactionSnapshot? {
        reviewReactions[reactionKey(id, kind: kind)]
    }

    private func commentReaction(_ id: String, kind: String) -> NativeReactionSnapshot? {
        commentReactions[reactionKey(id, kind: kind)]
    }

    private func reactionButton(
        title: String,
        systemImage: String,
        tint: Color,
        disabled: Bool = false,
        action: @escaping () -> Void,
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .foregroundStyle(tint)
        }
        .buttonStyle(.plain)
        .disabled(disabled)
    }

    private func authorizeWrite() -> Bool {
        if model.isRestoringSession {
            message = "正在恢复登录状态，请稍候"
            return false
        }
        guard model.isAuthenticated else {
            showingAccount = true
            return false
        }
        return true
    }

    private func beginReply(to comment: NativeCommentSnapshot) {
        guard comment.parentId == nil, authorizeWrite() else { return }
        replyTargetId = comment.id
        commentEditorFocused = true
        persistCommentDraft()
    }

    private func cancelReply() {
        replyTargetId = nil
        commentEditorFocused = false
        persistCommentDraft()
    }

    private func restorePersonalState() {
        guard model.isAuthenticated else {
            selectedScore = 0
            selectedCollectionStatus = nil
            selectedCollectionProgress = 0
            return
        }
        selectedScore = community?.personal?.userRating ?? model.cachedUserRating(subjectId: subjectId) ?? 0
        selectedCollectionStatus = community?.personal?.isCollected == false
            ? nil
            : community?.personal?.collectionStatus
        selectedCollectionProgress = max(0, community?.personal?.collectionEpisodeProgress ?? 0)
    }

    private var commentDraftKey: String {
        "anime.ios.comment-draft.\(subjectId)"
    }

    private func loadCommentDraft() {
        guard !didLoadCommentDraft else { return }
        didLoadCommentDraft = true
        guard
            let data = UserDefaults.standard.data(forKey: commentDraftKey),
            let draft = try? JSONDecoder().decode(NativeCommentDraft.self, from: data)
        else { return }
        commentText = draft.text
        commentSpoiler = draft.spoiler
        replyTargetId = draft.parentId
    }

    private func persistCommentDraft() {
        guard didLoadCommentDraft else { return }
        let text = commentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty || replyTargetId != nil else {
            UserDefaults.standard.removeObject(forKey: commentDraftKey)
            return
        }
        let draft = NativeCommentDraft(text: commentText, spoiler: commentSpoiler, parentId: replyTargetId)
        if let data = try? JSONEncoder().encode(draft) {
            UserDefaults.standard.set(data, forKey: commentDraftKey)
        }
    }

    private func saveRating(_ score: Int, previousScore: Int? = nil) {
        guard !isSavingRating else { return }
        isSavingRating = true
        message = nil
        model.saveRating(subjectId: subjectId, score: score) { error in
            isSavingRating = false
            if let error {
                selectedScore = previousScore ?? 0
                message = "评分失败：\(error)"
            } else {
                message = "评分已保存"
            }
            if error == nil { onReload() }
        }
    }

    private func saveCollection(_ status: String?) {
        guard !isSavingCollection else { return }
        isSavingCollection = true
        message = nil
        model.setCollection(subjectId: subjectId, status: status, episodeProgress: status == nil ? nil : selectedCollectionProgress) { error in
            isSavingCollection = false
            if error == nil {
                selectedCollectionStatus = status
                message = status.map { "已加入\($0.displayName)" } ?? "已移出片库"
                onReload()
            } else if let error {
                message = "收藏失败：\(error)"
            }
        }
    }

    private func saveCollectionProgress(_ progress: Int) {
        guard let status = selectedCollectionStatus, !isSavingCollection else { return }
        let maximum = max(0, totalEpisodes ?? 999)
        let normalized = min(max(progress, 0), maximum)
        guard normalized != selectedCollectionProgress else { return }
        let previous = selectedCollectionProgress
        selectedCollectionProgress = normalized
        isSavingCollection = true
        model.setCollection(subjectId: subjectId, status: status, episodeProgress: normalized) { error in
            isSavingCollection = false
            if let error {
                selectedCollectionProgress = previous
                message = "进度保存失败：\(error)"
            } else {
                message = "观看进度已保存"
                onReload()
            }
        }
    }

    private var progressLabel: String {
        if let totalEpisodes, totalEpisodes > 0 {
            return "\(selectedCollectionProgress)/\(totalEpisodes)"
        }
        return "第 \(selectedCollectionProgress) 集"
    }

    private func submitComment() {
        let body = commentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty else { return }
        guard authorizeWrite(), !isSubmitting else { return }
        let allComments = mergedComments(community?.comments ?? [])
        let parentId = replyTargetId.flatMap { targetId in
            allComments.first { $0.id == targetId && $0.parentId == nil }?.id
        }
        isSubmitting = true
        message = nil
        model.createCommentAdvanced(subjectId: subjectId, body: body, spoiler: commentSpoiler, parentId: parentId) { comment, error in
            isSubmitting = false
            if let comment {
                localComments.insert(comment, at: 0)
                commentText = ""
                commentSpoiler = false
                replyTargetId = nil
                commentEditorFocused = false
                persistCommentDraft()
                message = "讨论已发布"
            } else if let error {
                message = "发布失败：\(error)"
            }
        }
    }
}

private struct NativeCommentDraft: Codable {
    let text: String
    let spoiler: Bool
    let parentId: String?
}

private struct NativeCommentEditor: View {
    let comment: NativeCommentSnapshot
    @ObservedObject var model: NativeAppModel
    let onComplete: (NativeCommentSnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var commentText: String
    @State private var spoiler: Bool
    @State private var isSaving = false

    init(comment: NativeCommentSnapshot, model: NativeAppModel, onComplete: @escaping (NativeCommentSnapshot?, String?) -> Void) {
        self.comment = comment
        self.model = model
        self.onComplete = onComplete
        _commentText = State(initialValue: comment.body)
        _spoiler = State(initialValue: comment.spoiler)
    }

    var body: some View {
        NavigationStack {
            Form {
                TextEditor(text: $commentText).frame(minHeight: 150)
                Toggle("包含剧透", isOn: $spoiler)
                Button(isSaving ? "保存中…" : "保存修改") { save() }
                    .disabled(isSaving || commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle("编辑讨论")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
            }
        }
    }

    private func save() {
        guard !isSaving else { return }
        let body = commentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty else { return }
        isSaving = true
        model.updateComment(id: comment.id, body: body, spoiler: spoiler) { value, error in
            isSaving = false
            onComplete(value, error)
            if value != nil { dismiss() }
        }
    }
}

struct NativeSettingsView: View {
    @ObservedObject var model: NativeAppModel
    @State private var showingAccount = false

    var body: some View {
        Form {
            Section("账号") {
                if model.session.status == "authenticated" {
                    LabeledContent("当前账号", value: model.session.displayName ?? "Anime 用户")
                    NavigationLink {
                        NativeAccountManagementView(model: model)
                    } label: {
                        Label("账号与数据", systemImage: "person.crop.circle.badge.checkmark")
                    }
                    Button("切换账号") { showingAccount = true }
                } else {
                    Button("登录或注册") { showingAccount = true }
                }
            }

            Section("服务") {
                NavigationLink {
                    NativeDiagnosticsView(model: model)
                } label: {
                    Label("服务诊断", systemImage: "waveform.path.ecg")
                }
            }

            Section("外观") {
                Picker("主题", selection: Binding(
                    get: { model.appearanceTheme },
                    set: { model.setAppearanceTheme($0) },
                )) {
                    Text("跟随系统").tag("system")
                    Text("浅色").tag("light")
                    Text("深色").tag("dark")
                }
                Toggle("原生玻璃效果", isOn: Binding(
                    get: { model.glassEnabled },
                    set: { model.setGlassEnabled($0) },
                ))
                Toggle("减少动态效果", isOn: Binding(
                    get: { model.reduceMotionEnabled },
                    set: { model.setReduceMotionEnabled($0) },
                ))
            }

            Section("关于") {
                LabeledContent("版本", value: appVersion)
                LabeledContent("界面", value: "SwiftUI 原生")
            }
        }
        .navigationTitle("设置")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showingAccount) {
            NativeAccountSheet(model: model)
                .presentationDetents([.medium, .large])
        }
    }

    private var appVersion: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "未知"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String
        return build.map { "\(version) (\($0))" } ?? version
    }
}

struct NativeAccountManagementView: View {
    @ObservedObject var model: NativeAppModel
    @State private var displayName = ""
    @State private var currentPassword = ""
    @State private var newPassword = ""
    @State private var message: String?
    @State private var exportText: String?
    @State private var showingDeleteConfirmation = false

    var body: some View {
        Form {
            Section("个人资料") {
                TextField("显示名称", text: $displayName)
                Button("保存显示名称") {
                    model.updateProfile(displayName: displayName) { error in
                        message = error.map { "保存失败：\($0)" } ?? "资料已更新"
                        if error == nil { model.loadProfile() }
                    }
                }
                .disabled(displayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section("修改密码") {
                SecureField("当前密码", text: $currentPassword)
                SecureField("新密码", text: $newPassword)
                Button("修改密码") {
                    model.changePassword(currentPassword: currentPassword, newPassword: newPassword) { error in
                        message = error.map { "修改失败：\($0)" } ?? "密码已修改"
                        if error == nil { currentPassword = ""; newPassword = "" }
                    }
                }
                .disabled(currentPassword.isEmpty || newPassword.count < 8)
            }

            Section("数据与同步") {
                NavigationLink {
                    NativeSyncView(model: model)
                } label: {
                    Label("Bangumi 同步", systemImage: "arrow.triangle.2.circlepath")
                }
                Button("导出我的数据") {
                    model.exportMyData { data, error in
                        if let data { exportText = "导出成功，共 \(data.utf8.count) 字节" }
                        else { exportText = "导出失败：\(error ?? "未知错误")" }
                    }
                }
            }

            Section {
                Button("注销账号", role: .destructive) { showingDeleteConfirmation = true }
            } footer: {
                Text("注销会删除服务器账号及其社区数据，且无法恢复。")
            }

            if let message { Section { Text(message).foregroundStyle(message.contains("失败") ? .red : .secondary) } }
            if let exportText { Section("导出结果") { Text(exportText).font(.footnote) } }
        }
        .navigationTitle("账号与数据")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            displayName = model.profile?.displayName ?? model.session.displayName ?? ""
            if model.profile == nil { model.loadProfile() }
        }
        .confirmationDialog("确定注销账号吗？", isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button("永久注销", role: .destructive) {
                model.deleteAccount { error in message = error.map { "注销失败：\($0)" } ?? "账号已注销" }
            }
        }
    }
}

struct NativeSyncView: View {
    @ObservedObject var model: NativeAppModel
    @State private var message: String?

    var body: some View {
        List {
            Section("同步状态") {
                if let status = model.syncStatus {
                    LabeledContent("待同步", value: String(status.pendingCount))
                    LabeledContent("失败", value: String(status.failedCount))
                    LabeledContent("冲突", value: String(status.conflictCount))
                    LabeledContent("Bangumi", value: status.bangumiLinked ? "已连接" : "未连接")
                    if let last = status.lastSuccessfulAt {
                        LabeledContent("上次成功", value: last.prefix(16).description)
                    }
                } else {
                    ProgressView("正在加载同步状态")
                }
                Button("立即同步") {
                    model.startSync { error in
                        message = error.map { "同步失败：\($0)" } ?? "同步请求已提交"
                        model.loadSyncStatus()
                        model.loadSyncConflicts()
                    }
                }
                if let message { Text(message).font(.footnote).foregroundStyle(.secondary) }
            }

            Section("冲突处理") {
                if model.syncConflicts.isEmpty {
                    Text("没有待处理冲突")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(model.syncConflicts) { conflict in
                        VStack(alignment: .leading, spacing: 8) {
                            Text("作品 \(conflict.subjectId) · \(conflict.fieldName)").font(.headline)
                            Text("本地：\(conflict.localValue)").font(.caption).foregroundStyle(.secondary)
                            Text("远端：\(conflict.remoteValue)").font(.caption).foregroundStyle(.secondary)
                            HStack {
                                Button("保留本地") { resolve(conflict, choice: "keep_local") }
                                Button("使用远端") { resolve(conflict, choice: "use_remote") }
                                Button("稍后") { resolve(conflict, choice: "later") }
                            }
                            .font(.caption.weight(.semibold))
                        }
                        .padding(.vertical, 5)
                    }
                }
            }
        }
        .scrollIndicators(.hidden)
        .navigationTitle("Bangumi 同步")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            model.loadSyncStatus()
            model.loadSyncConflicts()
        }
        .refreshable {
            await withCheckedContinuation { continuation in
                model.loadSyncStatus { _, _ in
                    model.loadSyncConflicts { _, _ in continuation.resume() }
                }
            }
        }
    }

    private func resolve(_ conflict: NativeSyncConflictSnapshot, choice: String) {
        model.resolveSyncConflict(id: conflict.id, expectedVersion: conflict.localVersion, choice: choice) { error in
            message = error.map { "处理失败：\($0)" } ?? "冲突已处理"
            model.loadSyncStatus()
            model.loadSyncConflicts()
        }
    }
}

struct NativeDiagnosticsView: View {
    @ObservedObject var model: NativeAppModel

    var body: some View {
        List {
            Section {
                Button {
                    model.loadDiagnostics()
                } label: {
                    if model.isLoadingDiagnostics {
                        HStack {
                            ProgressView()
                            Text("测试中…")
                        }
                    } else {
                        Text("重新测试")
                    }
                }
                .disabled(model.isLoadingDiagnostics)
            }
            Section("服务") {
                ForEach(model.diagnostics) { item in
                    HStack {
                        Image(systemName: item.healthy ? "checkmark.circle.fill" : "xmark.octagon.fill")
                            .foregroundStyle(item.healthy ? .green : .red)
                        VStack(alignment: .leading) {
                            Text(item.endpoint).font(.headline)
                            Text(diagnosticStatusText(item))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        .scrollIndicators(.hidden)
        .navigationTitle("服务诊断")
        .task { if model.diagnostics.isEmpty { model.loadDiagnostics() } }
    }

    private func diagnosticStatusText(_ item: NativeDiagnosticSnapshot) -> String {
        let latency = item.latencyMs.map { "\($0) ms" } ?? "无响应"
        if item.healthy {
            return "\(latency) · HTTP \(item.statusCode.map(String.init) ?? "—")"
        }
        if let error = item.errorMessage, !error.isEmpty {
            return "\(latency) · \(error)"
        }
        return "\(latency) · HTTP \(item.statusCode.map(String.init) ?? "—")"
    }
}

private struct NativeReviewComposer: View {
    let subjectId: Int64
    @ObservedObject var model: NativeAppModel
    let onComplete: (NativeReviewSnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var reviewBody = ""
    @State private var spoiler = false
    @State private var visibility = "public"
    @State private var isSubmitting = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("你的感受") {
                    TextEditor(text: $reviewBody)
                        .frame(minHeight: 160)
                    Toggle("包含剧透", isOn: $spoiler)
                    Picker("可见性", selection: $visibility) {
                        Text("公开").tag("public")
                        Text("仅自己").tag("private")
                    }
                }
                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundStyle(.red)
                    }
                }
                Section {
                    Button(isSubmitting ? "发布中…" : "发布评价") {
                        submit()
                    }
                        .disabled(isSubmitting || reviewBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .navigationTitle("写评价")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
            }
        }
    }

    private func submit() {
        let normalizedBody = reviewBody.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedBody.isEmpty else { return }
        guard !isSubmitting else { return }
        isSubmitting = true
        errorMessage = nil
        model.createReviewAdvanced(
            subjectId: subjectId,
            kind: "short",
            title: nil,
            body: normalizedBody,
            spoiler: spoiler,
            visibility: visibility,
        ) { review, error in
            isSubmitting = false
            if let review {
                onComplete(review, nil)
                dismiss()
            } else {
                errorMessage = error ?? "评价发布失败"
                onComplete(nil, errorMessage)
            }
        }
    }
}

private struct NativeAccountSheet: View {
    @ObservedObject var model: NativeAppModel
    @Environment(\.dismiss) private var dismiss
    @State private var username = ""
    @State private var password = ""
    @State private var displayName = ""
    @State private var isRegistering = false
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("Anime 账号") {
                    TextField("用户名", text: $username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField("密码", text: $password)
                    if isRegistering { TextField("显示名称", text: $displayName) }
                }
                Section {
                    Button(isLoading ? "提交中…" : (isRegistering ? "注册并登录" : "登录")) {
                        submit()
                    }
                    .disabled(isLoading || username.isEmpty || password.isEmpty || (isRegistering && displayName.isEmpty))
                    Button(isRegistering ? "已有账号，直接登录" : "没有账号，注册") {
                        isRegistering.toggle()
                    }
                }
                Section {
                    Button("使用 Bangumi 登录") {
                        model.beginBangumiLogin { errorMessage = $0 }
                    }
                }
                if let errorMessage {
                    Section { Text(errorMessage).foregroundStyle(.red) }
                }
            }
            .navigationTitle("账号")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
            }
        }
    }

    private func submit() {
        isLoading = true
        errorMessage = nil
        let finish: (String?) -> Void = { error in
            isLoading = false
            if let error { errorMessage = error } else { dismiss() }
        }
        if isRegistering {
            model.registerAnime(username: username, password: password, displayName: displayName, completion: finish)
        } else {
            model.loginWithAnime(username: username, password: password, completion: finish)
        }
    }
}

private struct NativeCatalogCard: View {
    let subject: NativeSubjectSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            NativeRemoteImage(url: subject.posterURL)
                .frame(height: 218)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            Text(subject.title)
                .font(.headline)
                .lineLimit(2)
            HStack(spacing: 5) {
                Image(systemName: "star.fill")
                    .foregroundStyle(.orange)
                Text(subject.rating.map { String(format: "%.1f", $0) } ?? "暂无评分")
            }
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
        }
    }
}

private struct NativeCollectionCard: View {
    let item: NativeCollectionItemSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            NativeRemoteImage(url: item.posterURL)
                .frame(height: 218)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            Text(item.title)
                .font(.headline)
                .lineLimit(2)
            HStack {
                Text(item.status.displayName)
                Spacer()
                Text("\(item.episodeProgress)/\(item.totalEpisodes)")
            }
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
        }
    }
}

private struct NativeActivityCard: View {
    let item: NativeActivityItemSnapshot
    let destination: AnyView?

    var body: some View {
        Group {
            if let destination {
                NavigationLink { destination } label: { content(subject: item.subjectSummary) }
            } else {
                content(subject: item.subjectSummary)
            }
        }
        .buttonStyle(.plain)
    }

    private func content(subject: NativeSubjectSummary?) -> some View {
        HStack(alignment: .top, spacing: 12) {
            NativeAvatar(url: item.actorAvatarUrl.flatMap(URL.init(string:)), name: item.actorName)
            VStack(alignment: .leading, spacing: 7) {
                Text(item.actorName).font(.headline)
                Text(item.summary).foregroundStyle(.primary)
                Text(item.occurredAt.replacingOccurrences(of: "T", with: " ").prefix(16))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if let subject {
                    Label(subject.title, systemImage: "film")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.tint)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct NativeNotificationRow: View {
    let notification: NativeNotificationSnapshot
    let destination: AnyView?
    let onRead: () -> Void

    var body: some View {
        Group {
            if let destination {
                NavigationLink {
                    destination
                } label: {
                    content
                }
                .simultaneousGesture(TapGesture().onEnded { _ in onRead() })
            } else {
                Button(action: onRead) {
                    content
                }
            }
        }
        .buttonStyle(.plain)
    }

    private var content: some View {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: notification.readAt == nil ? "bell.badge.fill" : "bell")
                    .foregroundStyle(notification.readAt == nil ? Color.accentColor : Color.secondary)
                VStack(alignment: .leading, spacing: 5) {
                    Text(notification.title).font(.headline)
                    Text(notification.createdAt.replacingOccurrences(of: "T", with: " ").prefix(16))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            .padding(16)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct NativeInlineError: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("加载失败", systemImage: "exclamationmark.triangle")
                .font(.headline)
            Text(message).font(.subheadline).foregroundStyle(.secondary)
            Button("重试", action: retry).buttonStyle(.borderedProminent)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct NativeRemoteImage: View {
    let url: URL?

    var body: some View {
        AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image): image.resizable().scaledToFill()
            case .failure:
                ZStack {
                    Color.secondary.opacity(0.14)
                    Image(systemName: "photo").foregroundStyle(.secondary)
                }
            default: Color.secondary.opacity(0.12)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }
}

private struct NativeAvatar: View {
    let url: URL?
    let name: String?

    var body: some View {
        NativeRemoteImage(url: url)
            .overlay {
                if url == nil {
                    Text(String(name?.first ?? "A"))
                        .font(.headline)
                        .foregroundStyle(.white)
                }
            }
            .frame(width: 48, height: 48)
            .background(Color.accentColor.gradient)
            .clipShape(Circle())
    }
}

private struct NativeMetric: View {
    let value: Int
    let title: String

    var body: some View {
        VStack(spacing: 4) {
            Text(String(value)).font(.title3.weight(.bold))
            Text(title).font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct NativeActionRow: View {
    let title: String
    let subtitle: String
    let systemImage: String
    var tint: Color = .accentColor

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.title3)
                .foregroundStyle(tint)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(.headline)
                Text(subtitle).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(.tertiary)
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private extension NativeSubjectSummary {
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

private extension NativeCollectionItemSnapshot {
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

private extension NativeActivityItemSnapshot {
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

private extension NativeNotificationSnapshot {
    var title: String {
        let actor = actorName ?? "有人"
        switch kind {
        case "review_liked": return "\(actor) 赞了你的评价"
        case "comment_liked": return "\(actor) 赞了你的评论"
        case "comment_replied": return "\(actor) 回复了你的评论"
        case "followed": return "\(actor) 关注了你"
        default: return "\(actor) 与你产生了互动"
        }
    }
}

private extension NativeSessionSnapshot {
    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
}

private extension String {
    var displayName: String {
        switch self {
        case "Wish", "wish", "想看": return "想看"
        case "Watching", "watching", "在看": return "在看"
        case "Completed", "completed", "看过": return "看过"
        case "OnHold", "on_hold", "onhold", "搁置": return "搁置"
        case "Dropped", "dropped", "抛弃": return "抛弃"
        default: return self
        }
    }

    var feedDisplayName: String {
        switch self {
        case "following": return "关注"
        case "popular": return "热门"
        case "public": return "全站"
        default: return self
        }
    }

    var searchTypeName: String {
        switch self {
        case "tv": return "TV"
        case "web": return "Web"
        case "ova": return "OVA"
        case "movie": return "剧场版"
        case "other": return "其他"
        default: return self
        }
    }

    var searchAiringName: String {
        switch self {
        case "announced": return "未开播"
        case "airing": return "连载中"
        case "finished": return "已完结"
        default: return self
        }
    }

    var searchSortName: String {
        switch self {
        case "rating": return "评分优先"
        case "updated": return "最近更新"
        default: return "相关度"
        }
    }
}
