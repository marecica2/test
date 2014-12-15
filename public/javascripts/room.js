var messages = [];
var socket = io.connect(star.server_host);
var content = document.getElementById("content");
var callSound = null;
var title = "";
var caller = null;
var users = [];
var t;
var myStream = null;

star.data = null;
star.maximized = null;
star.maximizedScreen = null;
star.maximizedId = null;
star.selectedId = null;
star.muted = false;
star.cameraoOff = false;
star.switchAuto = true;
star.chatMimized = true;
star.screenShare = false;
    
var webrtc = null;
if(star.userInRoom){
    webrtc = new SimpleWebRTC({
        localVideoEl: 'localVideo',
        //remoteVideosEl: 'remotes',
        autoRequestMedia: true,
        url: star.server_host,
        debug: false,
        detectSpeakingEvents: true,
        autoAdjustMic: false
    });
}


// if user is room
if(webrtc != null){
    
    //bind all event handlers when webrtc is ready
    webrtc.on('readyToCall', function () {
        
         //automatically create user_joined event
         socket.emit('user_joined', {"user" : star.user, "room" : star.room, "usr" : star.usr, "peer" : webrtc.connection.socket.sessionid});
        
         // automatic join to room
         var room = star.room
         joinRoom(room, joinRoomCallback);
         
         $(document).click(function(){
             $(".peer-controls").hide();
         });

         // muting unmuting
         $("#controls-camera").click(function(){
             if(!star.cameraoOff){
                 star.cameraoOff = true;
                 webrtc.pauseVideo();
                 $(this).removeClass("btn-dark");
                 $(this).addClass("btn-danger");
             } else {
                 star.cameraoOff = false;
                 webrtc.resumeVideo();
                 $(this).removeClass("btn-danger");
                 $(this).addClass("btn-dark");
             }
         });

         // muting unmuting
         $("#controls-mute").click(function(){
             if(star.muted == false){
                 $(this).removeClass("btn-dark");
                 $(this).addClass("btn-danger");
                 $(this).html("<i class='icon-mute'></i>");
                 $(".peer-label-muted[data-id='"+getPeerId()+"']").show();
                 webrtc.mute();
                 star.muted = true;
             } else {
                 $(this).removeClass("btn-danger");
                 $(this).addClass("btn-dark");
                 $(this).html("<i class='icon-sound'></i>");
                 $(".peer-label-muted[data-id='"+getPeerId()+"']").hide();
                 webrtc.unmute();
                 star.muted = false;
             }
             var data = {};
             data.id = getPeerId();
             data.muted = star.muted;
             socket_message_broadcast("mute-broadcast", data);
         });

         $(document).on("click", ".peer-mute", function(event){
             event.stopPropagation();
             var id = $(this).attr("data-id");
             if(id == getPeerId()){
                 $("#controls-mute").html("<i class='icon-mute'></i>");
                 $("#controls-mute").removeClass("btn-dark");
                 $("#controls-mute").addClass("btn-danger");
                 webrtc.mute();
                 star.muted = true;
             }
             $(".peer-label-muted[data-id='"+id+"']").show();
             $(".peer-controls[data-id='"+id+"']").hide();
             var data = {};
             data.id = id;
             data.muted = true;
             socket_message_broadcast("mute-broadcast", data);
         });

         $(document).on("click", ".peer-unmute", function(event){
             event.stopPropagation();
             var id = $(this).attr("data-id");
             if(id == getPeerId()){
                 $("#controls-mute").html("<i class='icon-sound'></i>");
                 $("#controls-mute").removeClass("btn-danger");
                 $("#controls-mute").addClass("btn-dark");
                 webrtc.unmute();
                 star.muted = false;                 
             }
             $(".peer-label-muted[data-id='"+id+"']").hide();
             $(".peer-controls[data-id='"+id+"']").hide();
             var data = {};
             data.id = id;
             data.muted = false;
             socket_message_broadcast("mute-broadcast", data);
         });
         
         // peer dropdown clicked
         $(document).on("click", ".video-dropdown", function(event){
             event.stopPropagation();
             var id = $(this).attr("data-id");
             if($(".peer-controls[data-id='"+id+"']").is(':visible'))
                 $(".peer-controls[data-id='"+id+"']").hide();
             else {
                 $(".peer-controls").hide();
                 $(".peer-controls[data-id='"+id+"']").show();
             }
         });
         
         // maximize mimize peer
         $(document).on("click", ".user-elm", function(){
             var id = $(this).attr("data-id");
             var local = $(this).parent().parent().parent().attr("data-local");
             
    
             if(star.selectedId == null || star.selectedId != id){
                 star.selectedId = id;
                 star.switchAuto = false;
                 maximizeMimize(id, local);
                 $(this).attr("data-reverse", "false");
             } else {
                 star.selectedId = null;
                 star.switchAuto = true;
                 maximizeMimize(id, local);
                 $(this).attr("data-reverse", "true");
             }
         });
    
         // screenshare
         if (!webrtc.capabilities.screenSharing) {
             $('#screenShareButton').attr('disabled', 'disabled');
         }
         
         $('#screenShareButton').click(function () {
             if (!star.screenShare) {
                 getScreenId(function (error, sourceId, screen_constraints) {
                     if(sourceId && sourceId != 'firefox') {
                         screen_constraints = {
                             video: {
                                 mandatory: {
                                     chromeMediaSource: 'screen',
                                     maxWidth: 1920,
                                     maxHeight: 1080,
                                     minAspectRatio: 1.77
                                 }
                             }
                         };

                         if (error === 'permission-denied') return alert('Permission is denied.');
                         if (error === 'not-chrome') return alert('Please use chrome.');

                         if (!error && sourceId) {
                             screen_constraints.video.mandatory.chromeMediaSource = 'desktop';
                             screen_constraints.video.mandatory.chromeMediaSourceId = sourceId;
                         }
                     }

                     navigator.getUserMedia = navigator.mozGetUserMedia || navigator.webkitGetUserMedia;
                     navigator.getUserMedia(screen_constraints, function (stream) {
                         myStream = stream;
                         myStream.type = "screenShare";
                         webrtc.shareScreenMy(myStream);
                         //$("#screen-unique-id")[0].src = URL.createObjectURL(stream);
                     }, function (error) {
                         console.error(error);
                     });
                 }); 
                 $(".peer-label-screenshare[data-id='"+getPeerId()+"']").show();
                 star.screenShare = true;
                 $("#screen-unique-id").show();
                 webrtc.pauseVideo();                 
                 $(this).removeClass("btn-dark");
                 $(this).addClass("btn-success");
                 var data = {};
                 data.id = getPeerId();
                 data.screenShare = true;
                 socket_message_broadcast("screenshare-broadcast", data);
             } else {
                 $(".peer-label-screenshare[data-id='"+getPeerId()+"']").hide();
                 $("#screen-unique-id")[0].src = "";
                 $("#screen-unique-id").hide();
                 webrtc.resumeVideo();
                 webrtc.stopScreenShare();
                 star.screenShare = false;
                 $(this).addClass("btn-dark");
                 $(this).removeClass("btn-success");
                 var data = {};
                 data.id = getPeerId();
                 data.screenShare = false;
                 socket_message_broadcast("screenshare-broadcast", data);
             }
         });         
             
         
         $("#controls-switchauto").click(function() {
             star.switchAuto = !star.switchAuto;
             if(star.switchAuto)
                 $(this).addClass("btn-success");
             else
                 $(this).removeClass("btn-success");
         });
         
         
         $("#controls-hangup").click(function() {
             $(".controls").hide();
             peerRemoveCurrent();
             for(var i = 0; i < peers().length; i++){
                 peers()[i].end();
             }
             
             $("#remotes").remove();
             $("#localVideo").remove();
             webrtc.leaveRoom();
             webrtc.stopLocalVideo();
             webrtc.connection.removeAllListeners();
             webrtc.connection.disconnect();
             var data = {};
             data.id = getPeerId();
             socket_message_all("controls-hangup", data);
             send_chat(' has left the conversation', star.user);

             window.close();
         });
         
    });
    
    
    webrtc.on('localScreenRemoved', function () {
    
    });
    
    // local screen share obtained
    webrtc.on('localScreenAdded', function (video) {
        
    });
    
    
    // we got access to the camera
    webrtc.on('localStream', function (stream) {
        console.log("local stream added " + getPeerId());
        
        // move the video avatar
        var id = getPeerId();
        $(webrtc.getLocalVideoContainer()).parent().wrap("<div data-id='"+id+"' data-local='true' style='position:relative' id='video-element-"+id+"' class='user-elm'></div>");
        $(webrtc.getLocalVideoContainer()).parent().parent().append("<div id='localVolume' class='volumeBar'></div>");
        webrtc.getLocalVideoContainer().play();
    });
    
    
    // a peer video has been added
    webrtc.on('videoAdded', function (video, peer) {
        console.log('remote video added ' +peer.id);
        
        var remotes = document.getElementById('remotes');
        var isScreen = $(video).attr("id").indexOf("_screen") != -1 ? true : false;
    
        if (remotes) {
            if(isScreen){
                //$("#video-element-"+peer.id+"[data-screen='true']").remove();
                //$("#"+peer.id+"_video_incoming").remove();
                //$(container).wrap("<div data-screen='true' data-id='"+peer.id+"' id='video-element-"+peer.id+"' class='user-elm'></div>");
                
                // replace it with screen stream
                $("#"+peer.id+"_video_incoming").before(video);
    
                // hide original video
                $("#"+peer.id+"_video_incoming").hide();
    
                video.play();
                video.play();
            } else {
                var container = document.createElement('div');
                container.className = 'videoContainer';
                container.id = 'container_' + webrtc.getDomId(peer);
                container.appendChild(video);
                
                // move the video avatar
                remotes.appendChild(container);
    
                // suppress contextmenu
                //video.oncontextmenu = function () { return false; };
    
                // show the ice connection state
                if (peer && peer.pc) {
                    var connstate = document.createElement('div');
                    connstate.className = 'connectionstate';
                    container.appendChild(connstate);
                }            
                $(container).wrap("<div  data-id='"+peer.id+"' id='video-element-"+peer.id+"' class='user-elm'></div>");
                // show the remote volume
                var vol = document.createElement('div');
                vol.id = 'volume_' + peer.id;
                vol.className = 'volumeBar';
                $("#video-element-"+peer.id).append($(vol));
                video.play();
            }
        }
    })
    
    
    // a peer was removed
    webrtc.on('videoRemoved', function (video, peer) {
        //console.log('video removed ');
        
        var isScreen = $(video).attr("id").indexOf("_screen") != -1 ? true : false;
        if(isScreen){
            //$("#video-element-"+peer.id+"[data-screen='true']").remove();
            //console.log("removing screen");
            //console.log(video);
            
            // remove screen share video
            $(video).remove();
            $("#"+peer.id+"_screen_incoming").remove();
            star.maximizedScreen = null;
    
            // show original video
            $("#"+peer.id+"_video_incoming").show();
        } else {
            console.log("video removed");
            console.log(peer);
            $(video).remove();
            //$("#"+peer.id+"_video_incoming").remove();
            $("#video-element-"+peer.id).remove();
        }
    });
    
    
    // local volume has changed
    var volumes = 0;
    webrtc.on('volumeChange', function (volume, treshold) {
        if(!star.muted){
            showVolume(document.getElementById('localVolume'), volume);
            if(volume > treshold && star.switchAuto){
                var id = getPeerId();
                
                if(star.maximizedId == null || id != star.maximizedId || star.switchAuto){
                    maximizeMimize(id, true);
                }
                var data = {};
                data.id = getPeerId();
                data.volume = volume;
                socket_message_broadcast("speaking", data);
            }
        }
    });
    
    //webrtc.webrtc.on("speaking", star.room, function(data){
    //});
}


