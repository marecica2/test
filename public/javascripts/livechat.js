var socket = io.connect(star.server_host);

if(false){
    var star = {};
}  
star.chatfirst = true;
star.chatRoomUsers = [];
star.chatRoomRecipientUser = null;
star.admins = 0;
star.users = 0;

$(document).ready(function(){
    var usr = {};
    usr.room = star.chatRoom;
    usr.userName = star.userName;
    usr.userAvatar = star.userAvatar;
    usr.admin = star.isOwner;
    usr.userUuid = star.userUuid;
    usr.listingUuid = star.listingUuid;
    usr.listingTitle = star.listingTitle;
    socket.emit('chatroom_joined', usr);  
    
    $(".open-chat").click(function() {
        if ($(".style-switcher.closed").length>0) 
            $('.style-switcher .trigger').click();
    });

    $("#widgr-open-chat").click(function() {
        $(".style-switcher-container .trigger").click();
    });
    
    socket.on('chatRoom-getUsers-resp', function(data) {
        star.chatRoomUsers = JSON.parse(data);
        star.messageSend();
    });
    
    $(document).on("click", "#chat-send2", function(){
        star.messageSend();
    });

    $(document).on("keyup", "#chat-text2", function(e){
        if(e.keyCode == 13)
            star.messageSend();
    });
    
    $(".chatroom-user-clear").click(function(){
        $(this).hide();
        $(".chatroom-user-select").html("");
    });

    // create instant room
    $("#create-instant-room").click(star.instantRoom);
    
    // select user
    $(".style-switcher-container").on("click", ".chatroom-user", star.userSwitch);     

    // users update
    socket.on('chatroom_update', star.usersRender);
    
    // user disconnect
    socket.on('chatroom_disconnect', star.userDisconnect);
    
    // render message
    socket.on('chatRoom-message-render', star.messageRender); 
    
    // load history
    $(document).on("click", ".load-feeds", star.feedsLoad);
    
    // send email message
    $(document).on("click", ".widgr-send-msg-btn", star.sendMail);                     
    
    // start chat btn
    $(document).on("click", ".widgr-startchat-btn", star.startChat);                     

    // chat open
    $(document).on("click", ".widgr-iframe-btn", function(){
        $(".widgr-chat-noiframe").hide();
        $(".widgr-chat-iframe").show();
    });                    
    
    $(document).on("click", ".style-switcher-trigger", function(){
        if(star.visible){
            $(".style-switcher-content").show();
            $(".style-switcher").addClass("opened");
            star.visible = false;
        } else {
            $(".style-switcher-content").hide();
            $(".style-switcher").removeClass("opened");
            star.visible = true;
        }
    });   
    
    if(star.logged)
        $(".widgr-chat-input").show();
});

star.getSessionId = function(){
    return socket.socket.sessionid;
}




//
// Users
//

star.instantRoom = function(){
    var user = star.getRecipientByUuid(star.chatRoomRecipientUser);
    if(user.userUuid.length > 0 && star.listing.length > 0)
    var params = "id="+star.listing+"&uuid="+user.userUuid+"&"+star.token;
    if($("#create-instant-room-charging").is(":checked")){
        params += "&free=true";
    }
    roomServices.instantRoom(params, function(resp){
        var data = {};
        data.type = "room";
        data.room = star.chatRoom;
        data.message = resp.url;
        data.client = star.getSessionId();
        data.sender = star.getRecipientByUuid(star.userUuid).id;
        data.senderUuid = star.userUuid;
        data.senderName = star.userName;
        data.senderAvatar = star.userAvatar;
        data.recipient = star.getRecipientByUuid(star.chatRoomRecipientUser).id;
        data.recipientUuid = star.chatRoomRecipientUser;
        data.recipientName = star.getRecipientByUuid(star.chatRoomRecipientUser).userName;
        data.recipientAvatar = star.getRecipientByUuid(star.chatRoomRecipientUser).userAvatar;
        socket.emit('chatRoom-message', data);                
        star.messageSend();
    });
}

