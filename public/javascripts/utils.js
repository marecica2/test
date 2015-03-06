star.utils = {};

star.utils.Interval = function(fn, time) {
    var timer = false;
    this.start = function () {
        if (!this.isRunning())
            timer = setInterval(fn, time);
    };
    this.stop = function () {
        clearInterval(timer);
        timer = false;
    };
    this.isRunning = function () {
        return timer !== false;
    };
}


star.utils.getHashParams = function () {
    var hashParams = {};
    var e,
        a = /\+/g,  // Regex for replacing addition symbol with a space
        r = /([^&;=]+)=?([^&;]*)/g,
        d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
        q = window.location.hash.substring(1);
    while (e = r.exec(q))
       hashParams[d(e[1])] = d(e[2]);
    return hashParams;
}

star.utils.getParams = function () {
    var hashParams = {};
    var e,
        a = /\+/g,  // Regex for replacing addition symbol with a space
        r = /([^&;=]+)=?([^&;]*)/g,
        d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
        q = window.location.search.substring(1);
    while (e = r.exec(q))
       hashParams[d(e[1])] = d(e[2]);
    return hashParams;
}

star.utils.trimTo = function(input, len) {
    if(input != undefined && input != null && input.length > 10 && input.length > len){
        input = input.substring(0, len-1);
        input += "...";
    }
    return input;
}

star.utils.uuid = function() {
    function s4() {
      return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
    }
    return s4() + s4() + s4() + s4() + s4() + s4() + s4() + s4();
  }

star.utils.uploadFiles = function(url, files, clbk) {
    for (var i = 0; i < files.length; i++) {
      var formData = new FormData();
      var file = files[i];
      console.log(file.name);
      formData.append("attachment", file);
      formData.append("contentType", file.type);
      formData.append("size", file.size);
      formData.append("name", file.name);
      console.log(formData);
      
      $("#progresses").append("<div class='progress' id='progressContainer"+i+"'><div class='progress-bar progress-bar-striped active' id='progress"+i+"' role='progressbar' aria-valuenow='45' aria-valuemin='0' aria-valuemax='100'></div></div>");
      var xhr = new XMLHttpRequest();
      xhr.open('POST', url, true);
      
      (function(i, clbk) {
          var index = i;
          xhr.onload = function(e) { 
              $("#upload").replaceWith($("#upload").clone(true));                  
              $("#progressContainer"+index).remove();
              var uuid = e.currentTarget.responseText;
              if(clbk != undefined)
                  clbk(uuid);
              $.post("/delete-temp-files", function(){});
          };
          
          xhr.upload.onprogress = function(e) {
              if (e.lengthComputable) {
                  var up = (e.loaded / e.total) * 100;
                  $("#progress"+i).css("width", up+"%");
              }
          }
      })(i, clbk);      

      $("#progress"+i).show();
      xhr.send(formData);
    }
};   