function usersRender(data) {
    data = JSON.parse(data);
    var html = "";
    users = [];
    for ( var index in data) {
        var row = data[index];
            for ( var usr in row) {
                if(row[usr].room == star.room){
                    html = "";
                    users.push(row[usr]);
                    
                    var peerId = row[usr].peer;
                    if(row[usr] == getPeerId())
                        peerId = row[usr].id;
                    
                    html += "<div id='user-item-"+peerId+"' data-id='"+peerId+"' style='position:relative;' title='"+row[usr].user+"'>";
                    
                    // peer labels
                    html += "<div style='position:absolute;top:5px;left:5px;z-index:9999;'>";
                    html += "   <div style='display:none' class='peer-label-screenshare peer-control-lbl btn-success' data-id='"+peerId+"'><i class='fa fa-desktop'></i></div>"
                    html += "   <div style='display:none' class='peer-label-muted peer-control-lbl btn-danger' data-id='"+peerId+"'><i class='icon-mute'></i></div>"
                    html += "</div>";
                    
                    // peer controls
                    html += "<div class='video-dropdown' data-id='"+peerId+"'><i class='fa fa-chevron-down color-link-light'></i></div>"
                    html += "<div class='peer-controls' data-id='"+peerId+"' style='display:none;z-index:9999;opacity:0.8'>";
                    html += "   <button class='peer-mute btn margin-clear btn-short btn-dark avatar-mute-btn btn-peer' data-type='audio' data-name='" + row[usr].user + "' data-id='"+peerId+"'>"
                    html += "       <i class='icon-mute'></i> "+i18n("mute");
                    html += "   </button> ";
                    html += "   <button class='peer-unmute btn margin-clear btn-short btn-dark avatar-mute-btn btn-peer' data-type='audio' data-name='" + row[usr].user + "' data-id='"+peerId+"'>"
                    html += "       <i class='icon-sound'></i> "+i18n("unmute");
                    html += "   </button> ";
                    html += "</div>";

                    // peer avatar
                    html += "<div id='"+ peerId +"_video_small' class='peer-avatar'>";
                    html += "   <div class='peer-avatar-label'>" + (row[usr].usr != undefined ? row[usr].usr.name : row[usr].user) + "</div>";
                    html += "</div>";
    
                    html += "</div>";
                    
                    function closure(elm, html) {
                        $("#video-element-"+elm).waitUntilExists(function(){
                            $("#video-element-"+elm).prepend(html);
                        });
                    };
                    closure(peerId, html);
                }
            }
    }
    
    // check if buttons should be enabled or disabled  
    if(webrtc.webrtc.peers.length == 0 ){
    } else {
    }
}

