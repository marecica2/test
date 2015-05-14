var star = {};
star.loaded = false;
star.visible = true;
star.eventMethod = window.addEventListener ? "addEventListener" : "attachEvent";
star.messageEvent = star.eventMethod == "attachEvent" ? "onmessage" : "message";
eventer = window[star.eventMethod];

eventer(star.messageEvent,function(e) {
    var key = e.message ? "message" : "data";
    var params = e.data;
    params = JSON.parse(params)

    // init params
    if(params.type == "init"){
        star.baseUrl = params["data-bs"];
        star.sHost = params["data-ws"]; 
        star.chatRoom = params["data-room"]; 

        star.listingTitle = params["data-listingTitle"]; 
        star.listingImage = params["data-listingImage"]; 
        star.listingCharging = params["data-listingCharging"]; 
        star.listingPrice = params["data-listingPrice"]; 
        star.listingDuration = params["data-listingDuration"]; 
        star.listingCurrency = params["data-listingCurrency"]; 
        star.listingFirstFree = params["data-listingFirstFree"]; 
        
        star.ownerName = params["data-owner"]; 
        star.ownerAvatar = params["data-owner-avatar"]; 
        star.ownerUuid = params["data-owner-uuid"]; 
        star.ownerCompany = params["data-owner-company"]; 
        star.isOwner = false;
        star.logged = false;
        star.userAvatar = "public/images/avatar";
        star.userUuid = star.uuid();
        star.userName = "Guest"+ Math.floor(Math.random()*900);
        if(params["data-lg-user"] != undefined){
            star.logged = true;
            star.userUuid = params["data-lg-user-ui"]; 
            star.userName = params["data-lg-user"]; 
            star.userAvatar = params["data-lg-user-ava"]; 
            star.userLogin = params["data-lg-user-lg"];
        }    
        star.embedInit();  
    }
    if(params.type == "chat-open"){
        star.chatOpen();
    }
    if(params.type == "resize"){
        star.resize(params);
    }
    if(params.type == "reload"){
        document.location.reload();
    }
},false);


star.embedInit = function(){
    star.container = $(".widgr-embed-container");
    if(star.loaded)
        return;
    
    star.container[0].contentWindow.postMessage("widgr-frame-ready", '*');
    $('head').append('<link href="https://fonts.googleapis.com/css?family=Open+Sans:400italic,700italic,400,700,300&amp;subset=latin,latin-ext" rel="stylesheet" type="text/css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/fonts/embedded.css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/stylesheets/embed.css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/stylesheets/embed-red.css">');

    $.getScript(star.sHost+"/socket.io/socket.io.js", function() {
        $.getScript(star.baseUrl+"/public/javascripts/utils.js", function(){
            star.loaded = true;
            
            // add chat box if not exist
            if($(".style-switcher-container")[0] == undefined){
                $('body').append(star.chatContent(star));
                
                var widgr_int2 = setInterval(function(){
                        $("#widgr-container").show();
                        clearInterval(widgr_int2);
                    }, 1000);                    
            }
  
            // read user name from cookie
            var name = star.utils.getCookie("widgr-name");
            if(!star.logged && name != ""){
                star.userName = name;
                star.userUuid = star.utils.getCookie("widgr-user-uuid");
                $(".widgr-custom-name").val(star.userName);
                $(".widgr-custom-name").hide();
                $(".widgr-chat-input").show();
                $(".widgr-startchat-btn").hide();
            }
            
            $(document).on("click", ".widgr-iframe-btn", function(){
                $(".widgr-chat-noiframe").hide();
                $(".widgr-chat-iframe").show();
            });                    

            $.getScript(star.baseUrl+"/public/javascripts/room.js", function(){
                
                // send msg
                $(document).on("click", ".widgr-send-msg-btn", function(){
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
                        star.container[0].contentWindow.postMessage(JSON.stringify(msg), '*');
                        var html = "<p>Your message has been sent</p>";
                        html += "<p>"+msg.subject+"<br/>";
                        html += msg.body+"</p>";
                        $(".widgr-email").html(html);
                        $(".widgr-email-validation").hide();
                    } else {
                        $(".widgr-email-validation").show();
                    }
                });                     

                // start chat btn
                $(document).on("click", ".widgr-startchat-btn", function(){
                    star.userName = $(".widgr-custom-name").val();
                    star.utils.setCookieMinutes("widgr-name", $(".widgr-custom-name").val(), 15);
                    star.utils.setCookieMinutes("widgr-user-uuid", star.userUuid, 15);
                    
                    var usr = {};
                    usr.room = star.chatRoom;
                    usr.userName = star.userName;
                    usr.userUuid = star.userUuid;
                    usr.userAvatar = star.userAvatar;
                    usr.admin = star.isOwner;
                    socket.emit('chatroom_reconnect', usr);  
                    
                    $(".widgr-custom-name").hide();
                    $(".widgr-chat-input").show();
                    $(".widgr-startchat-btn").hide();
                });                     
                
                if(star.logged){
                    $(".widgr-chat-input").show();
                }
                
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
                
            });
        
        });    
    });
}

