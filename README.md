# Blink KMM

Blink is a powerful alert monitoring application built with Kotlin Multiplatform Mobile (KMM) for both Android and iOS platforms. It allows users to create and manage various types of alerts including weather conditions, price changes, and content updates.

## Features

- **Multiple Alert Types**:
  - Weather alerts with real-time weather data
  - Price monitoring for stocks and assets
  - Content change detection for web pages
  - Custom alert types for flexibility

- **Smart Notifications**:
  - Priority-based notification system
  - Customizable alert conditions
  - Rich notifications with actions
  - Alert grouping and management

- **Efficient Background Processing**:
  - Battery-efficient alert checking
  - Adaptive scheduling based on alert types
  - Network-aware operations
  - Persistent alert storage

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Xcode 13 or later (for iOS development)
- JDK 11 or later
- Kotlin 1.8.0 or later

### API Keys

The application requires the following API keys:

1. OpenWeather API key for weather data
2. AlphaVantage API key for price data

Add these to your `local.properties` file:
```properties
openWeatherApiKey=your_openweather_api_key
alphaVantageApiKey=your_alphavantage_api_key
```

### Building the Project

1. Clone the repository
```bash
git clone https://github.com/yourusername/Blink-KMM.git
```

2. Open the project in Android Studio

3. Sync project with Gradle files

4. Run the application on your desired device/emulator

## Architecture

The project follows Clean Architecture principles and is organized into the following modules:

- `shared`: Common KMM code shared between platforms
- `androidApp`: Android-specific implementation
- `iosApp`: iOS-specific implementation

### Key Components

- Alert Processing System
- Notification Management
- Background Task Scheduling
- Local Data Storage
- API Integrations

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- OpenWeather API for weather data
- AlphaVantage API for market data
- Kotlin Multiplatform Mobile team for the amazing framework
