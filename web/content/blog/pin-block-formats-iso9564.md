---
title: "ISO 9564 PIN Block Formats 0–4: What They Are and How They Differ"
description: "Understand ISO 9564 PIN block formats 0–4: layout, padding, and when each format is used in payment systems and HSM workflows."
date: "2025-07-15"
tags: [PIN, ISO 9564, PIN block, payment security, HSM]
category: "PIN Security"
author: "AiCortex Team"
read_time: "7 min read"
---

If you have ever stared at a 16-byte hex string labeled “PIN block” and wondered whether you are looking at Format 0, Format 1, or something your gateway invented last Tuesday, you are not alone. PIN blocks are one of the most frequently misunderstood artifacts in card payments—yet they sit at the center of online PIN verification, ATM PIN change, and host-to-HSM cryptography.

This article explains **ISO 9564** PIN block formats **0 through 4** at a practical level: what each format carries, how padding works, and why the “same PIN” can look completely different depending on format and keying. For hands-on generation and inspection, **ISO8583Studio** ([iso8583.studio](https://iso8583.studio)) is a free cross-platform desktop app (Windows, macOS, Linux) with 70+ payment tools, including PIN utilities that help you build and validate PIN blocks without chaining together fragile scripts.

## Why PIN blocks exist

A Personal Identification Number is rarely transmitted as digits alone. Instead, systems wrap the PIN into a fixed-width block (commonly 64 bits / 8 bytes, sometimes 128 bits for AES variants) and encrypt that block under a PIN encryption key—typically a **TPK** (terminal PIN key) or **ZPK** (zone PIN key) in classic terminology—before it crosses untrusted networks.

The **format** defines how PIN length, PIN digits, and fill digits are arranged *before* encryption. If you encrypt the wrong layout, verification will fail even when the PIN and keys are correct.

## Format 0 (ANSI) — the common ISO case

**ISO 9564-1 Format 0** is widely used in international interchange. Conceptually:

- The left nibble of the first byte encodes the **PIN length** (number of PIN digits).
- Following nibbles carry the PIN digits in **BCD** (Binary Coded Decimal), each digit 0–9 occupying one nibble.
- Remaining nibbles are filled with a **padding nibble** (commonly `F`) until the block is complete for the algorithm width.

For an 8-byte DES-style block, you get a well-defined pattern: length + PIN digits + padding. The critical testing mistake is treating ASCII ‘0’–‘9’ bytes as BCD—**they are not interchangeable**.

### Quick sanity checks for testers

- Confirm PIN length matches the digit count you typed.
- Confirm there are no illegal nibbles (e.g., `A`–`E`) in the PIN digit region unless your implementation explicitly allows non-decimal experiments (most production systems do not).

## Format 1 (supplementary) — random padding

**Format 1** also encodes PIN length and PIN digits, but introduces **random padding nibbles** in the fill area (rather than a fixed filler like `F`). The goal is to reduce determinism: two encryptions of the same PIN should not necessarily yield identical plaintext PIN blocks before encryption.

When debugging “PIN verify fails intermittently,” verify whether your HSM or simulator expects Format 1 rules for randomness and whether your test harness fixes the RNG (for reproducible vectors).

## Format 2 — IBM 3624 legacy layout

**Format 2** is associated with legacy IBM host ecosystems. It is less common in modern open-loop card brand specs than Format 0, but it still appears in migrations, mainframe integrations, and certain regional stacks.

From a testing perspective, treat Format 2 as a **contract** with the host:

- Confirm the PIN block builder and the verifier agree on nibble order, length encoding, and padding rules.
- If your documentation simply says “3624,” be careful: PIN offset verification and PIN block formatting are related concepts but **not identical**.

## Format 3 — reserved

**Format 3** is reserved in ISO 9564. You may encounter vendor-specific documentation that repurposes terminology; in strict ISO terms, do not assume interoperability without a written specification from both ends.

If a gateway claims “Format 3,” ask for a byte-level reference implementation or official spec citation—your test vectors depend on it.

## Format 4 — AES PIN block (wider block)

As systems move to **AES** and larger block sizes, **Format 4** provides a standardized layout appropriate for 128-bit blocks (16 bytes). It is not just “Format 0 but longer”—field widths and encoding rules are defined for the AES context.

Practical implications:

- Keys and algorithms must match (AES vs triple-DES).
- Intermediate systems that assume an 8-byte PIN block may reject or truncate data unless explicitly upgraded.

## Comparison at a glance

| Format | Typical width | Padding style | Common usage notes |
|--------|----------------|---------------|-------------------|
| 0 | 64-bit | Fixed fill (e.g., `F`) | Widely used ISO interchange |
| 1 | 64-bit | Random fill nibbles | Reduced determinism in plaintext pattern |
| 2 | 64-bit | IBM 3624 legacy | Mainframe / legacy migrations |
| 3 | — | Reserved | Do not assume meaning without spec |
| 4 | 128-bit | Per ISO 9564 AES definition | Modern AES PIN encryption workflows |

## Practical testing workflow

1. **Freeze your vectors** — PIN, PAN (if required by your method), keys (test keys only), and expected intermediate values.
2. **Build the PIN block in plaintext** exactly as specified—nibble-by-nibble.
3. **Encrypt** under the correct key variant (TPK/ZPK/etc.) using the correct algorithm.
4. **Verify** independently: decrypt on the other side or use a trusted HSM simulator output.

## How ISO8583Studio helps

Instead of duct-taping spreadsheets to openssl commands, **ISO8583Studio** brings PIN tooling together with broader payment testing utilities—cryptography helpers, key-management oriented workflows, and message-oriented debugging—so you can iterate quickly on a laptop during integration sprints.

## Conclusion

Understanding ISO 9564 formats is the difference between “the PIN is correct” and “the PIN block is correct.” Master Format 0 for mainstream cases, recognize Format 1 randomness requirements, respect legacy Format 2 contracts, and plan explicitly for AES Format 4 when you modernize cryptography.

**Download ISO8583Studio** today from [iso8583.studio](https://iso8583.studio) and turn PIN block mysteries into repeatable, verifiable test cases—one clear hex dump at a time.
