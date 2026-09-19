import SwiftUI
import Foundation
import PhotosUI
import UIKit

struct NativeSettingsView: View {
    @ObservedObject var model: NativeAppModel
    @State private var showingAccount = false

    var body: some View {
        Form {
            Section(AnimeL10n.key(.accountSection)) {
                if model.session.status == "authenticated" {
                    LabeledContent(AnimeL10n.key(.username), value: model.session.displayName ?? AnimeL10n.string(.userFallback))
                    NavigationLink {
                        NativeAccountManagementView(model: model)
                    } label: {
                        Label(AnimeL10n.key(.accountData), systemImage: "person.crop.circle.badge.checkmark")
                    }
                    Button(AnimeL10n.key(.accountSwitch)) { showingAccount = true }
                } else {
                    Button(AnimeL10n.key(.actionLoginOrRegister)) { showingAccount = true }
                }
            }

            Section(AnimeL10n.key(.serviceSection)) {
                NavigationLink {
                    NativeDiagnosticsView(model: model)
                } label: {
                    Label(AnimeL10n.key(.diagnostics), systemImage: "waveform.path.ecg")
                }
            }

            Section(AnimeL10n.key(.appearanceSection)) {
                Picker(AnimeL10n.key(.profileTheme), selection: Binding(
                    get: { model.appearanceTheme },
                    set: { model.setAppearanceTheme($0) },
                )) {
                    Text(AnimeL10n.key(.profileSystem)).tag("system")
                    Text(AnimeL10n.key(.profileLight)).tag("light")
                    Text(AnimeL10n.key(.profileDark)).tag("dark")
                }
                Toggle(AnimeL10n.key(.profileGlass), isOn: Binding(
                    get: { model.glassEnabled },
                    set: { model.setGlassEnabled($0) },
                ))
                Toggle(AnimeL10n.key(.profileReduceMotion), isOn: Binding(
                    get: { model.reduceMotionEnabled },
                    set: { model.setReduceMotionEnabled($0) },
                ))
            }

            Section(AnimeL10n.key(.languageSection)) {
                Picker(AnimeL10n.key(.profileAppLanguage), selection: Binding(
                    get: { model.languagePreference.rawValue },
                    set: { model.setLanguage($0) },
                )) {
                    ForEach(NativeLanguagePreference.allCases) { language in
                        Text(LocalizedStringKey(language.displayName)).tag(language.rawValue)
                    }
                }
            }

            Section(AnimeL10n.key(.aboutSection)) {
                LabeledContent(AnimeL10n.key(.version), value: appVersion)
                LabeledContent(AnimeL10n.key(.interfaceValue), value: AnimeL10n.string(.interfaceValue))
            }
        }
        .navigationTitle(AnimeL10n.key(.settings))
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showingAccount) {
            NativeAccountSheet(model: model)
                .presentationDetents([.medium, .large])
        }
    }

    private var appVersion: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? AnimeL10n.string(.genericUnknown)
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
    @State private var selectedAvatar: PhotosPickerItem?
    @State private var isUploadingAvatar = false

    var body: some View {
        Form {
                Section(AnimeL10n.key(.profileTitle)) {
                HStack(spacing: 12) {
                    NativeAvatar(url: model.profile?.avatarURL ?? model.session.avatarURL, name: model.profile?.displayName ?? model.session.displayName)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(AnimeL10n.key(.profilePicture)).font(.headline)
                        Text(AnimeL10n.key(.profilePictureFormats))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    PhotosPicker(selection: $selectedAvatar, matching: .images) {
                        Image(systemName: "camera.fill")
                    }
                    .buttonStyle(.bordered)
                    .disabled(isUploadingAvatar)
                }
                TextField(AnimeL10n.key(.displayName), text: $displayName)
                Button(AnimeL10n.key(.profileSaveDisplayName)) {
                    model.updateProfile(displayName: displayName) { error in
                        message = error.map { AnimeL10n.string(.profileSaveFailed, $0) } ?? AnimeL10n.string(.profileSaved)
                        if error == nil { model.loadProfile() }
                    }
                }
                .disabled(displayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section(AnimeL10n.key(.passwordChange)) {
                SecureField(AnimeL10n.key(.passwordCurrent), text: $currentPassword)
                SecureField(AnimeL10n.key(.passwordNew), text: $newPassword)
                Button(AnimeL10n.key(.passwordChange)) {
                    model.changePassword(currentPassword: currentPassword, newPassword: newPassword) { error in
                        message = error.map { AnimeL10n.string(.passwordChangeFailed, $0) } ?? AnimeL10n.string(.passwordChanged)
                        if error == nil { currentPassword = ""; newPassword = "" }
                    }
                }
                .disabled(currentPassword.isEmpty || newPassword.count < 8)
            }

            Section(AnimeL10n.key(.accountData)) {
                NavigationLink {
                    NativeSyncView(model: model)
                } label: {
                    Label(AnimeL10n.key(.bangumiSync), systemImage: "arrow.triangle.2.circlepath")
                }
                Button(AnimeL10n.key(.exportData)) {
                    model.exportMyData { data, error in
                        if let data { exportText = AnimeL10n.string(.exportSuccess, String(data.utf8.count)) }
                        else { exportText = AnimeL10n.string(.exportFailed, error ?? AnimeL10n.string(.genericUnknown)) }
                    }
                }
            }

            Section {
                Button(AnimeL10n.key(.actionDeleteAccount), role: .destructive) { showingDeleteConfirmation = true }
            } footer: {
                Text(AnimeL10n.key(.accountDeleteWarning))
            }

            if let message { Section { Text(message).foregroundStyle(message.contains(AnimeL10n.string(.errorFailureMarker)) ? .red : .secondary) } }
            if let exportText { Section(AnimeL10n.key(.exportResult)) { Text(exportText).font(.footnote) } }
        }
        .navigationTitle(AnimeL10n.key(.accountData))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            displayName = model.profile?.displayName ?? model.session.displayName ?? ""
            if model.profile == nil { model.loadProfile() }
        }
        .onChange(of: selectedAvatar) { _, item in
            guard let item else { return }
            Task {
                do {
                    guard let data = try await item.loadTransferable(type: Data.self) else {
                        message = AnimeL10n.string(.avatarReadFailed)
                        return
                    }
                    isUploadingAvatar = true
                    let jpegData = UIImage(data: data)?.jpegData(compressionQuality: 0.86) ?? data
                    model.uploadAvatar(base64: jpegData.base64EncodedString(), contentType: "image/jpeg") { error in
                        isUploadingAvatar = false
                        message = error.map { AnimeL10n.string(.avatarUploadFailed, $0) } ?? AnimeL10n.string(.avatarUpdated)
                        if error == nil { selectedAvatar = nil }
                    }
                } catch {
                    message = AnimeL10n.string(.avatarReadFailed, error.localizedDescription)
                }
            }
        }
        .confirmationDialog(AnimeL10n.key(.accountDeleteConfirmation), isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button(AnimeL10n.key(.accountDeleteForever), role: .destructive) {
                model.deleteAccount { error in message = error.map { AnimeL10n.string(.accountDeleteFailed, $0) } ?? AnimeL10n.string(.accountDeleted) }
            }
        }
    }
}

