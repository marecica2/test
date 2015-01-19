
star.moreComments = function(params, dashboard){
    star.comments.first += star.comments.size;
    params += "first="+star.comments.first+"&count="+star.comments.count+"&";

    $("#spinnerComments").show();
    $("#moreResultsComments").hide();
    starServices.getComments(params, function(data){

        var html = star.renderComments(data, dashboard);
        setTimeout(function(){
            $("#spinnerComments").hide();
            $("#moreResultsComments").show();
            $("#comments-container").append(html);
            if(typeof(FB) !== "undefined")
                FB.Canvas.setSize();
        }, 250);
    });
}

star.loadComments = function(params, dashboard){
    star.comments = {};
    star.comments.size = 4;
    star.comments.first = 0;
    star.comments.count = star.comments.size;
    
    params += "first="+star.comments.first+"&count="+star.comments.count+"&";
    
    $("#spinnerComments").show();
    $("#moreResultsComments").hide();
    
    starServices.getComments(params, function(data){
        $("#comments-container").html("");
        
        var html = star.renderComments(data, dashboard);
        setTimeout(function(){
            $("#spinnerComments").hide();
            $("#moreResultsComments").show();
            $("#comments-container").append(html);
            if(typeof(FB) !== "undefined")
                FB.Canvas.setSize();
        }, 250);
    });
    
    $(window).scroll(function() {
        var diff = Math.abs($(window).scrollTop() - ($(document).height() - $(window).height()));
        if(diff < 1) {
            $("#moreResultsComments").click();              
        }
   });     
}

star.renderComments = function(data, dashboard){
    var html = "";
    for(var i = 0; i < data.length; i++){
        var item = data[i];
        $(".commentsContainer").show();
        html += "<div class='image-box shadow-blur mb-20 object-non-visible animated object-visible fadeInLeft' data-animation-effect='fadeInLeft' data-effect-delay='300'>";
        html += "   <div class='image-box-body'>";
        
        if(item.isDeletable != undefined && item.isDeletable){
            html += "           <a data-href='/event/comment/delete?uuid="+item.uuid+"' style='position:absolute;right:15px;top:10px' class='btn btn-light-gray btn-short comment-delete'><i class='color-link fa fa-times'></i></a>";
        }  
        
        html += "       <div class='title'>";
        html += "           <img src='../"+item.createdByAvatarUrl+"_64x64' class='img-circle' style='height:40px;float:left;margin-right:10px'> ";
        html += "           <a href='/user/"+item.createdByLogin+"'>"+item.createdByName+"</a>";
        html += "           <p><span class=''>on "+starUtils.formatDate(item.created)+"</span> ";
        html += "           <span class='tip'>"+starUtils.formatTime(item.created)+"</span></p>";
        html += "       </div>";
        
        if(dashboard && item.event != undefined){
            html += "           <div class='overlay-container'>";
            html += "               <div style='width:100%;height:160px;background: url(\"/"+item.listingImage+"\"); background-size: cover;padding:10px'>";
            html += "                   <h3 class='shadow margin-clear text-shadow'>"+item.listingName+"</h3>";
            html += "                   <span class='text-shadow'>"+starUtils.formatDate(item.eventStart)+"</span>";
            html += "                   <span class='text-shadow'>"+starUtils.formatTime(item.eventStart)+"</span> - ";
            html += "                   <span class='text-shadow'>"+starUtils.formatTime(item.eventEnd)+"</span>";
            html += "               </div>";
            html += "               <div class='overlay'>";
            html += "                   <div class='overlay-links'>";
            html += "                       <a href='/event/"+item.event+"'><i class='fa fa-link'></i></a>";
            html += "                   </div>";
            html += "               </div>";
            html += "           </div>";
        }
        
        else if(dashboard && item.listing != undefined){
            html += "           <div class='overlay-container'>";
            html += "               <div style='width:100%;height:160px;background: url(\"/"+item.listingImage+"\"); background-size: cover;padding:10px'>";
            html += "                   <h3 class='shadow margin-clear text-shadow'>"+item.listingName+"</h3>";
            html += "               </div>";
            html += "               <div class='overlay'>";
            html += "                   <div class='overlay-links'>";
            html += "                       <a href='/channel/"+item.listing+"'><i class='fa fa-link'></i></a>";
            html += "                   </div>";
            html += "               </div>";
            html += "           </div>";
        }
        
        html += "       <div class='black margin-top'>"+item.comment+"</div>"; 
        
        if(item.attachments.length > 0){
            html += "           <strong>"+i18n('attachments')+"</strong>";
            html += "           <table>";
            for(var j = 0; j < item.attachments.length; j++){
            var a = item.attachments[j];
            html += "               <tr>";
            html += "                   <td>";
            html += "                       <span><i class='fa fa-file'></i> <a href='/public/uploads/"+a.path+"' download='"+a.name+"'>"+a.name+"</a></span>";
            html += "                       </td>";
            html += "                       <td>";
            html += "                       &nbsp;&nbsp;"+starUtils.formatFilesize(a.size)+"";
            html += "                       </td>";
            html += "               </tr>";
            }
            html += "           </table>";
        }        
        
        if(item.replies.length > 0){
            if(item.attachments.length > 0)
                html += "<br/>";
            html += "<div class='small'>";
            for(var j = 0; j < item.replies.length; j++){
                var r = item.replies[j];
                html += "<div class='padding-bottom'>";
                html += "   <img class='avatar16' src='/"+r.createdByAvatarUrl+"_32x32'> ";
                html += "   <a href=''>"+r.createdByName+"</a> <span class='opacity'>" + starUtils.formatDate(r.created) + " " + starUtils.formatTime(r.created) + "</span><br/>";
                html +=     r.comment;
                html += "</div>";
            }
            html += "</div>";
        }   

        if(star.user && starCalendar.comments){
            html += "<a href='#' class='comment-reply pull-right'>"+i18n("write-a-reply")+"</a>";
            html += "<div style='display:none' class='comment-reply-input margin-top'>";
            html += "   <textarea class='form-control' placeholder='"+i18n('write-a-reply')+"'></textarea>";
            html += "   <button class='btn btn-default pull-right comment-reply-submit' data-id='"+item.uuid+"'>"+i18n('submit')+"</button>";
            html += "</div>";
            html += "<div class='clearfix'></div>"
        }
        
        html += "   </div>";
        html += "</div> ";        
    }
    return html;
}

