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
        star.user = "Guest"+ Math.floor(Math.random()*900) ;
        star.avatar = 'public/images/avatar';
        star.isOwner = false;
        
        if(params["data-lg-user"] != undefined){
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
        console.log(params);
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
                $.getScript(star.baseUrl+"/public/javascripts/room.js", function(){
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
                    
                    $.getScript(star.baseUrl+"/public/style-switcher/style-switcher.js", function(){
                    });    
                });    
            });    
        });
    });
};

star.chatOpen = function(){
    $(".style-switcher-container .trigger").click();
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
    var html = '<div id="widgr-container">';
    
    html += '<div class="style-switcher shadow closed shadow-small style-switcher-container" style="display:none;">';
    html += '   <span class="widgr-tooltip default-bg shadow" style="display:nones"><strong class="blink">'+star.ownerName+" "+i18n('we-are-online')+'</strong>&nbsp; <i style="float:right;" class="fa fa-times tooltip-close"></i><br/>'+i18n('click-to-open-chat')+'</span>';
    html += '   <div class="header" style="text-align:center">';
    html += '       <a class="trigger btn-default" href="#chat" style=""><i class="fa fa-comments-o"></i></a>';
    html += '       <strong style="color:white;">'+i18n("chat-with")+'</strong>';
    html += '   </div>';
    html += '   <div style="padding:5px;">';
    html += '       <div style="float:left"><img style="border-radius:5px;margin:0 7px 0px 0;height:50px;border:1px solid gray" src="'+star.baseUrl+"/"+star.ownerAvatar+'_32x32"></div>'+star.ownerName+'<br/> Accounting';
    html += '       <small style="display:inline-block" class="widgr-iframe-btn widgr-chat-noiframe">Please provide your name or <a href="#">sign in with Facebook</a></small>';
    html += '       <input type="text" class="form-control radius widgr-chat-noiframe" style="width:100%;margin-top:5px" placeholder="'+i18n('your-name')+'">';
    html += '       <iframe class="widgr-chat-iframe" style="margin:5px 0px;width:100%;display:none" src="'+star.baseUrl+'/login" seamless frameBorder="0"></iframe>';
    html += '       <div class="widgr-chat-noiframe">';
    html += '           <div id="content2" class="chat-window" style=""></div>';
    html += '           <table style="width:100%; border-collapse:collapse"><tr>';
    html += '               <td style="width:100%"><input id="chat-text2" class="form-control left-radius" maxlength="400" style="width:100%" placeholder="Message"></td>';
    html += '               <td><button id="chat-send2" class="btn btn-default btn-short right-radius" style="width:40px;height:35px;"><i class="fa fa-share fa-flip-horizontal"></i></button></td>';
    html += '           </tr></table>';
    html += '       </div>';
    html += '       <div class="widgr-chat-noiframe" style="text-align:center;padding:10px;font-size:12px">';
    html += '           <a target="_blank" class="black-link" href="'+star.baseUrl+'"><img style="vertical-align: middle;height: 30px" src="'+star.baseUrl+'/public/images/logo_purple.png"></a>';
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
        "message":"Message", 
        "your-name":"Enter your name", 
        "chat-with":"Chat with",
        "click-to-open-chat":"Open chat",
        "we-are-online":"is available"
};
star.i18nMessages.de = {
        "message":"Nachricht", 
        "your-name":"Ihre Name", 
        "chat-with":"Chat mit",
        "click-to-open-chat":"Chat offnen",
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