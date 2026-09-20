import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-terms-and-conditions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-terms-and-conditions' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <span>Terms and Conditions</span>
        </div>

        <h1 class="page-title">Terms and Conditions</h1>
        <p class="page-description">Open source software agreement for the ISO8583Studio desktop application. These terms govern the use of our financial transaction processing software.</p>

        <div class="legal-meta">
            <span><b>Version</b> 2.0 (Open Source)</span>
            <span><b>Effective</b> 9 June 2025</span>
            <span><b>Last revised</b> 20 September 2026</span>
            <span><b>License</b> AGPL v3</span>
        </div>

        <div class="feature-grid">
            <div class="feature-item">
                <div class="feature-item-icon">🆓</div>
                <h4>Completely free</h4>
                <p>No payment required, no license fees, no subscriptions — forever free.</p>
            </div>
            <div class="feature-item">
                <div class="feature-item-icon">⚠</div>
                <h4>No warranties</h4>
                <p>Open source software provided "as is" with no guarantees or support obligations.</p>
            </div>
            <div class="feature-item">
                <div class="feature-item-icon">🔄</div>
                <h4>Share alike</h4>
                <p>Strong copyleft — modified versions, including ones offered over a network, must be released under the AGPL v3 too.</p>
            </div>
        </div>

        <nav class="legal-toc" aria-label="Table of contents">
            <div class="info-card-title">Table of contents</div>
            <ol>
                <li><a href="/terms-and-conditions#agreement">Open source software agreement</a></li>
                <li><a href="/terms-and-conditions#license">GNU AGPL v3 grant</a></li>
                <li><a href="/terms-and-conditions#usage">Permitted usage</a></li>
                <li><a href="/terms-and-conditions#limitations">No warranties or guarantees</a></li>
                <li><a href="/terms-and-conditions#support">Community support</a></li>
                <li><a href="/terms-and-conditions#contact">Community and contact</a></li>
            </ol>
        </nav>

        <ui-section anchor="agreement" heading="Open source software agreement">
            <p>This Open Source Software Agreement ("Agreement") governs your use of ISO8583Studio desktop application software ("Software"), which is free and open source software provided by AiCortext Solutions Pvt. Ltd. ("we," "us," or "our") under the GNU Affero General Public License, version 3 (AGPL v3).</p>

            <div class="info-card tip">
                <div class="info-card-title">Acceptance</div>
                <p><strong>By downloading, installing, or using ISO8583Studio, you acknowledge that this is open source software provided "AS IS" without any warranty or obligation from the developers, distributed under the GNU Affero General Public License v3.</strong></p>
            </div>

            <div class="info-card warning">
                <div class="info-card-title">Important</div>
                <p>This is free, open source software distributed under the AGPL v3. There are no purchase requirements, no refunds (as the software is free), and no warranties or obligations from the developers regarding its performance or suitability for any purpose. The AGPL is a strong copyleft licence: if you distribute a modified version, or let others interact with a modified version over a network, you must offer them its complete corresponding source under the same licence.</p>
            </div>
        </ui-section>

        <ui-section anchor="license" heading="GNU AGPL v3 grant">

            <h3>Open source license</h3>
            <p>ISO8583Studio is distributed under the GNU Affero General Public License, version 3, which grants you the following rights:</p>
            <ul>
                <li><strong>Use:</strong> run the Software for any purpose, including commercial purposes</li>
                <li><strong>Reproduce:</strong> make unlimited copies of the Software</li>
                <li><strong>Modify:</strong> create modified versions and derivative works based on the Software</li>
                <li><strong>Distribute:</strong> distribute original or modified versions, under the same licence</li>
                <li><strong>Private use:</strong> run and modify the Software privately, without publishing your changes, for as long as you do not distribute it or offer it to others over a network</li>
                <li><strong>Commercial use:</strong> use the Software in commercial environments and profit-generating activities, including charging for copies or support</li>
                <li><strong>Patent grant:</strong> receive patent rights from contributors for their contributions</li>
                <li><strong>Source access:</strong> receive the complete corresponding source code of any version you are given</li>
            </ul>

            <h3>AGPL v3 key features</h3>
            <div class="info-card note">
                <div class="info-card-title">AGPL v3 characteristics</div>
                <ul>
                    <li><strong>Strong copyleft:</strong> derivative works must be released under the AGPL v3</li>
                    <li><strong>Network use is distribution:</strong> section 13 closes the "software as a service" gap — users interacting with a modified version over a network must be offered its source</li>
                    <li><strong>Patent protection:</strong> includes an express patent grant from contributors</li>
                    <li><strong>Trademark protection:</strong> does not grant rights to use names, trademarks, or logos</li>
                    <li><strong>Contribution licensing:</strong> contributions to the project are licensed under the same terms</li>
                </ul>
            </div>

            <div class="info-card warning">
                <div class="info-card-title">Network use clause (section 13)</div>
                <p><strong>If you modify ISO8583Studio and let users interact with it remotely over a network — for example as a hosted service or an internal web-facing tool — you must prominently offer those users the complete corresponding source code of your modified version, free of charge, under the AGPL v3.</strong></p>
            </div>

            <h3>License requirements</h3>
            <p>When redistributing the Software, or conveying a modified version, you must:</p>
            <ul>
                <li><strong>Preserve notices:</strong> keep all copyright, licence, and attribution notices intact</li>
                <li><strong>Include license:</strong> provide a copy of the GNU AGPL v3 with the Software</li>
                <li><strong>State changes:</strong> carry prominent notices stating that you modified the Software, and the date of the change</li>
                <li><strong>Release source:</strong> make the complete corresponding source of your version available under the AGPL v3, including to network users</li>
                <li><strong>Same licence:</strong> license the whole of any derivative work under the AGPL v3 — you may not relicense it under more permissive or proprietary terms</li>
                <li><strong>No further restrictions:</strong> do not impose additional legal or technical restrictions on the rights the licence grants</li>
                <li><strong>No trademark use:</strong> do not use the names, trademarks, or logos without permission</li>
            </ul>
        </ui-section>

        <ui-section anchor="usage" heading="Permitted usage">
            <p>As open source software under the AGPL v3, you may use ISO8583Studio for any purpose, including:</p>

            <h3>Financial transaction processing</h3>
            <ul>
                <li><strong>ISO 8583 message processing:</strong> create, modify, parse, and analyze ISO 8583 financial messages</li>
                <li><strong>Gateway configuration:</strong> set up and manage payment gateways, switches, and transaction processors</li>
                <li><strong>Protocol support:</strong> use TCP/IP, RS232, dial-up, and REST/SOAP protocols for financial communications</li>
                <li><strong>Host simulation:</strong> simulate host systems for testing and development purposes</li>
                <li><strong>Transaction testing:</strong> test transaction flows, message formats, and system integrations</li>
            </ul>

            <h3>Development and modification</h3>
            <ul>
                <li><strong>Source code access:</strong> access, read, and study the complete source code</li>
                <li><strong>Modification rights:</strong> modify the software to meet your specific requirements</li>
                <li><strong>Custom features:</strong> add new features and functionality as needed</li>
                <li><strong>Integration development:</strong> create custom integrations with other systems</li>
                <li><strong>Bug fixes:</strong> fix bugs and improve software stability</li>
                <li><strong>Derivative works:</strong> create derivative works based on the original Software</li>
            </ul>

            <h3>Distribution and commercial use</h3>
            <ul>
                <li><strong>Commercial operations:</strong> use in production environments for profit-generating activities</li>
                <li><strong>Redistribution:</strong> distribute original or modified versions, with proper notices and the complete corresponding source, under the AGPL v3</li>
                <li><strong>Incorporation:</strong> incorporate into other software or services, provided the combined work is released under the AGPL v3</li>
                <li><strong>Selling:</strong> sell copies, support, or software based on ISO8583Studio, as long as recipients get the source and the same licence</li>
                <li><strong>Hosted services:</strong> offer a modified version over a network, provided network users are offered its complete corresponding source</li>
            </ul>

            <div class="info-card warning">
                <div class="info-card-title">Copyleft obligation</div>
                <p>Internal use and unmodified use are unrestricted. But you may not take ISO8583Studio proprietary: a modified version that you distribute, embed in a product, or expose to users over a network must itself be released under the AGPL v3, with source. If those terms do not fit your use case, contact us at <a href="mailto:sk@iso8583.studio">sk@iso8583.studio</a> about alternative licensing.</p>
            </div>
        </ui-section>

        <ui-section anchor="limitations" heading="No warranties or guarantees">

            <h3>AGPL v3 disclaimer (section 15)</h3>
            <div class="info-card danger">
                <div class="info-card-title">Disclaimer of warranty</div>
                <p><strong>THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.</strong></p>
            </div>

            <p>As open source software distributed under the AGPL v3, ISO8583Studio comes with absolutely no warranties:</p>
            <ul>
                <li><strong>No warranty of functionality:</strong> no guarantee that the software will work for your specific needs</li>
                <li><strong>No warranty of quality:</strong> no assurance of software quality, reliability, or performance</li>
                <li><strong>No warranty of accuracy:</strong> no guarantee that calculations or processes will be accurate</li>
                <li><strong>No warranty of security:</strong> no guarantee that the software is secure or free from vulnerabilities</li>
                <li><strong>No warranty of compatibility:</strong> no guarantee of compatibility with your systems or environment</li>
                <li><strong>No warranty of non-infringement:</strong> no guarantee that use won't infringe third-party rights</li>
                <li><strong>No warranty of title:</strong> no warranty regarding ownership or title to the software</li>
            </ul>

            <h3>Financial transaction processing</h3>
            <div class="info-card warning">
                <div class="info-card-title">Critical warning</div>
                <p>This software processes financial transactions. You are solely responsible for testing, validation, and ensuring the software meets your regulatory and business requirements. No warranties are provided regarding financial accuracy or compliance.</p>
            </div>
            <ul>
                <li><strong>No transaction guarantees:</strong> no warranty that financial transactions will process correctly</li>
                <li><strong>No regulatory compliance:</strong> no guarantee of compliance with financial regulations</li>
                <li><strong>No data accuracy:</strong> no warranty regarding accuracy of financial calculations or data</li>
                <li><strong>No security assurance:</strong> no guarantee of security for sensitive financial data</li>
                <li><strong>No audit compliance:</strong> no warranty regarding audit trail accuracy or completeness</li>
            </ul>

            <h3>Complete disclaimer of liability</h3>
            <div class="info-card danger">
                <div class="info-card-title">Limitation of liability (section 16)</div>
                <p><strong>IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.</strong></p>
            </div>

            <p>The developers and contributors of ISO8583Studio have no liability whatsoever for:</p>
            <ul>
                <li><strong>Financial losses:</strong> any financial losses, lost profits, or business damages</li>
                <li><strong>Data loss:</strong> loss of data, corruption, or unauthorized access to information</li>
                <li><strong>System failures:</strong> system downtime, crashes, or integration failures</li>
                <li><strong>Transaction errors:</strong> errors in financial transaction processing or calculations</li>
                <li><strong>Security breaches:</strong> security vulnerabilities or data breaches</li>
                <li><strong>Regulatory issues:</strong> regulatory violations or compliance failures</li>
            </ul>

            <div class="info-card warning">
                <div class="info-card-title">At your own risk</div>
                <p><strong>You use this software entirely at your own risk. You are responsible for evaluating the software's suitability for your intended use and for all consequences of its use.</strong></p>
            </div>
        </ui-section>

        <ui-section anchor="support" heading="Community support">

            <h3>Open source support model</h3>
            <p>As open source software, support is primarily community-driven:</p>
            <ul>
                <li><strong>Community forums:</strong> user community provides peer-to-peer support</li>
                <li><strong>Documentation:</strong> comprehensive documentation maintained by the community</li>
                <li><strong>Issue tracking:</strong> public issue tracking on the project repository</li>
                <li><strong>Wiki and knowledge base:</strong> community-maintained documentation and guides</li>
            </ul>

            <h3>No guaranteed support</h3>
            <div class="info-card warning">
                <div class="info-card-title">Important</div>
                <p>There is no guaranteed support, service level agreements, or response times. Support is provided by volunteers on a best-effort basis.</p>
            </div>
            <ul>
                <li><strong>Best effort:</strong> community members help when available and willing</li>
                <li><strong>No SLA:</strong> no guaranteed response times or resolution schedules</li>
                <li><strong>Volunteer basis:</strong> all support is provided by volunteers in their spare time</li>
                <li><strong>Self-service:</strong> users are encouraged to read documentation and search existing issues first</li>
            </ul>

            <h3>How to get help</h3>
            <ul>
                <li><strong>Read documentation:</strong> check the <a href="/docs">documentation</a> first</li>
                <li><strong>Search issues:</strong> look through existing GitHub issues for solutions</li>
                <li><strong>Community forums:</strong> ask questions in community discussion forums</li>
                <li><strong>Report bugs:</strong> submit bug reports through the project's issue tracker</li>
                <li><strong>Contribute:</strong> contribute fixes and improvements back to the project</li>
            </ul>
        </ui-section>

        <ui-section anchor="contact" heading="Community and contact">
            <p>ISO8583Studio is open source software. Support comes from the community, and contributions are welcome from everyone.</p>

            <div class="hub-grid">
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio/discussions"><div class="hub-body"><div class="hub-title">Discussions <span class="badge badge-blue">Q&amp;A</span></div><p class="hub-desc">Community questions and discussions.</p><span class="hub-link">GitHub Discussions →</span></div></a>
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio/issues"><div class="hub-body"><div class="hub-title">Bug reports <span class="badge badge-yellow">Issues</span></div><p class="hub-desc">Report bugs and request features.</p><span class="hub-link">GitHub Issues →</span></div></a>
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio"><div class="hub-body"><div class="hub-title">Source code <span class="badge badge-green">AGPL v3</span></div><p class="hub-desc">Read the source, and contribute back to the project.</p><span class="hub-link">Open repository →</span></div></a>
                <a class="hub-card" href="mailto:sk@iso8583.studio"><div class="hub-body"><div class="hub-title">Professional services <span class="badge badge-purple">Optional</span></div><p class="hub-desc">Paid consulting, custom development, or enterprise support.</p><span class="hub-link">sk@iso8583.studio →</span></div></a>
            </div>

            <div class="info-card note">
                <div class="info-card-title">Remember</div>
                <p>This is open source software. Support is provided by the community on a volunteer basis with no guarantees or service level agreements.</p>
            </div>

            <div class="info-card tip">
                <div class="info-card-title">AGPL v3 summary</div>
                <p><strong>Permissions:</strong> commercial use, modification, distribution, patent use, private use<br>
                   <strong>Conditions:</strong> disclose source, license and copyright notice, same licence, state changes, network use is distribution<br>
                   <strong>Limitations:</strong> liability, trademark use, warranty</p>
                <p>This is not legal advice. See the full <a href="https://www.gnu.org/licenses/agpl-3.0.html#license-text">GNU AGPL v3 text</a>, also included as the <a href="https://github.com/hpkaushik121/Iso8583studio/blob/main/LICENSE">LICENSE file</a> in the repository, for complete terms.</p>
            </div>

            <div class="legal-foot">
                <p><strong>Terms version</strong> 2.0 (Open Source) &nbsp;·&nbsp; <strong>Effective date</strong> 9 June 2025 &nbsp;·&nbsp; <strong>Last revised</strong> 20 September 2026</p>
                <p>See also the <a href="/privacy-policy">Privacy Policy</a>.</p>
            </div>
        </ui-section>
</main>`,
})
export class TermsAndConditionsPage {}
