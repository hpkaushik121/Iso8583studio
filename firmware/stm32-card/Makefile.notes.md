# Build / flash / monitor cheatsheet

PlatformIO drives everything; no Makefile is needed. From this directory
(`firmware/stm32-card/`):

```
pio run                                # build
pio run --target upload                # flash via ST-Link
pio device monitor -b 115200           # USB-CDC console
```

Useful extras:

```
pio run --target clean                 # wipe build artefacts
pio run -v                             # verbose compile (for diagnosing flag issues)
pio device list                        # confirm the Nucleo's CDC port enumerated
```

The first build pulls down the `ststm32` platform, the STM32Cube L4 HAL, and
the USB device middleware — expect a few minutes on a cold cache. Subsequent
builds are incremental.

If `pio run --target upload` fails to find the ST-Link, check that the
on-board ST-Link's `NUCLEO` mass-storage device is mounted (cold-plug fix on
macOS) or run `pio run -t upload --upload-port /dev/cu.usbmodem*` to pin it.
