var star = {};
star.embedded = true;
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
        star.server_host = params["data-ws"]; 
        star.chatRoom = params["data-room"]; 
        
        star.listingVideo = params["data-listingVideo"]; 
        star.listingTitle = params["data-listingTitle"]; 
        star.listingUuid = params["data-listingUuid"]; 
        star.listing = star.listingUuid;
        star.listingImage = params["data-listingImage"]; 
        star.listingCharging = params["data-listingCharging"]; 
        star.listingPrice = params["data-listingPrice"]; 
        star.listingDuration = params["data-listingDuration"]; 
        star.listingCurrency = params["data-listingCurrency"]; 
        star.listingFirstFree = params["data-listingFirstFree"];
        star.listingStars = params["data-listingStars"];
        star.listingReviews = params["data-listingReviews"];
        star.listingUserName = params["data-listingUserName"];
        star.listingUserUuid = params["data-listingUserUuid"];
        
        star.ownerName = params["data-owner"]; 
        star.ownerAvatar = params["data-owner-avatar"]; 
        star.ownerUuid = params["data-owner-uuid"]; 
        star.ownerCompany = params["data-owner-company"]; 
        star.isOwner = false;

        if(params["data-lg-user"] != undefined){
            star.logged = true;
            star.userUuid = params["data-lg-user-ui"]; 
            star.userName = params["data-lg-user"]; 
            star.userAvatar = params["data-lg-user-ava"]; 
            star.userLogin = params["data-lg-user-lg"];
        } else {
            star.userAvatar = "public/images/avatar";
            star.userName = "Guest"+ Math.floor(Math.random()*900);
            star.logged = false;
            var uuid = star.getCookie("widgr-user-uuid");
            if(!star.logged && uuid != "")
                star.userUuid = star.getCookie("widgr-user-uuid");
            else {
                star.userUuid = star.uuid();
                star.setCookieMinutes("widgr-user-uuid", star.userUuid, 60);        
            }
        }
        
        star.loadScript("https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js", star.embedInit);
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
    if(params.type == "feeds"){
        var data  = params.data;
        var container = $(".load-feeds").parent();
        star.feedsRender(data, container);
    }
},false);