star.chatOpen = function(){
    $(".style-switcher-trigger").click();
};

star.resize = function(data){
    if(data != undefined){
        $(data.selector).height(data.height);
    }
}

star.chatContent = function(star){
    var html = '';
    
    html += '<div id="widgr-container" style="display:none">';
    html += '<div class="style-switcher shadow">';
    
    html += '   <div class="header style-switcher-trigger" style="text-align:center">';
    html += '       <a class="trigger" href="#chat"><i class="fa fa-comments-o"></i></a>';
    html += '       <strong style="margin-left:55px;margin-right:15px;">'+i18n("chat-with")+' '+'</strong> <small style="opacity:0;float:right;font-size:10px;margin-right:5px;color:rgba(255,255,255,0.9)" class="blink widgr-available">'+i18n('available')+'</small>';
    html += '   </div>';
    
    html += '   <div class="style-switcher-content" style="padding:15px;display:none">';
    //html += '   <div class="shadow-inset" style="background:url(\''+star.baseUrl + "/" + star.image+'\'); background-size:cover;height:100px;width:170px;float:left;vertical-align:middle"></div>'
    html += '   <div class="widgr-chat-noiframe" style="margin-bottom:20px;"><img style="float:left; margin-right: 10px; height:50px;" class="img-circle" src="'+star.baseUrl+"/"+star.ownerAvatar+'_64x64"><a target="_blank" href="'+star.baseUrl+"/user/id/"+star.ownerUuid+'">'+star.ownerName+'</a><br/>'+star.ownerCompany+'</div>';
    
    html += '   <iframe class="widgr-chat-iframe widgr-embedded-iframe" style="margin:5px 0px;min-width:100%;display:none" src="'+star.baseUrl+'/login" seamless frameBorder="0"></iframe>';
    
    html += '   <div class="widgr-chat-noiframe">';
    html += '       <div class="widgr-chat">';
    html += '           <div id="content2" class="chat-window widgr-chat-input widgr-chat-content" style="display:none"></div>';
    
    if(!star.logged){
        html += '       <div class="widgr-chat-noiframe">';
        html += '           <table class="widgr-start-chat-container" style="width:100%; border-collapse:collapse">';
        html += '               <tr>';
        html += '                   <td style="padding:0px;width:100%"><input type="text" class="form-control left-radius widgr-custom-name" maxlength="40" style="width:100%;" placeholder="'+i18n('your-name')+'"></td>';
        html += '                   <td style="padding:0px;"><button class="widgr-startchat-btn btn btn-default right-radius" style="width:100px;height:36px;">'+i18n('start-chat')+'</button></td>';
        html += '               </tr>';
        html += '           </table>';
        html += '       </div>';
    }    
    
    html += '           <table class="widgr-chat-input" style="display:none; width:100%; border-collapse:collapse;">';
    html += '               <tr>';
    html += '                   <td style="width:100%;padding:0px;"><input id="chat-text2" class="form-control left-radius" maxlength="400" style="width:100%" placeholder="'+i18n('message')+'"></td>';
    html += '                   <td style="padding:0px;"><button id="chat-send2" class="btn btn-default btn-short right-radius" style="width:40px;height:36px;"><i class="fa fa-share fa-flip-horizontal"></i></button></td>';
    html += '               </tr>';
    html += '           </table>';
    
    html += '       </div>';
    
    html += '       <div class="widgr-email">';
    html += '           <span class="vertical-padding">'+star.ownerName+ " "+i18n('not-available-now')+'</span><br/><br/>';
    html += '           <p class="widgr-email-validation" style="color:red;display:none">Please correct your input</p>'
    if(!star.logged){
        html += '       <input id="chat-text2" class="form-control radius vertical-padding widgr-email-sender" maxlength="50" style="width:100%" placeholder="'+i18n('your-email')+'">';
    }
    html += '           <input id="chat-text2" class="form-control radius vertical-padding widgr-email-subject" maxlength="100" style="width:100%" placeholder="'+i18n('subject')+'">';
    html += '           <textarea id="chat-text2" class="form-control radius vertical-padding widgr-email-body" maxlength="400" style="width:100%;height:100px;" placeholder="'+i18n('message')+'"></textarea>';
    html += '           <button class="btn btn-default radius widgr-send-msg-btn" style="width:100%;height:35px;"><i class="fa fa-envelope"></i> '+i18n('submit')+'</button>';
    html += '       </div>';
    
    html += '       <div style="text-align:center;padding:4px;font-size:12px">';
    if(!star.logged)
        html += '<div class="widgr-chat-noiframe" style="text-align:center;font-size:12px;padding:10px;">'+i18n('not-logged')+'</div>';    
    html += '           <i>Powered by </i><a target="_blank" class="black-link" style="opacity:1" href="'+star.baseUrl+'"><img style="height:17px; vertical-align:middle" src="'+star.baseUrl+'/public/images/logo_purple.png"></a>';
    html += '       </div>';
    html += '   </div>';


    html += '</div>';
    html += '</div>';
    return html;
};

