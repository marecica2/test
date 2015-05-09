var star = {};
star.eventMethod = window.addEventListener ? "addEventListener" : "attachEvent";
star.messageEvent = star.eventMethod == "attachEvent" ? "onmessage" : "message";
eventer = window[star.eventMethod];

// Listen to message from child window
eventer(star.messageEvent,function(e) {
    var key = e.message ? "message" : "data";
    var params = e.data;
    params = JSON.parse(params)

    // init params
    if(params.type == "init"){
        star.baseUrl = params["data-bs"];
        star.server_host = params["data-ws"]; 
        star.chatRoom = params["data-room"]; 
        star.ownerName = params["data-usr"]; 
        star.ownerAvatar = params["data-usr-ava"]; 
        star.ownerId = params["data-usr-id"]; 
        star.ownerComp = params["data-usr-comp"]; 
        star.user = "Guest"+ Math.floor(Math.random()*900);
        star.avatar = 'public/images/avatar';
        star.isOwner = false;
        star.logged = false;
        
        if(params["data-lg-user"] != undefined){
            star.logged = true;
            star.userUuid = params["data-lg-user-ui"]; 
            star.user = params["data-lg-user"]; 
            star.userAvatar = params["data-lg-user-ava"]; 
            star.lg = params["data-lg-user-lg"];
        } else {
            star.userAvatar = "public/images/avatar";
            star.userUuid = (new Date()).getTime()+"";
        }    
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
    star.container = $("#widgr-embedded-container");
    star.container.load(function() {
        //console.log($.fn.jquery);
        star.container[0].contentWindow.postMessage("widgr-frame-ready", '*');
        $('head').append('<link href="https://fonts.googleapis.com/css?family=Open+Sans:400italic,700italic,400,700,300&amp;subset=latin,latin-ext" rel="stylesheet" type="text/css">');
        $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/fonts/embedded.css">');
        $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/stylesheets/embed.css">');

        $.getScript(star.server_host+"/socket.io/socket.io.js", function() {
            $.getScript(star.baseUrl+"/public/javascripts/utils.js", function(){
                
                // add chat box if not exist
                if($(".style-switcher-container")[0] == undefined)
                    $('body').append(star.chatContent(star));
  
                // tooltip
                $(document).on("click", ".widgr-tooltip", function(){
                    $(".widgr-tooltip").hide();
                    star.utils.setCookie("widgr-tooltip", "true", 10);
                });                    

                $(document).on("click", ".widgr-iframe-btn", function(){
                    $(".widgr-chat-noiframe").hide();
                    $(".widgr-chat-iframe").show();
                });                    

                star.visible = true;
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
                
                // start chat
                $(document).on("click", ".widgr-startchat-btn", function(){
                    star.user = $(".widgr-custom-name").val();
                    $.getScript(star.baseUrl+"/public/javascripts/room.js", function(){
                        $(".widgr-custom-name").hide();
                        $(".widgr-chat-input").show();
                        $(".widgr-startchat-btn").hide();
                    });    
                });    
                
                $.getScript(star.baseUrl+"/public/javascripts/room.js", function(){
                    if(star.logged){
                        $(".widgr-chat-input").show();
                    }
                });
            
            });    
        });
    });
};

star.chatOpen = function(){
    $(".style-switcher-trigger").click();
};

star.resize = function(data){
    if(data != undefined){
        $(".widgr-chat-iframe").height(data.height);
        star.container.height(data.height);
        $(window).resize(function() {
            if(this.resizeTO) 
                clearTimeout(this.resizeTO);
            this.resizeTO = setTimeout(function(){
                star.container.height(data.height);
            }, 200);
        });    
    }
}

star.chatContent = function(star){
    var html = '<div id="widgr-container" class="">';
    
    html += '<div class="style-switcher shadow">';
    html += '   <div class="header style-switcher-trigger" style="text-align:center">';
    html += '       <a class="trigger" href="#chat"><i class="fa fa-comments-o"></i></a>';
    html += '       <strong style="color:white;">'+i18n("chat-with")+' '+star.ownerName+'</strong>';
    html += '   </div>';
    html += '   <div class="style-switcher-content" style="padding:5px;display:none">';
    html += '       <div style="margin-bottom:14px;"><img style="float:left;margin:0 7px 0px 0;height:42px;" class="img-circle" src="'+star.baseUrl+"/"+star.ownerAvatar+'_64x64"><a target="_blank" href="'+star.baseUrl+"/user/id/"+star.ownerId+'">'+star.ownerName+'</a><br/>'+star.ownerComp+'</div>';
    
    if(!star.logged){
        html += '   <div class="widgr-iframe-btn widgr-chat-noiframe" style="clear:both;"><small>You are not logged in. <a href="#">Sign in or register. Fastest with Facebook</a></small></div>';
        html += '   <table class="widgr-chat-noiframe" style="width:100%; border-collapse:collapse"><tr>';
        html += '       <td style="width:100%"><input type="text" class="form-control left-radius widgr-custom-name" maxlength="40" style="width:100%;" placeholder="'+i18n('your-name')+'"></td>';
        html += '       <td><button class="widgr-startchat-btn btn btn-default right-radius" style="width:100px;height:35px;">Start chat</button></td>';
        html += '   </tr></table>';
    }
    
    html += '       <iframe class="widgr-chat-iframe" style="margin:5px 0px;width:100%;display:none" src="'+star.baseUrl+'/login" seamless frameBorder="0"></iframe>';
    html += '       <div class="widgr-chat-noiframe widgr-chat-input" style="display:none">';
    html += '           <div id="content2" class="chat-window" style=""></div>';
    html += '           <table style="width:100%; border-collapse:collapse"><tr>';
    html += '               <td style="width:100%"><input id="chat-text2" class="form-control left-radius" maxlength="400" style="width:100%" placeholder="'+i18n('message')+'"></td>';
    html += '               <td><button id="chat-send2" class="btn btn-default btn-short right-radius" style="width:40px;height:35px;"><i class="fa fa-share fa-flip-horizontal"></i></button></td>';
    html += '           </tr></table>';
    html += '       </div>';
    html += '       <div class="widgr-chat-noiframe" style="text-align:left;padding:4px;font-size:12px">';
    html += '           <i>Powered by </i><a target="_blank" class="black-link" style="opacity:1" href="'+star.baseUrl+'"><img style="height:17px; vertical-align:middle" src="'+star.baseUrl+'/public/images/logo_purple.png"></a>';
    html += '       </div>';
    html += '   </div>';
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
        "message":"Write your message", 
        "your-name":"Or enter your name", 
        "chat-with":"Chat with",
        "click-to-open-chat":"Open chat",
        "join-vide-conference":"Join video call",
        "we-are-online":"is available"
};
star.i18nMessages.de = {
        "message":"Schreiben Sie eine Nachricht", 
        "your-name":"Oder eingeben Sie Ihre Name", 
        "chat-with":"Chat mit",
        "click-to-open-chat":"Chat offnen",
        "join-vide-conference":"Join Videoanruf",
        "we-are-online":"ist online"
};

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
star.loadScript("https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js", star.embedInit);