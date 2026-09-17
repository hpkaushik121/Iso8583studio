# Iso8583Studio

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.1.6.

## Payments (Pro registration)

The Pro form takes payment through the payments service, browser-only —
"shape C" in the [integration guide][guide]. This site is prerendered static
files with nowhere to run request-time code, so the shapes that create a quote
server-side and receive a signed callback are not available to it.

Two rules follow from that and are not negotiable:

- **The page may never name an amount.** It names a `price_point` and the
  service prices it. Sending an amount from a browser credential is `403`.
- **Nothing may be provisioned automatically from it.** There is no signed
  callback to trust, so Pro workspaces are opened by hand off the back of a
  payment the service records. The guide sanctions exactly that — fulfilling
  by hand or by email is the browser-only shape's intended use.

### How a chosen amount still works

The form lets the customer pick what to pay, which looks like it needs the
page to name a price. It does not. The page names a **quantity** of a small
unit SKU and the catalog prices it: with `PAYMENTS_UNIT_RUPEES=1`, ₹250 is
simply quantity 250. The service computes and freezes the total server-side,
so the amount never exists anywhere the customer can edit — which is the same
protection a fixed price point gets.

The cost of this is one tenant setting: **`browser_max_quantity` must be at
least the largest amount you offer** (the guide defaults it to 10). The
checkout's own breakdown will read as `250 × ₹1` rather than one ₹250 line.

Configure through the environment; `tools/build-payments-config.mjs` bakes the
values into the bundle at build time, reading the repo-root `.env` locally and
the workflow's variables in CI.

| Variable | |
| --- | --- |
| `PAYMENTS_PUB_KEY` | the `pmk_pub_` browser credential. Public by construction — it ships in the page source. **Never a `pmk_live_` key**; the build refuses one. |
| `PAYMENTS_PRICE_POINTS` | comma-separated SKUs the page may sell; the first is the unit SKU |
| `PAYMENTS_UNIT_RUPEES` | rupees per unit of that SKU, default `1` |
| `PAYMENTS_BASE_URL` | defaults to `https://payments.iso8583.studio` |

Unset is a supported state: checkout stays off and the form says so rather than
shipping a button that fails.

**Checkout cannot be exercised from a dev server**, and this is by design rather
than a gap. Only the deployed origin is in the tenant's `cors_allowed_origins`,
so a browser on `localhost` has its preflight refused and blocks the call before
it is sent — which surfaces as "check your connection", because a blocked
preflight and a dead network are the same `TypeError` to JavaScript. The console
carries the real explanation. Redirect URLs rule it out a second time: they must
be absolute `https`, so `http://localhost` is `400 invalid_redirect` regardless.
Verify checkout against the deployed site.

Still needed before it can take a payment:

- [x] A unit SKU published in the tenant's `browser_checkout_price_points`, and
      set in `PAYMENTS_PRICE_POINTS` with its price in `PAYMENTS_UNIT_RUPEES`
      — `pp_pro_initial`, confirmed at 100 paise per unit
- [ ] `browser_max_quantity` raised to the largest amount offered — ₹100,000 at
      a ₹1 unit needs 100000. **Still at the default 10**, so ₹10 is currently
      the largest payment the form can take
- [ ] Decide whether the typed amount is inclusive of GST. The price point adds
      18% on top today, so ₹250 typed is ₹295 charged. A tax-inclusive price
      point would make the two agree; until then the form says GST is added
- [ ] `checkout:create` on the credential — not granted by default
- [ ] `https://iso8583.studio` in `cors_allowed_origins`
- [ ] `https://iso8583.studio/pro` in `allowed_redirect_origins`
- [ ] one live ₹1 payment, confirmed captured

[guide]: https://iso8583studio.atlassian.net/wiki/spaces/SD/pages/2260994/Payments+Service+Integration+Guide

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