star.userSwitch = function(event){
    if(star.isOwner){
        var id = $(this).attr("data-id");
        var userName = $(this).attr("data-name");
        var userAvatar = $(this).attr("data-avatar");
        var userUuid = $(this).attr("data-uuid");
        var lbl = $(this).attr("data-lbl");
        
        if($(this).attr("data-team")){
            $(".chat-user-label").html('<span style="line-height:52px"><i class="fa fa-headphones fa-2x"></i> Team chat</span>');
            $(".user-containers").hide();
            star.chatRoomRecipientUser = null;
            
        } else {
            var usr = star.getRecipientByUuid(userUuid);
            if(star.userUuid != userUuid)
                star.chatRoomRecipientUser = userUuid;
            star.listing = usr.listingUuid;
            
            $(".widgr-msg-"+userUuid).hide();
            $(".chat-message-form").show();
            $(".chat-user-label").html('<a href="/user/id/'+userUuid+'" target="_blank"><img class="img-circle avatar32 margin10" src="/'+userAvatar+'_64x64">' + userName + '</a> <small>'+lbl+'</small>');
            $(".user-containers").hide();
            
            var topContent = $("#content2");
            var container = $(".user-containers[data-usr*='"+userUuid+"']");
            if(container[0] == undefined){
                topContent.append("<div class='user-containers' data-recipient='"+userUuid+"' data-sender='"+star.userUuid+"' data-usr='"+userUuid+"_"+star.userUuid+"' style=''><p style='text-align:center'><a class='load-feeds' style='cursor:pointer;font-weight:bold' data-from='0' >Older chats</a></p></div>");
                firstMessage = true;
            }
            container = $(".user-containers[data-usr*='"+userUuid+"']");
            container.show();               
        }
    }
}

star.usersRender = function(data) {
    var isAdminOnline = false;
    star.chatRoomUsers = JSON.parse(data);
    var html = '';
    html += '<li data-team="true" data-lbl="Team chat" class="chatroom-user list-item-gray" data-lbl="Team chat"><span style="margin:6px;"><i class="fa fa-headphones"></i></span><a style="cursor:pointer;font-weight:bold;line-height:36px">Team chat</a></li>';
    for(var i = 0; i < star.chatRoomUsers.length; i++){
        if(star.chatRoomUsers[i].admin == true){
            isAdminOnline = true;
            if(star.chatRoomUsers[i].userUuid != star.userUuid)
                star.admins += 1;
        } else {
            star.users += 1;
        }
        if(star.chatRoomUsers[i].userUuid != star.userUuid){
            var lbl = star.chatRoomUsers[i].listingTitle;
            if(star.chatRoomUsers[i].admin)
                lbl = "<span class='label default-bg'>Operator</span>";
            html += '<li  data-lbl="'+lbl+'" data-id="'+star.chatRoomUsers[i].client+'" data-uuid="'+star.chatRoomUsers[i].userUuid+'" data-avatar="'+star.chatRoomUsers[i].userAvatar+'" data-name="'+star.chatRoomUsers[i].userName+'" class="chatroom-user list-item-gray" ><a style="cursor:pointer;font-weight:bold"><img class="img-circle avatar22 margin5" src="/'+star.chatRoomUsers[i].userAvatar+'_32x32">' + star.chatRoomUsers[i].userName + '</a> <small>' + lbl + '</small> <small style="display:none" class="label label-danger widgr-msg-'+star.chatRoomUsers[i].userUuid+'">New message</small></li>';
        }
    }
    if(star.isOwner)
        $(".chat-avatars2").html(html);
    if(isAdminOnline){
        $(".widgr-chat").show();
        $(".widgr-email").hide();
    }
    else {
        $(".widgr-chat").hide();
        $(".widgr-email").show();
    }
    $(".users-count").html(star.users);
    $(".admins-count").html(star.admins);
}

star.userDisconnect = function(data) {
    if(star.isOwner && data != null){
        if(star.chatRoomRecipientUser == data.userUuid){
            var usr = star.getRecipientByUuid(data.userUuid);
            $(".chat-message-form").hide();
            
            var container = $(".user-containers[data-usr*='"+usr.userUuid+"']");
            var html = '<div>';
            html += '<div style="padding:5px; margin:5px;"> ' + usr.userName + " " + i18n("left-conversation");
            html += '</div>';
            html += '</div>';
            container.append(html);                    

            // scrolling
            $("#content2")[0].scrollTop =  $("#content2")[0].scrollHeight;
        }
    }
}




//
// Chat messages
//

star.startChat = function(){
    star.userName = $(".widgr-custom-name").val();
    star.utils.setCookieMinutes("widgr-name", $(".widgr-custom-name").val(), 60);
    star.utils.setCookieMinutes("widgr-user-uuid", star.userUuid, 60);
    
    var usr = {};
    usr.room = star.chatRoom;
    usr.userName = star.userName;
    usr.userUuid = star.userUuid;
    usr.userAvatar = star.userAvatar;
    usr.admin = star.isOwner;
    usr.listingUuid = star.listingUuid;
    usr.listingTitle = star.listingTitle;
    socket.emit('chatroom_reconnect', usr);  
    
    $(".widgr-custom-name").hide();
    $(".widgr-chat-input").show();
    $(".widgr-startchat-btn").hide();
    setTimeout(function(){
        $("#chat-text2")[0].focus();
    }, 100);
}