function maximizeMimize(id, local){
    
    // mimize maximized video
    if(star.maximizedId != null){
        var moveTo = $(".videoContainer", "#video-element-"+star.maximizedId);
        $(".videoContainer", "#video-element-"+star.maximizedId).show();
        $(star.maximized).css("position", "inherit");
        $(star.maximized).css("z-index", "-2");
        $(star.maximized).appendTo(moveTo);
        if(star.maximized != null)
            star.maximized.play();
        $("#video-element-"+star.maximizedId).css("border", "3px solid rgba(0,0,0,0.7)");
        $("#"+star.maximizedId+"_video_small").hide();
        
        // mimize maximized screen
        
        $(star.maximizedScreen).removeClass("video-screen");
        $(star.maximizedScreen).addClass("video-screen-mimized");
        $(star.maximizedScreen).appendTo(moveTo);
        if(star.maximizedScreen != null)
            star.maximizedScreen.play();
        star.maximizedScreen = null;
    }
    
    // maximize mimized
    if(star.selectedId != null || star.selectedId == id || star.maximizedId == null || star.switchAuto){
        star.maximizedId = id;
        if(id == getPeerId()){
            star.maximized = webrtc.getLocalVideoContainer();
            $("#screen-unique-id").show();
        } else {
            $("#screen-unique-id").hide();
            if(getPeerById(id) != null)
                star.maximized = getPeerById(id).videoEl;
                // if screen exists maximize it too
                if($("#"+id+"_video_incoming").length > 0)
                    star.maximizedScreen = $("#"+id+"_screen_incoming")[0];
        }

        // for video
        $(star.maximized).appendTo(".body");
        $(star.maximized).css("position", "fixed");
        $(star.maximized).css("top", "0px");
        $(".videoContainer", "#video-element-"+star.maximizedId).hide();
        $(star.maximized).css("height", "100%");
        $(star.maximized).css("z-index", "-2");
        $("#"+star.maximizedId+"_video_small").show();
        $(".container", "#video-element-"+star.maximizedId).hide();
        if(star.selectedId != null)
            $("#video-element-"+id).css("border", "3px solid rgba(255,255,255,0.7)");
        if(star.maximized != null)
            star.maximized.play();
        
        // for screen
        if(star.maximizedScreen != null){
            $(star.maximizedScreen).appendTo(".body");
            $(".videoContainer", "#video-element-"+star.maximizedId).hide();
            $(star.maximizedScreen).addClass("video-screen");
            $(star.maximizedScreen).removeClass("video-screen-mimized");
            if(star.maximizedScreen != null)
                star.maximizedScreen.play();
        } 
    }
}