$(document).ready(function(){
    $("#query-search").typeahead({
        minLength : 2, 
        source : function(value, process){
          var items = [];
          items.push(value);    
          var q = value.replace(/ /g, '+');
          $.getJSON("https://suggestqueries.google.com/complete/search?callback=?",
                  {
                    //"hl":"de", // Language
                    //"ds":"yt", // Restrict lookup to youtube
                    "jsonp":"suggestCallBack", // jsonp callback function name
                    "q":q, // query term
                    "client":"chrome" // force youtube style response, i.e. jsonp
                  }
              );
              suggestCallBack = function (data) {
                  data = data[1];
                  for(var i = 0; i < data.length; i++)
                      items.push(data[i]);
                  process(items);             
              };    
        },
        updater : function(item){
            //console.log("up " + item);
            return item;
        }, 
        matcher : function(item){
            //console.log("ma " + item);
            return true;
        }
    });    
    
    $("#upload-button").click(function(){
        $("#upload").click();
    });
    
    $("#upload").change(function(){
        var params = "";
        params = "temp="+starCalendar.temp;
        params += "&avatar=true";
        
        star.utils.uploadFiles('/fileupload?'+star.token+'&'+params, this.files, function(json){
            var resp = JSON.parse(json);
            $(".avatar-container").addClass("eg-wrapper");
            $(".eg-preview").show();
            $("#crop-button").show();
            $("#upload-button").hide();
            $("#image").attr("style", "");
            $("#image").attr("src", "../public/uploads/"+resp.url);
            $("#imageUrl").val("public/uploads/"+resp.url+"");
            $("#imageId").val(resp.uuid);

            $(".avatar-container > img").cropper({
                aspectRatio : starCalendar.aspectRatio,
                preview: ".avatar-preview",
                done : function(data) {
                    $("#x1").val(Math.round(data.x));
                    $("#y1").val(Math.round(data.y));
                    $("#x2").val(Math.round(data.x + data.width));
                    $("#y2").val(Math.round(data.y + data.height));
                    $("#imageIdCrop").val($("#imageId").val());
                }
            });
        });
    });
    $("#crop-button").click(function(){
        $("#cropForm").submit();
    });
    
});

star.utils.setCookie = function(name,value,days) {
    var date = new Date();
    date.setTime(date.getTime()+(9999*24*60*60*1000));
    var expires = "; Expires="+date.toGMTString();
    document.cookie = name+"="+value+expires+"; Path=/";
}

star.utils.getCookie = function(cname) {
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for(var i=0; i<ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1);
        if (c.indexOf(name) != -1) return c.substring(name.length,c.length);
    }
    return "";
}


star.utils.dismiss = function(name){
    star.utils.setCookie(name, "true");
}

star.utils.deleteCookie = function(cname) {
    var date = new Date();
    document.cookie = name + "=; Expires="+date.toGMTString()+"; Path=/";
    return "";
}


// other libs

var dateFormat = function () {
    var token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
        timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
        timezoneClip = /[^-+\dA-Z]/g,
        pad = function (val, len) {
            val = String(val);
            len = len || 2;
            while (val.length < len) val = "0" + val;
            return val;
        };

    // Regexes and supporting functions are cached through closure
    return function (date, mask, utc) {
        var dF = dateFormat;

        // You can't provide utc if you skip other args (use the "UTC:" mask prefix)
        if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
            mask = date;
            date = undefined;
        }

        // Passing date through Date applies Date.parse, if necessary
        date = date ? new Date(date) : new Date;
        if (isNaN(date)) throw SyntaxError("invalid date");

        mask = String(dF.masks[mask] || mask || dF.masks["default"]);

        // Allow setting the utc argument via the mask
        if (mask.slice(0, 4) == "UTC:") {
            mask = mask.slice(4);
            utc = true;
        }

        var _ = utc ? "getUTC" : "get",
            d = date[_ + "Date"](),
            D = date[_ + "Day"](),
            m = date[_ + "Month"](),
            y = date[_ + "FullYear"](),
            H = date[_ + "Hours"](),
            M = date[_ + "Minutes"](),
            s = date[_ + "Seconds"](),
            L = date[_ + "Milliseconds"](),
            o = utc ? 0 : date.getTimezoneOffset(),
            flags = {
                d:    d,
                dd:   pad(d),
                ddd:  dF.i18n.dayNames[D],
                dddd: dF.i18n.dayNames[D + 7],
                m:    m + 1,
                mm:   pad(m + 1),
                mmm:  dF.i18n.monthNames[m],
                mmmm: dF.i18n.monthNames[m + 12],
                yy:   String(y).slice(2),
                yyyy: y,
                h:    H % 12 || 12,
                hh:   pad(H % 12 || 12),
                H:    H,
                HH:   pad(H),
                M:    M,
                MM:   pad(M),
                s:    s,
                ss:   pad(s),
                l:    pad(L, 3),
                L:    pad(L > 99 ? Math.round(L / 10) : L),
                t:    H < 12 ? "a"  : "p",
                tt:   H < 12 ? "am" : "pm",
                T:    H < 12 ? "A"  : "P",
                TT:   H < 12 ? "AM" : "PM",
                Z:    utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
                o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
                S:    ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
            };

        return mask.replace(token, function ($0) {
            return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
        });
    };
}();


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
    var date = starUtils.getTimeZoneDate(long);
    return dateFormat(date.getTime(), dateFormat.masks.mediumDate);
};