star.embedInit = function(){
    star.container = $("#widgr-embedded-container");
    star.style = star.container.attr("data-style");
    star.chatboxTitle = star.container.attr("data-title");
    star.chatboxIcon = star.container.attr("data-icon");
    if(star.loaded)
        return;
    
    star.container[0].contentWindow.postMessage("widgr-frame-ready", '*');
    $('head').append('<link href="https://fonts.googleapis.com/css?family=Open+Sans:400italic,700italic,400,700,300&amp;subset=latin,latin-ext" rel="stylesheet" type="text/css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/fonts/embedded.css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/plugins/magnific-popup/magnific-popup.css">');
    $('head').append('<style type="text/css">.mfp-container:before { vertical-align: top; }</style>');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/stylesheets/embed.css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/stylesheets/embed-'+star.style+'.css">');

    $.getScript(star.server_host+"/socket.io/socket.io.js", function() {
        $.getScript(star.baseUrl+"/public/javascripts/utils.js", function(){
            $.getScript(star.baseUrl+"/public/plugins/magnific-popup/jquery.magnific-popup.min.js", function(){
                star.loaded = true;
               
                // read user name from cookie
                var name = star.utils.getCookie("widgr-name");
                if(!star.logged && name != "")
                    star.userName = name;
                
                // add chat box if not exist
                if($(".style-switcher-container")[0] == undefined){
                    $('body').append(star.utils.chatContent(star));
                    var widgr_int2 = setInterval(function(){
                            $("#widgr-container").show();
                            clearInterval(widgr_int2);
                        }, 1000);                    
                }   
                
                
                // popup iframe
                $(document).ready(function(){
                    if (($(".popup-iframe").length > 0)) {       
                        $('.popup-iframe').magnificPopup({
                            disableOn: 700,
                            type: 'iframe',
                            preloader: false,
                            fixedContentPos: false
                        });
                    };
                });                
                
                // set language 
                $(".widgr-lang-flag").hide();
                $(".widgr-lang-flag[data-lang="+star.getLocale()+"]").show();
                $(document).on("click", ".widgr-lang-select", function(){
                    $(".widgr-lang-flag").show();
                    $(".widgr-lang-flag").unbind();
                    $(document).on("click", ".widgr-lang-flag", function(){
                        star.setLocale($(this).attr("data-lang"));
                        console.log($(this).attr("data-lang"));
                        document.location.reload();
                    });             
                });             
                $(document).on("mouseleave", ".widgr-lang-select", function(){
                    $(".widgr-lang-flag").hide();
                    $(".widgr-lang-flag[data-lang="+star.getLocale()+"]").show();
                });             
                
                
                if(star.logged || name != ""){
                    $(".widgr-custom-name").val(star.userName);
                    $(".widgr-custom-name").hide();
                    $(".widgr-chat-input").show();
                    $(".widgr-startchat-btn").hide();
                }
                
                $('nav li ul').hide().removeClass('fallback');
                $('nav li').hover(
                  function () {
                    $('ul', this).stop().slideDown(100);
                  },
                  function () {
                    $('ul', this).stop().slideUp(100);
                  }
                );            
                
                $.getScript(star.baseUrl+"/public/javascripts/livechat.js", function(){
                    
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
};

star.uuid = function() {
    function s4() {
      return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
    }
    return s4() + s4() + s4() + s4() + s4() + s4() + s4() + s4();
}

star.setCookieMinutes = function(name,value,minutes) {
    var date = new Date();
    if(minutes == undefined)
        date.setTime(date.getTime()+(9999*24*60*60*1000));
    else
        date.setTime(date.getTime()+(minutes*60*1000));
    var expires = "; Expires="+date.toGMTString();
    document.cookie = name+"="+value+expires+"; Path=/";
}

star.getCookie = function(cname) {
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for(var i=0; i<ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1);
        if (c.indexOf(name) != -1) return c.substring(name.length,c.length);
    }
    return "";
}

star.getLocale = function() {
    var locale = navigator.language;
    locale = locale.substring(0, 2);
    var lc = star.getCookie("widgr-embed-locale");
    if(lc != "")
        locale = lc;
    if(typeof star.i18nMessages[locale] == "undefined")
        locale = "en";
    return locale;
}

star.setLocale = function(code) {
    star.utils.setCookie("widgr-embed-locale",  code);
}

i18n = function(code) {
    var locale = star.getLocale();
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

var prefix;
var version;

if (window.mozRTCPeerConnection || navigator.mozGetUserMedia) {
    prefix = 'moz';
    version = parseInt(navigator.userAgent.match(/Firefox\/([0-9]+)\./)[1], 10);
} else if (window.webkitRTCPeerConnection || navigator.webkitGetUserMedia) {
    prefix = 'webkit';
    version = navigator.userAgent.match(/Chrom(e|ium)/) && parseInt(navigator.userAgent.match(/Chrom(e|ium)\/([0-9]+)\./)[2], 10);
}

star.i18nMessages = {};
star.i18nMessages.en = {
        "incompatible-browser":"Warning! Your browser does not support video calls.",
        "download-browser":"Please download latest version of supported browser, ",
        "instant-video-call":"Instant videocall",
        "sign-facebook":"Sign in with Facebook (recommended)",
        "first-free":"First Session free",
        "watch-intro":"Watch intro",
        "online":"Online",
        "from":"from",
        "previous-messages":"Previous messages",
        "incorrect-input":"Please correct your input",
        "operator-available":"Operator will available for you in few seconds. Please wait.",
        "operator":"Operator",
        "message-sent":"Your message has been sent",
        "new-message":"New message",
        "left-conversation":"left conversation",
        "submit":"Submit", 
        "available":"Available", 
        "your-email":"Your email", 
        "subject":"Subject", 
        "message":"Write your message", 
        "or-your-name":"Or enter your name", 
        "chat-with":"Chat with",
        "click-to-open-chat":"Open chat",
        "join-vide-conference":"Join videocall",
        "start-chat":"Start chat",
        "not-logged":"You are not logged in. To use some features you would need an account. Just <a href='#' class='widgr-iframe-btn'>sign in with registration form</a>. Or",
        "not-available-now":"We are not online now, you can leave us a message and we will reply you later.",
        "offline-title":"Write us a message",
        "":""
};
star.i18nMessages.de = {
        "incompatible-browser":"Warning! Your browser does not support video calls.",
        "download-browser":"Please download latest version of supported browser, ",
        "instant-video-call":"Instant videocall",
        "sign-facebook":"Sign in with Facebook (recommended)",        
        "first-free":"First Sitzung frei",
        "watch-intro":"Watch intro",
        "online":"Online",
        "from":"from",
        "previous-messages":"Previous messages",
        "incorrect-input":"Please correct your input",
        "operator-available":"Operator will available for you in few seconds. Please wait.",
        "operator":"Operator",
        "message-sent":"Your message has been sent",
        "new-message":"New message",
        "left-conversation":"left conversation",
        "submit":"Submit", 
        "available":"Available", 
        "your-email":"Your email", 
        "subject":"Subject", 
        "message":"Write your message",         
        "message":"Schreiben Sie eine Nachricht", 
        "or-your-name":"Eingeben Sie Ihre Name", 
        "chat-with":"Chat mit",
        "click-to-open-chat":"Chat offnen",
        "join-vide-conference":"Videoanruf",
        "start-chat":"Start chat",
        "not-logged":"You are not logged in. To use some features you would need an account. Just <a href='#' class='widgr-iframe-btn'>sign in with registration form</a>. Or",
        "not-available-now":"Wir sind leider nicht online. is not online now, you can leave him message and he will reply you later.",
        "offline-title":"Nachricht senden",
        "":""
};
star.i18nMessages.sk = {
        "incompatible-browser":"Dôležité! Váš prehliadač nepodporuje videohovory.",
        "download-browser":"Please download latest version of supported browser, ",
        "instant-video-call":"Spustiť videohovor",
        "sign-facebook":"Prihláste sa pomocou Facebook (odporúčané)",
        "first-free":"Prvá konzultácia zadarmo",
        "watch-intro":"Pozrieť ukážku",
        "online":"Online",
        "from":"od",
        "previous-messages":"Staršie správy",
        "incorrect-input":"Prosím vyplňte formulár",
        "operator-available":"Operátor Vám bude k dispozícii o pár sekúnd. Prosím čakajte",
        "operator":"Operátor",
        "message-sent":"Vaša správa bola odoslaná",
        "new-message":"Nová správa",
        "left-conversation":"opustil konverzáciu",
        "submit":"Odoslať", 
        "available":"Online", 
        "your-email":"Váš email", 
        "subject":"Predmet", 
        "message":"Správa", 
        "or-your-name":"Alebo zadajte meno", 
        "chat-with":"Napísať správu",
        "click-to-open-chat":"Otvoriť chat",
        "join-vide-conference":"Spustiť videohovor",
        "start-chat":"Spustiť chat",
        "not-logged":"Nieste prihlásený. Pre plné použitie je potrebné sa zaregistrovať. <a href='#' class='widgr-iframe-btn'>Použite registračný formulár</a>. Alebo",
        "not-available-now":"Momentálne niesme online. Prosím zanechajte nám správu a my Vám odpovieme hneď ako to len bude možné",
        "offline-title":"Poslať správu",
        "":""
};