struct NativeSyncView: View {
    @ObservedObject var model: NativeAppModel
    @State private var message: String?

    var body: some View {
        List {
            Section(AnimeL10n.key(.syncSection)) {
                if let status = model.syncStatus {
                    LabeledContent(AnimeL10n.key(.syncPending), value: String(status.pendingCount))
                    LabeledContent(AnimeL10n.key(.syncFailed), value: String(status.failedCount))
                    LabeledContent(AnimeL10n.key(.syncConflicts), value: String(status.conflictCount))
                    LabeledContent("Bangumi", value: status.bangumiLinked ? AnimeL10n.string(.syncConnected) : AnimeL10n.string(.syncDisconnected))
                    if let last = status.lastSuccessfulAt {
                        LabeledContent(AnimeL10n.key(.syncLastSuccess), value: last.prefix(16).description)
                    }
                } else {
                    ProgressView(AnimeL10n.key(.syncLoading))
                }
                Button(AnimeL10n.key(.syncNow)) {
                    model.startSync { error in
                        message = error.map { AnimeL10n.string(.syncRequestFailed, $0) } ?? AnimeL10n.string(.syncRequestSubmitted)
                        model.loadSyncStatus()
                        model.loadSyncConflicts()
                    }
                }
                if let message { Text(message).font(.footnote).foregroundStyle(.secondary) }
            }

            Section(AnimeL10n.key(.syncConflictSection)) {
                if model.syncConflicts.isEmpty {
                    Text(AnimeL10n.key(.syncNoConflicts))
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(model.syncConflicts) { conflict in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(AnimeL10n.string(.syncConflictTitle, String(conflict.subjectId), conflict.fieldName)).font(.headline)
                            Text(AnimeL10n.string(.syncLocal, conflict.localValue)).font(.caption).foregroundStyle(.secondary)
                            Text(AnimeL10n.string(.syncRemote, conflict.remoteValue)).font(.caption).foregroundStyle(.secondary)
                            HStack {
                                Button(AnimeL10n.key(.syncKeepLocal)) { resolve(conflict, choice: "keep_local") }
                                Button(AnimeL10n.key(.syncUseRemote)) { resolve(conflict, choice: "use_remote") }
                                Button(AnimeL10n.key(.syncLater)) { resolve(conflict, choice: "later") }
                            }
                            .font(.caption.weight(.semibold))
                        }
                        .padding(.vertical, 5)
                    }
                }
            }
        }
        .scrollIndicators(.hidden)
        .navigationTitle(AnimeL10n.key(.bangumiSync))
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
            message = error.map { AnimeL10n.string(.syncResolveFailed, $0) } ?? AnimeL10n.string(.syncResolved)
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
                            Text(AnimeL10n.key(.diagnosticsTesting))
                        }
                    } else {
                        Text(AnimeL10n.key(.diagnosticsRetest))
                    }
                }
                .disabled(model.isLoadingDiagnostics)
            }
            Section(AnimeL10n.key(.serviceSection)) {
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
        .navigationTitle(AnimeL10n.key(.diagnostics))
        .task { if model.diagnostics.isEmpty { model.loadDiagnostics() } }
    }

    private func diagnosticStatusText(_ item: NativeDiagnosticSnapshot) -> String {
        let latency = item.latencyMs.map { "\($0) ms" } ?? AnimeL10n.string(.diagnosticsNoResponse)
        if item.healthy {
            return AnimeL10n.string(.diagnosticsHTTP, latency, item.statusCode.map(String.init) ?? "—")
        }
        if let error = item.errorMessage, !error.isEmpty {
            return "\(latency) · \(error)"
        }
        return AnimeL10n.string(.diagnosticsHTTP, latency, item.statusCode.map(String.init) ?? "—")
    }
}