i18n = function(code) {
    var locale = navigator.language;
    locale = locale.substring(0, 2);
    var message = star.i18nMessages && star.i18nMessages[locale][code] || code;
    // Encode %% to handle it later
    message = message.replace(/%%/g, "\0%\0");
    if (arguments.length > 1) {
        // Explicit ordered parameters
        for (var i=1; i<arguments.length; i++) {
            var r = new RegExp("%" + i + "\\$\\w", "g");
            message = message.replace(r, arguments[i]);
        }
        // Standard ordered parameters
        for (var i=1; i<arguments.length; i++) {
            message = message.replace(/%\w/, arguments[i]);
        }
    }
    // Decode encoded %% to single %
    message = message.replace("\0%\0", "%");
    // Imbricated messages
    var imbricated = message.match(/&\{.*?\}/g);
    if (imbricated) {
        for (var i=0; i<imbricated.length; i++) {
            var imbricated_code = imbricated[i].substring(2, imbricated[i].length-1).replace(/^\s*(.*?)\s*$/, "$1");
            message = message.replace(imbricated[i], i18nMessages[imbricated_code] || "");
        }
    }
    return message;
};

star.i18nMessages = {};
star.i18nMessages.en = {
        "submit":"Submit", 
        "available":"Available", 
        "your-email":"Your email", 
        "subject":"Subject", 
        "message":"Write your message", 
        "your-name":"Enter your name", 
        "chat-with":"Chat with",
        "click-to-open-chat":"Open chat",
        "join-vide-conference":"Join video call",
        "start-chat":"Start chat",
        "not-logged":"You are not logged in. To use some features you would need an account. Just <a href='#' class='widgr-iframe-btn'>sign in or register.</a> Learn more <a href='https://wid.gr' target='_blank'>about Widgr.</a>",
        "not-available-now":"is not online now, you can leave him message and he will reply you later.",
        "sign-in":"Sign in or register. Fastest with Facebook",
        "learn-more":"or learn <a href='' target='_blank'>more about Widgr.</a>",
        "we-are-online":"is available"
};
star.i18nMessages.de = {
        "submit":"Submit", 
        "available":"Available", 
        "your-email":"Your email", 
        "subject":"Subject", 
        "message":"Write your message",         
        "message":"Schreiben Sie eine Nachricht", 
        "your-name":"Eingeben Sie Ihre Name", 
        "chat-with":"Chat mit",
        "click-to-open-chat":"Chat offnen",
        "join-vide-conference":"Join Videoanruf",
        "start-chat":"Start chat",
        "not-logged":"You are not logged in. To use some features you would need an account. Just <a href='#' class='widgr-iframe-btn'>sign in or register. </a> Learn more <a href='https://wid.gr' target='_blank'>about Widgr.</a>",
        "not-available-now":"is not online now, you can leave him message and he will reply you later.",
        "sign-in":"Sign in or register. Fastest with Facebook",
        "we-are-online":"ist online"
};

star.uuid = function() {
    function s4() {
      return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
    }
    return s4() + s4() + s4() + s4() + s4() + s4() + s4() + s4();
}

star.loadScript = function(path, clbck){
    var script = document.createElement("SCRIPT");
    script.src = path;
    script.type = 'text/javascript';
    document.getElementsByTagName("head")[0].appendChild(script);
    var checkReady = function(callback) {
        if (window.jQuery) {
            callback(jQuery);
        }
        else {
            window.setTimeout(function() { checkReady(callback); }, 100);
        }
    };
    checkReady(function($) {
        clbck();
    });    
}

star.loadScript("https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js");