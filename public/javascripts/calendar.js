
$(document).ready(function() {
    var lang = starUtils.getCookie("PLAY_LANG");
        
    if (window!=window.top) {
        $(".button-fullscreen").show();
    }
    
    $(".event-dialog-save").click(starEvent.saveEventDialog);
    $(".event-dialog-save-notify").click(starCalendar.saveNotifyEventDialog);
    $(".event-dialog-create").click(starCalendar.popupSaveNewEvent);
    $(".event-dialog-propose").click(starCalendar.popupSaveNewEventPropose);
    $(".event-dialog-create-edit").click(starCalendar.popupSaveNewEventEdit);
    $("#dialog-invite-button").click(starEvent.inviteAdd);
    $(document).on("click", ".dialog-invite-delete", starEvent.inviteDelete);
    $(document).on("click", ".dialog-invite-accept", starEvent.inviteAccept);
    $(document).on("click", ".dialog-invite-decline", starEvent.inviteDecline);

    $(document).on("click", ".fc-button", function(){
        var cal = "";
        var click = false;
        if($(this).hasClass("fc-button-agendaWeek")){
            click = true;
            cal = "agendaWeek";
        }
        if($(this).hasClass("fc-button-agendaDay")){
            click = true;
            cal = "agendaDay";
        }
        if($(this).hasClass("fc-button-month")){
            click = true;
            cal = "month";
        }
        if(click){
            var data = {};
            data.type = cal; 
            starServices.setAgendaType(data, null, null);
        }
    });    

    
    $(".dialog-event-edit").click(function(){
        if($(this).attr("edit") == undefined){
            $(".event-view").hide();
            $(".event-edit").show();
            $(this).attr("edit", true);
            $(this).removeClass("fa-pencil");
            $(this).addClass("fa-times");
        } else {
            $(".event-view").show();
            $(".event-edit").hide();
            $(this).removeAttr("edit");
            $(this).removeClass("fa-times");
            $(this).addClass("fa-pencil");
        }
    });

    $(".facebook-share").click(function(){
        FB.ui(
            {
             method: 'feed',
             name: starCalendar.selectedEvent.title,
             caption: "com-manager",
             description: (starCalendar.selectedEvent.description),
             link: document.URL,
             picture: location.origin+"/public/images/logo.jpg"
            },
            function(response) {
              if (response && response.post_id) {
                console.log('Post was published.');
              } else {
                console.log('Post was not published.');
              }
            }
         ); 
    });

    $(".event-charging").change(function(){
        if("free" == starUtils.getRadio("event-charging"))
            $(".price-container").hide();
        else
            $(".price-container").show();
        
    });
    
    $("#popup-close").click(function(){
        starCalendar.calendar.fullCalendar('unselect');
        var popup = $("#myPopover");
        $('#myPopover').modal('hide');
        //popup.hide();
    });

    
    $(document).on("click", ".popup-event-edit", function(){
        starCalendar.calendar.fullCalendar('rerender');
        starCalendar.selectedEventId = event;
        var event = $(this).attr("data-event");
        if(!starCalendar.isPublic)
            window.location.replace("?event="+event);
        else
            window.location.replace("?id="+starCalendar.userUUID+"&event="+event);
    });
    
    
    $(".notify-check-all").change(function(){
        if($(this).prop("checked")){
            $('.notify-checkboxes').prop("checked", true);
        } else {
            $('.notify-checkboxes').prop("checked", false);
        }
    });

    
    $("#popup-event-delete").click(function(){
        var ok = confirm(i18n("app.confirmDeleteEvent"));
        if(ok){
            var event = {};
            event.uuid = starCalendar.selectedEvent.uuid;
            starServices.deleteEvent(event, function(){
                starCalendar.calendar.fullCalendar('refetchEvents');
                var popup = $("#myPopover");
                //popup.hide();
                $('#myPopover').modal('hide');
            }, null);
        }
    });

    
    $(".popup-event-notify").click(function(){
        var ok = confirm(i18n("app.confirmInvitation"));
        if(ok){
            console.log("Invitation send");
            //xxxx
            var d = {};
            d.uuid = starCalendar.selectedEvent.uuid;
            
            var arr = [];
            $(".notify-checkboxes").each(function() {
                var val = $(this).attr("data-uuid");
                if ($.inArray(val, arr) == -1 && $(this).prop("checked")) arr.push(val);
            });
            d.invites = arr;
            console.log(arr);
            
            $(".spinner-notify").show();
            starServices.inviteEvent(d, function(){
                var popup = $("#myPopover");
                popup.hide();
                $('#myPopover').modal('hide');
                $(".spinner-notify").hide();
            }, function(data){
                   console.log(data)
               });
        }
    });
    
    if(starCalendar.defaultView == "")
        starCalendar.defaultView = "agendaWeek";
    starCalendar.options = {
            header: {
                center: 'prev,today,next',
                left: 'title',
                right: 'month,agendaWeek,agendaDay'
            },
            buttonText: {
                today:    i18n('cal.today'),
                month:    i18n('cal.month'),
                week:     i18n('cal.week'),
                day:      i18n('cal.day')
            },
            monthNamesShort: [i18n('cal.jan'), i18n('cal.feb'), i18n('cal.mar'), i18n('cal.apr'), i18n('cal.may'), i18n('cal.jun'), i18n('cal.jul'), i18n('cal.aug'), i18n('cal.sep'), i18n('cal.oct'), i18n('cal.nov'), i18n('cal.dec')],
            monthNames: [i18n('cal.january'), i18n('cal.february'), i18n('cal.march'), i18n('cal.april'), i18n('cal.may'), i18n('cal.june'), i18n('cal.july'), i18n('cal.august'), i18n('cal.september'), i18n('cal.october'), i18n('cal.november'), i18n('cal.december')],
            dayNames: [i18n('cal.sunday'), i18n('cal.monday'), i18n('cal.tuesday'), i18n('cal.wednesday'), i18n('cal.thursday'), i18n('cal.friday'), i18n('cal.saturday')],
            dayNamesShort: [i18n('cal.sun'), i18n('cal.mon'), i18n('cal.tue'), i18n('cal.wed'), i18n('cal.thu'), i18n('cal.fri'), i18n('cal.sat')],
            allDaySlot: false,
            slotMinutes: 15,
            slotEventOverlap: true,
            selectable: starCalendar.isPublic && (!starCalendar.isLogged || starCalendar.userUUID == "") ? false : true,
            editable: starCalendar.isPublic && (!starCalendar.isLogged || starCalendar.userUUID == "") ? false : true,
            selectHelper: true,
            axisFormat: 'HH:mm',
            hiddenDays: starCalendar.hiddenDays,
            height: starCalendar.defaultView == "month" ? 700 : 2000 ,
            minTime: starCalendar.startHour,
            maxTime: starCalendar.endHour,
            defaultView: starCalendar.defaultView == undefined ? "agendaWeek" : starCalendar.defaultView,
            
            // handlers
            eventClick: starCalendar.clickEvent,
            eventMouseout: starCalendar.mouseOutEvent,
            eventMouseover: starCalendar.mouseOverEvent,
            select: function(start, end, allDay) {
                if(starCalendarInit.listings)
                    starCalendar.selectionNewEvent(calendar, start, end, allDay);
            },
            viewRender: function(view, element) {
                window.location.hash = "?from="+view.start.getTime()+"&to="+view.end.getTime();
            },
            eventResize: function(event,dayDelta,minuteDelta,revertFunc) {
                starEvent.updateEvent(event, true, null);
            },
            eventDrop: function(event,dayDelta,minuteDelta,allDay,revertFunc) {
                starEvent.updateEvent(event, true, null);
            },
            events: function(start, end, callback) {
                starCalendar.getEvents(start, end, callback);
            },
            eventAfterRender : function( event, element, view ){
                var paramEvent = starUtils.getParameterByName("event");
                if(paramEvent.length > 0 && starCalendar.showEvent == true && paramEvent == event.uuid){
                    starCalendar.selectedEvent = event;
                    starCalendar.selectedEventUUID = event.uuid
                    starCalendar.selectedEventId = event.uuid;
                    starCalendar.showDialog = true;
                }
                
                if(event.uuid == starCalendar.selectedEventId && starCalendar.showDialog){
                    starCalendar.showDialog = false;
                    starCalendar.showEvent = false;
                    $('#myModal').show();
                    $('#calendar').hide();
                    starCalendar.selectedEvent = event;
                    starCalendar.copyValuesToDialog(starCalendar.selectedEvent);
                    starCalendar.openEventDialog(event, element, view);
                }
                
                if(event.invisible == undefined || !event.invisible)
                    starCalendar.decorateEvent(event, element, view);
            },
            eventAfterAllRender: function(view){
                var lineElement = $(".fc-agenda-axis:contains('"+starCalendar.h+":00')");
                if(starCalendar.h < 10)
                    lineElement = $(".fc-agenda-axis:contains('0"+starCalendar.h+":00')");
                    
                if(starCalendar.mhalf)
                    lineElement = lineElement.parent().next().find(">:first-child");
                //lineElement.css("borderTop", "1px solid red");
                //lineElement.next().css("borderTop", "1px solid red");           
            }
    };
    
    // conditional options
    if(starCalendar.selectedEventDate != undefined){
        starCalendar.options.date = starCalendar.selectedEventDate.getUTCDate();
        starCalendar.options.month = starCalendar.selectedEventDate.getUTCMonth();
        starCalendar.options.year = starCalendar.selectedEventDate.getUTCFullYear();
    } 
    
    var date = new Date(parseInt(starUtils.getParameterByName("to")));
    if(!isNaN(date.getTime())){
        starCalendar.options.date = date.getUTCDate();
        starCalendar.options.month = date.getUTCMonth();
        starCalendar.options.year = date.getUTCFullYear();
    }
    
    var calendar = $('#calendar-div').fullCalendar(starCalendar.options);
    starCalendar.calendar = calendar;
});


