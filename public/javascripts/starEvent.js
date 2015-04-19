
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
        }, 10);
    });
}

star.loadComments = function(params, dashboard){
    star.comments = {};
    star.comments.size = 9;
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

        if(dashboard && item.event != undefined){
            html += "           <div class='overlay-container'>";
            html += "               <div style='width:100%;height:150px;background: url(\"/"+item.listingImage+"\"); background-size: cover;padding:10px'>";
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
            html += "               <div style='width:100%;height:150px;background: url(\"/"+item.listingImage+"\"); background-size: cover;padding:10px'>";
            html += "               </div>";
            html += "               <div class='overlay'>";
            html += "                   <div class='overlay-links'>";
            html += "                       <a href='/channel/"+item.listing+"'><i class='fa fa-link'></i></a>";
            html += "                   </div>";
            html += "               </div>";
            html += "           </div>";
        }
        
        html += "   <div class='image-box-body'>";
        if(item.isDeletable != undefined && item.isDeletable){
            html += "           <a data-href='/event/comment/delete?"+star.token+"&uuid="+item.uuid+"' style='position:absolute;right:15px;cursor:pointer;opacity:0.3' class='comment-delete'><i class='fa fa-times'></i></a>";
        }  
        var hr = false;
        if(dashboard && item.listingName != undefined){
            html += "                   <h3 class='shadow margin-top-clear '>"+item.listingName+"</h3>";
            hr = true;
        }
        if(dashboard && item.event != undefined){
            html += "       <span class=''>"+starUtils.formatDate(item.eventStart)+"</span>";
            html += "       <span class=''>"+starUtils.formatTime(item.eventStart)+"</span> - ";
            html += "       <span class=''>"+starUtils.formatTime(item.eventEnd)+"</span>";
            hr = true;
        }         
        if(hr)
            html += "       <hr>";
        
        html += "       <div class='title'>";
        html += "           <img src='../"+item.createdByAvatarUrl+"_64x64' class='img-circle' style='height:40px;float:left;margin-right:10px'> ";
        html += "           <span><a href='/user/"+item.createdByLogin+"'>"+item.createdByName+"</a></span>";
        html += "           <p><span class=''>on "+starUtils.formatDate(item.created)+"</span>, ";
        html += "           <span style='opacity:0.6'>"+starUtils.formatTime(item.created)+"</span></p>";
        html += "       </div>";
        
        html += "       <div class='black'>"+item.comment+"</div>"; 
        
        
        if(item.attachments.length > 0){
            html += "           <strong>"+i18n('attachments')+"</strong>";
            html += "           <table>";
            for(var j = 0; j < item.attachments.length; j++){
            var a = item.attachments[j];
            html += "               <tr>";
            html += "                   <td>";
            html += "                       <span><i class='fa fa-file'></i> <a target='_blank' href='/public/uploads/"+a.path+"'>"+a.name+"</a></span>";
            html += "                       </td>";
            html += "                       <td>";
            html += "                       &nbsp;&nbsp;"+starUtils.formatFilesize(a.size)+"";
            html += "                       </td>";
            html += "                       <td>";
            html += "                       &nbsp;&nbsp;<span><a target='_blank' href='https://docs.google.com/gview?url="+star.baseUrl+"/public/uploads/"+a.path+"'&embedded=true'><i class='fa fa-search'></i></a></span>";
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
                html +=     linkify(r.comment);
                html += "</div>";
            }
            html += "</div>";
        }   

        if(item.objectType == "user" || (star.user && item.commentsEnabled)){
            html += "<a href='#' class='comment-reply link pull-right'>"+i18n("write-a-reply")+"</a>";
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
        star[prefix].size = 9;
        star[prefix].first = 0;
        star[prefix].count = star[prefix].size;

        // initial get elements
        var url = "first="+star[prefix].first+"&count="+star[prefix].count+urlParams;
        var params = {};
        params.url = url;
        params.prefix = prefix;
        $("#moreResults"+prefix).hide();

        starServices.getItems(prefix, params, function(data){
            $("#moreResults"+prefix).hide();
            $("#spinner"+prefix).show();
            var html = star.renderItems(data, prefix);
            var f = function(html){
                $("#spinner"+prefix).hide();
                $(html).hide().appendTo($("#itemsList"+prefix)).fadeIn(500);
                $("#moreResults"+prefix).show();
                if(typeof FB !== "undefined")
                    FB.Canvas.setSize();
            }
            f(html);
        });

        // append elements        
        //$(window).unbind('scroll');
        $(window).scroll(function() {
            var diff = Math.abs($(window).scrollTop() - ($(document).height() - $(window).height()));
            if(diff < 1) {
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
    $("#moreResults"+prefix).hide();
    $("#spinner"+prefix).show();
    starServices.getItems(prefix, urlParams, function(data){
        var html = star.renderItems(data, urlParams.prefix);
           var f = function(html){
                $(html).hide().appendTo($("#itemsList"+urlParams.prefix)).fadeIn(100);
                $("#spinner"+urlParams.prefix).hide();
                $("#moreResults"+urlParams.prefix).show();
           }
           f(html);
    });
};

star.renderItems = function(data, prefix){

    var html = "";
    for(var i = 0; i < data.length; i++){
        var item = data[i];
        var listing = prefix.indexOf("Listing") >= 0 ? true : false;
        $(".container"+prefix).show();
    
        var url = "";
        if(listing)
            url = "/channel/"+item.uuid;
        else
            url = "/event/"+item.uuid;
        
        html += "<div itemscope itemtype='http://schema.org/Event' class='anchorlink pointer event-box2 shadow-blur image-box mb-20 object-non-visible animated object-visible fadeInLeft' data-href='"+url+"' data-animation-effect='fadeInLeft' data-effect-delay='300'>";
        html += "   <div class='overlay-container'>";
        
        html += "       <a href='"+url+"'>";
        html += "       <div style='background:url(\"/"+item.imageUrl+"_128x128\"); height:128px; width:100%; background-size: cover' class='shadow-inset-2'></div>";
        html += "       <span style='display:none' itemprop='url'>"+url+"</span>";
        html += "       <span style='display:none' itemprop='startDate'>2016-01-01</span>";
        html += "       <div style='position:absolute;bottom:5px;right:0px;width:270px'>";
        html += "           <div style='font-weight:bold;' class='white' itemprop='name'>"+star.utils.trimTo(item.title, 50)+"</div>";
        html += "       </div>";            
        html += "       </a>";            

        html += "   </div>";
        
        html += "   <meta itemprop='image' content='https://wid.gr/public/"+item.imageUrl+"_128x128'>";
        html += "   <div class='image-box-body' style='position:relative'>";
        
        html += "       <div style='position:absolute;top: -34px; width:100%'>";
        html += "           <img class='avatar64 img-circle' style='border:3px solid #fafafa; float:left; margin-right:10px' href='"+url+"' src='/"+item.createdByAvatarUrl+"_64x64'>"
        html += "       </div>";
        
        html += "       <div style='position:absolute;top:0px;right:0px;width:270px'>";
        if(item.available)
            html += "       <i class='fa fa-circle' style='color:green'></i>";            
        html += "           <small itemprop='performer' itemscope itemtype='http://schema.org/Person'><strong><a href='/user/"+item.createdByLogin+"' itemprop='name'>"+item.createdByName+"</a></strong></small>";
        if(item.language != null) 
            html += "       &middot; <img src='/public/images/flags/"+item.language+".gif'>"; 
        html += "           &middot; <small>"+i18n(item.category)+"</small>"
        html += "       </div>";
        
        if(listing)
            html += "       <p class='left' itemprop='description' style='font-size:0.9em;margin-top:20px'>" + star.utils.trimTo(item.description, 90) + "</p>";
        else
            html += starEvent.renderEventBody(item);
        
        html += "<div style='position:fixed;bottom:15px;left:15px'>";
        if(item.charging == 'free'){
            html += "<strong>"+i18n("free")+"</strong>";
        }
        
        html += "<span itemprop='offers' itemscope itemtype='http://schema.org/Offer'>";
        if(item.charging != 'free'){
            if(listing)
                html += i18n("from")+"&nbsp;<strong itemprop='price'>"+item.price+"</strong>&nbsp;"+item.currency+"&nbsp;";
            else
                html += "<strong itemprop='price'>"+item.priceTotal+"</strong>&nbsp;"+item.currency+"&nbsp;"+i18n("for")+" "+item.chargingTime+ " "+i18n("min") + " ";
            if(item.firstFree != undefined && item.firstFree == true){
                html += "<small class='label default-bg'>First free</small>";
            }       
        }    
        html += "</span>";

        if(listing){
            html += "<br/>";
            for(var j = 0; j < 5; j++){
                if(j < Math.round(item.ratingAvg))
                    html += "<i class='fa fa-star text-default' data-value='1'></i>";
                else
                        html += "<i class='fa fa-star text-light' data-value='1'></i>";
            }
            html += " <small>" + item.ratingCount + " "+ i18n("reviews") + "</small>";
        }
        html += "</div>";

        
        
        html += "   </div>";
        html += "</div>"; 
    }
    return html    
};



starEvent.renderEventBody = function(item){
    var current_date = new Date().getTime();
    var days, hours, minutes, seconds;
    var _second = 1000;
    var _minute = _second * 60;
    var _hour = _minute * 60;
    var _day = _hour *24
    var now = new Date();
    
    var end = item.eventStart;
    var distance = end - now;
    var before = distance > 0 ? false : true;
    var days = Math.abs(Math.floor(distance / _day));
    var hours = Math.abs(Math.floor( (distance % _day ) / _hour ));
    if(before){
        days = days - 1;
        hours = hours - 1;
    }
    var minutes = Math.abs(Math.floor( (distance % _hour) / _minute ));
    var seconds = Math.abs(Math.floor( (distance % _minute) / _second ));
    var html = "";
    html += "<div style='margin-top:20px;font-size:0.9em'>";
    html += "<span>"+starUtils.formatDate(item.eventStart)+"</span> ";
    html += "<span>"+starUtils.formatTime(item.eventStart)+"</span> - ";
    html += "<span>"+starUtils.formatTime(item.eventEnd)+"</span>";                
    html += "<br/>";
    html += "<strong>"+(before? i18n("before") : i18n("starts-in"))+"</strong> ";
    
    if(days > 1)
        html += days + " " + i18n("days");
    else {
        
        if(days > 0)
            html += days + " " + i18n("days")  + " "; 
        if(hours > 0)
            html += hours + " " + i18n("hrs") + " "; 
        if(minutes > 0)
            html += minutes + " " + i18n("min") + " ";
    }
    html += "<br/>";
            
    
    html += "</div>";
    return html;
};

starEvent.getFiles = function(){
    var params = "";
    starServices.getFiles(params, function(data){
        var html = starEvent.renderFiles(data);
        $("#filesContainer").html(html);
    });    
};

starEvent.deleteFile = function(elm){
    var params = "uuid="+$(elm).attr("data-uuid");
    if(confirm(i18n("delete-selected-item"))){
        starServices.deleteFile(params, function(data){
            starEvent.getFiles();
        });    
    }
};

starEvent.library = [];
starEvent.selectFile = function(elm){
    starEvent.library.push($(elm).attr("data-uuid"));
    $("#fileUploadComments").val($(elm).attr("data-uuid"));
    $("#typeComments").val("library");
    if($(elm).attr("data-contentType").indexOf("image") != -1){
        $("#images").append("<img class='img-thumbnail' style='height:100px;margin:4px' src='/public/uploads/"+$(elm).attr("data-url")+"_thumb'>");
    } else {
        $("#images").append("<span class='label default_bg'><i class='fa fa-file'></i> "+$(elm).attr("data-file")+"</span><br/>");
    }    
};

starEvent.renderFiles = function(data){
    var size = 0;
    var html = "<table class='table table-condensed table-hover' style='font-size:0.9em;'>";
    for(var i = 0; i < data.length; i++){
        html += "<tr>";
        html += "<td>";
        if(typeof star.media == "undefined")
            html += "<a class='btn btn-default btn-sm btn-short btn-margin' rel='tooltip' title='"+i18n("select-this-file")+"' data-file='"+data[i].name+"' data-uuid='"+data[i].uuid+"' data-contentType='"+data[i].contentType+"'  data-url='"+data[i].url+"' onclick='starEvent.selectFile(this)'><i class='fa fa-check'></i></a>";
        html += "&nbsp;&nbsp;<a target='_blank' title='"+data[i].name+"' href='/public/uploads/"+data[i].url+"'>"+star.utils.trimTo(data[i].name, 20)+"</a>";
        html += "</td>";
        html += "<td>"+starUtils.formatFilesize(data[i].size)+"</td>";
        html += "<td>"+starUtils.formatDate(data[i].created) + ", " +starUtils.formatTime(data[i].created)+"</td>";
        html += "<td>";
        html += "<a style='cursor:pointer' class='btn btn-light-gray btn-sm btn-margin'  data-uuid='"+data[i].uuid+"' onclick='starEvent.deleteFile(this)'><i class='fa fa-times'></i></a> ";
        html += "</td>";
        html += "</tr>";
        size += data[i].size;
    }
    html += "</table>";
    
    var percentage = (size*100)/104857600;
    var limit = "";
    limit += "" + starUtils.formatFilesize(size) + " (max 100MB, max 10MB per file)";
    limit += '<div class="progress"><div class="progress-bar progress-bar-default" role="progressbar" aria-valuenow="'+percentage+'" aria-valuemin="0" aria-valuemax="100" style="width: '+percentage+'%;"></div></div><br/>'
    $("#fileSizeContainer").html(limit);
    return html;
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
                    html += data[i].paid ? "<p class='label label-success'>"+i18n("paid")+"</p>" : "";
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
            
            var url = success[i].byCustomer ? "/user/"+success[i].customerLogin : "/user/"+success[i].userLogin;
            var avatar = success[i].byCustomer ? success[i].customerAvatar : success[i].userAvatar;
            if(avatar != prev || i == 0){
                html += "<a href='"+url+"'><img class=' avatar32 img-circle' style='' src='/"+avatar+"'></a>";
            } else {
                html +="<div style='width:32px'>&nbsp;</div>"
            }
            prev = avatar;
            html += "</td>";
            html += "<td style='padding-left:5px;vertical-align:top;'>";
            html += "<div style='font-size:0.8em;opacity:0.6'>";
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
 $.ajax({
     type: "POST",
     url: "/event/save?"+star.token,
     data: JSON.stringify(data),
     success: success,
     error: error,
     contentType: "application/json"
 });
};

starServices.updateEvent = function(data, success, error){
 $.ajax({
     type: "POST",
     url: "/event/update?"+star.token,
     data: JSON.stringify(data),
     success: success,
     error: error,
     contentType: "application/json"
 });
};

starServices.deleteEvent = function(data, success, error){
    $.ajax({
        type: "DELETE",
        url: "/event/delete?"+star.token,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.inviteEvent = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/event/invite?"+star.token,
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
        url: "/"+url+"?"+star.token+"&"+params.url,
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.getEvent = function(params, success, error){
    $.ajax({
        type: "GET",
        url: "/event?"+star.token+"&"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.createAttendance = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/attendance/save?"+star.token,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.updateAttendance = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/attendance/update?"+star.token,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.updateStyle = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/settings/style?"+star.token,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.deleteAttendance = function(data, success, error){
    $.ajax({
        type: "DELETE",
        url: "/attendance/delete?"+star.token,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.getAttendances = function(params, success, error){
    $.ajax({
        type: "GET",
        url: "/attendances?"+star.token+"&"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};


starServices.getActivities = function(params, success, error){
    $.ajax({
        type: "GET",
        url: "/public/activities?"+star.token+"&"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.getFiles = function(params, success, error){
    $.ajax({
        type: "GET",
        url: "/files?"+star.token+"&"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.deleteFile = function(params, success, error){
    $.ajax({
        type: "DELETE",
        url: "/files?"+star.token+"&"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.addComment = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/event/comment?"+star.token,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
   };

starServices.getComments = function(params, success, error){
    $.ajax({
        type: "GET",
        url: "/comments?"+star.token+"&"+params,
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.deleteComment = function(data, success, error){
    $.ajax({
        type: "DELETE",
        url: "/event/comment/delete?"+star.token,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.addCommentReply = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/comment/reply?"+star.token,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};

starServices.hangoutInvite = function(data, success, error){
    $.ajax({
        type: "POST",
        url: "/room/invite?"+star.token,
        data: JSON.stringify(data),
        success: success,
        error: error,
        contentType: "application/json"
    });
};


