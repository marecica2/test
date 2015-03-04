$(document).ready(function() {
    var lang = starUtils.getCookie("PLAY_LANG");
        
    if (window!=window.top) {
        $(".button-fullscreen").show();
    }
    
    $(".event-dialog-save").click(starEvent.saveEventDialog);
    $(".event-dialog-save-notify").click(starCalendar.saveNotifyEventDialog);
    $(".event-dialog-create").click(starCalendar.popupSaveNewEvent);
    $(".event-dialog-sync").click(starCalendar.syncGoogle);
    $(".event-dialog-propose").click(starCalendar.popupSaveNewEventPropose);
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
            star.utils.deleteCookie("agenda");
            star.utils.setCookie("agenda", cal);
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

    $(".event-charging").change(function(){
        if("free" == starUtils.getRadio("event-charging"))
            $(".price-container").hide();
        else
            $(".price-container").show();
        
    });
    
    $("#popup-close").click(function(){
        starCalendar.calendar.fullCalendar('unselect');
        $('#myPopover').modal('hide');
    });

    
    $(document).on("click", ".popup-event-edit", function(){
        starCalendar.calendar.fullCalendar('rerender');
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

    
    $(".popup-event-delete").click(function(){
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
    
    if(starCalendar.defaultView == "")
        starCalendar.defaultView = "agendaWeek";
    
    // sync with gcal
    var gcal = star.utils.getCookie("gcal");
    if(gcal != undefined && gcal == "true"){
        $("#google-checkbox").prop('checked', true);    
    } 
    $("#google-checkbox").change(function(){
        if($(this).is(":checked")){
            star.utils.setCookie("gcal", "true");
            starCalendar.calendar.fullCalendar( 'refetchEvents' );
        } else {
            star.utils.setCookie("gcal", "false");
            starCalendar.calendar.fullCalendar( 'refetchEvents' );
        }
    })
    
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
            selectable: starCalendar.editable,
            editable: starCalendar.editable,
            selectHelper: true,
            axisFormat: 'HH:mm',
            hiddenDays: starCalendar.hiddenDays,
            height: starCalendar.defaultView == "month" ? 700 : ((starCalendar.endHour - starCalendar.startHour) * 105) ,
            minTime: starCalendar.startHour,
            maxTime: starCalendar.endHour,
            defaultView: starCalendar.defaultView == undefined ? "agendaWeek" : starCalendar.defaultView,
            
            // handlers
            eventClick: starCalendar.clickEvent,
            eventMouseout: starCalendar.mouseOutEvent,
            eventMouseover: starCalendar.mouseOverEvent,
            select: function(start, end, allDay) {
                if(starCalendar.listings || starCalendar.listing)
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
                if(event.invisible == undefined || !event.invisible)
                    starCalendar.decorateEvent(event, element, view);
            },
            eventAfterAllRender: function(view){
                starCalendar.drawLine(view);
                if(typeof FB != "undefined")
                    FB.Canvas.setSize({ width: 640, height: $(window.document).height() });
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
starCalendar.drawLine = function(view){
    var parentDiv = $(".fc-agenda-slots:visible").parent();
    var timeline = parentDiv.children(".timeline");
    if (timeline.length == 0) { //if timeline isn't there, add it
        timeline = $("<hr>").addClass("timeline");
        parentDiv.prepend(timeline);
    }

    var curTime = new Date();

    var curCalView = $("#calendar-div").fullCalendar('getView');
    if (curCalView.visStart < curTime && curCalView.visEnd > curTime) {
        timeline.show();
    } else {
        timeline.hide();
        return;
    }

    var curSeconds = (curTime.getHours() * 60 * 60) + (curTime.getMinutes() * 60) + curTime.getSeconds();
    var percentOfDay = curSeconds / 86400; //24 * 60 * 60 = 86400, # of seconds in a day
    var topLoc = Math.floor(parentDiv.height() * percentOfDay);

    timeline.css("top", topLoc + "px");
    if (curCalView.name == "agendaWeek") { //week view, don't want the timeline to go the whole way across
        var dayCol = $(".fc-today:visible");
        var left = dayCol.position().left + 1;
        var width = dayCol.width()-2;
        timeline.css({
            left: left + "px",
            width: width + "px"
        });
    }    
};

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
        if(event.createdByAvatarUrl != undefined)
            html = "<img class='img-circle avatar22' src='/"+event.createdByAvatarUrl+"'> " + html;
        el.html(html + text);
    }
};

    
starCalendar.syncGoogle = function(){
    $.get("/event-sync-google?"+star.token+"&uuid="+starCalendar.selectedEvent.uuid);
    starCalendar.calendar.fullCalendar('refetchEvents');
    $('#myPopover').modal('hide');
}


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
    event.listing = $("#popup-listing").val();
    if(star.selectedEvent != null && star.selectedEvent.googleId != undefined)
        event.googleId = star.selectedEvent.googleId;
    if(starCalendar.userDisplayed != undefined && proposal != undefined && proposal == true){
        event.user = starCalendar.userDisplayed;
        event.proposal = true;
    } else {
        event.user = starCalendar.userUUID;
        event.proposal = false;
    }
    
    starServices.createEvent(event, function(successEvent){
        starCalendar.calendar.fullCalendar('refetchEvents');
    }, null);
    starCalendar.calendar.fullCalendar('unselect');

    // hide popup
    var popup = $("#myPopover");
    $('#myPopover').modal('hide');
    //popup.hide();
};


// selection event
starCalendar.selectionNewEvent = function(ccc, start, end, allDay, event) {
    // create popup and fill data
    var popup = $("#myPopover");
    
    // in case of month view create default time for new event 
    starCalendar.calendar = ccc;
    starCalendar.start = start;
    starCalendar.end = end;
    starCalendar.allDay = allDay;
    


    $(".event-title").html(star.utils.trimTo(i18n("New event"), 35));
    if(event != undefined){
        $(".event-title").html(star.utils.trimTo(i18n("add-google-event"), 35));
        star.selectedEvent = event;
    } else {
        star.selectedEvent = null;
    }
    
    $(".event-dialog-sync").hide();
    $(".event-time-from").html("");
    $(".event-time-to").html("");
    $("#timepicker1").val();
    $('#timepicker1').timepicker('setTime', start.getHours()+":"+start.getMinutes());
    $('#timepicker2').timepicker('setTime', end.getHours()+":"+end.getMinutes());
    $(".popup-edit-event").hide();
    $(".popup-event-delete").hide();
    $(".popup-new-event").show();
    $(".popup-event-view").hide();
    $(".popup-event-description").html("");
    $("#popup-event-title").val("");
    $("#popup-event-title").focus();
    $(".event-dialog-propose").show();
    $(".event-dialog-create").show();

    if(ccc.fullCalendar('getView').name == "month"){
        var now = new Date();
        var later = new Date(now.getTime() + (1000 * 60 * 15));
        $('.timepicker1').val((now.getHours()<10?'0':'') + now.getHours()+":"+(now.getMinutes()<10?'0':'') + now.getMinutes());
        $('.timepicker2').val((later.getHours()<10?'0':'') + later.getHours()+":"+(later.getMinutes()<10?'0':'') + later.getMinutes());
    }

    $('#myPopover').modal('show');
};


starCalendar.clickEvent = function(event, jsEvent, view) {
    starCalendar.selectedEvent = event;
    // copy values to the event detail page dialog
    if(event.uuid != undefined){
        starEvent.inviteLoad(event.uuid, function(){
            starCalendar.copyValuesToDialog(event);
            $('#myPopover').modal('show');
        });
    }

    if(event.uuid == undefined && event.googleId != null){
        if(starCalendar.listings)
            starCalendar.selectionNewEvent(starCalendar.calendar, event.start, event.end, false, event);
    }
    
    // prevent going to the url
    return false;
};

starCalendar.copyValuesToDialog = function(event){
    
    // event details in dialog
    $(".event-edit").hide();
    $(".event-view").show();
    var el = $(".event-detail-container");
    var html = "";
    if(event.uuid != undefined){
        if(event.charging != "free")
            html+="<span style='font-size:0.8em' class='label default-bg'>" + i18n("total-price") + ": " + event.priceTotal + " " + event.currency + "</span> ";
        else
            html+="<span style='font-size:0.8em' class='label default-bg'>" + i18n("free") + "</span> ";
    }
    el.html(html);
    $(".popup-event-charging").html(html);
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
    $(".event-dialog-sync").show();
    $(".event-dialog-propose").hide();
    $(".event-dialog-create").hide();
    if(!event.editable){
        $(".event-title-label").show();
        $(".event-description-label").show();
        $(".event-dialog-save-notify").hide();
        $(".popup-event-delete").hide();
        $(".popup-event-notify").hide();
    } else {
        $(".event-title-label").hide();
        $(".event-description-label").hide();
        $(".event-dialog-save").show();
        $(".event-dialog-save-notify").show();
        $(".popup-event-notify").show();
        $(".notify-check-all").show();
        $(".popup-event-delete").show();
    }
    $(".popup-event-description").html(star.utils.trimTo(event.description, 250));
    $(".event-anchor").each(function() {
        if(starCalendar.isPublic){
            $(this).attr("href", $(this).attr("data-url")+event.uuid+"&id="+starCalendar.userUUID);
        } else {
            $(this).attr("href", $(this).attr("data-url")+event.uuid);
        }
    });
    $(".event-createdByName").html("<img class='img-circle avatar22' src='/"+event.createdByAvatarUrl+"'> <a href='/public/user?id="+event.createdBy+"'>" + event.createdByName + "</a>");
    $(".event-title-label").val(event.title);
    $(".event-title-label").html(event.title);
    starCalendar.selectedEvent = event;
    if(event.currency == null || event.currency.length <= 0)
        $(".event-currency").val(starCalendar.defaultCurrency);
    else
        $(".event-currency").val(event.currency);
    $(".event-title").html(star.utils.trimTo(event.title, 35));
    if(event.start != null)
        $(".event-time-from").html(starUtils.formatDate(event.start) + " " + starUtils.formatTime(event.start));
    if(event.end != null)
        $(".event-time-to").html(starUtils.formatTime(event.end));
    $("#event-description").html(star.utils.trimTo(event.description, 100));
    $("#event-description-label").html(event.description);
    $(".event-charging-price").val(event.price);
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

starCalendar.defaultColor = "#812AAD"; 

starCalendar.getEvents = function(start, end, callback) {
    var clbck = callback; 
    var url = "start="+start.toJSON()+"&end="+end.toJSON()+"&uuid="+starCalendar.userUUID+"&type=calendar";

    if(starCalendar.userDisplayedLogin != undefined){
        url = "start="+start.toJSON()+"&end="+end.toJSON()+"&uuid="+starCalendar.userUUID+"&user="+starCalendar.userDisplayedLogin+"&type=request";
    }
    if(starCalendar.listing != undefined){
        url = "start="+start.toJSON()+"&end="+end.toJSON()+"&uuid="+starCalendar.userUUID+"&user="+starCalendar.userDisplayedLogin+"&type=request";
    }
    
    if(star.utils.getCookie("gcal") != undefined && star.utils.getCookie("gcal") == "true")
        url += "&gcal=true";
    else {
        url += "&gcal=false";
    }
    starCalendar.start = start;
    starCalendar.end = end;
    var params = {};
    params.url = url;
    params.gcal = star.utils.getCookie("gcal");
    starServices.getItems("", params, function(data){
        var events = [];
        var d = new Date();
        for(var i = 0; i < data.length; i++){
            var ev = data[i];
            
            var s = new Date();
            s.setTime(new Date(ev.eventStart));
            var e = new Date();
            e.setTime(new Date(ev.eventEnd));
            var d = new Date();
            d.setTime(new Date(ev.created));
            
            var isInvited = ev.isInvited;
            var isOwner = ev.isOwner;
            var color = ev.color;
            if(color == undefined || color == null || color == "")
                color = starCalendar.defaultColor;

            if(isInvited && !isOwner){
                var rgb = starCalendar.hexToRgb(color);
                if(rgb != null)
                    color = "rgba("+rgb.r+","+rgb.g+","+rgb.b+",0.6)";
            }
            
            if(ev.state == "customer_created"){
                color = "rgba(255,0,0,0.5)";
            }
            
            event = {};
            event.isInvite = ev.isInvite;
            event.user = ev.user;
            event.invisible = ev.invisible;
            event.title = ev.title != undefined? ev.title : "";
            event.description = ev.description;
            event.start = s;
            event.end = e;
            event.uuid = ev.uuid;
            event.state = ev.state;
            event.isInvite = ev.isInvite;
            event.price = ev.price;
            event.priceTotal = ev.priceTotal;
            event.charging = ev.charging;
            event.comments = ev.comments;
            event.currency = ev.currency;
            event.allDay = false;
            event.type = ev.type;
            event.privacy = ev.privacy;
            event.isOwner = ev.isOwner;
            event.backgroundColor = color;
            event.notifyInvited = ev.notifyInvited;
            event.created = d;
            event.createdBy = ev.createdBy;
            event.createdByName = ev.createdByName;
            event.createdByLogin = ev.createdByLogin;
            event.createdByAvatarUrl = ev.createdByAvatarUrl;
            if(ev.isEditable)
                event.editable = true;
            else
                event.editable = false;

            event.googleId = ev.googleId;
            if(ev.googleId != null && ev.uuid == null){
                event.editable = false;
            }

            // update selected event 
            if(starCalendar.selectedEvent != undefined && event.uuid == starCalendar.selectedEvent.uuid){
                starCalendar.selectedEvent = event;
            }
            events.push(event);
        }
        clbck(events);
    });
};