//
// event handlers
//

starCalendar.dialogColorPick = function(hex) {
    $("#event-color").val(hex);
    $("#selectedColor").css("background-color",hex);
};


starCalendar.decorateEvent = function(event, element, view){
    var el = $(".fc-event-title", element);
    var text = el.text();
    var length = view.name == "month" ? 6 : 20;
    if(true){
        if(text.length >= length)
            text = text.substring(0, length) + "...";
        var html = starEvent.eventIcons(event);
        html = "<img height='15px' class='avatar16 shadow' src='/"+event.createdByAvatarUrl+"'> " + html;
        el.html(html + text);
    }
};




    
starCalendar.popupSaveNewEventPropose = function(){
    starCalendar.popupSaveNewEvent(true);
}

    
// popup save is clicked
starCalendar.popupSaveNewEvent = function(proposal){
    // save to backend
    var event = {};
    event.eventStart = starCalendar.start; 
    event.eventEnd = starCalendar.end;
    event.title = $("#popup-event-title").val();
    event.description = $("#popup-event-title").val();
    event.currency = starCalendar.defaultCurrency;
    event.type = "p2p";
    event.charging = "free";
    event.privacy = "private";
    event.listing = $("#popup-listing").val();
    
    if(starCalendar.userDisplayed != undefined && proposal != undefined && proposal == true){
        event.user = starCalendar.userDisplayed;
        event.proposal = true;
    } else {
        event.user = starCalendar.userUUID;
        event.proposal = false;
    }
    
    starServices.createEvent(event, function(successEvent){
        starCalendar.selectedEventUUID = successEvent.uuid;
        starCalendar.selectedEvent = successEvent;
        starCalendar.selectedEventId = successEvent.uuid;
        starCalendar.calendar.fullCalendar('refetchEvents');
        starEvent.inviteLoad(successEvent.uuid);
        // set share link url
        $(".event-link-share").each(function() {
            if(starCalendar.isPublic){
                $(this).val(window.location.origin + "/public/calendar" + $(this).attr("data-url")+successEvent.uuid+"&id="+starCalendar.userUUID);
            } else {
                $(this).val(window.location.origin + $(this).attr("data-url")+successEvent.uuid);
            }
        });
    }, null);
    starCalendar.calendar.fullCalendar('unselect');

    // hide popup
    var popup = $("#myPopover");
    $('#myPopover').modal('hide');
    //popup.hide();
};


