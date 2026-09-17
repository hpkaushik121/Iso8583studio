---
title: "EMV ATR Parser: Reading Answer-to-Reset on Smart Cards"
description: "Decode smart card ATR bytes: TS, T0, T1, historical bytes, and protocol info for EMV and payment testing with ISO8583Studio."
date: "2025-06-01"
tags: [ATR, smart card, EMV, ISO7816, payment testing]
category: "EMV Tools"
author: "AiCortex Team"
read_time: "8 min read"
---

The Answer-to-Reset (ATR) is the first message you receive from a contact smart card after reset. It tells the reader which physical and logical protocols the card supports, how timing should be interpreted, and often carries **historical bytes** that manufacturers use for identification. For payment engineers, the ATR is not “the transaction”—but misreading it can cause wrong baud classes, wrong protocol selection (T=0 vs T=1), or wasted hours chasing communication issues that are really negotiation failures.

**ISO8583Studio** ([iso8583.studio](https://iso8583.studio)) is a free cross-platform desktop application for Windows, macOS, and Linux with 70+ tools for payment testing—including EMV utilities such as ATR parsing alongside Host Simulator, HSM Simulator (PayShield 10K), APDU Simulator, and cryptography suites. This article explains the ATR layout you will see in the field and how to interpret it methodically.

## Why the ATR matters in payment projects

EMV transactions ultimately ride on ISO 7816-style command/response pairs (APDUs). Before you issue `SELECT` or `GET PROCESSING OPTIONS`, the reader must:

1. Apply the correct voltage and clock.
2. Negotiate the transmission protocol (typically T=0 or T=1 for many EMV cards).
3. Establish block sizes and timing compatible with the card’s capabilities.

The ATR is the card’s “hello” packet containing the parameters needed for that negotiation. Test harnesses that dump ATR hex without parsing it leave teams blind to subtle incompatibilities.

## High-level ATR structure

An ATR is a variable-length byte string. It always begins with **TS**, followed by **T0**, and then optional interface bytes **TA1, TB1, TC1, TD1**, etc., followed by **historical bytes** and finally **TCK** in many cases.

| Section            | Typical purpose                                      |
|--------------------|------------------------------------------------------|
| TS                 | Convention (direct/inverse) and initial timing     |
| T0                 | Number of historical bytes; presence flags for Tx    |
| TA/TB/TC/TD        | Protocol parameters, clock stop, waiting times     |
| Historical bytes   | Card data (category indicator, optional proprietary) |
| TCK                | Check byte (present if more than one protocol)     |

Readers that mis-handle inverse convention can decode the entire stream incorrectly—so **TS** is not decorative.

## TS: the initial character

**TS** establishes bit convention:

- **Direct convention** — Data is interpreted as you expect in most PC-side tooling.
- **Inverse convention** — Bits are logically inverted; parsers must normalize before interpreting subsequent bytes.

If your trace shows “garbage” immediately after reset, verify whether your capture tool already inverted bits. Payment-focused tools often present the **already-normalized** ATR for readability.

## T0: historical bytes and presence bits

**T0** encodes two things:

- **b4–b1**: the number of **historical bytes** (0–15).
- **b8–b5**: flags indicating whether **TA1**, **TB1**, **TC1**, and **TD1** are present.

These flags determine how many interface bytes follow before historical bytes begin. This chaining continues: if **TD1** is present, it can announce **TA2**, **TB2**, and so on, and can indicate support for further protocols (e.g., T=1).

## TA1: clock rate and bit rate

**TA1** (when present) encodes clock frequency divisors and bitrate adjustment factors. In practical terms, it tells the terminal how aggressively it may increase communication speed. For interoperability testing, compare:

- What the card *claims* it supports.
- What your terminal *selects* after negotiation.

Mismatches here show up as intermittent timeouts—not cryptographic errors.

## TD bytes: protocol identifiers and chaining

**TD1** carries a **protocol number** for the first offered protocol (commonly T=0 or T=1) and a bit indicating whether another **TD** exists. This chaining is how multi-protocol cards declare fallbacks.

For EMV work, you will frequently see T=0 or T=1. Contactless paths differ (not ATR-based in the same way), so always scope your parser to the interface under test.

## Historical bytes: what to expect

Historical bytes are **not** a full EMV file system; they are a compact descriptor. ISO 7816 defines a **category indicator** in the first historical byte in many layouts, with optional manufacturer data following.

Use historical bytes for:

- Lab inventory (“which batch is this plastic?”).
- First-line triage when two cards behave differently despite identical applications.

Do **not** rely on historical bytes alone for cryptographic identity—that belongs to issuer keys and application data read later.

## TCK: checksum

When applicable, **TCK** ensures the bytes from T0 through the last interface byte and historical bytes XOR cleanly to `0x00` with the checksum byte included—per ISO 7816-3 rules. If your ATR fails TCK:

- Suspect a bad physical read.
- Suspect a bit-level convention error.
- Suspect a truncated capture.

## Practical parsing workflow

1. Capture raw ATR from your reader in hex.
2. Parse **TS**, normalize convention if needed.
3. Walk **T0** presence flags; consume **TA/TB/TC/TD** in order.
4. Read **historical bytes** using the count from **T0**.
5. Validate **TCK** when required.
6. Map protocol identifiers to your stack’s next step (PPS / PPS-like procedures as applicable).

## Example (illustrative only)

Suppose you record:

```
3B 6E 00 00 80 31 80 66 B0 84 12 01 6E 82 90 00
```

Your tooling should identify:

- Initial character and convention.
- Whether TA/TB/TC/TD bytes are present.
- Historical byte boundaries.

Never hand-guess split points on long ATRs—use a structured parser.

## Using ISO8583Studio alongside ATR analysis

**ISO8583Studio** integrates EMV-oriented utilities (tag parser, cryptogram helpers, SDA/DDA, ATR) with broader payment testing tools: Host Simulator, HSM Simulator (PayShield 10K), APDU Simulator, converters (Base64, BCD, Luhn), and key-management calculators (Thales, Futurex, Atalla, SafeNet, TR-31, keyshare, DEA keys). That means you can move from **ATR → protocol choice → APDU traces → crypto checks** without switching contexts.

## Conclusion

The ATR is a compact specification of how your reader and card will cooperate at the wire level. Parsing it reliably prevents a whole class of false positives in EMV debugging. **Get ISO8583Studio** at [https://iso8583.studio](https://iso8583.studio)—free, offline-friendly, and built for payment testers who need both EMV depth and a broad cryptography and key-management toolbox in one desktop app.
