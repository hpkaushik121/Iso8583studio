/**
 * Old /docs/* URL -> its new home. Simulator pages live under /simulator/*
 * (suffix dropped: /docs/hsm-simulator -> /simulator/hsm) and tool pages under
 * /tools/*. postbuild.mjs writes a meta-refresh stub at each old route;
 * check-links.mjs verifies every stub points where this map says.
 */
export const MOVED_ROUTES = {
  '/docs/host-simulator': '/simulator/host',
  '/docs/hsm-simulator': '/simulator/hsm',
  '/docs/hsm-command-console': '/simulator/hsm-command-console',
  '/docs/pos-simulator': '/simulator/pos',
  '/docs/apdu-simulator': '/simulator/apdu',
  '/docs/payment-switch': '/simulator/payment-switch',
  '/docs/issuer-simulator': '/simulator/issuer',
  '/docs/atm-simulator': '/simulator/atm',
  '/docs/ecr-simulator': '/simulator/ecr',
  '/docs/payment-simulators': '/simulator',
  '/docs/emv-tools': '/tools/emv-tools',
  '/docs/cipher-tools': '/tools/cipher-tools',
  '/docs/key-tools': '/tools/key-tools',
  '/docs/pin-tools': '/tools/pin-tools',
  '/docs/utility-tools': '/tools/utility-tools',
  '/docs/card-validation': '/tools/card-validation',
  '/docs/dukpt-tools': '/tools/dukpt-tools',
  '/docs/mac-tools': '/tools/mac-tools',
};