function joinRoomCallback(err, r){
    $(".controls").show();
}


function joinRoom(room, joinRoomCallback){
    var isInRoom = false;
    for(var i = 0; i < webrtc.webrtc.peers.length; i++){
        if(webrtc.webrtc.peers[i].id == getPeerId()){
            isInRoom = true;
        }
    }
    
    if(!isInRoom){
        webrtc.joinRoom(room , joinRoomCallback);
    } else {
        console.log("Join room status: Already in room:" + room + " peer:" + getPeerId());
    }
}


function showVolume(el, volume) {
    $(el).show();
    $(el).css("opacity", "1");
    if (!el) return;
    if (volume < -45) { // vary between -45 and -20
        el.style.height = '0px';
    } else if (volume > -20) {
        el.style.height = '100%';
    } else {
        el.style.height = '' + Math.floor((volume + 100) * 100 / 25 - 220) + '%';
    }
    $(el).fadeOut();
}




//
//
//message handler
//
//

socket.on('socket_message', function(data) {

    // notify other that user has joined
    if(data.event == "speaking"){
        if(data != undefined){
            
            // update volume bar 
            var vol = $("#volume_"+data.data.id);
            if(vol.length){
                showVolume(vol[0], data.data.volume);
            }
            
            // update maximized screen
            if(star.switchAuto){
                if(star.maximizedId == null || data.data.id != star.maximizedId){
                    maximizeMimize(data.data.id, false);
                }
            }
        }
    }
    
    if(data.event == "mute"){
        if(data.data.id == getPeerId()){
            console.log("you has been muted");
            webrtc.mute();
            $("#controls-mute").addClass("btn-danger");
            star.muted = true;
        }
    }

    if(data.event == "mute-broadcast"){
        if(data.data.id == getPeerId() && data.data.muted){
            $("#controls-mute").removeClass("btn-dark");
            $("#controls-mute").addClass("btn-danger");   
            $("#controls-mute").html("<i class='icon-mute'></i>");
            star.muted = true;
            webrtc.mute();
        }
        if(data.data.id == getPeerId() && !data.data.muted){
            $("#controls-mute").addClass("btn-dark");
            $("#controls-mute").removeClass("btn-danger");   
            $("#controls-mute").html("<i class='icon-sound'></i>");
            star.muted = false;
            webrtc.unmute();
        }
        if(data.data.muted){
            $(".peer-label-muted[data-id='"+data.data.id+"']").show();
        } else {
            $(".peer-label-muted[data-id='"+data.data.id+"']").hide();
        }
    }

    
    if(data.event == "screenshare-broadcast"){
        if(data.data.screenShare){
            $(".peer-label-screenshare[data-id='"+data.data.id+"']").show();
        } else {
            $(".peer-label-screenshare[data-id='"+data.data.id+"']").hide();
        }
    }
    

    // notify other that user has joined
    if(data.event == "user-joined"){
        peerRemoveCurrent();
    }

    else if(data.event == "controls-hangup"){
        // remove peer which has hanged up
        var id = data.data.id;
        //console.log("controls-hangup " + id);
        
        //remove video elements
        var peer = getPeerById(id);
        //console.log("removing peer " + id + " after hangup");

        if(peer != null){
            var video = $(peer.videoEl);
            if(video != null)
                video.remove();
        }
        
        //end disconnected peer manually
        while(peer != null){
            peer.end();
            peer = getPeerById(id);
        }
        
        peerRemoveCurrent();
        //console.log("remaining peers");
        //console.log(webrtc.webrtc.peers);
        
        // clean all in case of last peer
        if(webrtc.webrtc.peers.length == 0 ){
            webrtc.leaveRoom();
        } 
    }
    
    
    else if(data.event == "check-last-peer"){
        if(webrtc.webrtc.peers.length == 1){
            webrtc.webrtc.peers[0].end();
        }
    }    
    
});

