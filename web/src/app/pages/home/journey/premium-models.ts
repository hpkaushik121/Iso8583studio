// @ts-nocheck
/* Ported verbatim from the design system (templates/_journey/premium-models.js). */
// Brand-colored, actual 3D geometry. The HSM is a conceptual cutaway.
export function buildPremium(T){
 const group=()=>new T.Group();
 const mat=(c,r=.25,m=.35)=>new T.MeshPhysicalMaterial({color:c,roughness:r,metalness:m,clearcoat:1,clearcoatRoughness:.12});
 const shell=mat(0xb7d1e5,.23,.32),edge=mat(0xd5e8f6,.16,.58),blue=mat(0x1e88e5,.2,.5),dark=mat(0x192d48,.3,.4),board=mat(0x102b41,.58,.16),teal=mat(0x64d8cb,.22,.3);
 const glass=new T.MeshPhysicalMaterial({color:0x9fd3ff,metalness:.12,roughness:.105,transparent:true,opacity:.19,depthWrite:false,side:T.DoubleSide,clearcoat:1});
 const light=new T.MeshStandardMaterial({color:0x6ab7ff,emissive:0x1e88e5,emissiveIntensity:2.4,metalness:.2,roughness:.2});
 function rounded(w,h,d,r=.08){const s=new T.Shape(),x=-w/2,y=-h/2;r=Math.min(r,w/3,h/3);s.moveTo(x+r,y);s.lineTo(x+w-r,y);s.quadraticCurveTo(x+w,y,x+w,y+r);s.lineTo(x+w,y+h-r);s.quadraticCurveTo(x+w,y+h,x+w-r,y+h);s.lineTo(x+r,y+h);s.quadraticCurveTo(x,y+h,x,y+h-r);s.lineTo(x,y+r);s.quadraticCurveTo(x,y,x+r,y);const bevel=Math.min(d*.22,.025);const g=new T.ExtrudeGeometry(s,{depth:d-2*bevel,bevelEnabled:true,bevelSegments:3,steps:1,bevelSize:bevel,bevelThickness:bevel,curveSegments:12});g.translate(0,0,-d/2+bevel);g.computeVertexNormals();return g;}
 function box(parent,w,h,d,material,xyz=[0,0,0],r=.06){const o=new T.Mesh(rounded(w,h,d,r),material);o.position.set(...xyz);parent.add(o);return o;}
 function flat(parent,w,d,h,material,xyz,r=.06){const o=box(parent,w,d,h,material,xyz,r);o.rotation.x=-Math.PI/2;return o;}
 function text(parent,value,w,h,xyz,color='#e9ecf5',rot=0){const c=document.createElement('canvas');c.width=1024;c.height=Math.max(48,Math.round(1024*h/w));const x=c.getContext('2d');x.font='500 '+Math.round(c.height*.80)+'px monospace';x.textAlign='center';x.textBaseline='middle';x.fillStyle=color;x.fillText(value,512,c.height/2,985);const map=new T.CanvasTexture(c);map.colorSpace=T.SRGBColorSpace;const o=new T.Mesh(new T.PlaneGeometry(w,h),new T.MeshBasicMaterial({map,transparent:true,depthWrite:false}));o.position.set(...xyz);o.rotation.x=rot;parent.add(o);return o;}
 function screw(parent,x,y,z){const o=new T.Mesh(new T.CylinderGeometry(.025,.025,.008,16),edge);o.position.set(x,y,z);parent.add(o);const n=new T.Mesh(new T.BoxGeometry(.03,.004,.004),dark);n.position.set(x,y+.006,z);parent.add(n);}
 function led(parent,xyz,col=light,r=.014){const o=new T.Mesh(new T.SphereGeometry(r,12,8),col);o.position.set(...xyz);parent.add(o);return o;}
 const card=group();card.name='persistent_test_card';
 const cardFinish=mat(0xffffff,.26,.42);
 const c=document.createElement('canvas');c.width=1024;c.height=640;const cx=c.getContext('2d');const grad=cx.createLinearGradient(0,0,1024,640);grad.addColorStop(0,'#2f7cc4');grad.addColorStop(.38,'#1f5286');grad.addColorStop(.74,'#17293f');grad.addColorStop(1,'#101520');cx.fillStyle=grad;cx.fillRect(0,0,1024,640);
 let seed=37;for(let j=0;j<3800;j++){seed=(seed*1664525+1013904223)>>>0;cx.fillStyle=`rgba(159,211,255,${.012+(seed%5)*.006})`;cx.fillRect(seed%1024,(seed>>>10)%640,35,1);}
 const coating=new T.CanvasTexture(c);coating.colorSpace=T.SRGBColorSpace;cardFinish.map=coating;const front=box(card,.856,.538,.016,cardFinish,[0,0,0],.035);const uv=front.geometry.getAttribute('uv'),pos=front.geometry.getAttribute('position');for(let i=0;i<uv.count;i++)uv.setXY(i,pos.getX(i)/.856+.5,pos.getY(i)/.538+.5);uv.needsUpdate=true;
 box(card,.848,.530,.0018,mat(0x233a56,.18,.5),[0,0,-.008],.032);
 text(card,'DEBIT',.15,.032,[-.325,.196,.009],'#6ab7ff');
 text(card,'\u2022\u2022\u2022\u2022 4242',.34,.048,[-.18,-.112,.01],'#e9ecf5');text(card,'09/29',.13,.024,[-.305,-.196,.01],'#9aa3ba');
 // Lead edge (left) has actual metal contact segments and engraved gaps.
 box(card,.144,.114,.004,edge,[-.24,.035,.011],.014);
 for(let j=0;j<6;j++)box(card,.042,.049,.001,dark,[-.285+(j%3)*.045,.007+Math.floor(j/3)*.056,.0137],.004);
 for(let j=0;j<6;j++)box(card,.039,.046,.001,edge,[-.285+(j%3)*.045,.007+Math.floor(j/3)*.056,.0144],.004);
 for(let j=0;j<3;j++){const o=new T.Mesh(new T.TorusGeometry(.035+j*.021,.003,6,22,Math.PI*.8),light);o.rotation.z=-Math.PI*.4;o.position.set(.24,.031,.011);card.add(o);}
 // Backend appliance: frosted casing, drilled ventilation, compact side I/O.
 const host=group();host.name='backend_host';
 flat(host,2.65,1.63,.48,shell,[0,.005,0],.22);flat(host,2.64,1.62,.037,edge,[0,.258,0],.21);flat(host,2.56,1.54,.023,blue,[0,-.246,0],.17);
 // Perforation is batched into a single draw call.
 const ventGeo=new T.CylinderGeometry(.025,.025,.006,10);ventGeo.rotateX(Math.PI/2);
 const vents=new T.InstancedMesh(ventGeo,mat(0x15273c,.49,.12),60),vDummy=new T.Object3D();
 for(let row=0;row<5;row++)for(let col=0;col<12;col++){vDummy.position.set(-.56+col*.086,.124-row*.062,.838);vDummy.updateMatrix();vents.setMatrixAt(row*12+col,vDummy.matrix);}host.add(vents);
 const mark=new T.LineSegments(new T.EdgesGeometry(new T.BoxGeometry(.15,.15,.15)),new T.LineBasicMaterial({color:0x37536e,depthTest:false}));mark.rotation.set(.22,.50,.05);mark.position.set(-1.02,.01,.868);host.add(mark);
 for(let i=0;i<4;i++)led(host,[.92+i*.075,-.115,.841],i===0?teal:light,.018);
 for(let i=0;i<3;i++){box(host,.031,.17,.28,dark,[1.344,-.01,-.49+i*.43],.009);box(host,.038,.13,.22,edge,[1.353,-.01,-.49+i*.43],.008);box(host,.045,.093,.177,dark,[1.366,-.005,-.49+i*.43],.006);}
 for(let j=0;j<8;j++)box(host,.012,.085,.022,dark,[-1.341,-.02,-.43+j*.12],.003);
 text(host,'ISO8583 / HOST',1.21,.085,[0,.285,0],'#4a6984',-Math.PI/2);text(host,'SIMULATION ENGINE',.60,.043,[.59,.286,.43],'#65849c',-Math.PI/2);
 [-1.11,1.11].forEach(x=>[-.60,.60].forEach(z=>screw(host,x,.282,z)));
 // HSM: a true cavity with separate top and floating shielding, not a solid-box trick.
 const hsm=group();hsm.name='conceptual_hsm';
 // One continuous rounded, hollow enclosure avoids a boxy four-wall silhouette.
 function roundShape(w,h,r){const a=new T.Shape(),x=-w/2,y=-h/2;a.moveTo(x+r,y);a.lineTo(x+w-r,y);a.quadraticCurveTo(x+w,y,x+w,y+r);a.lineTo(x+w,y+h-r);a.quadraticCurveTo(x+w,y+h,x+w-r,y+h);a.lineTo(x+r,y+h);a.quadraticCurveTo(x,y+h,x,y+h-r);a.lineTo(x,y+r);a.quadraticCurveTo(x,y,x+r,y);return a;}
 const caseShape=roundShape(2.61,1.94,.25),holePts=roundShape(2.23,1.54,.15).getPoints(16).reverse(),hole=new T.Path();hole.moveTo(holePts[0].x,holePts[0].y);for(const point of holePts.slice(1))hole.lineTo(point.x,point.y);caseShape.holes.push(hole);
 const caseGeo=new T.ExtrudeGeometry(caseShape,{depth:.43,bevelEnabled:true,bevelSize:.04,bevelThickness:.04,bevelSegments:5,steps:1,curveSegments:16});caseGeo.translate(0,0,-.215);caseGeo.rotateX(-Math.PI/2);
 const enclosure=new T.Mesh(caseGeo,shell);enclosure.position.y=.005;hsm.add(enclosure);
 flat(hsm,2.61,1.94,.135,shell,[0,-.229,0],.25);flat(hsm,2.47,1.80,.020,blue,[0,-.304,0],.21);
 const caseRimPts=roundShape(2.61,1.94,.25).getPoints(16).map(p=>new T.Vector3(p.x,.266,-p.y));hsm.add(new T.LineLoop(new T.BufferGeometry().setFromPoints(caseRimPts),new T.LineBasicMaterial({color:0x9fd3ff,transparent:true,opacity:.8})));
 flat(hsm,2.22,1.49,.035,board,[0,-.126,0],.06);
 // Circuit traces and paired package blocks.
 for(let j=0;j<12;j++){const x=(j%6-2.5)*.32,z=j<6?-.50:.50;flat(hsm,.22,.24,.06,dark,[x,-.074,z],.02);for(let k=0;k<4;k++)flat(hsm,.02,.09,.009,edge,[x-.077+k*.05,-.068,z+(j<6?.17:-.17)],.002);}
 for(let j=0;j<8;j++){const x=(j-3.5)*.13;flat(hsm,.012,.75,.005,blue,[x,-.103,0],.002);}
 const core=flat(hsm,.71,.71,.17,edge,[0,.056,0],.06);flat(hsm,.55,.55,.059,light,[0,.167,0],.05);text(hsm,'HSM',.34,.115,[0,.201,0],'#e9ecf5',-Math.PI/2);
 for(let side of [-1,1])for(let j=0;j<8;j++){const p=(j-3.5)*.075;flat(hsm,.033,.105,.014,edge,[p,.073,side*.395],.007);flat(hsm,.105,.033,.014,edge,[side*.395,.073,p],.007);}
 const aura=new T.PointLight(0x1e88e5,1.5,3.2);aura.position.set(0,.40,0);hsm.add(aura);
 const glowCanvas=document.createElement('canvas');glowCanvas.width=glowCanvas.height=128;const gx=glowCanvas.getContext('2d'),gg=gx.createRadialGradient(64,64,2,64,64,64);gg.addColorStop(0,'rgba(106,183,255,.85)');gg.addColorStop(.24,'rgba(30,136,229,.36)');gg.addColorStop(1,'rgba(30,136,229,0)');gx.fillStyle=gg;gx.fillRect(0,0,128,128);const glowMap=new T.CanvasTexture(glowCanvas);glowMap.colorSpace=T.SRGBColorSpace;const coreGlow=new T.Sprite(new T.SpriteMaterial({map:glowMap,transparent:true,opacity:.88,depthWrite:false,blending:T.AdditiveBlending}));coreGlow.position.set(0,.23,0);coreGlow.scale.set(1.70,1.70,1);hsm.add(coreGlow);hsm.userData.coreGlow=coreGlow;
 const lid=group();hsm.add(lid);flat(lid,2.61,1.94,.085,glass,[0,0,0],.25);
 const outline=new T.LineSegments(new T.EdgesGeometry(rounded(2.58,1.91,.07,.24),35),new T.LineBasicMaterial({color:0x9fd3ff,transparent:true,opacity:.92}));outline.rotation.x=-Math.PI/2;lid.add(outline);
 const rimPoints=roundShape(2.61,1.94,.25).getPoints(16).map(p=>new T.Vector3(p.x,.05,-p.y));const lidRim=new T.LineLoop(new T.BufferGeometry().setFromPoints(rimPoints),new T.LineBasicMaterial({color:0x9fd3ff,transparent:true,opacity:.9}));lid.add(lidRim);
 const shield=flat(hsm,1.55,1.10,.028,glass,[0,.22,0],.08);
 text(hsm,'HSM',.60,.185,[.22,.022,1.016],'#344f69');text(hsm,'SIMULATION',.57,.040,[.22,-.130,1.017],'#436684');
 const hsmMark=new T.LineSegments(new T.EdgesGeometry(new T.BoxGeometry(.16,.16,.16)),new T.LineBasicMaterial({color:0x37536e,depthTest:false}));hsmMark.rotation.set(.22,.50,.05);hsmMark.position.set(-.46,.015,1.028);hsm.add(hsmMark);
 [-.93,-.85,-.77].forEach(x=>led(hsm,[x,-.104,1.016],teal,.013));
 [-1.09,1.09].forEach(x=>[-.69,.69].forEach(z=>screw(lid,x,.049,z)));
 hsm.userData.lid=lid;hsm.userData.shield=shield;hsm.userData.core=core;
 // Faceted glass network globe: batched points and a continuous triangular mesh.
 const network=group();network.name='network_mesh';
 const globeGeo=new T.IcosahedronGeometry(1.075,2),globeMaterial=new T.MeshPhysicalMaterial({color:0x338fe0,emissive:0x064980,emissiveIntensity:.22,roughness:.20,metalness:.04,transparent:true,opacity:.10,depthWrite:false,flatShading:true,side:T.DoubleSide});
 network.add(new T.Mesh(globeGeo,globeMaterial));
 network.add(new T.LineSegments(new T.WireframeGeometry(globeGeo),new T.LineBasicMaterial({color:0x7fc9ff,transparent:true,opacity:.34,depthWrite:false,toneMapped:false})));
 const nodePositions=[],seen=new Set(),gp=globeGeo.getAttribute('position');for(let i=0;i<gp.count;i++){const x=gp.getX(i),y=gp.getY(i),z=gp.getZ(i),key=[x,y,z].map(v=>v.toFixed(4)).join(',');if(!seen.has(key)){seen.add(key);nodePositions.push(x,y,z);}}
 const pointGeo=new T.BufferGeometry();pointGeo.setAttribute('position',new T.Float32BufferAttribute(nodePositions,3));
 const pointTexCanvas=document.createElement('canvas');pointTexCanvas.width=pointTexCanvas.height=64;const px=pointTexCanvas.getContext('2d'),pg=px.createRadialGradient(32,32,0,32,32,32);pg.addColorStop(0,'rgba(233,247,255,1)');pg.addColorStop(.13,'rgba(174,222,255,.95)');pg.addColorStop(.35,'rgba(66,161,255,.34)');pg.addColorStop(1,'rgba(30,136,229,0)');px.fillStyle=pg;px.fillRect(0,0,64,64);const pointMap=new T.CanvasTexture(pointTexCanvas);pointMap.colorSpace=T.SRGBColorSpace;
 network.add(new T.Points(pointGeo,new T.PointsMaterial({map:pointMap,color:0xc9eaff,size:8,transparent:true,opacity:.95,depthWrite:false,blending:T.AdditiveBlending,sizeAttenuation:false,toneMapped:false})));
 const brightPositions=[];for(let i=0;i<nodePositions.length;i+=21)brightPositions.push(...nodePositions.slice(i,i+3));const brightGeo=new T.BufferGeometry();brightGeo.setAttribute('position',new T.Float32BufferAttribute(brightPositions,3));network.add(new T.Points(brightGeo,new T.PointsMaterial({map:pointMap,color:0xc9edff,size:15,transparent:true,opacity:.92,depthWrite:false,blending:T.AdditiveBlending,sizeAttenuation:false,toneMapped:false})));
 const cubeMaterial=new T.MeshPhysicalMaterial({color:0x9fd3ff,emissive:0x1e88e5,emissiveIntensity:.42,roughness:.15,metalness:.25,clearcoat:1}),cubeEdge=new T.LineBasicMaterial({color:0xb9e7ff,transparent:true,opacity:.86});
 for(const [x,y,z,size] of [[-.81,.77,.36,.17],[.68,.35,.93,.20],[-.72,-.52,.90,.15],[1.17,-.20,.27,.13]]){
  const cg=new T.BoxGeometry(size,size,size),cube=new T.Mesh(cg,cubeMaterial);cube.position.set(x,y,z);cube.rotation.set(.24,.42,.13);cube.add(new T.LineSegments(new T.EdgesGeometry(cg),cubeEdge));network.add(cube);
  const from=new T.Vector3(x,y,z),to=from.clone().normalize().multiplyScalar(1.06);network.add(new T.Line(new T.BufferGeometry().setFromPoints([from,to]),new T.LineBasicMaterial({color:0x6ab7ff,transparent:true,opacity:.7})));
 }
 // Issuer: a classical bank facade integrated with a modern server enclosure.
 const issuer=group();issuer.name='issuer_host';
 box(issuer,1.90,1.58,1.52,shell,[0,0,0],.14);
 flat(issuer,2.10,1.72,.13,edge,[0,-.85,0],.13);
 flat(issuer,1.99,1.64,.095,shell,[0,-.744,0],.10);
 flat(issuer,1.99,1.64,.10,edge,[0,.826,0],.10);
 // Recessed facade and raised stone details on the front face.
 box(issuer,1.53,1.32,.020,mat(0x83a9c9,.31,.28),[0,.015,.786],.07);
 box(issuer,1.35,.102,.13,edge,[0,-.582,.857],.03);
 box(issuer,1.24,.054,.11,edge,[0,-.490,.877],.017);
 const pedimentShape=new T.Shape();pedimentShape.moveTo(-.69,.455);pedimentShape.lineTo(0,.866);pedimentShape.lineTo(.69,.455);pedimentShape.closePath();
 const pedimentGeo=new T.ExtrudeGeometry(pedimentShape,{depth:.105,bevelEnabled:true,bevelSize:.022,bevelThickness:.019,bevelSegments:3,steps:1});
 const pediment=new T.Mesh(pedimentGeo,edge);pediment.position.z=.82;issuer.add(pediment);
 const innerTriangle=new T.Shape();innerTriangle.moveTo(-.43,.523);innerTriangle.lineTo(0,.772);innerTriangle.lineTo(.43,.523);innerTriangle.closePath();const inset=new T.Mesh(new T.ShapeGeometry(innerTriangle),shell);inset.position.z=.951;issuer.add(inset);
 box(issuer,1.38,.085,.16,edge,[0,.436,.848],.025);
 for(const x of [-.45,-.15,.15,.45]){
   const col=new T.Mesh(new T.CylinderGeometry(.065,.082,.76,20),edge);col.position.set(x,-.04,.882);issuer.add(col);
   for(const y of [-.444,.373]) {const cap=new T.Mesh(new T.CylinderGeometry(.105,.105,.057,20),edge);cap.position.set(x,y,.882);issuer.add(cap);}
   // Fine vertical fluting catches the studio rim light.
   for(let j=0;j<7;j++){const a=(j/6)*Math.PI;const flute=new T.Mesh(new T.CylinderGeometry(.005,.005,.66,5),shell);flute.position.set(x+Math.cos(a)*.069,-.04,.882+Math.sin(a)*.069);issuer.add(flute);}
 }
 // Visible right side contains blue server racks and small activity lamps.
 box(issuer,.025,1.32,1.24,dark,[.969,.005,0],.01);
 for(let j=0;j<5;j++){
   box(issuer,.057,.185,1.10,mat(0x335575,.23,.45),[1.006,.47-j*.234,0],.012);
   for(let k=0;k<4;k++)box(issuer,.060,.068,.014,edge,[1.042,.47-j*.234,-.39+k*.047],.004);
   for(let k=0;k<5;k++)led(issuer,[1.05,.47-j*.234,.01+k*.091],j%2?light:teal,.016);
   box(issuer,.07,.027,.115,edge,[1.047,.42-j*.234,.45],.008);
 }
 for(let side of [-1,1])for(let z of [-.53,.53])screw(issuer,side*.79,.884,z);
 text(issuer,'ISSUER',.67,.08,[0,.884,.13],'#456681',-Math.PI/2);
 return {card,host,hsm,network,issuer};
}
