star.initItems = function(prefix, urlParams){
    $(document).ready(function(){
        star[prefix] = {};
        star[prefix].size = 9;
        star[prefix].first = 0;
        star[prefix].count = star[prefix].size;

        // initial get elements
        var url = "first="+star[prefix].first+"&count="+star[prefix].count+urlParams;
        var params = {};
        params.url = url;
        params.prefix = prefix;
        

        starServices.getItems(prefix, params, function(data){
            $("#spinner"+prefix).show();
            $("#moreResults"+prefix).hide();
            var html = star.generateItemsHtml(data, prefix);
            var f = function(html){
                $(html).hide().appendTo($("#itemsList"+prefix)).fadeIn(500);
                $("#spinner"+prefix).hide();
                $("#moreResults"+prefix).show();
            }
            setTimeout(function(){
                f(html);
            }, 50);
        });

        // append elements        
        $(window).unbind('scroll');
        $(window).scroll(function() {
            var diff = Math.abs($(window).scrollTop() - ($(document).height() - $(window).height()));
            if(diff < 1) {
                //console.log($(window).scrollTop() + "  " + ($(document).height() - $(window).height()) + " diff" + diff)
                $("#moreResults"+prefix).click();              
            }
       }); 
        
       // event handlers
       $("#moreResults"+prefix).off();
       $("#moreResults"+prefix).click(function(){
           star[prefix].first += star[prefix].size;
           var url = "first="+star[prefix].first+"&count="+star[prefix].count+urlParams;
           params.url = url;
           star.loadItems(prefix, params);                  
       });

    });    
};








star.loadItems = function(prefix, urlParams){
    $("#spinner"+prefix).show();
    $("#moreResults"+prefix).hide();
    starServices.getItems(prefix, urlParams, function(data){
        var html = star.generateItemsHtml(data, urlParams.prefix);
           var f = function(html){
                $(html).hide().appendTo($("#itemsList"+urlParams.prefix)).fadeIn(500);
                $("#spinner"+urlParams.prefix).hide();
                $("#moreResults"+urlParams.prefix).show();
           }
        setTimeout(function(){
                f(html);
          }, 100);
    });
};