// selection event
starCalendar.selectionNewEvent = function(ccc, start, end, allDay) {
    // create popup and fill data
    var popup = $("#myPopover");
    $('#myPopover').modal('show');
    
    // in case of month view create default time for new event 
    if(allDay){
        var now = new Date();
        start.setHours(now.getMinutes() >= 30 ? now.getHours()+1 : now.getHours());
        var minutes = now.getMinutes() >= 30 ? 0 : 30;
        start.setMinutes(minutes);
        end = new Date(start.getTime() + 1000*60*30);
    }
    
    starCalendar.calendar = ccc;
    starCalendar.start = start;
    starCalendar.end = end;
    starCalendar.allDay = allDay;

    $(".event-title").val(i18n("app.newEvent"));
    $(".event-title").html(i18n("app.newEvent"));
    $(".event-time-from").html(starUtils.formatDateTime(start));
    $(".event-time-to").html(starUtils.formatTime(end));
    
    // show popup after selection is made
    $(".popup-edit-event").hide();
    $(".popup-new-event").show();
    $(".popup-event-description").html("");
    $('#myPopover').modal('show');
    $("#popup-event-title").val("");
    $("#popup-event-title").focus();
};


starCalendar.clickEvent = function(event, jsEvent, view) {
    // show hide appropriate content
    starCalendar.selectedEvent = event;
    var popup = $("#myPopover");

    // copy values to the event detail page dialog
    if(jsEvent != undefined){
        starEvent.inviteLoad(event.uuid, function(){
            starCalendar.copyValuesToDialog(event);
            if(event.uuid.length > 0){
                $('#myPopover').modal('show');
            }
        });
    }
    
    // reinit facebook
    var url = document.location.origin + "/public/calendar/"+starCalendar.userUUID+"/"+event.uuid;
    var url2 = document.location.origin + "/public/calendar/"+starCalendar.userUUID+"#?event="+event.uuid;
    $(".fb-meta-title").attr("content", event.title);
    $(".fb-meta-description").attr("content", event.description);
    $(".event-anchor-comments").attr("href", url);
    $(".facebook-share-link").attr("href", url2);
    //$(".fb-meta-url").attr("content", url);
    //if(FB != undefined){
    //    FB.XFBML.parse();
    //}
    
    // prevent going to the url
    return false;
};

