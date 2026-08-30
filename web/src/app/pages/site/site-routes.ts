import { Routes } from '@angular/router';

/** One lazy component per page. */
export const siteRoutes: Routes = [
  {
    path: "",
    loadComponent: () => import('./home').then((c) => c.HomePage),
    data: { seo: {
      "title": "ISO8583Studio — Payment Testing & Simulation Platform",
      "description": "ISO8583Studio — the payment engineer's workbench. Watch a live 0200 parse to the bit, then simulate hosts, HSMs and switches. 64 tools & 9 simulators for ISO 8583, EMV, HSM and keys on Windows, macOS & Linux.",
      "keywords": "ISO 8583, ISO8583, payment testing, transaction simulator, host simulator, HSM simulator, EMV tools, DUKPT, PIN block, payment switch, fintech tool",
      "path": "/",
      "ogType": "website",
      "image": "https://iso8583.studio/images/img.png",
      "robots": "index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1",
      "author": "Sourabh Kaushik",
      "jsonLd": {
        "@context": "https://schema.org",
        "@type": "SoftwareApplication",
        "name": "ISO8583Studio",
        "operatingSystem": "Windows, macOS, Linux",
        "applicationCategory": "DeveloperApplication",
        "offers": {
          "@type": "Offer",
          "price": "0",
          "priceCurrency": "USD"
        },
        "url": "https://iso8583.studio/",
        "downloadUrl": "https://github.com/hpkaushik121/Iso8583studio/releases/latest",
        "softwareVersion": "latest",
        "description": "The payment engineer's workbench: nine simulators and 64 tools for ISO 8583, EMV, HSM and key operations, on Windows, macOS and Linux.",
        "author": {
          "@type": "Organization",
          "name": "AiCortex",
          "url": "https://iso8583.studio/"
        },
        "license": "https://github.com/hpkaushik121/Iso8583studio/blob/main/LICENSE",
        "featureList": [
          "ISO 8583 Message Processing",
          "Host Simulator",
          "HSM Simulator (Thales payShield 10K)",
          "POS Simulator",
          "APDU Simulator",
          "ATM Simulator",
          "ECR Simulator",
          "Switch Simulator",
          "Issuer Simulator",
          "Gateway Configuration (TCP/IP, REST, RS232, Dial-Up)",
          "EMV Certification Tools",
          "PIN Block Operations",
          "Key Management (DUKPT, TR-31, 3DES, AES)",
          "Real-time Transaction Monitoring"
        ]
      }
    } },
  },
  {
    path: "privacy-policy",
    loadComponent: () => import('./privacy-policy').then((c) => c.PrivacyPolicyPage),
    data: { seo: {
      "title": "Privacy Policy - ISO8583Studio",
      "description": "ISO8583Studio Privacy Policy - How we protect your data. Desktop-first application with local data processing. GDPR, CCPA compliant. No transaction data collection. Transparent data practices for fintech software.",
      "keywords": "ISO8583Studio privacy policy, fintech privacy, payment software data protection, ISO 8583 data privacy, desktop application privacy, GDPR compliant fintech, CCPA compliant payment software, local data processing, financial data security, PCI DSS guidelines",
      "path": "/privacy-policy",
      "ogType": "website",
      "image": "https://iso8583.studio/images/app.png",
      "robots": "index, follow"
    } },
  },
  {
    path: "terms-and-conditions",
    loadComponent: () => import('./terms-and-conditions').then((c) => c.TermsAndConditionsPage),
    data: { seo: {
      "title": "Terms and Conditions - ISO8583Studio",
      "description": "ISO8583Studio Terms and Conditions - Open source software agreement under Apache License 2.0. Free to use, modify, and distribute. No warranties. Professional fintech desktop application.",
      "keywords": "ISO8583Studio terms and conditions, ISO8583Studio license, Apache License 2.0, open source payment software, fintech software license, ISO 8583 software terms, payment processing software agreement, free payment software, open source fintech",
      "path": "/terms-and-conditions",
      "ogType": "website",
      "image": "https://iso8583.studio/images/app.png",
      "robots": "index, follow"
    } },
  },
  {
    path: "cloud-simulators",
    loadComponent: () => import('./cloud-simulators').then((c) => c.CloudSimulatorsPage),
    data: { seo: {
      "title": "Cloud Payment Simulators — Host, HSM, POS, Issuer | ISO8583Studio",
      "description": "Hosted host, HSM, POS, card, acquirer and issuer simulators. Scriptable, CI-ready payment ecosystem testing without hardware.",
      "path": "/cloud-simulators",
      "ogType": "website"
    } },
  },
  {
    path: "contact",
    loadComponent: () => import('./contact').then((c) => c.ContactPage),
    data: { seo: {
      "title": "Contact Us - ISO8583Studio",
      "description": "Talk to the ISO8583Studio team — certification and middleware engagements, support, bug reports and feature requests.",
      "path": "/contact",
      "ogType": "website"
    } },
  },
  {
    path: "download",
    loadComponent: () => import('./download').then((c) => c.DownloadPage),
    data: { seo: {
      "title": "Download ISO8583Studio — Free Payment Testing Studio",
      "description": "Download ISO8583Studio for Windows, macOS or Linux. Free, open-source payment testing: 9 simulators and 64 tools for ISO 8583, EMV, HSM and key operations.",
      "keywords": "download iso8583 simulator, payment testing tool download, iso 8583 software, hsm simulator download, free payment simulator",
      "path": "/download",
      "ogType": "website"
    } },
  },
  {
    path: "emv-certification",
    loadComponent: () => import('./emv-certification').then((c) => c.EmvCertificationPage),
    data: { seo: {
      "title": "EMV Certification Services — L1, L2, L3 | ISO8583Studio",
      "description": "EMV Level 1, 2 and 3 certification and development services: kernel expertise, pre-certification testing and lab submission support.",
      "path": "/emv-certification",
      "ogType": "website"
    } },
  },
  {
    path: "kernel",
    loadComponent: () => import('./kernel').then((c) => c.KernelPage),
    data: { seo: {
      "title": "Kernel Development & Hardware Integration | ISO8583Studio",
      "description": "Android kernel customization, device driver development and payment hardware integration for smart POS devices.",
      "path": "/kernel",
      "ogType": "website"
    } },
  },
  {
    path: "middleware",
    loadComponent: () => import('./middleware').then((c) => c.MiddlewarePage),
    data: { seo: {
      "title": "Payment Middleware & Transaction Orchestration | ISO8583Studio",
      "description": "Payment middleware: intelligent routing, protocol translation between ISO 8583, JSON, XML and REST, with full transaction transparency.",
      "path": "/middleware",
      "ogType": "website"
    } },
  },
  {
    path: "pro",
    loadComponent: () => import('./pro').then((c) => c.ProPage),
    data: { seo: {
      "title": "ISO8583Studio Pro — register for early access",
      "description": "ISO8583Studio Pro — hosted simulator endpoints, scheme certification packs, unlimited scripted test suites, priority support. Register for access.",
      "path": "/pro",
      "ogType": "website"
    } },
  },
  {
    path: "docs",
    loadComponent: () => import('./docs').then((c) => c.DocsPage),
    data: { seo: {
      "title": "Documentation - ISO8583Studio",
      "description": "ISO8583Studio documentation hub — guides for all nine payment simulators, tool references for EMV, cryptography, keys and payment utilities, plus certification and middleware services.",
      "keywords": "ISO8583Studio documentation, ISO 8583 guide, host simulator tutorial, HSM simulator guide, payShield documentation, EMV tools reference, payment testing documentation",
      "path": "/docs",
      "ogType": "website",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/apdu-simulator",
    loadComponent: () => import('./docs-apdu-simulator').then((c) => c.DocsApduSimulatorPage),
    data: { seo: {
      "title": "APDU Simulator Documentation - ISO8583Studio",
      "description": "ISO8583Studio APDU Simulator documentation - run an EMV card profile in-process, drive a real card through a PC/SC reader, or emulate a contact card on STM32 firmware for an external POS terminal. Trace every APDU exchange, run test plans and export an L3 report.",
      "keywords": "APDU simulator, EMV card emulator, ISO 7816-4 APDU, card profile, ATR, AID selection, PC/SC reader, smart card emulation, STM32 card emulator, USB-CDC card, EMV test plan, L3 certification report, Visa VCPS, Mastercard M/Chip, logic analyzer capture, sigrok, APDU trace, status word, issuer master key, EMV personalization",
      "path": "/docs/apdu-simulator",
      "ogType": "article",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/atm-simulator",
    loadComponent: () => import('./docs-atm-simulator').then((c) => c.DocsAtmSimulatorPage),
    data: { seo: {
      "title": "ATM Simulator Documentation - ISO8583Studio",
      "description": "ISO8583Studio ATM Simulator (in development) - drive ATM cash-withdrawal, balance and PIN-change transactions to a host over ISO 8583 with NDC/DDC device flows.",
      "keywords": "ATM simulator, NDC DDC simulator, cash withdrawal testing, ATM ISO 8583, ATM host testing, PIN change, balance inquiry",
      "path": "/docs/atm-simulator",
      "ogType": "website"
    } },
  },
  {
    path: "docs/card-validation",
    loadComponent: () => import('./docs-card-validation').then((c) => c.DocsCardValidationPage),
    data: { seo: {
      "title": "Card Validation Tools Documentation - ISO8583Studio",
      "description": "ISO8583Studio Card Validation tools documentation - Generate and validate MasterCard dynamic CVC3 and American Express CSC card security codes.",
      "keywords": "CVV calculator, CVV2, CVC2, CVC3, MasterCard CVC, AMEX CSC, card security code, card verification value, payment validation, dynamic CVC3",
      "path": "/docs/card-validation",
      "ogType": "website",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/cipher-tools",
    loadComponent: () => import('./docs-cipher-tools').then((c) => c.DocsCipherToolsPage),
    data: { seo: {
      "title": "Cryptographic Tools Documentation - ISO8583Studio",
      "description": "ISO8583Studio Cryptographic Tools documentation - AES, DES/3DES, RSA, ECDSA, and Format-Preserving Encryption (FPE) calculators for payment system testing.",
      "keywords": "AES calculator, DES 3DES calculator, RSA calculator, ECDSA, FPE FF1 FF3, format preserving encryption, Thales RSA, payment encryption tools, hex key encryption, ECB CBC mode",
      "path": "/docs/cipher-tools",
      "ogType": "website",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/contributing",
    loadComponent: () => import('./docs-contributing').then((c) => c.DocsContributingPage),
    data: { seo: {
      "title": "How to Contribute - ISO8583Studio",
      "description": "Contribute to ISO8583Studio — Kotlin Multiplatform development setup, prerequisites, project layout, code style and the pull-request flow.",
      "path": "/docs/contributing",
      "ogType": "website"
    } },
  },
  {
    path: "docs/dukpt-tools",
    loadComponent: () => import('./docs-dukpt-tools').then((c) => c.DocsDukptToolsPage),
    data: { seo: {
      "title": "DUKPT Tools Documentation - ISO8583Studio",
      "description": "ISO8583Studio DUKPT Tools documentation - Derive transaction keys from BDK and KSN for both DUKPT AES (X9.24-3) and DUKPT ISO 9797 (X9.24-1) variants.",
      "keywords": "DUKPT calculator, DUKPT AES, DUKPT ISO 9797, BDK, KSN, IPEK, key serial number, base derivation key, ANSI X9.24-1, ANSI X9.24-3, transaction key derivation",
      "path": "/docs/dukpt-tools",
      "ogType": "website",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/ecr-simulator",
    loadComponent: () => import('./docs-ecr-simulator').then((c) => c.DocsEcrSimulatorPage),
    data: { seo: {
      "title": "ECR Simulator Documentation - ISO8583Studio",
      "description": "ISO8583Studio ECR Simulator (in development) - simulate an electronic cash register integrated with a payment terminal, exchanging sale, void and refund messages.",
      "keywords": "ECR simulator, electronic cash register, ECR POS integration, register terminal protocol, sale void refund, RS232 ECR, cash register payment",
      "path": "/docs/ecr-simulator",
      "ogType": "website"
    } },
  },
  {
    path: "docs/emv-tools",
    loadComponent: () => import('./docs-emv-tools').then((c) => c.DocsEmvToolsPage),
    data: { seo: {
      "title": "EMV Tools Documentation - ISO8583Studio",
      "description": "ISO8583Studio EMV Tools documentation - SDA and DDA verification, EMV 4.1 / 4.2 / M-Chip / VSDC cryptogram calculators, issuer script secure messaging, CAP token computation and Visa HCE contactless keys.",
      "keywords": "EMV tools, SDA, DDA, UDK derivation, session key, application cryptogram, ARQC, TC, AAC, ARPC, M/Chip, VSDC, CAP token, EMV secure messaging, HCE, LUK, qVSDC",
      "path": "/docs/emv-tools",
      "ogType": "website",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/host-simulator",
    loadComponent: () => import('./docs-host-simulator').then((c) => c.DocsHostSimulatorPage),
    data: { seo: {
      "title": "Host Simulator Documentation - ISO8583Studio",
      "description": "ISO8583Studio Host Simulator documentation - Simulate acquirer and issuer host responses for POS terminals and ATMs. Configure Server/Client/Proxy gateways, transaction rules, ISO 8583 templates, and dynamic placeholders.",
      "keywords": "ISO 8583 host simulator, payment host emulator, acquirer simulator, issuer simulator, POS terminal testing, ATM testing tool, ISO8583 message builder, transaction rule engine, payment gateway simulator, TCP/IP payment gateway, REST payment API, RS232 serial payment, dial-up payment connection, ISO 8583 message parser, MTI message type indicator, processing code, ISO 8583 field mapping, ByteArray message format, JSON ISO8583, XML ISO8583, payment proxy server, unsolicited message, host handler, transaction monitoring, payment protocol testing, financial message analysis, payment testing environment, gateway configuration, SSL TLS payment, DES 3DES AES encryption, payment log visualization",
      "path": "/docs/host-simulator",
      "ogType": "article",
      "image": "https://iso8583.studio/images/app.png",
      "robots": "index, follow",
      "jsonLd": {
        "@context": "https://schema.org",
        "@type": "TechArticle",
        "headline": "ISO8583Studio Host Simulator Documentation",
        "description": "Comprehensive documentation for the ISO8583Studio Host Simulator - simulate acquirer and issuer host responses for payment terminals, ATMs, and client applications.",
        "url": "https://iso8583.studio/docs/host-simulator/",
        "author": {
          "@type": "Organization",
          "name": "AiCortex"
        },
        "about": [
          {
            "@type": "Thing",
            "name": "ISO 8583"
          },
          {
            "@type": "Thing",
            "name": "Payment Processing"
          },
          {
            "@type": "Thing",
            "name": "Host Simulation"
          }
        ]
      }
    } },
  },
  {
    path: "docs/hsm-command-console",
    loadComponent: () => import('./docs-hsm-command-console').then((c) => c.DocsHsmCommandConsolePage),
    data: { seo: {
      "title": "HSM Command Console Documentation - ISO8583Studio",
      "description": "ISO8583Studio HSM Command Console - a host-command client for Thales payShield, Futurex, SafeNet Luna, Utimaco and nCipher HSMs. Send commands, chain scenarios, and run load tests over TCP/IP with optional TLS.",
      "keywords": "HSM command console, Thales payShield console, HSM host commands, payShield 10K, Futurex Excrypt, SafeNet Luna, Utimaco CryptoServer, nCipher nShield, HSM load test, HSM scenario builder, HSM TLS client",
      "path": "/docs/hsm-command-console",
      "ogType": "website"
    } },
  },
  {
    path: "docs/hsm-simulator",
    loadComponent: () => import('./docs-hsm-simulator').then((c) => c.DocsHsmSimulatorPage),
    data: { seo: {
      "title": "HSM Simulator Documentation - ISO8583Studio",
      "description": "ISO8583Studio HSM Simulator documentation - Emulate a payment HSM from a device profile: pick vendor, model and firmware, then drive the Thales payShield command engine. 35+ host commands: key management, PIN operations, MAC generation, RSA, encryption, DUKPT, and LMK storage.",
      "keywords": "HSM simulator, HSM vendor profiles, SafeNet Luna, Utimaco CryptoServer, Futurex Excrypt, nCipher nShield, Thales payShield 10K emulator, payShield 9000, hardware security module, HSM emulator, payment HSM, key management, PIN block operations, PIN translation, PIN verification, DUKPT key derivation, ZMK zone master key, ZPK zone PIN key, TPK terminal PIN key, BDK base derivation key, ZEK zone encryption key, 3DES encryption, AES encryption, RSA key generation, MAC generation, ISO 9797, LMK local master key, key check value KCV, PIN block format, ISO 9564, VISA PVV, IBM 3624 PIN, cryptographic operations, payment security, HSM host commands, payShield commands, HSM testing, payment cryptography, financial HSM, key injection, secure key storage, HSM API, PIN encryption",
      "path": "/docs/hsm-simulator",
      "ogType": "article",
      "image": "https://iso8583.studio/images/app.png",
      "robots": "index, follow",
      "jsonLd": {
        "@context": "https://schema.org",
        "@type": "TechArticle",
        "headline": "HSM Simulator Documentation - Thales payShield 10K Emulation",
        "description": "Complete documentation for the ISO8583Studio HSM Simulator emulating a Thales payShield 10K. Covers key management, PIN operations, encryption, MAC, RSA, and 35+ host commands.",
        "url": "https://iso8583.studio/docs/hsm-simulator/",
        "author": {
          "@type": "Organization",
          "name": "AiCortex"
        },
        "about": [
          {
            "@type": "Thing",
            "name": "Hardware Security Module"
          },
          {
            "@type": "Thing",
            "name": "Thales payShield"
          },
          {
            "@type": "Thing",
            "name": "Payment Cryptography"
          }
        ]
      }
    } },
  },
  {
    path: "docs/installation",
    loadComponent: () => import('./docs-installation').then((c) => c.DocsInstallationPage),
    data: { seo: {
      "title": "Installation - ISO8583Studio",
      "description": "How to install ISO8583Studio on Windows, macOS and Linux — prerequisites (JDK 11+), the release JAR, and building the Kotlin Multiplatform source with Gradle.",
      "path": "/docs/installation",
      "ogType": "website"
    } },
  },
  {
    path: "docs/issuer-simulator",
    loadComponent: () => import('./docs-issuer-simulator').then((c) => c.DocsIssuerSimulatorPage),
    data: { seo: {
      "title": "Issuer System Documentation - ISO8583Studio",
      "description": "ISO8583Studio Issuer System (in development) - issuer-side authorization host that approves or declines transactions, verifies PINs and returns ISO 8583 0210 responses.",
      "keywords": "issuer simulator, issuer authorization host, ISO 8583 0210, PIN verification, stand-in authorization, card account validation, decline response codes",
      "path": "/docs/issuer-simulator",
      "ogType": "website"
    } },
  },
  {
    path: "docs/key-tools",
    loadComponent: () => import('./docs-key-tools').then((c) => c.DocsKeyToolsPage),
    data: { seo: {
      "title": "Key Management Tools Documentation - ISO8583Studio",
      "description": "ISO8583Studio Key Management Tools documentation - DEA / 3DES key utilities, TR-31 and Thales key blocks, vendor-specific key calculators (Thales, Futurex, Atalla, Safenet), keyshare generation, and SSL/X.509 certificate workflows.",
      "keywords": "TR-31 key block, Thales key block, key calculator, Futurex key, Atalla key, Safenet key, keyshare splitting, SSL certificate, X.509, CSR, DES parity, key management, payment HSM keys",
      "path": "/docs/key-tools",
      "ogType": "website",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/mac-tools",
    loadComponent: () => import('./docs-mac-tools').then((c) => c.DocsMacToolsPage),
    data: { seo: {
      "title": "MAC Tools Documentation - ISO8583Studio",
      "description": "ISO8583Studio MAC Tools documentation - HMAC, ISO/IEC 9797-1, ANSI X9.9 / X9.19 and TDES CBC-MAC calculators for payment integrity testing.",
      "keywords": "MAC calculator, HMAC, ISO 9797-1, ANSI X9.9, ANSI X9.19, TDES CBC MAC, message authentication code, payment integrity",
      "path": "/docs/mac-tools",
      "ogType": "website",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/payment-simulators",
    loadComponent: () => import('./docs-payment-simulators').then((c) => c.DocsPaymentSimulatorsPage),
    data: { seo: {
      "title": "Payment Simulators - ISO8583Studio",
      "description": "All nine ISO8583Studio payment simulators — host, HSM, HSM command console, POS, APDU, switch, issuer, ATM and ECR — with documentation for each.",
      "keywords": "payment simulators, ISO 8583 simulator, host simulator, HSM simulator, POS simulator, APDU simulator, payment switch, issuer simulator, ATM simulator, ECR simulator",
      "path": "/docs/payment-simulators",
      "ogType": "website"
    } },
  },
  {
    path: "docs/payment-switch",
    loadComponent: () => import('./docs-payment-switch').then((c) => c.DocsPaymentSwitchPage),
    data: { seo: {
      "title": "Payment Switch Documentation - ISO8583Studio",
      "description": "ISO8583Studio Payment Switch (in development) - route and translate ISO 8583 traffic between acquirers and issuers with BIN routing and protocol translation.",
      "keywords": "payment switch simulator, ISO 8583 switch, transaction routing, BIN routing, protocol translation, acquirer issuer switch, stand-in processing",
      "path": "/docs/payment-switch",
      "ogType": "website"
    } },
  },
  {
    path: "docs/pin-tools",
    loadComponent: () => import('./docs-pin-tools').then((c) => c.DocsPinToolsPage),
    data: { seo: {
      "title": "Payment Utilities Documentation - ISO8583Studio",
      "description": "ISO8583Studio Payment Utilities documentation - PIN block calculators (ISO 9564 formats 0-4, OEM variants), AES PIN block, TPK-to-ZPK PIN block translation and DUKPT PIN encryption.",
      "keywords": "PIN block calculator, ISO 9564, PIN block format 0, format 1, format 3, format 4, AES PIN block, PIN block translation, TPK ZPK, DUKPT PIN, payment PIN testing",
      "path": "/docs/pin-tools",
      "ogType": "website",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/pos-simulator",
    loadComponent: () => import('./docs-pos-simulator').then((c) => c.DocsPosSimulatorPage),
    data: { seo: {
      "title": "POS Simulator Documentation - ISO8583Studio",
      "description": "ISO8583Studio POS Simulator - boot a real Android terminal in an emulator. Pick a PAX, Ingenico, Sunmi, Verifone, Kozen or Newland model, apply its screen, memory, peripherals and spoofed build identity, then install and run your payment app against it.",
      "keywords": "POS simulator, Android POS emulator, payment terminal emulator, PAX A910S emulator, PAX A920, Ingenico AXIUM, Sunmi P2, Verifone T650c, Kozen N2, Newland N910, NexGo N86, Telpo M1, AVD terminal profile, ro.product spoofing, Android emulator config.ini, terminal hardware profile, thermal printer simulation, PED PIN block, PC/SC card reader, payment app testing",
      "path": "/docs/pos-simulator",
      "ogType": "website"
    } },
  },
  {
    path: "docs/utility-tools",
    loadComponent: () => import('./docs-utility-tools').then((c) => c.DocsUtilityToolsPage),
    data: { seo: {
      "title": "Data Converters Documentation - ISO8583Studio",
      "description": "ISO8583Studio Data Converters documentation - Base64 and Base94 encoders, BCD converter, character encoding, Luhn check digits and the Track 2 codec for EMV tag 57.",
      "keywords": "Base64 encoder, Base94 encoder, BCD converter, binary coded decimal, character encoder, hex to ASCII, Luhn check digit, Mod 10, Track 2 codec, EMV tag 57, magstripe track 2",
      "path": "/docs/utility-tools",
      "ogType": "website",
      "robots": "index, follow"
    } },
  },
  {
    path: "docs/versions",
    loadComponent: () => import('./docs-versions').then((c) => c.DocsVersionsPage),
    data: { seo: {
      "title": "Versions - ISO8583Studio",
      "description": "ISO8583Studio release versions — current release, download links and the release channel on GitHub.",
      "path": "/docs/versions",
      "ogType": "website"
    } },
  },
];
