const {WebSocketServer}=require('ws');const dgram=require('dgram');
function player(port,name){const w=new WebSocketServer({port});w.on('connection',(s,r)=>{console.log(`${name}: probe from ${r.socket.remoteAddress}`);s.send(JSON.stringify({type:'welcome',server:name,ip:'192.168.1.28'}));s.send(JSON.stringify({type:'state_update',queue:[]}))});}
player(48080,'SongHub Android 2.1.67');player(48090,'SongHub PC (custom port)');
const u=dgram.createSocket('udp4');setInterval(()=>u.send('SONGHUB_SERVER:48090',48080,'127.0.0.1'),1000);
