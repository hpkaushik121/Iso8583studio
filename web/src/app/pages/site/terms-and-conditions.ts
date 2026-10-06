import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { UiIcon } from '../../ui';
import { LONGFORM, LfBadge, LfHighlight, LfTocEntry } from '../shared/longform';

/**
 * The wording of this page is a legal document: the long-form shell supplies
 * the layout only, and every sentence is carried over from the terms as
 * published. Change the text only on instruction.
 */
@Component({
  selector: 'page-terms-and-conditions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [...LONGFORM, RouterLink, UiIcon],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-terms-and-conditions' },
  template: `
    <lf-header [crumbs]="crumbs" [badge]="badge" title="Terms and Conditions" [meta]="meta" [highlights]="highlights"
               lede="Open source software agreement for the ISO8583Studio desktop application. These terms govern the use of our financial transaction processing software." />

    <lf-body [toc]="toc">
      <section lfSection id="agreement" n="01" heading="Open source software agreement">
        <p class="lf-p">This Open Source Software Agreement ("Agreement") governs your use of ISO8583Studio desktop application software ("Software"), which is free and open source software provided by AiCortext Solutions Pvt. Ltd. ("we," "us," or "our") under the GNU Affero General Public License, version 3 (AGPL v3).</p>
        <lf-note tone="blue" icon="seal-check" title="Acceptance" [legal]="true">By downloading, installing, or using ISO8583Studio, you acknowledge that this is open source software provided "AS IS" without any warranty or obligation from the developers, distributed under the GNU Affero General Public License v3.</lf-note>
        <lf-note tone="neutral" icon="info" title="Important">This is free, open source software distributed under the AGPL v3. There are no purchase requirements, no refunds (as the software is free), and no warranties or obligations from the developers regarding its performance or suitability for any purpose. The AGPL is a strong copyleft licence: if you distribute a modified version, or let others interact with a modified version over a network, you must offer them its complete corresponding source under the same licence.</lf-note>
      </section>

      <section lfSection id="license" n="02" heading="GNU AGPL v3 grant">
        <h3 class="lf-h3">Open source license</h3>
        <p class="lf-p">ISO8583Studio is distributed under the GNU Affero General Public License, version 3, which grants you the following rights:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Use:</strong> run the Software for any purpose, including commercial purposes</li>
          <li><strong>Reproduce:</strong> make unlimited copies of the Software</li>
          <li><strong>Modify:</strong> create modified versions and derivative works based on the Software</li>
          <li><strong>Distribute:</strong> distribute original or modified versions, under the same licence</li>
          <li><strong>Private use:</strong> run and modify the Software privately, without publishing your changes, for as long as you do not distribute it or offer it to others over a network</li>
          <li><strong>Commercial use:</strong> use the Software in commercial environments and profit-generating activities, including charging for copies or support</li>
          <li><strong>Patent grant:</strong> receive patent rights from contributors for their contributions</li>
          <li><strong>Source access:</strong> receive the complete corresponding source code of any version you are given</li>
        </ul>
        <h3 class="lf-h3">AGPL v3 key features</h3>
        <lf-note tone="teal" icon="shield-check" title="AGPL v3 characteristics">
          <ul class="lf-ul lf-ul--def">
            <li><strong>Strong copyleft:</strong> derivative works must be released under the AGPL v3</li>
            <li><strong>Network use is distribution:</strong> section 13 closes the "software as a service" gap — users interacting with a modified version over a network must be offered its source</li>
            <li><strong>Patent protection:</strong> includes an express patent grant from contributors</li>
            <li><strong>Trademark protection:</strong> does not grant rights to use names, trademarks, or logos</li>
            <li><strong>Contribution licensing:</strong> contributions to the project are licensed under the same terms</li>
          </ul>
        </lf-note>
        <lf-note tone="warn" icon="share-network" title="Network use clause (section 13)" [legal]="true">If you modify ISO8583Studio and let users interact with it remotely over a network — for example as a hosted service or an internal web-facing tool — you must prominently offer those users the complete corresponding source code of your modified version, free of charge, under the AGPL v3.</lf-note>
        <h3 class="lf-h3">License requirements</h3>
        <p class="lf-p">When redistributing the Software, or conveying a modified version, you must:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Preserve notices:</strong> keep all copyright, licence, and attribution notices intact</li>
          <li><strong>Include license:</strong> provide a copy of the GNU AGPL v3 with the Software</li>
          <li><strong>State changes:</strong> carry prominent notices stating that you modified the Software, and the date of the change</li>
          <li><strong>Release source:</strong> make the complete corresponding source of your version available under the AGPL v3, including to network users</li>
          <li><strong>Same licence:</strong> license the whole of any derivative work under the AGPL v3 — you may not relicense it under more permissive or proprietary terms</li>
          <li><strong>No further restrictions:</strong> do not impose additional legal or technical restrictions on the rights the licence grants</li>
          <li><strong>No trademark use:</strong> do not use the names, trademarks, or logos without permission</li>
        </ul>
      </section>

      <section lfSection id="usage" n="03" heading="Permitted usage">
        <p class="lf-p">As open source software under the AGPL v3, you may use ISO8583Studio for any purpose, including:</p>
        <h3 class="lf-h3">Financial transaction processing</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>ISO 8583 message processing:</strong> create, modify, parse, and analyze ISO 8583 financial messages</li>
          <li><strong>Gateway configuration:</strong> set up and manage payment gateways, switches, and transaction processors</li>
          <li><strong>Protocol support:</strong> use TCP/IP, RS232, dial-up, and REST/SOAP protocols for financial communications</li>
          <li><strong>Host simulation:</strong> simulate host systems for testing and development purposes</li>
          <li><strong>Transaction testing:</strong> test transaction flows, message formats, and system integrations</li>
        </ul>
        <h3 class="lf-h3">Development and modification</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Source code access:</strong> access, read, and study the complete source code</li>
          <li><strong>Modification rights:</strong> modify the software to meet your specific requirements</li>
          <li><strong>Custom features:</strong> add new features and functionality as needed</li>
          <li><strong>Integration development:</strong> create custom integrations with other systems</li>
          <li><strong>Bug fixes:</strong> fix bugs and improve software stability</li>
          <li><strong>Derivative works:</strong> create derivative works based on the original Software</li>
        </ul>
        <h3 class="lf-h3">Distribution and commercial use</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Commercial operations:</strong> use in production environments for profit-generating activities</li>
          <li><strong>Redistribution:</strong> distribute original or modified versions, with proper notices and the complete corresponding source, under the AGPL v3</li>
          <li><strong>Incorporation:</strong> incorporate into other software or services, provided the combined work is released under the AGPL v3</li>
          <li><strong>Selling:</strong> sell copies, support, or software based on ISO8583Studio, as long as recipients get the source and the same licence</li>
          <li><strong>Hosted services:</strong> offer a modified version over a network, provided network users are offered its complete corresponding source</li>
        </ul>
        <lf-note tone="warn" icon="copyleft" title="Copyleft obligation">Internal use and unmodified use are unrestricted. But you may not take ISO8583Studio proprietary: a modified version that you distribute, embed in a product, or expose to users over a network must itself be released under the AGPL v3, with source. If those terms do not fit your use case, contact us at <a href="mailto:sk@iso8583.studio">sk@iso8583.studio</a> about alternative licensing.</lf-note>
      </section>

      <section lfSection id="limitations" n="04" heading="No warranties or guarantees">
        <h3 class="lf-h3">AGPL v3 disclaimer (section 15)</h3>
        <lf-note tone="warn" icon="warning" title="Disclaimer of warranty" [legal]="true">THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.</lf-note>
        <p class="lf-p">As open source software distributed under the AGPL v3, ISO8583Studio comes with absolutely no warranties:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>No warranty of functionality:</strong> no guarantee that the software will work for your specific needs</li>
          <li><strong>No warranty of quality:</strong> no assurance of software quality, reliability, or performance</li>
          <li><strong>No warranty of accuracy:</strong> no guarantee that calculations or processes will be accurate</li>
          <li><strong>No warranty of security:</strong> no guarantee that the software is secure or free from vulnerabilities</li>
          <li><strong>No warranty of compatibility:</strong> no guarantee of compatibility with your systems or environment</li>
          <li><strong>No warranty of non-infringement:</strong> no guarantee that use won't infringe third-party rights</li>
          <li><strong>No warranty of title:</strong> no warranty regarding ownership or title to the software</li>
        </ul>
        <h3 class="lf-h3">Financial transaction processing</h3>
        <lf-note tone="warn" icon="siren" title="Critical warning">This software processes financial transactions. You are solely responsible for testing, validation, and ensuring the software meets your regulatory and business requirements. No warranties are provided regarding financial accuracy or compliance.</lf-note>
        <ul class="lf-ul lf-ul--def">
          <li><strong>No transaction guarantees:</strong> no warranty that financial transactions will process correctly</li>
          <li><strong>No regulatory compliance:</strong> no guarantee of compliance with financial regulations</li>
          <li><strong>No data accuracy:</strong> no warranty regarding accuracy of financial calculations or data</li>
          <li><strong>No security assurance:</strong> no guarantee of security for sensitive financial data</li>
          <li><strong>No audit compliance:</strong> no warranty regarding audit trail accuracy or completeness</li>
        </ul>
        <h3 class="lf-h3">Complete disclaimer of liability</h3>
        <lf-note tone="warn" icon="scales" title="Limitation of liability (section 16)" [legal]="true">IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.</lf-note>
        <p class="lf-p">The developers and contributors of ISO8583Studio have no liability whatsoever for:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Financial losses:</strong> any financial losses, lost profits, or business damages</li>
          <li><strong>Data loss:</strong> loss of data, corruption, or unauthorized access to information</li>
          <li><strong>System failures:</strong> system downtime, crashes, or integration failures</li>
          <li><strong>Transaction errors:</strong> errors in financial transaction processing or calculations</li>
          <li><strong>Security breaches:</strong> security vulnerabilities or data breaches</li>
          <li><strong>Regulatory issues:</strong> regulatory violations or compliance failures</li>
        </ul>
        <lf-note tone="warn" icon="hand-palm" title="At your own risk" [legal]="true">You use this software entirely at your own risk. You are responsible for evaluating the software's suitability for your intended use and for all consequences of its use.</lf-note>
      </section>

      <section lfSection id="support" n="05" heading="Community support">
        <h3 class="lf-h3">Open source support model</h3>
        <p class="lf-p">As open source software, support is primarily community-driven:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Community forums:</strong> user community provides peer-to-peer support</li>
          <li><strong>Documentation:</strong> comprehensive documentation maintained by the community</li>
          <li><strong>Issue tracking:</strong> public issue tracking on the project repository</li>
          <li><strong>Wiki and knowledge base:</strong> community-maintained documentation and guides</li>
        </ul>
        <h3 class="lf-h3">No guaranteed support</h3>
        <lf-note tone="neutral" icon="info" title="Important">There is no guaranteed support, service level agreements, or response times. Support is provided by volunteers on a best-effort basis.</lf-note>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Best effort:</strong> community members help when available and willing</li>
          <li><strong>No SLA:</strong> no guaranteed response times or resolution schedules</li>
          <li><strong>Volunteer basis:</strong> all support is provided by volunteers in their spare time</li>
          <li><strong>Self-service:</strong> users are encouraged to read documentation and search existing issues first</li>
        </ul>
        <h3 class="lf-h3">How to get help</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Read documentation:</strong> check the <a href="/docs">documentation</a> first</li>
          <li><strong>Search issues:</strong> look through existing GitHub issues for solutions</li>
          <li><strong>Community forums:</strong> ask questions in community discussion forums</li>
          <li><strong>Report bugs:</strong> submit bug reports through the project's issue tracker</li>
          <li><strong>Contribute:</strong> contribute fixes and improvements back to the project</li>
        </ul>
      </section>

      <section lfSection id="contact" n="06" heading="Community and contact" [hold]="false">
        <p class="lf-p ds-hold" [style.--d]="260">ISO8583Studio is open source software. Support comes from the community, and contributions are welcome from everyone.</p>
        <div class="lf-cards">
          <lf-link-card class="ds-item" [style.--d]="320" icon="chats-circle" eyebrow="Discussions" title="Q&amp;A"
                        body="Community questions and discussions."
                        cta="GitHub Discussions" href="https://github.com/hpkaushik121/Iso8583studio/discussions" />
          <lf-link-card class="ds-item" [style.--d]="460" icon="bug" eyebrow="Bug reports" title="Issues"
                        body="Report bugs and request features."
                        cta="GitHub Issues" href="https://github.com/hpkaushik121/Iso8583studio/issues" />
          <lf-link-card class="ds-item" [style.--d]="600" icon="github-logo" eyebrow="Source code" title="AGPL v3"
                        body="Read the source, and contribute back to the project."
                        cta="Open repository" href="https://github.com/hpkaushik121/Iso8583studio" />
          <lf-link-card class="ds-item" [style.--d]="740" icon="briefcase" eyebrow="Professional services" title="Optional"
                        body="Paid consulting, custom development, or enterprise support."
                        cta="sk@iso8583.studio" href="mailto:sk@iso8583.studio" />
        </div>
        <div class="ds-hold" [style.--d]="380">
          <lf-note tone="neutral" icon="users-three" title="Remember">This is open source software. Support is provided by the community on a volunteer basis with no guarantees or service level agreements.</lf-note>
          <h3 class="lf-h3">AGPL v3 summary</h3>
          <ul class="lf-ul lf-ul--def">
            <li><strong>Permissions:</strong> commercial use, modification, distribution, patent use, private use</li>
            <li><strong>Conditions:</strong> disclose source, license and copyright notice, same licence, state changes, network use is distribution</li>
            <li><strong>Limitations:</strong> liability, trademark use, warranty</li>
          </ul>
          <p class="lf-p">This is not legal advice. See the full <a href="https://www.gnu.org/licenses/agpl-3.0.html#license-text">GNU AGPL v3 text</a>, also included as the <a href="https://github.com/hpkaushik121/Iso8583studio/blob/main/LICENSE">LICENSE file</a> in the repository, for complete terms.</p>
          <div class="lf-foot">
            <span class="lf-foot-line">Terms version 2.0 (Open Source) · Effective date 9 June 2025 · Last revised 20 September 2026</span>
            <a class="lf-foot-link" routerLink="/privacy-policy">Privacy Policy <ui-icon name="arrow-right" [size]="13" /></a>
          </div>
        </div>
      </section>
    </lf-body>
  `,
})
export class TermsAndConditionsPage {
  protected readonly crumbs = [{ label: 'Home', link: '/' }, { label: 'Terms and Conditions' }];
  protected readonly badge: LfBadge = { tone: 'teal', label: 'Legal · AGPL v3' };
  protected readonly meta = [
    ['Version', '2.0 (Open Source)'], ['Effective', '9 June 2025'],
    ['Last revised', '20 September 2026'], ['License', 'AGPL v3'],
  ] as const;
  protected readonly highlights: LfHighlight[] = [
    { icon: 'gift', tone: 'teal', title: 'Completely free', body: 'No payment required, no license fees, no subscriptions — forever free.' },
    { icon: 'warning', tone: 'warn', title: 'No warranties', body: 'Open source software provided "as is" with no guarantees or support obligations.' },
    { icon: 'arrows-clockwise', tone: 'blue', title: 'Share alike', body: 'Strong copyleft — modified versions, including ones offered over a network, must be released under the AGPL v3 too.' },
  ];
  protected readonly toc: LfTocEntry[] = [
    { id: 'agreement', n: '01', label: 'Open source software agreement' },
    { id: 'license', n: '02', label: 'GNU AGPL v3 grant' },
    { id: 'usage', n: '03', label: 'Permitted usage' },
    { id: 'limitations', n: '04', label: 'No warranties or guarantees' },
    { id: 'support', n: '05', label: 'Community support' },
    { id: 'contact', n: '06', label: 'Community and contact' },
  ];
}
