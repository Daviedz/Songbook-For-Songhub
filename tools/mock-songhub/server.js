// behaves like SongHub: melody cycles 0..5, transpose/tempo clamp -8..8, state_update after each command
const {WebSocketServer}=require('ws');
const wss=new WebSocketServer({port:48080});
let st={queue:['036527'],now:{code:'032538',title:'Friends Lang Tayo (Original Karaoke Version)',artist:'Grin Department'},status:'playing',transpose:0,tempo:0,melody:0};
const bc=()=>wss.clients.forEach(c=>c.send(JSON.stringify({type:'state_update',queue:st.queue,status:st.status,now_playing:st.now,transpose:st.transpose,tempo:st.tempo,melody:st.melody})));
const cl=v=>Math.max(-8,Math.min(8,v));
wss.on('connection',ws=>{ws.send(JSON.stringify({type:'welcome',server:'mock'}));bc();
 ws.on('message',(d,isBin)=>{
  if(isBin){const op=d[0],v=d.length>1?(d[1]<<24>>24):0;console.log('BIN',op,v);if(op===4)st.transpose=cl(st.transpose+v);if(op===5)st.tempo=cl(st.tempo+v)}
  else{const j=JSON.parse(d);if(j.action==='melody'){st.melody=(st.melody+1)%6;console.log('melody ->',st.melody)}}
  setTimeout(bc,150)})});
