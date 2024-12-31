import WidgetKit
import SwiftUI
import shared

struct Provider: TimelineProvider {
    let weatherProcessor = AppState().weatherProcessor
    
    func placeholder(in context: Context) -> WeatherAlertEntry {
        WeatherAlertEntry(date: Date(), alerts: [])
    }
    
    func getSnapshot(in context: Context, completion: @escaping (WeatherAlertEntry) -> ()) {
        weatherProcessor.processAlerts { result, error in
            let alerts = result as? [Alert] ?? []
            let entry = WeatherAlertEntry(date: Date(), alerts: alerts)
            completion(entry)
        }
    }
    
    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        weatherProcessor.processAlerts { result, error in
            let alerts = result as? [Alert] ?? []
            let entry = WeatherAlertEntry(date: Date(), alerts: alerts)
            
            // Update every 30 minutes
            let nextUpdate = Calendar.current.date(byAdding: .minute, value: 30, to: Date())!
            let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
            
            completion(timeline)
        }
    }
}

struct WeatherAlertEntry: TimelineEntry {
    let date: Date
    let alerts: [Alert]
}

struct WeatherAlertWidgetEntryView : View {
    var entry: Provider.Entry
    @Environment(\.widgetFamily) var family
    
    var body: some View {
        switch family {
        case .systemSmall:
            SmallWidgetView(entry: entry)
        case .systemMedium:
            MediumWidgetView(entry: entry)
        case .systemLarge:
            LargeWidgetView(entry: entry)
        @unknown default:
            SmallWidgetView(entry: entry)
        }
    }
}

struct SmallWidgetView: View {
    let entry: Provider.Entry
    
    var body: some View {
        VStack(alignment: .leading) {
            Text("Weather Alerts")
                .font(.caption)
                .foregroundColor(.secondary)
            
            if entry.alerts.isEmpty {
                Text("No active alerts")
                    .font(.caption2)
            } else {
                let activeAlerts = entry.alerts.filter { $0.status == AlertStatus.active }
                Text("\(activeAlerts.count) Active")
                    .font(.title2)
                    .foregroundColor(.red)
            }
        }
        .padding()
    }
}

struct MediumWidgetView: View {
    let entry: Provider.Entry
    
    var body: some View {
        VStack(alignment: .leading) {
            Text("Weather Alerts")
                .font(.caption)
                .foregroundColor(.secondary)
            
            if entry.alerts.isEmpty {
                Text("No active alerts")
                    .font(.body)
            } else {
                let activeAlerts = entry.alerts.prefix(2)
                ForEach(Array(activeAlerts), id: \.id) { alert in
                    HStack {
                        Circle()
                            .fill(alert.status == AlertStatus.active ? Color.red : Color.orange)
                            .frame(width: 8, height: 8)
                        Text(alert.title)
                            .font(.caption)
                            .lineLimit(1)
                    }
                }
            }
        }
        .padding()
    }
}

struct LargeWidgetView: View {
    let entry: Provider.Entry
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Weather Alerts")
                .font(.headline)
                .foregroundColor(.secondary)
            
            if entry.alerts.isEmpty {
                Text("No active alerts")
                    .font(.body)
            } else {
                let activeAlerts = entry.alerts.prefix(4)
                ForEach(Array(activeAlerts), id: \.id) { alert in
                    VStack(alignment: .leading) {
                        HStack {
                            Circle()
                                .fill(alert.status == AlertStatus.active ? Color.red : Color.orange)
                                .frame(width: 8, height: 8)
                            Text(alert.title)
                                .font(.caption)
                                .lineLimit(1)
                        }
                        Text(alert.description_)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }
                }
            }
        }
        .padding()
    }
}

@main
struct WeatherAlertWidget: Widget {
    let kind: String = "WeatherAlertWidget"
    
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            WeatherAlertWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Weather Alerts")
        .description("Shows current weather alerts")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
