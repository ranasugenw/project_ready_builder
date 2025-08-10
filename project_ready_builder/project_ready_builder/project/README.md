# Jarvis Pattern Overlay - Advanced Trading Pattern Detection

An Android app that uses computer vision and machine learning to detect 250+ trading patterns in real-time by overlaying analysis on trading apps like CoinSwitch, Groww, and Binance.

## 🌟 Features

### Core Functionality
- **Screen Capture Analysis**: Captures and processes trading charts from any app
- **250+ Pattern Detection**: Rule-based + ML detection of candlestick and chart patterns  
- **30+ Technical Indicators**: RSI, MACD, EMA/SMA, ATR, Bollinger Bands, ADX, and more
- **Real-time Overlay**: Draws pattern outlines and signals over trading apps
- **Multi-timeframe Support**: 1m, 2m, 3m, 5m, 10m, 30m, 1h, 2h, 5h analysis
- **Signal Generation**: BUY/SELL/HOLD recommendations with confidence scores

### Pattern Types
**Candlestick Patterns:**
- Doji, Hammer, Shooting Star, Engulfing (Bullish/Bearish)
- Morning Star, Evening Star, Three White Soldiers, Three Black Crows
- Dark Cloud Cover, Piercing Line, Harami, Spinning Top

**Chart Patterns:**
- Double Top/Bottom, Triple Top/Bottom, Head & Shoulders
- Triangles (Ascending, Descending, Symmetrical)
- Wedges, Flags, Pennants, Channels, Cup & Handle

**ML Patterns:**
- TensorFlow Lite model for complex pattern recognition
- Fuzzy pattern detection with confidence scoring

### Technical Indicators
- **Trend**: SMA, EMA (multiple periods), ADX, MACD
- **Momentum**: RSI, Stochastic, CCI, Williams %R, Momentum
- **Volatility**: ATR, Bollinger Bands  
- **Volume**: OBV, VWAP, Volume Profile approximations
- **Custom**: Fisher Transform, custom indicator parameters

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 23+ (Android 6.0+)
- Device with Android 7.0+ for overlay permissions
- 4GB+ RAM recommended for ML processing

### Build Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd JarvisPatternOverlay
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Install Dependencies**
   - Android Studio will automatically download dependencies
   - Ensure all SDK components are installed

4. **Add Required Assets**
   - Create `app/src/main/assets/tesseract/` directory
   - Download `eng.traineddata` from Tesseract and place it there
   - Add the TensorFlow Lite model file: `pattern_classifier.tflite`

5. **Build the Project**
   ```bash
   ./gradlew assembleDebug
   ```

6. **Install on Device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### First Run Setup

1. **Grant Permissions**
   - Screen capture permission (MediaProjection)
   - System overlay permission (Display over other apps)
   - Storage permissions for CSV import/export

2. **Complete Onboarding**
   - Follow the setup wizard
   - Select your target trading app
   - Choose chart theme (light/dark)

3. **Calibrate Chart Area**
   - Open your trading app
   - Return to Jarvis and tap "Calibrate"  
   - Draw rectangle around the price chart area
   - Set top and bottom price values for scale

4. **Start Analysis**
   - Tap "Start Analysis" in the main screen
   - Switch to your trading app
   - Pattern overlays will appear automatically

## 🔧 Configuration

### App Settings
- **Target App**: Select which trading app to analyze
- **Chart Theme**: Light/Dark theme matching
- **Timeframes**: Choose active timeframes to monitor
- **FPS**: Adjust capture frame rate (1-10 FPS)
- **Confidence**: Minimum pattern confidence threshold

### Indicator Parameters
- **RSI Period**: Default 14, adjustable 5-50
- **MACD**: Fast(12), Slow(26), Signal(9) periods
- **Bollinger Bands**: Period(20), Standard Deviations(2.0)
- **ATR Period**: Default 14 for stop-loss calculation

### Pattern Detection
- **Minimum Confidence**: 0.1-1.0 (default 0.6)
- **Pattern Variants**: 250+ combinations with different thresholds
- **ML Model**: Custom TensorFlow Lite model for complex patterns

## 🤖 Machine Learning Model

### Training Data Format
The TensorFlow Lite model expects input features:
- **OHLC Data**: Last 50 normalized candles (200 features)
- **Technical Indicators**: RSI, MACD histogram (2 features)
- **Total Input Size**: 202 features

### Output Classes
- Bullish patterns (confidence 0-1)
- Bearish patterns (confidence 0-1)  
- Continuation patterns (confidence 0-1)
- Reversal patterns (confidence 0-1)

### Training Your Own Model
1. **Collect Data**: Historical OHLC data with pattern labels
2. **Feature Engineering**: Normalize prices, calculate indicators
3. **Model Architecture**: Dense neural network, 4-class output
4. **Training**: Use TensorFlow/Keras with pattern classification
5. **Convert**: Use TensorFlow Lite converter
6. **Deploy**: Replace `pattern_classifier.tflite` in assets

Example training script structure:
```python
import tensorflow as tf
from tensorflow import keras

# Model architecture
model = keras.Sequential([
    keras.layers.Dense(128, activation='relu', input_shape=(202,)),
    keras.layers.Dropout(0.3),
    keras.layers.Dense(64, activation='relu'),
    keras.layers.Dropout(0.3),
    keras.layers.Dense(32, activation='relu'),
    keras.layers.Dense(4, activation='softmax')  # 4 pattern classes
])

# Compile and train
model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])
model.fit(X_train, y_train, epochs=100, validation_data=(X_val, y_val))

# Convert to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
```

