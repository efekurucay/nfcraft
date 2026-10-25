import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            ReadView()
                .tabItem { Label("Read", systemImage: "wave.3.right") }
            WriteView()
                .tabItem { Label("Write", systemImage: "square.and.pencil") }
            AboutView()
                .tabItem { Label("About", systemImage: "info.circle") }
        }
    }
}

struct ReadView: View {
    @EnvironmentObject var nfc: NFCService

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    if !nfc.isAvailable {
                        Banner(text: "NFC is not available on this device.", tone: .warning)
                    }
                    if let tag = nfc.lastTag {
                        TagCard(tag: tag)
                    } else {
                        ContactlessGlyph()
                            .padding(.top, 40)
                        Text("Scan a tag to see its type, UID and NDEF content.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }

                    if case .error(let message) = nfc.phase {
                        Banner(text: message, tone: .warning)
                    }

                    Button {
                        nfc.read()
                    } label: {
                        Label(nfc.phase == .scanning ? "Scanning…" : "Scan a tag", systemImage: "wave.3.right")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .disabled(!nfc.isAvailable || nfc.phase == .scanning)
                }
                .padding()
            }
            .navigationTitle("Read")
        }
    }
}

struct WriteView: View {
    @EnvironmentObject var nfc: NFCService
    @State private var records: [NDEFRecordModel] = []
    @State private var showAdd = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if records.isEmpty {
                        Text("Add records, then write them to a tag.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .padding(.top, 24)
                    } else {
                        ForEach(records) { record in
                            HStack {
                                Text(record.summary).font(.callout)
                                Spacer()
                            }
                            .padding()
                            .background(Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    }

                    Button {
                        showAdd = true
                    } label: {
                        Label("Add record", systemImage: "plus")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    if case .error(let message) = nfc.phase {
                        Banner(text: message, tone: .warning)
                    }
                    if case .done(let message) = nfc.phase {
                        Banner(text: message, tone: .ok)
                    }

                    Button {
                        nfc.write(records)
                    } label: {
                        Label("Write to tag", systemImage: "square.and.pencil")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .disabled(records.isEmpty || nfc.phase == .scanning)
                }
                .padding()
            }
            .navigationTitle("Write")
            .sheet(isPresented: $showAdd) {
                AddRecordSheet { records.append($0) }
            }
        }
    }
}

struct AddRecordSheet: View {
    enum Kind: String, CaseIterable { case url = "URL", text = "Text" }
    @Environment(\.dismiss) private var dismiss
    @State private var kind: Kind = .url
    @State private var value = ""
    let onAdd: (NDEFRecordModel) -> Void

    var body: some View {
        NavigationStack {
            Form {
                Picker("Type", selection: $kind) {
                    ForEach(Kind.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)
                TextField(kind == .url ? "https://example.com" : "Text", text: $value)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
            .navigationTitle("Add record")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        let record: NDEFRecordModel = kind == .url ? .uri(value) : .text(value, lang: "en")
                        onAdd(record)
                        dismiss()
                    }
                    .disabled(value.isEmpty)
                }
            }
        }
    }
}

struct AboutView: View {
    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("NFCraft reads and writes NFC tags using CoreNFC.")
                }
                Section("What iOS allows") {
                    Label("Read tag type + UID", systemImage: "checkmark.circle")
                    Label("Read & write NDEF", systemImage: "checkmark.circle")
                }
                Section("Not available on iOS") {
                    Label("Raw APDU to any technology", systemImage: "xmark.circle")
                    Label("MIFARE Classic dump / Crypto1", systemImage: "xmark.circle")
                    Label("Tag emulation (HCE)", systemImage: "xmark.circle")
                    Label("Automation tasks", systemImage: "xmark.circle")
                }
                Section {
                    Text("The full toolkit lives in the Android app.")
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("About")
        }
    }
}

// MARK: - Reusable views

struct TagCard: View {
    let tag: ScannedTag

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            row("Type", tag.typeName)
            row("UID", tag.identifier)
            row("NDEF", "\(tag.records.count) record(s) · \(tag.ndefWritable ? "writable" : "read-only") · \(tag.capacity) B")
            if !tag.records.isEmpty {
                Divider()
                ForEach(Array(tag.records.enumerated()), id: \.offset) { _, record in
                    Text(record.summary).font(.callout)
                }
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func row(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label.uppercased()).font(.caption2).foregroundStyle(.secondary)
            Text(value).font(.body)
        }
    }
}

struct ContactlessGlyph: View {
    var body: some View {
        Image(systemName: "wave.3.right.circle.fill")
            .resizable()
            .scaledToFit()
            .frame(width: 120, height: 120)
            .foregroundStyle(Color(red: 0.18, green: 0.36, blue: 1.0))
    }
}

struct Banner: View {
    enum Tone { case ok, warning }
    let text: String
    let tone: Tone

    var body: some View {
        Text(text)
            .font(.callout)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background((tone == .ok ? Color.green : Color.orange).opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

#Preview {
    ContentView().environmentObject(NFCService())
}