star.generateItemsHtml = function(data, prefix){
    var current_date = new Date().getTime();
    var days, hours, minutes, seconds;
    var _second = 1000;
    var _minute = _second * 60;
    var _hour = _minute * 60;
    var _day = _hour *24
    var now = new Date();

    var html = "";
    for(var i = 0; i < data.length; i++){
        var item = data[i];
        if(prefix.indexOf("Listing") >= 0){
            html += "<div class='shadow-blur' style='text-align:left;background:white;  position:relative; width:320px; height:240px; float:left;margin:0px; margin:5px;border-top:5px solid #{if item && item.color} ${item.color} #{/if} #{else} #3A87AD #{/else};'>";
            html += "   <div class='link' data-href='/listing-new?uuid="+item.uuid+"' style='cursor:pointer'>";
            html += "           <img src='../"+item.imageUrl+"_128x128' />";
            html += "       ";
            html += "       <div style='position:absolute;top:90px;left:10px'>";
            html += "           <img class='img-circle' style='border:3px solid white;float:left' src='../"+item.createdByAvatarUrl+"_64x64'>";
            html += "       </div>";
            html += "       ";
            html += "       <div style='margin-top: -40px;margin-left: 90px; width: 230px; position:relative;height:40px; line-height: 1.2; overflow: hidden; text-overflow: ellipsis;'>";
            html += "           <span style='position:absolute; bottom:0px; max-height: 40px; text-shadow: 1px 1px 8px #000;'>";
            html += "               <span style='color:white; text-decoration: none; font-size: 1.3em; font-weight: bold' href='/user/${item.user.login}' >"+item.title+"</span>";
            html += "           </span>";
            html += "       </div>";
            html += "       ";
            html += "       <div class='segoe-bold' style='margin-left: 90px;margin-top:5px; width: 250px; position:relative;height:40px'>";
            html += "           <span style='color:black; text-decoration: none; font-size: 1.0em; font-weight: bold'>"+item.createdByName+"</span>";
            html += "       </div>";
            html += "       ";
            html += "       <div style='width:100%; position:absolute; bottom:0px; padding:10px' >";
            html += "           <hr style='border:1px solid #f7f7f7' />";
            html += "           <p> ";
            html += "               <div style='float:left; font-size:0.8em; font-weight: bold; text-align: center'>";
            for(var j = 0; j < 5; j++){
                html += "                           <i style='font-size:2em' class='fa fa-star blue' data-value='1'></i>";
            }
            html += "                   <br/>";
            html += "                   123 reviews";
            html += "               </div>";
            html += "                       ";
            html += "               <div style='float:right;'>";

            if(item.charging == 'free'){
                html += "                   <span style='font-size:1.2em; font-weight: bold'>Free</span>";
            }

            if(item.charging == 'before'){
                html += "                   <span style='font-size:1.2em; font-weight: bold'>"+item.price+" "+item.currency+"</span>";
            }
            
            html += "               </div>";
            html += "           </p>";
            html += "       </div>";
            html += "       </div>      ";
            html += "</div> ";            
            
        } else {
            var end = data[i].eventStart;
            var distance = end - now;
            var days = Math.floor(distance / _day);
            var hours = Math.floor( (distance % _day ) / _hour );
            var minutes = Math.floor( (distance % _hour) / _minute );
            var seconds = Math.floor( (distance % _minute) / _second );

            var color = "#3a87ad";
            if(data[i] != undefined && data[i].color != undefined && data[i].color.length > 0)
                color = data[i].color;
            if(!data[i].isOwner)
                color = "darkgray";
            
            event = data[i];
            
            html += "<div class='event_poster shadow-blur'>";
            html += "   <div class='poster_wrapper'>";
            html += "       <a href='/event-detail?uuid="+event.uuid+"' class='poster_link'>";
            html += "           <div class='poster_image' style='background:url(\"../"+event.imageUrl+"_128x128\") no-repeat black center;background-size: 375px 150px;width:150px;height:150px'></div>";
            html += "       </a>";
            html += "       <div class='description' style='font-size:0.9em;min-height:40%'>";
            html += "           <strong><a href='/event-detail?uuid="+event.uuid+"' class='event_title color-link-light'>"+event.title+"</a></strong>";
            html += "           <p class='event_blurb'>";
            html += "               <img height='16' src='/"+event.createdByAvatarUrl+"_32x32'> <a href='/user/"+event.createdByLogin+"' class='color-link-light'>"+event.createdByName+"</a>";
            html += "           </p>";
            html += "       </div>";
            html += "   </div>";
            html += "   <div class='actions'>";
            html += "   </div>";
            html += "   <div class='event-time black' style='padding:5px'>";
            html += "       <span class='event-time-from' style='font-weight:bold' id='event-time-from'>"+starUtils.formatDate(event.eventStart)+"</span><br/> ";
            html += "       <span class='event-time-to'>"+starUtils.formatTime(event.eventStart)+"</span> - ";
            html += "       <span class='event-time-to'>"+starUtils.formatTime(event.eventEnd)+"</span>";
            html += "   </div>";
            html += "</div>";            
        }
    }
    return html    
};






starEvent.editEventDialogShow = function(elm){
    if($(elm).attr("edit") == undefined){
        $(".event-view").hide();
        $(".event-edit").show();
        $(elm).attr("edit", true);
        $(elm).removeClass("fa-pencil");
        $(elm).addClass("fa-times");
    } else {
        $(".event-view").show();
        $(".event-edit").hide();
        $(elm).removeAttr("edit");
        $(elm).removeClass("fa-times");
        $(elm).addClass("fa-pencil");
    }
};