## 📊 Backtesting

### CSV Import Format
```csv
timestamp,open,high,low,close,volume
1640995200000,47000.5,47250.0,46800.0,47100.0,1250.5
1640995260000,47100.0,47180.0,46950.0,47050.0,980.2
```

### Backtest Metrics
- **Profit/Loss**: Total return percentage
- **Win Rate**: Percentage of profitable trades
- **Average R:R**: Risk to reward ratio
- **Maximum Drawdown**: Largest peak-to-trough decline
- **Sharpe Ratio**: Risk-adjusted returns

## 🎯 Signal Generation

### Entry Signals
- **BUY**: Multiple bullish patterns with >60% confidence
- **SELL**: Multiple bearish patterns with >60% confidence  
- **HOLD**: Conflicting or low-confidence patterns

### Risk Management
- **Stop Loss**: 2x ATR from entry price
- **Target 1**: 2x ATR profit (1:1 risk-reward)
- **Target 2**: 4x ATR profit (1:2 risk-reward)
- **Position Size**: 2% account risk per trade

### Signal Components
```kotlin
data class PatternSignal(
    val action: SignalAction,           // BUY/SELL/HOLD
    val confidence: Double,             // 0.0-1.0
    val entryPrice: Double,             // Recommended entry
    val stopLoss: Double,               // Risk management
    val target1: Double,                // First profit target
    val target2: Double,                // Second profit target
    val timeframe: String,              // Active timeframe
    val expectedDuration: String,       // "3-8 candles"
    val probability: Double,            // Success probability
    val positionSize: Double,           // Recommended size %
    val riskRewardRatio: Double,        // R:R ratio
    val reasoning: String               // Pattern explanation
)
```

## 🔒 Privacy & Security

### Data Handling
- **Local Storage**: All data stored on device only
- **No Cloud**: No data transmitted to external servers
- **Encryption**: Local database can be encrypted
- **Export**: Settings and journal can be exported

### Permissions
- **Screen Capture**: Only for chart analysis, not stored
- **Overlay**: Required for pattern display over apps
- **Storage**: Only for CSV import/export and settings

### Battery Optimization
- **FPS Throttling**: Adjustable capture rate
- **Background Pause**: Analysis stops when screen off
- **Low Battery**: Automatic service shutdown
- **Hardware Acceleration**: GPU processing when available

## ⚠️ Important Limitations

### Image Processing Accuracy
- **Lighting Dependent**: Requires good screen visibility
- **Chart Compatibility**: Best with standard candlestick charts
- **Scale Calibration**: Manual price scale setup required
- **App Updates**: May require re-calibration after app updates

### Recommended API Integration
For production trading, consider integrating with exchange APIs:
- **Binance API**: Real-time OHLC data
- **CoinGecko API**: Price and volume data
- **Alpha Vantage**: Traditional market data

### Performance Considerations
- **RAM Usage**: 2-4GB during active analysis
- **CPU Load**: Intensive during pattern detection
- **Battery Drain**: Continuous screen capture and processing
- **Heat Generation**: Extended use may warm device

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Indicator Tests
- RSI calculation accuracy
- MACD signal generation
- ATR volatility measurement
- Pattern detection logic

### Integration Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] Screen capture permissions
- [ ] Overlay display functionality  
- [ ] Pattern detection accuracy
- [ ] Signal generation logic
- [ ] CSV import/export
- [ ] Settings persistence
- [ ] Battery optimization

## 📖 Architecture

### Project Structure
```
app/
├── src/main/java/com/jarvis/patternoverlay/
│   ├── data/                 # Database, Repository
│   ├── engine/              # Pattern & Indicator engines
│   ├── model/               # Data models
│   ├── service/             # Background services
│   ├── ui/                  # Activities, ViewModels
│   ├── vision/              # Image processing
│   └── JarvisApplication.kt # Application class
├── src/main/res/            # Resources, layouts
├── src/main/assets/         # TensorFlow models, OCR data
└── src/test/                # Unit tests
```

### Key Components

**Services:**
- `ScreenCaptureService`: MediaProjection handling
- `PatternDetectionService`: Analysis processing
- `OverlayService`: UI overlay management

**Engines:**
- `PatternEngine`: Rule-based + ML pattern detection
- `IndicatorEngine`: Technical analysis calculations

**Vision:**
- `ImageProcessor`: OpenCV image processing
- `TesseractOCR`: Price label extraction

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Create feature branch
3. Follow Kotlin coding standards
4. Add unit tests for new features
5. Update documentation
6. Submit pull request

### Code Style
- **Kotlin**: Official style guide
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Manual DI in Application class
- **Async**: Coroutines for background processing

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## ⚖️ Disclaimer

**Investment Warning**: This app is for educational and analysis purposes only. Trading cryptocurrencies and stocks involves substantial risk and may result in significant financial losses. Never invest more than you can afford to lose.

**Accuracy Notice**: Pattern detection accuracy depends on screen quality, lighting, and chart formatting. Always verify signals with additional analysis before making trading decisions.

**No Financial Advice**: This app does not provide financial advice. Always consult with qualified financial advisors before making investment decisions.

---

## 🆘 Support

For issues, feature requests, or questions:
- Create an issue in the GitHub repository
- Check existing documentation and FAQ
- Review calibration guide for setup problems

**Common Issues:**
- Pattern detection not working → Check calibration
- Overlay not showing → Verify system overlay permission
- High battery usage → Reduce FPS in settings
- App crashes → Check available RAM and restart device

---

Built with ❤️ for the trading community. Happy pattern hunting! 📈