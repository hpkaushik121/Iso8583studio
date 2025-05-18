# Overview

ISO8583 is the international standard for financial transaction card-originated interchange messaging. It is used by payment systems worldwide to communicate transaction data between different parties in card payment ecosystems. ISO8583Studio aims to make working with these messages easier for developers, testers, and financial system engineers.

## Key Features

### Host Simulator
- Easily simulate financial hosts to test payment applications and gateways
- Configure response templates for different transaction types
- Support for various message length encodings (BCD, High-Low, Low-High, None)
- Dynamic field value generation including timestamps, echoed values, and random data
- Configurable response timing and network behavior simulation

### Customizable Fields
- Full control over ISO8583 field definitions and values
- Support for all ISO8583 data types (numeric, alphanumeric, binary, etc.)
- Custom field length configuration (fixed, LLVAR, LLLVAR)
- Field templates library with common financial message fields
- Ability to extend and customize field definitions

### Interceptor
- Intercept and modify messages in real-time between client and server
- Rules-based message modification capabilities
- Conditional routing based on message content
- Ability to inject, modify, or delete specific fields
- Script custom transformations using Kotlin scripting
- Save and replay intercepted messages for testing
- Visual diff comparison between original and modified messages

### Monitor
- Comprehensive transaction monitoring in real-time
- Detailed view of both raw and parsed message formats
- Color-coded field visualization for easier analysis
- Search and filtering capabilities across transaction history
- Full hexadecimal and ASCII dump of message contents
- Bitmap analysis showing which fields are present
- Field-by-field comparison between request and response
- Transaction timing statistics and performance metrics
- Export functionality for logs and captured transactions
- Support for various logging formats (JSON, XML, CSV)
- Timeline view of message sequence for multi-message transactions
- Aggregated statistics for transaction volumes, types, and response codes
- Alert configuration for specific transaction conditions or error codes
- Integration with external logging systems

The ISO8583Studio provides a complete toolkit for financial message testing, debugging, and development, offering capabilities typically found only in enterprise-grade payment testing solutions but with a modern, user-friendly interface and powerful customization options.