starEvent.eventIcons = function(event){
    var html = "";
    if(event.type == "p2p"){
       html += ("<i class='fa fa-comments' title='"+i18n("app.eventP2P")+"'></i> ");
    }
    if(event.type == "livecast"){
        html += ("<i class='fa fa-globe' title='"+i18n("app.eventLivecast")+"'></i> ");
    }
    if(event.charging == "free"){
        html += ("<span ><i class='fa fa-ban' title='"+i18n("app.eventFree")+"'></i></span> ");
    }
    if(event.charging == "before"){
        html += ("<span ><i class='fa fa-dollar' title='"+i18n("app.eventPayBefore")+"'></i></span> ");
    }
    if(event.charging == "after"){
        html += ("<span ><i class='fa fa-dollar' title='"+i18n("app.eventPayAfter")+"'></i></span> ");
    }
    if(event.privacy == "public"){
        html += ("<i class='fa fa-unlock' title='"+i18n("app.eventPublic")+"'></i> ");
    } 
    if(event.privacy == "private"){
        html += ("<i class='fa fa-lock' title='"+i18n("app.eventPrivate")+"'></i> ");
    }
    if(event.privacy == "hidden"){
        html += ("<i class='fa fa-eye-slash' title='"+i18n("app.eventHidden")+"'></i> ");
    }
    return html;
};

starEvent.saveEventDialog = function(e){
    starCalendar.selectedEvent.title = $("#event-title").val();
    starCalendar.selectedEvent.description = $("#event-description").val();
    starCalendar.selectedEvent.backgroundColor = $("#event-color").val();
    starCalendar.selectedEvent.color = $("#event-color").val();
    if(starUtils.getRadio("event-privacy") != undefined)
        starCalendar.selectedEvent.privacy = starUtils.getRadio("event-privacy");
    if(starUtils.getRadio("event-type") != undefined)
        starCalendar.selectedEvent.type = starUtils.getRadio("event-type");
    if(starUtils.getRadio("event-charging") != undefined)
        starCalendar.selectedEvent.charging = starUtils.getRadio("event-charging");
    if($(".event-currency").val() != undefined)
        starCalendar.selectedEvent.currency = $(".event-currency").val();

    var pr = $(".event-charging-price").val();
    starCalendar.selectedEvent.price = pr == undefined ? "0" : pr;
    
    var valid = true;
    if(starCalendar.selectedEvent.charging != "free" && isNaN(starCalendar.selectedEvent.price)){
        valid = false;
        $("#modal-validation-price").show();
    } else {
        $("#modal-validation-price").hide();
    }
    
    if(valid){
        starEvent.updateEvent(starCalendar.selectedEvent, function(){
            starEvent.copyValuesToDialog(starCalendar.selectedEvent);
            $(".dialog-event-edit").click();
            $(".spinner-save-notify").hide();
        });
    }
    return false;
}


starEvent.updateEvent = function(event, timeChanged, success) {
    var eventUpdate = {};
    eventUpdate.uuid = event.uuid; 
    eventUpdate.eventStart = event.start; 
    eventUpdate.eventEnd = event.end;
    eventUpdate.description = event.description;
    eventUpdate.title = event.title;
    eventUpdate.color = event.backgroundColor;
    eventUpdate.type = event.type;
    //eventUpdate.privacy = event.privacy;
    //eventUpdate.price = event.price;
    //eventUpdate.currency = event.currency;
    //eventUpdate.charging = event.charging;
    console.log(timeChanged);
    console.log(timeChanged);
    console.log(timeChanged);
    if(timeChanged != undefined){
        eventUpdate.changedTime = true;
    }
    starServices.updateEvent(eventUpdate, success, null);
};




