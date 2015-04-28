var socket = io.connect(star.server_host);
var content = document.getElementById("content");
var callSound = null;
var title = "";
var caller = null;
var users = [];
var t;
var myStream = null;
var recordAudio, recordVideo;

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
star.drawing = false;
star.commentsDialog = false;
star.users = [];
    
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

//automatically create user_joined event
var usr = {};
usr.user = star.user;
usr.room = star.room;
usr.avatar = star.userAvatar;
usr.avatar = star.avatar;
usr.admin = star.isOwner;
usr.login = star.login;
usr.uuid = star.userUuid;
if(webrtc == null)
    socket.emit('user_joined', usr);


// video call buttons handlers
$(document).ready(function(){
    
    // stop instant broadcast
    $(".btn-instant-stop").click(function(){
        send_chat(' stopped broadcasting', star.user);
        var data = {};
        data.id = socket.socket.sessionid;
        socket_message_broadcast("instant-room-stop-broadcast", data);
        window.location = $(this).attr("data-href");
          
    });

    // start instat room
    $(".no-schedule-start").click(function(e){
        
    });

    // go to private room
    $(".btn-instant-room").click(function(){
        send_chat(' went to private room', star.user);
        
        var data = {};
        data.id = socket.socket.sessionid;
        socket_message_broadcast("instant-room-private-broadcast", data);
        window.location = $(this).attr("data-href");        
    });

    // start instant broadcast
    $(".btn-instant-start").click(function(){
        var data = {};
        data.id = socket.socket.sessionid;
        socket_message_broadcast("instant-room-start-broadcast", data);
        window.location = $(this).attr("data-href");        
    });
});


socket.on('socket_message', function(data) {
    if(data.event == "instant-room-private-broadcast"){
        setTimeout(function(){ window.location.reload();
        }, 5000);
    } 
    if(data.event == "instant-room-stop-broadcast"){
        window.location.reload();
    } 
    if(data.event == "no-schedule-start"){
        window.location.reload();
    } 
    if(data.event == "instant-room-start-broadcast"){
        setTimeout(function(){ window.location.reload();
        }, 5000);
    }  
 });

