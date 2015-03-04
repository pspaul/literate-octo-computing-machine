/*! (c) 2011-2015 Datraverse B.V. */

/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */

/*
 * NOTE: the *! comment blocks are part of the compressed version.
 */

/*
 * SavaPage jQuery Mobile User Web App
 */
( function($, window, document, JSON, _ns) {"use strict";

        /* jslint browser: true */
        /* global $, jQuery, alert */

        /*
         * The JSON object is defined in json2.js (which is part of the CometD
         * jquery package)
         */

        /*
         * See Mantis #320.
         *
         * Note: do NOT use the ($("#page-main-thumbnail-images div
         * img[height]").length === 0)
         * construct, since this does NOT work in all browsers (e.g. Opera
         * desktop).
         */
        _ns.thumbnails2Load = 0;

        _ns.removeImgHeight = function(imgDomElement) {
            $(imgDomElement).removeAttr("height");
            _ns.thumbnails2Load--;
            if (_ns.thumbnails2Load === 0) {
                /*
                 * All thumbnails are loaded, so resume CometD.
                 */
                _ns.cometd.start(_ns.model.user.cometdToken);
                _ns.userEvent.resume();
                //alert("All loaded!");
            }
        };

        /*
        * ___________________________________________________________________
        * GENERAL NOTES
        *
        * (1) For the 'curtain' and showPageLoadingMsg to work, use the $.post
        * (ajax) construct, and NOT the synchronous apiCall(). Otherwise
        * AppleWebKit based browsers (Chrome) won't work.
        *
        * (2) $('x').offset({'left' : left}) does NOT work in Google Chrome use
        * $('x').css({'left' : left + 'px'}); instead
        *
        */

        /*
         * IMPORTANT: Disable the websocket transport before doing the handshake.
         * Google Chrome uses WebSockets, but this produces Invoking
         * event#monitorUserEven bursts after an event is reported back. TODO: it
         * works as it is now, but is websocketEnabled also needed?
         */
        $.cometd.websocketEnabled = false;
        $.cometd.unregisterTransport('websocket');

        /*
         * When the jQuery Mobile starts to execute, it triggers a mobileinit
         * event on the document object, to which you can bind to apply overrides to
         * jQuery Mobile's defaults.
         */
        $(document).on("mobileinit", null, null, function() {
            $.mobile.defaultPageTransition = "none";
            $.mobile.defaultDialogTransition = "none";
        });

        /**
         *
         */
        function DeviceEvent(_cometd) {
            var _this = this
            //
            , _longPollPending = false, _paused = false
            //
            , _onEvent
            //
            ;

            // this.onCardSwipe()
            // this.onEventIgnored
            // this.onEventError()
            // this.onPollInvitation()
            // this.onWaitingForEvent()
            // this.onException()

            /**
             * NOTE: use _this instead of this.
             */
            _onEvent = function(message) {
                var res;

                _longPollPending = false;

                if (!_paused) {
                    res = $.parseJSON(message.data);
                    if (res.event === 'card-swipe') {
                        _this.onCardSwipe(res.cardNumber);
                    } else if (res.event === 'error') {
                        _this.onEventError(res.error);
                    } else {
                        _this.onPollInvitation();
                    }
                }
            };

            /**
             * Adds a listener to the '/device/event' channel.
             * <p>
             * This is the channel the consumer of '/service/device'
             * publications, publishes (writes responses) to.
             * </p>
             */
            this.addListener = function() {
                _cometd.addListener('/device/event', _onEvent);
                // Get things started: invite to do a poll
                this.onPollInvitation();
            };

            /**
             * The long poll as 'publish' to the '/service/device' channel.
             *
             * <p>
             * The '/service/' channel is used for client to server
             * communication,
             * contrary to <strong>Normal</strong> channels (whose name starts
             * with
             * any other string, except '/meta/', and is used to broadcast
             * messages
             * between clients).
             * </p>
             */
            this.poll = function(language) {
                _longPollPending = true;
                //this.onWaitingForEvent();
                try {
                    $.cometd.publish('/service/device', {
                        language : language
                    });
                } catch (err) {
                    this.onException(err);
                }
            };

            /**
             * Public function to pause the long poll: a pause indication is set.
             */
            this.pause = function() {
                _paused = true;
            };

            /**
             * Public function to resumes the long poll after a pause: the pause
             * indication is unset, and when no long-poll is pending, a new long
             * poll is started.
             */
            this.resume = function() {
                _paused = false;
                if (!_longPollPending && _cometd.isOn()) {
                    this.onPollInvitation();
                }
            };

        }// DeviceEvent

        /**
         *
         */
        function ProxyPrintEvent(_cometd) {
            var _this = this
            //
            , _onEvent
            //
            , _longPollPending = false
            //
            , _subscription
            //
            ;

            // this.onException();

            /**
             * NOTE: use _this instead of this.
             */
            _onEvent = function(message) {

                var res = $.parseJSON(message.data);

                _longPollPending = false;

                if (res.event === 'printed') {
                    _this.onPrinted(res);
                } else if (res.event === 'error') {
                    _this.onError(res);
                } else {
                    _this.onError('An unknown error occurred.');
                }
            };

            /**
             * Adds a listener to the '/proxyprint/event' channel. This method
             * is idempotent: only the first call will actually do the cometd
             * addListener call.
             * <p>
             * This is the channel the consumer of '/service/proxyprint'
             * publications, publishes (writes responses) to.
             * </p>
             * @return The subscription.
             */
            this.addListener = function() {
                if (!_subscription) {
                    _longPollPending = false;
                    _subscription = _cometd.addListener('/proxyprint/event', _onEvent);
                }
            };

            /*
             *
             */
            this.removeListener = function() {
                if (_subscription) {
                    _cometd.removeListener(_subscription);
                    _subscription = null;
                }
                _longPollPending = false;
            };

            /**
             * The long poll as 'publish' to the '/service/proxyprint' channel.
             *
             * <p>
             * The '/service/' channel is used for client to server
             * communication,
             * contrary to <strong>Normal</strong> channels (whose name starts
             * with
             * any other string, except '/meta/', and is used to broadcast
             * messages
             * between clients).
             * </p>
             */
            this.poll = function(idUser, printerName, readerName, language) {

                if (!_longPollPending) {

                    _longPollPending = true;

                    // lazy subscribe
                    if (!_subscription) {
                        this.addListener();
                    }
                    try {
                        $.cometd.publish('/service/proxyprint', {
                            idUser : idUser,
                            printerName : printerName,
                            readerName : readerName,
                            language : language
                        });
                    } catch (err) {
                        _longPollPending = false;
                        this.onException("ProxyPrint poll exception: " + err);
                    }
                }

            };

        }// ProxyPrintEvent

        /**
         *
         */
        function UserEvent(_cometd, _api) {
            var
            // _super = new Base()
            //, _self = derive(_super)
            _this = this
            //
            , _longPollStartTime = null
            //
            , _paused = false
            //
            , _onEvent
            //
            , _subscription
            //
            ;

            // this.onJobEvent()
            // this.onEventIgnored
            // this.onEventError()
            // this.onPollInvitation()
            // this.onWaitingForEvent()
            // this.onException();

            /**
             * NOTE: use _this instead of this.
             */
            _onEvent = function(message) {

                var res;

                _longPollStartTime = null;

                res = $.parseJSON(message.data);

                if (!_paused) {
                                        
                    // UserEventEnum
                    if (res.event === "PRINT_IN") {
                        
                        _this.onJobEvent(res);
                        
                        /*
                         * Do NOT  send invitation for a next poll, since this
                         * is done by the handler of the onJobEvent.
                         */
                        
                    } else if (res.event === "ERROR") {
                        
                        _this.onEventError(res.error);
                        
                    } else if (res.event === "SERVER_SHUTDOWN") {
                        
                        _this.onEventError('Server shutdown.');
                        
                    } else {

                        if (res.event === "PRINT_MSG") {
                            _this.onMsgEvent(res.data);
                        } else if (res.event === "ACCOUNT") {
                            _this.onNullEvent(res.data);
                            _this.onAccountEvent(res.stats);
                        } else if (res.event === "NULL") {
                            _this.onNullEvent(res.data);
                            _this.onAccountEvent(res.stats);
                        }

                        _this.onPollInvitation();
                        
                    }
                    //} else {
                    //_this.onEventIgnored();
                }
            };

            /**
             * Subscribes to the '/user/event' channel.
             * <p>
             * This is the channel the consumer of '/service/user' publications,
             * publishes (writes responses) to.
             * </p>
             */
            this.addListener = function() {
                if (!_subscription) {
                    _subscription = _cometd.addListener('/user/event', _onEvent);
                    /*
                     * Get things started: invite to do a poll.
                     */
                    this.onPollInvitation();
                }
            };

            /*
             * Does NOT work since _subscription remains undefined (?).
             */
            this.removeListener = function() {
                if (_subscription) {
                    _cometd.removeListener(_subscription);
                    _subscription = null;
                }
            };

            this.isLongPollPending = function() {
                return _longPollStartTime;
            };

            this.setLongPollLost = function() {
                _longPollStartTime = null;
            };

            /**
             * The long poll as 'publish' to the '/service/user' channel.
             *
             * <p>
             * The '/service/' channel is used for client to server
             * communication,
             * contrary to <strong>Normal</strong> channels (whose name starts
             * with
             * any other string, except '/meta/', and is used to broadcast
             * messages
             * between clients).
             * </p>
             */
            this.poll = function(userid, pagecount, uniqueUrlVal, prevMsgTime, language, base64) {
                
                if (!_longPollStartTime) {
                    _longPollStartTime = new Date().getTime();
                    this.onWaitingForEvent();
                    try {
                        $.cometd.publish('/service/user', {
                            user : userid,
                            'page-offset' : pagecount,
                            'unique-url-value' : uniqueUrlVal,
                            'msg-prev-time' : prevMsgTime,
                            language : language,
                            base64 : base64,
                            webAppClient : true
                        });
                    } catch (err) {
                        this.onException(err);
                    }
                }
            };

            /**
             *
             */
            this.isPaused = function() {
                return _paused;
            };

            /**
             * Public function to pause the long poll: a pause indication is set,
             * and the current long-poll is interrupted.
             */
            this.pause = function() {
                _paused = true;
                _api.call({
                    request : 'exit-event-monitor'
                });
            };

            /**
             * Public function to resumes the long poll after a pause: the pause
             * indication is unset, and when no long-poll is pending, a new long
             * poll is started.
             */
            this.resume = function() {
                _paused = false;
                if (!_longPollStartTime && _cometd.isOn()) {
                    this.onPollInvitation();
                }
            };

        }// UserEvent

        /**
         *
         */
        function PageLetterhead(_i18n, _view, _model) {
            var
            //_page = new Page(_i18n, _view)
            //, _self = derive(_page)
            _this = this
            //
            ;

            // this.onRefresh();
            // this.onApply()
            // this.onChange()
            // this.onDelete()
            // this.onCreate()
            // this.onShow()
            // this.onHide()

            /**
             */
            this.getSelected = function() {
                return _model.getSelLetterheadObj('#letterhead-list');
            };

            $('#page-letterhead').on('pagecreate', function(event) {

                $('#letterhead-list').change(function(event) {
                    /*
                    * The onChange() function delegates work to
                    * several other functions (etc...). Along the
                    * line a popup message dialog might be
                    * displayed, which will not work, since it is
                    * handled after this dialog is closed.
                    */

                    /*
                    * We can execute onChange() async, so its
                    * execution is unbound from the handling of
                    * this event...
                    */
                    // setTimeout(_this.onChange, 10);
                    /*
                     * ... or, act as usual and solve this at
                     * another place, by using _view.message().
                     */
                    _this.onChange();
                    return false;
                });

                $('#button-letterhead-apply').click(function() {

                    var pub = $("#sp-letterhead-public");

                    /*
                     * Executing these statements AFTER the
                     * call has NO effect, why ?!
                     */
                    $('#letterhead-list :selected').text($('#letterhead-name').val());
                    $("#letterhead-list").selectmenu('refresh');

                    _this.onApply($('#letterhead-list').val(), $('#letterhead-name').val(), _view.isRadioIdSelected('sp-letterhead-pos', 'sp-letterhead-pos-f'), _this.getSelected().pub, (pub.length > 0 && pub.is(':checked')));

                    return false;
                });

                $('#button-letterhead-delete').click(function() {
                    _this.onDelete($('#letterhead-list').val());
                    return false;
                });

                $('#button-letterhead-create').click(function() {
                    _this.onCreate();
                    return false;
                });

                $('#button-letterhead-refresh').click(function() {
                    _this.onRefresh();
                    return false;
                });

                $('#letterhead-thumbnails').on('tap', null, null, function(event) {

                    var src = $(event.target).attr('src'), html;

                    if (src) {
                        /*
                         * The letterhead thumbnail is already in full detail, so
                         * the detail pop-up will position as expected (as the height
                         * and width are known).
                         */
                        html = '<img alt="" src="' + src + '"/>';
                        html += '<a href="#" data-rel="back" class="ui-btn ui-corner-all ui-shadow ui-btn-a ui-icon-delete ui-btn-icon-notext ui-btn-right"/>';

                        $('#sp-popup-letterhead-page').html(html).enhanceWithin().popup('open', {
                            positionTo : 'window'
                        });
                    }
                });

                $('.sp-img-popup').on({
                    popupbeforeposition : function() {
                        // popupafteropen : function() {
                        var height = $(window).height() - 60 + "px";
                        $(".sp-img-popup img").height(height);
                    }
                });

            }).on("pagebeforeshow", function(event, ui) {

                _this.onShow();

            }).on("pageshow", function(event, ui) {
                /*
                 * Mantis #320: while the dialog is in view, CometD is stopped,
                 * so images get loaded in iOS Safari.
                 */
                _ns.userEvent.pause();
                _ns.cometd.stop();
                //

            }).on('pagebeforehide', function(event, ui) {

                _this.onHide();

            }).on("pagehide", function(event, ui) {
                /*
                 * Mantis #320: start CometD again.
                 */
                _ns.cometd.start(_ns.model.user.cometdToken);
                _ns.userEvent.resume();
                //
            });
        }

        /**
         *
         */
        function PageBrowser(_i18n, _view, _model) {
            var _this = this
            //
            , cssClassRotated = 'sp-img-rotated'
            //
            , _setSliderValue = function(value) {
                $('#browser-slider').val(value).slider("refresh").trigger('change');                
            }
            //
            , _navSlider = function(increment) {
                var selSlider = $('#browser-slider'), max = parseInt(selSlider.attr('max'), 10), value;
                
                if (max <= 1) {
                    return;
                }
                value = parseInt(selSlider.val(), 10);
                value += increment;
                
                if (value < 1) {
                    value = max;
                } else if (value > max) {
                    value = 1;
                }
                _setSliderValue(value);                
            }
            //
            , navRight = function() {
                _navSlider(1);
            }
            //
            , navLeft = function() {
                _navSlider(-1);
            };

            /**
             * Adds (replace) page images to the browser.
             */
            this.addImages = function() {

                var width100Percent;

                //
                if ($('#page-browser').children().length === 0) {
                    // not loaded
                    return;
                }

                width100Percent = $('#content-browser img').hasClass('fit_width');

                if (width100Percent) {
                    $('#content-browser img').addClass('fit_width');
                }

                this.adjustImages();
                this.adjustSlider();
            };

            /**
             * Adjusts the height of the browser images according to the
             * viewport.
             *
             * Rotated images are handled as landscape orientation, i.e. they are
             * adjusted to the viewport width. Other (un-rotated) images are
             * handled
             * as portrait and adjusted to the viewport height.
             */
            this.adjustImages = function() {

                var yContentPadding, yImagePadding, yFooter
                //
                , yImage, yViewPort;

                if ($('#page-browser').children().length === 0) {
                    // not loaded
                    return;
                }
                if ($('#content-browser img').hasClass('fit_width')) {
                    return;
                }

                //+------------------------------------------- Viewport
                //| (yContentPadding)
                //| +---------------------------------+ #content-browser
                //| | (yImagePadding)                 |
                //| | +-----------------------------+ #page-browser-images
                //| | |xxxxxxxxxxxxxxxxxxxxxxxxxxx  | |
                //| | |xxxxxxxxxxxxxxxxxxxxxxxxxxx  |
                //| | |...........................  | (yImage)
                //| | |xxxxxxxxxxxxxxxxxxxxxxxxxxx  |
                //| | |xxxxxxxxxxxxxxxxxxxxxxxxxxx  | |
                //| | +----------------------------+  |
                //| | (yImagePadding)                 |
                //| +---------------------------------+
                //| (yContentPadding)
                //| +---------------------------------+ #footer-browser
                //| | xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx | (yFooter)
                //| +---------------------------------+
                //| (yContentPadding)
                //+-------------------------------------------

                // vpWidth = _view.getViewportWidth();
                yViewPort = _view.getViewportHeight();

                yContentPadding = $('#content-browser').position().top;
                yImagePadding = $('#page-browser-images').position().top - $('#content-browser').position().top;
                yFooter = $('#footer-browser').outerHeight(true);

                yImage = yViewPort - 3 * yContentPadding - yFooter - 2 * yImagePadding;

                $('#content-browser img').each(function() {
                    if ($(this).hasClass(cssClassRotated)) {
                        // $(this).css({'width' : widthImg + 'px'});
                        $(this).css({
                            'height' : yImage + 'px'
                        });
                    } else {
                        $(this).css({
                            'height' : yImage + 'px'
                        });
                    }
                });
            };

            /**
             * @param nPageInView The page in the viewport.
             */
            this.setImgUrls = function(nPageInView) {

                var iPageLast, i1, i2, i3, url, imgCache, parent, images
                //
                , imgWlk, urlArray = [], iPageArray = [], imgArray = [], iImgUsed = {}
                //
                , i, j,
                //
                idImgBase = 'sp-browse-image-', prop = {};

                /*
                 * Lazy init.
                 */
                if (!_this.tnUrl2Img) {
                    _this.tnUrl2Img = {};
                }

                if (_model.myTotPages > 0) {

                    iPageLast = (_model.myTotPages - 1);

                    // zero-based page numbers
                    i2 = nPageInView - 1;
                    i1 = (i2 > 0) ? (i2 - 1) : iPageLast;
                    i3 = (i2 < iPageLast) ? (i2 + 1) : 0;

                    iPageArray = [i1, i2, i3];

                    parent = $('#page-browser-images');
                    images = parent.children();
                    images.removeClass('active');

                    /*
                     * Set all images as ready to use.
                     */
                    for ( i = 0; i < iPageArray.length; i++) {
                        iImgUsed[i] = false;
                    }

                    /*
                     *
                     */
                    for ( i = 0; i < iPageArray.length; i++) {

                        imgWlk = null;

                        url = _model.myJobPageUrlTemplate.replace("{0}", iPageArray[i]).replace("{1}", _model.uniqueImgUrlValue4Browser);

                        imgCache = _this.tnUrl2Img[url];

                        if (imgCache) {
                            /*
                             * We found the URL in cache.
                             */
                            if (iImgUsed[imgCache.i]) {
                                /*
                                 * Oops, already used.
                                 */
                                imgCache = null;
                            } else {
                                /*
                                 * Use existing image.
                                 */
                                iImgUsed[imgCache.i] = true;
                                imgWlk = imgCache.img;
                                //window.console.log('[' + i + '] FOUND [' +
                                // iPageArray[i] + '] at position [' + imgCache.i
                                // +
                                // ']');
                            }
                        }

                        if (!imgCache) {
                            /*
                             * Find first image that is not used.
                             */
                            for ( j = 0; j < iPageArray.length; j++) {
                                if (iImgUsed[j] === false) {
                                    iImgUsed[j] = true;
                                    imgWlk = images.eq(j);
                                    imgWlk.attr('src', _view.getImgSrc(url));
                                    break;
                                }
                            }

                        }

                        imgWlk.attr('id', idImgBase + i);

                        if (_model.getPageRotate(iPageArray[i])) {
                            imgWlk.addClass(cssClassRotated);
                        } else {
                            imgWlk.removeClass(cssClassRotated);
                        }

                        urlArray[i] = url;
                        imgArray[i] = imgWlk;
                    }

                    /*
                     * Active
                     */
                    $('#' + idImgBase + 1).addClass('active');

                    /*
                     * Re-order
                     */
                    $('#' + idImgBase + 1).insertAfter('#' + idImgBase + '0');
                    $('#' + idImgBase + 2).insertAfter('#' + idImgBase + 1);
                }

                /*
                 * Fill the url2img map.
                 */
                _this.tnUrl2Img = {};

                for ( i = 0; i < iPageArray.length; i++) {
                    prop = {};
                    prop.i = i;
                    prop.img = imgArray[i];

                    _this.tnUrl2Img[urlArray[i]] = prop;
                }

            };

            /**
             *
             */
            this.adjustSlider = function() {
                var selSlider = $('#browser-slider'), nPages, val, min;

                nPages = _model.myTotPages;
                val = nPages;

                if ($('#page-main-thumbnail-images .main_selected').length > 0) {

                    val = _model.getPageNumber($('#page-main-thumbnail-images img').index($('#page-main-thumbnail-images img.main_selected')));
                    /*
                     * reset, so a new page in Browse mode will select the LAST
                     * image.
                     */
                    $('#page-main-thumbnail-images img').removeClass('main_selected');
                }

                min = 1;
                if (nPages === 0) {
                    min = 0;
                }

                selSlider.attr('min', min);
                selSlider.attr('max', nPages);
                
                _setSliderValue(val);
                                
                _view.visible(selSlider, nPages > 1);
            };

            /**
             *
             */
            this.setImages = function() {
                if ($('#page-browser').children().length === 0) {
                    // not loaded
                    return;
                }
                if (_model.myJobPages !== null) {
                    this.addImages();
                }
            };
            
            $('#page-browser').on('pagecreate', function(event) {

                _this.setImages();

                $('.image_reel img').mousedown(function(e) {
                    e.preventDefault();
                });

                $(window).resize(function() {
                    _view.pages.pagebrowser.adjustImages();
                });
                // Show first image
                $(".image_reel img:first").addClass("active");

                $("#browser-slider").change(function() {

                    var val, image;

                    /*
                     * Note: the parseInt() is NEEDED to compare
                     * string values as numerics, i.e. '2' < '10'
                     * should be true
                     */
                    if (parseInt($(this).val(), 10) > parseInt($(this).attr('max'), 10)) {
                        $(this).val($(this).attr('max'));
                    }

                    val = parseInt($(this).val(), 10);

                    _this.setImgUrls(val);

                    image = $("#page-browser-images").find('img').eq(1);
                    // $(".image_reel img").removeClass('active');
                    image.addClass('active');
                    // detailedScanImageInBrowser();
                });
                $("#browser-nav-right").click(function() {
                    navRight();
                    return false;
                });

                $("#browser-nav-left").click(function() {
                    navLeft();
                    return false;
                });

                $('#page-browser-images').on('vmousedown', null, null, function(event) {
                    event.preventDefault();
                }).on('swipeleft', null, null, function(event) {
                    navRight();
                }).on('swiperight', null, null, function(event) {
                    navLeft();
                }).on('tap', null, null, function(event) {
                    // ----------------------------------------------------------
                    // IMPORTANT: make sure to handle this event last!
                    // If tap is handled before swipes, the tap event is
                    // also
                    // triggered in case of swipe event
                    // ----------------------------------------------------------
                    // So, we don't need the tolerance check any more (left
                    // here
                    // for documentation though)
                    // ----------------------------------------------------------
                    // Since this event is also triggered with swipes, use a
                    // tolerance: 10px
                    //
                    // if((Math.abs(xStart - event.pageX) < 10) &&
                    // (Math.abs(yStart -
                    // event.pageY) < 10)) {
                    // ----------------------------------------------------------
                    var images = $('#content-browser img');
                    images.toggleClass('fit_width');
                    if (images.hasClass('fit_width')) {
                        images.css('height', '');
                    } else {
                        _view.pages.pagebrowser.adjustImages();
                    }
                    // }
                    return false;
                });

                $("#browser-delete").click(function() {
                    $('#button-main-clear').click();
                    return false;
                });

            }).on("pagebeforeshow", function(event, ui) {
                var prevPage = ui.prevPage.attr('id');
                /*
                 * When we come back from page-clear we do NOT want to adjust the
                 * slider.
                 */
                if (prevPage === 'page-main') {
                    _this.adjustSlider();
                }

                /*
                 * Mantis #320: while the browser is in view, CometD is stopped,
                 * so images get loaded in iOS Safari
                 */
                _ns.userEvent.pause();
                _ns.cometd.stop();

            }).on("pageshow", function(event, ui) {

                // Adjust when page is settled.
                _this.adjustImages();

            }).on("pagehide", function(event, ui) {
                /*
                 * Mantis #320: start CometD again.
                 */
                _ns.cometd.start(_ns.model.user.cometdToken);
                _ns.userEvent.resume();
            });
        }

        /**
         *
         */
        function PageClear(_i18n, _view, _model) {
            var _this = this
            //
            ;
            // this.onClear()

            $('#page-clear').on('pagecreate', function(event) {

                $('#clear-pages-all').change(function(e) {
                    $('#clear-pages-image-ranges-label').hide();
                    $('#clear-pages-image-ranges').hide();
                });

                $('#clear-pages-custom').change(function(e) {
                    $('#clear-pages-image-ranges-label').show();
                    $('#clear-pages-image-ranges').show();
                    $('#clear-pages-image-ranges').focus();
                });

                $('#button-clear-pages-ok').click(function(e) {
                    var _ranges
                    //
                    , selected = $("input:radio[name='clear-pages']:checked").val()
                    //
                    ;

                    if (selected === 'clear-pages-all') {
                        _ranges = '1-';
                    } else if (selected === 'clear-pages-custom') {
                        _ranges = $('#clear-pages-image-ranges').val();
                    } else {
                        return false;
                    }

                    if (!$.trim(_ranges)) {
                        _view.message(_i18n.format('msg-clear-pages-range-empty', null));
                        return false;
                    }

                    _this.onClear(_ranges);

                    return false;
                });
            }).on("pagebeforeshow", function(event, ui) {
                $('#clear-pages-image-ranges').val('');
                $('input[name="clear-pages"]').prop('checked', false);
                //@JQ-1.9.1
                $('#clear-pages-custom').click().checkboxradio("refresh");
            });
        }

        /**
         *
         */
        function PageAccountTrx(_i18n, _view, _model, _api) {
            var
            // AccountTrx (common for Admin and User WebApp)
            _panel = _ns.PanelAccountTrxBase;

            /*
             *
             */
            _panel.jqId = '#content-accounttrx';

            /**
             *
             */
            $('#page-accounttrx').on('pagecreate', function(event) {

                /*
                 * AccountTrx Panel
                 */
                $(this).on('click', '#button-accounttrx-apply', null, function() {
                    _panel.page(_panel, 1);
                    return false;
                });

                $(this).on('click', '#button-accounttrx-default', null, function() {
                    _panel.applyDefaults(_panel);
                    _panel.m2v(_panel);
                    return false;
                });

                $(this).on('click', '#button-goto-doclog', null, function() {
                    _view.showUserPageAsync('#page-doclog', 'DocLog');
                    return false;
                });

                $(this).on('click', ".sp-download-receipt", null, function() {
                    _api.download("pos-receipt-download-user", null, $(this).attr('data-savapage'));
                    return false;
                });

            }).on("pagebeforeshow", function(event, ui) {
                /*
                 * _panel.input.select.user_id is only used in a Admin WebApp
                 * context. In a User WebApp context the server will use the
                 * logged
                 * in user.
                 */

                /*
                 * Reset to defaults, since a new user might have logged on.
                 */
                _panel.applyDefaults(_panel);
                _panel.refresh(_panel);
            });
        }

        /**
         *
         */
        function PageDocLog(_i18n, _view, _model, _api) {

            var
            // DocLog (common for Admin and User WebApp)
            _panel = _ns.PanelDocLogBase
            //
            ;

            _panel.jqId = '#content-doclog';

            /**
             *
             */
            $('#page-doclog').on('pagecreate', function(event) {

                /*
                 * DocLog Panel
                 */
                $(this).on('click', '#button-doclog-apply', null, function() {
                    _panel.page(_panel, 1);
                    return false;
                });

                $(this).on('click', '#button-doclog-default', null, function() {
                    _panel.applyDefaults(_panel);
                    _panel.m2v(_panel);
                    return false;
                });

                $(this).on('change', "input[name='sp-doclog-select-type']", null, function() {
                    _panel.setVisibility(_panel);
                    return false;
                });

                $(this).on('click', '#button-goto-accounttrx', null, function() {
                    _view.showUserPageAsync('#page-accounttrx', 'AccountTrx');
                    return false;
                });

            }).on("pagebeforeshow", function(event, ui) {
                /*
                 * _panel.input.select.user_id is only used in a Admin WebApp
                 * context. In a User WebApp context the server will use the
                 * logged
                 * in user.
                 */

                /*
                 * Reset to defaults, since a new user might have logged on.
                 */
                _panel.applyDefaults(_panel);
                _panel.refresh(_panel);
            });
        }

        /**
         *
         */
        function PagePdfProp(_i18n, _view, _model) {
            var _this = this
            //
            , _setVisibility, _m2V, _v2M, _allowAll, _validate;

            // this.onHide()
            // this.onShow()
            // this.onDownload()

            /*
             *
             */
            _setVisibility = function() {

                if ($("#pdf-encryption").is(':checked')) {
                    $('#pdf-allow-block').show();
                    $('#pdf-apply-security').checkboxradio('enable');
                } else {
                    $('#pdf-allow-block').hide();
                    $('#pdf-apply-security').checkboxradio('disable');
                }

                $('#pdf-apply-description').checkboxradio((($('#pdf-subject').val() + $('#pdf-keywords').val()) === '') ? 'disable' : 'enable');
                $('#pdf-apply-passwords').checkboxradio((($('#pdf-pw-user').val() + $('#pdf-pw-owner').val()) === '') ? 'disable' : 'enable');
            };

            /*
             * Model to View
             */
            _m2V = function() {
                var wlk;

                if (!_model.propPdf) {
                    return;
                }

                wlk = _model.propPdf.desc;

                $('#pdf-title').val(_model.myTitle);
                $('#pdf-author').val(wlk.author);
                $('#pdf-subject').val(wlk.subject);
                $('#pdf-keywords').val(wlk.keywords);
                _view.checkCb("#pdf-encryption", (_model.propPdf.encryption.length > 0));

                wlk = _model.propPdf.allow;
                _view.checkCb("#pdf-allow-printing", wlk.printing);
                _view.checkCb("#pdf-allow-degraded-printing", wlk.degradedPrinting);
                _view.checkCb("#pdf-allow-modify-contents", wlk.modifyContents);
                _view.checkCb("#pdf-allow-modify-annotations", wlk.modifyAnnotations);
                _view.checkCb("#pdf-allow-assembly", wlk.assembly);
                _view.checkCb("#pdf-allow-copy", wlk.copy);
                _view.checkCb("#pdf-allow-copy-for-access", wlk.copyForAccess);

                wlk = _model.propPdf.pw;
                $('#pdf-pw-owner').val(wlk.owner);
                $('#pdf-pw-owner-c').val(wlk.owner);
                $('#pdf-pw-user').val(wlk.user);
                $('#pdf-pw-user-c').val(wlk.user);

                wlk = _model.propPdf.apply;
                _view.checkCb("#pdf-apply-security", wlk.encryption);
                _view.checkCb("#pdf-apply-passwords", wlk.passwords);
                _view.checkCb("#pdf-apply-description", (wlk.subject || wlk.keywords));

                _setVisibility();

            };
            /*
             * View to Model
             */
            _v2M = function() {

                var wlk;

                _model.myTitle = $('#pdf-title').val();

                wlk = _model.propPdf.desc;
                wlk.title = _model.myTitle;
                wlk.author = $('#pdf-author').val();
                wlk.subject = $('#pdf-subject').val();
                wlk.keywords = $('#pdf-keywords').val();

                if ($('#pdf-encryption').is(':checked')) {
                    _model.propPdf.encryption = $('#pdf-encryption').val();
                } else {
                    _model.propPdf.encryption = "";
                }

                wlk = _model.propPdf.allow;
                wlk.printing = $("#pdf-allow-printing").is(':checked');
                wlk.degradedPrinting = $("#pdf-allow-degraded-printing").is(':checked');
                wlk.modifyContents = $("#pdf-allow-modify-contents").is(':checked');
                wlk.modifyAnnotations = $("#pdf-allow-modify-annotations").is(':checked');
                wlk.assembly = $("#pdf-allow-assembly").is(':checked');
                wlk.copy = $("#pdf-allow-copy").is(':checked');
                wlk.copyForAccess = $("#pdf-allow-copy-for-access").is(':checked');

                wlk = _model.propPdf.pw;
                wlk.owner = $('#pdf-pw-owner').val();
                wlk.user = $('#pdf-pw-user').val();

                wlk = _model.propPdf.apply;
                wlk.encryption = $("#pdf-apply-security").is(':checked');
                wlk.passwords = $("#pdf-apply-passwords").is(':checked');
                wlk.subject = $("#pdf-apply-description").is(':checked');
                wlk.keywords = wlk.subject;
            };

            /*
             *
             */
            _allowAll = function(allow) {
                $('#pdf-allow').find('[type=checkbox]').each(function(index) {
                    $(this).prop('checked', allow).checkboxradio("refresh");
                    //@JQ-1.9.1
                });
            };
            /*
             * Validate user input
             */
            _validate = function() {
                var msg = null
                //
                , pwO = $('#pdf-pw-owner').val()
                //
                , pwU = $('#pdf-pw-user').val();

                if (pwO !== $('#pdf-pw-owner-c').val()) {
                    $('#pdf-pw-owner-c').val('');
                    msg = 'msg-password-mismatch';
                } else if (pwU !== $('#pdf-pw-user-c').val()) {
                    msg = 'msg-password-mismatch';
                    $('#pdf-pw-user-c').val('');
                } else if (pwO !== '' && pwO === pwU) {
                    msg = 'msg-pdf-identical-owner-user-pw';
                }
                if (msg !== null) {
                    _view.message(_i18n.format(msg, null));
                    return false;
                }
                return true;
            };

            /*
             *
             */
            $('#page-pdf-properties').on('pagecreate', function(event) {

                $("#pdf-encryption").on("change", null, null, function(event, ui) {
                    _setVisibility();
                });

                $('.sp-pdf-apply-src').on("change", null, null, function(event, ui) {
                    _setVisibility();
                });

                $('#button-pdf-properties-default').click(function() {
                    _model.propPdf = _model.propPdfDefault;
                    _m2V();
                    return false;
                });

                $('#button-pdf-allow-all').click(function() {
                    _allowAll(true);
                    return false;
                });

                $('#button-pdf-allow-none').click(function() {
                    _allowAll(false);
                    return false;
                });

                $('#button-pdf-download').click(function() {
                    if (_validate()) {
                        _v2M();
                        _this.onDownload();
                    }
                    // IMPORTANT return false !!!
                    return false;
                });

                $('#button-pdf-send').click(function() {
                    if (_validate()) {
                        _v2M();
                        _view.showUserPageAsync('#page-send', 'Send');
                    }
                    return false;
                });

                $('#pdf-title-list').change(function(event) {
                    _model.myTitle = $('#pdf-title-list :selected').text();
                    $('#pdf-title').val(_model.myTitle);
                    $(this).val('').selectmenu('refresh');
                    return false;
                });

            }).on("pagebeforeshow", function(event, ui) {
                _m2V();
                _this.onShow();
            }).on('pagebeforehide', function(event, ui) {
                if (_validate()) {
                    _v2M();
                    _this.onHide();
                } else {
                    _view.changePage($(this));
                    // stay on this page
                }
            });
        }

        /**
         *
         */
        function PageVoucherRedeem(_i18n, _view, _model) {
            var _this = this
            //
            ;

            $("#page-voucher-redeem").on("pagecreate", function(event) {

                $('#button-voucher-redeem-ok').click(function() {
                    var sel = $("#voucher-redeem-card-number");
                    _this.onRedeemVoucher(sel.val());
                    sel.val("");
                    return false;
                });

            }).on("pagebeforeshow", function(event, ui) {
                $("#voucher-redeem-number").val("");
            });
        }

        /**
         *
         */
        function PageOutbox(_i18n, _view, _model) {
            var _this = this
            //
            , _close = function() {
                $('#button-outbox-back').click();
                return false;
            }
            //
            , _refresh = function() {
                var html = _view.getUserPageHtml('OutboxAddin');
                if (html) {
                    $('#outbox-job-list').html(html);
                    /*
                     * JQM 1.4.0: strange, the listview needs a separate
                     * refresh.
                     */
                    $('#page-outbox').enhanceWithin();
                    $('#outbox-job-list').listview('refresh');
                    
                    $('.sparkline-printout-pie').sparkline('html', {
                        type : 'pie',
                        sliceColors : [_view.colorPrinter, _view.colorSheet]
                    });
                }
                return false;
            }
            //
            ;

            $('#page-outbox').on("pagecreate", function(event) {

                $('#button-outbox-clear').click(function() {
                    _this.onOutboxClear();
                    return _close();
                });
                                
                $('#button-outbox-extend').click(function() {
                    _this.onOutboxExtend();
                    return _close();
                });
                
                $(this).on('click', '.sp-outbox-remove-job', null, function() {
                    _this.onOutboxDeleteJob($(this).attr('data-savapage'));
                    if (_model.user.stats.outbox.jobCount > 0) {
                        return _refresh(); 
                    } 
                    return _close();                    
                });
                
            //}).on("pagebeforeshow", function(event, ui) {
            }).on("pageshow", function(event, ui) {
                _refresh();                
            //}).on('pagebeforehide', function(event, ui) {                
            });
        }

        /**
         *
         */
        function PageSend(_i18n, _view, _model) {
            var _this = this
            //
            ;

            $("#page-send").on("pagecreate", function(event) {

                $('#button-send-send').click(function() {
                    _this.onSend($('#send-mailto').val(), _model.pdfPageRanges, _model.removeGraphics);
                    $('#pdf-page-ranges').val('');
                    return false;
                });

                $('#button-send-settings-default').click(function() {
                    $('#send-mailto').val(_model.user.mailDefault);
                    return false;
                });
            }).on("pagebeforeshow", function(event, ui) {
                $('#send-mailto').val(_model.user.mail);
            }).on('pagebeforehide', function(event, ui) {
                _model.user.mail = $('#send-mailto').val();
            });
        }

        /**
         * Constructor
         */
        function PageUserPinReset(_i18n, _view) {

            var _page, _self, _onSelectReset;

            _page = new _ns.Page(_i18n, _view, '#page-user-pin-reset', 'UserPinReset');
            _self = _ns.derive(_page);

            /**
             *
             */
            _self.onSelectReset = function(foo) {
                _onSelectReset = foo;
            };

            /**
             *
             */
            $(_self.id()).on('pagecreate', function(event) {

                $('#button-user-pin-reset').click(function(e) {
                    if (_view.checkPwMatch($('#user-pin-reset'), $('#user-pin-reset-confirm'))) {
                        _onSelectReset($("#user-pin-reset").val());
                    }
                    return false;
                });
            }).on('pagebeforehide', function(event, ui) {
                $("#user-pin-reset").val('');
                $("#user-pin-reset-confirm").val('');
            });
            /*
             * IMPORTANT
             */
            return _self;
        }

        /**
         *
         */
        function PageDashboard(_i18n, _view, _model) {

            $('#page-dashboard').on('pagecreate', function(event) {

                if ($('#button-user-pw-dialog')) {
                    $(this).on('click', '#button-user-pw-dialog', null, function() {
                        _view.showPageAsync('#page-user-pw-reset', 'UserPasswordReset', function() {
                            $('#user-pw-reset-title').html(_model.user.id);
                        });
                        return false;
                    });
                }
                if ($('#button-user-pin-dialog')) {
                    $(this).on('click', '#button-user-pin-dialog', null, function() {
                        _view.showPageAsync('#page-user-pin-reset', 'UserPinReset', function() {
                            $('#user-pin-reset-title').html(_model.user.id);
                        });
                        return false;
                    });
                }

                if ($('#button-redeem-voucher')) {
                    $(this).on('click', '#button-voucher-redeem', null, function() {
                        _view.showUserPage('#page-voucher-redeem', 'AccountVoucherRedeem');
                        return false;
                    });
                }

                $(window).resize(function() {
                    var sel = $('#dashboard-piechart')
                    //
                    , width = sel.parent().width();
                    sel.width(width);
                    try {
                        _model.dashboardPiechart.replot({});
                    } catch(ignore) {
                        // replot() get throw error: no code intended.
                    }
                });

            }).on("pagebeforeshow", function(event, ui) {

                _view.visible($('.sp-internal-user'), _model.user.internal);

            });
        }

        // =========================================================================
        /**
         * Constructor
         */
        function PageFileUpload(_i18n, _view, _model) {

            /*
             * Page is pre-loaded, so no _class needed
             */
            var _page = new _ns.Page(_i18n, _view, '#page-file-upload')
            //
            , _self = _ns.derive(_page)
            //
            ;

            /**
             *
             */
            $(_self.id()).on('pagecreate', function(event) {

                $('#file-upload-feedback').hide();
                // initial hide
                $('#button-file-upload-submit').on('click', null, null, function() {
                    $('#file-upload-feedback').show();
                    return true;
                });

            }).on("pagebeforeshow", function(event, ui) {

                _ns.deferAppWakeUp(true);

            }).on('pagebeforehide', function(event, ui) {
                /*
                 * Clear and Hide content
                 */
                $('#file-upload-feedback').hide();
                $('#file-upload-feedback').html('');
                $('#button-file-upload-reset').click();

                /*
                 * IMPORTANT: _ns.deferAppWakeUp(false) is performed in
                 * main.onShow()
                 */

            });
            return _self;
        }

        // =========================================================================
        /**
         * Constructor
         */
        function PageMain(_i18n, _view, _model) {

            var _this = this
            //
            , _IMG_PADDING = 3
            //
            , _IMG_BORDER = 0
            //
            , _isEditMode = false
            //
            , _totImages = 0
            //
            , _mousedownPosLeft, _mousedownPageX, _mousedownTarget
            //
            , _isThumbnailDragged = false
            //
            , _IMG_WIDTH = function() {
                return _model.MY_THUMBNAIL_WIDTH;
            }
            //
            , _moveLeft, _moveRight, _moveToBegin, _moveToEnd, _moveJobs
            //
            , _showArrange, _showCutPageRanges, _getCutPageRanges, _countProp
            //
            , _getJobRanges, _getFirstJob, _showArrButtons
            //
            , _onThumbnailTap, _onThumbnailTapHold, _onPageInfoTap
            // mapping page URLs to jQuery <img> selectors
            , _tnUrl2Img;

            // for now ...
            this.id = '#page-main';

            // this.onCreated = null;
            // this.onLogout = null;
            // this.onClose = null;
            // this.onPollToggle = null;
            // this.onShow = null;
            // this.onHide = null;
            // this.onRefreshPages = null;
            // this.onExpandPage = null;
            // this.onPageMove = null;
            // this.onPageDelete = null;
            // this.onPopupJobApply = null;
            // this.onPopupJobDelete = null;

            /**
             * Clears all traces of previous editing.
             */
            this.clearEditState = function() {
                _showArrange(_isEditMode);
            };

            /**
             *
             */
            this.showUserStats = function() {
                var stats = _model.user.stats
                //
                , outbox
                //
                , pages = 0
                //
                , status
                //
                ;

                if (stats) {
                    
                    $('#sparkline-user-pie').sparkline([stats.pagesPrintOut, stats.pagesPrintIn, stats.pagesPdfOut], {
                        type : 'pie',
                        sliceColors : _view.userChartColors
                    });
                    
                    status = stats.accountInfo.status;
                    $('#mini-user-balance').html(stats.accountInfo.balance).attr("class", status === "CREDIT" ? "sp-txt-warn" : (status === "DEBIT" ? "sp-txt-valid" : "sp-txt-error"));
                    
                    outbox = _model.user.stats.outbox;
                }
                
                if (outbox) {
                                
                    $.each(outbox.jobs, function(key, job) {
                        pages += job.copies * job.pages;                        
                    });

                    _view.visible($('#button-mini-outbox'), outbox.jobCount > 0);
                                                            
                    $('#button-mini-outbox .sp-outbox-remaining-time').html(outbox.localeInfo.remainTime);
                    $('#button-mini-outbox .sp-outbox-jobs').html(outbox.jobCount);
                    $('#button-mini-outbox .sp-outbox-pages').html(pages);
                    $('#button-mini-outbox .sp-outbox-cost').html(outbox.localeInfo.cost);
                }

                _model.setJobsMatchMediaSources(_view);

            };

            /**
             * Initializes the nUrl2Img map with the pages from the _model.
             */
            _tnUrl2Img = function() {

                var i = 0;

                $('#page-main-thumbnail-images img').each(function() {
                    var prop = {};
                    prop.i = i;
                    prop.img = $(this);
                    _this.tnUrl2Img[_model.myJobPages[i].url] = prop;
                    //window.console.log('[' + i + ']');
                    i = i + 1;
                });
            };

            /**
             *
             */
            this.setThumbnails2Load = function() {

                if (!_this.tnUrl2Img) {
                    _this.tnUrl2Img = {};
                }

                _ns.thumbnails2Load = 0;
                $.each(_model.myJobPages, function(key, page) {

                    if (!_this.tnUrl2Img[page.url]) {
                        _ns.thumbnails2Load++;
                    }
                });

            };

            /**
             *
             */
            this.setThumbnails = function() {

                var wlkPageNr = 1
                //
                , divPrv
                //
                , imgContainer = $('#page-main-thumbnail-images');

                if (!_this.tnUrl2Img) {
                    _this.tnUrl2Img = {};
                }

                /*
                 * HTML INJECT RUN: Iterate the incoming pages and check if
                 * <img> for url is already there.
                 */
                $.each(_model.myJobPages, function(key, page) {

                    var divCur, item, tnUrl, span
                    //
                    , imgWidth = _IMG_WIDTH()
                    //
                    , imgHeightA4 = imgWidth * 1.4
                    //
                    ;

                    tnUrl = _this.tnUrl2Img[page.url];

                    if (tnUrl) {
                        /*
                         * Yes, <img> for url is already there: move to current
                         * position.
                         */
                        divCur = tnUrl.img.parent();
                    } else {
                        /*
                         * Mantis #320: we set the 'height' attribute with the
                         * A4 assumption, later on at the removeImgHeight() the
                         * height is removed so the image will show with the
                         * fixed width and the proper height (ratio).
                         */
                        item = "";
                        item += '<div><img onload="org.savapage.removeImgHeight(this)" width="' + imgWidth + '" height="' + imgHeightA4 + '" border="0" src="' + _view.getImgSrc(page.url) + '" style="padding: ' + _IMG_PADDING + 'px;"/>';
                        item += '<a title="' + page.media + '" href="#" class="sp-thumbnail-subscript ui-btn ui-mini" style="margin-top:-' + (2 * _IMG_PADDING + 1) + 'px; margin-right: ' + _IMG_PADDING + 'px; margin-left: ' + _IMG_PADDING + 'px; border: none; box-shadow: none;">';
                        item += '<span class="sp-thumbnail-page"/>';
                        item += '<span class="sp-thumbnail-tot-pages"/>';
                        item += '<span class="sp-thumbnail-tot-chunk"/>';
                        item += '</a></div>';

                        divCur = $(item);

                    }

                    if (divPrv) {
                        divCur.insertAfter(divPrv);
                    } else {
                        imgContainer.prepend(divCur);
                    }

                    /*
                     * Set the logical page sequence number.
                     */
                    span = divCur.find('.sp-thumbnail-page');
                    span.text(wlkPageNr + '/');
                    if (page.drm) {
                        span.addClass('sp-txt-warn');
                    } else {
                        span.removeClass('sp-txt-warn');
                    }

                    /*
                     * Set the page total.
                     */
                    span = divCur.find('.sp-thumbnail-tot-pages');
                    if (page.drm) {
                        span.addClass('sp-txt-warn');
                    } else {
                        span.removeClass('sp-txt-warn');
                    }
                    /*
                     * Set the chunk total.
                     */
                    span = divCur.find('.sp-thumbnail-tot-chunk');
                    if (page.drm) {
                        span.addClass('sp-txt-warn');
                    } else {
                        span.removeClass('sp-txt-warn');
                    }
                    if (page.pages > 1) {
                        span.text(' (' + page.pages + ')');
                    } else {
                        span.text('');
                    }

                    //--------------------------
                    wlkPageNr += page.pages;
                    divPrv = divCur;
                });

                if (_model.myTotPages === 0) {
                    imgContainer.empty();
                } else {
                    /*
                     * Remove existing thumbnails that were not used.
                     */
                    imgContainer.find('div:gt(' + (_model.myJobPages.length - 1) + ')').remove();

                    /*
                     * Update totals for thumbnail already there.
                     */
                    $.each(imgContainer.find('.sp-thumbnail-tot-pages'), function(key, value) {
                        $(this).text(_model.myTotPages);
                    });
                }

                /*
                 * Trigger jQuery Mobile
                 */
                imgContainer.enhanceWithin();

                this.adjustThumbnailVisibility();

                if (_model.myTotPages > 0) {
                    $('#page-main-thumbnail-viewport').removeClass('thumbnail_viewport_empty');
                } else {
                    $('#page-main-thumbnail-viewport').addClass('thumbnail_viewport_empty');
                }

                //
                _tnUrl2Img();
            };

            /*
             * Thumbnail viewport
             */
            _moveLeft = function() {
                var right = $('.thumbnail_reel').position().left;
                if (right > (_totImages - 1) * -_IMG_WIDTH()) {
                    $('.thumbnail_reel').animate({
                        'right' : right,
                        'left' : (right - _IMG_WIDTH())
                    });
                }
            };
            _moveRight = function() {
                var right = $('.thumbnail_reel').position().left;
                if (right <= 0) {
                    $('.thumbnail_reel').animate({
                        'right' : right,
                        'left' : (right + _IMG_WIDTH())
                    });
                }
            };
            _moveToBegin = function() {
                
                /*
                 * We check the INNER width and calculate with the OUT width
                 * NOTE: it seems that INNER width is zero when a popup dialog is
                 * overlayed.
                 */
                if ($('#page-main-thumbnail-viewport').innerWidth() > 0) {
                    /*
                    * IMPORTANT: offset() does NOT work in Google Chrome therefore
                    * we use css().
                    */
                    $('.thumbnail_reel').css({
                        'left' : $('#page-main-thumbnail-viewport').offset().left + $('#thumbnail_nav_l').outerWidth() + 'px'
                    });
                }
            };
            _moveToEnd = function() {

                var widthViewport;

                /*
                 * We check the INNER width and calculate with the OUT width
                 * NOTE: it seems that INNER width is zero when a popup dialog is
                 * overlayed.
                 */
                if ($('#page-main-thumbnail-viewport').innerWidth() > 0) {
                    /*
                     * IMPORTANT: offset() does NOT work in Google Chrome
                     * therefore we use css().
                     */
                    widthViewport = $('#page-main-thumbnail-viewport').outerWidth();

                    $('.thumbnail_reel').css({
                        'left' : (widthViewport - $('#page-main-thumbnail-images').outerWidth() - $('#thumbnail_nav_r').outerWidth() + 'px')
                    });
                }
            };

            _showArrButtons = function(show) {
                var selEdit = $('.main_arr_edit')
                //
                , selPaste = $('.main_arr_paste')
                //
                , selUndo = $('#main-arr-undo')
                //
                , bCut = (_countProp(_model.myCutPages) > 0);

                if (show) {
                    selEdit.removeClass('ui-disabled');
                    if (bCut) {
                        selPaste.removeClass('ui-disabled');
                    } else {
                        selPaste.addClass('ui-disabled');
                    }
                } else {
                    selEdit.addClass('ui-disabled');
                }
                if (bCut) {
                    selUndo.removeClass('ui-disabled');
                } else {
                    selUndo.addClass('ui-disabled');
                }
            };

            /**
             *
             */
            _showArrange = function(bShow) {
                $('#page-main-thumbnail-images div').removeClass('sp-thumbnail-selected').removeClass('sp-thumbnail-cut');
                _model.myCutPages = {};
                _showCutPageRanges();
                _showArrButtons(false);

                _view.visible($('.main_action_arrange'), bShow);
                _view.visible($('.main_action'), !bShow);

                _isEditMode = bShow;
            };

            /*
             * Calculates the formatted real page range from thumbnail pages with
             * a
             * certain CSS class. The format is: "n,n,...".
             *
             * @param {string} cssClass The CSS class. @returns {string} The
             * formatted page range.
             */
            _getJobRanges = function(cssClass) {
                var ranges = null, nOffset = 0, iOffset = 0
                //
                , tn = $('#page-main-thumbnail-images div');

                $.each($('.' + cssClass), function(key, obj) {
                    if (ranges === null) {
                        ranges = '';
                        iOffset = tn.index(obj);
                        nOffset = _model.getPageNumber(iOffset);
                    } else {
                        ranges += ",";
                    }
                    ranges += nOffset + (tn.index(obj) - iOffset);
                });
                return ranges;
            };
            /*
             *
             */
            _getFirstJob = function(selClass) {
                var tn = $('#page-main-thumbnail-images div'),
                //
                first = $('.' + selClass).first();

                if (first !== null) {
                    first = (tn.index(first) + 1);
                }
                return first;
            };

            /**
             *
             */
            _moveJobs = function(bBefore) {
                var ranges = _getCutPageRanges()
                //
                , position = _model.getPageNumber(_getFirstJob('sp-thumbnail-selected')) - 1;

                if (ranges !== null && position !== null) {
                    if (!bBefore) {
                        position += 1;
                    }
                    // alert('move ['+ranges+'] at ['+position+']');
                    _this.onPageMove(ranges, position);
                    _showArrButtons(false);
                }
            };

            /**
             * Counts the number of properties in an object.
             *
             * Since IE8 does NOT support Object.keys(_model.myCutPages).length
             * we
             * use a workaround.
             */
            _countProp = function(obj) {
                var n = 0, p;
                for (p in obj) {
                    n++;
                }
                return n;
            };

            /**
             *
             */
            _showCutPageRanges = function() {
                if (_countProp(_model.myCutPages) > 0) {
                    $('#main-page-range-cut').html(_getCutPageRanges());
                    $('#button-mini-cut').show();
                } else {
                    $('#button-mini-cut').hide();
                }
            };
            /**
             * Creates a string with page range format from the cut pages.
             *
             * Example: '3-4,7,9-11'
             */
            _getCutPageRanges = function() {
                var ranges = '', pageStart, pageEnd, pagePrv
                //
                , page
                //
                , addRange = function() {
                    if (ranges !== '') {
                        ranges += ',';
                    }
                    ranges += pageStart;
                    if (pageStart !== pageEnd) {
                        ranges += '-' + pageEnd;
                    }
                };

                for (page in _model.myCutPages) {
                    if (pagePrv) {
                        if (parseInt(pagePrv, 10) + 1 === parseInt(page, 10)) {
                            pageEnd = page;
                        } else {
                            addRange();
                            pagePrv = null;
                        }
                    }
                    if (!pagePrv) {
                        pageStart = page;
                        pageEnd = page;
                    }
                    pagePrv = page;
                }
                if (pagePrv) {
                    addRange();
                }
                return ranges;
            };

            /**
             * Adjusts the visibility (width, padding, hide/show) of the
             * thumbnail
             * images, and move the last image to the right side (moveToEnd)
             */
            this.adjustThumbnailVisibility = function() {

                var widthTot = 0, tn, selMainAct = $('.main_actions'), selPdfButton, widthImg;

                _totImages = 0;

                $(".thumbnail_reel img").each(function(index, image) {
                    _totImages += 1;
                    $(image).css({
                        'width' : _IMG_WIDTH() + 'px',
                        'padding' : _IMG_PADDING + 'px'
                    });
                });

                widthImg = (_IMG_WIDTH() + 2 * _IMG_PADDING + 2 * _IMG_BORDER);
                widthTot = widthImg * _totImages;

                $('.thumbnail_reel').css({
                    'width' : +widthTot + 'px'
                });

                _moveToEnd();

                if (_totImages === 0) {
                    selMainAct.addClass('ui-disabled');
                } else {
                    selMainAct.removeClass('ui-disabled');
                    selPdfButton = $('#button-main-pdf-properties');
                    if (_model.myJobsDrm) {
                        selPdfButton.addClass('ui-disabled');
                    } else {
                        selPdfButton.removeClass('ui-disabled');
                    }
                }

                /*
                 * Set the CSS class for cut thumbnails.
                 */
                tn = $('#page-main-thumbnail-images div');

                $.each(tn, function(key, obj) {
                    var n = _model.getPageNumber(tn.index($(this)));
                    if (_model.myCutPages[n] === true) {
                        $(this).addClass('sp-thumbnail-cut');
                    } else {
                        $(this).removeClass('sp-thumbnail-cut');
                    }
                });

                _showCutPageRanges();
            };

            /*
             *
             */
            _onPageInfoTap = function(sel) {

                var iImg = -1, page, job, sel2
                //
                , cssCls = 'sp-thumbnail-subscript'
                //
                ;
                /*
                 * We try to find out which part of the <a> button was tapped by
                 * querying the discriminating CSS class. If no class is found we
                 * check if the <img> was tapped.
                 */
                if (!sel.hasClass(cssCls)) {
                    cssCls = 'sp-thumbnail-tot-pages';
                    if (!sel.hasClass(cssCls)) {
                        cssCls = 'sp-thumbnail-page';
                        if (!sel.hasClass(cssCls)) {
                            cssCls = 'sp-thumbnail-tot-chunk';
                            if (!sel.hasClass(cssCls)) {
                                cssCls = null;
                            }
                        }
                    }
                }
                /*
                 * Find out the zero-based index of the tapped object.
                 */
                sel2 = '#page-main-thumbnail-images ';
                if (cssCls) {
                    sel2 += '.' + cssCls;
                } else if (sel.attr('src')) {
                    /*
                     * <img> was tapped.
                     */
                    sel2 += 'img';
                }

                iImg = $(sel2).index(sel);

                if (iImg > -1) {

                    page = _model.myJobPages[iImg];
                    job = _model.myJobs[page.job];

                    _model.iPopUpJob = page.job;

                    $('#popup-job-info-name').html(job.title);
                    $('#popup-job-info-pages-selected').html(job.pagesSelected);
                    $('#popup-job-info-pages').html(job.pages);

                    $('#popup-job-info-media').html(job.media);
                    $('#popup-job-info-drm').html(job.drm ? '&nbsp;DRM' : '');

                    _view.checkCb("#sp-popup-job-undelete", false);
                    _view.checkCb("#sp-popup-job-rotate", (job.rotate !== '0'));

                    $('#sp-popup-job-info').enhanceWithin().popup('open', {
                        positionTo : '#page-main-thumbnail-images .sp-thumbnail-page:eq(' + iImg + ')'
                    });

                    if (job.pagesSelected === job.pages) {
                        $('#sp-popup-job-undelete').checkboxradio('disable');
                    } else {
                        $('#sp-popup-job-undelete').checkboxradio('enable');
                    }
                }
            };

            /**
             *
             */
            _onThumbnailTapHold = function(thumbnail) {
                // for now ...
                _onPageInfoTap(thumbnail);
            };

            /**
             *
             */
            _onThumbnailTap = function(thumbnail) {

                var tn
                //
                , iImage = $('#page-main-thumbnail-images img').index(thumbnail);

                if (iImage < 0) {
                    return;
                }

                $('#page-main-thumbnail-images img').removeClass('main_selected');

                if (_model.myJobPages[iImage].pages === 1) {
                    /*
                     * Image representing a single page
                     */
                    if (_isEditMode) {
                        tn = thumbnail.parent();
                        if (tn.hasClass('sp-thumbnail-cut')) {

                            tn.removeClass('sp-thumbnail-cut');
                            delete _model.myCutPages[_model.getPageNumber(iImage)];
                            _showCutPageRanges();

                        } else {
                            tn.toggleClass('sp-thumbnail-selected');
                        }
                        _showArrButtons($('.sp-thumbnail-selected').length > 0);
                    } else {
                        thumbnail.addClass('main_selected');
                        _view.changePage($('#page-browser'));
                    }
                } else {
                    /*
                     * Image representing multiple pages
                     */
                    _this.onExpandPage(_model.getPageNumber(iImage));
                }
            };
            /**
             *
             */
            $('#page-main').on('pagecreate', function(event) {

                var taphold = false
                //
                , widthImg = (_IMG_WIDTH() + 2 * _IMG_PADDING + 2 * _IMG_BORDER)
                // Use the ratio of ISO A4 + extra padding
                , maxHeight = widthImg * (297 / 210) + 8 * _IMG_PADDING + 2 * _IMG_BORDER;

                $('#page-main-thumbnail-viewport').css({
                    'height' : +maxHeight + 'px'
                });

                _showArrange(false);

                /*
                 * Hide text in buttons, show text as title instead.
                 */
                $(".sp-nav-button-txt").hide();
                $.each($(".sp-nav-button-txt"), function(key, obj) {
                    $(this).hide();
                    $(this).closest("li").attr("title", "\n " + $(this).text() + " \n");
                });

                // Now that the images are loaded (needed for iOS Safari), hide
                // them.
                $(".sp-main-status-ind").hide();

                /*
                 *
                 */
                $('#thumbnail_nav_r').css('right', $('#page-main-thumbnail-viewport').innerWidth() + 'px');

                // Thumbnail viewport
                $('.thumbnail_reel img').mousedown(function(e) {
                    e.preventDefault();
                });
                // Button on fixed footer
                $('#button-cometd-status').click(function() {
                    _view.showPageAsync('#page-info', 'AppAbout');
                    return false;
                });

                $('#button-mini-upload').click(function() {
                    /*
                     * This page is a fixed part of WebAppUserPage.html
                     */
                    _view.changePage('#page-file-upload');
                    return false;
                });

                $('#button-mini-outbox').click(function() {
                    /*
                     * This page is a fixed part of WebAppUserPage.html
                     */
                    _view.changePage('#page-outbox');
                    return false;
                    
                });

                $('#button-mini-user').click(function() {

                    var html, xydata, piedata, pageId = '#page-dashboard';

                    /*
                     * This page is a fixed part of WebAppUserPage.html
                     */
                    _view.changePage(pageId);

                    html = _view.getUserPageHtml('UserDashboard');

                    if (html) {

                        /*
                         * Update HTML.
                         */
                        $('#dashboard-title').html(_model.user.id);
                        $('#dashboard-list').html(html);

                        /*
                         * JQM 1.4.0: strange, the listview needs a separate
                         * refresh.
                         */
                        $(pageId).enhanceWithin();
                        $('#dashboard-list').listview('refresh');

                        //----
                        xydata = _view.jqPlotData('dashboard-xychart', false);
                        piedata = _view.jqPlotData('dashboard-piechart', false);

                        if (!xydata || !piedata) {
                            return;
                        }
                        if (_model.dashboardPiechart) {
                            /*
                             * IMPORTANT: Release all resources occupied by the
                             * jqPlot.
                             * NOT releasing introduces a HUGE memory leak, each
                             * time
                             * the plot is refreshed.
                             */
                            _model.dashboardPiechart.destroy();
                        }
                        _model.dashboardPiechart = _view.showPieChart('dashboard-piechart', piedata);
                        // NOT NOW: wait till week get displayed on x-axis
                        //_view.showXyChart('dashboard-xychart', xydata);
                    }
                    return false;
                });

                $('#button-logout').click(function() {
                    _this.onLogout();
                    return false;
                });

                /*
                 *
                 */
                $('#sp-popup-job-apply').click(function() {
                    _this.onPopupJobApply();
                });
                $('#sp-popup-job-delete').click(function() {
                    _this.onPopupJobDelete();
                });
                /*
                 *
                 */
                $('#button-main-clear').click(function() {
                    _view.showUserPageAsync('#page-clear', 'Clear');
                    return false;
                });

                $('#button-main-print').click(function() {
                    _view.showUserPageAsync('#page-print', 'Print');
                    return false;
                });

                $('#button-main-pdf-properties').click(function() {
                    _view.showUserPageAsync('#page-pdf-properties', 'PdfProperties');
                    return false;
                });

                $('#button-main-letterhead').click(function() {
                    _view.showUserPageAsync('#page-letterhead', 'Letterhead');
                    return false;
                });

                $('#button-main-doclog').click(function() {
                    _view.showUserPageAsync('#page-doclog', 'DocLog');
                    return false;
                });

                $('#button-browser').click(function() {
                    _view.changePage($('#page-browser'));
                    return false;
                });

                $('#button-main-refresh').click(function() {
                    _this.onRefreshApp();
                    return false;
                });

                $('#thumbnail_nav_l').on('tap', null, null, function() {
                    if (taphold) {
                        taphold = false;
                    } else {
                        _moveLeft();
                    }
                }).on('taphold', null, null, function(event) {
                    // prevent default behavior (selection of element)
                    event.preventDefault();
                    taphold = true;
                    _moveToBegin();
                });

                $('#thumbnail_nav_r').on('tap', null, null, function() {
                    if (taphold) {
                        taphold = false;
                    } else {
                        _moveRight();
                    }
                }).on('taphold', null, null, function(event) {
                    // prevent default behavior (selection of element)
                    event.preventDefault();
                    taphold = true;
                    _moveToEnd();
                });

                $(window).resize(function() {
                    _this.adjustThumbnailVisibility();
                });

                $('#page-main-thumbnail-images').on('vmousedown', null, null, function(event) {
                    event.preventDefault();
                    // !!!!!!
                    _model.myIsDragging = true;
                    _isThumbnailDragged = false;
                    _mousedownPosLeft = $(this).position().left;
                    _mousedownPageX = event.pageX;
                    _mousedownTarget = $(event.target);

                }).on('vmousemove', null, null, function(event) {
                    if (_model.myIsDragging) {

                        // needed for IE8
                        event.preventDefault();

                        // -------------------------------------------------------
                        // css() seems to be much faster than offset().
                        // -------------------------------------------------------
                        $(this).css({
                            'left' : _mousedownPosLeft - (_mousedownPageX - event.pageX) + 'px'
                        });

                        if (!_isThumbnailDragged) {
                            _isThumbnailDragged = (Math.abs(_mousedownPageX - event.pageX) > (_IMG_WIDTH() / 2));
                        }
                    }
                }).on('vmouseup', null, null, function(event) {
                    _model.myIsDragging = false;

                }).on('mouseup', null, null, function(event) {
                    /*
                     * IMPORTANT: it is crucial to stop
                     * dragging mode at all times when a any
                     * mouseup event occurs. E.g. Opera
                     * triggers this plain 'mouseup' (JQM
                     * does not convert this into the
                     * virtual 'vmouseup'event).
                     */
                    _model.myIsDragging = false;

                }).on('tap', null, null, function(event) {

                    _model.myIsDragging = false;

                    if (taphold) {
                        taphold = false;
                        return false;
                    }
                    if (!_isThumbnailDragged) {

                        if (_mousedownTarget.attr('src')) {
                            // this is a tap on the <img>
                            _onThumbnailTap(_mousedownTarget);
                        } else {
                            _onPageInfoTap(_mousedownTarget);
                        }
                        return false;
                    }

                }).on('taphold', null, null, function(event) {

                    if (!_isThumbnailDragged) {
                        taphold = true;
                        _model.myIsDragging = false;
                        _onThumbnailTapHold(_mousedownTarget);
                        return false;
                    }
                });

                // ----------------------------------------------------------------------
                // Actions to arranging the jobs
                // ----------------------------------------------------------------------
                $('#main-arr-select-all').click(function() {
                    $('#page-main-thumbnail-images div').addClass('sp-thumbnail-selected');
                    _showArrButtons(true);
                    return false;
                });
                $('#main-arr-unselect-all').click(function() {
                    $('#page-main-thumbnail-images div').removeClass('sp-thumbnail-selected');
                    _showArrButtons(false);
                    return false;
                });
                $('#main-arr-undo').click(function() {
                    $('#page-main-thumbnail-images div').removeClass('sp-thumbnail-selected').removeClass('sp-thumbnail-cut');
                    _model.myCutPages = {};
                    _showCutPageRanges();
                    _showArrButtons(false);
                    return false;
                });

                /*
                 *
                 */
                $('#main-arr-cut').click(function() {

                    $('.sp-thumbnail-selected').addClass('sp-thumbnail-cut').removeClass('sp-thumbnail-selected');

                    var tn = $('#page-main-thumbnail-images div');

                    /*
                     * Update myCutPages by traverse
                     * each thumbnail
                     */
                    $.each(tn, function(key, obj) {
                        var n = _model.getPageNumber(tn.index($(this)));
                        if ($(this).hasClass('sp-thumbnail-cut')) {
                            _model.myCutPages[n] = true;
                        } else {
                            if (_model.myCutPages[n]) {
                                delete _model.myCutPages[n];
                            }
                        }
                    });
                    _showCutPageRanges();
                    _showArrButtons(false);
                    return false;
                });

                $('#main-arr-paste-b').click(function() {
                    _moveJobs(true);
                    return false;
                });

                $('#main-arr-paste-a').click(function() {
                    _moveJobs(false);
                    return false;
                });

                $('#main-arr-delete').click(function() {
                    // _mousedownTarget.removeClass('main_selected');
                    $('#page-main-thumbnail-images img').removeClass('main_selected');
                    var ranges = _getJobRanges('sp-thumbnail-selected');
                    if (ranges !== null) {
                        //alert('delete ['+ranges+']');
                        _this.onPageDelete(ranges);
                        _showArrButtons(false);
                    }
                    return false;
                });

                $('#main-arr-return').click(function() {
                    _showArrange(false);
                    return false;
                });
                $('#main-arr-edit').click(function() {
                    _showArrange(true);
                    return false;
                });

                // Last, but not least!!
                _this.onCreated();

            }).on("pageshow", function(event, ui) {
                /* @2014-02-20
                 *
                 * Use "pageshow" event instead of "pagebeforeshow", because
                 * the pie sparkline (in footer) needs to be settled first,
                 * before its data can be rendered.
                 *
                 * @2012-10-17
                 *
                 * IMPORTANT: do NOT kick the refresh because the long
                 * polling CometD does the job, i.e. the first poll gives
                 * back the current pages.
                 *
                 * WARNING: executing $('#button-main-refresh').click();
                 * will result in a concurrent retrieve of the same images.
                 * E.g. in Opera Browser this seems to be highly optimized
                 * (as compared to Chromium, Firefox) since Wicket throws
                 * java.lang.IllegalStateException: Header was already
                 * written to response!
                 */
                _this.onShow();

            }).on('pagebeforehide', function(event, ui) {
                _this.onHide();
            });
        }

        /**
         *
         */
        function PagePrintSettings(_i18n, _view, _model) {

            var PRINT_OPT_PFX = 'print-opt-'
            //
            , PRINT_OPT_DIV_SFX = '-div'
            //
            , CUSTOM_HTML5_DATA_ATTR = 'data-savapage'
            //
            , _getPrinterOptionId = function(ippKeyword) {

                var sel, i = 0;

                $.each(_model.myPrinterOpt, function(key, value) {
                    if (key === ippKeyword) {
                        sel = PRINT_OPT_PFX + i;
                        return;
                    }
                    i += 1;
                });

                return sel;
            }
            /**
             * 
             */
            , _isMediaSourceAutoSelected = function () {
                return $("select[data-savapage='media-source']").val() === 'auto';
            }
            /*
             * @param target The target media-source select selector. 
             */ 
            , _onChangeMediaSource = function (target) {
                var isAuto, isManual, mediaOptId
                //
                , ippOption = target.attr(CUSTOM_HTML5_DATA_ATTR) 
                //
                , singleMediaSourceMedia, singleJobMedia
                //
                , isScaling, isSingleMediaMatch 
                ;

                if (ippOption === 'media-source') {

                    isAuto = target.val() === 'auto';
                    
                    if (isAuto) {
                        
                        _model.printSelectedMedia = null;                        
                                                
                    } else {                       
                        
                        isManual = target.val() === 'manual';
                    
                        // Show the 'media' option when media-source 'manual'.
                        mediaOptId = _getPrinterOptionId('media');
                        
                        _view.visible($('#' + mediaOptId + PRINT_OPT_DIV_SFX), isManual);
                        _view.visible($('.sp-print-job-media-info'), true);
                        
                        if (isManual) {
                            _model.printSelectedMedia = $('#' + mediaOptId).val();                             
                        } else {
                            _model.printSelectedMedia = _model.getMediaSourceMedia(target.val());                                   
                        }                        
                   }

                _model.isPrintManualFeed = isManual;        
                   
                   
                } else if (ippOption === 'media') {
                    _model.printSelectedMedia = target.val();                        
                }
                
                singleMediaSourceMedia = _model.getSingleMediaSourceMedia();
                singleJobMedia = _model.getSingleJobMedia();
                
                // Single jobs media and single media-source media?
                isSingleMediaMatch = singleMediaSourceMedia === singleJobMedia 
                    && (!_model.printSelectedMedia || _model.printSelectedMedia === singleJobMedia);
                              
                isAuto = _isMediaSourceAutoSelected();
                                          
                isScaling = !(isSingleMediaMatch || isAuto 
                    || (!isAuto && _model.printSelectedMedia === singleJobMedia));   

                _view.visible($('.sp-print-job-scaling'), isScaling);
                _view.visible($('.sp-print-job-media-info'), isScaling);
                
                _model.setJobsMatchMedia(_view);
                                                   
            }            
            //
            // Choices from model to view.
            //
            , _m2v = function() {
                var i = 0
                //
                ,selMediaSource = $("select[data-savapage='media-source']")
                //                
                ;
                $.each(_model.myPrinterOpt, function(key, value) {                    
                    $('#' + PRINT_OPT_PFX + i).val(value).selectmenu('refresh');
                    i += 1;
                });
                                                
                _onChangeMediaSource(selMediaSource);                
            }
            //
            // Choices from view to model.
            //                        
            , _v2m = function() {
                              
                var i = 0;
                
                $.each(_model.myPrinterOpt, function(key, value) {
                    _model.myPrinterOpt[key] = $('#' + PRINT_OPT_PFX + i).val();
                    i += 1;
                });                

               _model.printPageScaling =_view.getRadioValue('print-page-scaling-enum');                                
                             
            }
            //
            ;

            $('#page-printer-settings').on('pagecreate', function(event) {

                $('#button-print-settings-default').click(function() {
                    _model.setPrinterDefaults();
                    _m2v();
                    _model.setJobsMatchMediaSources(_view);
                    return false;
                });

                /*
                 * When page-scaling is changed.
                 */
                $('input[name=print-page-scaling-enum]:radio').change(function(event) {
                    _model.printPageScaling = _view.getRadioValue('print-page-scaling-enum');
                    _model.setJobsMatchMedia(_view);
                    _model.setJobsMatchMediaSources(_view);
                });

                /*
                 * When any printer option is changed.
                 */
                $('#printer-options').change(function(event) {
                    _onChangeMediaSource($(event.target));                    
                    _model.setJobsMatchMediaSources(_view);
                });

            }).on("pagebeforeshow", function(event, ui) {

                var i = 0, selExpr, html, selMediaSource
                //
                ;

                _model.myFirstPageShowPrintSettings = false;


                $('#title-printer-settings').html(_model.myPrinter.alias);
                
                _view.visible($('.sp-print-job-media-info'), false);

                // Set visibility of widgets based on job media status.
                _model.setJobsMatchMediaSources(_view);

                _model.hasPrinterManualMedia = false;

                html = '';

                i = 0;

                $.each(_model.myPrinter.groups, function(key, group) {
                    
                    var isMediaSourceMatch = _model.isMediaSourceMatch();
                    
                    $.each(group.options, function(key, option) {

                        var keyword = option.keyword
                        //
                        , selected = _model.myPrinterOpt[keyword]
                        //
                        , selectedSkipped = false
                        //
                        , firstChoice
                        //
                        ;

                        selExpr = PRINT_OPT_PFX + i;

                        html += '<div id="' + selExpr + PRINT_OPT_DIV_SFX + '">';
                        html += '<label for="' + selExpr + '">' + option.text + '</label>';
                        html += '<select ' + CUSTOM_HTML5_DATA_ATTR + '="' + keyword + '" id="' + selExpr + '" data-native-menu="false">';

                        $.each(option.choices, function(key, val) {
                            var skip = false;
                            
                            if (keyword === 'media-source' && val.choice === 'manual') {
                                _model.hasPrinterManualMedia = true;
                            }

                            skip = !isMediaSourceMatch && keyword === 'media-source' && val.choice === 'auto';

                            if (skip) {
                                
                                if (selected === val.choice) {
                                    selectedSkipped = true;
                                }
                                                                                                
                            } else {
                                
                                if (!firstChoice) {
                                    firstChoice = val.choice;
                                }                                
                                
                                html += '<option value="' + val.choice + '"';
                                if (selected === val.choice) {
                                    html += ' selected';
                                }
                                html += '>' + val.text + '</option>';
                            }
                        });

                        if(selectedSkipped) {
                            _model.myPrinterOpt[keyword] = firstChoice;
                            option.defchoiceOverride = firstChoice;
                        }                                    

                        html += '</select>';
                        html += '</div>';

                        i += 1;
                    });

                });

                $('#printer-options').html(html);

                $(this).enhanceWithin();

                selMediaSource = $("select[data-savapage='media-source']");
                _onChangeMediaSource(selMediaSource);
                                    
                i = 0;

                $.each(_model.myPrinterOpt, function(key, value) {
                    var sel = $('#' + PRINT_OPT_PFX + i + PRINT_OPT_DIV_SFX);
                    if (key === 'media-source') {                        
                        _view.visible(sel, true);
                    } else if (key === 'media') {
                        _view.visible(sel, selMediaSource.val() === 'manual');
                    }
                    i += 1;
                });
                                
            }).on('pagebeforehide', function(event, ui) {
                _v2m();
            });
        }

        /**
         *
         */
        function PagePrint(_i18n, _view, _model, _api) {

            var
            //_page = _page = new Page(_i18n, _view)
            //, _self = derive(_page)
            _this = this
            //
            , _quickPrinterCache = []
            //
            , _quickPrinterSelected
            //
            , _fastPrintAvailable
			//
            , _getPrinterImg = function(item, isDirect) {
                if (item.printer.readerSecured) {
                    if(isDirect) {
                        return 'device-card-reader-terminal-16x16.png';
                    } 
                    return 'device-card-reader-16x16.png';
                }
                if (item.printer.terminalSecured) {
                    return 'printer-terminal-custom-16x16.png';
                }
                return 'printer-terminal-any-16x16.png';
            }
            //
            , _getQuickPrinterHtml = function(item) {
                var html
                //
                , authMode = item.printer.authMode
                //
                , isDirect = (authMode === 'DIRECT' || authMode === 'FAST_DIRECT')
                //
                , isFast = (authMode === 'FAST' || authMode === 'FAST_DIRECT' || authMode === 'FAST_HOLD')
                //
                ;
                html = "<img width=\"16\" height=\"16\" src=\"/images/" + _getPrinterImg(item, isDirect) + "\"/>";
                html += "<span class=\"ui-mini sp-txt-wrap\">";
                html += item.text + " &bull; " + (item.printer.location || "&nbsp;");
                html += "<span/>";

                if (isFast) {
                    html += "<span class=\"ui-li-count\">Fast</span>";
                }
                return html;
            }
            //
            , _onQuickPrinterSearch = function(target, filter) {
                /* QuickSearchFilterDto */
                var res, html = "";

                if (!_quickPrinterSelected || (_quickPrinterSelected && filter !==  _quickPrinterSelected.text)) {
                    _view.visible($('#content-print .printer-selected'), false);
                } else {
                    _view.visible($('#button-print-settings'), true);
                    return;
                }

                if (_model.myPrinter) {
                    _this.onClearPrinter();
                }
                
                _model.myPrinterReaderName = undefined;

                _quickPrinterCache = [];
                _quickPrinterSelected = undefined;

                res = _api.call({
                    request : "printer-quick-search",
                    dto : JSON.stringify({
                        filter : filter,
                        maxResults : 5
                    })
                });

                if (res.result.code === '0') {

                    _quickPrinterCache = res.items;
                    _fastPrintAvailable = res.fastPrintAvailable;

                    if (_fastPrintAvailable && _model.myFirstPageShowPrint) {
                        _this.onFastProxyPrintRenew(false);
                    }

                    _view.visible($('#content-print .printer-fast-print-info'), res.fastPrintAvailable);

                    $.each(_quickPrinterCache, function(key, item) {

                        var authMode = item.printer.authMode
                        //
                        , enabled = (authMode !== 'FAST')
                        //
                        ;

                        html += "<li class=\"ui-mini ui-li-has-icon\" data-icon=\"false\" data-savapage=\"" + key + "\">";

                        if (enabled) {
                            html += "<a tabindex=\"2\" href=\"#\">";
                        }

                        html += _getQuickPrinterHtml(item);


                        if (enabled) {
                            html += "</a>";
                        }

                        html += "</li>";

                    });
                } else {
                    _view.showApiMsg(res);
                }

                target.html(html).filterable("refresh");
            }
            //
            , _onSelectPrinter = function(selection, filterable) {
                var attr = "data-savapage"
                //
                , sel = $("#sp-print-qs-printer")
                //
                , printer;

                _quickPrinterSelected = _quickPrinterCache[selection.attr(attr)];

                $('#button-print-settings').html(_getQuickPrinterHtml(_quickPrinterSelected));

                printer = _quickPrinterSelected.printer;

                sel.attr(attr, _quickPrinterSelected.key);
                sel.val(_quickPrinterSelected.text);

                filterable.empty();

                if (printer.readerSecured) {
                    _model.myPrinterReaderName = printer.readerName;
                }

                $(".sp-print-printer-selected").show();
                _view.visible($('#content-print .printer-selected'), true);
                _view.visible($('#content-print .printer-fast-print-info'), false);

                _this.onPrinter(printer.name);

                $("#print-title").focus();
            }
            //
            ;

            // this.onShow()
            // this.onHide()
            // this.onPrint()
            // this.onPrinter()
            // this.onSettings()

            this.clearInput = function() {
                $('#slider-print-copies').val(1).slider("refresh");
                $('#print-page-ranges').val('');
                if (!$('#delete-pages-after-print')[0].disabled) {                
                    _view.checkCb("#delete-pages-after-print", false);
                }
            };

            $('#page-print').on('pagecreate', function(event) {

                var filterablePrinter = $("#sp-print-qs-printer-filter");

                filterablePrinter.focus();

                //
                filterablePrinter.on("filterablebeforefilter", function(e, data) {
                    e.preventDefault();
                    _onQuickPrinterSearch($(this), data.input.get(0).value);
                });

                // Show available printers on first open
                _onQuickPrinterSearch(filterablePrinter, "");

                $(this).on('click', '#sp-print-qs-printer-filter li', null, function() {
                    _onSelectPrinter($(this), filterablePrinter);
                });

                $('#button-print').click(function(e) {
                    _this.onPrint(_view.isCbChecked($("#delete-pages-after-print")), false, _view.isCbChecked($("#print-remove-graphics")));
                    return false;
                });

                $('#button-print-and-close').click(function(e) {
                    _this.onPrint(_view.isCbChecked($("#delete-pages-after-print")), true, _view.isCbChecked($("#print-remove-graphics")));
                    return false;
                });

                $('#sp-button-print-auth-cancel').click(function(e) {
                    _this.onPrintAuthCancel();
                    return false;
                });

                $('#button-print-settings').click(function(e) {
                    _this.onSettings(_quickPrinterSelected.printer.name);
                    return false;
                });

                $('#button-printer-fast-print-renew').click(function(e) {
                    _this.onFastProxyPrintRenew(true);
                    return false;
                });

                $('#print-title-list').change(function(event) {
                    _model.myTitle = $('#print-title-list :selected').text();
                    $('#print-title').val(_model.myTitle);
                    $(this).val('').selectmenu('refresh');
                    return false;
                });

            }).on("pagebeforeshow", function(event, ui) {
                _this.onShow();
                if (_fastPrintAvailable) {
                    _this.onFastProxyPrintRenew(false);
                }
            }).on('pagebeforehide', function(event, ui) {
                _this.onHide();
            });
        }

        /**
         *
         */
        function Model(_i18n) {

            var
            //_super = new Base()
            //, _self = derive(_super)
            _this = this
            //
            , _LOC_AUTH_NAME = 'sp.auth.user.name'
            //
            , _LOC_AUTH_TOKEN = 'sp.auth.user.token'
            //
            , _LOC_LANG = 'sp.user.language'
            //
            ;

            this.MediaMatchEnum = {
                MATCH : 1,
                CLASH : 2
            };

            /*
             * Value: this.MediaMatchEnum.
             */
            this.jobsMatchMedia = null;
            this.jobsMatchMediaSources = null;

            /*
             * PageScalingEnum
             */
            this.printPageScaling = 'CROP';

            /**
             * 
             */
            this.printSelectedMedia = null;
            this.hasPrinterManualMedia = false;
            this.isPrintManualFeed = false; 
                        
            /*
             * 
             */
            this.uniqueImgUrlValue = '';
            this.uniqueImgUrlValue4Browser = '';

            this.authToken = {};

            this.propPdfDefault = null;

            this.MY_THUMBNAIL_WIDTH = 70;

            this.myIsDragging = false;

            this.myShowUserStats = true;
            this.myShowUserStatsGet = false;
            this.myFirstPageShowPrint = true;
            this.myFirstPageShowPrintSettings = true;
            this.myFirstPageShowLetterhead = true;

            this.myTitle = null;
            this.iPopUpJob = -1;
            this.myJobs = [];
            this.myJobPages = [];
            this.myTotPages = 0;
            this.myPrinter = null;

            /*
             * map: key(ipp keyword), value
             */
            this.myPrinterOpt = {};

            /*
             *
             */
            this.prevMsgTime = null;

            /*
             * The chosen letterhead
             */
            this.myLetterheadIdx = 'none';

            /*
             * Array[]
             */
            this.letterheads = null;
            /*
             * The default letterhead
             */
            this.letterheadDefault = null;

            /*
             * The pages(chunks) shown as images in thumbnail reel and page
             * browser
             */
            this.myCutPages = {};

            /*
             * this.user = { alias : null, id : null, admin : null, role : null,
             * mail : null, mailDefault : null, loggedIn : false };
             */
            this.user = new _ns.User();

            this.propPdf = this.propPdfDefault;

            /**
             * Creates job media map: ippMedia -> count, mediaUi
             */
            this.createMediaJobMap = function() {
                
                var mapMediaJobs = [];
                                 
                $.each(_this.myJobs, function(key, value) {

                    var media = value.media, entry = mapMediaJobs[media];

                    if (!entry) {
                        entry = {
                            count : 0,
                            mediaUi : value.mediaUi
                        };
                        mapMediaJobs[media] = entry;
                    }
                    entry.count++;
                });
                return mapMediaJobs;                
            };

            /**
             * Gets the media of a media-source
             */
            this.getMediaSourceMedia = function(source) {
                var media;
                                
                if (_this.myPrinter) {
                    // org.savapage.core.dto.IppMediaSourceMappingDto
                    $.each(_this.myPrinter.mediaSources, function(key, value) {
                        if (source === value.source) {
                            media = value.media;
                            return;
                        }
                    });
                }
                return media;                
            };

            /**
             * Gets the single media of media-sources, or null if media sources 
             * have different media.
             */
            this.getSingleMediaSourceMedia = function() {
                
                var singleMedia;
                                
                if (_this.myPrinter) {
                    // org.savapage.core.dto.IppMediaSourceMappingDto
                    $.each(_this.myPrinter.mediaSources, function(key, value) {                       
                        if (value.media) {
                            if (singleMedia === undefined) {
                                singleMedia = value.media;
                            }
                            if (singleMedia !== value.media) {
                                singleMedia = null;                                
                            }
                        }
                    });
                }
                return singleMedia;                
            };

            /**
             * Gets the single media of jobs, or null if jobs 
             * have different media.
             */
            this.getSingleJobMedia = function() {
                
                var singleMedia;
                                
                $.each(_this.myJobs, function(key, value) {
                    if (value.media) {
                        if (singleMedia === undefined) {
                            singleMedia = value.media;
                        }
                        if (singleMedia !== value.media) {
                            singleMedia = null;                                
                        }
                    }
                });
                return singleMedia;                
            };

            /**
             * Creates media-source map: ippMedia -> count, source
             */
            this.createPrinterMediaSourcesMap = function() {
                
                var mapMediaSources = [];

                if (_this.myPrinter) {

                    // org.savapage.core.dto.IppMediaSourceMappingDto
                    $.each(_this.myPrinter.mediaSources, function(key, value) {

                        var media = value.media, entry = mapMediaSources[media];

                        if (!entry) {
                            entry = {
                                count : 0,
                                source : value.source
                            };
                            mapMediaSources[media] = entry;
                        }
                        entry.count++;
                    });
                }
                return mapMediaSources;                
            };

            /**
             * Displays info about job media and selected 'media' ands sets 
             * this.jobsMatchMedia.
             */
            this.setJobsMatchMedia = function(_view) {

                var html = '', mediaWlk
                //
                , mapMediaJobs = this.createMediaJobMap()
                //
                , media = this.printSelectedMedia
                //
                , selJobMediaInfo = $('.sp-print-job-media-info')
                //
                ;
                
                // Do we have a job/media match/clash?
                this.jobsMatchMedia = this.MediaMatchEnum.MATCH;

                // Create the html.
                for (mediaWlk in mapMediaJobs) {

                    if (html.length) {
                        html += ' ';
                    }

                    html += '<span class="';

                    if (media && mediaWlk !== media) {                        
                        this.jobsMatchMedia = this.MediaMatchEnum.CLASH;
                        if (this.printPageScaling === 'CROP') {
                            html += 'sp-ipp-media-info-crop';
                        } else if (this.printPageScaling === 'EXPAND'){
                            html += 'sp-ipp-media-info-expand';
                        } else {
                            html += 'sp-ipp-media-info-shrink';
                        }
                    } else {
                        html += 'sp-ipp-media-info-match';
                    }
                    html += '">' + mapMediaJobs[mediaWlk].mediaUi + '</span>';
                }

                if (html.length === 0 || !this.myPrinter) {
                    _view.visible(selJobMediaInfo, false);
                } else {
                    selJobMediaInfo.empty().append(html);
                }
            };

            /**
             * Displays info about job media and available 'media-source' for 
             * the selected printer ands sets this.jobsMatchMediaSources.
             */
            this.setJobsMatchMediaSources = function(_view) {

                var html = '', selHtml
                //
                , mapMediaJobs = this.createMediaJobMap()
                //
                , mapMediaSources = this.createPrinterMediaSourcesMap() 
                //
                , mediaWlk
                //
                , IS_UNIQUE_MEDIASOURCE_REQUIRED = false
                //
                ;

                if (this.isPrintManualFeed) {
                    html += '<span class="sp-ipp-media-info-match">M</span>';
                }                
                
                // Do we have a job/source media match/clash?
                this.jobsMatchMediaSources = this.MediaMatchEnum.MATCH;

                // Create the html.
                for (mediaWlk in mapMediaJobs) {

                    if (html.length) {
                        html += ' ';
                    }

                    html += '<span class="';

                    if (!mapMediaSources[mediaWlk] || (IS_UNIQUE_MEDIASOURCE_REQUIRED && mapMediaSources[mediaWlk].count > 1)) {
                        
                        this.jobsMatchMediaSources = this.MediaMatchEnum.CLASH;
                        
                        if (this.printPageScaling === 'CROP') {
                            html += 'sp-ipp-media-info-crop';
                        } else if (this.printPageScaling === 'EXPAND'){
                            html += 'sp-ipp-media-info-expand';
                        } else {
                            html += 'sp-ipp-media-info-shrink';
                        }
                        
                    } else {
                        html += 'sp-ipp-media-info-match';
                    }
                    html += '">' + mapMediaJobs[mediaWlk].mediaUi + '</span>';
                }

                selHtml = $('.sp-print-job-media-sources-info');
                selHtml.empty().append(html);
                
                if (html.length === 0) {
                    selHtml.hide();
                } else {
                    selHtml.show();
                }                
            };

            /**
             * Does the media of the jobs match the AVAILABLE media of the 
             * selected printer.
             */
            this.isMediaSourceMatch = function() {
                return this.jobsMatchMediaSources === this.MediaMatchEnum.MATCH;
            };
            
            /**
             * Does the media of the jobs match the user SELECTED media of the
             * selected printer.
             */
            this.isMediaJobMatch = function() {
                return this.jobsMatchMedia === this.MediaMatchEnum.MATCH;
            };            

            this.refreshUniqueImgUrlValue = function() {
                // number of milliseconds since 1970/01/01
                var d = new Date();
                this.uniqueImgUrlValue = d.getTime().toString();
                this.uniqueImgUrlValue4Browser = this.uniqueImgUrlValue;
            };

            this.refreshUniqueImgUrlValue4Browser = function() {
                var d = new Date();
                this.uniqueImgUrlValue4Browser = d.getTime().toString();
            };

            this.isPdfEncrypt = function() {
                return (this.propPdf && this.propPdf.encryption.length > 0);
            };

            this.isLetterhead = function() {
                return (this.letterheadDefault !== null);
            };

            this.initAuth = function() {
                var item;

                this.authToken = {};

                item = _LOC_AUTH_NAME;
                if (window.localStorage[item] !== null) {
                    this.authToken.user = window.localStorage[item];
                }

                item = _LOC_AUTH_TOKEN;
                if (window.localStorage[item] !== null) {
                    this.authToken.token = window.localStorage[item];
                }

                item = _LOC_LANG;
                if (window.localStorage[item] !== null) {
                    this.authToken.language = window.localStorage[item];
                }

            };

            this.setLanguage = function(lang) {
                this.authToken.language = lang;
                window.localStorage[_LOC_LANG] = lang;
            };

            this.setAuthToken = function(user, token, language) {
                var item;

                item = _LOC_AUTH_NAME;
                this.authToken.user = user;
                window.localStorage[item] = user;

                item = _LOC_AUTH_TOKEN;
                this.authToken.token = token;
                window.localStorage[item] = token;

                if (language) {
                    this.setLanguage(language);
                }
            };

            /**
             *
             */
            this.setJobProps = function(jobs, pages, url_template) {
                this.myJobsDrm = false;
                this.myJobs = jobs;
                this.myJobPages = pages;
                this.myJobPageUrlTemplate = url_template;

                _this.myTotPages = 0;
                $.each(_this.myJobPages, function(key, value) {
                    _this.myTotPages += value.pages;
                    if (value.drm) {
                        _this.myJobsDrm = true;
                    }
                });
            };

            /**
             */
            this.startSession = function() {

                this.myShowUserStats = true;
                this.myShowUserStatsGet = false;

                this.myFirstPageShowPrint = true;
                this.myFirstPageShowPrintSettings = true;
                this.myFirstPageShowLetterhead = true;
                this.myPrinterOpt = {};
                this.myJobsDrm = false;
                this.myJobs = [];
                this.myJobPages = [];
                this.myCutPages = {};
                this.myPrinter = null;
                this.propPdf = this.propPdfDefault;
                this.refreshUniqueImgUrlValue();
                this.prevMsgTime = null;
            };

            /**
             */
            this.getPageCount = function() {
                var tot = 0;
                if (this.myJobPages) {
                    $.each(this.myJobPages, function(key, val) {
                        tot += val.pages;
                    });
                }
                return tot;
            };

            /**
             * Gets the accumulated one-based page number from the index of the
             * visible job page.
             */
            this.getPageNumber = function(iPage) {
                var i = 0, nPage = 1;
                for ( i = 0; i < iPage; i++) {
                    nPage += this.myJobPages[i].pages;
                }
                return nPage;
            };

            /**
             * Gets the rotate of the zero-based accumulated page number.
             *
             * @return null when no rotation
             */
            this.getPageRotate = function(iPage) {

                var i = 0, nPageTot = 0, rotate, jobPage;

                for ( i = 0; i < this.myJobPages.length; i++) {
                    jobPage = this.myJobPages[i];
                    nPageTot += jobPage.pages;
                    if (iPage < nPageTot) {
                        rotate = _this.myJobs[jobPage.job].rotate;
                        if (rotate === "0") {
                            return null;
                        }
                        return rotate;
                    }
                }
                return null;
            };

            /**
             */
            this.setPrinterDefaults = function() {

                _this.myPrinterOpt = {};

                /*
                 * Flatten the options to one (1) array
                 */
                $.each(_this.myPrinter.groups, function(key, group) {

                    $.each(group.options, function(k2, option) {
                        _this.myPrinterOpt[option.keyword] = option.defchoiceOverride || option.defchoice;
                    });

                    //if (group.subgroups) {
                    //    $.each(group.subgroups, function(k3, subgroup) {
                    //        // TODO
                    //    });
                    // }

                });
            };
            /**
             */
            this.addJobPages = function(pages) {
                var i;
                for ( i = 0; i < pages.length; i++) {
                    this.myJobPages.push(pages[i]);
                }
            };

            /**
             * Return the selected letterhead object.
             *
             * Get the zero-based index of the selected letterhead in an option
             * list, compensate for the header and 'none' options (-2), and use
             * the
             * index to read into the _model.letterheads[]
             *
             * @param sel
             *            The jQuery selector of the select option list.
             */
            this.getSelLetterheadObj = function(sel) {
                var idx = $(sel + ' option').index($(sel + ' :selected')) - 2;
                return this.letterheads[idx];
            };

        }

        /**
         *
         */
        function Controller(_i18n, _model, _view, _api, _cometd, _userEvent, _deviceEvent, _proxyprintEvent) {

            var
            //_super = new Base()
            //
            //, _self = derive(_super)
            //
            _util = _ns.Utils
            //
            , _this = this
            //
            , i18nRefresh, _getLetterheads, _tbIndUser, _initUser
            //
            , _adaptLetterheadPage
            //
            , _refreshLetterheadList, _saveSelectedletterhead, _savePdfProps
            //
            , _handleSafePageEvent
            /*
             * The current icon (name of the icon from the standard JQuery Mobile
             * icon set). This should match the initial value in html.
             */, _iconCur = 'info'
            //
            , _changeIcon
            //
            , _prepareReaderForPrinter
            //
            , _cometdMaxNetworkDelay
            //
            , _timeoutAuthPrint, _countdownAuthPrint, _clearTimeoutAuthPrint
            /**
             * 
             */            
            , _saveRemoveGraphics = function(sel) {
                _model.removeGraphics = _view.isCbChecked($(sel));
                _view.visible($('#button-mini-no-graphics'), _model.removeGraphics);
            }
            //
            , _refreshPrinterInd = function() {
                
                var trgColor = $('.sp-button-mini-print-color')
                //
                , trgMono = $('.sp-button-mini-print-mono')
                //
                , trgDuplex = $('.sp-button-mini-print-duplex')
                //
                , trgNup = $('.sp-button-mini-print-n-up')
                //
                , ippAttrVal
                //
                , isColor
                //
                ;

                _model.myTitle = $('#print-title').val();
                _saveSelectedletterhead('#print-letterhead-list');
                _saveRemoveGraphics('#print-remove-graphics');

                // Check IPP attributes value
                ippAttrVal = _model.myPrinterOpt['print-color-mode'];
                isColor = ippAttrVal && ippAttrVal === 'color';
                _view.visible(trgColor, isColor);
                _view.visible(trgMono, _model.myPrinter && !isColor);

                // Check IPP attributes value
                ippAttrVal = _model.myPrinterOpt['number-up'];
                _view.visible(trgNup, ippAttrVal && ippAttrVal !== '1');

                // Check IPP attributes value
                ippAttrVal = _model.myPrinterOpt.sides;
                _view.visible(trgDuplex, ippAttrVal && ippAttrVal !== 'one-sided');
            }
            //
            ;

            /*
             *
             */
            i18nRefresh = function(i18nNew) {
                if (i18nNew && i18nNew.i18n) {
                    _i18n.refresh(i18nNew.i18n);
                    $.mobile.pageLoadErrorMessage = _i18n.string('msg-page-load-error');
                } else {
                    _view.message('i18n initialization failed.');
                }
            };

            _tbIndUser = function() {
                // suppressed for now
                //$('#mini-user-name').html(_model.user.id);
                $.noop();
            };

            _initUser = function(loginRes) {

                var res;

                _model.startSession();

                _model.prevMsgTime = loginRes.systime;

                _model.user.key_id = loginRes.key_id;
                _model.user.id = loginRes.id;
                _model.user.fullname = loginRes.fullname;

                _model.language = loginRes.language;
                _model.setAuthToken(loginRes.id, loginRes.authtoken, _model.language);

                /*
                 * This is the token used for CometD authentication. See: Java
                 * org.savapage.server.cometd.BayeuxAuthenticator
                 */
                _model.user.cometdToken = loginRes.cometdToken;

                _model.user.stats = loginRes.stats;
                
                _model.user.admin = loginRes.admin;
                _model.user.internal = loginRes.internal;
                _model.user.role = loginRes.role;
                _model.user.mail = loginRes.mail;
                _model.user.mailDefault = loginRes.mail;
                _model.letterheads = null;
                _model.propPdfDefault.desc.author = _model.user.fullname;

                /*
                 * _api.call({ request : 'exit-event-monitor' });
                 * THIS IS NOT NEEDED, because server-side login took care of
                 * this.
                 */
                /*
                 *
                 */
                res = _api.call({
                    'request' : 'pdf-get-properties'
                });

                _view.showApiMsg(res);

                if (res.result.code === "0") {
                    _model.propPdf = res.props;
                }

                _getLetterheads();

            };

            /**
             * Prepares for Login User input.
             */
            this.initLoginUserInput = function() {
                if (_model.authCardIp) {
                    _cometd.start(_model.cometdDeviceToken);
                }
            };

            this.onWakeUp = function(deltaMsec) {

                var buttonGoOn = $('#sp-popup-wakeup-refresh');

                /*
                * When paused there is NO pending long-poll: we fall back to the
                * normal "resume" behavior.
                */

                // Not for now (because of #320)
                //if (_userEvent.isPaused()) {
                //    return;
                //}

                /*
                 * At this point we want user interaction: so, stop the timer!
                 */
                _ns.stopAppWatchdog();

                /*
                 * By pausing we tell the server to interrupt the long-poll: as
                 * a result of this call connection to server might be lost or
                 * session expired).
                 */
                _ns.userEvent.pause();

                /*
                 * #320
                 */
                _userEvent.removeListener();
                _proxyprintEvent.removeListener();

                _ns.cometd.stop();

                /*
                 * Are we still logged in (may be connection to server is lost or
                 * session is expired)?
                 */
                if (!_model.user.loggedIn) {
                    return false;
                }

                /*
                 * The popup we need is part of #page-main, so first navigate to
                 * that page (if not already there).
                 */
                if (_view.activePage().attr('id') !== 'page-main') {
                    _view.changePage($('#page-main'));
                }

                /*
                 * We use a popup since we NEED user action to continue.
                 * Reason: some devices give some CPU cycles every 10-20 seconds
                 * to the Web App when it is in the background...
                 */
                $('#sp-popup-wakeup').popup('open', {
                    positionTo : 'window'
                });

                buttonGoOn.focus();

                buttonGoOn.click(function(e) {

                    _ns.startAppWatchdog();

                    $('#sp-popup-wakeup').popup('close');

                    _userEvent.setLongPollLost();

                    // Mantis #320
                    _ns.cometd.start(_ns.model.user.cometdToken);
                    _userEvent.resume();

                    return false;
                });
            };

            /*
             *
             */
            this.init = function() {
                var res, language
                //
                , authModeRequest = _util.getUrlParam('login');

                _model.initAuth();

                res = _api.call({
                    request : 'constants',
                    authMode : authModeRequest
                });

                /*
                 * FIX for Opera to prevent endless reloads when server is down:
                 * check the return code. Display a basic message (English is all
                 * we
                 * have), no fancy stuff (cause jQuery might not be working).
                 */
                if (!res || res.result.code !== '0') {
                    _view.message('connection to server is lost');
                    return;
                }
                _ns.configAppWatchdog(_this.onWakeUp, res.watchdogHeartbeatSecs, res.watchdogTimeoutSecs);

                _model.prevMsgTime = res.systime;

                _view.pages.login.setAuthMode(res.authName, res.authId, res.authCardLocal, res.authCardIp, res.authModeDefault, res.authCardPinReq, res.authCardSelfAssoc, res.cardLocalMaxMsecs, res.cardAssocMaxSecs);

                // Configures CometD without starting it.
                _cometdMaxNetworkDelay = res.cometdMaxNetworkDelay;
                _cometd.configure(_cometdMaxNetworkDelay);

                /*
                 *
                 */
                _i18n.initValues(res.i18n_values);

                _model.authCardIp = res.authCardIp;
                _model.cometdDeviceToken = res.cometdToken;
                _model.maxIdleSeconds = res.maxIdleSeconds;

                _model.MY_THUMBNAIL_WIDTH = res['thumbnail-width'];
                _model.propPdfDefault = res['pdf-prop-default'];

                _view.imgBase64 = res.img_base64;

                language = _util.getUrlParam('language');
                if (!language) {
                    language = _model.authToken.language || '';
                }

                res = _api.call({
                    request : 'language',
                    language : language
                });

                i18nRefresh(res);

                _view.initI18n(res.language);

                // http://what-ho.posterous.com/preventing-image-dragging
                //
                // Modern web-browsers trying to be helpful provide default
                // actions
                // when you select things on a page.
                //
                // For Jquery-based websites, the following one-liner will
                // prevent
                // image dragging from happening
                $('img').mousedown(function(e) {
                    e.preventDefault();
                });

                $('body').on('vmouseup', null, null, function(event) {
                    _model.myIsDragging = false;
                });

                /*
                 * One-time binding the click to a function. We don't want
                 * to bind each time the panel is loaded.
                 *
                 * Even if #id doesn't exist yet (because the panel is not
                 * loaded) this code is executed.
                 */
                $('body').on('click', '#sp-button-continue-after-expire', null, function() {
                    _ns.restartWebApp();
                    return false;
                });

            };

            /*
             * authMode === _view.AUTH_MODE*
             */
            this.login = function(authMode, authId, authPw, authToken, assocCardNumber) {

                _model.user.loggedIn = false;

                _api.callAsync({
                    request : 'login',
                    authMode : authMode,
                    authId : authId,
                    authPw : authPw,
                    authToken : authToken,
                    assocCardNumber : assocCardNumber,
                    role : 'user'
                }, function(data) {

                    _model.user.loggedIn = (data.result.code === '0');

                    if (_model.user.loggedIn) {

                        if (_model.authCardIp) {
                            _cometd.stop();
                        }

                        _initUser(data);

                        _view.loadUserPage('#page-main', 'Main');
                        _view.loadUserPage('#page-browser', 'Browser');
                        _view.changePage($('#page-main'));

                        _view.pages.main.adjustThumbnailVisibility();
                        _model.myShowUserStats = true;

                        _tbIndUser();

                        if (_model.maxIdleSeconds) {
                            _ns.monitorUserIdle(_model.maxIdleSeconds, _view.pages.main.onLogout);
                        }

                        _ns.startAppWatchdog();

                    } else {

                        if (_view.activePage().attr('id') === 'page-login') {
                            /*
                             * This was a login error resulting from user input
                             * in the Login dialog.
                             */
                            if (data.authCardSelfAssoc) {
                                /*
                                 * Card not found, but self association is
                                 * allowed.
                                 */
                                _view.pages.login.notifyCardAssoc(authId);

                            } else {
                                _view.pages.login.notifyLoginFailed(( assocCardNumber ? null : authMode), data.result.txt);

                                if (_model.authCardIp) {
                                    _deviceEvent.resume();
                                }
                            }
                        } else {
                            /*
                             * This was a login error with authToken from local
                             * storage.
                             */
                            _view.pages.login.loadShowAsync(function() {
                                _this.initLoginUserInput();
                            });
                        }
                    }

                }, function() {
                    $.noop();
                }, function(data) {
                    _view.showApiMsg(data);
                });

            };

            /**
             *
             */
            _adaptLetterheadPage = function() {
                var res, pub, html = '', lh;

                if ($('#letterhead-list').val() === 'none') {
                    $('.letterhead-actions').hide();
                } else {
                    $('#letterhead-name').val($('#letterhead-list :selected').text());
                    $('.letterhead-actions').show();

                    lh = _view.pages.letterhead.getSelected();

                    if (!lh) {
                        return;
                    }

                    pub = lh.pub;

                    res = _api.call({
                        request : 'letterhead-get',
                        id : $('#letterhead-list').val(),
                        pub : ( pub ? '1' : '0'),
                        base64 : (_view.imgBase64 ? '1' : '0')
                    });

                    // _view.showApiMsg(res);
                    if (res.result.txt) {
                        _view.message(res.result.txt);
                    }

                    $("#letterhead-thumbnails").empty();

                    if (res.result.code === '0') {

                        $.each(res.pages, function(key, value) {

                            var imgWidth = _model.MY_THUMBNAIL_WIDTH
                            //
                            , imgHeightA4 = imgWidth * 1.4
                            //
                            ;
                            // Without detail ...
                            // html += '<img src="' + value.url + '"/>';

                            /*
                             * We get the letterhead thumbnail in full detail,
                             * so the detail pop-up will position as expected
                             * (as the height and width are known).
                             */
                            html += '<img width="' + imgWidth + '" height="' + imgHeightA4 + '" alt="" src="' + _view.getImgSrc(value.url) + '" style="padding: 3px; margin: 3px; border: 1px solid silver"/>';
                        });

                        $("#letterhead-thumbnails").append(html);

                        _view.checkRadio('sp-letterhead-pos', 'sp-letterhead-pos-' + ((res.foreground) ? 'f' : 'b'));
                        _view.checkCb('#sp-letterhead-public', res.pub);
                        _view.visible($('.letterhead-edit-actions'), !res.pub || _model.user.admin);

                    } else {
                        $("#letterhead-thumbnails").append(res.result.txt);
                        $('.letterhead-edit-actions').hide();
                    }
                }
            };

            /**
             * Lazy retrieve of letterhead list.
             */
            _getLetterheads = function() {
                var res;
                
                if (_model.letterheads !== null) {
                    return;
                }

                _model.letterheads = [];
                _model.letterheadDefault = null;
                _model.myLetterheadIdx = 'none';

                res = _api.call({
                    'request' : 'letterhead-list'
                });

                _view.showApiMsg(res);

                if (res.result.code === '0') {
                    _model.letterheads = res.letterheads;
                    _model.letterheadDefault = res['default'];
                    if (_model.letterheadDefault) {
                        _model.myLetterheadIdx = _model.letterheadDefault;
                    }
                }
            };

            /**
             *
             */
            _prepareReaderForPrinter = function() {
                if (_model.myPrinterReaderName) {
                    // addListener() is idempotent.
                    _proxyprintEvent.addListener();
                }
            };

            /**
             * Sets (refreshed) a job titles optionmenu from the model.
             *
             * @param sel
             *            The selector, e.g. '#print-title-list'
             */
            this.setJobTitleMenu = function(sel) {
                var options = '<option value="">Select a title</option>';
                $.each(_model.myJobs, function(key, value) {
                    options += '<option value="' + key + '">' + value.title + '</option>';
                });
                $(sel).empty().append(options);
                $(sel).val('').selectmenu('refresh');
            };

            /**
             * Sets (refreshed) a letterhead optionmenu from the model.
             *
             * @param sel
             *            The selector, e.g. '#letterhead-list'
             */
            this.setLetterheadMenu = function(sel) {

                _getLetterheads();

                var options = '<option value="">Select a Letterhead</option><option value="none"';

                if (!_model.letterheadDefault) {
                    options += ' selected';
                }
                options += '>- none -</option>';

                $.each(_model.letterheads, function(key, value) {
                    options += '<option value="' + value.id + '"';
                    if (_model.myLetterheadIdx === value.id) {
                        options += ' selected';
                    }
                    options += '>' + value.name + '</option>';
                });

                $(sel).empty().append(options).selectmenu('refresh');
            };

            /**
             *
             */
            _refreshLetterheadList = function() {
                _model.letterheads = null;
                $('.letterhead-actions').hide();
                _this.setLetterheadMenu('#letterhead-list');
                _adaptLetterheadPage();
            };

            /**
             *
             */
            _handleSafePageEvent = function(res) {

                _model.user.stats = res.stats;
                
                _model.setJobProps(res.jobs, res.pages, res.url_template);

                /*
                 * DRY RUN: iterate the incoming pages and check if <img> is
                 * already there: if not the thumbnail image needs to be loaded.
                 */
                _view.pages.main.setThumbnails2Load();

                /*
                 * Perform next steps when this event is done.
                 */
                window.setTimeout(function() {

                    /*
                     * Mantis #320: if there are thumbnails to be loaded, we
                     * stop CometD, so iOS Safari will load the images :-)
                     */

                    if (_ns.thumbnails2Load > 0) {
                        _ns.userEvent.pause();
                        _ns.cometd.stop();
                    } else {
                        _ns.userEvent.onPollInvitation();
                    }

                    _view.pages.main.setThumbnails();

                    _view.pages.pagebrowser.setImages();

                    if (_view.isPageActive('page-main')) {
                        _view.pages.main.showUserStats();
                    } else {
                        _model.myShowUserStats = true;
                    }

                }, 10);

            };

            ////////////////////////////////////////////////////////
            // C A L L - B A C K S
            ////////////////////////////////////////////////////////

            // ----------------------------------------------------
            // Common Panel parameters.
            // ----------------------------------------------------

            _ns.PanelCommon.view = _view;
            _ns.PanelCommon.userId = _model.user.id;

            _ns.PanelCommon.onDisconnected = function() {
                _model.user.loggedIn = false;
                _view.changePage($('#page-login'));
                _view.showApiMsg(_api.simulateDisconnectError());
            };

            _ns.PanelCommon.refreshPanelCommon = function(wClass, skipBeforeLoad, thePanel) {

                var jqId = thePanel.jqId
                //
                , data = thePanel.getInput(thePanel)
                //
                , jsonData = JSON.stringify(data)
                //
                ;

                $.mobile.loading("show");
                $.ajax({
                    type : "POST",
                    async : true,
                    url : '/pages/' + wClass,
                    data : {
                        user : _ns.PanelCommon.userId,
                        data : jsonData
                    }
                }).done(function(html) {
                    $(jqId).html(html).enhanceWithin();

                    // Hide the top divider with the title
                    // $(jqId + ' .ui-li-divider').hide();
                    $(jqId + ' > ul > .ui-li-divider').hide();

                    /*
                    * We can't retrieve the json-rsp here, why?
                    */
                    //thePanel.onOutput(thePanel, $.parseJSON($(jqId + '
                    // .json-rsp').text()));
                    thePanel.onOutput(thePanel, undefined);
                    thePanel.afterload(thePanel);
                }).fail(function() {
                    _ns.PanelCommon.onDisconnected();
                }).always(function() {
                    $.mobile.loading("hide");
                });

            };

            // --------------------------------
            // Call-back: api
            // --------------------------------
            _api.onExpired(function() {
                _model.user.loggedIn = false;
                _view.showExpiredDialog();
            });

            // Call-back: api
            _api.onDisconnected(function() {
                _model.user.loggedIn = false;
                _view.changePage($('#page-login'));
            });
            // -----------------------------
            // Call-back: polling
            // -----------------------------
            _changeIcon = function(icon) {
                if (icon !== _iconCur) {
                    $("#button-cometd-status").addClass("ui-icon-" + icon).removeClass("ui-icon-" + _iconCur);
                    _iconCur = icon;
                }
            };

            //--------------------------------------------------
            _userEvent.onWaitingForEvent = function() {
                _changeIcon("check");
            };

            _cometd.onConnecting = function() {
                _changeIcon("recycle");
            };

            _cometd.onHandshakeSuccess = function() {
                if (_model.user.loggedIn) {
                    _userEvent.addListener();
                } else if (_model.authCardIp) {
                    _deviceEvent.addListener();
                }
            };
            _cometd.onHandshakeFailure = function() {
                _changeIcon("alert");
            };
            _cometd.onReconnect = function() {
                //_changeIcon(_userEvent.getLongPollPending() ? "check" :
                // "plus");
                if (_userEvent.isLongPollPending()) {
                    _changeIcon("check");
                } else {
                    _changeIcon("plus");
                    _userEvent.onPollInvitation();
                }
            };

            _cometd.onConnectionBroken = function() {
                _changeIcon("delete");
                if (_model.user.loggedIn) {
                    _this.onWakeUp();
                }
            };
            _cometd.onConnectionClosed = function() {
                /*
                 * IMPORTANT: This is end-state in Google Chrome.
                 */
                _changeIcon("minus");
            };

            _cometd.onDisconnecting = function() {
                _changeIcon("clock");
            };

            /*
             * As reported by listener on /meta/unsuccessful channel.
             */
            _cometd.onUnsuccessful = function(message) {
                /*
                * #327: We handle this message as redundant: no action needed.
                */
                //_changeIcon("forbidden");
                //alert(JSON.stringify(message));
                //_this.onWakeUp();
                $.noop();
            };

            //--------------------------------------------------
            _deviceEvent.onCardSwipe = function(cardNumber) {
                _deviceEvent.pause();
                _view.pages.login.notifyCardIp(cardNumber);
            };
            _deviceEvent.onEventError = function(msg) {
                _view.message(msg);
                _cometd.stop();
            };
            _deviceEvent.onPollInvitation = function() {
                _deviceEvent.poll(_model.language);
            };

            //_deviceEvent.onEventIgnored = function() {
            //};
            //_deviceEvent.onWaitingForEvent = function() {
            //};

            _deviceEvent.onException = function(msg) {
                _view.message(msg);
            };

            _clearTimeoutAuthPrint = function() {
                if (_timeoutAuthPrint) {
                    window.clearTimeout(_timeoutAuthPrint);
                    _timeoutAuthPrint = null;
                }
            };

            //--------------------------------------------------
            _proxyprintEvent.onPrinted = function(res) {

                _clearTimeoutAuthPrint();

                $('#sp-popup-print-auth').popup('close');

                if (_model.closePrintDlg) {
                    //Do NOT use $('#button-printer-back').click();
                    _view.changePage($('#page-main'));
                }

                _view.message(res.result.txt);

                _model.prevMsgTime = res.data.msgTime;
            };

            _proxyprintEvent.onError = function(res) {

                _clearTimeoutAuthPrint();

                /*
                 * Do NOT use _view.showApiMsg(res), since this
                 * is also a popup
                 */
                _view.visible($('#auth-popup-content-wait'), false);
                _view.visible($('#auth-popup-content-msg'), true);

                $('#auth-popup-user-msg-title').text(_view.apiResMsgTitle(res));
                $('#auth-popup-user-msg-title').attr('class', _view.apiResMsgCssClass(res));
                $('#auth-popup-user-msg').attr('class', _view.apiResMsgCssClass(res));
                $('#auth-popup-user-msg').text(res.result.txt);
            };

            _proxyprintEvent.onException = function(msg) {
                _view.message(msg);
            };

            //--------------------------------------------------
            //_userEvent.onEventIgnored = function() {
            // $('#cometd-status-poll').empty().append('event (ignored)');
            //};

            _userEvent.onEventError = function(msg) {
                _view.message(msg);
                _cometd.stop();
            };

            _userEvent.onException = function(msg) {
                //_view.message(msg);
                _view.pages.main.onRefreshApp();
            };

            _userEvent.onJobEvent = function(res) {
                _model.refreshUniqueImgUrlValue4Browser();
                _handleSafePageEvent(res);
            };

            _userEvent.onAccountEvent = function(stats) {
                _model.user.stats = stats;
                _view.pages.main.showUserStats();
            };

            _userEvent.onNullEvent = function(data) {
                _model.prevMsgTime = data.msgTime;
            };

            _userEvent.onMsgEvent = function(data) {
                var msg = '', i = 0;

                _model.prevMsgTime = data.msgTime;

                $.each(data.messages, function(key, value) {
                    var err = value.level > 0;
                    msg += (i > 0 ? '<br>' : '');
                    msg += ( err ? '<span class="sp-txt-warn">' : '');
                    msg += value.text;
                    msg += ( err ? '</span>' : '');
                    i = i + 1;
                });
                _view.message(msg);
            };

            _userEvent.onPollInvitation = function() {
                _userEvent.poll(_model.user.id, _model.getPageCount(), 
                    _model.uniqueImgUrlValue, _model.prevMsgTime, 
                    _model.language, _view.imgBase64);
            };

            /*
             *
             */
            _view.onDisconnected = function() {
                _model.user.loggedIn = false;
                _view.pages.login.loadShowAsync();
            };

            /**
             * Callbacks: page language
             */
            _view.pages.language.onSelectLanguage(function(lang) {
                /*
                 * This call sets the locale for the current session and returns
                 * strings needed for off-line mode.
                 */
                var res = _api.call({
                    'request' : 'language',
                    'language' : lang
                });

                if (res.result.code === "0") {

                    _model.setLanguage(lang);

                    i18nRefresh(res);
                    /*
                     * By submitting, the newly localized login page is displayed
                     */
                    _ns.restartWebApp();
                }
            });

            /**
             * Callbacks: page clear
             */
            _view.pages.clear.onClear = function(ranges) {
                var res = _api.call({
                    request : 'page-delete',
                    ranges : ranges
                });
                if (res.result.code === "0") {
                    _view.pages.main.onRefreshPages();
                    $('#button-clear-pages-cancel').click();
                }
                _view.showApiMsg(res);
            };

            /**
             * Callbacks: page redeem voucher
             */
            _view.pages.voucherRedeem.onRedeemVoucher = function(cardNumber) {
                var res = _api.call({
                    request : "account-voucher-redeem",
                    cardNumber : cardNumber
                });
                if (res.result.code === "0") {
                    $("#button-voucher-redeem-back").click();
                }
                _view.showApiMsg(res);
            };

            /**
             * Callbacks: page outbox
             */
            _view.pages.outbox.onOutboxClear = function() {
                var res = _api.call({
                    request : 'outbox-clear'
                });
                if (res.result.code === "0") {
                    _model.user.stats = res.stats;
                    _model.myShowUserStats = true;
                }
                _view.showApiMsg(res);
            };
            
            _view.pages.outbox.onOutboxExtend = function() {
                var res = _api.call({
                    request : 'outbox-extend'
                });
                if (res.result.code === "0") {
                    _model.user.stats = res.stats;
                    _model.myShowUserStats = true;
                }
                _view.showApiMsg(res);
            };

            _view.pages.outbox.onOutboxDeleteJob = function(jobId) {
                var res = _api.call({
                    request : 'outbox-delete-job',
                    jobId : jobId
                });
                if (res.result.code === "0") {
                    _model.user.stats = res.stats;
                    _model.myShowUserStats = true;
                }
                _view.showApiMsg(res);
            };

            /**
             * Callbacks: page send
             */
            _view.pages.send.onSend = function(mailto, ranges, removeGraphics) {
                var res;
                
                if (_util.isEmailValid(mailto)) {
                    res = _api.call({
                        request : 'send',
                        mailto : mailto,
                        ranges : ranges,
                        graphics : ( removeGraphics ? '0' : '1')
                    });
                    if (res.result.code === "0") {
                        _model.user.stats = res.stats;
                        _model.myShowUserStats = true;
                    }
                    _view.showApiMsg(res);
                } else {
                    _view.message(_i18n.format('msg-email-invalid', [mailto]));
                }
            };

            /**
             * Callbacks: page Pdf properties
             */

            _view.pages.pdfprop.onShow = function() {
                _this.setLetterheadMenu('#pdf-letterhead-list');
                _this.setJobTitleMenu('#pdf-title-list');
                _view.checkCb('#pdf-remove-graphics', _model.removeGraphics);
            };

            /**
             * @return true if PDF props saved ok, false is an error occurred.
             */
            _view.pages.pdfprop.onHide = function() {

                _saveSelectedletterhead('#pdf-letterhead-list');
                _saveRemoveGraphics('#pdf-remove-graphics');

                _model.pdfPageRanges = $('#pdf-page-ranges').val();

                $('#pdf-page-ranges').val('');

                if (!_savePdfProps()) {
                    return false;
                }
                return true;
            };

            /**
             * @return true if PDF props saved ok, false is an error occurred.
             */
            _view.pages.pdfprop.onDownload = function() {

                _saveRemoveGraphics('#pdf-remove-graphics');

                if (!_saveSelectedletterhead('#pdf-letterhead-list', true)) {
                    return false;
                }
                if (!_savePdfProps()) {
                    return false;
                }
                window.location.assign(_api.getUrl4Pdf($('#pdf-page-ranges').val(), _model.removeGraphics));
                $('#pdf-page-ranges').val('');
                _model.myShowUserStatsGet = true;
                return true;
            };

            /**
             *
             */
            _view.pages.print.onFastProxyPrintRenew = function(showMsg) {
                var res = _api.call({
                    request : 'print-fast-renew'
                })
                //
                , resOk = (res.result.code === '0')
                //
                ;

                if (resOk) {
                    $('#printer-fast-print-expiry').text(res.expiry);
                }

                if (showMsg || !resOk) {
                    _view.message(res.result.txt);
                }

            };

            /**
             *
             */
            _view.pages.print.onPrintAuthCancel = function() {
                var res;

                _clearTimeoutAuthPrint();
                $('#sp-popup-print-auth').popup('close');

                res = _api.call({
                    request : 'print-auth-cancel',
                    idUser : _model.user.key_id,
                    printer : _model.myPrinter.name
                });
                _view.message(res.result.txt);
            };

            /**
             * Callbacks: page print
             */
            _view.pages.print.onPrint = function(isClear, isClose, removeGraphics) {

                var res
                //
                , clear = ( isClear ? '1' : '0')
                //
                , graphics = ( removeGraphics ? '0' : '1')
                //
                , sel, cost, visible
                //
                ;

                if (_saveSelectedletterhead('#print-letterhead-list')) {
                    return;
                }

                _model.myTitle = $('#print-title').val();

                res = _api.call({
                    user : _model.user.id,
                    request : 'printer-print',
                    printer : _model.myPrinter.name,
                    readerName : _model.myPrinterReaderName,
                    jobName : _model.myTitle,
                    pageScaling : _model.printPageScaling,
                    copies : $('#slider-print-copies').val(),
                    ranges : $('#print-page-ranges').val(),
                    graphics : graphics,
                    clear : clear,
                    options : JSON.stringify(_model.myPrinterOpt)
                });

                if (res.requestStatus === 'NEEDS_AUTH') {

                    _view.pages.print.clearInput();
                    _model.closePrintDlg = isClose;

                    _proxyprintEvent.poll(_model.user.key_id, _model.myPrinter.name, _model.myPrinterReaderName, _model.language);

                    _view.visible($('#auth-popup-content-wait'), true);
                    _view.visible($('#auth-popup-content-msg'), false);

                    /*
                     * Financial.
                     */
                    cost = res.formattedCost;
                    visible = (cost !== null);

                    sel = $('#sp-popup-print-auth-cost');
                    _view.visible(sel, visible);

                    if (visible) {
                        if (res.currencySymbol) {
                            cost = res.currencySymbol + ' ' + cost;
                        }
                        sel.text(cost);
                    }

                    /*
                     * Countdown start.
                     */
                    $('#sp-print-auth-countdown').text(res.printAuthExpirySecs);

                    /*
                     *
                     */
                    $('#sp-popup-print-auth').popup('open', {});

                    /*
                     * Countdown timer.
                     */
                    _countdownAuthPrint = res.printAuthExpirySecs - 1;
                    _timeoutAuthPrint = window.setInterval(function() {
                        $('#sp-print-auth-countdown').text(_countdownAuthPrint);
                        if (_countdownAuthPrint-- === 0) {
                            _clearTimeoutAuthPrint();
                        }
                    }, 1000);

                    return;
                }

                if (res.result.code === '0') {
                    _view.pages.print.clearInput();
                    if (isClose) {
                        $('#button-printer-back').click();
                    }
                    if (isClear) {
                        _view.pages.main.onRefreshPages();
                    }
                    _model.user.stats = res.stats;
                    _model.myShowUserStats = true;
                }
                _view.showApiMsg(res);

            };

            /**
             *
             */
            _view.pages.print.onPrinter = function(printerName) {
                var res;

                if (!_model.myPrinter || _model.myPrinter.name !== printerName) {

                    res = _api.call({
                        'request' : 'printer-detail',
                        'printer' : printerName
                    });

                    if (res.result.code === '0') {
                        _model.myPrinter = res.printer;
                        _model.setPrinterDefaults();
                        _model.myFirstPageShowPrintSettings = true;
                    }

                    _view.visible($('#button-print-settings'), _model.myPrinter.groups.length > 0);
                    
                    _view.showUserPageAsync('#page-printer-settings', 'PrinterSettings');
                    
                }

                _prepareReaderForPrinter();

            };

            /**
             * TODO: looks very much like onPrinter() function above
             */
            _view.pages.print.onSettings = function(printerName) {
                var res;

                if (_model.myPrinter.name !== printerName) {

                    res = _api.call({
                        'request' : 'printer-detail',
                        'printer' : printerName
                    });

                    if (res.result.code === '0') {
                        _model.myPrinter = res.printer;
                        _model.setPrinterDefaults();
                        _model.myFirstPageShowPrintSettings = true;
                    }
                }
                _view.showUserPageAsync('#page-printer-settings', 'PrinterSettings');
            };

            _view.pages.print.onClearPrinter = function() {
                    _model.myPrinter = undefined;
                    _model.isPrintManualFeed = false;                    
                    _model.printPageScaling = 'CROP';
                    _model.setJobsMatchMediaSources(_view);
                    _model.setJobsMatchMedia(_view);
                    _model.myPrinterOpt = [];
                    _refreshPrinterInd();
            };

            _view.pages.print.onShow = function() {

                if (_model.myFirstPageShowPrint) {
                    _model.setJobsMatchMediaSources(_view);
                    _model.myFirstPageShowPrint = false;
                }

                /*
                 * Make sure that _prepareReaderForPrinter() is performed,
                 * either by refreshing the list, or by calling directly.
                 * Reason: in case of onWakeup, the _proxyprintEvent listener
                 * is stopped.
                 */
                _prepareReaderForPrinter();

                _view.checkCb('#print-remove-graphics', _model.removeGraphics);
                _this.setLetterheadMenu('#print-letterhead-list');
                _this.setJobTitleMenu('#print-title-list');

                $('#print-title').val(_model.myTitle);

                // Refreshes display of possible changed inbox media.
                _model.setJobsMatchMedia(_view);
                
                _refreshPrinterInd();
            };

            _view.pages.print.onHide = function() {
                _refreshPrinterInd();
            };

            /**
             * Callbacks: page letterhead
             */

            _view.pages.letterhead.onApply = function(id, name, fg, pub, pubNew) {

                var res = _api.call({
                    request : 'letterhead-set',
                    id : id,
                    data : JSON.stringify({
                        name : name,
                        foreground : fg,
                        pub : pub,
                        'pub-new' : pubNew
                    })
                });
                _view.showApiMsg(res);

                /*
                 * update in cache.
                 */
                if (res.result.code === "0") {
                    $.each(_model.letterheads, function(key, value) {
                        if (id === value.id) {
                            value.name = name;
                            value.foreground = fg;
                            value.pub = pubNew;
                        }
                    });
                }
            };
            // ----------------
            _view.pages.letterhead.onRefresh = function() {
                _refreshLetterheadList();
            };

            // ----------------
            _view.pages.letterhead.onShow = function() {
                _view.visible($('#button-letterhead-create'), _model.myJobs.length && !_model.myJobsDrm);

                if (_model.myFirstPageShowLetterhead) {
                    _model.myFirstPageShowLetterhead = false;
                    _refreshLetterheadList();
                } else {
                    _this.setLetterheadMenu('#letterhead-list');
                    _adaptLetterheadPage();
                }
            };

            /**
             * Saves the PDF properties.
             *
             * @return true if saved ok, false is an error occurred
             */
            _savePdfProps = function() {
                var pdf = _model.propPdf
                //
                , res = _api.call({
                    request : 'pdf-set-properties',
                    props : JSON.stringify(_model.propPdf)
                });
                _view.showApiMsg(res);
                _view.visible($('#button-mini-lock'), pdf.encryption !== '' && pdf.apply.encryption);
                _view.visible($('#button-mini-keys'), (pdf.pw.owner !== '' || pdf.pw.user !== '') && pdf.apply.passwords);

                return (res && res.result.code === '0');
            };

            /**
             * Saves (attach/detach) the selected letterhead from the selector
             * (optionmenu).
             *
             * @param sel
             *            The selector.
             * @param force
             *            If true, always do attach/detach API call.
             *
             * @return true if saved successfully, false when save failed.
             */
            _saveSelectedletterhead = function(sel, force) {
                var pub, res = null, ret = false
                //
                , lh
                //
                , letterheadIdx = $(sel + ' :selected').val()
                //
                , mini = $('#button-mini-letterhead')
                //
                ;

                _view.visible(mini, letterheadIdx !== 'none');

                if (!force && (letterheadIdx === _model.myLetterheadIdx)) {
                    return;
                }

                if (letterheadIdx === 'none') {
                    res = _api.call({
                        'request' : 'letterhead-detach'
                    });
                } else {
                    lh = _model.getSelLetterheadObj(sel);
                    if (lh) {
                        pub = lh.pub;
                        res = _api.call({
                            'request' : 'letterhead-attach',
                            'id' : letterheadIdx,
                            pub : ( pub ? '1' : '0')
                        });
                    }
                }

                if (res) {
                    if (res.result.code === "0") {
                        _model.myLetterheadIdx = letterheadIdx;
                        if (letterheadIdx === 'none') {
                            _model.letterheadDefault = null;
                        } else {
                            $.each(_model.letterheads, function(key, value) {
                                if (_model.myLetterheadIdx === value.id) {
                                    _model.letterheadDefault = value.id;
                                }
                            });
                        }
                        ret = true;
                    } else {
                        _model.myLetterheadIdx = 'none';
                        _model.letterheadDefault = null;
                    }
                    _view.showApiMsg(res);
                }

                return ret;
            };

            // ----------------
            _view.pages.letterhead.onHide = function() {
                _saveSelectedletterhead('#letterhead-list');
            };

            // ----------------
            _view.pages.letterhead.onChange = function() {
                _adaptLetterheadPage();
            };

            // ----------------
            _view.pages.letterhead.onDelete = function(id) {
                var pub = _view.pages.letterhead.getSelected().pub
                //
                , res = _api.call({
                    'request' : 'letterhead-delete',
                    'id' : id,
                    pub : ( pub ? '1' : '0')
                });
                _view.showApiMsg(res);
                if (res.result.code === '0') {
                    _refreshLetterheadList();
                }
            };

            // ----------------
            _view.pages.letterhead.onCreate = function() {
                var res = _api.call({
                    'request' : 'letterhead-new'
                });
                if (res.result.code === '0') {
                    _refreshLetterheadList();
                }
                _view.showApiMsg(res);
            };

            /**
             * Callbacks: page login
             */

            _view.pages.login.onShow(function() {
                _model.user.loggedIn = false;
            });

            _view.pages.login.onLanguage(function() {
                _view.pages.language.loadShowAsync();
            });

            _view.pages.login.onLogin(function(mode, id, pw, assocCardNumber) {
                _this.login(mode, id, pw, null, assocCardNumber);
            });

            _view.pages.login.onCardAssocCancel = function() {
                if (_model.authCardIp) {
                    _deviceEvent.resume();
                }
            };

            /**
             * Callbacks: page main
             */

            _view.pages.main.onCreated = function() {
                /*
                 * Because of #320 we want to know for sure all images on the
                 * main page are loaded before we start CometD long polling.
                 */
                _cometd.start(_model.user.cometdToken);
            };

            _view.pages.main.onLogout = function() {
                /*
                 *
                 */
                var res = _api.call({
                    request : 'logout',
                    authToken : _model.authToken.token
                });

                if (res.result.code !== '0') {
                    /*
                     * NOTE: when we are disconnected the onDisconnected()
                     * callback is called, which displays the login window. The BACK
                     * button WILL work in this case. See Mantis #108 how to prevent
                     * this.
                     *
                     * Is there a way to unload the whole application !??
                     * window.location.reload(true) does NOT work (why?)
                     */

                    /*
                     * Do NOT use _view.showApiMsg(res), because the login dialog
                     * will not show.
                     */
                    _view.message(res.result.txt);
                    _model.setAuthToken(null, null);
                }

                _view.pages.main.onClose();

            };

            _view.pages.main.onClose = function() {

                _userEvent.removeListener();
                _proxyprintEvent.removeListener();

                _model.startSession();

                /*
                 * Prevent that BACK button shows private data when disconnected.
                 * Mantis #108
                 */
                $('#page-main').empty();

                /*
                * A BRUTE FORCE solution: make sure back button does not work, i.e.
                * another user could use back button on login screen to get to
                * pages of the previous user !!
                *
                * NOTE: parameter is set to true to force the browser to get the
                * page from the server (default false reloads the page from the
                * cache).
                */
                // window.location.reload(true);
                /*
                 * TEST: is this alternative better? Yes, but it only works when
                 * we
                 * are connected (because the commit is implemented by wicket as
                 * an
                 * Ajax call).
                 */
                _ns.restartWebApp();
            };

            _view.pages.main.onShow = function() {

                var res;

                _ns.deferAppWakeUp(false);
                // first statement

                _userEvent.resume();

                if (_model.myShowUserStats || _model.myShowUserStatsGet) {
                    _model.myShowUserStats = false;
                    if (_model.myShowUserStatsGet) {
                        _model.myShowUserStatsGet = false;
                        res = _api.call({
                            'request' : 'user-get-stats'
                        });
                        if (res.result.code === "0") {
                            _model.user.stats = res.stats;
                        } else {
                            _view.showApiMsg(res);
                        }
                    }
                    _view.pages.main.showUserStats();
                }
            };

            _view.pages.main.onHide = function() {
                _userEvent.pause();
            };

            _view.pages.main.onPageMove = function(ranges, position) {

                var data = _api.call({
                    'request' : 'page-move',
                    'ranges' : ranges,
                    'position' : position
                });
                if (data.result.code === "0") {
                    $('#main-arr-undo').click();
                    _view.pages.main.onRefreshPages();
                }
                _view.showApiMsg(data);
            };

            _view.pages.main.onPageDelete = function(ranges) {
                var data = _api.call({
                    'request' : 'page-delete',
                    'ranges' : ranges
                });
                if (data.result.code === "0") {
                    _view.pages.main.onRefreshPages();
                }
                _view.showApiMsg(data);
            };

            /*
             * Expands a page image representing multiple pages.
             */
            _view.pages.main.onExpandPage = function(nPage) {

                var data = _api.call({
                    request : 'job-pages',
                    'first-detail-page' : nPage,
                    'unique-url-value' : _model.uniqueImgUrlValue,
                    base64 : (_view.imgBase64 ? '1' : '0')
                });
                if (data.result.code === '0') {
                    _handleSafePageEvent(data);
                } else {
                    _view.showApiMsg(data);
                }

            };

            _view.pages.main.onRefreshApp = function() {
                /*
                 * Exit/clear the UserEventMonitor for this user in THIS SESSION,
                 * so we can start with a clean slade. If we do NOT close it here
                 * we run the risk of having two (2) or more UserEventMonitor's
                 * running for the same client IP address.
                 */
                _api.call({
                    request : 'exit-event-monitor'
                });
                _view.pages.main.onClose();
            };

            _view.pages.main.onRefreshPages = function() {
                var main = _view.pages.main;

                _model.refreshUniqueImgUrlValue4Browser();

                main.clearEditState();
                /*
                 * Use the expand page to initialize view on the first page.
                 */
                main.onExpandPage(0);
            };

            _view.pages.main.onPopupJobApply = function() {

                var data = _api.call({
                    'request' : 'job-edit',
                    ijob : _model.iPopUpJob,
                    data : JSON.stringify({
                        rotate : $('#sp-popup-job-rotate').is(':checked'),
                        undelete : $('#sp-popup-job-undelete').is(':checked')
                    })
                });
                if (data.result.code === '0') {
                    $('#sp-popup-job-info').popup('close');
                    _view.pages.main.onRefreshPages();
                }
                _view.showApiMsg(data);
            };

            _view.pages.main.onPopupJobDelete = function() {
                var data = _api.call({
                    'request' : 'job-delete',
                    ijob : _model.iPopUpJob
                });
                if (data.result.code === '0') {
                    $('#sp-popup-job-info').popup('close');
                    _view.pages.main.onRefreshPages();
                }
                _view.showApiMsg(data);
            };

            /**
             * Callbacks: page User Password Reset
             */
            _view.pages.userPwReset.onSelectReset(function(password) {
                var res = _api.call({
                    request : 'reset-user-pw',
                    iuser : _model.user.id,
                    password : password
                });
                _view.showApiMsg(res);
                if (res.result.code === '0') {
                    _view.changePage($('#page-dashboard'));
                }
            });

            /**
             * Callbacks: page User PIN Reset
             */
            _view.pages.userPinReset.onSelectReset(function(pin) {
                var res = _api.call({
                    request : 'reset-user-pin',
                    user : _model.user.id,
                    pin : pin
                });
                _view.showApiMsg(res);
                if (res.result.code === '0') {
                    _view.changePage($('#page-dashboard'));
                }
            });

        }// Controller

        /**
         *
         */
        $.SavaPage = function() {

            var _i18n = new _ns.I18n(), _model = new Model()
            //
            , _api = new _ns.Api(_i18n, _model.user)
            //
            , _view = new _ns.View(_i18n, _api)
            //
            , _cometd, _userEvent, _deviceEvent, _proxyprintEvent, _ctrl
            //
            ;

            _view.pages = {
                language : new _ns.PageLanguage(_i18n, _view, _model),
                login : new _ns.PageLogin(_i18n, _view, _api),
                letterhead : new PageLetterhead(_i18n, _view, _model),
                clear : new PageClear(_i18n, _view, _model),
                accountTrx : new PageAccountTrx(_i18n, _view, _model, _api),
                doclog : new PageDocLog(_i18n, _view, _model, _api),
                outbox : new PageOutbox(_i18n, _view, _model),
                send : new PageSend(_i18n, _view, _model),
                pagebrowser : new PageBrowser(_i18n, _view, _model),
                pageDashboard : new PageDashboard(_i18n, _view, _model),
                voucherRedeem : new PageVoucherRedeem(_i18n, _view, _model),
                pdfprop : new PagePdfProp(_i18n, _view, _model),
                main : new PageMain(_i18n, _view, _model),
                print : new PagePrint(_i18n, _view, _model, _api),
                print_settings : new PagePrintSettings(_i18n, _view, _model),
                fileUpload : new PageFileUpload(_i18n, _view, _model),
                userPinReset : new PageUserPinReset(_i18n, _view, _model),
                userPwReset : new _ns.PageUserPasswordReset(_i18n, _view, _model)
            };

            _cometd = new _ns.Cometd();
            _userEvent = new UserEvent(_cometd, _api);

            // Mantis #320
            _ns.cometd = _cometd;
            _ns.userEvent = _userEvent;
            _ns.model = _model;
            //

            _deviceEvent = new DeviceEvent(_cometd);
            _proxyprintEvent = new ProxyPrintEvent(_cometd);

            _ctrl = new Controller(_i18n, _model, _view, _api, _cometd, _userEvent, _deviceEvent, _proxyprintEvent);

            this.init = function() {
                
                var user = _ns.Utils.getUrlParam('user');

                _ctrl.init();

                if (user) {
                    _ctrl.login(_view.AUTH_MODE_NAME, user, null, null);
                } else if (_model.authToken.user && _model.authToken.token) {
                    _ctrl.login(_view.AUTH_MODE_NAME, _model.authToken.user, null, _model.authToken.token);
                } else {
                    _ctrl.initLoginUserInput();
                    // Initial load/show of Login dialog
                    _view.pages.login.loadShowAsync();
                }
            };
            
            $(window).on('beforeunload', function() {
                // NOT returning anything will not show the unload dialog.
                /*
                if (!_ns.isRestartWebApp) {
                    return document.title;
                }
                */
            }).on('unload', function() {    
                _api.removeCallbacks();
                _api.call({
                    request : 'webapp-unload'
                });
            });            
        };

        // Instantiate the singleton savapage instance when the DOM is leaded.
        $(function() {
            $.savapage = new $.SavaPage();
            // do NOT initialize here (to early for some browsers, like Opera)
            // $.savapage.init();
        });

        // Initialize AFTER document is read
        $(document).on("ready", null, null, function() {
            $.savapage.init();            
        });
        
    
    }(jQuery, this, this.document, JSON, this.org.savapage));