// invitations
starEvent.inviteLoad = function(event, clbck){
    $(".dialog-invite-list").html("");
    var cb = clbck;
    starServices.getAttendances("event="+event, function(data){
        
        $(".dialog-invite-list").each(function() {
            var checkboxes = false;
            if($(this).attr("data-checkboxes") == "true"){
                checkboxes = true;
            }
            var html = "";
            for(var i in data){
                html += "<tr>";
                html += "<td style=''>";

                //checkboxes
                if(starCalendar.selectedEvent.editable && checkboxes){
                    html += "<input value='x' id='notify-"+data[i].uuid+"' data-uuid='"+data[i].uuid+"' type='checkbox' class='notify-checkboxes'> ";
                    html += "<label for='notify-" + data[i].uuid + "'>";
                }
                
                var isAdmin = data[i].isForUser ? " <strong>"+i18n("app.admin")+"</strong>" : "";
                if(data[i].avatar){
                    html += "<img class='avatar16' src='/"+data[i].avatar+"_32x32'>&nbsp;";
                }
                
                if(starCalendar.login != data[i].email)
                    html += "<a href='/public/user?id="+data[i].customer+"' title='"+data[i].email+"'>";
                
                if(starCalendar.login == data[i].email){
                    html += i18n("app.you") + " &nbsp;";
                } else if(data[i].customer == null && !starCalendar.isPublic){
                    html += "" + data[i].email ;
                } else {
                    html += "" + data[i].name ;
                }
                
                if(starCalendar.login != data[i].email)
                    html += "</a>";
                
                //checkboxes
                if(starCalendar.selectedEvent.editable && checkboxes){
                    html += "</label>";
                }
                html += "</td>";

                if(starCalendar.selectedEvent.charging != "free"){
                    html += "<td style='text-align:center;'>";
                    html += data[i].paid ? "<p class='label label-success'>paid</p>" : "<p class='label label-default'>unpaid</p>";
                    html += "</td>";
                }
                
                html += "<td style='text-align:center;'>";
                if(data[i].result == "accepted"){
                    html += "&nbsp;<p class='label label-success'>"+i18n("app.accepted")+"</p>&nbsp;";
                } else if(data[i].result == "declined"){
                    html += "&nbsp;<p class='label label-danger'>"+i18n("app.declined")+"</p>&nbsp;";
                } else {
                    html += "&nbsp;<p class='label label-default'>"+i18n("app.waiting")+"</p>&nbsp;";
                }
                html += "</td>";
                html += "<td style='text-align:right;vertical-align:middle;'>";
                
                html += "<nobr style='font-size:0.6em;'>";
                if(starCalendar.login == data[i].email){
                    html += "<a href='#' class='color-link btn btn-default btn-xs' title='"+i18n("app.acceptedInvitation")+"'><i data-uuid='" + data[i].uuid + "' class='fa fa-check dialog-invite-accept fa-1x'></i></a> ";
                    html += "<a href='#' class='color-link btn btn-default btn-xs' title='"+i18n("app.declineInvitation")+"'><i data-uuid='" + data[i].uuid + "'  class='fa fa-times dialog-invite-decline fa-1x'></i></a> &nbsp; ";
                }
                if(starCalendar.selectedEvent.editable){
                    html += "<a href='#' class='color-link btn btn-default btn-xs' title='"+i18n("app.deleteInvitation")+"'><i data-uuid='" + data[i].uuid + "' class='fa fa-trash dialog-invite-delete fa-1x'></i></a> ";
                }

                html += "</nobr>";
                html += "</td>";
                html += "</tr>";
                
                
                $(this).html(html);
            }
        });
        
        if(cb != undefined)
            cb();
    });
};


starEvent.inviteAdd = function(){
    var data = {};
    data.email = $("#dialog-invite-input").val();
    data.event = starCalendar.selectedEvent.uuid;
    if(data.email.length > 0){
        starServices.createAttendance(data, function(res){
            starEvent.inviteLoad(starCalendar.selectedEvent.uuid);
            $("#dialog-invite-input").val("");
        });
    }
};



