import SwiftUI
import Foundation
import PhotosUI
import UIKit

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
                DatePicker(AnimeL10n.key(.calendarDate), selection: $selectedDate, displayedComponents: .date)
                    .datePickerStyle(.compact)
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))

                if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                } else if let calendar = model.calendar, calendar.date == dateString {
                    if calendar.items.isEmpty {
                        ContentUnavailableView(AnimeL10n.key(.calendarEmpty), systemImage: "calendar.badge.clock", description: Text(AnimeL10n.key(.calendarEmptyDescription)))
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
                    ProgressView(AnimeL10n.key(.calendarLoading))
                        .frame(maxWidth: .infinity, minHeight: 180)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(AnimeL10n.key(.calendar))
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
            NativePosterImage(url: subject.posterURL, width: 60, height: 80, cornerRadius: 12)
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

