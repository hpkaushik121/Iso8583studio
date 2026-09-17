/* ISO8583Studio site components — single source of truth for header & footer.
   Usage: <site-header root="../../"></site-header> … <site-footer root="../../"></site-footer>
   root = relative path from the page to the site root. */
(function(){
  function headerHtml(r){ return `<header class="nav-bar">
    <div class="nav-in">
        <a class="brand" href="${r}index.html"><img src="${r}images/app.png" alt="ISO8583Studio logo">ISO8583Studio</a>
        <nav class="nav-links">
            <div>
                <button class="nav-a">Simulators <svg width="10" height="6" viewBox="0 0 10 6" fill="none"><path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg></button>
                <div class="menu mega">
                    <a href="${r}docs/host-simulator/index.html"><span class="mi">⇄</span><div><b>Host Simulator <span class="cnt">Available</span></b><span>Acquirer / issuer host, proxy</span></div></a>
                    <a href="${r}docs/hsm-simulator/index.html"><span class="mi">⚿</span><div><b>HSM Simulator <span class="cnt">Available</span></b><span>payShield 10K keys, PIN, MAC</span></div></a>
                    <a href="${r}docs/hsm-command-console/index.html"><span class="mi">›_</span><div><b>HSM Command Console <span class="cnt">Beta</span></b><span>Host-command client</span></div></a>
                    <a href="${r}docs/pos-simulator/index.html"><span class="mi">▤</span><div><b>POS Simulator <span class="cnt">Available</span></b><span>Terminal, EMV &amp; contactless</span></div></a>
                    <a href="${r}docs/apdu-simulator/index.html"><span class="mi">▣</span><div><b>APDU Simulator <span class="cnt">Available</span></b><span>Card session &amp; TLV</span></div></a>
                    <a href="${r}docs/payment-switch/index.html"><span class="mi">⇆</span><div><b>Switch Simulator <span class="cnt">Dev</span></b><span>Routing &amp; translation</span></div></a>
                    <a href="${r}docs/issuer-simulator/index.html"><span class="mi">◈</span><div><b>Issuer System <span class="cnt">Dev</span></b><span>Authorization decisioning</span></div></a>
                    <a href="${r}docs/atm-simulator/index.html"><span class="mi">▧</span><div><b>ATM Simulator <span class="cnt">Dev</span></b><span>Cash withdrawal, NDC/DDC</span></div></a>
                    <a href="${r}docs/ecr-simulator/index.html"><span class="mi">▦</span><div><b>ECR Simulator <span class="cnt">Dev</span></b><span>Register ↔ POS integration</span></div></a>
                </div>
            </div>
            <div>
                <button class="nav-a">Tools <svg width="10" height="6" viewBox="0 0 10 6" fill="none"><path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg></button>
                <div class="menu mega">
                    <a href="${r}docs/payment-simulators/index.html"><span class="mi">⇄</span><div><b>Payment Simulators <span class="cnt">9</span></b><span>Host, HSM, POS, ATM, switch &amp; scheme</span></div></a>
                    <a href="${r}docs/emv-tools/index.html"><span class="mi">▣</span><div><b>EMV &amp; Card Tools <span class="cnt">12</span></b><span>Cryptograms, SDA/DDA, ATR, tags, CVV</span></div></a>
                    <a href="${r}docs/cipher-tools/index.html"><span class="mi">⬡</span><div><b>Cryptographic Tools <span class="cnt">7</span></b><span>AES, DES/3DES, RSA, FPE, hashing</span></div></a>
                    <a href="${r}docs/key-tools/index.html"><span class="mi">⚿</span><div><b>Key Management <span class="cnt">10</span></b><span>DUKPT, TR-31, shares, Thales, Futurex</span></div></a>
                    <a href="${r}docs/pin-tools/index.html"><span class="mi">▤</span><div><b>Payment Utilities <span class="cnt">21</span></b><span>PIN blocks, PVV, MAC, parsing</span></div></a>
                    <a href="${r}docs/utility-tools/index.html"><span class="mi">⇋</span><div><b>Data Converters <span class="cnt">5</span></b><span>Base64, hex, EBCDIC, BCD</span></div></a>
                </div>
            </div>
            <div>
                <button class="nav-a">Solutions <svg width="10" height="6" viewBox="0 0 10 6" fill="none"><path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg></button>
                <div class="menu">
                    <a href="${r}emv-certification/index.html"><span class="mi">✓</span><div><b>EMV Certification</b><span>L1/L2/L3 &amp; scheme certification</span></div></a>
                    <a href="${r}cloud-simulators/index.html"><span class="mi">☁</span><div><b>Cloud Simulators</b><span>Hosted test endpoints for CI</span></div></a>
                    <a href="${r}middleware/index.html"><span class="mi">⇄</span><div><b>Payment Middleware</b><span>Switching, routing, translation</span></div></a>
                    <a href="${r}kernel/index.html"><span class="mi">▦</span><div><b>Kernel Development</b><span>EMV L2 kernel engineering</span></div></a>
                </div>
            </div>
            <div><a class="nav-a" href="${r}docs/index.html">Docs</a></div>
            <div><a class="nav-a" href="${r}blogs/index.html">Blog</a></div>
        </nav>
        <div class="nav-right">
            <a class="pro-pill" href="${r}pro/index.html" title="ISO8583Studio Pro">✦ Pro</a>
            <a class="btn btn-blue" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download</a>
            <button class="ham" id="hamBtn" aria-label="Menu">☰</button>
        </div>
    </div>
</header>
<div class="m-menu" id="mMenu">
    <div class="grp">Simulators</div>
    <a href="${r}docs/host-simulator/index.html">Host Simulator</a>
    <a href="${r}docs/hsm-simulator/index.html">HSM Simulator</a>
    <a href="${r}docs/hsm-command-console/index.html">HSM Command Console</a>
    <a href="${r}docs/pos-simulator/index.html">POS Simulator</a>
    <a href="${r}docs/apdu-simulator/index.html">APDU Simulator</a>
    <a href="${r}docs/payment-switch/index.html">Switch Simulator</a>
    <a href="${r}docs/issuer-simulator/index.html">Issuer System</a>
    <a href="${r}docs/atm-simulator/index.html">ATM Simulator</a>
    <a href="${r}docs/ecr-simulator/index.html">ECR Simulator</a>
    <div class="grp">Tools</div>
    <a href="${r}docs/emv-tools/index.html">EMV &amp; Card Tools</a>
    <a href="${r}docs/cipher-tools/index.html">Cryptographic Tools</a>
    <a href="${r}docs/key-tools/index.html">Key Management</a>
    <a href="${r}docs/pin-tools/index.html">Payment Utilities</a>
    <a href="${r}docs/utility-tools/index.html">Data Converters</a>
    <div class="grp">Solutions</div>
    <a href="${r}emv-certification/index.html">EMV Certification</a>
    <a href="${r}cloud-simulators/index.html">Cloud Simulators</a>
    <a href="${r}middleware/index.html">Payment Middleware</a>
    <a href="${r}kernel/index.html">Kernel Development</a>
    <div class="grp">Resources</div>
    <a href="${r}docs/index.html">Documentation</a>
    <a href="${r}docs/installation/index.html">Installation</a>
    <a href="${r}docs/versions/index.html">Versions</a>
    <a href="${r}docs/contributing/index.html">Contribute</a>
    <a href="${r}contact/index.html">Contact</a>
    <a href="${r}blogs/index.html">Blog</a>
    <a href="${r}pro/index.html">ISO8583Studio Pro</a>
    <a href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download Studio</a>
</div>`; }
  function footerHtml(r){ return `<footer>
    <div class="wrap">
        <div class="f-grid">
            <div class="f-brand">
                <a class="brand" href="${r}index.html"><img src="${r}images/app.png" alt="">ISO8583Studio</a>
                <p>Professional ISO 8583 payment transaction processing, simulation and testing. Built with Kotlin Multiplatform &amp; Compose Desktop.</p>
                <div class="f-social"><a href="https://github.com/users/hpkaushik121/projects/1" title="Roadmap"><svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6h-5.6z"/></svg></a><a href="https://www.linkedin.com/company/iso8583-studio" title="LinkedIn"><svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433a2.062 2.062 0 01-2.063-2.065 2.064 2.064 0 112.063 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/></svg></a><a href="https://github.com/hpkaushik121" title="GitHub"><svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/></svg></a><a href="https://medium.com/@iso8583.studio" title="Medium"><svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M13.54 12a6.8 6.8 0 01-6.77 6.82A6.8 6.8 0 010 12a6.8 6.8 0 016.77-6.82A6.8 6.8 0 0113.54 12zM20.96 12c0 3.54-1.51 6.42-3.38 6.42-1.87 0-3.39-2.88-3.39-6.42s1.52-6.42 3.39-6.42 3.38 2.88 3.38 6.42M24 12c0 3.17-.53 5.75-1.19 5.75-.66 0-1.19-2.58-1.19-5.75s.53-5.75 1.19-5.75S24 8.83 24 12z"/></svg></a></div>
            </div>
            <div class="f-col"><b>Simulators</b>
                <a href="${r}docs/host-simulator/index.html">Host Simulator</a>
                <a href="${r}docs/hsm-simulator/index.html">HSM Simulator</a>
                <a href="${r}docs/hsm-command-console/index.html">HSM Command Console</a>
                <a href="${r}docs/pos-simulator/index.html">POS Simulator</a>
                <a href="${r}docs/apdu-simulator/index.html">APDU Simulator</a>
                <a href="${r}docs/payment-switch/index.html">Switch Simulator</a>
                <a href="${r}docs/atm-simulator/index.html">ATM Simulator</a>
                <a href="${r}docs/ecr-simulator/index.html">ECR Simulator</a>
                <a href="${r}docs/issuer-simulator/index.html">Issuer System</a>
            </div>
            <div class="f-col"><b>Tools</b>
                <a href="${r}docs/emv-tools/index.html">EMV &amp; Card Tools</a>
                <a href="${r}docs/cipher-tools/index.html">Cryptographic Tools</a>
                <a href="${r}docs/key-tools/index.html">Key Management</a>
                <a href="${r}docs/pin-tools/index.html">Payment Utilities</a>
                <a href="${r}docs/utility-tools/index.html">Data Converters</a>
            </div>
            <div class="f-col"><b>Solutions</b>
                <a href="${r}emv-certification/index.html">EMV Certification</a>
                <a href="${r}cloud-simulators/index.html">Cloud Simulators</a>
                <a href="${r}middleware/index.html">Payment Middleware</a>
                <a href="${r}kernel/index.html">Kernel Development</a>
            </div>
            <div class="f-col"><b>Resources</b>
                <a href="${r}docs/index.html">Documentation</a>
    <a href="${r}docs/installation/index.html">Installation</a>
    <a href="${r}docs/versions/index.html">Versions</a>
    <a href="${r}docs/contributing/index.html">Contribute</a>
    <a href="${r}contact/index.html">Contact</a>
                <a href="${r}blogs/index.html">Blog</a>
                <a href="${r}pro/index.html">ISO8583Studio Pro</a>
            </div>
            <div class="f-col"><b>Legal</b>
                <a href="${r}privacy-policy.html">Privacy Policy</a>
                <a href="${r}terms-and-conditions.html">Terms &amp; Conditions</a>
            </div>
        </div>
        <div class="f-btm">
            <span>© 2026 AiCortex · ISO8583Studio</span>
            <span class="mono">Built with ❤ for the payments community</span>
        </div>
    </div>
</footer>`; }
  class SiteHeader extends HTMLElement{
    connectedCallback(){
      var r = this.getAttribute('root') || './';
      this.innerHTML = headerHtml(r);
      var b = this.querySelector('#hamBtn'), m = this.querySelector('#mMenu');
      if (b && m) b.addEventListener('click', function(){ m.classList.toggle('open'); });
    }
  }
  class SiteFooter extends HTMLElement{
    connectedCallback(){
      var r = this.getAttribute('root') || './';
      this.innerHTML = footerHtml(r);
    }
  }
  class SitePro extends HTMLElement{
    connectedCallback(){
      var r = this.getAttribute('root') || './';
      var t = this.getAttribute('label') || 'Testing with a team, or certifying with a scheme?';
      this.innerHTML = '<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>' + t +
        ' Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p>' +
        '<a href="' + r + 'pro/index.html">Register for Pro →</a></aside>';
    }
  }

  /* Download intercept: one-time Pro offer before any download */
  function proRoot(){
    var el = document.querySelector('site-header,site-footer,site-pro');
    return (el && el.getAttribute('root')) || './';
  }
  function buildModal(href){
    var r = proRoot();
    var ov = document.createElement('div');
    ov.className = 'pro-modal-ov';
    ov.innerHTML = '<div class="pro-modal" role="dialog" aria-modal="true" aria-label="Try ISO8583Studio Pro">' +
      '<button class="pm-x" aria-label="Close">×</button>' +
      '<span class="pn-tag">✦ Pro</span>' +
      '<h3>Before you download — try Pro</h3>' +
      '<p>The studio is free forever. Pro raises the ceiling — more cryptographic throughput, the full algorithm set, deeper simulator tuning, and hosted endpoints your CI can reach.</p>' +
      '<ul class="pm-list"><li>Higher CPS — multi-threaded crypto for load &amp; soak tests</li><li>Full algorithm set: RSA, ECC, SHA-3, FPE, AES DUKPT</li><li>Deep tweaks: field overrides, latency &amp; error injection</li><li>Hosted endpoints, scheme test packs, priority support</li></ul>' +
      '<div class="pm-actions"><a class="btn btn-blue" href="' + r + 'pro/index.html">Register for Pro — from ₹2</a>' +
      '<a class="pm-skip" href="' + href + '">Just download the free studio →</a></div></div>';
    function close(){ ov.remove(); document.body.style.overflow = ''; }
    ov.addEventListener('click', function(e){ if (e.target === ov) close(); });
    ov.querySelector('.pm-x').addEventListener('click', close);
    document.addEventListener('keydown', function esc(e){ if (e.key === 'Escape'){ close(); document.removeEventListener('keydown', esc); } });
    document.body.appendChild(ov);
    document.body.style.overflow = 'hidden';
  }
  function isDownload(a){
    if (!a) return false;
    var h = a.getAttribute('href') || '';
    if (/releases\/latest|releases\/download|\.(dmg|exe|msi|deb|rpm|jar|zip)(\?|$)/i.test(h)) return true;
    return /^\s*(⬇\s*)?download\b/i.test(a.textContent || '') && /github\.com\/hpkaushik121/i.test(h);
  }
  document.addEventListener('click', function(e){
    var a = e.target.closest && e.target.closest('a');
    if (!isDownload(a)) return;
    e.preventDefault();
    buildModal(a.getAttribute('href'));
  }, true);


  /* Cookie consent — injected once per page, choice stored in localStorage */
  function cookieBanner(){
    var KEY = 'iso8583-cookie-consent';
    try { if (localStorage.getItem(KEY)) return; } catch(e){}
    var r = proRoot();
    var el = document.createElement('div');
    el.className = 'cookie-bar';
    el.setAttribute('role','region');
    el.setAttribute('aria-label','Cookie consent');
    el.innerHTML = '<p>We use cookies to remember your preferences and to measure how the site is used. ' +
      'See our <a href="' + r + 'privacy-policy.html">Privacy Policy</a>.</p>' +
      '<div class="cb-actions"><button class="cb-decline" type="button">Essential only</button>' +
      '<button class="btn btn-blue cb-accept" type="button">Accept all</button></div>';
    function done(v){ try { localStorage.setItem(KEY, v); } catch(e){} el.classList.add('out'); setTimeout(function(){ el.remove(); }, 220); }
    el.querySelector('.cb-accept').addEventListener('click', function(){ done('all'); });
    el.querySelector('.cb-decline').addEventListener('click', function(){ done('essential'); });
    document.body.appendChild(el);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', cookieBanner);
  else cookieBanner();

  if (!customElements.get('site-pro')) customElements.define('site-pro', SitePro);
  if (!customElements.get('site-header')) customElements.define('site-header', SiteHeader);
  if (!customElements.get('site-footer')) customElements.define('site-footer', SiteFooter);
})();