starUtils.formatTime = function(long) {
    var date = starUtils.getTimeZoneDate(long);
    return dateFormat(date.getTime(), dateFormat.masks.shortTime);
};

starUtils.formatDateTime = function(date) {
    date = new Date(date);
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

starUtils.getTimeZoneDate = function(long)
{
    var date = new Date();
    //date.setTime(long + (star.timezone * 60000));
    date.setTime(long);
    
//    var timezoneJs = starUtils.getCookie("timezoneJs");
//    if(timezoneJs != undefined){
//        date.setTime(date.getTime() - (timezoneJs*60000));
//        return date;
//    }
    return date;
}

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

function linkify(inputText) {
    var replacedText, replacePattern1, replacePattern2, replacePattern3;
    replacePattern1 = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
    replacedText = inputText.replace(replacePattern1, '<a href="$1" target="_blank">$1</a>');
    replacePattern2 = /(^|[^\/])(www\.[\S]+(\b|$))/gim;
    replacedText = replacedText.replace(replacePattern2, '$1<a href="http://$2" target="_blank">$2</a>');
    replacePattern3 = /(([a-zA-Z0-9\-\_\.])+@[a-zA-Z\_]+?(\.[a-zA-Z]{2,6})+)/gim;
    replacedText = replacedText.replace(replacePattern3, '<a href="mailto:$1">$1</a>');
    return replacedText;
}

// Some common format strings
dateFormat.masks = {
    "default":      "ddd mmm dd yyyy HH:MM:ss",
    shortDate:      "m/d/yy",
    mediumDate:     "d mmm",
    longDate:       "d mmmm, yyyy",
    fullDate:       "dddd, mmmm d, yyyy",
    shortTime:      "h:MM TT",
    mediumTime:     "h:MM:ss TT",
    longTime:       "h:MM:ss TT Z",
    isoDate:        "yyyy-mm-dd",
    isoTime:        "HH:MM:ss",
    isoDateTime:    "yyyy-mm-dd'T'HH:MM:ss",
    isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
    dayNames: [
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    ],
    monthNames: [
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
    ]
};

// For convenience...
Date.prototype.format = function (mask, utc) {
    return dateFormat(this, mask, utc);
};


//ratings
(function ($) {
    $.fn.rating = function () {
      var element;

      // A private function to highlight a star corresponding to a given value
      function _paintValue(ratingInput, value, active_icon, inactive_icon) {
        var selectedStar = $(ratingInput).find('[data-value="' + value + '"]');
        selectedStar.removeClass(inactive_icon).addClass(active_icon);
        selectedStar.prevAll('[data-value]').removeClass(inactive_icon).addClass(active_icon);
        selectedStar.nextAll('[data-value]').removeClass(active_icon).addClass(inactive_icon);
      }

      // A private function to remove the highlight for a selected rating
      function _clearValue(ratingInput, active_icon, inactive_icon) {
        var self = $(ratingInput);
        self.find('[data-value]').removeClass(active_icon).addClass(inactive_icon);
      }

      // A private function to change the actual value to the hidden field
      function _updateValue(input, val) {
        input.val(val).trigger('change');
        if (val === input.data('empty-value')) {
          input.siblings('.rating-clear').hide();
        } else {
          input.siblings('.rating-clear').show();
        }
      }


      // Iterate and transform all selected inputs
      for (element = this.length - 1; element >= 0; element--) {

        var el, i,
          originalInput = $(this[element]),
          max = originalInput.data('max') || 5,
          min = originalInput.data('min') || 0,
          def_val = originalInput.val() || 0,
          lib = originalInput.data('icon-lib') || 'glyphicon'
          active = originalInput.data('active-icon') || 'glyphicon-star',
          inactive = originalInput.data('inactive-icon') || 'glyphicon-star-empty',
          clearable = originalInput.data('clearable') || null,
          clearable_i = originalInput.data('clearable-icon') || 'glyphicon-remove',
          stars = '';

        // HTML element construction
        for (i = min; i <= max; i++) {
          // Create <max> empty stars
          if(i  <= def_val){
            stars += ['<i style="font-size:2em" class="',lib, ' ', active, '" data-value="', i, '"></i> '].join('');
            }
          else{
              stars += ['<i style="font-size:2em" class="',lib, ' ', inactive, '" data-value="', i, '"></i> '].join('')
              }
        }
        // Add a clear link if clearable option is set
        if (clearable) {
            stars += [
            ' <a class="rating-clear" style="display:none;" href="javascript:void">',
            '<span class="',lib,' ',clearable_i,'"></span> ',
            clearable,
            '</a>'].join('');
        }

        // Clone with data and events the original input to preserve any additional data and event bindings.
        var newInput = originalInput.clone(true)
          .addClass('hidden')
          .data('max', max)
          .data('min', min)
          .data('icon-lib', lib)
          .data('active-icon', active)
          .data('inactive-icon', inactive);

        // Rating widget is wrapped inside a div
        el = [
          '<div class="rating-input">',
          stars,
          '</div>'].join('');

        // Replace original inputs HTML with the new one
        if (originalInput.parents('.rating-input').length <= 0) {
          originalInput.replaceWith($(el).append(newInput));
        }

      }

      // Give live to the newly generated widgets
      $('.rating-input')
        // Highlight stars on hovering
        .on('mouseenter', '[data-value]', function () {
          var self = $(this);
           input = self.siblings('input');
          _paintValue(self.closest('.rating-input'), self.data('value'), input.data('active-icon'), input.data('inactive-icon'));
        })
        // View current value while mouse is out
        .on('mouseleave', '[data-value]', function () {
          var self = $(this),
            input = self.siblings('input'),
            val = input.val(),
            min = input.data('min'),
            max = input.data('max'),
            active = input.data('active-icon'),
            inactive = input.data('inactive-icon');
          if (val >= min && val <= max) {
            _paintValue(self.closest('.rating-input'), val, active, inactive);
          } else {
            _clearValue(self.closest('.rating-input'), active, inactive);
          }
        })
        // Set the selected value to the hidden field
        .on('click', '[data-value]', function (e) {
          var self = $(this),
            val = self.data('value'),
            input = self.siblings('input');
          _updateValue(input,val);
          e.preventDefault();
          return false;
        })
        // Remove value on clear
        .on('click', '.rating-clear', function (e) {
          var self = $(this),
            input = self.siblings('input'),
            active = input.data('active-icon'),
            inactive = input.data('inactive-icon');
          _updateValue(input, input.data('empty-value'));
          _clearValue(self.closest('.rating-input'), active, inactive);
          e.preventDefault();
          return false;
        })
        // Initialize view with default value
        .each(function () {
          var input = $(this).find('input'),
            val = input.val(),
            min = input.data('min'),
            max = input.data('max');
          if (val !== "" && +val >= min && +val <= max) {
            _paintValue(this, val);
            $(this).find('.rating-clear').show();
          }
          else {
            input.val(input.data('empty-value'));
            _clearValue(this);
          }
        });

    };

    // Auto apply conversion of number fields with class 'rating' into rating-fields
    $(function () {
      if ($('input.rating[type=number]').length > 0) {
        $('input.rating[type=number]').rating();
      }
    });
}(jQuery));