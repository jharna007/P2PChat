# P2PChat - Android Peer-to-Peer Chat Application

A production-ready Android P2P chat application using WebRTC and Firebase Firestore for signaling. Built with Kotlin, MVVM architecture, and Material Design 3.

## 🚀 Features

- **Peer-to-Peer Messaging**: Direct communication between devices using WebRTC data channels
- **Firebase Signaling**: Reliable connection establishment using Firestore
- **Material Design 3**: Modern, adaptive UI with dark/light theme support  
- **Room Database**: Local message persistence with offline capability
- **QR Code Support**: Easy room joining via QR code scanning and generation
- **GitHub Actions CI/CD**: Automated build and deployment pipeline
- **Real-time Connection Status**: Visual feedback on connection state
- **Message Delivery Status**: Track message sending, delivery, and failure states
- **Room Management**: Auto-expiring rooms with cleanup functionality

## 🏗️ Architecture

This project follows the **MVVM (Model-View-ViewModel)** architecture pattern with Repository pattern:

```
app/src/main/java/com/kaifcodec/p2pchat/
├── ui/
│   ├── main/MainActivity.kt
│   ├── chat/ChatActivity.kt  
│   ├── join/JoinRoomActivity.kt
│   ├── adapters/MessageAdapter.kt
│   └── viewmodels/ChatViewModel.kt
├── data/
│   ├── repository/ChatRepository.kt
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── entities/Message.kt
│   │   └── dao/MessageDao.kt
│   └── remote/FirebaseSignaling.kt
├── webrtc/
│   └── WebRTCClient.kt
├── models/
│   ├── ChatMessage.kt
│   ├── ConnectionState.kt
│   └── SignalData.kt
└── utils/
    ├── Constants.kt
    ├── Extensions.kt
    └── Logger.kt
```

## 📱 Tech Stack

- **Language**: Kotlin
- **UI**: Android Views with ViewBinding, Material Design 3
- **Architecture**: MVVM with Repository Pattern
- **Database**: Room (SQLite wrapper)
- **Networking**: WebRTC for P2P, Firebase Firestore for signaling
- **Dependency Injection**: Manual DI (lightweight approach)
- **Concurrency**: Kotlin Coroutines + Flow
- **QR Code**: ZXing Android Embedded
- **Build System**: Gradle with Kotlin DSL
- **CI/CD**: GitHub Actions

## 🛠️ Setup Instructions

### 1. Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or higher
- Android SDK with API level 34 (Android 14)
- Git

### 2. Clone the Repository

```bash
git clone <your-repository-url>
cd P2PChat
```

### 3. Firebase Configuration

**IMPORTANT**: Replace the placeholder Firebase configuration with your actual `google-services.json`:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add an Android app with package name: `com.kaifcodec.p2pchat`
4. Download the `google-services.json` file
5. Replace `app/google-services-placeholder.json` with your actual `app/google-services.json`

#### Firebase Setup:
```bash
# Enable Firestore Database
1. Go to Firebase Console → Firestore Database
2. Create database in "test mode" (for development)
3. Set rules for production:

rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /rooms/{roomId}/{document=**} {
      allow read, write: if true; // Configure proper auth in production
    }
  }
}
```

### 4. Build and Run