socket.on('user_disconnect', function(data) {
    var disconnected_client = JSON.parse(data).client;
    usersRender(data);
    
    for(var i = 0; i < webrtc.webrtc.peers.length ; i++){
        if(webrtc.webrtc.peers[i].id == disconnected_client){
            webrtc.webrtc.peers[i].end();
        }
    }
    
    if(webrtc.webrtc.peers.length == 1 && webrtc.webrtc.peers[0].id == getPeerId()){
    //    webrtc.webrtc.peers[0].end();
    }
    
    if(webrtc.webrtc.peers.length == 0){
    //    $("#controls").hide();
    } 
});

socket.on('user_update', function(data) {
    console.log("user_update event");
    usersRender(data);
});





//
//
// universal handlers
//
//

function socket_message_all(event, data) {
    //console.log("emiting socket_message_all data: " + event);
    socket.emit('socket_message_all', {
        "user" : star.user,
        "room" : star.room,
        "event" : event,
        "client" : socket.socket.sessionid,
        "data" : data
    });
};

function socket_message_broadcast(event, data) {
    //console.log("emiting socket_message_broadcast:" + event);
    socket.emit('socket_message_broadcast', {
        "user" : star.user,
        "room" : star.room,
        "event" : event,
        "client" : socket.socket.sessionid,
        "data" : data
    });
};

