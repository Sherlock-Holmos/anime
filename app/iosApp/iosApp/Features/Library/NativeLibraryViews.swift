import SwiftUI
import Foundation
import PhotosUI
import UIKit

struct NativeLibraryView: View {
    @ObservedObject var model: NativeAppModel
    @State private var query = ""
    @State private var results: [NativeSubjectSummary] = []
    @State private var nextCursor: String?
    @State private var isSearchPresented = false
    @State private var hasSubmittedSearch = false
    @State private var isLoadingResults = false
    @State private var errorMessage: String?
    @State private var selectedType = "all"
    @State private var selectedAiring = "all"
    @State private var selectedSort = "relevance"
    @State private var yearStartText = ""
    @State private var yearEndText = ""
    @State private var showingFilters = false
    @State private var recentSearches: [String] = []
    @State private var requestGeneration = 0
    @State private var requestedCursors = Set<String>()
    @State private var exploreGeneration = 0
    @State private var rankingItems: [NativeSubjectSummary] = []
    @State private var seasonalItems: [NativeSubjectSummary] = []
    @State private var isLoadingExploreRails = false
    @State private var exploreErrorMessage: String?
    @State private var currentBrowseMode: NativeLibraryBrowseMode?
    @State private var currentSectionID: String?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private static let recentSearchesKey = "anime.ios.library.recentSearches"

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 24) {
                    if hasSubmittedSearch {
                        resultContent
                    } else if isSearchPresented {
                        searchLandingContent
                    } else {
                        exploreHomeContent
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 112)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .searchable(
                text: $query,
                isPresented: $isSearchPresented,
                placement: .navigationBarDrawer(displayMode: .automatic),
                prompt: AnimeL10n.key(.searchPrompt),
            )
            .onSubmit(of: .search, submitSearch)
            .refreshable { await refreshExploreContent() }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        if hasSubmittedSearch && !isSearchPresented {
                            showingFilters = true
                        } else if isSearchPresented {
                            showingFilters = true
                        } else {
                            isSearchPresented = true
                        }
                    } label: {
                        Image(systemName: isSearchPresented || hasSubmittedSearch ? "line.3.horizontal.decrease.circle" : "magnifyingglass")
                    }
                    .accessibilityLabel(AnimeL10n.key(isSearchPresented || hasSubmittedSearch ? .searchFilters : .search))
                }
            }
            .navigationDestination(for: NativeSubjectSummary.self) { subject in
                NativeSubjectDetailView(summary: subject, model: model)
            }
        }
        .task {
            model.start()
            if model.searchDiscovery == nil { model.loadSearchDiscovery() }
            if model.discovery == nil { model.refresh() }
            loadRecentSearches()
            loadExploreRails()
        }
        .onChange(of: isSearchPresented) { _, presented in
            if presented {
                hasSubmittedSearch = false
            } else if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                hasSubmittedSearch = false
            }
        }
        .sheet(isPresented: $showingFilters) {
            NativeSearchFiltersSheet(
                selectedType: $selectedType,
                selectedAiring: $selectedAiring,
                selectedSort: $selectedSort,
                yearStartText: $yearStartText,
                yearEndText: $yearEndText,
            ) {
                if canRunQuery {
                    submitSearch()
                }
            }
            .presentationDetents([.medium, .large])
        }
    }

    @ViewBuilder
    private var exploreHomeContent: some View {
        VStack(alignment: .leading, spacing: 28) {
            Text(AnimeL10n.key(.library))
                .font(.largeTitle.weight(.bold))
                .accessibilityAddTraits(.isHeader)

            browseEntrySection

            if !themeSections.isEmpty {
                librarySectionHeader(.libraryThemes)
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 14) {
                        ForEach(themeSections) { section in
                            Button {
                                showSection(section)
                            } label: {
                                NativeLibraryThemeCard(section: section, reduceTransparency: reduceTransparency)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(section.title)
                        }
                    }
                }
            }

            if !rankingItems.isEmpty || isLoadingExploreRails {
                libraryRail(
                    title: .libraryRanking,
                    subjects: rankingItems,
                    isLoading: isLoadingExploreRails,
                    mode: .rating,
                )
            }

            if !seasonalItems.isEmpty || isLoadingExploreRails {
                libraryRail(
                    title: .librarySeason,
                    subjects: seasonalItems,
                    isLoading: isLoadingExploreRails,
                    mode: .seasonal,
                )
            }

            if let topRated = section(withID: "top-rated"), !topRated.subjects.isEmpty {
                libraryRail(title: .libraryHighRated, subjects: topRated.subjects, isLoading: false, mode: .section(topRated.id))
            }

            if let discovery = model.searchDiscovery, !discovery.recommendations.isEmpty {
                libraryRail(title: .libraryCurated, subjects: discovery.recommendations, isLoading: false, mode: .curated)
            }

            if let exploreErrorMessage {
                NativeLibraryInlineError(message: exploreErrorMessage) { loadExploreRails() }
            }
        }
    }

    private var browseEntrySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(AnimeL10n.key(.libraryBrowse))
                .font(.title2.weight(.bold))
                .accessibilityAddTraits(.isHeader)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                browseEntry(.type, systemImage: "square.grid.2x2") {
                    showingFilters = true
                }
                if themeSections.isEmpty {
                    browseEntry(.topics, systemImage: "rectangle.stack") { }
                        .disabled(true)
                } else {
                    NavigationLink {
                        NativeLibraryTopicListView(sections: themeSections, model: model)
                    } label: {
                        NativeLibraryBrowseEntryLabel(kind: .topics, systemImage: "rectangle.stack")
                    }
                    .buttonStyle(.plain)
                }
                browseEntry(.rankings, systemImage: "chart.bar.fill") { browseRating() }
                browseEntry(.seasonal, systemImage: "calendar") { browseSeasonal() }
            }
        }
    }

    private func browseEntry(
        _ kind: NativeLibraryBrowseEntryKind,
        systemImage: String,
        action: @escaping () -> Void,
    ) -> some View {
        Button(action: action) {
            NativeLibraryBrowseEntryLabel(kind: kind, systemImage: systemImage)
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func librarySectionHeader(_ key: AnimeL10n.Key) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(AnimeL10n.key(key))
                .font(.title2.weight(.bold))
                .accessibilityAddTraits(.isHeader)
            Spacer()
            if key == .libraryThemes {
                NavigationLink {
                    NativeLibraryTopicListView(sections: themeSections, model: model)
                } label: {
                    Text(AnimeL10n.key(.seeAll))
                        .font(.subheadline.weight(.semibold))
                }
                .accessibilityLabel(AnimeL10n.key(.seeAll))
            }
        }
    }

    private func libraryRail(
        title: AnimeL10n.Key,
        subjects: [NativeSubjectSummary],
        isLoading: Bool,
        mode: NativeLibraryBrowseMode,
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text(AnimeL10n.key(title))
                    .font(.title2.weight(.bold))
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                Button(AnimeL10n.key(.seeAll)) { browse(mode) }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.tint)
            }
            if isLoading && subjects.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity, minHeight: 112)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(alignment: .top, spacing: 14) {
                        ForEach(subjects) { subject in
                            NavigationLink(value: subject) {
                                NativeLibrarySubjectCard(subject: subject)
                                    .frame(width: 132)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    private var searchLandingContent: some View {
        VStack(alignment: .leading, spacing: 24) {
            if !recentSearches.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text(AnimeL10n.key(.libraryRecentSearches))
                            .font(.title3.weight(.bold))
                            .accessibilityAddTraits(.isHeader)
                        Spacer()
                        Button(AnimeL10n.key(.libraryClearRecent)) { clearRecentSearches() }
                            .font(.footnote.weight(.semibold))
                    }
                    FlowLayout(items: recentSearches) { term in
                        Button(term) {
                            query = term
                            submitSearch()
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }

            if let discovery = model.searchDiscovery, !discovery.trending.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text(AnimeL10n.key(.searchTrending))
                        .font(.title3.weight(.bold))
                        .accessibilityAddTraits(.isHeader)
                    FlowLayout(items: discovery.trending) { term in
                        Button(term) {
                            query = term
                            submitSearch()
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }

            VStack(alignment: .leading, spacing: 12) {
                Text(AnimeL10n.key(.libraryQuickExplore))
                    .font(.title3.weight(.bold))
                    .accessibilityAddTraits(.isHeader)
                HStack(spacing: 10) {
                    browseEntry(.type, systemImage: "square.grid.2x2") { showingFilters = true }
                    browseEntry(.rankings, systemImage: "chart.bar.fill") { browseRating() }
                    browseEntry(.seasonal, systemImage: "calendar") { browseSeasonal() }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 8)
    }

    private var resultContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text(resultHeading)
                    .font(.title2.weight(.bold))
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                if hasActiveFilters {
                    Text(activeFilterSummary)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                }
            }
            if isLoadingResults && results.isEmpty {
                ProgressView(AnimeL10n.key(.searchLoading))
                    .frame(maxWidth: .infinity, minHeight: 180)
            } else if results.isEmpty {
                ContentUnavailableView(
                    AnimeL10n.key(.librarySearchEmpty),
                    systemImage: "magnifyingglass",
                    description: Text(AnimeL10n.key(.librarySearchEmptyDescription)),
                )
                .frame(maxWidth: .infinity, minHeight: 220)
            } else {
                LazyVGrid(columns: posterGridColumns, spacing: 20) {
                    ForEach(results) { subject in
                        NavigationLink(value: subject) {
                            NativeLibrarySubjectCard(subject: subject)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            if isLoadingResults && !results.isEmpty {
                ProgressView(AnimeL10n.key(.libraryLoadingMore))
                    .frame(maxWidth: .infinity)
            } else if let nextCursor, !isLoadingResults {
                Button(AnimeL10n.key(.actionLoadMore)) { loadMore(cursor: nextCursor) }
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.bordered)
            }
            if let errorMessage {
                NativeLibraryInlineError(message: errorMessage) {
                    if let nextCursor { loadMore(cursor: nextCursor) } else { submitSearch() }
                }
            }
        }
    }

    private var resultHeading: String {
        if !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return AnimeL10n.string(.searchResults)
        }
        return browseHeading
    }

    private var browseHeading: String {
        if let sectionID = currentSectionID, let section = section(withID: sectionID) { return section.title }
        if currentBrowseMode == .rating { return AnimeL10n.string(.libraryRanking) }
        if currentBrowseMode == .seasonal { return AnimeL10n.string(.librarySeason) }
        if currentBrowseMode == .curated { return AnimeL10n.string(.libraryCurated) }
        return AnimeL10n.string(.searchResults)
    }

    private func submitSearch() {
        guard canRunQuery else { return }
        let normalized = query.trimmingCharacters(in: .whitespacesAndNewlines)
        currentBrowseMode = nil
        currentSectionID = nil
        hasSubmittedSearch = true
        isSearchPresented = true
        runQuery(query: normalized) { snapshot, error in
            if error == nil, !normalized.isEmpty {
                saveRecentSearch(normalized)
                model.saveSearchHistory(query: normalized)
            }
        }
    }

    private func runQuery(
        query: String,
        onFinish: @escaping (NativeSearchResultsSnapshot?, String?) -> Void = { _, _ in },
    ) {
        requestGeneration += 1
        let generation = requestGeneration
        requestedCursors.removeAll()
        results = []
        nextCursor = nil
        errorMessage = nil
        isLoadingResults = true
        model.search(
            query: query,
            typesCsv: typeQueryValue,
            yearStart: Int(yearStartText),
            yearEnd: Int(yearEndText),
            airingCsv: airingQueryValue,
            sort: selectedSort,
        ) { snapshot, error in
            guard generation == requestGeneration else { return }
            if let snapshot {
                results = deduplicated(results + snapshot.items)
                nextCursor = snapshot.nextCursor
            }
            errorMessage = error
            isLoadingResults = false
            onFinish(snapshot, error)
        }
    }

    private func loadMore(cursor: String) {
        guard !isLoadingResults, cursor == nextCursor, !requestedCursors.contains(cursor) else { return }
        requestedCursors.insert(cursor)
        let generation = requestGeneration
        isLoadingResults = true
        errorMessage = nil
        model.search(
            query: query.trimmingCharacters(in: .whitespacesAndNewlines),
            cursor: cursor,
            typesCsv: typeQueryValue,
            yearStart: Int(yearStartText),
            yearEnd: Int(yearEndText),
            airingCsv: airingQueryValue,
            sort: selectedSort,
        ) { snapshot, error in
            guard generation == requestGeneration else { return }
            if let snapshot {
                results = deduplicated(results + snapshot.items)
                nextCursor = snapshot.nextCursor
            }
            errorMessage = error
            isLoadingResults = false
        }
    }

    private func browse(_ mode: NativeLibraryBrowseMode) {
        switch mode {
        case .rating: browseRating()
        case .seasonal: browseSeasonal()
        case .curated:
            if let recommendations = model.searchDiscovery?.recommendations {
                results = recommendations
                nextCursor = nil
                currentBrowseMode = .curated
                currentSectionID = nil
                hasSubmittedSearch = true
                isSearchPresented = false
            }
        case let .section(id):
            if let section = section(withID: id) { showSection(section) }
        }
    }

    private func browseRating() {
        query = ""
        selectedType = "all"
        selectedAiring = "all"
        selectedSort = "rating"
        yearStartText = ""
        yearEndText = ""
        currentBrowseMode = .rating
        currentSectionID = nil
        hasSubmittedSearch = true
        isSearchPresented = false
        runQuery(query: "")
    }

    private func browseSeasonal() {
        query = ""
        selectedType = "all"
        selectedAiring = "airing"
        selectedSort = "updated"
        let year = Calendar.current.component(.year, from: Date())
        yearStartText = String(year)
        yearEndText = String(year)
        currentBrowseMode = .seasonal
        currentSectionID = nil
        hasSubmittedSearch = true
        isSearchPresented = false
        runQuery(query: "")
    }

    private func showSection(_ section: NativeDiscoverySection) {
        query = ""
        results = section.subjects
        nextCursor = nil
        errorMessage = nil
        currentBrowseMode = .section(section.id)
        currentSectionID = section.id
        hasSubmittedSearch = true
        isSearchPresented = false
    }

    private func refreshExploreContent() async {
        model.refresh(force: true)
        await withCheckedContinuation { continuation in
            model.loadSearchDiscovery { _ in continuation.resume() }
        }
        loadExploreRails()
    }

    private func loadExploreRails() {
        exploreGeneration += 1
        let generation = exploreGeneration
        isLoadingExploreRails = true
        exploreErrorMessage = nil
        let year = Calendar.current.component(.year, from: Date())
        var completed = 0
        var firstError: String?
        let finish: (String?) -> Void = { error in
            guard generation == exploreGeneration else { return }
            completed += 1
            if firstError == nil { firstError = error }
            if completed == 2 {
                isLoadingExploreRails = false
                exploreErrorMessage = firstError
            }
        }
        model.search(query: "", sort: "rating") { snapshot, error in
            guard generation == exploreGeneration else { return }
            if let snapshot { rankingItems = deduplicated(snapshot.items) }
            finish(error)
        }
        model.search(query: "", yearStart: year, yearEnd: year, airingCsv: "airing", sort: "updated") { snapshot, error in
            guard generation == exploreGeneration else { return }
            if let snapshot { seasonalItems = deduplicated(snapshot.items) }
            finish(error)
        }
    }

    private func loadRecentSearches() {
        recentSearches = UserDefaults.standard.stringArray(forKey: Self.recentSearchesKey) ?? []
    }

    private func saveRecentSearch(_ value: String) {
        recentSearches = ([value] + recentSearches.filter { $0.caseInsensitiveCompare(value) != .orderedSame }).prefix(10).map { $0 }
        UserDefaults.standard.set(recentSearches, forKey: Self.recentSearchesKey)
    }

    private func clearRecentSearches() {
        recentSearches = []
        UserDefaults.standard.removeObject(forKey: Self.recentSearchesKey)
    }

    private func deduplicated(_ values: [NativeSubjectSummary]) -> [NativeSubjectSummary] {
        var seen = Set<Int64>()
        return values.filter { seen.insert($0.id).inserted }
    }

    private var themeSections: [NativeDiscoverySection] { model.discovery?.sections ?? [] }
    private var canRunQuery: Bool {
        !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || hasActiveFilters
    }
    private var posterGridColumns: [GridItem] {
        dynamicTypeSize.isAccessibilitySize ? [GridItem(.flexible())] : [GridItem(.flexible()), GridItem(.flexible())]
    }
    private func section(withID id: String) -> NativeDiscoverySection? { themeSections.first { $0.id == id } }
    private var typeQueryValue: String? { selectedType == "all" ? nil : selectedType }
    private var airingQueryValue: String? { selectedAiring == "all" ? nil : selectedAiring }
    private var hasActiveFilters: Bool {
        selectedType != "all" || selectedAiring != "all" || selectedSort != "relevance" || !yearStartText.isEmpty || !yearEndText.isEmpty
    }
    private var activeFilterSummary: String {
        var values: [String] = []
        if selectedType != "all" { values.append(selectedType.searchTypeName) }
        if selectedAiring != "all" { values.append(selectedAiring.searchAiringName) }
        if selectedSort != "relevance" { values.append(selectedSort.searchSortName) }
        if !yearStartText.isEmpty || !yearEndText.isEmpty { values.append(AnimeL10n.string(.filterYear)) }
        return values.joined(separator: " · ")
    }
}

private enum NativeLibraryBrowseEntryKind {
    case type, topics, rankings, seasonal

    var title: AnimeL10n.Key {
        switch self {
        case .type: return .libraryBrowseType
        case .topics: return .libraryBrowseTopics
        case .rankings: return .libraryBrowseRankings
        case .seasonal: return .libraryBrowseSeasonal
        }
    }
}

private enum NativeLibraryBrowseMode: Equatable {
    case rating, seasonal, curated, section(String)
}

private struct NativeLibraryBrowseEntryLabel: View {
    let kind: NativeLibraryBrowseEntryKind
    let systemImage: String

    var body: some View {
        Label(AnimeL10n.key(kind.title), systemImage: systemImage)
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 54, alignment: .leading)
            .padding(.horizontal, 14)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(.quaternary, lineWidth: 0.7)
            }
    }
}

private struct NativeLibraryThemeCard: View {
    let section: NativeDiscoverySection
    let reduceTransparency: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 4) {
                ForEach(section.subjects.prefix(4)) { subject in
                    NativePosterImage(url: subject.posterURL, width: 56, height: 74, cornerRadius: 8)
                }
            }
            Text(section.title)
                .font(.headline)
                .lineLimit(2)
            Text(AnimeL10n.string(.librarySectionCount, section.subjects.count))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .frame(width: 280, alignment: .leading)
        .background(
            reduceTransparency ? AnyShapeStyle(Color(uiColor: .secondarySystemGroupedBackground)) : AnyShapeStyle(.regularMaterial),
            in: RoundedRectangle(cornerRadius: 20, style: .continuous),
        )
    }
}