```bash
# Grant execute permission (macOS/Linux)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## 🔧 Configuration

### WebRTC Configuration

The app uses Google's public STUN servers by default:
- `stun:stun.l.google.com:19302`
- `stun:stun1.l.google.com:19302`
- `stun:stun2.l.google.com:19302`

For production, consider setting up your own TURN server for better reliability across restrictive networks.

### App Configuration

Key configuration constants in `Constants.kt`:
```kotlin
const val ROOM_EXPIRY_TIME = 24 * 60 * 60 * 1000L // 24 hours
const val CONNECTION_TIMEOUT = 30000L // 30 seconds
const val MAX_MESSAGE_LENGTH = 1000
const val MAX_MESSAGES_PER_MINUTE = 10
const val ROOM_CODE_LENGTH = 6
```

## 📋 Usage

### Creating a Room
1. Open the app
2. Tap "Create New Room" 
3. Share the generated 6-character room code or QR code
4. Wait for others to join

### Joining a Room
1. Tap "Join Room" or "Scan QR Code"
2. Enter the room code or scan the QR code
3. Start chatting once connected

### Features
- **Real-time messaging**: Messages appear instantly when connection is established
- **Delivery status**: See when messages are sending, sent, or failed
- **Connection status**: Visual indicators show connection state
- **Room sharing**: Generate QR codes or share room codes via any app
- **Offline storage**: Messages persist locally using Room database

## 🤖 GitHub Actions CI/CD

The project includes a comprehensive CI/CD pipeline that:

### Automated Builds
- Builds on every push to `main` or `develop` branches
- Runs on pull requests to `main` branch
- Supports both debug and release APK generation

### Pipeline Steps
1. **Code checkout** with actions/checkout@v4
2. **Java 17 setup** using Temurin distribution
3. **Gradle caching** for faster builds
4. **Firebase configuration** from GitHub secrets
5. **Code linting** with Android lint
6. **Unit testing** execution
7. **APK building** (debug + release)
8. **Artifact upload** for generated APKs and reports

### GitHub Secrets Configuration

Add these secrets to your GitHub repository:

```
GOOGLE_SERVICES_JSON=<your-google-services.json-content>
```

To add the secret:
1. Go to Repository → Settings → Secrets and variables → Actions
2. Click "New repository secret"  
3. Name: `GOOGLE_SERVICES_JSON`
4. Value: Paste the entire content of your `google-services.json` file
5. Click "Add secret"

### Running CI/CD

The pipeline automatically triggers on:
- Push to `main` or `develop` branches
- Pull requests to `main` branch

Manual trigger:
1. Go to Actions tab in your GitHub repository
2. Select "Android CI/CD" workflow
3. Click "Run workflow"

## 🧪 Testing

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Instrumented Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Test Coverage
- Repository layer tests
- Utility function tests  
- WebRTC connection tests
- Database migration tests

## 🔐 Security Considerations

### Current Implementation
- Input validation for room codes and messages
- Rate limiting (10 messages/minute)
- Auto-expiring rooms (24 hours)
- Local message encryption using AES-256
- Network security config for HTTPS enforcement

### Production Recommendations
1. **Implement Firebase Authentication**
2. **Add proper Firestore security rules**
3. **Set up your own TURN server**
4. **Enable ProGuard/R8 obfuscation**
5. **Add certificate pinning**
6. **Implement message encryption end-to-end**

## 🚀 Performance Optimizations

- **Efficient RecyclerView**: ViewBinding with DiffUtil for message lists
- **Database pagination**: Room database with Flow for reactive updates
- **Memory leak prevention**: Proper lifecycle management
- **Background operations**: Coroutines with appropriate dispatchers
- **WebRTC resource management**: Cleanup on app pause/resume

## 🐛 Troubleshooting

### Common Issues

**1. Build Failures**
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

**2. Firebase Connection Issues**
- Verify `google-services.json` is correctly placed
- Check Firebase project configuration
- Ensure Firestore is enabled and configured

**3. WebRTC Connection Problems**
- Check network connectivity
- Verify STUN server accessibility
- Consider NAT/firewall restrictions (may need TURN server)

**4. GitHub Actions Failing**
- Ensure `GOOGLE_SERVICES_JSON` secret is properly set
- Check if all required dependencies are available
- Verify Gradle wrapper permissions

### Debug Logs

Enable verbose logging by setting `BuildConfig.DEBUG = true` and check logcat:
```bash
adb logcat -s P2PChat
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

For issues, questions, or feature requests:
1. Check existing [GitHub Issues](../../issues)
2. Create a new issue with detailed description
3. Include logs, device info, and steps to reproduce

## 🚀 Roadmap

- [ ] End-to-end message encryption
- [ ] File and media sharing support
- [ ] Group chat rooms (multiple participants)
- [ ] Push notifications
- [ ] Desktop companion app
- [ ] Voice/video calling integration

---

**Built with ❤️ using Android, Kotlin, WebRTC, and Firebase**