star.initItems = function(prefix, urlParams){
    $(document).ready(function(){
        star[prefix] = {};
        star[prefix].size = 4;
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
            var html = star.renderItems(data, prefix);
            var f = function(html){
                $(html).hide().appendTo($("#itemsList"+prefix)).fadeIn(500);
                $("#spinner"+prefix).hide();
                $("#moreResults"+prefix).show();
                if(typeof(FB) !== "undefined")
                    FB.Canvas.setSize();
            }
            setTimeout(function(){
                f(html);
            }, 250);
        });

        // append elements        
        //$(window).unbind('scroll');
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
        var html = star.renderItems(data, urlParams.prefix);
           var f = function(html){
                $(html).hide().appendTo($("#itemsList"+urlParams.prefix)).fadeIn(500);
                $("#spinner"+urlParams.prefix).hide();
                $("#moreResults"+urlParams.prefix).show();
                if(typeof(FB) !== "undefined")
                    FB.Canvas.setSize();
           }
        setTimeout(function(){
                f(html);
          }, 250);
    });
};

star.renderItems = function(data, prefix){
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
        if(!item.invisible)
            $(".container"+prefix).show();
        if(prefix.indexOf("Listing") >= 0){
            html += "<div class='event-box shadow-blur image-box mb-20 object-non-visible animated object-visible fadeInLeft' data-animation-effect='fadeInLeft' data-effect-delay='300'>";
            html += "   <div class='overlay-container'>";
            html += "       <mark style='position:absolute;bottom:0px;padding:5px' class='title'><img class='avatar16 img-circle' style='margin:3px 5px 0 0; float:left' src='/"+item.createdByAvatarUrl+"_32x32'>"+item.createdByName+" </mark>";
            html += "       <img src='/"+item.imageUrl+"_128x128' alt=''>";
            html += "       <div class='overlay'>";
            html += "           <div class='overlay-links'>";
            html += "               <a href='/channel/"+item.uuid+"'><i class='fa fa-link'></i></a>";
            html += "               <a href='/user/"+item.createdByLogin+"'><i class='icon-user'></i></a>";
            html += "           </div>";
            html += "       </div>";
            html += "   </div>";
            html += "   <div class='image-box-body'>";
            html += "       <div style='height:45px;'><small>"+i18n(item.category)+" &middot; "+i18n(item.type)+"</small></div> ";
            
            
            if(item.charging == 'free'){
                html += "<strong>"+i18n("free")+"</strong>";
            }
            if(item.charging != 'free'){
                html += i18n("from")+"&nbsp;<strong>"+item.price+"</strong>&nbsp;"+item.currency+"&nbsp;";
                if(item.firstFree != undefined && item.firstFree == true){
                    html += " <small class='label default-bg'>First free</small>";
                }       
            }       
            
            html += "   <p class='event-box-links'><span><a href='/channel/"+item.uuid+"' class='link-left'><span>"+star.utils.trimTo(item.title, 40)+"</span></a></span></p>";
            html += "   <div class='event-box-link' style='position:fixed;bottom:15px'>";            
            for(var j = 0; j < 5; j++){
                if(j < item.ratingAvg)
                    html += "<i class='fa fa-star' data-value='1'></i>";
                else
                    html += "<i class='fa fa-star-o' data-value='1'></i>";
            }
            html += " " + item.ratingCount + " "+ i18n("reviews");
            
            html += "   </div>";
            
            html += "   </div>";
            html += "</div>"; 
            
            
        } else {
            var end = data[i].eventStart;
            var distance = end - now;
            var before = distance > 0 ? false : true;
            var days = Math.abs(Math.floor(distance / _day));
            if(before)
                days = days - 1;
            var hours = Math.abs(Math.floor( (distance % _day ) / _hour ));
            var minutes = Math.abs(Math.floor( (distance % _hour) / _minute ));
            var seconds = Math.abs(Math.floor( (distance % _minute) / _second ));

            var color = "#3a87ad";
            if(data[i] != undefined && data[i].color != undefined && data[i].color.length > 0)
                color = data[i].color;
            if(!data[i].isOwner)
                color = "darkgray";
            
            var item = data[i];
            if(item.isOwner || item.uuid != undefined){
                html += "<div class='event-box shadow-blur image-box mb-20 object-non-visible animated object-visible fadeInLeft' data-animation-effect='fadeInLeft' data-effect-delay='300'>";
                html += "   <div class='overlay-container'>";
                html += "       <mark style='position:absolute;bottom:0px;padding:5px' class='title'><img class='avatar16 img-circle' style='margin:3px 5px 0 0; float:left' src='/"+item.createdByAvatarUrl+"_32x32'>"+item.createdByName+" </mark>";
                html += "       <img src='/"+item.imageUrl+"' alt=''>";
                html += "       <div class='overlay'>";
                html += "           <div class='overlay-links'>";
                html += "               <a href='/event/"+item.uuid+"'><i class='fa fa-link'></i></a>";
                html += "               <a href='/user/"+item.createdByLogin+"'><i class='icon-user'></i></a>";
                html += "           </div>";
                html += "       </div>";
                html += "   </div>";
                html += "   <div class='image-box-body' style='font-size:0.9em'>";
                html += "           <div style='height:45px;'>";
                html += "               <span>"+i18n(item.category)+"</span> &middot; <span>"+i18n(item.type)+"</span>";
                if(item.state == 'customer_created'){
                    html += "       <img class='img-circle avatar22' src='/"+item.customerAvatarUrl+"_32x32'>";
                    html += "       <span class='label label-danger'>"+i18n("waiting")+"</span>";
                }
                html += "           </div>";
               
                html += "           <p>";
                html += "           <strong>"+starUtils.formatDate(item.eventStart)+"</strong>";
                html += "           <span>"+starUtils.formatTime(item.eventStart)+"</span> - ";
                html += "           <span>"+starUtils.formatTime(item.eventEnd)+"</span><br/>";
                html += "           <strong>"+(before? i18n("before") : i18n("starts-in"))+"</strong> " + (days > 0 ? (days + " " + i18n("days")) : "") + " " + (hours > 0 ? (hours + " " + i18n("hrs")) : "") + " " + minutes + " " + i18n("min");          
                html += "           <br/>";            
                if(item.charging == 'free'){
                    html += "<strong>"+i18n("free")+"</strong>";
                }
                if(item.charging == 'before'){
                    html += "       <strong>"+item.priceTotal+"</strong> <small>"+item.currency+"</small> " + i18n("for") + " " +item.chargingTime+" " + i18n("min");
                }    
                html += "           </p>";
                
                html += "           <a style='position:fixed;bottom:15px' data-placement='top' title='"+item.title+"' href='/event/"+item.uuid+"'><span>"+star.utils.trimTo(item.title, 30)+"</span></a>";
                html += "   </div>";
                html += "</div>";                           
            }
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
       html += ("<i class='fa fa-comments' title='"+i18n("p2p")+"'></i> ");
    }
    if(event.type == "broadcast"){
        html += ("<i class='fa fa-globe' title='"+i18n("broadcast")+"'></i> ");
    }
    if(event.type == "instant"){
        html += ("<i class='fa fa-globe' title='"+i18n("instant")+"'></i> ");
    }
    if(event.type == "hangout"){
        html += ("<i class='fa fa-globe' title='"+i18n("hangout")+"'></i> ");
    }
    if(event.type == "hangoutAir"){
        html += ("<i class='fa fa-globe' title='"+i18n("hangoutAir")+"'></i> ");
    }
    if(event.charging == "free"){
        html += ("<span ><i class='fa fa-ban' title='"+i18n("free")+"'></i></span> ");
    }
    if(event.charging == "before"){
        html += ("<span ><i class='fa fa-dollar' title='"+i18n("paid")+"'></i></span> ");
    }
    if(event.charging == "after"){
        html += ("<span ><i class='fa fa-dollar' title='"+i18n("paid")+"'></i></span> ");
    }
    if(event.privacy == "public"){
        html += ("<i class='fa fa-unlock' title='"+i18n("public")+"'></i> ");
    } 
    if(event.privacy == "private"){
        html += ("<i class='fa fa-lock' title='"+i18n("private")+"'></i> ");
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
                    html += "<img class='avatar22 img-circle' src='/"+data[i].avatar+"_32x32'>&nbsp;";
                }
                
                if(starCalendar.login != data[i].email)
                    html += "<a href='/user/"+data[i].email+"' title='"+data[i].email+"'>";
                
                if(starCalendar.login == data[i].email){
                    html += i18n("you") + " &nbsp;";
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

                if(starCalendar.selectedEvent.charging != "free" && starCalendar.selectedEvent.isOwner){
                    html += "<td style='text-align:center;'>";
                    html += data[i].paid ? "<p class='label label-success'>paid</p>" : "<p class='label label-default'>unpaid</p>";
                    html += "</td>";
                }
                
                html += "<td style='text-align:center;'>";
                if(data[i].result == "accepted"){
                    html += "&nbsp;<p class='label label-success'>"+i18n("accepted")+"</p>&nbsp;";
                } else if(data[i].result == "declined"){
                    html += "&nbsp;<p class='label label-danger'>"+i18n("declined")+"</p>&nbsp;";
                } else {
                    html += "&nbsp;<p class='label label-default'>"+i18n("waiting")+"</p>&nbsp;";
                }
                html += "</td>";
                html += "<td style='text-align:right;vertical-align:middle;'>";
                
                html += "</td>";
                html += "</tr>";
                $(this).html(html);
            }
        });
        
        if(cb != undefined)
            cb();
    });
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
            
            html += "<td style='vertical-align:top;width:32px'>";
            
            var avatar = success[i].byCustomer ? success[i].customerAvatar : success[i].userAvatar;
            if(avatar != prev || i == 0){
                html += "<img class=' avatar32 img-circle' style='' src='/"+avatar+"'>";
            } else {
                html +="<div style='width:32px'>&nbsp;</div>"
            }
            prev = avatar;
            html += "</td>";
            html += "<td style='padding-left:5px;vertical-align:top;'>";
            html += "<div style='font-size:0.8em;opacity:0.6'>";
            var url = success[i].byCustomer ? "/user/"+success[i].customerLogin : "/user/"+success[i].userLogin;
            var name = success[i].byCustomer ? success[i].customer : success[i].user;
            html += "<a class='' href='" + url +"'>" + name + "</a>&nbsp; " + i18n("on");
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


starEvent.updateStyle = function(data){
    starServices.updateStyle(data, function(){console.log("success");});
}



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
        url = "channels-get";
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

starServices.updateStyle = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/settings/style",
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

starServices.getComments = function(params, success, error){
    $.ajax({
        type: "GET",
        url: "/comments?"+params,
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

starServices.addCommentReply = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/comment/reply",
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.hangoutInvite = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/room/invite",
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

starUtils.formatFilesize = function(fileSizeInBytes) {
    var i = -1;
    var byteUnits = [' kB', ' MB', ' GB', ' TB', 'PB', 'EB', 'ZB', 'YB'];
    do {
        fileSizeInBytes = fileSizeInBytes / 1024;
        i++;
    } while (fileSizeInBytes > 1024);
    return Math.max(fileSizeInBytes, 0.1).toFixed(1) + byteUnits[i];    
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