starCalendar.copyValuesToDialog = function(event){
    var popup = $("#myPopover");
    
    // event details in dialog
    $(".event-edit").hide();
    $(".event-view").show();
    var el = $(".event-detail-container");
    var html = "<span>" + (event.charging != "free" && event.price.length > 0 ? ("<span style='font-size:1em' class='label label-primary'>" + event.price + " " + event.currency + "</span> "): "") + starEvent.eventIcons(event) + "</span>";
    el.html(html);

    $(".popup-edit-event").show();
    $(".popup-new-event").hide();
    $("#modal-validation-price").hide();
    $('#myPopover').modal('hide');

    if(event.charging == "free")
        $(".price-container").hide();
    else
        $(".price-container").show();
    
    if(event.type == "p2p")
        $(".popup-event-type-p2p").show();
    else
        $(".popup-event-type-p2p").hide();
    if(event.type == "livecast")
        $(".popup-event-type-livecast").show();
    else
        $(".popup-event-type-livecast").hide();
    
    $(".popup-event-view").show();
    $(".popup-event-approvement").hide();
    if(event.state == 'customer_created' && event.isOwner){
        $(".popup-event-approvement").show();
    }
    
    if(!event.editable){
        $("#popup-event-edit").hide();
        $(".event-title-label").show();
        $(".event-description-label").show();
        $(".event-dialog-save-notify").hide();
        $("#popup-event-delete").hide();
        $(".popup-event-notify").hide();
    } else {
        $("#popup-event-edit").show();
        $(".event-title-label").hide();
        $(".event-description-label").hide();
        $(".event-dialog-save").show();
        $(".event-dialog-save-notify").show();
        $(".popup-event-notify").show();
        $(".notify-check-all").show();
        $("#popup-event-delete").show();
    }
        
    $(".popup-event-description").html(event.description);
    var html = "<span>" + (event.charging != "free" && event.price.length > 0 ? ("<span style='font-size:1em' class='label label-primary'>" + event.price + " " + event.currency + "</span> "): "") + starEvent.eventIcons(event) + "</span>";
    $(".popup-event-charging").html(html);
    
    $(".event-anchor").each(function() {
        if(starCalendar.isPublic){
            $(this).attr("href", $(this).attr("data-url")+event.uuid+"&id="+starCalendar.userUUID);
        } else {
            $(this).attr("href", $(this).attr("data-url")+event.uuid);
        }
    });
    
    $(".event-link-share").each(function() {
        if(starCalendar.isPublic){
            $(this).val(window.location.origin + "/public/calendar" + $(this).attr("data-url")+event.uuid+"&id="+starCalendar.userUUID);
        } else {
            $(this).val(window.location.origin + $(this).attr("data-url")+event.uuid);
        }
    });    
    
    $(".event-createdByName").html("<a href='/calendar/"+event.createdByLogin+"'><i class='fa fa-calendar'></i></a> <img class='avatar16' src='/"+event.createdByAvatarUrl+"'> <a href='/public/user?id="+event.createdBy+"'>" + event.createdByName + "</a>");
    $(".event-title").val(event.title);
    $(".event-title-label").val(event.title);
    $(".event-title-label").html(event.title);
    
    starCalendar.selectedEvent = event;
    
    if(event.currency == null || event.currency.length <= 0)
        $(".event-currency").val(starCalendar.defaultCurrency);
    else
        $(".event-currency").val(event.currency);
    
    $(".event-title").html(event.title);
    if(event.start != null)
        $(".event-time-from").html(starUtils.formatDate(event.start) + " " + starUtils.formatTime(event.start));
    if(event.end != null)
        $(".event-time-to").html(starUtils.formatTime(event.end));
    $("#event-description").html(event.description);
    $("#event-description-label").html(event.description);
    $(".event-charging-price").val(event.price);
    $("#event-color").val(event.backgroundColor);
    $("#selectedColor").css("background-color",event.backgroundColor);
    if(event.backgroundColor == null || event.backgroundColor.length == 0)
        $("#selectedColor").css("background-color","#3A87AD");
    
};


