// @ts-nocheck
/* Ported verbatim from the design system (templates/_journey/terminal-model.js). */
// Premium POS terminal; local X is width, +Y is the top, +Z is the card slot.
// Set the display with terminal.userData.setState('ready'|'processing'|'approved').
// Card planes face +Y. Slot coordinates and orientations are in local space.
export function buildTerminal(T) {
  const terminal = new T.Group(); terminal.name = 'pearl_payment_terminal';
  const material = (color, roughness=.24, metalness=.16) => new T.MeshPhysicalMaterial({color, roughness, metalness, clearcoat:1, clearcoatRoughness:.12});
  const pearl=material(0xc5dded,.23,.25), rim=material(0xe3f1fa,.19,.28), bottom=material(0x7899b2,.27,.33), navy=material(0x111d2d,.28,.12), silver=material(0xabc8de,.19,.62), blue=material(0x1e88e5,.2,.32), mint=material(0x81e4d7,.23,.16);
  const glow=new T.MeshStandardMaterial({color:0x9fd3ff,emissive:0x1e88e5,emissiveIntensity:1.5,roughness:.2});
  function rounded(w,h,d,r=.15,bevel=.04){
    const s=new T.Shape(),x=-w/2,y=-h/2; r=Math.min(r,w/2-.001,h/2-.001);bevel=Math.min(bevel,d*.42);
    s.moveTo(x+r,y);s.lineTo(x+w-r,y);s.quadraticCurveTo(x+w,y,x+w,y+r);s.lineTo(x+w,y+h-r);s.quadraticCurveTo(x+w,y+h,x+w-r,y+h);s.lineTo(x+r,y+h);s.quadraticCurveTo(x,y+h,x,y+h-r);s.lineTo(x,y+r);s.quadraticCurveTo(x,y,x+r,y);
    const g=new T.ExtrudeGeometry(s,{depth:d-2*bevel,steps:1,bevelEnabled:true,bevelSize:bevel,bevelThickness:bevel,bevelSegments:5,curveSegments:16});g.translate(0,0,-d/2+bevel);g.computeVertexNormals();return g;
  }
  function box(parent,w,h,d,mat,x,y,z,r=.10,bevel=.025){const o=new T.Mesh(rounded(w,h,d,r,bevel),mat);o.position.set(x,y,z);parent.add(o);return o;}
  function flat(parent,w,d,h,mat,x,y,z,r=.10,bevel=.025){const o=box(parent,w,d,h,mat,x,y,z,r,bevel);o.rotation.x=-Math.PI/2;return o;}
  function label(parent,value,w,h,x,y,z,color='#344d65',font='500 100px "IBM Plex Sans", Arial'){
    const c=document.createElement('canvas');c.width=512;c.height=Math.max(48,Math.round(512*h/w));const ctx=c.getContext('2d');ctx.font=`500 ${Math.round(c.height*.83)}px Arial`;ctx.fillStyle=color;ctx.textAlign='center';ctx.textBaseline='middle';ctx.fillText(value,256,c.height*.53,495);const tex=new T.CanvasTexture(c);tex.colorSpace=T.SRGBColorSpace;
    const o=new T.Mesh(new T.PlaneGeometry(w,h),new T.MeshBasicMaterial({map:tex,transparent:true,depthWrite:false}));o.position.set(x,y,z);o.rotation.x=-Math.PI/2;parent.add(o);return o;
  }
  // Broad, smooth side wall plus separately moulded upper and lower housings.
  flat(terminal,2.20,3.55,.43,bottom,0,-.065,0,.32,.13);
  flat(terminal,2.29,3.57,.34,pearl,0,.115,-.015,.33,.105);
  flat(terminal,2.255,3.54,.023,silver,0,.045,-.015,.315,.009);
  flat(terminal,2.26,3.55,.265,pearl,0,.205,-.015,.31,.085);
  // Inner black bezel, polished display gasket and raised glass.
  flat(terminal,1.91,1.85,.071,navy,0,.375,-.67,.17,.025);
  flat(terminal,1.78,1.71,.016,silver,0,.412,-.67,.13,.006);
  flat(terminal,1.745,1.675,.025,navy,0,.426,-.67,.12,.009);
  const screenCanvas=document.createElement('canvas');screenCanvas.width=768;screenCanvas.height=736;
  const screenTex=new T.CanvasTexture(screenCanvas);screenTex.colorSpace=T.SRGBColorSpace;screenTex.anisotropy=4;
  const screen=flat(terminal,1.63,1.54,.009,new T.MeshBasicMaterial({map:screenTex}),0,.447,-.67,.082,.003);
  const uv=screen.geometry.getAttribute('uv'),pos=screen.geometry.getAttribute('position');for(let i=0;i<uv.count;i++)uv.setXY(i,pos.getX(i)/1.63+.5,pos.getY(i)/1.54+.5);uv.needsUpdate=true;
  const cover=new T.MeshPhysicalMaterial({color:0xa5cbe7,roughness:.11,metalness:.1,transparent:true,opacity:.07,clearcoat:1,depthWrite:false});flat(terminal,1.64,1.55,.008,cover,0,.454,-.67,.08,.002);
  // A subtle diagonal highlight is baked into the display so it also reads on mobile.
  let state='';
  function setState(next='ready') {
    if(next===state)return;state=next;terminal.userData.displayState=next;const x=screenCanvas.getContext('2d'),w=768,h=736;
    const bg=x.createLinearGradient(0,0,w,h);bg.addColorStop(0,'#16223a');bg.addColorStop(.52,'#243651');bg.addColorStop(1,'#101927');x.fillStyle=bg;x.fillRect(0,0,w,h);
    const shine=x.createLinearGradient(0,0,550,680);shine.addColorStop(0,'rgba(187,219,244,.12)');shine.addColorStop(.48,'rgba(159,211,255,.035)');shine.addColorStop(1,'rgba(159,211,255,0)');x.fillStyle=shine;x.beginPath();x.moveTo(0,0);x.lineTo(w,0);x.lineTo(360,h);x.lineTo(0,h);x.fill();
    x.textAlign='center';x.textBaseline='middle';x.font='500 23px "IBM Plex Mono", monospace';x.fillStyle='#9fb8d0';x.fillText('ISO8583STUDIO',w/2,66);
    x.strokeStyle=next==='approved'?'#81e4d7':'#83bffc';x.fillStyle=next==='approved'?'#81e4d7':'#83bffc';x.lineWidth=9;x.lineCap='round';
    if(next==='approved'){
      x.beginPath();x.arc(w/2,265,72,0,Math.PI*2);x.fill();x.strokeStyle='#183444';x.lineWidth=13;x.beginPath();x.moveTo(349,265);x.lineTo(376,291);x.lineTo(424,239);x.stroke();
    } else if(next==='processing') {
      x.save();x.translate(w/2,265);x.beginPath();for(let i=0;i<48;i++){const a=i*Math.PI/24,r=i%6<3?72:57;x.lineTo(Math.cos(a)*r,Math.sin(a)*r);}x.closePath();x.fill();x.globalCompositeOperation='destination-out';x.beginPath();x.arc(0,0,26,0,Math.PI*2);x.fill();x.restore();
      x.globalCompositeOperation='source-over';x.fillStyle='#263b56';x.beginPath();x.arc(w/2,265,25,0,Math.PI*2);x.fill();
    } else {
      x.strokeStyle='#7eb6e5';x.lineWidth=4;x.beginPath();x.ellipse(w/2,265,112,78,0,0,Math.PI*2);x.stroke();x.lineWidth=10;
      for(let i=0;i<4;i++){x.beginPath();x.arc(w/2-54,265,30+i*24,-.88,.88);x.stroke();}x.lineWidth=5;x.beginPath();x.moveTo(439,240);x.lineTo(473,250);x.moveTo(439,263);x.lineTo(473,273);x.stroke();
    }
    x.fillStyle=next==='approved'?'#81e4d7':'#edf6ff';x.font='500 48px "IBM Plex Sans", Arial';x.fillText(next==='approved'?'APPROVED':next==='connected'?'Card connected':next==='reading'?'Reading chip':next==='processing'?'Processing':'Ready for card',w/2,421);
    x.fillStyle='#8eabc4';x.font='400 25px "IBM Plex Sans", Arial';x.fillText(next==='approved'?'Response 00':next==='connected'?'EMV handshake complete':next==='reading'?'Establishing EMV session':next==='processing'?'Building the request':'Insert your test card',w/2,485);
    for(let i=0;i<4;i++){x.fillStyle=next==='approved'?'#64d8cb':i===0?'#9fd3ff':'#3d77a5';x.beginPath();x.arc(w/2+(i-1.5)*32,560,7,0,Math.PI*2);x.fill();}screenTex.needsUpdate=true;
  }
  setState('ready');
  // Key recesses, soft-touch sculpted keycaps, and legible sub-surface legends.
  const labels=['1','2','3','4','5','6','7','8','9','×','0','✓'];
  for(let row=0;row<4;row++)for(let col=0;col<3;col++){
    const x=(col-1)*.60,z=.49+row*.30,i=row*3+col;
    flat(terminal,.48,.249,.029,bottom,x,.356,z,.072,.012);
    flat(terminal,.459,.227,.092,i===11?mint:rim,x,.402,z,.067,.033);
    label(terminal,labels[i],.30,.17,x,.453,z,i===11?'#138c80':i===9?'#9c6571':'#45657f');
    if(i<9)label(terminal,['','ABC','DEF','GHI','JKL','MNO','PQRS','TUV','WXYZ'][i],.18,.038,x,.455,z+.059,'#6b8b9f','500 54px Arial');
  }
  // Contact card entry: dark aperture, polished lip and inset guide rails.
  box(terminal,1.86,.24,.063,bottom,0,.038,1.798,.11,.012);
  box(terminal,1.71,.132,.067,navy,0,.065,1.839,.054,.014);
  box(terminal,1.67,.030,.026,silver,0,.147,1.867,.014,.008);
  box(terminal,1.65,.019,.026,silver,0,-.019,1.868,.009,.006);
  for(const side of [-1,1])box(terminal,.065,.105,.07,silver,side*.82,.064,1.86,.015,.011);
  // Flush power key, side seam, speaker perforations and underside feet.
  for(const side of [-1,1]) {
    const key=box(terminal,.026,.09,.40,bottom,side*1.16,.07,-.18,.025,.01);
    for(let j=0;j<8;j++) {const vent=box(terminal,.02,.045,.035,navy,side*1.157,-.09,-.70+j*.073,.014,.008);}
    for(const z of [-1.24,1.20]) {const screw=new T.Mesh(new T.CylinderGeometry(.030,.030,.010,16),silver);screw.rotation.z=Math.PI/2;screw.position.set(side*1.136,-.075,z);terminal.add(screw);}
  }
  for(const x of [-.74,.74])for(const z of [-1.23,1.17])flat(terminal,.31,.38,.055,navy,x,-.286,z,.08,.019);
  const led=new T.Mesh(new T.SphereGeometry(.026,16,10),glow);led.scale.y=.32;led.position.set(.92,.381,.07);terminal.add(led);
  label(terminal,'STUDIO',.53,.075,0,.351,1.64,'#5c7b96');
  terminal.userData.screen=screen;terminal.userData.screenTexture=screenTex;terminal.userData.setState=setState;
  terminal.userData.cardSlot={position:new T.Vector3(0,.065,1.84),quaternion:new T.Quaternion().setFromEuler(new T.Euler(-Math.PI/2,0,0)),insertedPosition:new T.Vector3(0,.065,2.10),approachPosition:new T.Vector3(0,.065,3.25)};
  terminal.userData.dimensions={width:2.35,depth:3.70,height:.76};
  return terminal;
}