function socket_message_broadcast_to(event, to, data) {
    //console.log("emiting socket_message_broadcast_to: " + event + " to: " + to);
    socket.emit('socket_message_broadcast_to', {
        "user" : star.user,
        "room" : star.room,
        "event" : event,
        "to" : to,
        "client" : socket.socket.sessionid,
        "data" : data
    });
};

function socket_message_broadcast_peers(event, data) {
    var peers = webrtc.webrtc.peers;
    var ids = [];
    
    for(usr in users){
        for(peer in peers){
            if(users[usr].id == peers[peer].id){
                ids.push(users[usr].id);
            }
        }
    }
    
    for(id in ids){
        socket_message_broadcast_to(event, ids[id], data);
    }
};

function socket_message_broadcast_not_peers(event, data) {
    var peers = webrtc.webrtc.peers;
    var ids = [];
    
    for(usr in users){
        var isPeer = false;
        for(peer in peers){
            if(users[usr].id == peers[peer].id){
                isPeer = true;
                break;
            }
        }
        if(!isPeer){
            ids.push(users[usr].id);
        }
    }
    
    for(id in ids){
        socket_message_broadcast_to(event, ids[id], data);
    }
};

function send_chat(message, usr){
    if(usr == undefined){
        socket.emit('send', { "message": message });  
    }
    else {
        socket.emit('send', { "message": message, "username": usr });  
    }
};





//
//
// helpers
//
//

function usersRefresh(){
    socket.emit("user_refresh", {});
}


function getPeerId(){
    //return socket.socket.sessionid;
    return webrtc.connection.socket.sessionid;
}