starCalendar.saveNotifyEventDialog = function(){
    $('.notify-checkboxes').prop("checked", true);
    $(".popup-event-notify").click();
    $(".spinner-save-notify").show();
    $(".spinner-notify").hide();
    starEvent.saveEventDialog();
}

starCalendar.hexToRgb = function(hex) {
    var shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
    hex = hex.replace(shorthandRegex, function(m, r, g, b) {
        return r + r + g + g + b + b;
    });
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
}

starCalendar.defaultColor = "#3A87AD"; 

starCalendar.getEvents = function(start, end, callback) {
    var clbck = callback; 

    var url = "start="+start.toJSON()+"&end="+end.toJSON()+"&uuid="+starCalendar.userUUID+"&type=calendar";
    if(starCalendar.userDisplayedLogin != undefined){
        url = "start="+start.toJSON()+"&end="+end.toJSON()+"&uuid="+starCalendar.userUUID+"&user="+starCalendar.userDisplayedLogin+"&type=request";
    }
    if(starCalendar.userDisplayedLogin != undefined){
        url = "start="+start.toJSON()+"&end="+end.toJSON()+"&uuid="+starCalendar.userUUID+"&user="+starCalendar.userDisplayedLogin+"&type=request";
    }
    if(starCalendarInit.listing != undefined){
        url = "start="+start.toJSON()+"&end="+end.toJSON()+"&uuid="+starCalendar.userUUID+"&user="+starCalendar.userDisplayedLogin+"&type=request";
        //url = "start="+start.toJSON()+"&end="+end.toJSON()+"&uuid="+starCalendar.userUUID+"&listing="+starCalendarInit.listing+"&type=request";
    }
    
    starCalendar.start = start;
    starCalendar.end = end;
    var params = {};
    params.url = url;
    starServices.getItems("", params, function(data){
        var events = [];
        var d = new Date();
        for(var i = 0; i < data.length; i++){
            var s = new Date();
            s.setTime(new Date(data[i].eventStart));
            var e = new Date();
            e.setTime(new Date(data[i].eventEnd));
            var d = new Date();
            d.setTime(new Date(data[i].created));
            
            var isInvited = data[i].isInvited;
            var color = data[i].color;
            console.log(data[i]);
            if(color == undefined || color == null || color == "")
                color = starCalendar.defaultColor;

            if(isInvited){
                var rgb = starCalendar.hexToRgb(color);
                if(rgb != null)
                    color = "rgba("+rgb.r+","+rgb.g+","+rgb.b+",0.5)";
            }
            
            if(data[i].state == "customer_created"){
                color = "rgba(255,0,0,0.4)";
            }
            
            event = {};
            event.isInvite = data[i].isInvite;
            event.user = data[i].user;
            event.invisible = data[i].invisible;
            event.title = data[i].title;
            event.description = data[i].description;
            event.start = s;
            event.end = e;
            event.uuid = data[i].uuid;
            event.state = data[i].state;
            event.isInvite = data[i].isInvite;
            event.price = data[i].price;
            event.charging = data[i].charging;
            event.comments = data[i].comments;
            event.currency = data[i].currency;
            event.allDay = false;
            event.type = data[i].type;
            event.privacy = data[i].privacy;
            event.isOwner = data[i].isOwner;
            event.backgroundColor = color;
            event.notifyInvited = data[i].notifyInvited;
            event.created = d;
            event.createdBy = data[i].createdBy;
            event.createdByName = data[i].createdByName;
            event.createdByLogin = data[i].createdByLogin;
            event.createdByAvatarUrl = data[i].createdByAvatarUrl;
            if(data[i].isEditable)
                event.editable = true;
            else
                event.editable = false;

            // update selected event 
            if(starCalendar.selectedEvent != undefined && event.uuid == starCalendar.selectedEvent.uuid){
                starCalendar.selectedEvent = event;
            }
            events.push(event);
        }
        clbck(events);
    });
};




