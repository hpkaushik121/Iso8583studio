---
title: "ISO 9564 PIN Block Formats 0–4: What They Are and How They Differ"
description: "Understand ISO 9564 PIN block formats 0–4: layout, padding, and when each format is used in payment systems and HSM workflows."
date: "2025-07-15"
updated: "2026-10-08"
tags: [PIN, ISO 9564, PIN block, payment security, HSM]
category: "PIN Security"
author: "Sourabh Kaushik"
read_time: "4 min read"
---

If you have ever stared at a 16-byte hex string labeled “PIN block” and wondered whether you are looking at Format 0, Format 1, or something your gateway invented last Tuesday, you are not alone. PIN blocks are one of the most frequently misunderstood artifacts in card payments—yet they sit at the center of online PIN verification, ATM PIN change, and host-to-HSM cryptography.

This article explains **ISO 9564** PIN block formats **0 through 4** at a practical level: what each format carries, how padding works, and why the “same PIN” can look completely different depending on format and keying. For hands-on generation and inspection, **ISO8583Studio** ([iso8583.studio](https://iso8583.studio)) is a free cross-platform desktop app (Windows, macOS, Linux) with 64 payment tools, including PIN utilities that help you build and validate PIN blocks without chaining together fragile scripts.

## Why PIN blocks exist

A Personal Identification Number is rarely transmitted as digits alone. Instead, systems wrap the PIN into a fixed-width block (commonly 64 bits / 8 bytes, sometimes 128 bits for AES variants) and encrypt that block under a PIN encryption key—typically a **TPK** (terminal PIN key) or **ZPK** (zone PIN key) in classic terminology—before it crosses untrusted networks.

The **format** defines how PIN length, PIN digits, and fill digits are arranged *before* encryption. If you encrypt the wrong layout, verification will fail even when the PIN and keys are correct.

## Format 0 (ANSI) — the common ISO case

**ISO 9564-1 Format 0** is widely used in international interchange. Conceptually:

- The first nibble is the **control field** and is `0` for Format 0.
- The second nibble encodes the **PIN length** (4–12 digits).
- Following nibbles carry the PIN digits in **BCD** (Binary Coded Decimal), each digit 0–9 occupying one nibble.
- Remaining nibbles are filled with `F` to 16 nibbles.
- That 8-byte block is then **XORed with the PAN block**: `0000` followed by the rightmost 12 PAN digits excluding the check digit. This is what ties the PIN block to the card and what separates Format 0 from Format 1.

For an 8-byte DES-style block, you get a well-defined pattern: length + PIN digits + padding. The critical testing mistake is treating ASCII ‘0’–‘9’ bytes as BCD—**they are not interchangeable**.

### Quick sanity checks for testers

- Confirm PIN length matches the digit count you typed.
- Confirm there are no illegal nibbles (e.g., `A`–`E`) in the PIN digit region unless your implementation explicitly allows non-decimal experiments (most production systems do not).

## Format 1 (supplementary) — random padding

**Format 1** also encodes PIN length and PIN digits, but introduces **random padding nibbles** in the fill area (rather than a fixed filler like `F`). The goal is to reduce determinism: two encryptions of the same PIN should not necessarily yield identical plaintext PIN blocks before encryption.

When debugging “PIN verify fails intermittently,” verify whether your HSM or simulator expects Format 1 rules for randomness and whether your test harness fixes the RNG (for reproducible vectors).

## Format 2 — the offline (ICC) PIN block

**Format 2** is the layout used to send a PIN to the chip card itself for offline verification — the EMV `VERIFY` command. Control nibble `2`, then the PIN length, then the PIN digits, then `F` fill to 16 nibbles; **no PAN XOR**, because the card already knows its own PAN. It is not a host-to-host format and must not be sent to an issuer or HSM expecting Format 0.

From a testing perspective:

- A Format 2 block is plaintext between the terminal kernel and the card (or enciphered with the card's RSA key for enciphered offline PIN); it never travels in ISO 8583 Field 52.
- Do not confuse it with **IBM 3624**, which is a PIN *verification* method (PIN offset) and not a PIN block format at all.

## Format 3 — Format 0 with random fill

**Format 3** is a defined ISO 9564-1 format and is widely deployed. It is built like Format 0 — control nibble `3`, PIN length, PIN digits, then XOR with the same PAN block — but the fill nibbles are **random values from `A` to `F`** instead of a constant `F`. That removes the fixed plaintext pattern Format 0 exposes while keeping the PAN binding Format 1 lacks.

For testing: a Format 3 block for the same PIN and PAN is different on every build, so your harness must either fix the random source or verify by decrypting rather than by comparing ciphertext.

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
| 2 | 64-bit | `F` fill, no PAN XOR | Offline PIN to the chip (EMV `VERIFY`) |
| 3 | 64-bit | Random `A`–`F` fill, PAN XOR | Online interchange, like Format 0 without the fixed pattern |
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
