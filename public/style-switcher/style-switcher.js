/*!
 * jQuery Cookie Plugin v1.4.1
 * https://github.com/carhartl/jquery-cookie
 *
 * Copyright 2006, 2014 Klaus Hartl
 * Released under the MIT license
 */

!function(a){"function"==typeof define&&define.amd?define(["jquery"],a):"object"==typeof exports?a(require("jquery")):a(jQuery)}(function(a){function c(a){return h.raw?a:encodeURIComponent(a)}function d(a){return h.raw?a:decodeURIComponent(a)}function e(a){return c(h.json?JSON.stringify(a):String(a))}function f(a){0===a.indexOf('"')&&(a=a.slice(1,-1).replace(/\\"/g,'"').replace(/\\\\/g,"\\"));try{return a=decodeURIComponent(a.replace(b," ")),h.json?JSON.parse(a):a}catch(c){}}function g(b,c){var d=h.raw?b:f(b);return a.isFunction(c)?c(d):d}var b=/\+/g,h=a.cookie=function(b,f,i){if(arguments.length>1&&!a.isFunction(f)){if(i=a.extend({},h.defaults,i),"number"==typeof i.expires){var j=i.expires,k=i.expires=new Date;k.setTime(+k+864e5*j)}return document.cookie=[c(b),"=",e(f),i.expires?"; expires="+i.expires.toUTCString():"",i.path?"; path="+i.path:"",i.domain?"; domain="+i.domain:"",i.secure?"; secure":""].join("")}for(var l=b?void 0:{},m=document.cookie?document.cookie.split("; "):[],n=0,o=m.length;o>n;n++){var p=m[n].split("="),q=d(p.shift()),r=p.join("=");if(b&&b===q){l=g(r,f);break}b||void 0===(r=g(r))||(l[q]=r)}return l};h.defaults={},a.removeCookie=function(b,c){return void 0===a.cookie(b)?!1:(a.cookie(b,"",a.extend({},c,{expires:-1})),!a.cookie(b))}});

jQuery(document).ready(function($) {

		style_switcher = $('.style-switcher'),
		panelWidth = style_switcher.outerWidth(true);
		
			
		$('.style-switcher .trigger').on("click", function(){
			var $this = $(this);
			if ($(".style-switcher.closed").length>0) {
				style_switcher.animate({"left" : "0px"});
				$(".style-switcher.closed").removeClass("closed");
				$(".style-switcher").addClass("opened");
				$(".style-switcher .trigger i").removeClass("fa-comments-o").addClass("fa-times");
			} else {
				$(".style-switcher.opened").removeClass("opened");
				$(".style-switcher").addClass("closed");
				$(".style-switcher .trigger i").removeClass("fa-times").addClass("fa-comments-o");
				style_switcher.animate({"left" : '-' + panelWidth});
			}
			return false;
		});

		// init open
		var chat = starUtils.getHashParameterByName("chat");
		if(chat == "open"){
		    $('.style-switcher .trigger').click();
		    window.location.hash = "";
		}
});    	