function getPeerById(id){
    var peers = webrtc.webrtc.peers;
    if(peers != undefined){
        for(var i = 0; i < peers.length; i++){
            if(peers[i].id == id){
                return peers[i];
            }
        }
    }
    return null;
}

function peerRemoveCurrent(){
    var peer = getPeerById(getPeerId());
    if(peer != null)
        peer.end();
}

function getUserById(id){
    for(var i = 0; i < users.length; i++){
        if(users[i].id == id){
            return users[i];
        }
    }
}

function s4() {
    return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
};

function uuid() {
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
           s4() + '-' + s4() + s4() + s4();
}

function peers(){
    return webrtc.webrtc.peers;
}


// chat services
$(document).ready(function(){
    $("#chat-slider").click(function() {
        var elm = $("#chat-container");
        if(star.chatMimized){
            elm.animate({right:'0px'},100);
            star.chatMimized = false;
        } else {
            elm.animate({right:'-325px'},100);
            star.chatMimized = true;
        }
    });
    
    
    // chat
    $("#content").click(function() {
        //$("#chat-text").focus();
    });

    // chat send
    $("#chat-send").click(function() {
        var comment = $("#chat-text").val();
        comment = comment.replace(/</g, '&lt;')
        comment = comment.replace(/>/g, '&gt;');
        send_chat(comment, $("#chat-name").val());
        $("#chat-text").val("");
        return false;
    });
});

socket.on('message', function(data) {
    if (data.message) {
        var html = '';
        if(data.username){
            var date = new Date().toLocaleString();
            html += '<b title="'+date+'">' + (data.username ? data.username : 'Server') + ': </b>';
            html += data.message + '<br />';
        }
        content.innerHTML = content.innerHTML + html;
        
        //if(messages.length > 20){
        //    messages.splice(0, 1);
        //}
        
        console.log(data);
        var elm = $("#chat-container");
        if(star.chatMimized){
            elm.animate({right:'0px'},100);
            star.chatMimized = false;
        }
    }
    content.scrollTop = content.scrollHeight;
});

$("#chat-send").click(function(){
    var data = {};
    data.event = star.room;
    data.name = star.user;
    data.comment = $("#chat-text").val();
    roomServices.saveFeed(data, function(){
    });
});

var roomServices = {};
roomServices.saveFeed = function(data, success, error){
 $.ajax({
     type: "POST",
     url: "/public/feed",
     data: JSON.stringify(data),
     success: success,
     error: error,
     contentType: "application/json"
 });
};

roomServices.getFeeds = function(params, success, error){
 $.ajax({
     type: "GET",
     url: "/public/feeds?"+params,
     success: success,
     error: error,
     contentType: "application/json"
 });
};

$(document).ready(function(){
    roomServices.getFeeds("event="+star.room, function(data){
        var html = "";
        
        for(var i = data.length-1; i >= 0; i--){
            var date = new Date(data[i].created).toLocaleString();
            html += "<b title='"+date+"' >"+data[i].name+": </b>"+data[i].comment+"<br/>";
        }
        $("#content").html(html);
    });
    
});



(function ($) {
    /**
    * @function
    * @property {object} jQuery plugin which runs handler function once specified element is inserted into the DOM
    * @param {function} handler A function to execute at the time when the element is inserted
    * @param {bool} shouldRunHandlerOnce Optional: if true, handler is unbound after its first invocation
    * @example $(selector).waitUntilExists(function);
    */

    $.fn.waitUntilExists    = function (handler, shouldRunHandlerOnce, isChild) {
        var found       = 'found';
        var $this       = $(this.selector);
        var $elements   = $this.not(function () { return $(this).data(found); }).each(handler).data(found, true);

        if (!isChild)
        {
            (window.waitUntilExists_Intervals = window.waitUntilExists_Intervals || {})[this.selector] =
                window.setInterval(function () { $this.waitUntilExists(handler, shouldRunHandlerOnce, true); }, 200)
            ;
        }
        else if (shouldRunHandlerOnce && $elements.length)
        {
            window.clearInterval(window.waitUntilExists_Intervals[this.selector]);
        }

        return $this;
    }

    }(jQuery));