private struct NativeLibrarySubjectCard: View {
    let subject: NativeSubjectSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            NativePosterImage(url: subject.posterURL, aspectRatio: 3 / 4, cornerRadius: 14)
            Text(subject.title)
                .font(.headline)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
            Text([subject.year.map(String.init), subject.type.displayName].compactMap { $0 }.joined(separator: " · "))
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            if let rating = subject.rating {
                Label(AnimeL10n.string(.subjectRating, String(format: "%.1f", rating)), systemImage: "star.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.orange)
            } else {
                Text(AnimeL10n.key(.subjectNoRating))
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(subject.title)
    }
}

private struct NativeLibraryInlineError: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            Text(message).font(.footnote).foregroundStyle(.secondary)
            Spacer()
            Button(AnimeL10n.key(.actionRetry)) { retry() }
                .font(.footnote.weight(.semibold))
        }
        .padding(12)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct FlowLayout<Data: RandomAccessCollection, Content: View>: View where Data.Element: Hashable {
    let items: Data
    let content: (Data.Element) -> Content

    init(items: Data, @ViewBuilder content: @escaping (Data.Element) -> Content) {
        self.items = items
        self.content = content
    }

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 96), alignment: .leading)],
            alignment: .leading,
            spacing: 8,
        ) {
            ForEach(items, id: \.self) { item in content(item) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct NativeLibraryTopicListView: View {
    let sections: [NativeDiscoverySection]
    @ObservedObject var model: NativeAppModel

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                ForEach(sections) { section in
                    NavigationLink {
                        NativeLibrarySectionView(section: section, model: model)
                    } label: {
                        NativeLibraryThemeCard(section: section, reduceTransparency: false)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
            .padding(.bottom, 32)
        }
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(AnimeL10n.key(.libraryBrowseTopics))
        .navigationBarTitleDisplayMode(.large)
    }
}

private struct NativeLibrarySectionView: View {
    let section: NativeDiscoverySection
    @ObservedObject var model: NativeAppModel

    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 20) {
                ForEach(section.subjects) { subject in
                    NavigationLink {
                        NativeSubjectDetailView(summary: subject, model: model)
                    } label: {
                        NativeLibrarySubjectCard(subject: subject)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
            .padding(.bottom, 32)
        }
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(section.title)
        .navigationBarTitleDisplayMode(.large)
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
                Section(AnimeL10n.key(.searchFilters)) {
                    Picker(AnimeL10n.key(.filterType), selection: $selectedType) {
                        Text(AnimeL10n.key(.filterAllTypes)).tag("all")
                        Text(AnimeL10n.key(.subjectTypeTv)).tag("tv")
                        Text(AnimeL10n.key(.subjectTypeWeb)).tag("web")
                        Text(AnimeL10n.key(.subjectTypeOva)).tag("ova")
                        Text(AnimeL10n.key(.filterMovie)).tag("movie")
                        Text(AnimeL10n.key(.filterOther)).tag("other")
                    }
                    Picker(AnimeL10n.key(.filterAiring), selection: $selectedAiring) {
                        Text(AnimeL10n.key(.filterAllStatuses)).tag("all")
                        Text(AnimeL10n.key(.filterAnnounced)).tag("announced")
                        Text(AnimeL10n.key(.filterCurrentlyAiring)).tag("airing")
                        Text(AnimeL10n.key(.filterFinished)).tag("finished")
                    }
                    Picker(AnimeL10n.key(.filterSort), selection: $selectedSort) {
                        Text(AnimeL10n.key(.sortRelevance)).tag("relevance")
                        Text(AnimeL10n.key(.sortRating)).tag("rating")
                        Text(AnimeL10n.key(.sortUpdated)).tag("updated")
                    }
                }

                Section(AnimeL10n.key(.filterYearRange)) {
                    TextField(AnimeL10n.key(.filterYearStart), text: $yearStartText)
                        .keyboardType(.numberPad)
                    TextField(AnimeL10n.key(.filterYearEnd), text: $yearEndText)
                        .keyboardType(.numberPad)
                    if let validationMessage {
                        Text(validationMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    Button(AnimeL10n.key(.clearFilters)) {
                        selectedType = "all"
                        selectedAiring = "all"
                        selectedSort = "relevance"
                        yearStartText = ""
                        yearEndText = ""
                        validationMessage = nil
                    }
                }
            }
            .navigationTitle(AnimeL10n.key(.searchFilters))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AnimeL10n.key(.actionCancel)) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(AnimeL10n.key(.applyFilters)) { apply() }
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
            validationMessage = AnimeL10n.string(.validationYearNumber)
            return
        }
        guard (startValue == nil || startValue! >= 1900), (endValue == nil || endValue! >= 1900) else {
            validationMessage = AnimeL10n.string(.validationYearMinimum)
            return
        }
        guard startValue == nil || endValue == nil || startValue! <= endValue! else {
            validationMessage = AnimeL10n.string(.validationYearOrder)
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
                        LazyVGrid(columns: posterGridColumns, spacing: 18) {
                            ForEach(page.items) { item in
                                NavigationLink {
                                    NativeSubjectDetailView(summary: item.subjectSummary, model: model)
                                } label: {
                                    NativeCollectionCard(item: item)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .id(selectedStatus)
                        if let cursor = page.nextCursor {
                            Button(AnimeL10n.key(.actionLoadMore)) {
                                model.loadCollection(status: selectedStatus, cursor: cursor, append: true) { _, error in
                                    errorMessage = error
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .buttonStyle(.bordered)
                        }
                    } else if !model.isSessionReady || model.isLoadingCollection {
                        ProgressView(AnimeL10n.key(.libraryRestoring))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    } else {
                        ContentUnavailableView(AnimeL10n.key(.libraryEmpty), systemImage: "books.vertical", description: Text(AnimeL10n.key(.libraryEmptyDescription)))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .navigationTitle(AnimeL10n.key(.libraryTitle))
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

    private func loadCollection(_ status: String) {
        errorMessage = nil
        model.loadCollection(status: status) { _, error in
            errorMessage = error
        }
    }

    private let posterGridColumns = [GridItem(.flexible()), GridItem(.flexible())]
}

