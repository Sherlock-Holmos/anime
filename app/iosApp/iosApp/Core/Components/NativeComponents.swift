import SwiftUI
import Foundation
import PhotosUI
import UIKit

struct NativeReviewComposer: View {
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
                Section(AnimeL10n.key(.formFeeling)) {
                    TextEditor(text: $reviewBody)
                        .frame(minHeight: 160)
                    Toggle(AnimeL10n.key(.formSpoiler), isOn: $spoiler)
                    Picker(AnimeL10n.key(.visibility), selection: $visibility) {
                        Text(AnimeL10n.key(.visibilityPublic)).tag("public")
                        Text(AnimeL10n.key(.visibilityPrivate)).tag("private")
                    }
                }
                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundStyle(.red)
                    }
                }
                Section {
                    Button(isSubmitting ? AnimeL10n.key(.formPosting) : AnimeL10n.key(.actionPublish)) {
                        submit()
                    }
                        .disabled(isSubmitting || reviewBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .navigationTitle(AnimeL10n.key(.writeReview))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AnimeL10n.key(.actionCancel)) { dismiss() }
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
                errorMessage = error ?? AnimeL10n.string(.reviewPublishFailed)
                onComplete(nil, errorMessage)
            }
        }
    }
}

struct NativeAccountSheet: View {
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
                Section(AnimeL10n.key(.bangumiAccount)) {
                    TextField(AnimeL10n.key(.accountUsername), text: $username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField(AnimeL10n.key(.accountPassword), text: $password)
                    if isRegistering { TextField(AnimeL10n.key(.accountDisplayName), text: $displayName) }
                }
                Section {
                    Button(isLoading ? AnimeL10n.key(.accountSubmitting) : (isRegistering ? AnimeL10n.key(.accountRegisterLogin) : AnimeL10n.key(.actionLogin))) {
                        submit()
                    }
                    .disabled(isLoading || username.isEmpty || password.isEmpty || (isRegistering && displayName.isEmpty))
                    Button(isRegistering ? AnimeL10n.key(.accountExistingLogin) : AnimeL10n.key(.accountNoAccountRegister)) {
                        isRegistering.toggle()
                    }
                }
                Section {
                    Button(AnimeL10n.key(.accountBangumiLogin)) {
                        model.beginBangumiLogin { errorMessage = $0 }
                    }
                }
                if let errorMessage {
                    Section { Text(errorMessage).foregroundStyle(.red) }
                }
            }
            .navigationTitle(AnimeL10n.key(.account))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(AnimeL10n.key(.accountClose)) { dismiss() } }
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

struct NativeCatalogCard: View {
    let subject: NativeSubjectSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            NativePosterImage(url: subject.posterURL, aspectRatio: 3 / 4, cornerRadius: 16)
            Text(subject.title)
                .font(.headline)
                .lineLimit(2)
            HStack(spacing: 5) {
                Image(systemName: "star.fill")
                    .foregroundStyle(.orange)
            Text(subject.rating.map { String(format: "%.1f", $0) } ?? AnimeL10n.string(.subjectNoRating))
            }
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct NativeCollectionCard: View {
    let item: NativeCollectionItemSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            NativePosterImage(url: item.posterURL, aspectRatio: 3 / 4, cornerRadius: 16)
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
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct NativeRatingRow: View {
    let rating: NativeProfileRatingSnapshot

    var body: some View {
        HStack(spacing: 12) {
            NativePosterImage(url: rating.posterURL, width: 56, height: 75, cornerRadius: 10)
            VStack(alignment: .leading, spacing: 6) {
                Text(rating.title).font(.headline).lineLimit(2)
                Label("\(rating.score) / 10", systemImage: "star.fill")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.orange)
                if !rating.tags.isEmpty {
                    Text(rating.tags.joined(separator: " · "))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
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

struct NativeActivityCard: View {
    let item: NativeActivityItemSnapshot
    let destination: AnyView?
    let onDelete: (() -> Void)?

    var body: some View {
        Group {
            if let destination {
                NavigationLink { destination } label: { content(subject: item.subjectSummary) }
            } else {
                content(subject: item.subjectSummary)
            }
        }
        .buttonStyle(.plain)
        .contextMenu {
            if let onDelete {
                Button(AnimeL10n.key(.activityWithdraw), role: .destructive, action: onDelete)
            }
        }
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

struct NativeNotificationRow: View {
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

struct NativeInlineError: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(AnimeL10n.key(.statusLoadingFailed), systemImage: "exclamationmark.triangle")
                .font(.headline)
            Text(message).font(.subheadline).foregroundStyle(.secondary)
            Button(AnimeL10n.key(.actionRetry), action: retry).buttonStyle(.borderedProminent)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

struct NativePosterImage: View {
    let url: URL?
    let width: CGFloat?
    let height: CGFloat?
    let aspectRatio: CGFloat?
    let cornerRadius: CGFloat

    init(
        url: URL?,
        width: CGFloat? = nil,
        height: CGFloat? = nil,
        aspectRatio: CGFloat? = nil,
        cornerRadius: CGFloat = 14,
    ) {
        self.url = url
        self.width = width
        self.height = height
        self.aspectRatio = aspectRatio
        self.cornerRadius = cornerRadius
    }

    var body: some View {
        posterImage
            .frame(width: width, height: height)
            .aspectRatio(width == nil && height == nil ? (aspectRatio ?? 3 / 4) : nil, contentMode: .fill)
            .frame(maxWidth: width == nil && height == nil ? .infinity : nil)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .contentShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    private var posterImage: some View {
        NativePosterLoader(url: url)
    }
}

private struct NativePosterLoader: View {
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
        .clipped()
    }
}

struct NativeRemoteImage: View {
    let url: URL?
    var contentMode: ContentMode = .fit

    var body: some View {
        AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image): image.resizable().aspectRatio(contentMode: contentMode)
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

struct NativeAvatar: View {
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

struct NativeMetric: View {
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

struct NativeActionRow: View {
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
