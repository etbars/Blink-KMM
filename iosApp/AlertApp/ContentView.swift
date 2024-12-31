import SwiftUI
import shared

struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @State private var alerts: [Alert] = []
    @State private var isLoading = false
    @State private var error: String?
    
    var body: some View {
        NavigationView {
            List {
                if isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else if let error = error {
                    Text(error)
                        .foregroundColor(.red)
                } else {
                    ForEach(alerts, id: \.id) { alert in
                        AlertRow(alert: alert)
                    }
                }
            }
            .navigationTitle("Weather Alerts")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: refreshAlerts) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
        }
        .onAppear {
            refreshAlerts()
        }
    }
    
    private func refreshAlerts() {
        isLoading = true
        error = nil
        
        appState.weatherProcessor.processAlerts { result, error in
            isLoading = false
            if let error = error {
                self.error = error.localizedDescription
            } else if let alerts = result as? [Alert] {
                self.alerts = alerts
            }
        }
    }
}

struct AlertRow: View {
    let alert: Alert
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(alert.title)
                .font(.headline)
            Text(alert.description_)
                .font(.subheadline)
                .foregroundColor(.secondary)
            HStack {
                Image(systemName: statusIcon)
                    .foregroundColor(statusColor)
                Text(alert.status.description())
                    .font(.caption)
                    .foregroundColor(statusColor)
                Spacer()
                Text(formattedDate)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 8)
    }
    
    private var statusIcon: String {
        switch alert.status {
        case .active: return "exclamationmark.circle.fill"
        case .pending: return "clock.fill"
        case .completed: return "checkmark.circle.fill"
        default: return "questionmark.circle.fill"
        }
    }
    
    private var statusColor: Color {
        switch alert.status {
        case .active: return .red
        case .pending: return .orange
        case .completed: return .green
        default: return .gray
        }
    }
    
    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: alert.createdAt)
    }
}

#Preview {
    ContentView()
        .environmentObject(AppState())
}