starEvent.eventIcons = function(event){
    var html = "";
    if(event.type == "p2p"){
       html += ("<i class='fa fa-comments' title='"+i18n("app.eventP2P")+"'></i> ");
    }
    if(event.type == "livecast"){
        html += ("<i class='fa fa-globe' title='"+i18n("app.eventLivecast")+"'></i> ");
    }
    if(event.charging == "free"){
        html += ("<span ><i class='fa fa-ban' title='"+i18n("app.eventFree")+"'></i></span> ");
    }
    if(event.charging == "before"){
        html += ("<span ><i class='fa fa-dollar' title='"+i18n("app.eventPayBefore")+"'></i></span> ");
    }
    if(event.charging == "after"){
        html += ("<span ><i class='fa fa-dollar' title='"+i18n("app.eventPayAfter")+"'></i></span> ");
    }
    if(event.privacy == "public"){
        html += ("<i class='fa fa-unlock' title='"+i18n("app.eventPublic")+"'></i> ");
    } 
    if(event.privacy == "private"){
        html += ("<i class='fa fa-lock' title='"+i18n("app.eventPrivate")+"'></i> ");
    }
    if(event.privacy == "hidden"){
        html += ("<i class='fa fa-eye-slash' title='"+i18n("app.eventHidden")+"'></i> ");
    }
    return html;
};




starEvent.inviteDelete = function(){
    var data = {};
    var uuid = $(this).attr("data-uuid");
    data.uuid = uuid;
    starServices.deleteAttendance(data, function(data){
        starEvent.inviteLoad(starCalendar.selectedEvent.uuid);
    });
};


starEvent.inviteAccept = function(){
    var data = {};
    var uuid = $(this).attr("data-uuid");
    data.uuid = uuid;
    data.result = "accepted";
    starServices.updateAttendance(data, function(){
        starEvent.inviteLoad(starCalendar.selectedEvent.uuid);
    });
};


starEvent.inviteDecline = function(){
    var data = {};
    var uuid = $(this).attr("data-uuid");
    data.uuid = uuid;
    data.result = "declined";
    starServices.updateAttendance(data, function(){
        starEvent.inviteLoad(starCalendar.selectedEvent.uuid);
    });
};


starEvent.loadActivities = function(uuid){
    var id = starUtils.getParameterByName("id");
    var params = "limit="+starCalendar.activitiesLimit;
    if(id != "")
        params += "&id="+id;
    if(uuid != undefined)
        params += "&uuid="+uuid;
    else {
        $(".activities").html("");
    }
    
    starServices.getActivities(params, function(success){
        var prev = null;
        if(success.length > 0)
            prev = success[0].byCustomer ? success[0].customerAvatar : success[0].userAvatar;
        for(var i = 0; i < success.length; i++){
            var html = "";
            html += "<li class='activity-item'>";
            html += "<table style='width:100%;'>";
            html += "<tr>";
            
            html += "<td style='vertical-align:top'>";
            
            var avatar = success[i].byCustomer ? success[i].customerAvatar : success[i].userAvatar;
            if(avatar != prev || i == 0){
                html += "<img class=' avatar32' style='' src='/"+avatar+"'>";
            } else {
                html +="<div style='width:32px'>&nbsp;</div>"
            }
            prev = avatar;
            html += "</td>";
            html += "<td style='padding-left:5px;vertical-align:top;width:100%'>";
            html += "<div style='font-size:0.9em;color:#ccc'>";
            var url = success[i].byCustomer ? "/user/"+success[i].customerLogin : "/user/"+success[i].userLogin;
            var name = success[i].byCustomer ? success[i].customer : success[i].user;
            html += "<a class='' href='" + url +"'>" + name + "</a>&nbsp;on ";
            html += starUtils.formatDateTime(success[i].created) + " " + " &nbsp; ";
            html +="</div>";

            html += "<div style='font-size:0.9em;'>";
            html += success[i].message;
            html +="</div>";
            
            html += "</td>";
            html += "</tr>";
            html += "</table>";
            html += "</li>";
            $(html).hide().css("opacity", 0.0).css("position", "relative").css("left", 400)
                .appendTo(".activities").slideDown('fast').delay(i * 20)
                .animate({opacity: 1.0, left: 0}, {queue : true, duration : 400, complete : function(){$(".activities").removeAttr("style");  }  });
        }
        
        
        var rows = $("li", ".activities");
        if(rows.length > starCalendar.activitiesLimit){
            var diff = rows.length - starCalendar.activitiesLimit;
            for(var i = 0; i < diff; i++){
                rows.last().animate({opacity: 0.0 }, {complete : function(){
                            $(this).remove();
                        }
                    });
            }
        }
    });
};