if(webrtc != null){
    webrtc.on('readyToCall', function () {
        if(webrtc != null)
            usr.peer = getPeerId();
         socket.emit('user_joined', usr);
        
         // automatic join to room
         var room = star.room
         joinRoom(room, joinRoomCallback);
         
         $(document).click(function(){
             $(".peer-controls").hide();
         });

         // show invite modal when there is only one user
         var stopInterval = setInterval(function(){
             if($(".user-elm").length <= 1)
                 $('#myModal').modal({show:true});
             clearInterval(stopInterval);
         }, 15000);
         
         // camera on off
         $("#controls-camera").click(function(){
             if(!star.cameraoOff){
                 star.cameraoOff = true;
                 webrtc.pauseVideo();
                 $(this).removeClass("btn-dark");
                 $(this).addClass("btn-danger");
                 $(".peer-label-camera[data-id='"+getPeerId()+"']").show();
                 $(".videoContainer", "#video-element-"+getPeerId()).hide();
                 $("#"+getPeerId()+"_video_small").show();
             } else {
                 star.cameraoOff = false;
                 webrtc.resumeVideo();
                 $(this).removeClass("btn-danger");
                 $(this).addClass("btn-dark");
                 $(".peer-label-camera[data-id='"+getPeerId()+"']").hide();
                 if(star.maximizedId != getPeerId())
                     $(".videoContainer", "#video-element-"+getPeerId()).show();
                 if(star.maximizedId != getPeerId())
                     $("#"+getPeerId()+"_video_small").hide();
             }
             
             var data = {};
             data.id = getPeerId();
             data.cameraoOff = star.cameraoOff;
             socket_message_broadcast("camera-broadcast", data);
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
                 var height = $(".peer-controls[data-id='"+id+"']").height() + 5;
                 $(".peer-controls[data-id='"+id+"']").css("top", "-"+height+"px");
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
         
         // hangup
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

             //$("#ratingModal").modal({"show" : true});
             window.close();
         });         
    
         // screenshare
         if (!webrtc.capabilities.screenSharing) {
             $('#screenShareButton').attr('disabled', 'disabled');
         }
         if(navigator.userAgent.toLowerCase().indexOf('chrome') == -1)
             $('#screenShareButton').attr('disabled', 'disabled');
         
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
                         
                         $(".peer-label-screenshare[data-id='"+getPeerId()+"']").show();
                         star.screenShare = true;
                         $("#screen-unique-id").show();
                         webrtc.pauseVideo();
                         
                         var data = {};
                         data.id = getPeerId();
                         data.screenShare = true;
                         socket_message_broadcast("screenshare-broadcast", data);
                         $("#screenShareButton").removeClass("btn-dark");
                         $("#screenShareButton").addClass("btn-success");
                     }, function (error) {
                         console.error(error);
                         $('#extensionModal').modal({show:true});
                     });
                 }); 
                 

             } else {
                 $(".peer-label-screenshare[data-id='"+getPeerId()+"']").hide();
                 $("#screen-unique-id")[0].src = "";
                 $("#screen-unique-id").hide();
                 webrtc.resumeVideo();
                 webrtc.stopScreenShare();
                 star.screenShare = false;
                 $("#screenShareButton").removeClass("btn-success");
                 $("#screenShareButton").addClass("btn-dark");
                 var data = {};
                 data.id = getPeerId();
                 data.screenShare = false;
                 socket_message_broadcast("screenshare-broadcast", data);
             }
         });         
    });
    
    
    webrtc.on('localScreenRemoved', function () {
    });
    
    
    webrtc.on('localScreenAdded', function (video) {
    });
    

    webrtc.on('connectionReady', function (id) {
        $(webrtc.getLocalVideoContainer()).parent().wrap("<div data-id='"+id+"' data-local='true' style='position:relative;' id='video-element-"+id+"' class='user-elm'></div>");
        $(webrtc.getLocalVideoContainer()).parent().parent().append("<div id='localVolume' class='volumeBar'></div>");
        webrtc.getLocalVideoContainer().play();
    });
    
    
    // we got access to the camera
    webrtc.on('localStream', function (stream) {
        
    });
    
    
    // a peer video has been added
    webrtc.on('videoAdded', function (video, peer) {
        var remotes = document.getElementById('remotes');
        var isScreen = $(video).attr("id").indexOf("_screen") != -1 ? true : false;
    
        if (remotes) {
            if(isScreen){
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
        var isScreen = $(video).attr("id").indexOf("_screen") != -1 ? true : false;
        if(isScreen){
            // remove screen share video
            $(video).remove();
            $("#"+peer.id+"_screen_incoming").remove();
            star.maximizedScreen = null;
    
            // show original video
            $("#"+peer.id+"_video_incoming").show();
        } else {
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
                    
                    
                    // dropdown + peer controls
                    html += "<div class='video-dropdown' data-id='"+peerId+"'><i class='fa fa-chevron-down color-link-light'></i></div>"
                    html += "<div class='peer-controls' data-id='"+peerId+"' style='display:none;z-index:9999;opacity:0.8'>";
                    if(typeof row[usr].avatar != "undefined"){
                        html += "<a class='btn margin-clear btn-short btn-dark avatar-mute-btn btn-peer' href='/user/"+row[usr].login+"' target='_blank' >"
                            html += "<i class='icon-user'></i> "+i18n("view-profile");
                        html += "</a> ";
                    }
                    html += "<button class='peer-mute btn margin-clear btn-short btn-dark avatar-mute-btn btn-peer' data-type='audio' data-name='" + row[usr].user + "' data-id='"+peerId+"'>"
                    html += "   <i class='icon-mute'></i> "+i18n("mute");
                    html += "</button> ";
                    html += "</div>";
                    
                    html += "<div id='user-item-"+peerId+"' data-id='"+peerId+"' style='position:relative;' title='"+row[usr].user+"'>";
                    
                    // peer labels
                    html += "<div style='position:absolute;top:5px;left:5px;z-index:9999;'>";
                    html += "   <div style='display:none' class='peer-label-screenshare peer-control-lbl btn-success' data-id='"+peerId+"'><i class='fa fa-desktop'></i></div>"
                    html += "   <div style='display:none' class='peer-label-muted peer-control-lbl btn-danger' data-id='"+peerId+"'><i class='icon-mute'></i></div>"
                    html += "   <div style='display:none' class='peer-label-camera peer-control-lbl btn-danger' data-id='"+peerId+"'><i class='fa fa-eye-slash'></i></div>"
                    html += "</div>";
                   
 

                    // peer avatar
                    html += "<div id='"+ peerId +"_video_small' class='peer-avatar'>";
                    html += "   <div class='peer-avatar-label'>" + (row[usr].usr != undefined ? row[usr].usr.name : row[usr].user);
                    if(row[usr].avatar != undefined && row[usr].avatar != null)
                        html += "       <br/><img src='"+row[usr].avatar+"_32x32' class='img-circle'>";
                    html += "   </div>";
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
        $("#video-element-"+star.maximizedId).css("border", "4px solid rgba(0,0,0,0.0)");
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
            $("#video-element-"+id).css("border", "4px solid rgba(255,255,255,0.7)");
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






//
//
//message handlers
//
//

if(star.userInRoom){
    socket.on('socket_message', function(data) {
        if(data.event == "speaking"){
            if(data != undefined){
                
                // update volume bar 
                var vol = $("#volume_"+data.data.id);
                if(vol.length){
                    showVolume(vol[0], data.data.volume);
                }
                
                // update maximized screen
                if(star.switchAuto && data.data.id != getPeerId()){
                    if(star.maximizedId == null || data.data.id != star.maximizedId){
                        maximizeMimize(data.data.id, false);
                    }
                }
            }
        }
        
        if(data.event == "mute"){
            if(data.data.id == getPeerId()){
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
    
        if(data.event == "camera-broadcast"){
            if(data.data.cameraoOff){
                $(".peer-label-camera[data-id='"+data.data.id+"']").show();
                if(star.maximizedId != data.data.id)
                    $(".videoContainer", "#video-element-"+data.data.id).hide();
                if(star.maximizedId != data.data.id)
                    $("#"+data.data.id+"_video_small").show();
                
            } else {
                $(".peer-label-camera[data-id='"+data.data.id+"']").hide();
                if(star.maximizedId != data.data.id)
                    $(".videoContainer", "#video-element-"+data.data.id).show();
                if(star.maximizedId != data.data.id)
                    $("#"+data.data.id+"_video_small").hide();
            }
        }
    
        if(data.event == "screenshare-broadcast"){
            if(data.data.screenShare){
                $(".peer-label-screenshare[data-id='"+data.data.id+"']").show();
            } else {
                $(".peer-label-screenshare[data-id='"+data.data.id+"']").hide();
            }
        }
    
        if(data.event == "user-joined"){
            peerRemoveCurrent();
        }

        if(data.event == "controls-hangup"){
            var id = data.data.id;
            var peer = getPeerById(id);
            if(peer != null){
                var video = $(peer.videoEl);
                if(video != null)
                    video.remove();
            }
            while(peer != null){
                peer.end();
                peer = getPeerById(id);
            }
            peerRemoveCurrent();
            if(webrtc.webrtc.peers.length == 0 ){
                webrtc.leaveRoom();
            } 
        }
        
        if(data.event == "check-last-peer"){
            if(webrtc.webrtc.peers.length == 1){
                webrtc.webrtc.peers[0].end();
            }
        }    
    });
    
    
    socket.on('user_disconnect', function(data) {
        var peerId = JSON.parse(data).peer;
        var peer = getPeerById(peerId);
        if(peer != null && peer != undefined){
            peer.end();
            var video = $(peer.videoEl);
            if(video != null){
                video.remove();
                $("#video-element-"+peer.id).remove();
            }
        }
    });
    
    
    socket.on('user_update', function(data) {
        usersRender(data);
    });
    
} 





//
//
// universal socket handlers
//
//

function socket_message_all(event, data) {
    socket.emit('socket_message_all', {
        "user" : star.user,
        "room" : star.room,
        "event" : event,
        "client" : socket.socket.sessionid,
        "data" : data
    });
};

function socket_message_broadcast(event, data) {
    socket.emit('socket_message_broadcast', {
        "user" : star.user,
        "room" : star.room,
        "event" : event,
        "client" : socket.socket.sessionid,
        "data" : data
    });
};

function socket_message_broadcast_to(event, to, data) {
    socket.emit('socket_message_broadcast_to', {
        "user" : star.user,
        "room" : star.room,
        "event" : event,
        "to" : to,
        "client" : socket.socket.sessionid,
        "data" : data
    });
};

function send_chat(message, usr){
    if(usr == undefined){
        socket.emit('send', { "message": message, "room" : star.room });  
    }
    else {
        socket.emit('send', { "message": message, "username": usr, "room" : star.room});  
    }
};




// 
//
// Canvas drawing
//
//
if(star.userInRoom){
    var canvas = {};
    canvas.type = "pen";
    canvas.canvas = null;
    canvas.ctx; 
    canvas.flag = false, 
    canvas.prevX = 0; 
    canvas.currX = 0; 
    canvas.prevY = 0; 
    canvas.currY = 0, 
    canvas.dot_flag = false;
    canvas.x = "rgba(255, 255, 255, 0.95)"; 
    canvas.y = 2;
    canvas.history = [];
    
    canvas.init = function() {
        canvas.canvas = $(".canvas-paper")[0];
        canvas.ctx = canvas.canvas.getContext("2d");
        canvas.w = canvas.canvas.width;
        canvas.h = canvas.canvas.height;
    
        canvas.canvas.addEventListener("mousemove", function (e) {
            canvas.findxy('move', e);
        }, false);
        canvas.canvas.addEventListener("mousedown", function (e) {
            canvas.findxy('down', e);
        }, false);
        canvas.canvas.addEventListener("mouseup", function (e) {
            canvas.findxy('up', e);
            if(canvas.history.length >= 11)
                canvas.history.shift();
            canvas.history.push(canvas.canvas.toDataURL());
        }, false);
        canvas.canvas.addEventListener("mouseout", function (e) {
            canvas.findxy('out', e);
        }, false);
    
    }
    
    
    canvas.mode = function(style){
        if(style == 'pen'){
            canvas.type = "pen";
            $("#mode-eraser").removeClass("active");
        } else {
            canvas.type = "eraser";
            $("#mode-pen").removeClass("active");
            $("#mode-eraser").addClass("active");
        }
    }
    
    
    canvas.undo = function() {
        if(canvas.history.length > 0){
            var undo =  canvas.history.pop();
            var image = new Image;
            image.src = undo;
            canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
            canvas.ctx.drawImage(image,0, 0);
            
            var data = {};
            data.image = canvas.canvas.toDataURL();
            socket_message_broadcast("canvas-undo", data);
        }
    }
    
    
    canvas.undoMessage = function(data){
        var image = new Image;
            image.src = data.image;
            canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
            canvas.ctx.drawImage(image,0, 0);
    }
    
    
    canvas.draw = function() {
        canvas.ctx.beginPath();
        
        if(canvas.type == "pen"){
            canvas.ctx.globalCompositeOperation="source-over";
            canvas.ctx.moveTo(canvas.prevX, canvas.prevY);
            canvas.ctx.lineTo(canvas.currX, canvas.currY);
            canvas.ctx.lineCap="round";
            canvas.ctx.strokeStyle = canvas.x;
            canvas.ctx.lineWidth = canvas.y;
            canvas.ctx.stroke();
            canvas.ctx.closePath();
        } else {
            canvas.ctx.globalCompositeOperation="destination-out";
            canvas.ctx.arc(canvas.currX,canvas.currY,30,0,Math.PI*2,false);
            canvas.ctx.fill();
        }
        
        var data = {};
        data.type = canvas.type;
        data.prevX = canvas.prevX;
        data.prevY = canvas.prevY;
        data.currX = canvas.currX;
        data.currY = canvas.currY;
        socket_message_broadcast("canvas-draw", data);
    }
    
    
    canvas.drawMessage = function(data){
        if(data != undefined){
            canvas.ctx.beginPath();
            if(data.type == "pen"){
                canvas.ctx.globalCompositeOperation="source-over";
                canvas.ctx.moveTo(data.prevX, data.prevY);
                canvas.ctx.lineTo(data.currX, data.currY);
                canvas.ctx.lineCap="round";
                canvas.ctx.strokeStyle = canvas.x;
                canvas.ctx.lineWidth = canvas.y;
                canvas.ctx.stroke();
                canvas.ctx.closePath();
            } else {
                canvas.ctx.globalCompositeOperation="destination-out";
                canvas.ctx.arc(data.currX,data.currY,30,0,Math.PI*2,false);
                canvas.ctx.fill();            
            }
        }
    }
    
    
    canvas.resize = function(){
        $(canvas.canvas).attr({ width: canvas.w, height: canvas.h });
        var image = new Image;
        if(canvas.history != undefined && canvas.history.length > 0){
            image.src = canvas.history[canvas.history.length-1];
            canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
            canvas.ctx.drawImage(image,0, 0);
        }
        var data = {};
        data.width = canvas.w;
        data.height = canvas.h;
        data.image = canvas.canvas.toDataURL();
        socket_message_broadcast("canvas-resize", data);
    }
    
    
    canvas.resizeMessage = function(data){
        $(".canvas-container").show();
        
        canvas.w = data.width;
        canvas.h = data.height;
        $(canvas.canvas).height(data.height);
        $(canvas.canvas).width(data.width);
        $(canvas.canvas).attr({ width: data.width, height: data.height });
        $(".ui-wrapper").height(data.height);
        $(".ui-wrapper").width(data.width);
        
        //redraw image
        var image = new Image;
        image.src = data.image;
        canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
        canvas.ctx.drawImage(image,0, 0);
        
        // activate button
        star.drawing = true;
        $("#canvas-open").removeClass("btn-dark");
        $("#canvas-open").addClass("btn-success");
        $(".canvas-container").show();
    }
    
    
    canvas.erase = function() {
        canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
        var data = {};
        socket_message_broadcast("canvas-erase", data);
    }
    
    
    canvas.eraseMessage = function(){
        canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
    }
    
    
    canvas.save = function() {
        // draw patter on background
        var img = new Image();
        img.src = '../images/canvas_pattern_0.jpg';
        canvas.ctx.globalCompositeOperation="destination-over";
        var ptrn = canvas.ctx.createPattern(img,'repeat');
        canvas.ctx.fillStyle = ptrn;
        canvas.ctx.fillRect(0,0,canvas.w,canvas.h);
    
        var dataURL = canvas.canvas.toDataURL("image/png");
    
        // upload the image
        var blob = canvas.dataURItoBlob(dataURL);
        var name = "canvas-"+starUtils.s4()+".png";
        if($("#canvas-name").val().length > 0)
            name = $("#canvas-name").val()+".png";
        blob.name = name;
        blob.lastModifiedDate = new Date();
        blob.lastModified = blob.lastModifiedDate.getTime();
        var files = [];
        files.push(blob)
        var params = "";
        var temp = star.utils.uuid();
        params = "temp="+temp;
        star.utils.uploadFiles('/fileupload?'+star.token+'&'+params, files, function(json){
            var response = JSON.parse(json);
            
            // save comment
            var data = {};
            data.uuid = star.room;
            data.type = "file";
            data.objectType = "event";
            data.comment = "canvas.jpg";
            data.tempId = temp;
            
            canvas.erase();
    
            starServices.addComment(data, function(){
                var d = {};
                socket_message_broadcast("files-reload", d);
                star.loadComments(star.params, star.dashboard);
            });        
        });
    }
    
    canvas.filesReload = function(){
        star.loadComments(star.params, star.dashboard);
        canvas.commentsOpen();
    }
    
    
    canvas.dataURItoBlob = function(dataURI) {
        dataURI = dataURI.split(',');
        var type = dataURI[0].split(':')[1].split(';')[0],
            byteString = atob(dataURI[1]),
            byteStringLength = byteString.length,
            arrayBuffer = new ArrayBuffer(byteStringLength),
            intArray = new Uint8Array(arrayBuffer);
        for (var i = 0; i < byteStringLength; i++) {
            intArray[i] = byteString.charCodeAt(i);
        }
        return new Blob([intArray], {
            type: type
        });
    }
    
    
    canvas.findxy = function(res, e) {
        if(e.type == "touchmove"){
            canvas.prevX = canvas.currX;
            canvas.prevY = canvas.currY;
            canvas.currX = e.changedTouches[0].clientX - $(canvas.canvas).offset().left;
            canvas.currY = e.changedTouches[0].clientY - $(canvas.canvas).offset().top;
            if(isNaN(canvas.prevX)){
                canvas.prevX = canvas.currX;
                canvas.prevY = canvas.currY;
            }
        } else {
            canvas.prevX = canvas.currX;
            canvas.prevY = canvas.currY;
            canvas.currX = e.clientX - $(canvas.canvas).offset().left;
            canvas.currY = e.clientY - $(canvas.canvas).offset().top;
        }
    
        if (res == 'down') {
            canvas.flag = true;
            canvas.dot_flag = true;
            if (canvas.dot_flag) {
                canvas.ctx.beginPath();
                canvas.ctx.moveTo(canvas.currX-1, canvas.currY);
                canvas.ctx.lineTo(canvas.currX, canvas.currY);
                canvas.ctx.lineCap="round";
                canvas.ctx.strokeStyle = canvas.x;
                canvas.ctx.lineWidth = canvas.y;
                canvas.ctx.stroke();
                canvas.ctx.closePath();
                
                var data = {};
                data.type = canvas.type;
                data.prevX = canvas.currX-1;
                data.prevY = canvas.currY;
                data.currX = canvas.currX;
                data.currY = canvas.currY;
                socket_message_broadcast("canvas-draw", data);
            }
        }
        if (res == 'up' || res == "out") {
            canvas.flag = false;
        }
        if (res == 'move') {
            if (canvas.flag) {
                canvas.draw();
            }
        }
    }
    
    canvas.share = function(elm){
        var data = {};
        data.width = canvas.w;
        data.height = canvas.h;
        data.image = canvas.canvas.toDataURL();
        socket_message_broadcast("canvas-resize", data);
        $(elm).addClass("btn-primary");
    }
    
    canvas.close = function(){
        star.drawing = false;
        $(".canvas-container").hide();
        $("#canvas-open").addClass("btn-dark");
        $("#canvas-open").removeClass("btn-success");    
    }
    
    canvas.commentsClose = function(){
        star.commentsDialog = false;
        $(".comments-container").hide();
        $("#comments-open").addClass("btn-dark");
        $("#comments-open").removeClass("btn-success");    
    }

    canvas.commentsOpen = function(){
        star.commentsDialog = true;
        $(".comments-container").show();
        $("#comments-open").removeClass("btn-dark");
        $("#comments-open").addClass("btn-success");    
    }
    
    $(document).ready(function(){
        document.addEventListener("touchstart", canvas.touchHandler, true);
        document.addEventListener("touchmove", canvas.touchHandler, true);
        document.addEventListener("touchend", canvas.touchHandler, true);
        document.addEventListener("touchcancel", canvas.touchHandler, true);        
        
        $("#canvas-open").click(function(e){
            $(".draggable").css("z-index", "1000");
            $(".canvas-container").css("z-index", "1001");
            
            if(star.drawing == false){
                star.drawing = true;
                $("#canvas-open").removeClass("btn-dark");
                $("#canvas-open").addClass("btn-success");
                $(".canvas-container").show();
            } else {
                star.drawing = false;
                $(".canvas-container").hide();
                $("#canvas-open").addClass("btn-dark");
                $("#canvas-open").removeClass("btn-success");             
            }
        });
    
        $("#comments-open").click(function(e){
            $(".draggable").css("z-index", "1000");
            $(".comments-container").css("z-index", "1001");
            
            if(star.commentsDialog == false){
                star.commentsDialog = true;
                $("#comments-open").removeClass("btn-dark");
                $("#comments-open").addClass("btn-success");
                $(".comments-container").show();
            } else {
                star.commentsDialog = false;
                $(".comments-container").hide();
                $("#comments-open").addClass("btn-dark");
                $("#comments-open").removeClass("btn-success");             
            }
        });
        
        $(".comments-container").css('position','fixed');
        $(".comments-container").css("left", "30px");
        $(".comments-container").css("top", "30px");
        $(".comments-resize-2").resizable({
            minHeight: 300,
            minWidth: 340
        });
        
        // dragging
        $(".draggable").draggable({handle:".drag-slider"});
        $(".draggable").mousedown(function(){
            $(".draggable").css("z-index", "1000");
            $(this).css("z-index", "1001");
        });
        
        $(".canvas-container").css('position','fixed');
        $(".canvas-container").css("left", "10px");
        $(".canvas-container").css("top", "10px");
        $(".canvas-container").show();
        $(".canvas-paper").resizable({
            minHeight: 340,
            minWidth: 460,
            stop: function(event, ui) {
                  canvas.w = ui.size.width;
                  canvas.h = ui.size.height;
                  canvas.resize();
            }
        });

        canvas.init(); 
        $(".canvas-container").hide();


    });
    
    socket.on('socket_message', function(data) {
        if(data.event == "canvas-draw"){
            canvas.drawMessage(data.data);
        }     
        if(data.event == "canvas-undo"){
            canvas.undoMessage(data.data);
        }     
        if(data.event == "canvas-resize"){
            canvas.resizeMessage(data.data);
        }     
        if(data.event == "canvas-erase"){
            canvas.eraseMessage();
        }     
        if(data.event == "canvas-save"){
        }     
        if(data.event == "files-reload"){
            canvas.filesReload();
        }     
     });
    
    
    canvas.touchHandler = function(event) {
        var touch = event.changedTouches[0];
        
        var simulatedEvent = document.createEvent("MouseEvent");
            simulatedEvent.initMouseEvent({
            touchstart: "mousedown",
            touchmove: "mousemove",
            touchend: "mouseup"
        }[event.type], true, true, window, 1,
            touch.screenX, touch.screenY,
            touch.clientX, touch.clientY, false,
            false, false, false, 0, null);
    
        touch.target.dispatchEvent(simulatedEvent);
    }
}





//
//
// Embedded Chat 
//
//
star.chatroomusers = [];
if(webrtc == null){
    var usr = {};
    usr.room = star.chatRoom;
    usr.user = star.user;
    usr.userAvatar = star.userAvatar;
    usr.admin = star.isOwner;
    usr.userUuid = star.userUuid;
    if(!star.isOwner)
        star.chatRoomRecipientUser = star.ownerName;
    else
        $(".chat-message-form").hide();
    socket.emit('chatroom_joined', usr);    
    
    $(document).ready(function(){
        $(".open-chat").click(function() {
            if ($(".style-switcher.closed").length>0) 
                $('.style-switcher .trigger').click();
        });

        $("#widgr-open-chat").click(function() {
            $(".style-switcher-container .trigger").click();
        });
        
        function chatSend2(){
            var data = {};
            data.room = star.chatRoom;
            data.message = $("#chat-text2").val();
            data.client = getSessionId();
            data.sender = getRecipient(star.user).id;
            data.senderUuid = star.userUuid;
            data.senderName = star.user;
            data.senderAvatar = star.userAvatar;
            data.recipient = getRecipient(star.chatRoomRecipientUser).id;
            data.recipientUuid = getRecipient(star.chatRoomRecipientUser).userUuid;
            data.recipientName = star.chatRoomRecipientUser;
            data.recipientAvatar = getRecipient(star.chatRoomRecipientUser).userAvatar;
            socket.emit('chatRoom-message', data);
            
            $("#chat-text2").val("");
            $("#chat-text2").focus();
            return false;
        }
        
        function getRecipient(name){
            for(var i = 0; i < star.chatroomusers.length; i++){
                if(star.chatroomusers[i].user == name){
                    return star.chatroomusers[i];
                }
            }
            return null;
        };
        
        $(document).on("click", "#chat-send2", function(){
            chatSend2();
        });

        $(document).on("keyup", "#chat-text2", function(e){
            if(e.keyCode == 13)
                chatSend2();
        });
        
        $(".chatroom-user-clear").click(function(){
            $(this).hide();
            $(".chatroom-user-select").html("");
            star.chatRoomRecipient = null;
            star.chatRoomRecipientUser = null;
        });

        $("#create-instant-room").click(function(){
            var user = getRecipient(star.chatRoomRecipientUser);
            if(user.userUuid.length > 0 && star.listing.length > 0)
            var params = "id="+star.listing+"&uuid="+user.userUuid+"&"+star.token;
            roomServices.instantRoom(params, function(data){
                console.log(data);
                $("#chat-text2").val(data.url);
                chatSend2();
            });
        });
        
        $(".style-switcher-container").on("click", ".chatroom-user", function(event){
            if(star.isOwner){
                var id = $(this).attr("data-id");
                var user = $(this).attr("data-name");
                var avatar = $(this).attr("data-avatar");
                var uuid = $(this).attr("data-uuid");
                star.chatRoomRecipient = id;
                star.chatRoomRecipientUser = user;
                star.chatRoomRecipientAvatar = avatar;
                $(".chat-message-form").show();
                $(".chat-user-label").html("<img src='/"+avatar+"_32x32' class='avatar16 img-circle'> " +  user);
                $(".user-containers").hide();
                var container = $(".user-containers[data-usr*='"+uuid+"']");
                container.show();
            }
        });     
        
        socket.on('chatroom_update', function(data) {
            var isAdminOnline = false;
            star.chatroomusers = JSON.parse(data)[star.chatRoom];
            
            var html = '';
            for(var i = 0; i < star.chatroomusers.length; i++){
                if(star.chatroomusers[i].admin == true){
                    isAdminOnline = true;
                }
                if(star.chatroomusers[i].user != star.user){
                    html += '<li><a href="#" data-id="'+star.chatroomusers[i].client+'" data-uuid="'+star.chatroomusers[i].userUuid+'" data-avatar="'+star.chatroomusers[i].userAvatar+'" data-name="'+star.chatroomusers[i].user+'" class="black-link chatroom-user"><img class="img-circle avatar16" style="margin:1px;" src="/'+star.chatroomusers[i].userAvatar+'_32x32"> ' + star.chatroomusers[i].user+ '</a></li>';
                }
            }
            if(isAdminOnline)
                $(".style-switcher").show();
            else
                $(".style-switcher").hide();
            if(star.isOwner){
                $(".chat-avatars2").html(html);
            }
            $(".users-count").html(star.chatroomusers.length-1);
        });
        
        socket.on('chatroom_disconnect', function(data) {
            if(star.isOwner){
                if(star.chatRoomRecipientUser == data.user){
                    $(".chat-user-label").html(i18n("select-user"));
                    star.chatRoomRecipientUser = null;
                    var usr = getRecipient(data.user);
                    $(".chat-message-form").hide();
                    
                    var container = $(".user-containers[data-usr*='"+usr.userUuid+"']");
                    var html = '<div>';
                    html += '<div style="padding:5px; margin:5px;"> ' + usr.user + " " + i18n("left-conversation");
                    html += '</div>';
                    html += '</div>';
                    container.append(html);                    

                    // scrolling
                    $("#content2")[0].scrollTop =  $("#content2")[0].scrollHeight;
                }
                
            }
        });
        
        socket.on('chatRoom-message-render', function(data) {
            var topContent = $("#content2");
            $(".user-containers").hide();
            var uuid = data.recipientUuid == star.userUuid ? data.senderUuid : data.recipientUuid;
            var name = data.recipientName == star.user ? data.senderName : data.recipientName;
            var avatar = data.recipientAvatar == star.userAvatar ? data.senderAvatar : data.recipientAvatar;
            var container = $(".user-containers[data-usr*='"+uuid+"']");
            var style = data.senderName != star.user ? "widgr-bubble-right" : "widgr-bub";
            var now = new Date();
            var time = starUtils.formatTime2(now);
            
            if(container[0] == undefined)
                topContent.append("<div class='user-containers' data-usr='"+uuid+"_"+star.userUuid+"'></div>");
            container = $(".user-containers[data-usr*='"+uuid+"']");
            var html = '<div>';
            html += '<span style="line-height:32px; font-size:13px; color:gray" ><img class="img-circle avatar32" style="vertical-align:middle" src="/'+data.senderAvatar+'_32x32"> ' + data.senderName + '<span style="float:right;margin-right:10px">'+time+"</span></span>";
            html += '<div class="'+style+'">';
            html += linkify(data.message.replace(/>/g, '&gt;'));
            html += '</div>';
            html += '</div>';
            container.append(html);
            container.show();

            // switch the user selector
            if(star.chatRoomRecipientUser != name){
                star.chatRoomRecipientUser = name;
                $(".chat-user-label").html("<img src='/"+avatar+"_32x32' class='avatar16 img-circle'> " +  name);
            }
            $(".chat-message-form").show();
            
            // scrolling and notification
            $("#content2")[0].scrollTop =  $("#content2")[0].scrollHeight;
            if ($(".style-switcher.closed").length>0) 
                $('.style-switcher .trigger').click();
            var audio = new Audio('/public/images/ring.mp3');
            audio.play();
        });        
    });
}




//
//
// Hangout Chat 
//
//
if(webrtc != null){
    socket.on('message', function(data) {
        if (data.message) {
            var html = '';
            if(data.username != undefined)
                html += '<strong>' + data.username.replace(/>/g, '&gt;') + '</strong>: ';
            var message = linkify(data.message);
            html += message + '<br/>';
            $("#content").append(html);
            var elm = $("#chat-container");
            if(star.chatMimized){
                elm.animate({right:'0px'},100);
                star.chatMimized = false;
            }
        }
        $("#content").scrollTop[0] = content.scrollHeight;
    });
    
    
    $(document).ready(function(){
        
        // init room feeds
        roomServices.getFeeds("event="+star.room, function(data){
            var html = "";
            for(var i = data.length-1; i >= 0; i--){
                var date = new Date(data[i].created).toLocaleString();
                html += "<b title='"+date+"' >"+data[i].name+": </b>"+linkify(data[i].comment)+"<br/>";
            }
            $("#content").html(html);
        });        
        
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
        
        $("#chat-send").click(function() {
            var comment = $("#chat-text").val();
            send_chat(comment, $("#chat-name").val());
            $("#chat-text").val("");
            var data = {};
            data.uuid = star.room;
            data.name = star.user;
            data.comment = comment;
            roomServices.saveFeed(data, function(){
            });
            return false;
        });
    });
}


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

roomServices.instantRoom = function(params, success, error){
    var data = {};
    $.ajax({
        type: "POST",
        url: "/instant-room-rest?"+params,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });    
};





//
//
// helper functions
//
//

function usersRefresh(){
    socket.emit("user_refresh", {});
}


function getPeerId(){
    //return socket.socket.sessionid;
    return webrtc.connection.socket.sessionid;
}


function getSessionId(){
    return socket.socket.sessionid;
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



(function ($) {
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
