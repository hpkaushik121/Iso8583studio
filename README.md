# ISO8583Studio Documentation

![ISO8583Studio](https://img.shields.io/badge/ISO8583-Studio-blue?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?style=for-the-badge&logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Desktop-4285F4?style=for-the-badge)
![License](https://img.shields.io/badge/License-Apache-green?style=for-the-badge)

A professional desktop application for ISO8583 financial transaction processing, configuration, testing, and monitoring. Built with Kotlin Multiplatform and Compose Desktop for cross-platform compatibility.

![ISO8583Studio Dashboard](docs/images/img.png)

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [User Interface](#-user-interface)
- [Advanced Features](#-advanced-features)
- [API Reference](#-api-reference)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)


## 🎯 Overview

ISO8583Studio is a comprehensive desktop application designed for financial institutions, payment processors, and developers working with ISO8583 messaging standards. It provides an intuitive interface for configuring gateways, testing transactions, and monitoring real-time activity.

### Key Benefits
- **Professional Grade**: Enterprise-ready solution for production environments
- **User-Friendly**: Intuitive interface requiring no technical expertise
- **Cross-Platform**: Runs on Windows, macOS, and Linux
- **Real-Time**: Live monitoring and transaction processing
- **Secure**: Advanced encryption and HSM support
- **Flexible**: Multiple message formats and protocols

## ✨ Features

### 🔧 Gateway Configuration
- **Multiple Gateway Types**:
    - **Server Mode**: Accept incoming connections
    - **Client Mode**: Connect to external hosts
    - **Proxy Mode**: Bridge between systems
- **Connection Types**:
    - TCP/IP with custom IP and port settings
    - RS232 serial communication
    - Dial-up connections
    - REST API integration
- **Advanced Settings**:
    - Connection timeout configuration
    - Maximum concurrent connections
    - Error handling options
    - Message length type selection

### 📊 Real-Time Monitoring
- **Live Transaction Tracking**: View transactions as they occur
- **Performance Metrics**: Connection statistics and throughput
- **Detailed Logging**: Comprehensive activity logs with filtering
- **Error Analysis**: Automatic error detection and reporting
- **Traffic Monitoring**: Bytes sent/received tracking
- **Connection Management**: Active connection status

### 🧪 Host Simulator
- **Built-in Testing**: No external dependencies required
- **Transaction Templates**: Pre-configured message templates
- **Custom Responses**: Configure automated response generation
- **Field Editor**: Interactive ISO8583 field editing
- **Message Types**: Support for various MTI (Message Type Indicators)
- **Processing Codes**: Configurable transaction processing codes

### 🔄 Multi-Format Support
- **Binary Format**: Native ISO8583 binary processing
- **Hex Format**: Hexadecimal representation
- **JSON Format**: Modern web-friendly format with nested structures
- **XML Format**: Hierarchical data representation
- **Key-Value Format**: Simple delimiter-separated pairs
- **YAML Configuration**: Flexible field mapping configuration

### 🔐 Advanced Security [WIP]
- **Encryption/Decryption**: Built-in cryptographic support
- **HSM Integration**: Hardware Security Module compatibility
- **Client Authentication**: Secure client ID and password validation
- **Key Management**: Comprehensive security key handling
- **Cipher Support**: Multiple encryption algorithms (AES, DES, etc.)
- **Secure Transmission**: Encrypted data transmission

### 📝 Log Management
- **Multiple Log Levels**: Simple, Raw Data, Text Data, Parsed Data
- **File Management**: Configurable log file names and sizes
- **Encoding Options**: Multiple text encoding support
- **Template Integration**: ISO8583 template-based parsing
- **Real-time Viewing**: Live log streaming and filtering

## 🚀 Installation

### System Requirements
- **Operating Systems**: Windows 10+, macOS 10.14+, Linux (Ubuntu 18.04+)
- **Java Runtime**: JDK 11 or higher
- **Memory**: Minimum 512MB RAM (2GB recommended)
- **Disk Space**: 100MB free space

### Download Options

#### Option 1: GitHub Releases (Recommended)
```bash
# Download latest release
wget https://github.com/hpkaushik121/Iso8583studio/releases/latest/download/ISO8583Studio.jar

# Run the application
java -jar ISO8583Studio.jar
```

#### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/hpkaushik121/Iso8583studio.git
cd Iso8583studio

# Build the project
./gradlew build

# Run the application
./gradlew run
```

## 🏁 Quick Start

### 1. First Launch
1. Download and run ISO8583Studio
2. The application will open with a default configuration
3. Navigate through the tabbed interface to explore features

### 2. Basic Gateway Setup
```kotlin
// Example configuration
Gateway Configuration:
├── Name: "Payment Gateway"
├── Type: Server
├── Address: 127.0.0.1
├── Port: 8080
├── Max Connections: 100
└── Timeout: 30 seconds
```

### 3. Create Your First Transaction
1. Go to **Host Simulator** tab
2. Click **Add New Transaction**
3. Set MTI (e.g., "0200" for Authorization Request)
4. Configure Processing Code (e.g., "000000")
5. Add required fields (PAN, Amount, etc.)
6. Test the transaction

## ⚙️ Configuration

### Gateway Types

#### Server Mode
```yaml
Configuration:
  gatewayType: SERVER
  serverAddress: "0.0.0.0"
  serverPort: 8080
  maxConcurrentConnections: 100
  transmissionType: SYNCHRONOUS
```

**Use Cases**:
- Payment processing systems
- Authorization servers
- Transaction switches

#### Client Mode
```yaml
Configuration:
  gatewayType: CLIENT
  destinationServer: "192.168.1.100"
  destinationPort: 8080
  clientID: "CLIENT001"
  locationID: "LOC001"
```

**Use Cases**:
- POS terminals
- ATM connections
- Mobile payment apps

#### Proxy Mode
```yaml
Configuration:
  gatewayType: PROXY
  # Incoming connections
  serverAddress: "0.0.0.0"
  serverPort: 8080
  # Outgoing connections
  destinationServer: "host.bank.com"
  destinationPort: 8443
```

**Use Cases**:
- Transaction routing
- Protocol translation
- Load balancing

### Connection Types

#### TCP/IP Configuration
```yaml
TCP/IP Settings:
  address: "192.168.1.100"
  port: 8080
  timeout: 30
  keepAlive: true
```

#### Serial Communication (RS232)
```yaml
Serial Settings:
  comPort: "COM1"
  baudRate: "115200"
  dataBits: 8
  stopBits: 1
  parity: "NONE"
```

#### REST API Integration
```yaml
REST Configuration:
  url: "https://api.payment.com/process"
  method: "POST"
  headers:
    Content-Type: "application/json"
    Authorization: "Bearer token"
```

### Message Format Configuration

#### YAML Configuration Example
```yaml
formatType: JSON
mti:
  nestedKey: header.messageType
fieldMappings:
  "2":
    nestedKey: card.pan
    description: "Primary Account Number"
  "3":
    key: processingCode
    description: "Processing Code"
  "4":
    nestedKey: transaction.amount
    description: "Transaction Amount"
  "11":
    nestedKey: header.traceNumber
    description: "Systems Trace Audit Number"
settings:
  prettyPrint: true
  encoding: UTF-8
```

## 🖥️ User Interface

### Main Interface Overview

![Gateway Configuration Interface](docs/images/img_1.png)

The configuration screen lists your simulator configurations on the left, with tabbed settings (Gateway Type, Transmission Settings, Log Settings, Advanced Options) for the selected configuration on the right.

### Tab Structure

#### 1. Gateway Type Tab
- **Gateway Type Selection**: Server, Client, or Proxy
- **Basic Information**: Name, description, creation date
- **Authentication Settings**: Client ID, Location ID, Password
- **Testing Tools**: PosGateway test, EVN Component test

#### 2. Transmission Settings Tab
- **Transmission Type**: Synchronous or Asynchronous
- **Incoming Connections**: TCP/IP, RS232, Dial-up settings
- **Outgoing Connections**: Destination server configuration
- **Advanced Options**: Timeouts, concurrent connections, error handling

#### 3. Log Settings Tab
- **Log Configuration**: File name, maximum size
- **Content Options**: Simple, Raw Data, Text Data, Parsed Data
- **Encoding Settings**: Character encoding selection
- **Template Integration**: ISO8583 parsing templates

#### 4. Advanced Options Tab
- **Property Grid**: Advanced configuration parameters
- **HSM Settings**: Hardware Security Module configuration
- **Performance Tuning**: Thread pool size, connection timeouts
- **Custom Parameters**: Specialized configuration options

#### 5. Key Settings Tab
- **Encryption Keys**: AES, DES, and other cipher keys
- **Key Management**: Add, modify, delete security keys
- **Cipher Configuration**: Algorithm and mode selection
- **Key Assignment**: Map keys to specific operations

### Monitor Screen

![Live Transaction Monitoring](docs/images/docs/host-simulator/overview.png)

The live view shows the gateway status, transaction count, and each transaction's formatted request/response alongside the raw hex payloads as messages flow through.

### Host Simulator Screen

![Host Simulator Transaction Settings](docs/images/img_3.png)

Define transaction templates (MTI and processing code) on the left and configure each ISO8583 field — PAN, processing code, amount, STAN, and more — on the right.

## 🔧 Advanced Features

### ISO8583 Message Editor

The built-in message editor provides comprehensive field editing capabilities:

```kotlin
// Example ISO8583 Message Structure
ISO8583 Message:
├── TPDU Header: 6000000000
├── Message Type: 0200
├── Bitmap: [Fields 2,3,4,11,12,13,22,25,35,41,42]
└── Data Fields:
    ├── Field 2 (PAN): 4541822000289640
    ├── Field 3 (Processing Code): 000000
    ├── Field 4 (Amount): 000009999000
    ├── Field 11 (STAN): 000077
    ├── Field 12 (Time): 111412
    └── Field 13 (Date): 0204
```

### Template Configuration

Create reusable message templates:

```yaml
# Purchase Transaction Template
messageType: "0200"
processingCode: "000000"
mandatoryFields:
  - field: 2
    name: "PAN"
    type: "LLVAR"
    maxLength: 19
  - field: 3
    name: "Processing Code"
    type: "FIXED"
    length: 6
  - field: 4
    name: "Amount"
    type: "FIXED"
    length: 12
optionalFields:
  - field: 22
    name: "POS Entry Mode"
    type: "FIXED"
    length: 3
```

### Format Conversion

Convert between different message formats:

```javascript
// Input (Binary ISO8583)
0200B220000100000000164111111111111111000000001000000077111412020400224111111111111111113456789012345

// Output (JSON)
{
  "header": {
    "messageType": "0200"
  },
  "card": {
    "pan": "4111111111111111"
  },
  "transaction": {
    "amount": "000000001000",
    "processingCode": "000000"
  },
  "system": {
    "traceNumber": "000077"
  }
}
```

### Security Configuration [WIP]

Configure encryption and security settings:

```kotlin
// Security Key Configuration
SecurityKey(
    name = "MasterKey",
    cipherType = CipherType.AES_256,
    cipherMode = CipherMode.CBC,
    keyValue = "0123456789ABCDEF0123456789ABCDEF",
    description = "Master encryption key for transactions"
)

// HSM Configuration
HSMConfig(
    enabled = true,
    provider = "SafeNet",
    slotNumber = 1,
    pin = "********"
)
```

## 📚 API Reference

### Configuration Classes

#### GatewayConfig
```kotlin
data class GatewayConfig(
    val name: String,
    val gatewayType: GatewayType,
    val serverAddress: String,
    val serverPort: Int,
    val destinationServer: String,
    val destinationPort: Int,
    val maxConcurrentConnection: Int,
    val transactionTimeOut: Int,
    val transmissionType: TransmissionType,
    val connectionType: ConnectionType,
    val logFileName: String,
    val maxLogSizeInMB: Int,
    val bitTemplate: Array<BitSpecific>,
    val restConfiguration: RestConfiguration?
    ... more
)
```

#### RestConfiguration
```kotlin
data class RestConfiguration(
    val url: String = "",
    val method: HttpMethod = HttpMethod.POST,
    val headers: Map<String, String> = emptyMap(),
    val timeout: Int = 30
)
```

#### BitSpecific
```kotlin
data class BitSpecific(
    val bitNumber: Byte,
    val bitLength: BitLength,
    val bitType: BitType,
    val maxLength: Int,
    val description: String
)
```

### Enumerations

#### GatewayType
```kotlin
enum class GatewayType {
    SERVER,    // Accept incoming connections
    CLIENT,    // Connect to external host
    PROXY.      // Bridge between systems
}
```

#### ConnectionType
```kotlin
enum class ConnectionType {
    TCP_IP,    // TCP/IP socket connection
    COM,       // Serial RS232 connection
    DIAL_UP,   // Dial-up modem connection
    REST       // REST API connection
}
```

#### MessageLengthType
```kotlin
enum class MessageLengthType {
    BCD(0),
    NONE(3),
    STRING_4(5),
    HEX_HL(1),
    HEX_LH(2)
}
```

#### CodeFormat
```kotlin
enum class CodeFormat(val displayName: String, val requiresYamlConfig: Boolean) {
    BYTE_ARRAY("Binary", false),
    HEX("Hexadecimal", false),
    JSON("JSON", true),
    XML("XML", true),
    PLAIN_TEXT("Key-Value", true)
}
```

## 🔍 Troubleshooting

### Common Issues

#### Connection Problems
```bash
Issue: "Connection refused"
Solution:
1. Check if target host is reachable
2. Verify port number is correct
3. Ensure firewall allows connections
4. Check if service is running on target port
```

#### Message Format Errors
```bash
Issue: "Invalid message format"
Solution:
1. Verify ISO8583 template configuration
2. Check field lengths and types
3. Validate bitmap configuration
4. Ensure proper encoding settings
```

#### Performance Issues
```bash
Issue: "Slow transaction processing"
Solution:
1. Increase thread pool size
2. Optimize connection timeout values
3. Check network latency
4. Monitor system resources
```

### Debug Mode

Enable debug logging for detailed troubleshooting:

```kotlin
// Add to log settings
LogLevel.DEBUG -> Enable detailed logging
LogLevel.TRACE -> Enable message tracing
LogLevel.ERROR -> Show only errors
```

### Log Analysis

Monitor log files for common patterns:

```bash
# Connection issues
grep "Connection" iso8583.log

# Transaction errors
grep "ERROR" iso8583.log | grep "Transaction"

# Performance metrics
grep "Processing time" iso8583.log
```

## 🤝 Contributing

### Development Setup

1. **Clone Repository**
```bash
git clone https://github.com/hpkaushik121/Iso8583studio.git
cd Iso8583studio
```

2. **Setup Development Environment**
```bash
# Install required tools
# - JDK 11+
# - IntelliJ IDEA (recommended)
# - Git

# Build project
./gradlew build
```

3. **Run Tests**
```bash
./gradlew test
```

### Code Style

Follow Kotlin coding conventions:

```kotlin
// Class names: PascalCase
class GatewayConfiguration

// Function names: camelCase
fun processTransaction()

// Constants: UPPER_SNAKE_CASE
const val DEFAULT_TIMEOUT = 30

// Variables: camelCase
val connectionManager = ConnectionManager()
```

### Submitting Changes

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Make changes and test thoroughly
4. Commit with descriptive messages: `git commit -m "Add REST API support"`
5. Push to your fork: `git push origin feature/new-feature`
6. Create a Pull Request

### Issue Reporting

When reporting issues, include:

```markdown
## Environment
- OS: Windows 10 / macOS 12 / Ubuntu 20.04
- Java Version: OpenJDK 11.0.2
- Application Version: v1.2.3

## Steps to Reproduce
1. Open ISO8583Studio
2. Configure gateway as Server
3. Start monitoring
4. Send test transaction

## Expected Behavior
Transaction should be processed successfully

## Actual Behavior
Application crashes with NullPointerException

## Additional Information
- Log files attached
- Configuration file included
- Screenshots of error
```

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- ISO8583 specification contributors
- Kotlin Multiplatform team
- Compose Desktop developers
- Financial technology community
- Open source contributors

## 📞 Support

- **GitHub Issues**: [Report bugs and request features](https://github.com/hpkaushik121/Iso8583studio/issues)
- **Discussions**: [Community discussions and Q&A](https://github.com/hpkaushik121/Iso8583studio/discussions)
- **Documentation**: [Wiki pages](https://github.com/hpkaushik121/Iso8583studio/wiki)
- **Releases**: [Download latest versions](https://github.com/hpkaushik121/Iso8583studio/releases)

---

**ISO8583Studio** - Professional Financial Transaction Processing Made Simple

*Built with ❤️ for the financial technology community*