star.messageSend = function(){
    if(typeof star.chatRoomUsers == "undefined" || star.chatRoomUsers.length == 0){
        var data = {};
        socket.emit('chatRoom-getUsers', data);
    } else {
        
        if($("#chat-text2").val() == null || $("#chat-text2").val().length == 0)
            return; 
        
        var data = {};
        data.type = "message";
        data.room = star.chatRoom;
        data.message = $("#chat-text2").val();
        data.client = star.getSessionId();
        data.sender = star.getRecipientByUuid(star.userUuid).id;
        data.senderUuid = star.userUuid;
        data.senderName = star.userName;
        data.senderAvatar = star.userAvatar;
        if(star.chatRoomRecipientUser != null){
            data.recipient = star.getRecipientByUuid(star.chatRoomRecipientUser).id;
            data.recipientUuid = star.chatRoomRecipientUser;
            data.recipientName = star.getRecipientByUuid(star.chatRoomRecipientUser).userName;
            data.recipientAvatar = star.getRecipientByUuid(star.chatRoomRecipientUser).userAvatar;
            data.chatfirst = star.chatfirst;
        }
        socket.emit('chatRoom-message', data);
        
        if(star.chatRoomRecipientUser == data.recipientUuid){
            $(".widgr-msg-"+data.recipientUuid).fadeOut("slow");
        }  
        
        $("#chat-text2").val("");
        $("#chat-text2").focus();
        
        // save feed
        var saveFeed = {};
        saveFeed.type = "save-feed";
        saveFeed.uuid = star.chatRoom;
        saveFeed.sender = data.senderUuid;
        saveFeed.senderName = data.senderName;
        saveFeed.recipient = data.recipientUuid;
        saveFeed.recipientName = data.recipientName;
        saveFeed.comment = data.message;
        if(star.embedded){
            star.container[0].contentWindow.postMessage(JSON.stringify(saveFeed), '*');
        } else {
            roomServices.saveFeed(saveFeed, function(){
            });                 
        }
    }
    return false;
}

star.messageRender = function(data) {
    var userUuid = typeof data.recipientUuid == "undefined" || data.recipientUuid == star.userUuid ? data.senderUuid : data.recipientUuid;
    var userName = typeof data.recipientUuid == "undefined" || data.recipientUuid == star.userUuid ? data.senderName : data.recipientName;
    var avatar = typeof data.recipientUuid == "undefined" || data.recipientUuid == star.userUuid ? data.senderAvatar : data.recipientAvatar;
    var style = data.senderName != star.userName ? "widgr-bubble-right" : "widgr-bubble-left";
    var now = new Date();
    var time = starUtils.formatDate(now) + " " + starUtils.formatTime2(now);
    var firstMessage = false;

    var topContent = $("#content2");
    var container = $(".user-containers[data-usr*='"+userUuid+"']");
    if(container[0] == undefined){
        topContent.append("<div class='user-containers' data-recipient='"+data.recipientUuid+"' data-sender='"+data.senderUuid+"' data-usr='"+userUuid+"_"+star.userUuid+"' style='display:none'><p style='text-align:center'><a class='load-feeds' style='cursor:pointer;font-weight:bold' data-from='0' >Older chats</a></p></div>");
        firstMessage = true;
    }
    container = $(".user-containers[data-usr*='"+userUuid+"']");

    
    // header
    var html = '';
    html += '<span style="line-height:32px; font-size:13px; color:gray" ><img class="img-circle avatar32 margin10" style="vertical-align:middle" src="'+star.baseUrl+"/"+data.senderAvatar+'_32x32"><a target="_blank" href="'+star.baseUrl+"/user/id/"+data.senderUuid+'">' + data.senderName + '</a> <span style="float:right;margin-right:10px">'+time+"</span></span>";
    
    // content
    html += '<div class="'+style+'" >';
    if(data.type == "room"){
        html += '<span style="font-size:18px;"><i class="fa fa-phone"></i> <a href="'+data.message+'&tempName='+star.userName+'" target="_blank">'+i18n("join-vide-conference")+'</a>';
    }
    if(data.type == "message"){
        html += linkify(data.message.replace(/>/g, '&gt;'));
    }
    html += '</div>';
    
    // notification label
    if(star.userUuid != userUuid && star.chatRoomRecipientUser != userUuid){
        $(".widgr-msg-"+userUuid).fadeOut("slow").fadeIn("slow");
    }
    
    // first message
    if(star.chatfirst && star.isOwner == false && star.userUuid == data.senderUuid && firstMessage){
        star.chatfirst = false;
        html += '<br/>';
        html += '<div class="widgr-bubble-right">';
        html +=  "Operator will available for you in few seconds. Please wait.";
        html += '</div>';
    }
    container.append(html);

    // switch the user selector
    if(!$("#auto-switch-off").is(":checked")){
        $(".user-containers").hide();
        container.show();
        if(star.chatRoomRecipientUser != userUuid){
            if(star.userUuid != userUuid)
                star.chatRoomRecipientUser = userUuid;
            var usr = star.getRecipientByUuid(userUuid);
            var lbl = usr.listingTitle;
            star.listing = usr.listingUuid;
            if(usr.admin)
                lbl = "<span class='label default-bg'>Operator</span>";
            $(".chat-user-label").html("<img src='/"+avatar+"_32x32' class='avatar32 margin10 img-circle'><a href='/user/id"+userUuid+"' target='_blank'>" +  userName + "</a> <small>" + lbl + "</small>");
        }
        $(".chat-message-form").show();
    }
    
    // auto open embedded chat box
    $(".style-switcher").addClass("opened");
    $(".style-switcher-content").show();
    $(".widgr-chat-content").show();
    star.visible = false;
    
    // scrolling and notification
    $("#content2")[0].scrollTop =  $("#content2")[0].scrollHeight;
    if ($(".style-switcher.closed").length>0) 
        $('.style-switcher .trigger').click();
    var audio = new Audio(star.baseUrl+'/public/images/ring.mp3');
    audio.play();
}

