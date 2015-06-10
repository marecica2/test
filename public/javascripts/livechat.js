var socket = io.connect(star.server_host);

if(false){
    var star = {};
}  
star.chatfirst = true;
star.adminMessage = false;
star.chatRoomUsers = [];
star.chatRoomRecipientUser = null;
star.admins = 0;
star.users = 0;




//
// Users
//

star.instantRoom = function(){
    var user = star.getRecipientByUuid(star.chatRoomRecipientUser);
    if(user.userUuid.length > 0 && star.listing.length > 0)
    var params = "id="+star.listing+"&uuid="+user.userUuid+"&"+star.token;
    if($(this).attr("data-paid") == "false"){
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
        
        var saveFeed = {};
        saveFeed.type = "save-feed";
        saveFeed.uuid = star.chatRoom;
        saveFeed.listing = star.listing;
        saveFeed.sender = star.userUuid;
        saveFeed.senderName = star.userName;
        saveFeed.recipient = star.chatRoomRecipientUser;
        saveFeed.recipientName = star.getRecipientByUuid(star.chatRoomRecipientUser).userName;
        //saveFeed.comment = '<span style="font-size:18px;"><a href="'+resp.url+'" target="_blank"><i class="fa fa-phone"></i> '+i18n("join-vide-conference")+'</a>';
        saveFeed.comment = resp.url;
        if(star.embedded){
            star.container[0].contentWindow.postMessage(JSON.stringify(saveFeed), '*');
        } else {
            roomServices.saveFeed(saveFeed, function(){
            });                 
        }
    });
}

star.userSwitch = function(){
    $(".team-switch").hide();
    $(".user-switch").show();
    
    star.adminMessage = false;
    var id = $(this).attr("data-id");
    var userName = $(this).attr("data-name");
    var userAvatar = $(this).attr("data-avatar");
    var userUuid = $(this).attr("data-uuid");
    var lbl = $(this).attr("data-lbl");
    
    var usr = star.getRecipientByUuid(userUuid);
    if(star.userUuid != userUuid)
        star.chatRoomRecipientUser = userUuid;
    star.listing = usr.listingUuid;
    
    $(".widgr-msg-"+userUuid).hide();
    $(".chat-message-form").show();
    $(".chat-user-label").html('<a href="/user/id/'+userUuid+'" target="_blank"><img class="img-circle avatar32 margin10" src="/'+userAvatar+'_64x64">' + userName + '</a> <small>'+lbl+'</small>');
    $(".user-containers").hide();
    var first = false;
    var topContent = $("#widgr-chat-content");
    var container = $(".user-containers[data-usr*='"+userUuid+"']");
    if(container[0] == undefined){
        var first = true;
        topContent.append("<div class='user-containers' data-recipient='"+userUuid+"' data-sender='"+star.userUuid+"' data-usr='"+userUuid+"_"+star.userUuid+"' style=''><p style='text-align:center'><a class='load-feeds' style='cursor:pointer;font-weight:bold' data-from='0' >"+i18n("previous-messages")+"</a></p></div>");
        firstMessage = true;
    }
    container = $(".user-containers[data-usr*='"+userUuid+"']");
    container.show();   
        

    // load feeds for the first time
    if(first){
        star.scrollDown = true;
        $(".load-feeds:visible").click();
    }
    $("#widgr-chat-content")[0].scrollTop =  $("#widgr-chat-content")[0].scrollHeight + 500;
}

star.teamSwitch = function(){
    $(".team-switch").show();
    $(".user-switch").hide();
    $(".widgr-msg-team").fadeOut("slow");
    $(".widgr-chat-content-team")[0].scrollTop =  $(".widgr-chat-content-team")[0].scrollHeight + 500;      
}

