import SwiftUI
import Foundation
import PhotosUI
import UIKit

struct NativeAdminView: View {
    @ObservedObject var model: NativeAppModel
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    if let overview = model.adminOverview {
                        Text(AnimeL10n.key(.adminConsole)).font(.title2.weight(.bold))
                        Text(AnimeL10n.string(.adminRole, overview.role)).font(.subheadline).foregroundStyle(.secondary)
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                            NativeMetric(value: Int(overview.usersActive), title: AnimeL10n.string(.adminActiveUsers))
                            NativeMetric(value: Int(overview.usersTotal), title: AnimeL10n.string(.adminTotalUsers))
                            NativeMetric(value: Int(overview.reviewsPublished), title: AnimeL10n.string(.adminPublishedReviews))
                            NativeMetric(value: Int(overview.commentsPublished), title: AnimeL10n.string(.adminPublishedComments))
                            NativeMetric(value: Int(overview.openCommentReports), title: AnimeL10n.string(.adminOpenReports))
                        }
                        Text(AnimeL10n.key(.adminReportQueue))
                            .font(.headline)
                            .padding(.top, 8)
                        if model.adminReports.isEmpty {
                            Text(AnimeL10n.key(.adminNoReports))
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        } else {
                            ForEach(model.adminReports) { report in
                                VStack(alignment: .leading, spacing: 9) {
                                    HStack {
                                        Text(report.reasonCode.reportDisplayName).font(.headline)
                                        Spacer()
                                        Text(report.status.adminReportDisplayName)
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(.secondary)
                                    }
                                    Text(AnimeL10n.string(.adminReportPeople, report.reporterName, report.authorName))
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                    if let subjectTitle = report.subjectTitle {
                                        Text(subjectTitle).font(.caption).foregroundStyle(.tint)
                                    }
                                    Text(report.body).lineLimit(4)
                                    HStack(spacing: 8) {
                                        Button(AnimeL10n.key(.adminClaim)) { reportAction(report, action: "claim") }
                                            .buttonStyle(.bordered)
                                            .disabled(report.status != "open")
                                        Button(AnimeL10n.key(.adminKeep)) { reportAction(report, action: "resolve", contentAction: "published") }
                                            .buttonStyle(.bordered)
                                        Button(AnimeL10n.key(.adminHide), role: .destructive) { reportAction(report, action: "resolve", contentAction: "hidden") }
                                            .buttonStyle(.bordered)
                                        Button(AnimeL10n.key(.adminDismiss)) { reportAction(report, action: "dismiss") }
                                            .buttonStyle(.bordered)
                                    }
                                }
                                .padding(14)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                        }
                        Text(AnimeL10n.key(.adminCommentModeration))
                            .font(.headline)
                            .padding(.top, 8)
                        if model.adminComments.isEmpty {
                            Text(AnimeL10n.key(.adminNoComments))
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        } else {
                            ForEach(model.adminComments) { comment in
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack {
                                        Text(comment.authorName).font(.headline)
                                        Spacer()
                                        Text(comment.moderationStatus.adminDisplayName)
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(.secondary)
                                    }
                                    if let subjectTitle = comment.subjectTitle {
                                        Text(subjectTitle)
                                            .font(.caption)
                                            .foregroundStyle(.tint)
                                    }
                                    Text(comment.body)
                                        .lineLimit(4)
                                    HStack(spacing: 8) {
                                        Button(AnimeL10n.key(.actionPublish)) { moderate(comment.id, action: "published") }
                                            .buttonStyle(.bordered)
                                            .disabled(comment.moderationStatus == "published")
                                        Button(AnimeL10n.key(.adminHide)) { moderate(comment.id, action: "hidden") }
                                            .buttonStyle(.bordered)
                                            .disabled(comment.moderationStatus == "hidden")
                                        Button(AnimeL10n.key(.actionDelete), role: .destructive) { moderate(comment.id, action: "deleted") }
                                            .buttonStyle(.bordered)
                                    }
                                }
                                .padding(14)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                        }
                        NativeActionRow(title: AnimeL10n.string(.adminAccount), subtitle: AnimeL10n.string(.adminAccountDescription), systemImage: "person.badge.key")
                    } else if let errorMessage {
                        NativeInlineError(message: errorMessage) { load() }
                    } else {
                        ProgressView(AnimeL10n.key(.adminLoading))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    }
                }
                .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
            .navigationTitle(AnimeL10n.key(.adminConsole))
            .navigationBarTitleDisplayMode(.large)
            .task { load() }
            .refreshable { await refresh() }
    }

    private func load() {
        errorMessage = nil
        model.loadAdminOverview { _, error in errorMessage = error }
        model.loadAdminComments { _, error in if errorMessage == nil { errorMessage = error } }
        model.loadAdminReports { _, error in if errorMessage == nil { errorMessage = error } }
    }

    private func moderate(_ id: String, action: String) {
        model.moderateComment(id: id, action: action) { error in
            if let error { errorMessage = error }
        }
    }

    private func reportAction(_ report: NativeAdminReportSnapshot, action: String, contentAction: String? = nil) {
        model.adminReportAction(id: report.id, action: action, contentAction: contentAction) { error in
            if let error { errorMessage = AnimeL10n.string(.activityOperationFailed, error) }
        }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadAdminOverview { _, error in
                errorMessage = error
                model.loadAdminComments { _, commentsError in
                    if errorMessage == nil { errorMessage = commentsError }
                    model.loadAdminReports { _, reportsError in
                        if errorMessage == nil { errorMessage = reportsError }
                        continuation.resume()
                    }
                }
            }
        }
    }
}