//
// services
//
var starServices = {};

starServices.createEvent = function(data, success, error){
 //console.log("peforming ajax post");
 //console.log(data);
 $.ajax({
     type: "POST",
     url: "/event/save",
     data: JSON.stringify(data),
     success: success,
     error: error,
     contentType: "application/json"
 });
};

starServices.updateEvent = function(data, success, error){
 //console.log("peforming ajax post");
 //console.log(data);
 $.ajax({
     type: "POST",
     url: "/event/update",
     data: JSON.stringify(data),
     success: success,
     error: error,
     contentType: "application/json"
 });
};

starServices.deleteEvent = function(data, success, error){
    //console.log("peforming ajax delete");
    //console.log(data);
    $.ajax({
        type: "DELETE",
        url: "/event/delete",
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.inviteEvent = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/event/invite",
        data: JSON.stringify(data),
        contentType: "application/json",
        success: success,
        error: error
    });
};

starServices.getItems = function(prefix, params, success, error){
    var url = "events";
    if(prefix.indexOf("Listing") != -1)
        url = "listings";
    $.ajax({
        type: "GET",
        url: "/"+url+"?"+params.url,
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.getEvent = function(params, success, error){
    $.ajax({
        type: "GET",
        url: "/event?"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.createAttendance = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/attendance/save",
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.updateAttendance = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/attendance/update",
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.deleteAttendance = function(data, success, error){
    $.ajax({
        type: "DELETE",
        url: "/attendance/delete",
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.getAttendances = function(params, success, error){
    $.ajax({
        type: "GET",
        url: "/attendances?"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.getActivities = function(params, success, error){
    $.ajax({
        type: "GET",
        url: "/public/activities?"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.addComment = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/event/comment",
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.deleteComment = function(data, success, error){
    $.ajax({
        type: "DELETE",
        url: "/event/comment",
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.setAgendaType = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/settings/agenda",
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

//
//star utils
//
var starUtils = {};
starUtils.s4 = function() {
 return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
};

starUtils.getRadio = function(name) {
    return $("input:radio[name='"+name+"']:checked").val();
};

starUtils.setRadio = function(name, value) {
    $("input:radio[name='"+name+"'][value='" + value + "']").prop('checked', true);
};

starUtils.uuid = function() {
 return starUtils.s4() + starUtils.s4() + '-' + starUtils.s4() + '-' + starUtils.s4() + '-' +
 starUtils.s4() + '-' + starUtils.s4() + starUtils.s4() + starUtils.s4();
};

starUtils.formatDateTime = function(long) {
    var d = new Date()
    var diff = d.getTimezoneOffset();
    var s = new Date();
    s.setTime(long + (diff*60000));
    return s.toLocaleDateString() + " " + s.toLocaleTimeString();
};

starUtils.formatDate = function(long) {
    return dateFormat(long, dateFormat.masks.mediumDate);
};

starUtils.formatTime = function(long) {
    return dateFormat(long, dateFormat.masks.shortTime);
};

starUtils.formatDateTime = function(date) {
    date = new Date(date)
    if(date != undefined)
        return dateFormat(date, dateFormat.masks.mediumDate) + " " + dateFormat(date, dateFormat.masks.shortTime);
    return null;
};

starUtils.getParameterByName = function(name) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(document.URL);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
};


starUtils.getCookie = function(cname)
{
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for(var i=0; i<ca.length; i++) {
      var c = ca[i].trim();
      if (c.indexOf(name)==0) return c.substring(name.length,c.length);
    }
    return "";
}

starUtils.facebook = function(d, s, id) {
    var js, fjs = d.getElementsByTagName(s)[0];
    if (d.getElementById(id)) return;
    js = d.createElement(s); js.id = id;
    js.src = "//connect.facebook.net/en_US/all.js#xfbml=1&appId=117287758301883";
    fjs.parentNode.insertBefore(js, fjs);
}
//(document, 'script', 'facebook-jssdk'));
