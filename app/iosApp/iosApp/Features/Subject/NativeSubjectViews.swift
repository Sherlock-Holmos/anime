import SwiftUI
import Foundation
import PhotosUI
import UIKit

struct NativeSubjectSectionsView: View {
    let subjectId: Int64
    @ObservedObject var model: NativeAppModel
    @State private var selectedSection = "episodes"
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 14) {
                Picker(AnimeL10n.key(.subjectMaterial), selection: $selectedSection) {
                    Text(AnimeL10n.key(.subjectEpisodes)).tag("episodes")
                    Text(AnimeL10n.key(.subjectCharacters)).tag("characters")
                    Text(AnimeL10n.key(.subjectRelations)).tag("relations")
                }
                .pickerStyle(.segmented)

                if let errorMessage {
                    NativeInlineError(message: errorMessage) { load(force: true) }
                } else if let sections = model.subjectSections[subjectId] {
                    sectionContent(sections)
                } else {
                    ProgressView(AnimeL10n.key(.subjectLoadingMaterial))
                        .frame(maxWidth: .infinity, minHeight: 220)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(AnimeL10n.key(.subjectDetails))
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
        .refreshable { await refresh() }
    }

    @ViewBuilder
    private func sectionContent(_ sections: NativeSubjectSectionsSnapshot) -> some View {
        switch selectedSection {
        case "characters":
            if sections.characters.isEmpty {
                ContentUnavailableView(AnimeL10n.key(.subjectNoCharacters), systemImage: "person.2")
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
                                Text(AnimeL10n.string(.subjectVoiceActor, character.actors.map(\.name).joined(separator: "、")))
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
                ContentUnavailableView(AnimeL10n.key(.subjectNoRelations), systemImage: "link")
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
                ContentUnavailableView(AnimeL10n.key(.subjectNoEpisodes), systemImage: "list.number")
            } else {
                ForEach(sections.episodes) { episode in
                    HStack(spacing: 12) {
                        Text(episode.number.map { String(format: "%g", $0) } ?? "—")
                            .font(.headline.monospacedDigit())
                            .frame(width: 42)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(episode.title ?? AnimeL10n.string(.subjectUnnamedEpisode))
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