star.sendMail = function(){
    var valid = true;
    var msg = {};
    msg.type = "msg";
    msg.sender = $(".widgr-email-sender").val();
    msg.subject = $(".widgr-email-subject").val();
    msg.body = $(".widgr-email-body").val();
    if(!msg.sender)
        valid = false;
    if(!msg.subject)
        valid = false;
    if(!msg.body)
        valid = false;
    var re = /^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/i;
    if(!re.test(msg.sender))
        valid = false;
    
    if(valid){
        if(star.container != undefined)
            star.container[0].contentWindow.postMessage(JSON.stringify(msg), '*');
        else {
            if(star.logged)
                msg.user = star.userUuid;
            msg.recipient = star.ownerUuid;
            starServices.sendMessage(msg);
        }
        var html = "<p>Your message has been sent</p>";
        html += "<p>"+msg.subject+"<br/>";
        html += msg.body+"</p>";
        $(".widgr-email").html(html);
        $(".widgr-email-validation").hide();
    } else {
        $(".widgr-email-validation").show();
    }
}





//
// Feeds
//

star.feedsLoad = function() {
    var btn = $(this);
    var from = parseInt(btn.attr("data-from"));
    var max = 10;
    var btnContainer = $(this).parent();
    var container = $(this).parent().parent();
    var params = "";

    var sender = container.attr("data-sender");
    if(typeof sender != "undefined")
        params += "&sender="+sender;
    
    var recipient = container.attr("data-recipient");
    if(typeof recipient != "undefined")
        params += "&recipient="+recipient;
    params += "&from="+from+"&max="+max;
    
    if(star.embedded){
        var req = {};
        req.type = "get-feeds";
        req.params = params;
        star.container[0].contentWindow.postMessage(JSON.stringify(req), '*');
    } else {
        roomServices.getFeeds(params, function(data){
            star.feedsRender(data, btnContainer);
        });
    }
    btn.attr("data-from", from + max); 
}

star.feedsRender = function(data, btnContainer){
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
}

star.getRecipientByUuid = function(uuid){
    for(var i = 0; i < star.chatRoomUsers.length; i++){
        if(star.chatRoomUsers[i].userUuid == uuid){
            return star.chatRoomUsers[i];
        }
    }
    if(!star.isOwner){
        for(var i = 0; i < star.chatRoomUsers.length; i++){
            if(star.chatRoomUsers[i].admin == true){
                star.chatRoomRecipientUser = star.chatRoomUsers[i].userUuid;
                return star.chatRoomUsers[i];
            }
        }
    }
    return null;
};


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
