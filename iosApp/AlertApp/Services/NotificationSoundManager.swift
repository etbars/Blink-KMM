import Foundation
import AVFoundation
import UserNotifications
import shared

class NotificationSoundManager {
    static let shared = NotificationSoundManager()
    private var soundPlayers: [String: AVAudioPlayer] = [:]
    
    private init() {
        setupAudioSession()
    }
    
    private func setupAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Failed to setup audio session: \(error)")
        }
    }
    
    func getSoundName(for alert: Alert) -> String {
        guard let weatherType = alert.weatherType else {
            return alert.type == .price ? "price_alert.wav" : "general_alert.wav"
        }
        
        switch weatherType {
        case .tornado, .hurricane:
            return "severe_weather.wav"
        case .rain, .flood:
            return "heavy_rain.wav"
        case .wind:
            return "wind_alert.wav"
        default:
            return "general_alert.wav"
        }
    }
    
    func getNotificationSound(for alert: Alert) -> UNNotificationSound {
        if alert.isCritical {
            return UNNotificationSound.defaultCriticalSound(withAudioVolume: 1.0)
        }
        
        let soundName = getSoundName(for: alert)
        return UNNotificationSound(named: UNNotificationSoundName(soundName))
    }
    
    func preloadSounds() {
        let sounds = ["severe_weather.wav", "heavy_rain.wav", "wind_alert.wav", "price_alert.wav", "general_alert.wav"]
        
        for sound in sounds {
            if let soundURL = Bundle.main.url(forResource: sound.replacingOccurrences(of: ".wav", with: ""), withExtension: "wav") {
                do {
                    let player = try AVAudioPlayer(contentsOf: soundURL)
                    player.prepareToPlay()
                    soundPlayers[sound] = player
                } catch {
                    print("Failed to load sound \(sound): \(error)")
                }
            }
        }
    }
    
    func playSound(for alert: Alert) {
        let soundName = getSoundName(for: alert)
        if let player = soundPlayers[soundName] {
            player.play()
        }
    }
}
