if(!star.utils.detectWebrtc().support){
    $(".widgr-compatibilty-warning").show();
}

var socket = io.connect(star.server_host);
var content = document.getElementById("content");
var callSound = null;
var title = "";
var caller = null;
var users = [];
var t;
var myStream = null;
var recordAudio, recordVideo;
var room = star.room;

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
webrtc = new SimpleWebRTC({
    localVideoEl: 'localVideo',
    autoRequestMedia: true,
    url: star.server_host,
    debug: false,
    detectSpeakingEvents: true,
    autoAdjustMic: false
});

var usr = {};
usr.user = star.user;
usr.room = star.room;
usr.avatar = star.userAvatar;
usr.avatar = star.avatar;
usr.admin = star.isOwner;
usr.login = star.login;
usr.uuid = star.userUuid;


$('#joinModal').modal({show:true});

// video call buttons handlers
$(document).ready(function(){
    
    // stop instant broadcast
    $(".btn-instant-stop").click(function(){
        star.send_chat(' stopped broadcasting', star.user);
        var data = {};
        data.id = socket.socket.sessionid;
        star.socket_message_broadcast("instant-room-stop-broadcast", data);
        window.location = $(this).attr("data-href");
    });

    // start instat room
    $(".no-schedule-start").click(function(e){
        
    });

    // go to private room
    $(".btn-instant-room").click(function(){
        star.send_chat(' went to private room', star.user);
        
        var data = {};
        data.id = socket.socket.sessionid;
        star.socket_message_broadcast("instant-room-private-broadcast", data);
        window.location = $(this).attr("data-href");        
    });

    // start instant broadcast
    $(".btn-instant-start").click(function(){
        var data = {};
        data.id = socket.socket.sessionid;
        star.socket_message_broadcast("instant-room-start-broadcast", data);
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

webrtc.on('readyToCall', function () {

     $(document).click(function(){
         $(".peer-controls").hide();
     });

     // camera on off
     $("#controls-camera").click(function(){
         if(!star.cameraoOff){
             star.cameraoOff = true;
             webrtc.pauseVideo();
             $(this).removeClass("btn-dark");
             $(this).addClass("btn-danger");
             $(".peer-label-camera[data-id='"+getPeerId()+"']").show();
             $(".videoContainer", "#video-element-"+getPeerId()).hide();
             $(".videoContainer", "#video-element-"+getPeerId()).addClass("hidden");
             $("#"+getPeerId()+"_video_small").addClass("displayed");
             $("#"+getPeerId()+"_video_small").show();
         } else {
             star.cameraoOff = false;
             webrtc.resumeVideo();
             $(this).removeClass("btn-danger");
             $(this).addClass("btn-dark");
             $(".peer-label-camera[data-id='"+getPeerId()+"']").hide();
             $(".videoContainer", "#video-element-"+getPeerId()).removeClass("hidden");
             $("#"+getPeerId()+"_video_small").removeClass("displayed");
             if(star.maximizedId != getPeerId())
                 $(".videoContainer", "#video-element-"+getPeerId()).show();
             if(star.maximizedId != getPeerId())
                 $("#"+getPeerId()+"_video_small").hide();
             
         }
         
         var data = {};
         data.id = getPeerId();
         data.cameraoOff = star.cameraoOff;
         star.socket_message_broadcast("camera-broadcast", data);
     });
     
     $("#controls-camera-ready").click(function(){
         if($(this).hasClass("btn-dark")){
             $(this).removeClass("btn-dark");
             $(this).addClass("btn-danger");
         } else {
             $(this).addClass("btn-dark");
             $(this).removeClass("btn-danger");
         }
         $("#controls-camera").click();
     });     

     // maximize mimize peer
     $(document).on("click", ".user-elm", function(){
         var id = $(this).attr("data-id");
         var local = $(this).attr("data-local");
         if(star.selectedId == null || star.selectedId != id){
             star.selectedId = id;
             star.switchAuto = false;
             star.maximizeMimize(id, local);
             $(this).attr("data-reverse", "false");
         } else {
             star.selectedId = null;
             star.switchAuto = true;
             star.maximizeMimize(id, local);
             $(this).attr("data-reverse", "true");
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
         star.socket_message_broadcast("mute-broadcast", data);
     });
     
     $("#controls-mute-ready").click(function(){
         if($(this).hasClass("btn-dark")){
             $(this).removeClass("btn-dark");
             $(this).addClass("btn-danger");
             $(this).html("<i class='icon-mute'></i>");         
         } else {
             $(this).addClass("btn-dark");
             $(this).removeClass("btn-danger");
             $(this).html("<i class='icon-sound'></i>");         
         }
         $("#controls-mute").click();
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
         star.socket_message_broadcast("mute-broadcast", data);
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
         star.socket_message_broadcast("mute-broadcast", data);
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
         star.socket_message_all("controls-hangup", data);
         star.send_chat(' has left the conversation', star.user);

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
                     star.socket_message_broadcast("screenshare-broadcast", data);
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
             star.socket_message_broadcast("screenshare-broadcast", data);
         }
     });         

     $('.webrtc-ready').show();
     $("#remotes").show();
     if(star.user == undefined)
         $("#input-name").show();

     $('#btn-call-start').click(function(){
         if(star.user == undefined && $("#input-name").val() == "" ){
             $("#input-name").focus();
             return;
         } 
         if(star.user == undefined && $("#input-name").val() != "" ){
             star.user = $("#input-name").val();
             usr.user = $("#input-name").val();
         }
             
         $(".ready-join-session").remove();
         $(".control-buttons").show();
         joinRoom(room, joinRoomCallback);        
         
         // show invite modal when there is only one user
         var stopInterval = setInterval(function(){
             if($(".user-elm").length <= 1)
                 $('#myModal').modal({show:true});
             clearInterval(stopInterval);
         }, 5000);   
         
         if(typeof star.listingUuid != "undefined")
             start();
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
    
    var currentUser = {};
    currentUser.peer = id;
    currentUser.avatar = star.userAvatar;
    currentUser.user = i18n("you");
    currentUser.login = star.userUuid;
    star.singleUserRender(currentUser);
    $(".user-elm").click();
    $(".user-elm").click();
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
            star.socket_message_broadcast("speaking", data);
        }
    }
});
    
    


star.usersRender = function(data) {
    var userList = JSON.parse(data);
    var html = "";
    users = [];
    star.users = userList;
    for(var i = 0; i < userList.length; i++) {
        users.push(userList[i]);
        var peerId = userList[i].peer;
        var currentUser = userList[i];
        star.singleUserRender(currentUser);
    }
}

star.singleUserRender = function(currentUser){
    html = "";
    // dropdown + peer controls
    html += "<div class='video-dropdown' data-id='"+currentUser.peer+"'><i class='fa fa-chevron-down color-link-light'></i></div>"
    html += "<div class='peer-controls' data-id='"+currentUser.peer+"' style='display:none;z-index:9999;opacity:0.8'>";
    if(typeof currentUser.avatar != "undefined"){
        html += "<a class='btn margin-clear btn-short btn-dark avatar-mute-btn btn-peer' href='/user/"+currentUser.login+"' target='_blank' >"
            html += "<i class='icon-user'></i> "+i18n("view-profile");
        html += "</a> ";
    }
    html += "<button class='peer-mute btn margin-clear btn-short btn-dark avatar-mute-btn btn-peer' data-type='audio' data-name='" + currentUser.user + "' data-id='"+currentUser.peer+"'>"
    html += "   <i class='icon-mute'></i> "+i18n("mute");
    html += "</button> ";
    html += "</div>";

    html += "<div id='user-item-"+currentUser.peer+"' data-id='"+currentUser.peer+"' style='position:relative;' title='"+currentUser.user+"'>";
    
    // peer labels
    html += "<div style='position:absolute;top:5px;left:5px;z-index:9999;'>";
    html += "   <div style='display:none' class='peer-label-screenshare peer-control-lbl btn-success' data-id='"+currentUser.peer+"'><i class='fa fa-desktop'></i></div>"
    html += "   <div "+(!currentUser.muted?"style='display:none'":"")+" class='peer-label-muted peer-control-lbl btn-danger' data-id='"+currentUser.peer+"'><i class='icon-mute'></i></div>"
    html += "   <div "+(!currentUser.cameraOff?"style='display:none'":"")+" class='peer-label-camera peer-control-lbl btn-danger' data-id='"+currentUser.peer+"'><i class='fa fa-eye-slash'></i></div>"
    html += "</div>";
               
    // peer avatar
    html += "<div id='"+ currentUser.peer +"_video_small' class='peer-avatar'>";
    html += "   <div class='peer-avatar-label'>" + (currentUser.usr != undefined ? currentUser.usr.name : currentUser.user);
    if(currentUser.avatar != undefined && currentUser.avatar != null)
        html += "       <br/><img src='"+currentUser.avatar+"_32x32' class='img-circle'>";
    html += "   </div>";
    html += "</div>";
    html += "</div>";
    
    function closure(elm, html) {
        $("#video-element-"+elm).waitUntilExists(function(){
            $("#video-element-"+elm).prepend(html);
        });
        
        setTimeout(function(){ 
            if(currentUser.cameraOff){
                $(".peer-label-camera[data-id='"+elm+"']").show();
                if(star.maximizedId != elm)
                    $(".videoContainer", "#video-element-"+elm).hide();
                if(star.maximizedId != elm)
                    $("#"+elm+"_video_small").show();
                $("#"+elm+"_video_small").addClass("displayed");
                $("#container_"+elm+"_video_incoming").addClass("hidden");
            }       
        }, 1000); 
    };
    closure(currentUser.peer, html);    
}

// mimize maximized video
star.maximizeMimize = function(id, local){

    if(star.maximizedId != null){
        var moveTo = $(".videoContainer", "#video-element-"+star.maximizedId);
        $(".videoContainer", "#video-element-"+star.maximizedId).show();
        
        $(star.maximized).css("position", "inherit");
        $(star.maximized).appendTo(moveTo);
        if(star.maximized != null)
            star.maximized.play();
        
        $("#video-element-"+star.maximizedId).css("border", "1px solid rgba(0,0,0,0.0)");
        $("#video-element-"+id).removeClass("user-elm-selected");
        
        if(!$("#"+star.maximizedId+"_video_small").hasClass("displayed"))
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
        $(star.maximized).appendTo(".maximized-container");
        $(".videoContainer", "#video-element-"+star.maximizedId).hide();
        $("#"+star.maximizedId+"_video_small").show();
        $(".container", "#video-element-"+star.maximizedId).hide();
        if(star.selectedId != null){
            $("#video-element-"+id).css("border", "1px solid rgba(255,255,255,0.9)");
            $("#video-element-"+id).addClass("user-elm-selected");
        }
        if(star.maximized != null)
            star.maximized.play();
        
        // for screen
        if(star.maximizedScreen != null){
            $(star.maximizedScreen).appendTo(".maximized-container");
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
                    star.maximizeMimize(data.data.id, false);
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
            $("#"+data.data.id+"_video_small").addClass("displayed");
            $("#container_"+data.data.id+"_video_incoming").addClass("hidden");
        } else {
            $(".peer-label-camera[data-id='"+data.data.id+"']").hide();
            if(star.maximizedId != data.data.id)
                $(".videoContainer", "#video-element-"+data.data.id).show();
            if(star.maximizedId != data.data.id)
                $("#"+data.data.id+"_video_small").hide();
            $("#"+data.data.id+"_video_small").removeClass("displayed");
            $("#container_"+data.data.id+"_video_incoming").removeClass("hidden");
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
    star.usersRender(data);
});
    





//
//
// universal socket handlers
//
//

star.socket_message_all = function(event, data) {
    socket.emit('socket_message_all', {
        "user" : star.user,
        "room" : star.room,
        "event" : event,
        "client" : socket.socket.sessionid,
        "data" : data
    });
};

star.socket_message_broadcast = function(event, data) {
    socket.emit('socket_message_broadcast', {
        "user" : star.user,
        "room" : star.room,
        "event" : event,
        "client" : socket.socket.sessionid,
        "data" : data
    });
};

star.socket_message_broadcast_to = function(event, to, data) {
    socket.emit('socket_message_broadcast_to', {
        "user" : star.user,
        "room" : star.room,
        "event" : event,
        "to" : to,
        "client" : socket.socket.sessionid,
        "data" : data
    });
};

star.send_chat = function(message, usr){
    if(usr == undefined){
        socket.emit('send', { "message": message, "room" : star.room });  
    }
    else {
        socket.emit('send', { "message": message, "username": usr, "room" : star.room});  
    }
};





//
//
// Video call Chat 
//
//
    
star.teamMessageSend = function(){
    if($("#chat-text3").val() == null || $("#chat-text3").val().length == 0)
        return;
    var data = {};
    data.type = "message";
    data.room = star.chatRoom;
    data.message = $("#chat-text3").val();
    data.client = star.getSessionId();
    data.sender = star.userUuid;
    data.senderUuid = star.userUuid;
    data.senderName = star.userName;
    data.senderAvatar = star.userAvatar;
    socket.emit('hangout-send', data); 

    var saveFeed = {};
    saveFeed.uuid = star.chatRoom;
    saveFeed.sender = data.senderUuid;
    saveFeed.senderName = data.senderName;
    saveFeed.comment = data.message;
    roomServices.saveFeed(saveFeed, function(){
    });           
    $("#chat-text3").val("");
    $("#chat-text3").focus();    
};

star.teamFeedsLoad = function(event, elm) {
    var btn = $(this);
    var from = parseInt(btn.attr("data-from"));
    var max = 5;    
    var btnContainer = btn.parent();
    var container = btn.parent().parent();
    var params = "&from="+from+"&max="+max+"&id="+star.chatRoom;    
    roomServices.getFeeds(params, function(data){
        star.feedsRender(data, btnContainer);
    });
    btn.attr("data-from", from + max); 
}

star.feedsRender = function(data, btnContainer, clbck){
    html = "";
    for(var i = data.length - 1; i >= 0; i--){
        time = starUtils.formatDate(new Date(data[i].created)) + " " + starUtils.formatTime2(new Date(data[i].created));
        html += '<span style="line-height:32px; font-size:13px; color:gray" ><a target="_blank" href="'+star.baseUrl+"/user/id/"+data[i].sender+'">' + data[i].senderName + '</a> <span style="float:right;margin-right:10px">'+ time +"</span></span>";
        if(star.userUuid == data[i].sender){
            html += '<div class="widgr-bubble-left">';
            html +=  data[i].comment;
            html += '</div>';
        } else {
            html += '<div class="widgr-bubble-right">';
            html +=  data[i].comment;
            html += '</div>';
        }
    }
    btnContainer.after(html);
    if(star.scrollDown){
        $("#widgr-chat-content")[0].scrollTop =  $("#widgr-chat-content")[0].scrollHeight;  
        star.scrollDown = false;
    }
    if(!star.isOwner && data.length > 0){
        $(".widgr-chat-content").show();
        $(".user-containers").show();
    }
}

star.teamMessageRender = function(data) {
    if(!star.chatOpen){
        $("#chat-open").click();
    }
    
    var now = new Date();
    var time = starUtils.formatDate(now) + " " + starUtils.formatTime2(now);
    var style = data.senderName != star.userName ? "widgr-bubble-right" : "widgr-bubble-left";
    var html = '';
    html += '<span style="line-height:32px; font-size:13px; color:gray" ><img class="img-circle avatar32 margin10" style="vertical-align:middle" src="'+star.baseUrl+"/"+data.senderAvatar+'_32x32"><a target="_blank" href="'+star.baseUrl+"/user/id/"+data.senderUuid+'">' + data.senderName + '</a> <span style="float:right;margin-right:10px">'+time+"</span></span>";
    html += '<div class="'+style+'" >';
    html += linkify(data.message.replace(/>/g, '&gt;'));
    html += '</div>';
    if(!$(".team-switch").is(":visible"))
        $(".widgr-msg-team").fadeOut("slow").fadeIn("slow");
    $(".widgr-chat-content-team").append(html);
    $(".widgr-chat-content-team")[0].scrollTop =  $(".widgr-chat-content-team")[0].scrollHeight;  
}

star.getSessionId = function(){
    return socket.socket.sessionid;
}

star.chatOpen = false;
$(document).ready(function(){
    $("#chat-open").click(function(){
        star.chatOpen = !star.chatOpen;
        if(star.chatOpen){
            $(this).addClass("btn-success");
            $(this).removeClass("btn-dark");
            $(".right-panel").show();
        } else {
            $(".right-panel").hide();
            $(this).removeClass("btn-success");
            $(this).addClass("btn-dark");
        }
    });

    $("#chat-close").click(function(){
        $("#chat-open").click();
    });
    
    // render team message
    socket.on('hangout-message', star.teamMessageRender);     
    
    // load team history
    $(document).on("click", ".team-feeds-load", star.teamFeedsLoad);    

    $(document).on("click", "#chat-send3", function(){
        star.teamMessageSend();
    });
    
    $(document).on("keyup", "#chat-text3", function(e){
        if(e.keyCode == 13)
            star.teamMessageSend();
    });        
    
    $(".team-feeds-load").click();
});




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

function peers(){
    return webrtc.webrtc.peers;
}

function joinRoomCallback(err, r){
    usr.peer = getPeerId();
    usr.client = getPeerId();
    usr.muted = star.muted;
    usr.cameraOff = star.cameraoOff;
    socket.emit('user_joined', usr);
    
//    setTimeout(function(){ 
//        var data = {};
//        data.id = getPeerId();
//        data.muted = star.muted;
//        star.socket_message_broadcast("mute-broadcast", data);
//        
//        data = {};
//        data.id = getPeerId();
//        data.cameraoOff = star.cameraoOff;
//        star.socket_message_broadcast("camera-broadcast", data);    
//    }, 2000);    
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

var clsStopwatch = function() {
    // Private vars
    var startAt = 0;    // Time of last start / resume. (0 if not running)
    var lapTime = 0;    // Time on the clock when last stopped in milliseconds

    var now = function() {
            return (new Date()).getTime(); 
        }; 

    // Public methods
    // Start or resume
    this.start = function() {
            startAt = startAt ? startAt : now();
        };

    // Stop or pause
    this.stop = function() {
            // If running, update elapsed time otherwise keep it
            lapTime = startAt ? lapTime + now() - startAt : lapTime;
            startAt = 0; // Paused
        };

    // Reset
    this.reset = function() {
            lapTime = startAt = 0;
        };

    // Duration
    this.time = function() {
            return lapTime + (startAt ? now() - startAt : 0); 
        };
};

var x = new clsStopwatch();
var $time;
var clocktimer;

function pad(num, size) {
    var s = "0000" + num;
    return s.substr(s.length - size);
}

function formatTime(time) {
    var h = m = s = ms = 0;
    var newTime = '';
    h = Math.floor( time / (60 * 60 * 1000) );
    time = time % (60 * 60 * 1000);
    m = Math.floor( time / (60 * 1000) );
    time = time % (60 * 1000);
    s = Math.floor( time / 1000 );
    ms = time % 1000;
    newTime = pad(h, 2) + ':' + pad(m, 2) + ':' + pad(s, 2);
    return newTime;
}

$time = document.getElementById('time');
if(typeof star.listingUuid != "undefined")
    update();

function update() {
    $time.innerHTML = formatTime(x.time());
}

function start() {
    clocktimer = setInterval("update()", 1000);
    x.start();
}

function stop() {
    x.stop();
    clearInterval(clocktimer);
}

