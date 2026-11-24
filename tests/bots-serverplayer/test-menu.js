const mc=require('minecraft-protocol');let c=mc.createClient({host:'127.0.0.1',port:25571,username:'OtherGuy',version:'1.21.11',auth:'offline'}), n=0;
c.on('error',e=>console.log('ERROR',e)); c.on('login',()=>{c.write('player_loaded',{});setTimeout(()=>c.write('chat_command',{command:'bots'}),1800)}); c.on('position',p=>{c.write('teleport_confirm',{teleportId:p.teleportId});c.write('player_loaded',{})});
c.on('open_window',p=>{console.log('WINDOW',++n,p.windowId,JSON.stringify(p.windowTitle)); const slot=[22,20,22,24,49,49][n-1];if(slot!==undefined)setTimeout(()=>{console.log('CLICK',slot);c.write('window_click',{windowId:p.windowId,stateId:0,slot,mouseButton:0,mode:0,changedSlots:[],cursorItem:null})},400)});
setTimeout(()=>process.exit(0),14000);