star.usersRender = function(data) {
    var isAdminOnline = false;
    var usrs = JSON.parse(data);
    if(usrs == null)
        return;
    
    star.chatRoomUsers = usrs;
    star.admins = 0;
    star.users = 0;
    
    var html = '';
    html += "<li class='list-item-gray team-chat-select'><span style='line-height:38px;'>&nbsp; <i class='fa fa-headphones'></i> &nbsp;Team Chat</span><br/><small style='color:silver'>Chat with your team</small> <small style='display:none' class='label label-danger widgr-msg-team'>"+i18n("new-message")+"</small></li>"
    for(var i = 0; i < star.chatRoomUsers.length; i++){
        if(star.chatRoomUsers[i].admin == true){
            isAdminOnline = true;
            if(star.chatRoomUsers[i].userUuid != star.userUuid)
                star.admins += 1;
        } else {
            star.users += 1;
        }
        if(star.chatRoomUsers[i].userUuid != star.userUuid){
            var lbl = "<a target='_blank' href='/listing/"+star.chatRoomUsers[i].listingUuid+"'>"+star.chatRoomUsers[i].listingTitle+"</a>";
            if(star.chatRoomUsers[i].admin)
                lbl = "<span class='label default-bg'>"+i18n("operator")+"</span>";
            html += '<li  data-lbl="'+lbl+'" data-id="'+star.chatRoomUsers[i].client+'" data-uuid="'+star.chatRoomUsers[i].userUuid+'" data-avatar="'+star.chatRoomUsers[i].userAvatar+'" data-name="'+star.chatRoomUsers[i].userName+'" class="chatroom-user list-item-gray" ><a><img class="img-circle avatar22 margin5" src="/'+star.chatRoomUsers[i].userAvatar+'_32x32">' + star.chatRoomUsers[i].userName + '</a> <br/><small style="color:silver">' + lbl + '</small> <small style="display:none" class="label label-danger widgr-msg-'+star.chatRoomUsers[i].userUuid+'">'+i18n("new-message")+'</small></li>';
        }
    }
    if(star.isOwner)
        $(".chat-avatars2").html(html);
    if(isAdminOnline){
        $(".widgr-instant-session-btn").show();
        $(".widgr-send-msg-button").hide();
        $(".widgr-chat").show();
        $(".widgr-email").hide();
        $(".widgr-online-status").show();
        $(".widgr-header-title").html(star.chatboxTitle);
    }
    else {
        $(".widgr-instant-session-btn").hide();
        $(".widgr-send-msg-button").show();
        $(".widgr-online-status").hide();
        $(".widgr-chat").hide();
        $(".widgr-email").show();
        $(".widgr-header-title").html(i18n("offline-title"));
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
            $("#widgr-chat-content")[0].scrollTop =  $("#widgr-chat-content")[0].scrollHeight + 500;
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
    
    $(".widgr-start-chat-container").hide();
    $(".widgr-chat-input").show();
    setTimeout(function(){
        $("#chat-text2")[0].focus();
    }, 100);
}

star.teamMessageSend = function(){
    if($("#chat-text3").val() == null || $("#chat-text3").val().length == 0)
        return;
    var data = {};
    data.type = "message";
    data.room = star.chatRoom;
    data.message = $("#chat-text3").val();
    data.client = star.getSessionId();
    data.sender = star.getRecipientByUuid(star.userUuid).id;
    data.senderUuid = star.userUuid;
    data.senderName = star.userName;
    data.senderAvatar = star.userAvatar;
    socket.emit('chatRoom-team-message', data);    
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

star.teamMessageRender = function(data) {
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
    $(".widgr-chat-content-team")[0].scrollTop =  $(".widgr-chat-content-team")[0].scrollHeight + 500;  
    
    var audio = new Audio(star.baseUrl+'/public/images/ring.mp3');
    audio.play();    
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
        
        if(star.chatRoomRecipientUser == data.recipientUuid)
            $(".widgr-msg-"+data.recipientUuid).fadeOut("slow");
        $("#chat-text2").val("");
        $("#chat-text2").focus();
        
        var saveFeed = {};
        saveFeed.type = "save-feed";
        saveFeed.uuid = star.chatRoom;
        saveFeed.listing = star.listing;
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
    var now = new Date();
    var time = starUtils.formatDate(now) + " " + starUtils.formatTime2(now);
    var firstMessage = false;
    var style = data.senderName != star.userName ? "widgr-bubble-right" : "widgr-bubble-left";
    var userUuid = typeof data.recipientUuid == "undefined" || data.recipientUuid == star.userUuid ? data.senderUuid : data.recipientUuid;
    var userName = typeof data.recipientUuid == "undefined" || data.recipientUuid == star.userUuid ? data.senderName : data.recipientName;
    var avatar = typeof data.recipientUuid == "undefined" || data.recipientUuid == star.userUuid ? data.senderAvatar : data.recipientAvatar;
    var topContent = $("#widgr-chat-content");
    var container;

    // init Basic message container 
    if(star.isOwner){
        container = $(".user-containers[data-usr*='"+userUuid+"']");
        if(container[0] == undefined){
            topContent.append("<div class='user-containers' data-recipient='"+data.recipientUuid+"' data-sender='"+data.senderUuid+"' data-usr='"+userUuid+"_"+star.userUuid+"' style='display:none'><p style='text-align:center'><a class='load-feeds' style='cursor:pointer;font-weight:bold' data-from='0' >"+i18n("previous-messages")+"</a></p></div>");
            firstMessage = true;
        }
        container = $(".user-containers[data-usr*='"+userUuid+"']");
    } else {
        container = $(".user-containers[data-usr='"+star.userUuid+"']");
    }
    
    // header
    var html = '';
    html += '<span style="line-height:32px; font-size:13px; color:gray" ><img class="img-circle avatar32 margin10" style="vertical-align:middle" src="'+star.baseUrl+"/"+data.senderAvatar+'_32x32"><a target="_blank" href="'+star.baseUrl+"/user/id/"+data.senderUuid+'">' + data.senderName + '</a> <span style="float:right;margin-right:10px">'+time+"</span></span>";
    
    // render message content
    html += '<div class="'+style+'" >';
    if(data.type == "room"){
        html += '<span style="font-size:18px;"><a href="'+data.message+'&tempName='+star.userName+'" target="_blank"><i class="fa fa-comments-o"></i> '+i18n("join-vide-conference")+'</a>';
    }
    if(data.type == "message"){
        html += linkify(data.message.replace(/>/g, '&gt;'));
    }
    html += '</div>';
    
    // display first message immediately
    if(star.chatfirst && !star.isOwner && star.userUuid == data.senderUuid){
        html += '<br/>';
        html += '<div class="widgr-bubble-right">';
        html +=  i18n("operator-available");
        html += '</div>';
    }
    star.chatfirst = false;
    container.append(html);

    if(!container.is(":visible"))
        $(".widgr-msg-"+userUuid).fadeOut("slow").fadeIn("slow");

    // auto switch the user selector
    if(!$("#auto-switch-off").is(":checked")){
        $(".user-containers").hide();
        container.show();
        if(star.chatRoomRecipientUser != userUuid){
            if(star.userUuid != userUuid)
                star.chatRoomRecipientUser = userUuid;
            var usr = star.getRecipientByUuid(userUuid);
            var lbl = "<a target='_blank' href='/listing/"+usr.listingUuid+"'>"+usr.listingTitle+"</a>";
            star.listing = usr.listingUuid;
            if(usr.admin)
                lbl = "<span class='label default-bg'>"+i18n("operator")+"</span>";
            $(".chat-user-label").html("<img src='/"+avatar+"_32x32' class='avatar32 margin10 img-circle'><a href='/user/id"+userUuid+"' target='_blank'>" +  userName + "</a> <small>" + lbl + "</small>");
        }
    }  
    
    // auto open embedded chat box
    $(".widgr-chatbox").addClass("opened");
    $(".widgr-chatbox-content").show();
    $(".widgr-chat-content").show();
    star.visible = false;
    
    $("#widgr-chat-content")[0].scrollTop =  $("#widgr-chat-content")[0].scrollHeight + 500;
    if ($(".widgr-chatbox.closed").length>0) 
        $('.widgr-chatbox .trigger').click();
    var audio = new Audio(star.baseUrl+'/public/images/ring.mp3');
    audio.play();
}

star.feedsLoad = function(event, elm) {
    var btn = $(this);
    if(typeof elm != "undefined")
        btn = elm;
    
    var from = parseInt(btn.attr("data-from"));
    var max = 30;
    var btnContainer = btn.parent();
    var container = btn.parent().parent();
    var params = "&from="+from+"&max="+max;

    if(star.isOwner){
        var sender = container.attr("data-sender");
        if(typeof sender != "undefined")
            params += "&sender="+star.chatRoomRecipientUser;
    } else {
        params += "&sender="+star.userUuid;
        params += "&listing="+star.listing;
    }

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

star.feedsRender = function(data, btnContainer, clbck){
    html = "";
    for(var i = data.length - 1; i >= 0; i--){
        time = starUtils.formatDate(new Date(data[i].created)) + " " + starUtils.formatTime2(new Date(data[i].created));
        html += '<span style="line-height:32px; font-size:13px; color:gray" ><a target="_blank" href="'+star.baseUrl+"/user/id/"+data[i].sender+'">' + data[i].senderName + '</a> <span style="float:right;margin-right:10px">'+ time +"</span></span>";
        if(star.userUuid == data[i].sender){
            html += '<div class="widgr-bubble-left">';
            html +=  linkify(data[i].comment);
            html += '</div>';
        } else {
            html += '<div class="widgr-bubble-right">';
            html +=  linkify(data[i].comment);
            html += '</div>';
        }
    }
    btnContainer.after(html);
    if(star.scrollDown){
        $("#widgr-chat-content")[0].scrollTop =  $("#widgr-chat-content")[0].scrollHeight + 500;  
        star.scrollDown = false;
    }
    if(!star.isOwner && data.length > 0){
        $(".widgr-chat-content").show();
        $(".user-containers").show();
    }
}

star.sendMail = function(){
    var valid = true;
    var msg = {};
    msg.type = "msg";
    msg.sender = $(".widgr-email-sender").val();
    msg.subject = $(".widgr-email-subject").val();
    msg.body = $(".widgr-email-body").val();
    if(!msg.sender && !star.logged)
        valid = false;
    if(!msg.subject)
        valid = false;
    if(!msg.body)
        valid = false;
    var re = /^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/i;
    if(!re.test(msg.sender) && !star.logged)
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
        var html = "<p>"+i18n("message-sent")+"</p>";
        html += "<p>"+msg.subject+"<br/>";
        html += msg.body+"</p>";
        $(".widgr-email").html(html);
        $(".widgr-email-validation").hide();
    } else {
        $(".widgr-email-validation").show();
    }
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
        if ($(".widgr-chatbox.closed").length>0) 
            $('.widgr-chatbox .trigger').click();
    });

    $("#widgr-open-chat").click(function() {
        $(".widgr-chatbox-container .trigger").click();
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

    
    // render team message
    socket.on('chatRoom-team-message-render', star.teamMessageRender);     
    
    // load team history
    $(document).on("click", ".team-feeds-load", star.teamFeedsLoad);    

    $(document).on("click", "#chat-send3", function(){
        star.teamMessageSend();
    });
    
    $(document).on("keyup", "#chat-text3", function(e){
        if(e.keyCode == 13)
            star.teamMessageSend();
    });
    
    
    // clear user on disconnect
    $(".chatroom-user-clear").click(function(){
        $(this).hide();
        $(".chatroom-user-select").html("");
    });

    // create instant room
    $(".create-instant-room").click(star.instantRoom);
    
    // select user
    $(".widgr-chatbox-container").on("click", ".chatroom-user", star.userSwitch);     

    // select user
    $(".widgr-chatbox-container").on("click", ".team-chat-select", star.teamSwitch);     

    // users update
    socket.on('chatroom_update', star.usersRender);
    
    // user disconnect
    socket.on('chatroom_disconnect', star.userDisconnect);
    
    // render message
    socket.on('chatRoom-message-render', star.messageRender); 
    
    // load feeds history
    $(document).on("click", ".load-feeds", star.feedsLoad);

    // send email message
    $(document).on("click", ".widgr-send-msg-btn", star.sendMail);                     
    
    // start chat btn
    $(document).on("click", ".widgr-startchat-btn", star.startChat);                     

    // chat open
    $(document).on("click", ".widgr-iframe-btn", function(){
        $(".widgr-chat").hide();
        $(".widgr-chat").after('<iframe class="widgr-chat-iframe widgr-embedded-iframe" style="width:100%" src="'+star.baseUrl+'/registration" seamless frameBorder="0"></iframe>');
    });                    

    // open close chatbox
    $(document).on("click", ".widgr-chatbox-trigger", function(){
        if(star.visible){
            $(".widgr-chatbox-content").show();
            $(".widgr-chatbox").addClass("opened");
            star.visible = false;
        } else {
            $(".widgr-chatbox-content").hide();
            $(".widgr-chatbox").removeClass("opened");
            star.visible = true;
        }
    });   

    $(".widgr-instant-session-btn").click(function(){
        $(".widgr-chatbox-trigger").click();
    });   
    
    if(star.logged)
        $(".widgr-chat-input").show();
    
    if(!star.isOwner){
        star.feedsLoad(null, $(".load-feeds"));
    }
    
    if(star.isOwner){
        $(".team-feeds-load").click();
    }
    
    if(!star.utils.detectWebrtc().support){
        $(".widgr-compatibilty-warning").show();
    }
});

star.getSessionId = function(){
    return socket.socket.sessionid;
}
