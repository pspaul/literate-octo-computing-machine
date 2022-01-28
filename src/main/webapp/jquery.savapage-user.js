// @license http://www.gnu.org/licenses/agpl-3.0.html AGPL-3.0

/*! SavaPage jQuery Mobile User Web App | (c) 2020 Datraverse B.V. | GNU
 * Affero General Public License */

/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */

/*
 * NOTE: the *! comment blocks are part of the compressed version.
 */

/*jslint browser: true*/
/*global $, jQuery, alert*/

/*
 * SavaPage jQuery Mobile User Web App
 */
(function($, window, document, JSON, _ns) {
    "use strict";

    /* jslint browser: true */
    /* global $, jQuery, alert */

    _ns.CSS_CLASS_THUMBNAIL_PNG = 'sp-thumbnail-png';
    _ns.CSS_CLASS_THUMBNAIL_SVG = 'sp-thumbnail-svg-overlay';
    _ns.CSS_CLASS_THUMBNAIL_PNG_SELECTED = 'main_selected';
    _ns.CSS_CLASS_PRELOAD_SPINNER = 'sp-preload-spinner';
    _ns.CSS_CLASS_THUMBNAIL_SUBSCRIPT = 'sp-thumbnail-subscript';
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
        $(imgDomElement).removeAttr('height').removeClass(_ns.CSS_CLASS_PRELOAD_SPINNER);
        _ns.thumbnails2Load--;
        if (_ns.thumbnails2Load === 0) {
            // All thumbnails are loaded, so resume.
            _ns.userEvent.resume();
        }
    };

    // See Java Wicket: Browser.html
    _ns.removeSpinnerL = function(imgDomElement) {
        var sel = $(imgDomElement);
        sel.removeClass(_ns.CSS_CLASS_PRELOAD_SPINNER + '-l');
        // Restore heigh/width.
        if (sel.hasClass('fit_width')) {
            sel.css('height', '');
        } else {
            sel.css('width', '');
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
     * When the jQuery Mobile starts to execute, it triggers a mobileinit
     * event on the document object, to which you can bind to apply overrides
     * to
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
        var _this = this,
            _longPollPending = false,
            _paused = false,
            _onEvent,
            _subscription;

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

            if (!_subscription) {
                _longPollPending = false;
                _subscription = _cometd.addListener('/device/event', _onEvent);
            }
            // Get things started: invite to do a poll
            this.onPollInvitation();
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
        this.poll = function(language, country) {
            _longPollPending = true;
            //this.onWaitingForEvent();
            try {
                $.cometd.publish('/service/device', {
                    language: language,
                    country: country
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
        var _this = this,
            _onEvent,
            _longPollPending = false,
            _subscription;

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
        this.poll = function(idUser, printerName, readerName, language, country) {

            if (!_longPollPending) {

                _longPollPending = true;

                // lazy subscribe
                if (!_subscription) {
                    this.addListener();
                }
                try {
                    $.cometd.publish('/service/proxyprint', {
                        idUser: idUser,
                        printerName: printerName,
                        readerName: readerName,
                        language: language,
                        country: country
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
            _this = this,
            _longPollStartTime = null,
            _paused = false,
            _onEvent,
            _subscription;

        /**
         * NOTE: use _this instead of this.
         */
        _onEvent = function(message) {
            var res = $.parseJSON(message.data);

            if (_ns.logger.isDebugEnabled()) {
                _ns.logger.debug('UserEvent: event ' + res.event + (_paused ? ' (paused)' : ''));
            }

            if (res.event === "SYS_MAINTENANCE") {
                _this.onSysMaintenance();
                return;
            }

            _longPollStartTime = null;

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

                    if (res.event === "PRINT_MSG" || res.event === "ACCOUNT" || res.event === "JOBTICKET") {
                        _this.onMsgEvent(res.data);
                        _this.onAccountEvent(res.stats);
                    } else if (res.event === "PRINT_IN_EXPIRED") {
                        _this.onMsgEvent(res.data, true);
                    } else if (res.event === "NULL") {
                        _this.onNullEvent(res.data);
                        _this.onAccountEvent(res.stats);
                    }
                    _this.onPollInvitation();
                }
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
                if (_ns.logger.isDebugEnabled()) {
                    _ns.logger.debug('UserEvent: addListener + onPollInvitation');
                }

                this.onPollInvitation();
            }
        };

        /*
         * Does NOT work since _subscription remains undefined (?).
         */
        this.removeListener = function() {
            if (_subscription) {
                if (_ns.logger.isDebugEnabled()) {
                    _ns.logger.debug('UserEvent: removeListener');
                }
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
        this.poll = function(userid, useridDocLog, pagecount, uniqueUrlVal,
            prevMsgTime, language, country, base64,
            userInternal, userDocLogInternal) {

            if (!_longPollStartTime && userid && _cometd.isOn()) {

                if (_ns.logger.isDebugEnabled()) {
                    _ns.logger.debug('UserEvent: poll()');
                }

                _longPollStartTime = new Date().getTime();
                this.onWaitingForEvent();
                try {
                    $.cometd.publish('/service/user', {
                        user: userid,
                        userInternal: userInternal,
                        userDocLog: useridDocLog,
                        userDocLogInternal: userDocLogInternal,
                        'page-offset': pagecount,
                        'unique-url-value': uniqueUrlVal,
                        'msg-prev-time': prevMsgTime,
                        language: language,
                        country: country,
                        base64: base64,
                        webAppClient: true
                    });
                } catch (err) {
                    _ns.logger.warn('UserEvent poll() exception: ' + err);
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
            if (_ns.logger.isDebugEnabled()) {
                _ns.logger.debug('UserEvent: pause()');
            }
            _paused = true;
            _api.call({
                request: 'exit-event-monitor'
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
                if (_ns.logger.isDebugEnabled()) {
                    _ns.logger.debug('UserEvent: resume()');
                }
                this.onPollInvitation();
            }
        };

    }// UserEvent

    /**
     *
     */
    function PageLetterhead(_i18n, _view, _model) {
        var _this = this;

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
                var src = $(event.target).attr('src'),
                    html;

                if (src) {
                    /*
                     * The letterhead thumbnail is already in full detail, so
                     * the detail pop-up will position as expected (as the
                     * height
                     * and width are known).
                     */
                    html = '<img alt="" src="' + src + '"/>';
                    html += '<a href="#" data-rel="back" class="ui-btn ui-corner-all ui-shadow ui-btn-a ui-icon-delete ui-btn-icon-notext ui-btn-right"/>';

                    $('#sp-popup-letterhead-page').html(html).enhanceWithin().popup('open', {
                        positionTo: 'window'
                    });
                }
            });

        }).on("pagebeforeshow", function(event, ui) {
            _this.onShow();
        }).on("pageshow", function(event, ui) {
            _ns.userEvent.pause();
        }).on('pagebeforehide', function(event, ui) {
            _this.onHide();
        }).on("pagehide", function(event, ui) {
            _ns.userEvent.resume();
        });
    }

    /**
     *
     */
    function PageBrowser(_i18n, _view, _model, _api) {
        const _CSS_CLASS_ROTATED = 'sp-img-rotated',
            _ID_CANVAS_IMG_DIV = 'sp-canvas-browser-img-div',
            _ID_CANVAS_IMG = 'sp-canvas-browser-img',
            _ID_CANVAS_CURTAIN = 'sp-canvas-browser-img-curtain';

        var _this = this,
            _imgCanvasEditor,
            _imgCanvasObjCountLoaded,
            _imgScrollBarWidth,
            _isBrushMode,
            _drawButtonPresent,
            _drawButtonClicked,
            _drawPanelOpen,

            /** */
            _setHtmlCanvasBrushWidth = function(sel) {
                _imgCanvasEditor.setBrushWidth(parseInt(sel.val(), 10));
            },

            /** */
            _setHtmlCanvasStrokeWidth = function(sel) {
                _imgCanvasEditor.setStrokeWidth(parseInt(sel.val(), 10));
                _imgCanvasEditor.setStrokeWidthSelected();
            },

            /** */
            _setHtmlCanvasDrawingMode = function() {
                _isBrushMode = _view.isCbChecked($('#sp-canvas-use-brush'));
                _imgCanvasEditor.enableDrawingMode(_isBrushMode);
                _view.enableUI($('.sp-canvas-drawing-mode-select'), !_isBrushMode);
                _view.visible($('.sp-canvas-drawing-mode-select-prop'), !_isBrushMode);
                _view.visible($('.sp-canvas-drawing-mode-brush'), _isBrushMode);
                _view.enableCheckboxRadio($('#sp-canvas-drawing-props-fixed'), !_isBrushMode);
                _view.enable($('#sp-canvas-drawing-select-all'), !_isBrushMode);
            },

            /** */
            _areHtmlCanvasDrawingPropsFixed = function() {
                return _view.isCbChecked($('#sp-canvas-drawing-props-fixed'));
            },

            /** */
            _initHtmlCanvasEditor = function() {
                var selToolsPanel = $('#sp-canvas-tools-panel');

                _imgCanvasEditor = new _ns.HtmlCanvasEditor(_ID_CANVAS_IMG, //
                    function(nObjects) {
                        // onAfterRender
                        _view.enable($('#sp-canvas-drawing-clear-all'), nObjects > 0);
                        _view.enable($('#sp-canvas-drawing-select-all'), !_isBrushMode && nObjects > 0);
                    }, function(nObjectsSelected) {
                        // onSelectionCreated
                        _view.enable($('#sp-canvas-drawing-selected-fill-transparent'), true);
                        _view.enable($('#sp-canvas-drawing-clear-selection'), true);
                        _view.enable($('#sp-canvas-drawing-info-selection'), nObjectsSelected === 1);
                    }, function() {
                        // onSelectionCleared
                        _view.enable($('#sp-canvas-drawing-selected-fill-transparent'), false);
                        _view.enable($('#sp-canvas-drawing-clear-selection'), false);
                        _view.enable($('#sp-canvas-drawing-info-selection'), false);
                    });

                _imgCanvasEditor.setFreeDrawingBrush('Pencil', $('#sp-canvas-drawing-brush-color').val(), 1);

                _setHtmlCanvasDrawingMode();
                _setHtmlCanvasBrushWidth($('#sp-canvas-drawing-brush-width'));
                _setHtmlCanvasStrokeWidth($('#sp-canvas-drawing-select-stroke-width'));

                //
                $("#sp-browser-page-draw").click(function() {
                    _drawButtonClicked = true;
                    $('#sp-browser-page-actions').popup('close');
                    $('#sp-canvas-tools-panel').panel("toggle");
                });

                selToolsPanel.panel("option", "animate", false);
                selToolsPanel.on("panelopen", function(event, ui) {
                    var css = {
                        'padding-left': $('#sp-canvas-tools-panel').css('width')
                    };
                    $('#content-browser').css(css);
                    $('#footer-browser-div').css(css);
                    $('#' + _ID_CANVAS_CURTAIN).hide();
                    _drawButtonClicked = false;
                }).on("panelclose", function(event, ui) {
                    var css = {
                        'padding-left': '0px'
                    };
                    // <Esc> is propagated to popup and panel
                    // Prevent close of panel if popup menu is active.
                    if (!_drawButtonClicked && $('#sp-browser-page-actions').parent().hasClass('ui-popup-active')) {
                        $(this).panel('open');
                        return false;
                    }
                    _drawButtonClicked = false;
                    $('#content-browser').css(css);
                    $('#footer-browser-div').css(css);
                    _imgCanvasEditor.deactivateAll();
                    $('#' + _ID_CANVAS_CURTAIN).show();
                });

                //
                $("#sp-canvas-drawing-add-rect").click(function() {
                    _imgCanvasEditor.addRect();
                });
                $("#sp-canvas-drawing-add-circle").click(function() {
                    _imgCanvasEditor.addCircle();
                });
                $("#sp-canvas-drawing-add-triangle").click(function() {
                    _imgCanvasEditor.addTriangle();
                });
                $("#sp-canvas-drawing-add-line").click(function() {
                    _imgCanvasEditor.addLine();
                });
                $("#sp-canvas-drawing-add-textbox").click(function() {
                    _imgCanvasEditor.addTextbox();
                });

                $('#sp-canvas-drawing-props-fixed').click(function() {
                    $('.sp-canvas-drawing-select-color').toggleClass('sp-canvas-color-fixed');
                });

                $("#sp-canvas-drawing-select-all").click(function() {
                    _imgCanvasEditor.selectAll();
                });

                $("#sp-canvas-drawing-clear-all").click(function() {
                    var prvZoom = _imgCanvasEditor.unZoom();
                    _imgCanvasEditor.clear();
                    _imgCanvasEditor.setBackgroundImage(_getActiveImageUrl());
                    _imgCanvasEditor.setZoom(prvZoom);
                    _resizeOverlayCanvas();
                });

                $("#sp-canvas-drawing-clear-selection").click(function() {
                    _imgCanvasEditor.clearSelection();
                });

                $("#sp-canvas-drawing-info-selection").click(function() {
                    _imgCanvasEditor.debugSelection();
                });

                $("#sp-canvas-drawing-zoomin").click(function() {
                    _imgCanvasEditor.zoomIn();
                    _resizeOverlayCanvas();
                });
                $("#sp-canvas-drawing-zoomout").click(function() {
                    _imgCanvasEditor.zoomOut();
                    _resizeOverlayCanvas();
                });
                $("#sp-canvas-drawing-unzoom").click(function() {
                    _imgCanvasEditor.unZoom();
                    _resizeOverlayCanvas();
                });

                $("#sp-canvas-drawing-save").click(function() {
                    var prvZoom = _imgCanvasEditor.unZoom(),
                        nObj = _imgCanvasEditor.countObjects(),
                        res = _api.call({
                            request: 'page-set-overlay',
                            dto: JSON.stringify({
                                imgUrl: $('#page-browser-images .active').attr('src'),
                                svg64: nObj > 0 ? window.btoa(_imgCanvasEditor.toSVG()) : null,
                                json64: nObj > 0 && _imgCanvasEditor.hasTextObjects() ? window.btoa(_imgCanvasEditor.toJSON()) : null
                            })
                        });

                    _imgCanvasEditor.setZoom(prvZoom);
                    _resizeOverlayCanvas();

                    _view.showApiMsg(res);
                    if (res.result.code === "0") {
                        _this.onOverlayOnOff();
                    }
                });

                $("#sp-canvas-drawing-undo-all").click(function() {
                    var prvZoom = _imgCanvasEditor.unZoom();
                    _setOverlayCanvas();
                    _imgCanvasEditor.setZoom(prvZoom);
                    _resizeOverlayCanvas();
                });

                $("input:checkbox[id='sp-canvas-use-brush']").change(function(event) {
                    _setHtmlCanvasDrawingMode();
                });

                $('#sp-canvas-drawing-brush-color').on('input', function() {
                    _imgCanvasEditor.setBrushColor($(this).val());
                });

                $('#sp-canvas-drawing-brush-width').on('input', function() {
                    _setHtmlCanvasBrushWidth($(this));
                });

                $('#sp-canvas-drawing-select-stroke-color').click(function(e) {
                    if (_areHtmlCanvasDrawingPropsFixed()) {
                        _imgCanvasEditor.setStrokeSelected();
                        e.preventDefault();
                    }
                }).on('input', function() {
                    _imgCanvasEditor.setStroke($(this).val());
                    _imgCanvasEditor.setStrokeSelected();
                });

                $('#sp-canvas-drawing-select-fill-color').click(function(e) {
                    if (_areHtmlCanvasDrawingPropsFixed()) {
                        _imgCanvasEditor.setFillSelected();
                        e.preventDefault();
                    }
                }).on('input', function() {
                    _imgCanvasEditor.setFill($(this).val());
                    _imgCanvasEditor.setFillSelected();
                });

                $('#sp-canvas-drawing-selected-fill-transparent').click(function(e) {
                    _imgCanvasEditor.setFillSelectedExt(null);
                    e.preventDefault();
                });

                $('#sp-canvas-drawing-select-stroke-width').click(function(e) {
                    _setHtmlCanvasStrokeWidth($(this));
                }).on('input', function() {
                    _setHtmlCanvasStrokeWidth($(this));
                });

                $('#sp-canvas-drawing-opacity').click(function(e) {
                    _imgCanvasEditor.setOpacitySelected();
                }).on('input', function() {
                    _imgCanvasEditor.setOpacityPerc(parseInt($(this).val(), 10));
                    _imgCanvasEditor.setOpacitySelected();
                });

                $('#page-browser-images').addClass('sp-overflow-scroll');
                _imgScrollBarWidth = _view.getOverflowScrollBarWidth();

                // Hide images in favour of canvas editor.
                $('#page-browser-images img').each(function() {
                    _view.visible($(this), false);
                });
                // Show <div> container of <canvas>
                _view.visible($('#' + _ID_CANVAS_IMG_DIV), true);
            },

            /** */
            _getMaxImgHeight = function() {
                var yContentPadding,
                    yImagePadding,
                    yFooter,
                    yImage,
                    yImageScrollbar = 0,
                    yViewPort;
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
                //| | (yImageScrollbar)  (optional)   |
                //| | (yImagePadding)                 |
                //| +---------------------------------+
                //| (yContentPadding)
                //| +---------------------------------+ #footer-browser
                //| | xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx | (yFooter)
                //| +---------------------------------+
                //| (yContentPadding)
                //+-------------------------------------------
                yViewPort = _view.getViewportHeight();

                yContentPadding = $('#content-browser').position().top;
                yImageScrollbar = _imgCanvasEditor ? _imgScrollBarWidth : 0;

                yImagePadding = $('#page-browser-images').position().top - $('#content-browser').position().top;
                yFooter = $('#footer-browser').outerHeight(true);

                yImage = yViewPort - 3 * yContentPadding - yFooter - 2 * yImagePadding - yImageScrollbar;

                return yImage;
            },

            /** */
            _resizeOverlayCanvasExt = function(selImg, selDiv) {
                var zoom = _imgCanvasEditor.getZoom(),
                    w = zoom * selImg.get(0).naturalWidth,
                    h = zoom * selImg.get(0).naturalHeight,
                    hMax = _getMaxImgHeight();

                _imgCanvasEditor.setWidth(w);
                _imgCanvasEditor.setHeight(h);

                selDiv.width(w);
                if (h < hMax) {
                    selDiv.height(h);
                } else {
                    selDiv.height(hMax);
                }

                $('#' + _ID_CANVAS_CURTAIN).css({
                    'height': h,
                    'width': w,
                    'margin-top': '-' + h + 'px'
                });
            },

            /** */
            _resizeOverlayCanvas = function() {
                _resizeOverlayCanvasExt($('#page-browser-images .active'), $('#' + _ID_CANVAS_IMG_DIV));
            },

            /** */
            _getActiveImageUrl = function() {
                return $('#page-browser-images .active').attr('src');
            },

            /** */
            _setOverlayCanvas = function() {
                var selImg,
                    w,
                    h,
                    hMax,
                    imgUrl,
                    res;

                selImg = $('#page-browser-images .active');
                w = selImg.get(0).naturalWidth;
                if (w === 0) {
                    $.mobile.loading("show");
                    // Image not loaded yet, try again: RECURSE !!
                    window.setTimeout(_setOverlayCanvas, 500);
                } else {
                    $.mobile.loading("hide");

                    imgUrl = selImg.attr('src');
                    _resizeOverlayCanvasExt(selImg, $('#' + _ID_CANVAS_IMG_DIV));

                    _imgCanvasEditor.clear();

                    if (_drawButtonPresent) {
                        // Load overlay
                        res = _api.call({
                            request: 'page-get-overlay',
                            dto: JSON.stringify({
                                imgUrl: imgUrl
                            })
                        });

                        if (res.result.code === '0') {
                            if (res.dto.json64) {
                                _imgCanvasEditor.loadJSON(window.atob(res.dto.json64));
                            } else if (res.dto.svg64) {
                                _imgCanvasEditor.loadSVG(window.atob(res.dto.svg64));
                            }
                            _imgCanvasObjCountLoaded = _imgCanvasEditor.countObjects();
                        } else {
                            _view.showApiMsg(res);
                        }
                    }
                    // Set background after loadJSON / loadSVG !
                    _imgCanvasEditor.setBackgroundImage(imgUrl);
                }
            },

            /** */
            _setSliderValue = function(value) {
                $('#browser-slider').val(value).slider("refresh").trigger('change');
            },

            /** */
            _getSliderMax = function(selSlider) {
                return parseInt(selSlider.attr('max'), 10);
            },
            /** */
            _navSlider = function(increment) {
                var selSlider = $('#browser-slider'),
                    max = _getSliderMax(selSlider),
                    value;

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
            },

            /** */
            _navRight = function() {
                _navSlider(1);
            },

            /** */
            _navLeft = function() {
                _navSlider(-1);
            };

        /** */
        this.isDrawActive = function() {
            return _drawButtonPresent;
        };
        /**
         * Adds (replace) page images to the browser.
         */
        this.addImages = function(nPageInView) {
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
            this.adjustSlider(nPageInView);
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
            var yImage;

            if ($('#page-browser').children().length === 0) {
                return;
            }
            if ($('#content-browser img').hasClass('fit_width')) {
                return;
            }

            yImage = _getMaxImgHeight();

            $('#content-browser img').each(function() {
                if ($(this).hasClass(_CSS_CLASS_ROTATED)) {
                    // $(this).css({'width' : widthImg + 'px'});
                    $(this).css({
                        'height': yImage + 'px'
                    });
                } else {
                    $(this).css({
                        'height': yImage + 'px'
                    });
                }
            });
        };

        /**
         * @param nPageInView The page in the viewport.
         */
        this.setImgUrls = function(nPageInView) {
            var iPageLast,
                i1,
                i2,
                i3,
                url,
                imgCache,
                parent,
                images,
                imgWlk,
                urlArray = [],
                iPageArray = [],
                imgArray = [],
                iImgUsed = {},
                i,
                j,
                idImgBase = 'sp-browse-image-',
                prop = {},
                tmp,
                sel;

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
                for (i = 0; i < iPageArray.length; i++) {
                    iImgUsed[i] = false;
                }

                /*
                 *
                 */
                for (i = 0; i < iPageArray.length; i++) {

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
                        }
                    }

                    if (!imgCache) {
                        /*
                         * Find first image that is not used.
                         */
                        for (j = 0; j < iPageArray.length; j++) {
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
                        imgWlk.addClass(_CSS_CLASS_ROTATED);
                    } else {
                        imgWlk.removeClass(_CSS_CLASS_ROTATED);
                    }

                    urlArray[i] = url;
                    imgArray[i] = imgWlk;
                }

                /*
                 * Active
                 */
                sel = $('#' + idImgBase + 1);
                tmp = sel.attr('src');

                // Force an image reload.
                sel.attr('src', '');

                // Set heigh/width so preload spinner is in view (orginal
                // values are restored after image load).
                if (sel.hasClass('fit_width')) {
                    sel.css('height', _view.getViewportHeight() + 'px');
                } else {
                    // Just a value to hold the spinner.
                    sel.css('width', '100px');
                }

                // Set spinner and set image.
                sel.addClass(_ns.CSS_CLASS_PRELOAD_SPINNER + '-l');
                sel.attr('src', tmp).addClass('active');

                /*
                 * Re-order
                 */
                sel.insertAfter('#' + idImgBase + '0');
                $('#' + idImgBase + 2).insertAfter('#' + idImgBase + 1);
            }

            /*
             * Fill the url2img map.
             */
            _this.tnUrl2Img = {};

            for (i = 0; i < iPageArray.length; i++) {
                prop = {};
                prop.i = i;
                prop.img = imgArray[i];

                _this.tnUrl2Img[urlArray[i]] = prop;
            }

        };

        /**
         *
         */
        this.adjustSlider = function(valRequested) {
            var selSlider = $('#browser-slider'),
                sel,
                nPages,
                val,
                min;

            nPages = _model.myTotPages;
            val = nPages;

            if ($('#page-main-thumbnail-images .' + _ns.CSS_CLASS_THUMBNAIL_PNG_SELECTED).length > 0) {
                sel = $('#page-main-thumbnail-images img.' + _ns.CSS_CLASS_THUMBNAIL_PNG);
                val = _model.getPageNumber(sel.index($('#page-main-thumbnail-images img.' + _ns.CSS_CLASS_THUMBNAIL_PNG_SELECTED)));
                /*
                 * reset, so a new page in Browse mode will select the LAST
                 * image.
                 */
                sel.removeClass(_ns.CSS_CLASS_THUMBNAIL_PNG_SELECTED);
            }

            min = 1;
            if (nPages === 0) {
                min = 0;
            }

            selSlider.attr('min', min);
            selSlider.attr('max', nPages);

            if (valRequested && valRequested < nPages) {
                val = valRequested;
            }

            _setSliderValue(val);

            _view.visible(selSlider, nPages > 1);
        };

        /**
         *
         */
        this.setImages = function(nPageInView) {
            if ($('#page-browser').children().length === 0) {
                // not loaded
                return;
            }

            if (_model.myJobPages === null || _model.myJobPages.length === 0) {
                // Mantis #873
                $('#page-browser-images img').attr('src', '');
            } else {
                this.addImages(nPageInView);
            }
        };

        $('#page-browser').on('pagecreate', function(event) {

            _initHtmlCanvasEditor();

            _drawButtonPresent = $('#sp-browser-page-draw').length > 0;

            _this.setImages();

            $('.image_reel img').mousedown(function(e) {
                e.preventDefault();
            });

            $(window).resize(function() {
                _view.pages.pagebrowser.adjustImages();
                _resizeOverlayCanvas();
            });

            // Show first image
            $(".image_reel img:first").addClass("active");

            $('#browser-slider').change(function() {
                var val,
                    image,
                    zoomPrv;

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
                image.addClass('active');

                zoomPrv = _imgCanvasEditor.unZoom();
                _setOverlayCanvas();
                _imgCanvasEditor.setZoom(zoomPrv);
                _resizeOverlayCanvas();
            });

            $("#browser-nav-right").click(function() {
                _navRight();
                return false;
            });

            $("#browser-nav-left").click(function() {
                _navLeft();
                return false;
            });

            $("#browser-delete").click(function() {
                var selSlider = $('#browser-slider');
                if (_this.onClear(selSlider.val())) {
                    if (_getSliderMax(selSlider) === 1) {
                        $('#browser-back').click();
                    }
                }
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

            _ns.userEvent.pause();

        }).on("pageshow", function(event, ui) {
            // Adjust when page is settled.
            _this.adjustImages();
            _resizeOverlayCanvas();
            if (_drawPanelOpen) {
                $('#sp-canvas-tools-panel').panel("open");
            }
        }).on("pagehide", function(event, ui) {
            _ns.userEvent.resume();
            _drawPanelOpen = $('#sp-canvas-tools-panel').hasClass('ui-panel-open');
        });
    }

    /**
     *
     */
    function PageClear(_i18n, _view, _model) {
        var _this = this;

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
                var _ranges,
                    selected = $("input:radio[name='clear-pages']:checked").val();

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

                if (_this.onClear(_ranges)) {
                    $('#button-clear-pages-cancel').click();
                }
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
        _panel.jqId = '#content-accounttrx';

        /**
         *
         */
        $('#page-accounttrx').on('pagecreate', function(event) {

            /*
             * AccountTrx Panel
             */
            $(this).on('click', '#button-accounttrx-apply', null, function() {
                _panel.page(1);
                return false;
            });

            $(this).on('click', '#button-accounttrx-default', null, function() {
                _panel.applyDefaults();
                _panel.m2v();
                return false;
            });

            $(this).on('click', '.sp-btn-accounttrx-report', null, function() {
                _panel.v2m();
                _api.download("report-user", _panel.input, "AccountTrxList", $(this).attr('data-savapage'));
                return true;
            });

            $(this).on('click', '#button-goto-doclog', null, function() {
                _view.showUserPageAsync('#page-doclog', 'DocLog');
                return false;
            });

            $(this).on('click', ".sp-download-receipt", null, function() {
                _api.download("pos-receipt-download-user", null, $(this).attr('data-savapage'));
                return false;
            }).on('click', ".sp-download-invoice", null, function() {
                _api.download("pos-invoice-download-user", null, $(this).attr('data-savapage'));
                return false;
            });

        }).on("pagebeforeshow", function(event, ui) {
            /*
             * _panel.input.select.user_id is only used in a Admin WebApp
             * context. In a User WebApp context the server will use the
             * logged in user.
             */

            /*
             * Reset to defaults, since a new user might have logged on.
             */
            _panel.applyDefaults();
            _panel.refresh();
        });
    }

    /**
     *
     */
    function PageDocLog(_i18n, _view, _model, _api) {
        var
            // DocLog (common for Admin and User WebApp)
            _panel = _ns.PanelDocLogBase,
            _onInboxRestorePrintIn;

        _panel.jqId = '#content-doclog';

        _onInboxRestorePrintIn = function(docLogId, replace) {
            var res = _api.call({
                'request': 'inbox-restore-printin',
                'dto': JSON.stringify({
                    'docLogId': docLogId,
                    'replace': replace
                })
            });
            _view.showApiMsg(res);
            _view.pages.main.refreshPagesOnShow = true;
            if (replace && res.result.code === '0') {
                _model.refreshUniqueImgUrlValue4Browser();
                _view.changePage($('#page-main'));
            }
        };

        /**
         *
         */
        $('#page-doclog').on('pagecreate', function(event) {

            /*
             * DocLog Panel
             */
            $(this).on('click', '#button-doclog-apply', null, function() {
                _panel.page(1);
                return false;
            });

            $(this).on('click', '#button-doclog-default', null, function() {
                _panel.applyDefaults();
                _panel.m2v();
                return false;
            });

            $(this).on('change', "input[name='sp-doclog-select-type']", null, function() {
                _panel.setVisibility();
                return false;
            });

            $(this).on('click', '#button-goto-accounttrx', null, function() {
                _view.showUserPageAsync('#page-accounttrx', 'AccountTrx');
                return false;
            });

            $(this).on('click', '.sp-doclog-accounttrx-info', null, function() {
                var html = _view.getPageHtml('DocLogAccountTrxAddin', {
                    docLogId: $(this).attr('data-savapage')
                }) || 'error';
                $('#sp-doclog-popup-addin').html(html);
                $('#sp-doclog-popup-title').text($(this).attr('title'));
                $('#sp-doclog-popup').enhanceWithin().popup('open', {
                    positionTo: $(this),
                    arrow: 't'
                });
            }).on('click', '.sp-inbox-restore-printin-add', null, function() {
                _onInboxRestorePrintIn($(this).attr('data-savapage'), false);
                return false;
            }).on('click', '.sp-inbox-restore-printin-replace', null, function() {
                _onInboxRestorePrintIn($(this).attr('data-savapage'), true);
                return false;
            });

            _panel.onDocStoreDownloadDelete($(this), _api, _view);

        }).on("pagebeforeshow", function(event, ui) {
            /*
             * _panel.input.select.user_id is only used in a Admin WebApp
             * context. In a User WebApp context the server will use the
             * logged in user.
             */

            /*
             * Reset to defaults, since a new user might have logged on.
             */
            _panel.applyDefaults();
            _panel.refresh();
        });
    }

    /**
     *
     */
    function PagePdfProp(_i18n, _view, _model) {
        var _this = this,
            _setVisibility,
            _m2V,
            _v2M,
            _allowAll,
            _validate,
            _onJobListChange;

        /*
         *
         */
        _setVisibility = function() {
            var encrypt = _view.isCbChecked($('#pdf-encryption')),
                sign = _view.isCbChecked($('#pdf-pgp-signature'));
            if (encrypt || sign) {
                $('#pdf-apply-security').checkboxradio('enable');
                _view.visible($('#pdf-allow-block'), encrypt);
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

            $('#pdf-title').val(_model.myPdfTitle);
            $('#pdf-author').val(wlk.author);
            $('#pdf-subject').val(wlk.subject);
            $('#pdf-keywords').val(wlk.keywords);
            _view.checkCb("#pdf-encryption", (_model.propPdf.encryption.length > 0));
            _view.checkCb("#pdf-pgp-signature", _model.propPdf.pgpSignature);

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

            // When opened from SafePage Sort mode, selected page ranges are
            // filled.
            $('#pdf-page-ranges').val(_model.getSelectPageRanges());

            _setVisibility();
        };
        /*
         * View to Model
         */
        _v2M = function() {
            var wlk;

            _model.myPdfTitle = $('#pdf-title').val();

            wlk = _model.propPdf.desc;
            wlk.title = _model.myPdfTitle;
            wlk.author = $('#pdf-author').val();
            wlk.subject = $('#pdf-subject').val();
            wlk.keywords = $('#pdf-keywords').val();

            if (_view.isCbChecked($('#pdf-encryption'))) {
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

            _model.propPdf.pgpSignature = wlk.encryption && _view.isCbChecked($('#pdf-pgp-signature'));
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
            var msg = null,
                pwO = $('#pdf-pw-owner').val(),
                pwU = $('#pdf-pw-user').val();

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
        _onJobListChange = function() {
            var sel = $('#pdf-job-list :selected'),
                selTitle = $('#pdf-title');

            if (_model.pdfJobIndex === '-1') {
                _model.myInboxTitle = selTitle.val();
            }

            _model.pdfJobIndex = sel.val();

            if (sel.val() === '-1') {
                selTitle.val(_model.myInboxTitle);
            } else {
                selTitle.val(sel.text());
            }
            _model.myPrintTitle = selTitle.val();
        };

        /*
         *
         */
        $('#page-pdf-properties').on('pagecreate', function(event) {

            $('#pdf-encryption').on("change", null, null, function(event, ui) {
                _setVisibility();
            });

            $('#pdf-pgp-signature').on("change", null, null, function(event, ui) {
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

            $('#pdf-job-list').change(function(event) {
                _onJobListChange();
                return false;
            });

        }).on("pagebeforeshow", function(event, ui) {
            _m2V();
            _this.onShow();
            _onJobListChange();
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
        var _this = this;

        $("#page-voucher-redeem").on("pagecreate", function(event) {

            $('#button-voucher-redeem-ok').click(function() {
                var sel = $("#voucher-redeem-card-number");
                _this.onRedeemVoucher(sel.val());
                sel.val("");
                return false;
            });

        }).on("pagebeforeshow", function(event, ui) {
            $("#voucher-redeem-card-number").val("");
        });
    }

    /**
     *
     */
    function PageCreditTransfer(_i18n, _view, _model) {
        var _this = this,
            _selMain = '#money-credit-transfer-main',
            _selCents = '#money-credit-transfer-cents';

        $("#page-credit-transfer").on("pagecreate", function(event) {

            $('#button-transfer-credit-ok').click(function() {
                if (_this.onTransferCredit($('#money-credit-transfer-user').val(), $(_selMain).val(), $(_selCents).val(), $('#money-credit-transfer-comment').val())) {
                    // Back to main window, this will start user event
                    // monitoring (picking up the account balance change).
                    _view.changePage($('#page-main'));
                }
                return false;
            });

        }).on("pagebeforeshow", function(event, ui) {

            $('#page-credit-transfer input').val('');
            $(_selCents).val('00');
            $('#credit-transfer-available').text(_model.user.stats.accountInfo.balance);

        }).on("pageshow", function(event, ui) {
            $(_selMain).focus();
        });
    }

    /**
     *
     */
    function PageMoneyTransfer(_i18n, _view, _model) {
        var _this = this,
            _selMain = '#money-transfer-main',
            _selCents = '#money-transfer-cents';

        this.onRefreshContent = function(html) {
            var button = '#button-money-transfer';
            $(button).off('click');
            $('#page-money-transfer-content').html(html);
            $('#page-money-transfer').enhanceWithin();
            $(button).on('click', function() {
                var hidden = $('#money-transfer-gateway');
                _this.onMoneyTransfer(hidden.attr('data-payment-gateway'), hidden.attr('data-payment-method'), $(_selMain).val(), $(_selCents).val());
                return false;
            });
        };
    }

    /**
     *
     */
    function PageOutbox(_i18n, _view, _model, _api) {
        var _this = this,

            _close = function() {
                $('#button-outbox-back').click();
                return false;
            },
            _countJobs = function() {
                return $('#outbox-job-list .sp-outbox-job-entry').length;
            },
            _countJobsToCancel = function() {
                return $('#outbox-job-list .sp-outbox-cancel-job').length + $('#outbox-job-list .sp-outbox-cancel-jobticket').length;
            },
            _refresh = function() {
                var jobsToCancel,
                    html = _view.getUserPageHtml('OutboxAddin', {
                        jobTickets: false,
                        expiryAsc: false
                    });

                if (html) {
                    $('#outbox-job-list').html(html).enhanceWithin();
                    $('.sp-sparkline-printout').sparkline('html', {
                        enableTagOptions: true
                    });
                    jobsToCancel = _countJobsToCancel();
                    $('#button-outbox-clear-job-count').text(jobsToCancel);
                    _view.visible($('#button-outbox-extend'), $('#outbox-job-list .sp-outbox-item-type-hold').length > 0);
                    _view.visible($('#button-outbox-clear'), jobsToCancel > 0);
                }
                return false;
            },
            _onAccountTrxInfo = function(src, jobticket) {
                var html = _view.getPageHtml('OutboxAccountTrxAddin', {
                    jobFileName: src.attr('data-savapage'),
                    jobticket: jobticket
                }) || 'error';
                $('#sp-outbox-popup-addin').html(html);
                // remove trailing poit suffix
                $('#sp-outbox-popup-title').text(src.attr('title').replace('. . .', ''));
                $('#sp-outbox-popup').enhanceWithin().popup('open', {
                    positionTo: src,
                    arrow: 't'
                });
            };

        $('#page-outbox').on("pagecreate", function(event) {

            $('#button-outbox-clear').click(function() {
                _this.onOutboxClear();
                _refresh();
                if (_countJobs() > 0) {
                    return false;
                }
                return _close();
            });

            $('#button-outbox-refresh').click(function() {
                _refresh();
                if (_countJobs() > 0) {
                    return false;
                }
                return _close();
            });

            $('#button-outbox-extend').click(function() {
                _this.onOutboxExtend();
                return _close();
            });

            $(this).on('click', '.sp-outbox-cancel-job', null, function() {
                _this.onOutboxDeleteJob($(this).attr('data-savapage'), false);
                if (_model.user.stats.outbox.jobCount > 0) {
                    return _refresh();
                }
                return _close();
            }).on('click', '.sp-outbox-cancel-jobticket', null, function() {
                _this.onOutboxDeleteJob($(this).attr('data-savapage'), true);
                if (_model.user.stats.outbox.jobCount > 0) {
                    return _refresh();
                }
                return _close();
            }).on('click', '.sp-outbox-preview-job', null, function() {
                _api.download("pdf-outbox", null, $(this).attr('data-savapage'));
                return false;
            }).on('click', '.sp-outbox-preview-jobticket', null, function() {
                _api.download("pdf-jobticket", null, $(this).attr('data-savapage'));
                return false;
            }).on('click', '.sp-outbox-account-trx-info-job', null, function() {
                _onAccountTrxInfo($(this), false);
            }).on('click', '.sp-outbox-account-trx-info-jobticket', null, function() {
                _onAccountTrxInfo($(this), true);
            });

        }).on("pageshow", function(event, ui) {
            _refresh();
        });
    }

    /**
     *
     */
    function PageGdprExport(_i18n, _view, _model, _api) {

        $("#page-gdpr-export").on("pagecreate", function(event) {
            $('#sp-btn-gdpr-download').click(function() {
                $('#sp-btn-gdpr-back').click();
                _api.download('user-export-data-history');
                return false;
            });
        });
    }

    /**
     *
     */
    function PageSend(_i18n, _view, _model) {
        var _this = this;

        $("#page-send").on("pagecreate", function(event) {

            $('#button-send-send').click(function() {
                _this.onSend($('#send-mailto').val(), _model.pdfPageRanges, _model.removeGraphics,
                    _model.ecoprint, _model.pdfGrayscale, _model.pdfRasterize);
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
        var _page,
            _self,
            _onSelectReset;

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
     * Constructor
     */
    function PageUserInternetPrinter(_i18n, _view, _model, _api) {
        var _pageId = '#page-user-internet-printer',
            _page = new _ns.Page(_i18n, _view, _pageId, 'InternetPrinter'),
            _self = _ns.derive(_page);

        $(_pageId).on('pagecreate', function(event) {
            $(this).on('click', '#user-uuid-replace-popup-btn', null, function() {
                $('#user-uuid-replace-popup').popup('open', {
                    positionTo: $(this)
                });
                $("#user-uuid-replace-btn-no").focus();
            }).on('click', '#user-uuid-replace-btn', null, function() {
                _view.showApiMsg(_api.call({
                    'request': 'user-uuid-replace'
                }));
                $('#user-uuid-replace-popup').popup('close');
                $('#page-user-internet-printer-content').html(_view.getUserPageHtml('InternetPrinterAddIn'));
                $(_pageId).enhanceWithin();
            });
        });
        return _self;
    }

    /**
     * Constructor
     */
    function PageUserTelegram(_i18n, _view, _model, _api) {
        var _pageId = '#page-user-telegram',
            _page = new _ns.Page(_i18n, _view, _pageId, 'UserTelegram'),
            _self = _ns.derive(_page),
            _savedTelegramID;

        $(_pageId).on('pagecreate', function(event) {
            $(this).on('click', '#user-set-telegram-id-btn-apply', null, function() {
                var id = $('#user-totp-telegram-id').val(),
                    res = _api.call({
                        request: 'user-set-telegram-id',
                        dto: JSON.stringify({
                            'id': id
                        })
                    });
                if (res.result.code === '0') {
                    _savedTelegramID = id;
                }
                _view.showApiMsg(res);
            }).on('click', '#user-set-telegram-id-btn-test', null, function() {
                _view.showApiMsg(_api.call({
                    request: 'user-test-telegram-id'
                }));
            }).on('click', '#user-set-telegram-id-btn-2-step', null, function() {
                _view.pages.main.showUserTOTP();
            }).on('click', '#user-set-telegram-id-btn-back', null, function() {
                $('#user-totp-telegram-id').val(_savedTelegramID);
            });
        }).on("pagebeforeshow", function(event, ui) {
            _savedTelegramID = $('#user-totp-telegram-id').val();
        });
        return _self;
    }

    /**
     * Constructor
     */
    function PageTOTPUser(_i18n, _view, _model, _api) {
        var _pageId = '#page-user-totp',
            _page = new _ns.Page(_i18n, _view, _pageId, 'TOTPUser'),
            _self = _ns.derive(_page),
            //
            _m2v = function() {
                var isEnabled = $('#sp-user-totp-enable-chb').is(':checked'),
                    isTelegram = $('#sp-user-totp-telegram-enable-chb').is(':checked');
                _view.visible($('#sp-user-totp-qr-code'), isEnabled && !isTelegram);
                _view.visible($('#sp-user-totp-telegram-enable-span'), isEnabled);
            };

        $(_pageId).on('pagecreate', function(event) {
            $(this).on('click', '#user-totp-replace-popup-btn', null, function() {
                $('#user-totp-replace-popup').popup('open', {
                    positionTo: $(this)
                });
                $("#user-totp-replace-btn-no").focus();
            }).on('click', '#user-totp-replace-btn', null, function() {
                _view.showApiMsg(_api.call({
                    'request': 'user-totp-replace'
                }));
                $('#user-totp-replace-popup').popup('close');
                $('#page-user-totp-content').html(_view.getUserPageHtml('TOTPUserAddIn'));
                $(_pageId).enhanceWithin();
                _m2v();
            }).on('change', "input:checkbox[id='sp-user-totp-enable-chb']", null, function(e) {
                _view.showApiMsg(_api.call({
                    'request': 'user-totp-enable',
                    dto: JSON.stringify({
                        'enabled': $(this).is(':checked')
                    })
                }));
                _m2v();
            }).on('change', "input:checkbox[id='sp-user-totp-telegram-enable-chb']", null, function(e) {
                _view.showApiMsg(_api.call({
                    'request': 'user-totp-telegram-enable',
                    dto: JSON.stringify({
                        'enabled': $(this).is(':checked')
                    })
                }));
                _m2v();
            });
        });
        return _self;
    }

    /**
     *
     */
    function PageDashboard(_i18n, _view, _model, _api) {

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

            if ($('#button-user-internet-printer-dialog')) {
                $(this).on('click', '#button-user-internet-printer-dialog', null, function() {
                    var pageId = '#page-user-internet-printer',
                        html = _view.getUserPageHtml('InternetPrinterAddIn');
                    _view.showUserPage(pageId, 'InternetPrinter');
                    if (html) {
                        $('#page-user-internet-printer-content').html(html);
                        $(pageId).enhanceWithin();
                    }
                    return false;
                });
            }

            if ($('#button-user-totp-dialog')) {
                $(this).on('click', '#button-user-totp-dialog', null, function() {
                    _view.pages.main.showUserTOTP();
                    return false;
                });
            }

            if ($('#button-user-telegram-dialog')) {
                $(this).on('click', '#button-user-telegram-dialog', null, function() {
                    _view.showUserPage('#page-user-telegram', 'UserTelegram');
                    return false;
                });
            }

            if ($('#button-voucher-redeem-page')) {
                $(this).on('click', '#button-voucher-redeem-page', null, function() {
                    _view.showUserPage('#page-voucher-redeem', 'AccountVoucherRedeem');
                    return false;
                });
            }

            if ($('#button-transfer-credit-page')) {
                $(this).on('click', '#button-transfer-credit-page', null, function() {
                    _view.showUserPage('#page-credit-transfer', 'AccountCreditTransfer');
                    return false;
                });
            }

            if ($('.sp-transfer-money-img')) {
                $(this).on('click', '.sp-transfer-money-img', null, function() {
                    // The transfer page is a fixed part of
                    // WebAppUserPage.html
                    // (we only refresh the content)
                    var pageId = '#page-money-transfer',
                        data = {
                            gateway: $(this).attr('data-payment-gateway'),
                            method: $(this).attr('data-payment-method')
                        },
                        html = _view.getUserPageHtml('AccountMoneyTransfer', data);
                    _view.changePage(pageId);
                    if (html) {
                        _view.pages.moneyTransfer.onRefreshContent(html);
                    }
                    return false;
                });
            }

            if ($('#button-transfer-bitcoin-page')) {
                $(this).on('click', '#button-transfer-bitcoin-page', null, function() {
                    // The transfer page is a fixed part of
                    // WebAppUserPage.html
                    // (we only refresh the content)
                    var pageId = '#page-bitcoin-transfer',
                        html = _view.getUserPageHtml('AccountBitcoinTransfer');
                    _view.changePage(pageId);
                    if (html) {
                        $('#page-bitcoin-transfer-content').html(html);
                        $(pageId).enhanceWithin();
                    }
                    return false;
                });
            }

            $(window).resize(function() {
                var sel = _view.getSelPresent($('#dashboard-piechart')), width;
                if (sel) {
                    width = sel.parent().width();
                    sel.width(width);
                    try {
                        _model.dashboardPiechart.replot({});
                    } catch (ignore) {
                        // replot() get throw error: no code intended.
                    }
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
        var _page = new _ns.Page(_i18n, _view, '#page-file-upload'),
            _DROPZONE,
            _DROPZONE_HTML,
            _self = _ns.derive(_page),
            //
            _isPdfVisible = function() {
                return $('#button-main-pdf-properties').length > 0;
            },
            _isPrintVisible = function() {
                return $('#button-main-print').length > 0;
            },
            _showUploadButton = function(enable) {
                _view.visible($('#sp-button-file-upload-submit').parent(), enable);
            },
            _showResetButton = function(enable) {
                _view.visible($('#sp-button-file-upload-reset').parent(), enable);
            },
            _setUploadFeedbackDefault = function(dropzone) {
                var feedback = $('#sp-webprint-upload-feedback');
                feedback.removeClass('sp-txt-warn').removeClass('sp-txt-valid').addClass('sp-txt-info');
                if (dropzone) {
                    feedback.html(_DROPZONE_HTML);
                } else {
                    feedback.html('').hide();
                }
            },
            _setButtonVisibility = function(sel, visible, hasInboxDocs) {
                _view.visible(sel, visible);
                if (visible) {
                    _view.enable(sel, hasInboxDocs);
                }
            },
            _setVisibility = function() {
                var hasInboxDocs = _model.hasInboxDocs(),
                    sel;
                _setButtonVisibility($('#sp-btn-file-upload-print'), _isPrintVisible(), hasInboxDocs);
                _setButtonVisibility($('#sp-btn-file-upload-pdf'), _isPdfVisible(), hasInboxDocs);

                sel = $('#sp-file-upload-delete-button');
                _view.visible(sel, hasInboxDocs);
                if (hasInboxDocs) {
                    _view.enableUI(sel, hasInboxDocs);
                    $('#sp-file-upload-delete-button span').html(hasInboxDocs ? _model.myJobs.length : "-");
                }
            },
            _onUploadStart = function() {
                $('#sp-button-file-upload-reset').click();
                $('#sp-webprint-upload-feedback').html('&nbsp;').show();
            },
            _onUploadDone = function() {
                _view.pages.main.onRefreshPages();
                _setVisibility();
                _showUploadButton(false);
                _showResetButton(true);
            },
            _onUploadMsgWarn = function(warn, files, filesStatus) {
                $('#sp-webprint-upload-feedback').html(_ns.DropZone.getHtmlWarning(_i18n, warn, files, filesStatus)).show();
                _showUploadButton(false);
                _showResetButton(true);
                $('#sp-webprint-upload-file').val('');
            },
            _onUploadMsgInfo = function(files) {
                var i,
                    fileSize,
                    html = '';
                for (i = 0; i < files.length; i++) {
                    fileSize = _ns.DropZone.humanFileSize(files[i].size);
                    html += '&bull; ' + files[i].name;
                    if (fileSize) {
                        html += ' (' + fileSize + ')';
                    }
                    html += '<br>';
                }
                html += '&bull; ' + _i18n.format('msg-file-upload-completed', null);
                $('#sp-webprint-upload-feedback').addClass('sp-txt-valid').html(html);
                _showResetButton(true);
            };

        $(_self.id()).on('pagecreate', function(event) {
            var zone,
                html,
                selAddIn = $('#file-upload-addin');

            if (selAddIn.children().length === 0) {
                html = _view.getUserPageHtml('FileUploadAddIn');
                $('#file-upload-title').html(_i18n.format('file-upload-title'));
                $('#file-upload-txt-font-family').html(_i18n.format('file-upload-txt-font-family'));
                //
                selAddIn.html(html).listview('refresh');

                $('#sp-button-file-upload-reset').attr('value', _i18n.format('button-reset')).button('refresh');
                $('#sp-button-file-upload-submit').attr('value', _i18n.format('button-upload')).button('refresh');
            }

            _DROPZONE = _model.webPrintDropZoneEnabled && _ns.DropZone.isSupported();
            _DROPZONE_HTML = _i18n.format('file-upload-dropzone-prompt-dialog');

            _showUploadButton(false);
            _showResetButton(false);

            _setUploadFeedbackDefault(_DROPZONE);

            if (_DROPZONE) {
                zone = $('#sp-webprint-upload-feedback');
                zone.addClass('sp-dropzone-small');

                _ns.DropZone.setCallbacks(zone, 'sp-dropzone-hover-small'
                    //
                    , _model.webPrintUploadUrl, _model.webPrintUploadFileParm
                    //
                    , _model.webPrintUploadFontParm
                    //
                    , function() {
                        return $('#file-upload-fontfamily').val();
                    }
                    //
                    , _model.webPrintMaxBytes, _model.webPrintFileExt, _i18n
                    //
                    , function() {// before send
                        _onUploadStart();
                    }, function() {// after send
                        _onUploadDone();
                    }, function(warn, infoArray, filesStatus) {
                        _onUploadMsgWarn(warn, infoArray, filesStatus);
                    }, function(files) {
                        _onUploadMsgInfo(files);
                    }, true);
            }

            $('#sp-webprint-upload-form').submit(function(e) {
                var files = document.getElementById('sp-webprint-upload-file').files;

                e.preventDefault();

                _ns.DropZone.sendFiles(files,
                    //
                    _model.webPrintUploadUrl, _model.webPrintUploadFileParm
                    //
                    , _model.webPrintUploadFontParm, $('#file-upload-fontfamily').val()
                    //
                    , _model.webPrintMaxBytes, _model.webPrintFileExt, _i18n
                    //
                    , function() {// before send
                        _onUploadStart();
                    }, function() {// after send
                        _onUploadDone();
                    }, function(warn) {
                        _onUploadMsgWarn(warn);
                    }, function(files) {
                        _onUploadMsgInfo(files);
                    });

                return false;
            });

            $(this).on('input', '#sp-webprint-upload-file', function() {
                var show = $(this).val();

                if (_ns.logger.isDebugEnabled()) {
                    (function logFiles(show, files) {
                        var file, i, msg = '';
                        for (i = 0; i < files.length; i++) {
                            file = files[i];
                            msg += file.name + ' (' + file.size + ') | ';
                        }
                        _ns.logger.debug('onInput #sp-webprint-upload-file: show = '
                            + (show ? 'true' : 'false') + ' | ' + msg);
                    })(show, document.getElementById('sp-webprint-upload-file').files);
                }

                _showUploadButton(show);
                _showResetButton(show);
                _setUploadFeedbackDefault(_DROPZONE);
            });

            $('#sp-button-file-upload-reset').click(function() {
                _setUploadFeedbackDefault(_DROPZONE);
                _showUploadButton(false);
                _showResetButton(false);
                return true;
            });

            $('#sp-file-upload-delete-button').click(function() {
                _self.onClear();
                _setVisibility();
                return true;
            });

            $('#sp-btn-file-upload-print').click(function() {
                $('#button-main-print').click();
                return true;
            });
            $('#sp-btn-file-upload-pdf').click(function() {
                _view.pages.main.onShowPdfDialog();
                return true;
            });
            $('#sp-btn-file-upload-inbox').click(function() {
                _view.changePage($('#page-main'));
                return true;
            });

        }).on("pagebeforeshow", function(event, ui) {

            _ns.deferAppWakeUp(true);

            _setVisibility();

        }).on('pagebeforehide', function(event, ui) {
            /*
             * Clear and Hide content
             */
            $('#sp-button-file-upload-reset').click();

            /*
            * IMPORTANT: _ns.deferAppWakeUp(false) is performed in
            * main.onShow()
            */

            // Mantis #717
            _ns.checkAppWakeUpAutoRestore();
        });
        return _self;
    }

    // =========================================================================
    /**
     * Constructor
     */
    function PageMain(_i18n, _view, _model, _api) {
        var _this = this,
            _util = _ns.Utils,
            _IMG_PADDING = 3,
            _IMG_BORDER = 0,
            _isEditMode = false,
            _totImages = 0,
            _mousedownPosLeft,
            _mousedownPageX,
            _mousedownTarget,
            _isThumbnailDragged = false,
            _moveLeft,
            _moveRight,
            _moveToBegin,
            _moveToEnd,
            _moveJobs,
            _showArrange,
            _showCutPageRanges,
            _showSelectPageRanges,
            _showOverlayPageRanges,
            _getFirstJob,
            _showArrButtons,
            _onThumbnailTap,
            _onThumbnailTapHold,
            _onPageInfoTap,
            // mapping page URLs to jQuery <img> selectors
            _tnUrl2Img,
            _IMG_WIDTH = function() {
                return _model.MY_THUMBNAIL_WIDTH;
            },

            /**
             * Set job expiration marker in thumbnail subscript.
             */
            _setThumbnailExpiry = function() {
                var subscripts = $('.' + _ns.CSS_CLASS_THUMBNAIL_SUBSCRIPT),
                    i = 0;
                $.each(_model.myJobPages, function(key, page) {
                    if (page.expiryTime > 0 && page.expiryTime - _model.prevMsgTime < page.expiryTimeSignal) {
                        subscripts.eq(i).addClass('sp-thumbnail-subscript-job-expired').addClass('ui-btn-icon-left').addClass('ui-icon-mini-expired-clock');
                    }
                    i = i + 1;
                });
            },
            //
            _quickMailTicketSearch,
            //
            _onQuickMailTicketSearchProps = function(props) {
                props.userId = _model.user.doclog.key_id;
                props.docStore = true;
            },
            _onQuickMailTicketSearchItemDisplay = function(item) {
                var html = '',
                    btnClass = 'sp-quick-search-mailticket-item-btn ' +
                        'ui-btn ui-input-btn ui-corner-all ui-shadow ' +
                        'ui-btn-inline ui-btn-icon-notext',
                    clazz = item.docStore ? 'sp-txt-valid' : 'sp-txt-warn';

                html += '<button class="ui-icon-plus ' + btnClass + '" />';
                html += '<button class="sp-quick-search-mailticket-item-btn-replace ui-icon-refresh ' + btnClass + '" />';

                html += '<span class="sp-txt-wrap ' + clazz + ' sp-font-monospace">'
                    + item.text + '</span><span class="sp-txt-info"> &bull; ' + item.email;
                html += '</span>';
                html += '<div style="font-weight: normal;" class="sp-txt-wrap sp-txt-info">';
                if (item.printOutJobs) {
                    html += '&nbsp;<img src="/images/printer-26x26.png" height="12"/>&nbsp;'
                        + item.printOutJobs
                        + '&nbsp;&bull;&nbsp;';
                }
                html += item.title + ' &bull; ' + item.paperSize + ' &bull; ' +
                    item.pages + ' &bull; ' + item.byteCount + '</div>';

                return html;
            },
            _onQuickMailTicketSearchSelect = function(quickSelected, event) {
                var res, replace = $(event.target).hasClass('sp-quick-search-mailticket-item-btn-replace');
                if (replace) {
                    _model.refreshUniqueImgUrlValues();
                }
                res = _api.call({
                    'request': 'inbox-restore-printin',
                    'dto': JSON.stringify({
                        'docLogId': quickSelected.key,
                        'replace': replace
                    })
                });
                // click the clear button.
                $('#sp-quick-search-mailticket').next('a').click();
                _view.showApiMsg(res);
                if (res.result.code === '0') {
                    _this.onExpandPage(0);
                }
            },
            _onQuickMailTicketSearchClear = function() {
                $.noop();
            };

        // for now ...
        this.id = '#page-main';

        //
        this.refreshPagesOnShow = false;

        /**
         * Clears all traces of previous editing.
         */
        this.clearEditState = function() {
            _showArrange(_isEditMode);
        };

        /**
         *
         */
        this.showUserTOTP = function() {
            var pageId = '#page-user-totp',
                html = _view.getUserPageHtml('TOTPUserAddIn');
            _view.showUserPage(pageId, 'TOTPUser');
            if (html) {
                $('#page-user-totp-content').html(html);
                $(pageId).enhanceWithin();
            }
        };

        /**
         *
         */
        this.showUserStats = function() {
            var stats = _model.user.stats,
                outbox, pages = 0,
                accInfo, status, selBalance;

            _setThumbnailExpiry();

            if (stats) {

                $('#sp-sparkline-user-pie').sparkline([stats.pagesPrintOut, stats.pagesPrintIn, stats.pagesPdfOut], {
                    enableTagOptions: true
                });

                selBalance = $('#mini-user-balance');
                accInfo = stats.accountInfo;
                if (selBalance && accInfo) {
                    status = accInfo.status;
                    selBalance.html(accInfo.balance).attr("class", status === "CREDIT" ? "sp-txt-warn" : (status === "DEBIT" ? "sp-txt-valid" : "sp-txt-error"));
                }

                selBalance = $('#mini-user-balance-papercut');
                accInfo = stats.accountInfoPaperCut;
                if (selBalance && accInfo) {
                    status = accInfo.status;
                    selBalance.html(accInfo.balance).attr("class", status === "CREDIT" ? "sp-txt-warn" : (status === "DEBIT" ? "sp-txt-valid" : "sp-txt-error"));
                }

                outbox = _model.user.stats.outbox;
            }

            if (outbox) {

                $.each(outbox.jobs, function(key, job) {
                    pages += job.copies * job.pages;
                });

                _view.visible($('#button-mini-outbox'), outbox.jobCount > 0);
                _view.enable($('#button-main-outbox'), outbox.jobCount > 0);

                $('#button-mini-outbox .sp-outbox-remaining-time').html(outbox.localeInfo.remainTime);
                $('#button-mini-outbox .sp-outbox-jobs').html(outbox.jobCount);
                $('#button-mini-outbox .sp-outbox-pages').html(pages);
                $('#button-mini-outbox .sp-outbox-cost').html(outbox.localeInfo.cost);
            }

            _model.showJobsMatchMediaSources(_view);
        };

        /**
         * Initializes the nUrl2Img map with the pages from the _model.
         */
        _tnUrl2Img = function() {
            var i = 0;

            _this.thumbnailImagesImgPNG().each(function() {
                var prop = {};
                prop.i = i;
                prop.img = $(this);
                _this.tnUrl2Img[_model.myJobPages[i].url] = prop;
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

        // Initialize with URL parameter.
        this.initialView = _ns.Utils.getUrlParam(_ns.URL_PARM.SHOW);

        // One-time initial view.
        this.setInitialView = function() {
            if (_this.initialView) {
                if (_model.myJobPages.length > 0) {
                    if (_this.initialView === _ns.URL_PARM_SHOW_PDF) {
                        _ns.Utils.asyncFoo(function() {
                            $('#button-main-pdf-properties').click();
                        });
                    } else if (_this.initialView === _ns.URL_PARM_SHOW_PRINT) {
                        _ns.Utils.asyncFoo(function() {
                            $('#button-main-print').click();
                        });
                    }
                }
                _this.initialView = null;
            }
        };

        /**
         *
         */
        this.setThumbnails = function() {
            var wlkPageNr = 1,
                divPrv,
                imgContainer = $('#page-main-thumbnail-images'),
                isDrawActive = _view.pages.pagebrowser.isDrawActive();

            if (!_this.tnUrl2Img) {
                _this.tnUrl2Img = {};
            }

            /*
             * HTML INJECT RUN: Iterate the incoming pages and check if
             * <img> for url is already there.
             */
            $.each(_model.myJobPages, function(key, page) {
                var divCur,
                    item,
                    tnUrl,
                    span,
                    title,
                    imgWidth = _IMG_WIDTH(),
                    imgHeightA4 = imgWidth * 1.4,
                    displayOverlay = isDrawActive && page.overlay;

                tnUrl = _this.tnUrl2Img[page.url];

                if (tnUrl) {
                    /*
                     * Yes, <img> for url is already there: move to current
                     * position.
                     */
                    divCur = tnUrl.img.parent();
                } else {
                    title = page.media;
                    if (page.expiryTime) {
                        title += ' &bull; ' + _ns.Utils.formatDateTime(new Date(page.expiryTime));
                    }
                    /*
                     * Mantis #320: we set the 'height' attribute with the
                     * A4 assumption, later on at the removeImgHeight() the
                     * height is removed so the image will show with the
                     * fixed width and the proper height (ratio).
                     */
                    item = "";
                    item += '<div>';

                    item += '<img class="' + _ns.CSS_CLASS_THUMBNAIL_PNG + ' ' + _ns.CSS_CLASS_PRELOAD_SPINNER + '" onload="org.savapage.removeImgHeight(this)" ';
                    item += 'width="' + imgWidth + '" height="' + imgHeightA4 + '" ';
                    item += 'border="0" src="' + _view.getImgSrc(page.url) + '" ';
                    item += 'style="';
                    item += 'padding: ' + _IMG_PADDING + 'px;';
                    item += '"';
                    item += '/>';

                    if (displayOverlay) {
                        item += '<img class="' + _ns.CSS_CLASS_THUMBNAIL_SVG + '" ';
                        item += 'style="';
                        item += 'margin-left: -' + (imgWidth + 2 * _IMG_PADDING) + 'px; ';
                        item += 'z-index:999;';
                        item += 'padding: ' + _IMG_PADDING + 'px;';
                        item += '" ';
                        item += 'border="0" src="data:image/svg+xml;base64,' + page.overlaySVG64 + '" ';
                        item += 'width="' + imgWidth + '"/>';
                    }

                    item += '<a tabindex="-1" title="' + title + '" href="#" class="' + _ns.CSS_CLASS_THUMBNAIL_SUBSCRIPT + ' ui-btn ui-mini" style="margin-top:-' + (2 * _IMG_PADDING + 1) + 'px; margin-right: ' + _IMG_PADDING + 'px; margin-left: ' + _IMG_PADDING + 'px; border: none; box-shadow: none;">';
                    item += '<span class="sp-thumbnail-page"/>';
                    item += '<span class="sp-thumbnail-tot-pages"/>';
                    item += '<span class="sp-thumbnail-tot-chunk"/>';

                    if (displayOverlay) {
                        item += '<span class="sp-txt-info"> &#9998;</span>';
                    }

                    item += '<span class="sp-txt-info sp-thumbnail-tot-chunk-overlays"/>';

                    if (_model.myJobs[page.job].rotate !== "0") {
                        item += " &#x21b7;";
                    }

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
                /*
                 * Set the chunk overlay total.
                 */
                span = divCur.find('.sp-thumbnail-tot-chunk-overlays');
                if (isDrawActive && ((page.overlayPages > 0 && !page.overlay) || page.overlayPages > 1)) {
                    if (page.overlay) {
                        span.html(' ' + page.overlayPages);
                    } else {
                        span.html(' ' + page.overlayPages + '&#9998;');
                    }
                } else {
                    span.html('');
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

            _showOverlayPageRanges();

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

            _setThumbnailExpiry();

            //
            _tnUrl2Img();

            //
            _this.setInitialView();
        };

        /*
         * Thumbnail viewport
         */
        _moveLeft = function() {
            var right = $('.thumbnail_reel').position().left;
            if (right > (_totImages - 1) * -_IMG_WIDTH()) {
                $('.thumbnail_reel').animate({
                    'right': right,
                    'left': (right - _IMG_WIDTH())
                });
            }
        };
        _moveRight = function() {
            var right = $('.thumbnail_reel').position().left;
            if (right <= 0) {
                $('.thumbnail_reel').animate({
                    'right': right,
                    'left': (right + _IMG_WIDTH())
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
                 * IMPORTANT: offset() does NOT work in Google Chrome
                 * therefore
                 * we use css().
                 */
                $('.thumbnail_reel').css({
                    'left': $('#page-main-thumbnail-viewport').offset().left + $('#thumbnail_nav_l').outerWidth() + 'px'
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
                    'left': (widthViewport - $('#page-main-thumbnail-images').outerWidth() - $('#thumbnail_nav_r').outerWidth() + 'px')
                });
            }
        };

        /*
         *
         */
        _showArrButtons = function() {
            var selEdit = $('.main_arr_edit li :first-child'),
                selEditRow2 = $('#main-navbar-arrange-row-2 li').filter('.main_arr_edit').find(':first-child'),
                selPaste = $('.main_arr_paste :first-child'),
                selUndo = $('#main-arr-undo'),
                bCut = _util.countProp(_model.myCutPages) > 0;

            // Show Paste buttons?
            if (_util.countProp(_model.mySelectPages) > 0) {
                _view.enableUI(selEdit, true);
                _view.enableUI(selEditRow2, true);
                _view.enableUI(selPaste, bCut);
            } else {
                _view.enableUI(selEdit, false);
                _view.enableUI(selEditRow2, false);
            }
            // Show Undo button?
            _view.enableUI(selUndo, bCut);
        };

        /**
         *
         */
        _showArrange = function(bShow) {
            $('#page-main-thumbnail-images div').removeClass('sp-thumbnail-selected').removeClass('sp-thumbnail-cut');
            _model.myCutPages = {};
            _model.mySelectPages = {};
            _showCutPageRanges();
            _showSelectPageRanges();
            _showArrButtons();

            _view.visible($('.main_action_arrange'), bShow);
            _view.visible($('.main_action'), !bShow);

            _isEditMode = bShow;
        };

        /*
         *
         */
        _getFirstJob = function(selClass) {
            var tn = $('#page-main-thumbnail-images div'),
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
            var ranges = _model.getCutPageRanges(),
                position = _model.getPageNumber(_getFirstJob('sp-thumbnail-selected')) - 1;

            if (ranges !== null && position !== null) {
                if (!bBefore) {
                    position += 1;
                }
                _this.onPageMove(ranges, position);
                _showArrButtons();
            }
        };

        /**
         *
         */
        _showCutPageRanges = function() {
            if (_util.countProp(_model.myCutPages) > 0) {
                $('#main-page-range-cut').html(_model.getCutPageRanges());
                $('#button-mini-cut').show();
            } else {
                $('#button-mini-cut').hide();
            }
        };

        /**
         *
         */
        _showSelectPageRanges = function() {
            if (_util.countProp(_model.mySelectPages) > 0) {
                $('#main-page-range-select').html(_model.getSelectPageRanges());
                $('#button-mini-select').show();
            } else {
                $('#button-mini-select').hide();
            }
        };
        /**
         *
         */
        _showOverlayPageRanges = function() {
            var pages = 0;
            if (_view.pages.pagebrowser.isDrawActive()) {
                $.each(_model.myJobPages, function(key, page) {
                    pages += page.overlayPages;
                });
            }
            if (pages > 0) {
                $('#main-page-count-overlays').html(pages);
                $('#button-mini-overlays').show();
            } else {
                $('#button-mini-overlays').hide();
            }
        };

        this.alignThumbnails = function() {
            _moveToEnd();
        };

        /**
         * Adjusts the visibility (width, padding, hide/show) of the
         * thumbnail images, and move the last image to the right side
         * (moveToEnd)
         */
        this.adjustThumbnailVisibility = function() {
            var widthTot = 0,
                tn,
                selMainAct = $('.main_actions button'),
                selPdfButton,
                widthImg;

            _totImages = 0;

            $(".thumbnail_reel img." + _ns.CSS_CLASS_THUMBNAIL_PNG).each(function(index, image) {
                _totImages += 1;
                $(image).css({
                    'width': _IMG_WIDTH() + 'px',
                    'padding': _IMG_PADDING + 'px'
                });
            });

            widthImg = (_IMG_WIDTH() + 2 * _IMG_PADDING + 2 * _IMG_BORDER);
            widthTot = widthImg * _totImages;

            $('.thumbnail_reel').css({
                'width': +widthTot + 'px'
            });

            _moveToEnd();

            _view.enableUI(selMainAct, _totImages !== 0);

            if (_totImages !== 0) {
                _view.enableUI($('#button-main-pdf-properties'), !_model.myJobsDrm);
            }

            /*
             * Set the CSS class for cut/selected thumbnails.
             */
            tn = $('#page-main-thumbnail-images div');

            $.each(tn, function(key, obj) {
                var n = _model.getPageNumber(tn.index($(this)));

                if (_model.myCutPages[n] === true) {
                    $(this).addClass('sp-thumbnail-cut');
                } else {
                    $(this).removeClass('sp-thumbnail-cut');
                }

                if (_model.mySelectPages[n] === true) {
                    $(this).addClass('sp-thumbnail-selected');
                } else {
                    $(this).removeClass('sp-thumbnail-selected');
                }

            });

            _showCutPageRanges();
            _showSelectPageRanges();
            _showOverlayPageRanges();
            _showArrButtons();
        };

        this.thumbnailImagesImgPNG = function() {
            return $('#page-main-thumbnail-images img.' + _ns.CSS_CLASS_THUMBNAIL_PNG);
        };
        /*
         *
         */
        _onPageInfoTap = function(sel) {
            var iImg = -1,
                page,
                job,
                sel2,
                nDel,
                html,
                cssCls = _ns.CSS_CLASS_THUMBNAIL_SUBSCRIPT;
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
                nDel = job.pages - job.pagesSelected;

                _model.iPopUpJob = page.job;

                $('#popup-job-info-name').html(job.title);
                $('#popup-job-info-pages').html(job.pages);

                $('#popup-job-info-pages-deleted').find('span:first').html(nDel);
                _view.visible($('#popup-job-info-pages-deleted'), nDel > 0);

                $('#popup-job-info-media').html(job.media);
                $('#popup-job-info-drm').html(job.drm ? '&nbsp;DRM' : '');

                _view.checkCb("#sp-popup-job-undelete", false);
                _view.checkCb("#sp-popup-job-rotate", (job.rotate !== '0'));

                sel2 = $('#sp-popup-job-expiry');

                if (page.expiryTime) {
                    if (page.expiryTime > 0 && page.expiryTime - _model.prevMsgTime < page.expiryTimeSignal) {
                        sel2.addClass('sp-thumbnail-subscript-job-expired');
                    } else {
                        sel2.removeClass('sp-thumbnail-subscript-job-expired');
                    }
                    sel2.find('span').html(_ns.Utils.formatDateTime(new Date(page.expiryTime)));
                }
                _view.visible(sel2, page.expiryTime);

                //
                html = _view.getUserPageHtml('PdfDocumentFontsAddIn', {
                    ijob: _model.iPopUpJob
                }) || 'error';
                $('#sp-popup-job-info-fonts-addin').html(html);

                $('#sp-popup-job-info').enhanceWithin().popup('open', {
                    positionTo: '#page-main-thumbnail-images .sp-thumbnail-page:eq(' + iImg + ')'
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
            var tn,
                nPage,
                selImage = _this.thumbnailImagesImgPNG(),
                iImage = selImage.index(thumbnail);

            if (iImage < 0) {
                return;
            }

            nPage = _model.getPageNumber(iImage);

            selImage.removeClass(_ns.CSS_CLASS_THUMBNAIL_PNG_SELECTED);

            if (_model.myJobPages[iImage].pages === 1) {
                /*
                 * Image representing a single page
                 */
                if (_isEditMode) {

                    tn = thumbnail.parent();

                    if (tn.hasClass('sp-thumbnail-cut')) {

                        tn.removeClass('sp-thumbnail-cut');
                        delete _model.myCutPages[nPage];
                        _showCutPageRanges();

                    } else {
                        tn.toggleClass('sp-thumbnail-selected');
                        if (tn.hasClass('sp-thumbnail-selected')) {
                            _model.mySelectPages[nPage] = true;
                        } else {
                            delete _model.mySelectPages[nPage];
                        }
                        _showSelectPageRanges();
                    }
                    _showArrButtons();
                } else {
                    thumbnail.addClass(_ns.CSS_CLASS_THUMBNAIL_PNG_SELECTED);
                    _view.changePage($('#page-browser'));
                }
            } else {
                /*
                 * Image representing multiple pages
                 */
                _this.onExpandPage(nPage);
            }
        };
        /**
         *
         */
        $('#page-main').on('pagecreate', function(event) {
            var taphold = false,
                widthImg = (_IMG_WIDTH() + 2 * _IMG_PADDING + 2 * _IMG_BORDER),
                // Use the ratio of ISO A4 + extra padding
                maxHeight = widthImg * (297 / 210) + 8 * _IMG_PADDING + 2 * _IMG_BORDER;

            if (_model.webPrintEnabled && _model.webPrintDropZoneEnabled && _ns.DropZone.isSupported()) {
                _ns.DropZone.setCallbacks($('#page-main-thumbnail-viewport'), 'sp-dropzone-hover'
                    //
                    , _model.webPrintUploadUrl, _model.webPrintUploadFileParm
                    //
                    , _model.webPrintUploadFontParm
                    //
                    , function() {
                        return $('#file-upload-fontfamily').val();
                    }
                    //
                    , _model.webPrintMaxBytes, _model.webPrintFileExt, _i18n
                    //
                    , function() {
                        _ns.userEvent.pause();
                    }, function() {
                        _ns.userEvent.resume();
                    }, function(warn, files, filesStatus) {
                        _view.msgDialogBox(_ns.DropZone.getHtmlWarning(_i18n, warn, files, filesStatus), 'sp-msg-popup-warn');
                    }, null, true);
            }

            $('#page-main-thumbnail-viewport').css({
                'height': +maxHeight + 'px'
            });

            _showArrange(false);

            /*
             * Hide text in buttons, show text as title instead, when desktop
             * browser.
             * See: OnOffEnum (Java)
             */
            if (_model.showNavButtonTxt === 'OFF' || (_model.showNavButtonTxt === 'AUTO' && !_ns.Utils.isMobileOrTablet())) {
                $(".sp-nav-button-txt").hide();
                $.each($(".sp-nav-button-txt"), function(key, obj) {
                    $(this).hide();
                    $(this).closest("li").attr("title", $(this).text());
                });
            }

            // Now that the images are loaded (needed for iOS Safari), hide
            // them.
            $(".sp-main-status-ind").hide();

            //
            $('#thumbnail_nav_r').css('right', $('#page-main-thumbnail-viewport').innerWidth() + 'px');

            // Thumbnail viewport
            $('.thumbnail_reel img').mousedown(function(e) {
                e.preventDefault();
            });

            $('.sp-btn-about').click(function() {
                _view.showPageAsync('#page-info', 'AppAbout');
                return false;
            });

            $('#button-mini-upload').click(function() {
                var pageId = '#page-file-upload';
                /*
                 * This page is a fixed part of WebAppUserPage.html
                 */
                _view.changePage(pageId);
                return false;
            });

            $('.sp-btn-show-outbox').click(function() {
                _view.showUserPage('#page-outbox', 'Outbox');
                return false;
            });

            $('#button-main-help').click(function() {
                // shift focus from this button...
                $('#page-main').focus();
                // ... and open help page in new tab.
                window.open($('#sp-button-mini-help').attr('href'), '_blank').focus();
            });

            $('.sp-button-user-details').click(function() {
                var html, xydata, piedata,
                    pageId = '#page-dashboard';

                _view.showUserPage(pageId, 'UserDashboard');

                html = _view.getUserPageHtml('UserDashboardAddIn');

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
                    if (_view.getSelPresent($('#dashboard-piechart'))) {
                        xydata = _view.jqPlotData('dashboard-xychart', false);
                        piedata = _view.jqPlotData('dashboard-piechart', false);
                    }

                    if (!xydata || !piedata) {
                        return;
                    }
                    if (_model.dashboardPiechart) {
                        /*
                         * IMPORTANT: Release all resources occupied by the
                         * jqPlot. NOT releasing introduces a HUGE memory
                         * leak,
                         * each time the plot is refreshed.
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
                _model.determineCopyJobTicket();
                _model.myShowPrinterInd = true;
                if (_model.hasMultiplePrinters) {
                    _this.onShowPrintDialog();
                } else {
                    if (_model.isCopyJobTicket) {
                        _view.checkRadioValue('sp-print-jobticket-type', _model.TICKETTYPE_COPY);
                    }
                    _model.setPrintPreviewLandscapeHint(0);
                    _view.showUserPageAsync('#page-printer-settings', 'PrinterSettings');
                }
                return false;
            });

            $('#button-main-pdf-properties').click(function() {
                _this.onShowPdfDialog();
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
                _view.pages.pagebrowser.adjustSlider(1);
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
                if (_mousedownTarget.attr('src') && _mousedownTarget.hasClass(_ns.CSS_CLASS_THUMBNAIL_SVG)) {
                    _mousedownTarget = $(event.target).siblings('.' + _ns.CSS_CLASS_THUMBNAIL_PNG);
                }

            }).on('vmousemove', null, null, function(event) {
                if (_model.myIsDragging) {

                    // needed for IE8
                    event.preventDefault();

                    // -------------------------------------------------------
                    // css() seems to be much faster than offset().
                    // -------------------------------------------------------
                    $(this).css({
                        'left': _mousedownPosLeft - (_mousedownPageX - event.pageX) + 'px'
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
            // Actions when arranging SafePages.
            // ----------------------------------------------------------------------
            $('#main-arr-action-pdf').click(function() {
                _this.onShowPdfDialog();
                return false;
            });

            $('#main-arr-action-print').click(function() {
                _this.onShowPrintDialog();
                return false;
            });

            $('#main-arr-unselect-all').click(function() {
                $('#page-main-thumbnail-images div').removeClass('sp-thumbnail-selected');
                _model.mySelectPages = {};
                _showSelectPageRanges();
                _showArrButtons();
                return false;
            });

            $('#main-arr-undo').click(function() {
                $('#page-main-thumbnail-images div').removeClass('sp-thumbnail-cut');
                _model.myCutPages = {};
                _showCutPageRanges();
                _showArrButtons();
                return false;
            });

            /*
             *
             */
            $('#main-arr-cut').click(function() {
                var page;

                $('.sp-thumbnail-selected').addClass('sp-thumbnail-cut').removeClass('sp-thumbnail-selected');

                for (page in _model.mySelectPages) {
                    if (_model.mySelectPages.hasOwnProperty(page)) {
                        _model.myCutPages[page] = true;
                    }
                }
                _model.mySelectPages = {};

                _showCutPageRanges();
                _showSelectPageRanges();

                _showArrButtons();

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
                $('#page-main-thumbnail-images div').removeClass('sp-thumbnail-selected');
                var ranges = _model.getSelectPageRanges();
                if (ranges.length > 0) {
                    _this.onPageDelete(ranges);
                    _model.mySelectPages = {};
                    //Perform next steps when this event is done.
                    window.setTimeout(function() {
                        _showSelectPageRanges();
                        _showArrButtons();
                    }, 10);
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

            $('#button-print-delegation-main').click(function() {
                _view.showPageAsync('#page-print-delegation', 'PagePrintDelegation');
                return false;
            });

            if ($('#sp-quick-search-mailticket-filter').length) {

                _quickMailTicketSearch = new _ns.QuickObjectSearch(_view, _api);

                _quickMailTicketSearch.onCreate($(this), 'sp-quick-search-mailticket-filter'
                    , 'mailticket-quick-search', _onQuickMailTicketSearchProps
                    , _onQuickMailTicketSearchItemDisplay
                    , _onQuickMailTicketSearchSelect, _onQuickMailTicketSearchClear
                    , null, '.sp-quick-search-mailticket-item-btn');
            }
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

            // Show initial user view.
            if (_this.initialView === _ns.URL_PARM_SHOW_USER) {
                var sel = _view.getSelPresent($('#mini-user-balance-papercut'))
                    || _view.getSelPresent($('#mini-user-balance'));
                sel.click();
                _this.initialView = null;
            }

        }).on('pagebeforehide', function(event, ui) {
            _this.onHide();
        });
    }

    /**
     *
     */
    function PagePrintSettings(_i18n, _view, _model) {
        var _this = this,
            PRINT_OPT_PFX = 'print-opt-',
            PRINT_OPT_DIV_SFX = '-div',
            CUSTOM_HTML5_DATA_ATTR = 'data-savapage',
            CUSTOM_HTML5_DATA_ATTR_ICON = 'data-savapage-icon',
            CSS_CLASS_SELECT_IPP_OPTION = '_sp_select_ipp_option',
            CSS_CLASS_IPP_ICON = 'sp-ipp-icon',
            //
            _getPrinterOptionId = function(ippKeyword) {

                var sel,
                    i = 0;

                $.each(_model.myPrinterOpt, function(key, value) {
                    if (key === ippKeyword) {
                        sel = PRINT_OPT_PFX + i;
                        return;
                    }
                    i += 1;
                });

                return sel;
            },
            _isMediaSourceAutoSelected = function() {
                return $("select[data-savapage='media-source']").val() === 'auto';
            },
            _isPageRotate180Selected = function() {
                return $("select[data-savapage='org.savapage.int-page-rotate180']").val() === '1';
            },
            _showNumberUpPreview = function() {
                var selLandscape = $('#cb-nup-preview-landscape');
                if (selLandscape.length > 0) {
                    _ns.NumberUpPreview.show(_view, $("select[data-savapage='number-up']").val(), _isPageRotate180Selected(), $("select[data-savapage='org.savapage-finishings-punch']").val(), $("select[data-savapage='org.savapage-finishings-staple']").val(), _view.isCbChecked(selLandscape));
                }
            },
            _applyLandscapeHint = function() {
                var sel = $('#cb-nup-preview-landscape');
                if (sel.length > 0) {
                    _view.checkCbSel(sel, _model.printPreviewLandscapeHint);
                }
            },
            _probeChangePageRotate180 = function(target) {
                if (target.attr(CUSTOM_HTML5_DATA_ATTR) !== 'org.savapage.int-page-rotate180') {
                    return;
                }
                _showNumberUpPreview();
            },
            _probeChangeNumberUp = function(target) {
                if (target.attr(CUSTOM_HTML5_DATA_ATTR) !== 'number-up') {
                    return;
                }
                _showNumberUpPreview();
            },
            _probeChangeStaple = function(target) {
                if (target.attr(CUSTOM_HTML5_DATA_ATTR) !== 'org.savapage-finishings-staple') {
                    return;
                }
                _showNumberUpPreview();
            },
            _probeChangePunch = function(target) {
                if (target.attr(CUSTOM_HTML5_DATA_ATTR) !== 'org.savapage-finishings-punch') {
                    return;
                }
                _showNumberUpPreview();
            },
            /*
             * @param target The target media-source select selector.
             */
            _onChangeMediaSource = function(target) {
                var isAuto,
                    isManual,
                    mediaOptId,
                    ippOption = target.attr(CUSTOM_HTML5_DATA_ATTR),
                    singleMediaSourceMedia,
                    singleJobMedia,
                    isSingleMediaMatch,
                    isInboxDocs2Print = _model.hasInboxDocs() && !_model.isCopyJobTicket,
                    isMediaClash,
                    showScaling;

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
                    _model.selectedMediaSourceUI = isAuto ? '&nbsp;-&nbsp;' : target.find(":selected").text();

                } else if (ippOption === 'media') {
                    _model.printSelectedMedia = target.val();
                }

                singleMediaSourceMedia = _model.getSingleMediaSourceMedia();
                singleJobMedia = _model.getSingleJobMedia();

                // Single jobs media and single media-source media?
                isSingleMediaMatch = singleMediaSourceMedia === singleJobMedia && (!_model.printSelectedMedia || _model.printSelectedMedia === singleJobMedia);

                isAuto = _isMediaSourceAutoSelected();

                isMediaClash = !(isSingleMediaMatch || isAuto || (!isAuto && _model.printSelectedMedia === singleJobMedia));

                if (_model.printMediaClashCurrent !== isMediaClash) {
                    _model.printPageScaling = isMediaClash ? _model.PRINT_SCALING_CLASH_DFLT.value : _model.PRINT_SCALING_MATCH_DFLT.value;
                    _model.printMediaClashCurrent = isMediaClash;
                    _m2vPrintScaling();
                }

                _m2vPrintScalingPreview();

                showScaling = isInboxDocs2Print && (isMediaClash ? _model.PRINT_SCALING_CLASH_DFLT.show : _model.PRINT_SCALING_MATCH_DFLT.show);

                _view.visible($('.sp-print-job-scaling'), showScaling);
                _view.visible($('.sp-print-job-media-info'), showScaling);
                _view.visible($('.sp-print-job-info'), isInboxDocs2Print);

                _model.showCopyJobMedia(_view);

                if (isInboxDocs2Print) {
                    _model.showJobsMatchMedia(_view);
                }

            },
            //
            // Choices from model to view.
            //
            _m2v = function() {
                var i = 0;

                $.each(_model.myPrinterOpt, function(key, value) {
                    $('#' + PRINT_OPT_PFX + i).val(value).selectmenu('refresh');
                    i += 1;
                });

                _showNumberUpPreview();

                _onChangeMediaSource($("select[data-savapage='media-source']"));

                // resolve visibility
                _onChangeMediaSource($("select[data-savapage='media-type']"));
            },
            //
            // Choices from view to model.
            //
            _v2Options = function(printerOptions) {

                var i = 0;

                $.each(_model.myPrinterOpt, function(key, value) {
                    printerOptions[key] = $('#' + PRINT_OPT_PFX + i).val();
                    i += 1;
                });
            },
            //
            // Choices from view to model.
            //
            _v2m = function() {
                _v2Options(_model.myPrinterOpt);
                _model.printPageScaling = _view.getRadioValue('print-page-scaling-enum');
            },
            //
            _m2vPrintScaling = function() {
                _view.checkRadioValue('print-page-scaling-enum', _model.printPageScaling);
            },
            //
            _m2vPrintScalingPreview = function() {
                var html = _model.printSelectedMediaShort() || '';
                if (_model.printPageScaling !== _model.PRINT_SCALING_ENUM.NONE) {
                    if (html.length > 0) {
                        html += ' &bull; ';
                    }
                    html += _view.getRadioSelected('print-page-scaling-enum').prev('label').text().toLowerCase();
                }
                $('.sp-nup-preview-title').html(html);
                $('.sp-nup-preview').attr('title', _model.printSelectedMedia || '');
            },
            //
            _onNext = function() {
                var printerOptions = {};
                _v2Options(printerOptions);
                if (_this.onPrinterOptValidate(printerOptions)) {
                    _v2m();
                    return true;
                }
                return false;
            },

            /**
             * Injects HTML and CSS into IPP attr <option> elements.
             *
             * @param selSelect The $selector of the <select> element.
             * @param isInit If true, this is the initializing action.
             */
            _injectPrinterOptionSelect = function(selSelect, isInit) {
                var target_id = selSelect.attr('id');

                $('#' + target_id + ' option').each(function() {
                    var iconClass = $(this).attr(CUSTOM_HTML5_DATA_ATTR_ICON),
                        selOption,
                        ind;

                    if (!iconClass) {
                        return;
                    }

                    ind = $(this).index();
                    $('#' + target_id + '-menu').find('[data-option-index=' + ind + '] a').prepend('<span class="' + CSS_CLASS_IPP_ICON + ' ' + iconClass + '">&nbsp;</span>');

                    if (isInit && $(this).attr('selected')) {
                        selOption = $(this).closest('.ui-select').find('.ui-btn');
                        selOption.attr(CUSTOM_HTML5_DATA_ATTR_ICON, iconClass);
                        selOption.addClass(CSS_CLASS_IPP_ICON);
                        selOption.addClass(iconClass);
                    }
                });
            },

            _showPrinterOptions = function(thisPage) {
                var i = 0,
                    isMediaSourceMatch = _model.isMediaSourceMatch(),
                    selExpr,
                    html = '<ul data-role="listview">';

                _model.hasPrinterManualMedia = false;

                $.each(_model.myPrinter.groups, function(key, group) {
                    var j = 0,
                        skip;

                    $.each(group.options, function(key, option) {

                        var keyword = option.keyword,
                            selected = _model.myPrinterOpt[keyword],
                            selectedSkipped = false,
                            firstChoice;

                        // Set skip = true, if keyword must be skipped ...
                        skip = false;

                        // .. and if skipped ...
                        if (skip) {
                            i += 1;
                            // continue by returning this function.
                            return;
                        }

                        if (j === 0) {
                            html += '<li>';
                        }

                        selExpr = PRINT_OPT_PFX + i;

                        html += '<div id="' + selExpr + PRINT_OPT_DIV_SFX + '">';
                        html += '<label class="sp-txt-wrap" for="' + selExpr + '">' + option.uiText + '</label>';
                        html += '<select ' + CUSTOM_HTML5_DATA_ATTR + '="' + keyword + '" id="' + selExpr + '" data-mini="true" data-native-menu="false" class="' + CSS_CLASS_SELECT_IPP_OPTION + '">';

                        $.each(option.choices, function(key, val) {

                            skip = false;

                            if (keyword === 'media-source' && val.choice === 'manual') {
                                _model.hasPrinterManualMedia = true;
                            }

                            // Skip?
                            if (keyword === 'media-source') {
                                skip = val.choice === 'auto' && (_model.isCopyJobTicket || !isMediaSourceMatch);
                            }

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

                                if (val.uiIconClass) {
                                    html += ' ' + CUSTOM_HTML5_DATA_ATTR_ICON + '="' + val.uiIconClass + '"';
                                }

                                html += '>' + val.uiText + '</option>';
                            }
                        });

                        if (selectedSkipped) {
                            _model.myPrinterOpt[keyword] = firstChoice;
                            option.defchoiceOverride = firstChoice;
                        }

                        html += '</select>';
                        html += '</div>';

                        i += 1;
                        j += 1;
                    });

                    if (j > 0) {
                        html += '</li>';
                    }

                });

                html += '</ul>';
                $('#printer-options').html(html);
                thisPage.enhanceWithin();

                $('#printer-options').find('select').each(function() {
                    _injectPrinterOptionSelect($(this), true);
                });
            },

            /**
             *
             */
            _onChangeIppOption = function(select) {
                var iconNew = select.find(':selected').attr(CUSTOM_HTML5_DATA_ATTR_ICON),
                    iconOld,
                    selUi;

                if (!iconNew) {
                    return;
                }
                selUi = select.closest('.ui-select').find('.ui-btn');
                iconOld = selUi.attr(CUSTOM_HTML5_DATA_ATTR_ICON);

                if (iconOld) {
                    selUi.removeClass(iconOld);
                }
                selUi.attr(CUSTOM_HTML5_DATA_ATTR_ICON, iconNew);
                selUi.addClass(iconNew);
                /* Re-inject, because prev. injection is gone.*/
                _injectPrinterOptionSelect(select, false);
            };

        // A way to set visibility of media and scaling, also in other parts
        // of the application.
        this.m2v = function() {
            _m2v();
        };

        $('#page-printer-settings').on('pagecreate', function(event) {

            $('#button-print-settings-back').click(function(e) {
                $('#button-printer-back').click();
                return false;
            });

            $('#button-print-settings-next').click(function() {
                if (_onNext()) {
                    _view.changePage($('#page-print'));
                }
                return false;
            });

            $('#button-print-settings-next-invoicing').click(function() {
                if (_onNext()) {
                    $('#button-print-delegation').click();
                }
                return false;
            });

            $('#button-print-settings-default').click(function() {
                _model.setPrinterDefaults();
                _applyLandscapeHint();
                _m2v();
                _model.showJobsMatchMediaSources(_view);
                _model.showCopyJobMedia(_view);

                $('#printer-options').find('select').each(function() {
                    _onChangeIppOption($(this));
                });

                return false;
            });

            $(this).on('change', "input:checkbox[id='cb-nup-preview-landscape']", null, function() {
                _showNumberUpPreview();
            });

            /*
             * When page-scaling is changed.
             */
            $('input[name=print-page-scaling-enum]:radio').change(function(event) {
                _model.printPageScaling = _view.getRadioValue('print-page-scaling-enum');
                _model.showJobsMatchMedia(_view);
                _model.showJobsMatchMediaSources(_view);
                _m2vPrintScalingPreview();
            });

            /*
             * When any printer option is changed.
             */
            $('#printer-options').change(function(event) {
                var sel = $(event.target);
                _probeChangePageRotate180(sel);
                _probeChangeNumberUp(sel);
                _probeChangeStaple(sel);
                _probeChangePunch(sel);
                _onChangeMediaSource(sel);
                _model.showJobsMatchMediaSources(_view);
            });

            $(this).on('change', '.' + CSS_CLASS_SELECT_IPP_OPTION, null, function() {
                _onChangeIppOption($(this));
                return;
            });

        }).on("pagebeforeshow", function(event, ui) {
            var i = 0,
                selMediaSource;

            _model.myFirstPageShowPrintSettings = false;

            $('#title-printer-settings').html(_model.myPrinter.alias);
            _view.visible($('.sp-print-job-media-info'), false);

            // Set visibility of widgets based on job media status.
            _model.showJobsMatchMediaSources(_view);
            _model.showCopyJobMedia(_view);

            //
            _showPrinterOptions($(this));

            // Resolve visibility
            _m2vPrintScaling();

            _applyLandscapeHint();
            _showNumberUpPreview();

            _onChangeMediaSource($("select[data-savapage='media-type']"));

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

        });
    }

    /**
     *
     */
    function PagePrint(_i18n, _view, _model, _api, _ctrl) {

        var _this = this,
            _quickPrinterCache = [],
            _quickPrinterSelected,
            _lastPrinterFilter,
            _lastPrinterFilterJobTicket,
            _fastPrintAvailable,
            _hasDelegatedPrint,
            _jobticketDatetimeDefaultPresent,
            _qsButtons,
            //
            _getPrinterImg = function(item, isDirect) {
                if (item.printer.jobTicket) {
                    return 'printer-jobticket-32x32.png';
                }
                if (item.printer.readerSecured) {
                    if (isDirect) {
                        return 'device-card-reader-terminal-16x16.png';
                    }
                    return 'device-card-reader-16x16.png';
                }
                if (item.printer.terminalSecured) {
                    return 'printer-terminal-custom-16x16.png';
                }
                return 'printer-terminal-any-16x16.png';
            },

            _getQuickPrinterHtml = function(item) {
                var html,
                    authMode = item.printer.authMode,
                    isDirect = (authMode === 'DIRECT' || authMode === 'FAST_DIRECT'),
                    isFast = (authMode === 'FAST' || authMode === 'FAST_DIRECT' || authMode === 'FAST_HOLD');

                html = "<img width=\"16\" height=\"16\" src=\"/images/" + _getPrinterImg(item, isDirect) + "\"/>";
                html += "<span class=\"ui-mini sp-txt-wrap\">" + item.text;
                if (item.printer.location) {
                    html += " &bull; ";
                }
                html += item.printer.location || "&nbsp;";
                html += "<span/>";

                if (isFast) {
                    html += "<span class=\"ui-li-count\">Fast</span>";
                }
                return html;
            },

            _areMultiplePrinterAvailable = function() {
                var res = _api.call({
                    request: 'printer-quick-search-user',
                    dto: JSON.stringify({
                        filter: '',
                        jobTicket: null,
                        maxResults: 2
                    })
                });

                if (res.result.code === '0') {
                    return res.dto.items.length > 1;
                }
                _view.showApiMsg(res);
                return true;
            },

            _onQuickPrinterSearch = function(target, filter, startPosition, paging) {
                /* QuickSearchFilterPrinterDto */
                var res,
                    filterJobTicket = _model.hasInboxDocs() ? null : true,
                    html = "";

                // Prevent duplicate search on "focusout" of search field.
                if (!paging && _lastPrinterFilter === filter && _lastPrinterFilterJobTicket === filterJobTicket) {
                    return;
                }
                _lastPrinterFilter = filter;
                _lastPrinterFilterJobTicket = filterJobTicket;

                if (!_quickPrinterSelected || (_quickPrinterSelected && filter !== _quickPrinterSelected.text)) {
                    _view.visible($('#content-print .printer-selected'), false);
                    _view.repairVisibility();
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
                    request: 'printer-quick-search-user',
                    dto: JSON.stringify({
                        filter: filter,
                        jobTicket: filterJobTicket,
                        startPosition: startPosition,
                        maxResults: _model.PRINTERS_QUICK_SEARCH_MAX
                    })
                });

                if (res.result.code === '0') {

                    _quickPrinterCache = res.dto.items;
                    _fastPrintAvailable = res.dto.fastPrintAvailable;

                    if (_fastPrintAvailable && _model.myFirstPageShowPrint) {
                        _this.onFastProxyPrintRenew(false);
                    }

                    _view.visible($('#content-print .printer-fast-print-info'), _fastPrintAvailable);

                    $.each(_quickPrinterCache, function(key, item) {
                        html += "<li class=\"ui-mini ui-li-has-icon\" data-icon=\"false\" data-savapage=\"" + key + "\">";
                        html += "<a tabindex=\"0\" href=\"#\">";
                        html += _getQuickPrinterHtml(item);
                        html += "</a></li>";
                    });

                    _ns.QuickSearchUtils.setNavigation(_view, _qsButtons, res.dto);

                } else {
                    _view.showApiMsg(res);
                }

                target.html(html).filterable("refresh");
            },

            _onSelectPrinter = function(selection, filterable) {
                var attr = "data-savapage",
                    sel = $("#sp-print-qs-printer"),
                    printer;

                _quickPrinterSelected = _quickPrinterCache[selection.attr(attr)];

                $('#button-print-settings').html(_getQuickPrinterHtml(_quickPrinterSelected));

                printer = _quickPrinterSelected.printer;

                sel.attr(attr, _quickPrinterSelected.key);
                sel.val(_quickPrinterSelected.text);

                filterable.empty();

                if (printer.readerSecured) {
                    _model.myPrinterReaderName = printer.readerName;
                }

                if (_this.onPrinter(printer.name)) {

                    $(".sp-print-printer-selected").show();
                    _view.visible($('#content-print .printer-selected'), true);
                    _view.visible($('#content-print .printer-fast-print-info'), false);

                    $("#print-title").focus();

                    _view.visible(_qsButtons, false);

                } else {
                    // An error occurred, re-show available printers...
                    sel.val('');
                    _onQuickPrinterSearch($("#sp-print-qs-printer-filter"), "");
                }

                _view.visible($('.sp-jobticket'), _model.myPrinter.jobTicket);
                _view.visible($('.sp-jobticket-labels'), _model.myPrinter.jobTicketLabelsEnabled);
                _view.visible($('.sp-proxyprint'), !_model.myPrinter.jobTicket);
            },

            _isDelegatedPrint = function() {
                var sel = $('#print-as-delegate');
                if (_hasDelegatedPrint && sel.length === 0 && _model.printDelegationCopies > 0) {
                    return true;
                }
                return sel && !_view.isFlipswitchDisabled(sel) && _view.isCbChecked(sel);
            },

            _getJobTicketType = function(isJobTicket) {
                return isJobTicket ? _view.getRadioValue('sp-print-jobticket-type') || _model.TICKETTYPE_PRINT : _model.TICKETTYPE_PRINT;
            },

            _onPrint = function(isClose, calcCostMode) {
                var clearScope = null,
                    isJobticket = _model.myPrinter.jobTicket,
                    separateDocs = null,
                    selWlk;

                if (_view.isCbChecked($("#delete-pages-after-print"))) {
                    clearScope = _view.getRadioValue('delete-pages-after-print-scope');
                }

                if (isJobticket) {
                    selWlk = $("#print-documents-separate-ticket");
                } else {
                    selWlk = $("#print-documents-separate-print");
                }
                separateDocs = selWlk.length > 0 ? _view.isCbChecked(selWlk) : null;

                return _this.onPrint(clearScope, isClose, _view.isCbChecked($("#print-remove-graphics"))
                    , _view.isCbChecked($("#print-ecoprint")), _view.isCbChecked($("#print-collate"))
                    , _view.isCbChecked($('#print-archive-print-job'))
                    , _isDelegatedPrint(), separateDocs
                    , isJobticket, _getJobTicketType(_model.myPrinter.jobTicket)
                    , _view.isCbChecked($('#cb-nup-preview-landscape'))
                    , calcCostMode);
            },

            _onPrintAsync = function() {
                $.mobile.loading("show");
                _ns.Utils.asyncFoo(function() {
                    _onPrint(true);
                    $.mobile.loading("hide");
                });
            },

            _onPrintPopup = function(src) {
                var res = _onPrint(false, true);
                if (res.result.code === "0") {
                    $('#sp-popup-print-and-close-cost').html(res.dto.cost);
                    $('#sp-popup-print-and-close').popup('open', {
                        positionTo: src ? src : 'window',
                        arrow: 'b'
                    });
                } else {
                    _view.showApiMsg(res);
                }
            },

            _onJobTicketType = function(ticketType) {
                var isPrint = ticketType === _model.TICKETTYPE_PRINT,
                    multipleJobs = isPrint && _model.hasMultipleVanillaJobs();

                _view.visible($('.sp-jobticket-print'), isPrint);
                _view.visible($('#sp-print-page-ranges-div'), isPrint);
                _view.visible($('#sp-jobticket-copy-pages-div'), !isPrint);

                _setVisibilityPrintSeparately(multipleJobs, true);

                _model.isCopyJobTicket = !isPrint;
                _model.showCopyJobMedia(_view);

                if (!_model.isCopyJobTicket) {
                    _model.showJobsMatchMediaSources(_view);
                }

                _model.showPrintJobMedia(_view);

                _this.onChangeJobTicketType(_model.isCopyJobTicket);
            },

            _setVisibilityPrintSeparately = function(multipleJobs, jobTicket) {
                _view.visible($('#print-documents-separate-print-div'), multipleJobs && !jobTicket);
                _view.visible($('#print-documents-separate-ticket-div'), multipleJobs && jobTicket);
            },

            _setVisibility = function() {
                var selCollate = $(".print-collate"),
                    copies,
                    delegatedPrint = _isDelegatedPrint(),
                    jobTicket = _model.myPrinter && _model.myPrinter.jobTicket,
                    jobTicketLabelsEnabled = _model.myPrinter && _model.myPrinter.jobTicketLabelsEnabled,
                    jobTicketType,
                    isPrintJob,
                    hasInboxDocs = _model.hasInboxDocs();

                if (delegatedPrint) {
                    copies = _model.printDelegationCopies;
                    $('#delegated-print-copies').val(copies);
                } else if (jobTicket) {
                    copies = parseInt($('#number-print-copies').val(), 10);
                } else {
                    copies = parseInt($('#slider-print-copies').val(), 10);
                }

                // Assume visible, evaluate later.
                _view.visible($('.sp-proxyprint-archive'), true);

                _view.visible($('#slider-print-copies-div'), !delegatedPrint && !jobTicket);
                _view.visible($('#number-print-copies-div'), !delegatedPrint && jobTicket);
                _view.visible($('#sp-print-shared-account-div'), !delegatedPrint);

                // Hide for now.
                _view.visible($('#delegated-print-copies-div'), false);

                if (copies > 1) {

                    selCollate.show();

                    if ($("#print-collate").is(':checked')) {
                        $('.print_collate_sheet_1_1').html('1');
                        $('.print_collate_sheet_1_2').html('2');
                        $('.print_collate_sheet_2_1').html('1');
                        $('.print_collate_sheet_2_2').html('2');
                    } else {
                        $('.print_collate_sheet_1_1').html('1');
                        $('.print_collate_sheet_1_2').html('1');
                        $('.print_collate_sheet_2_1').html('2');
                        $('.print_collate_sheet_2_2').html('2');
                    }
                } else {
                    selCollate.hide();
                }

                _view.visible($('.delete-pages-after-print-scope-enabled'), _view.isCbChecked($('#delete-pages-after-print')));

                if (_model.myPrinter) {
                    _view.visible($('.sp-proxyprint'), !jobTicket);
                    _view.visible($('.sp-jobticket'), jobTicket);
                    _view.visible($('.sp-jobticket-labels'), jobTicketLabelsEnabled);
                }

                _setVisibilityPrintSeparately(_model.hasMultipleVanillaJobs(), jobTicket);

                if (jobTicket) {
                    // (1) first enable.
                    _view.enable($('#sp-print-jobticket-type-print'), hasInboxDocs);
                    if (hasInboxDocs) {
                        jobTicketType = _getJobTicketType(jobTicket);
                    } else {
                        jobTicketType = _model.TICKETTYPE_COPY;
                    }
                    // (2) then check to see enabled/disabled state.
                    _view.checkRadioValue('sp-print-jobticket-type', jobTicketType);
                    _onJobTicketType(jobTicketType);

                    isPrintJob = jobTicketType !== _model.TICKETTYPE_COPY;
                } else {
                    isPrintJob = true;
                }
                _view.visible($('.sp-print-job-info'), isPrintJob && hasInboxDocs);
                _view.visible($('#sp-jobticket-copy-pages-div'), jobTicket && !isPrintJob);
                _view.visible($('#sp-print-page-ranges-div'), isPrintJob);

                if (!_model.myPrinter || _model.myPrinter.archiveDisabled) {
                    _view.visible($('.sp-proxyprint-archive'), false);
                }

            },

            _onJobListChange = function() {
                var sel = $('#print-job-list :selected'),
                    selTitle = $('#print-title'),
                    isAllDocsSelected = sel.val() === '-1';

                if (_model.printJobIndex === '-1') {
                    _model.myInboxTitle = selTitle.val();
                }

                _model.printJobIndex = sel.val();

                if (sel.val() === '-1') {
                    selTitle.val(_model.myInboxTitle);
                } else {
                    selTitle.val(sel.text());
                }
                _model.myPrintTitle = selTitle.val();

                _setVisibilityPrintSeparately(!_model.isCopyJobTicket && isAllDocsSelected && _model.hasMultipleVanillaJobs(), _model.myPrinter && _model.myPrinter.jobTicket);

                _model.setPrintPreviewLandscapeHint(isAllDocsSelected ? 0 : parseInt(sel.val(), 10));
            },

            _onJobTicketDomainListChange = function() {
                var selDomain = $('#sp-jobticket-domain-list :selected'),
                    domainID = selDomain.val(),
                    selUseList = $('#sp-jobticket-use-list'),
                    selUses = selUseList.find('option'),
                    selTagList = $('#sp-jobticket-tag-list'),
                    selTags = selTagList.find('option');

                _view.visible(selUses, false);
                _view.visible(selTags, false);

                _view.visible(selUses.filter('.sp-jobticket-domain'), true);
                _view.visible(selTags.filter('.sp-jobticket-domain'), true);

                _view.setSelectedValue(selUseList, '');
                _view.setSelectedValue(selTagList, '');

                if (domainID) {
                    _view.visible(selUses.filter('.sp-jobticket-domain-' + domainID), true);
                    _view.visible(selTags.filter('.sp-jobticket-domain-' + domainID), true);
                }
            },

            _resetPrinterSearch = function() {
                var val = '',
                    sel = $("#sp-print-qs-printer");
                sel.val('');
                _onQuickPrinterSearch($("#sp-print-qs-printer-filter"), val);
                _view.asyncFocus(sel);
            },

            _getJobTicketFirstValidDateTime = function(refDate, offsetDays) {
                var i,
                    msecDay = 24 * 60 * 60 * 1000,
                    firstDateTime = refDate.getTime() + offsetDays * msecDay;

                for (i = 0; i < 7; i++) {
                    if (_ns.Utils.findInArray(_model.JOBTICKET_DELIVERY_DAYS_OF_WEEK, new Date(firstDateTime).getDay())) {
                        break;
                    }
                    firstDateTime += msecDay;
                }
                return firstDateTime;
            };

        this.initJobTicketDateTime = function() {
            var selJobticketDate = $('#sp-jobticket-date'),
                selJobticketHrs,
                today = new Date();

            if (selJobticketDate.length > 0) {

                selJobticketDate.mobipick({
                    minDate: new Date(_getJobTicketFirstValidDateTime(today, _model.JOBTICKET_DELIVERY_DAYS_MIN))
                });

                _view.mobipickSetDate(selJobticketDate,
                    _getJobTicketFirstValidDateTime(today, _model.JOBTICKET_DELIVERY_DAYS));

                if (!_model.JOBTICKET_DELIVERY_DATE_PRESET) {
                    selJobticketDate.val('');
                }

                if (_jobticketDatetimeDefaultPresent) {
                    $('#sp-btn-jobticket-datetime-default').attr('title', selJobticketDate.val());
                }

                selJobticketHrs = $('#sp-jobticket-hrs');

                if (selJobticketHrs.length > 0) {
                    selJobticketHrs.val(_model.JOBTICKET_DELIVERY_HOUR);
                    $('#sp-jobticket-min').val(_model.JOBTICKET_DELIVERY_MINUTE);
                }
            }
        };

        this.clearInput = function() {
            var selCbClear = $('#delete-pages-after-print');

            $('#slider-print-copies').val(1).slider("refresh");
            $('#delegated-print-copies').val(1);
            $('#number-print-copies').val(1);
            $('#print-page-ranges').val('');
            $('#print-title').val('');

            $('#sp-jobticket-copy-pages').val(1);
            $('#sp-jobticket-remark').val('');

            this.initJobTicketDateTime();

            if (selCbClear[0] && !$('#delete-pages-after-print')[0].disabled) {
                _view.checkCb("#delete-pages-after-print", false);
            }

            _view.checkCb("#print-archive-print-job", false);
            _view.checkRadioValue('sp-print-jobticket-type', _model.TICKETTYPE_PRINT);
            _view.setSelectedFirst($('#sp-print-shared-account-list'));
        };

        $('#page-print').on('pagecreate', function(event) {
            var filterablePrinter = $("#sp-print-qs-printer-filter");

            filterablePrinter.focus();
            //
            _hasDelegatedPrint = $('#button-print-delegation').length > 0;
            _qsButtons = $("#sp-print-quicksearch-printer-div .sp-quicksearch-buttons");

            //
            filterablePrinter.on("filterablebeforefilter", function(e, data) {
                _onQuickPrinterSearch($(this), data.input.get(0).value);
            });

            $(this).on('click', '#sp-print-qs-printer-filter li', null, function() {
                _onSelectPrinter($(this), filterablePrinter);
            });

            _model.hasMultiplePrinters = _areMultiplePrinterAvailable();

            // Show available printers on first open
            _onQuickPrinterSearch(filterablePrinter, "");

            if (!_model.hasMultiplePrinters) {
                if ($('#sp-print-qs-printer-filter li').length === 1) {
                    // Just one printer found: select.
                    $('#sp-print-qs-printer-filter li').click();
                    _view.visible($('#sp-print-quicksearch-printer-div'), false);
                }
            }

            // Pinter list navigation
            $("#sp-print-quicksearch-printer-div .sp-quicksearch-buttons .ui-btn").click(function() {
                _onQuickPrinterSearch(filterablePrinter, $("#sp-print-qs-printer").val(), $(this).attr('data-savapage'), true);
            });

            $("#print-collate").on("change", null, null, function(event, ui) {
                _setVisibility();
            });

            $('#slider-print-copies').change(function() {
                _setVisibility();
            });

            $('#number-print-copies').on("input", function() {
                _setVisibility();
            });

            $('#delete-pages-after-print').change(function() {
                _setVisibility();
            });

            $(this).on('change', "input:checkbox[id='print-as-delegate']", null, function(e) {
                _setVisibility();
            });

            $('#button-print-delegation').click(function() {
                _view.showPageAsync('#page-print-delegation', 'PagePrintDelegation');
                return false;
            });

            $('input[name="sp-print-jobticket-type"]').click(function() {
                _onJobTicketType($(this).attr('value'));

                if (_model.isCopyJobTicket) {
                    if (_model.isMediaSourceAuto()) {
                        $('#button-print-settings').click();
                    }
                }

            });

            $('#button-printer-back').click(function(e) {
                var sel;
                _view.checkRadioValue('sp-print-jobticket-type', _model.TICKETTYPE_PRINT);
                _model.isCopyJobTicket = false;

                if (_model.PROXY_PRINT_CLEAR_DELEGATE) {
                    _view.pages.printDelegation.clear();
                }
                if (_model.PROXY_PRINT_CLEAR_PRINTER && _model.myPrinter) {
                    _model.myShowPrinterInd = false;
                    $('#button-print-settings-default').click();
                    if (_model.hasMultiplePrinters) {
                        _view.pages.print.onClearPrinter();
                    } else {
                        _view.pages.print.onClearPrinterInd();
                    }
                }

                if (!_model.JOBTICKET_DOMAINS_RETAIN) {
                    sel = $('#sp-jobticket-domain-list');
                    if (sel.length > 0) {
                        _view.setSelectedFirst(sel);
                    }
                }
                _model.myShowUserStatsGet = true;
                _view.changePage($('#page-main'));
                return false;
            });

            $('#button-print-and-close-popup').click(function(e) {
                _onPrintPopup($(this));
                return false;
            });
            $('#button-print-and-close-popup-cancel').click(function(e) {
                $('#sp-popup-print-and-close').popup('close');
                return false;
            });
            $('#button-print-and-close-popup-ok').click(function(e) {
                $('#sp-popup-print-and-close').popup('close');
                _onPrintAsync();
                return false;
            });
            $('#button-print-and-close').click(function(e) {
                _onPrintAsync();
                return false;
            });

            $('#button-send-jobticket').click(function(e) {
                _onPrint(true);
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

            $('#print-job-list').change(function(event) {
                _onJobListChange();
                return false;
            });

            $('#sp-jobticket-domain-list').change(function(event) {
                _onJobTicketDomainListChange();
                return false;
            });

            _jobticketDatetimeDefaultPresent = $('#sp-btn-jobticket-datetime-default').length > 0;

            _view.mobipick($("#sp-jobticket-date"), true);
            _this.initJobTicketDateTime();

            $('#sp-jobticket-date').change(function(event) {
                //Sunday is 0
                var day = _view.mobipickGetDate($('#sp-jobticket-date')).getDay(),
                    found = _ns.Utils.findInArray(_model.JOBTICKET_DELIVERY_DAYS_OF_WEEK, day);

                if (!found) {
                    _ns.Utils.asyncFoo(function(p1) {
                        _view.msgDialogBox(_i18n.format('msg-jobticket-delivery-day-invalid', [p1]), 'sp-msg-popup-warn');
                    }, $('#sp-jobticket-date').val());
                    _this.initJobTicketDateTime();
                }
                return false;
            });

            if (_jobticketDatetimeDefaultPresent) {
                $('#sp-btn-jobticket-datetime-default').click(function(e) {
                    _this.initJobTicketDateTime();
                });
            }

        }).on("pagebeforeshow", function(event, ui) {

            // Illegal state, because not authorized?
            if ($('#sp-illegalstate-page').length > 0) {
                return;
            }

            _lastPrinterFilter = undefined;
            _lastPrinterFilterJobTicket = undefined;

            if (_model.isPrintDialogFromMain) {
                _model.isPrintDialogFromMain = false;
                if (_model.hasMultiplePrinters && _model.PROXY_PRINT_CLEAR_PRINTER) {
                    _resetPrinterSearch();
                }
            }

            _setVisibility();
            _this.onShow();
            _onJobListChange();

            if ($('#sp-jobticket-domain-list').length > 0) {
                _onJobTicketDomainListChange();
            }

            _view.pages.printSettings.m2v();

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
        var _this = this,
            _model = this,
            _LOC_AUTH_NAME = 'sp.auth.user.name',
            _LOC_AUTH_TOKEN = 'sp.auth.user.token',
            _LOC_LANG = 'sp.user.language',
            _LOC_COUNTRY = 'sp.user.country',
            _getPageRangesFormatted;

        this.TICKETTYPE_PRINT = 'PRINT';
        this.TICKETTYPE_COPY = 'COPY';

        /**
         * Creates a string with page range format from pages array.
         *
         * Example: '3-4,7,9-11'
         */
        _getPageRangesFormatted = function(myPages) {
            var ranges = '',
                pageStart,
                pageEnd,
                pagePrv,
                page,
                //
                addRange = function() {
                    if (ranges !== '') {
                        ranges += ',';
                    }
                    ranges += pageStart;
                    if (pageStart !== pageEnd) {
                        ranges += '-' + pageEnd;
                    }
                };

            for (page in myPages) {

                if (myPages.hasOwnProperty(page)) {
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
            }
            if (pagePrv) {
                addRange();
            }
            return ranges;
        };

        this.webPrintEnabled = false;
        this.webPrintDropZoneEnabled = false;
        this.webPrintMaxBytes = 0;
        this.webPrintFileExt = [];
        this.webPrintUploadUrl = null;
        this.webPrintUploadFileParm = null;
        this.webPrintUploadFontParm = null;

        this.setMailTicketsLocalStorageParms = function() {
            _LOC_AUTH_NAME = 'sp.auth.mailtickets.name';
            _LOC_AUTH_TOKEN = 'sp.auth.mailtickets.token';
            _LOC_LANG = 'sp.mailtickets.language';
            _LOC_COUNTRY = 'sp.mailtickets.country';
        };
        this.setPaymentLocalStorageParms = function() {
            _LOC_AUTH_NAME = 'sp.auth.payment.name';
            _LOC_AUTH_TOKEN = 'sp.auth.payment.token';
            _LOC_LANG = 'sp.payment.language';
            _LOC_COUNTRY = 'sp.payment.country';
        };
        /**
         * Creates a string with page range format from the cut pages.
         *
         * Example: '3-4,7,9-11'
         */
        this.getCutPageRanges = function() {
            return _getPageRangesFormatted(_this.myCutPages);
        };

        /**
         * Creates a string with page range format from the selected pages.
         *
         * Example: '3-4,7,9-11'
         */
        this.getSelectPageRanges = function() {
            return _getPageRangesFormatted(_this.mySelectPages);
        };

        this.MediaMatchEnum = {
            MATCH: 1,
            CLASH: 2
        };

        //
        this.PRINT_SCALING_ENUM = {
            NONE: 'NONE',
            FIT: 'FIT'
        };

        /*
         * Value: this.MediaMatchEnum.
         */
        this.jobsMatchMedia = null;
        this.jobsMatchMediaSources = null;

        /*
         * PageScalingEnum
         */
        this.printPageScaling = this.PRINT_SCALING_ENUM.NONE;

        this.printMediaClashCurrent = undefined;

        /**
         *
         */
        this.selectedMediaSourceUI = null;

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

        this.preservePrintJobSettings = false;

        this.hasMultiplePrinters = true;
        this.isPrintDialogFromMain = true;
        this.PROXY_PRINT_CLEAR_PRINTER = false;
        this.PROXY_PRINT_CLEAR_DELEGATE = false;
        this.JOBTICKET_COPIER_ENABLE = false;

        // state
        this.isCopyJobTicket = false;

        this.myInboxTitle = null;
        this.myPrintTitle = null;
        this.myPdfTitle = null;
        this.myJobsVanilla = null;
        this.pdfJobIndex = -1;
        this.printJobIndex = -1;
        this.printPreviewLandscapeHint = null;
        this.iPopUpJob = -1;
        this.myJobs = [];
        this.myJobPages = [];
        this.myTotPages = 0;
        this.myPrinter = undefined;

        this.myShowPrinterInd = true;

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
        //
        this.mySelectPages = {};

        this.printSelectedMediaShort = function() {
            var kw = ['a5', 'a4', 'a3'],
                len = kw.length,
                i;

            if (!this.printSelectedMedia) {
                return '';
            }

            for (i = 0; i < len; i++) {
                if (this.printSelectedMedia.indexOf(kw[i]) !== -1) {
                    return kw[i].toUpperCase();
                }
            }
            return this.printSelectedMedia;
        };

        /*
         * this.user = { alias : null, id : null, admin : null, role : null,
         * mail : null, mailDefault : null, loggedIn : false };
         */
        this.user = new _ns.User();

        this.propPdf = this.propPdfDefault;

        this.determineCopyJobTicket = function() {
            this.isCopyJobTicket = this.JOBTICKET_COPIER_ENABLE && (this.myPrinter === undefined || (this.myPrinter && this.myPrinter.jobTicket)) && this.myJobs.length === 0;
        };

        /**
         * @param iJob 0-based index of job to use for orientation hint.
         */
        this.setPrintPreviewLandscapeHint = function(iJob) {
            this.printPreviewLandscapeHint = !this.isCopyJobTicket && this.myJobs[iJob].landscapeView;
        };

        /**
         *
         */
        this.hasInboxDocs = function() {
            return this.myJobs.length > 0;
        };

        /**
         * Creates job media map: ippMedia -> count, mediaUi
         */
        this.createMediaJobMap = function() {
            var mapMediaJobs = [];

            $.each(_this.myJobs, function(key, value) {

                var media = value.media,
                    entry = mapMediaJobs[media];

                if (!entry) {
                    entry = {
                        count: 0,
                        mediaUi: value.mediaUi
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

                    var media = value.media,
                        entry = mapMediaSources[media];

                    if (!entry) {
                        entry = {
                            count: 0,
                            source: value.source
                        };
                        mapMediaSources[media] = entry;
                    }
                    entry.count++;
                });
            }
            return mapMediaSources;
        };

        this.showCopyJobMedia = function(_view) {
            var sel;
            _view.visible($('.sp-copy-job-info'), this.isCopyJobTicket);
            if (this.isCopyJobTicket) {
                sel = $('.sp-copy-job-media-sources-info');
                sel.html(this.selectedMediaSourceUI || '&nbsp;-&nbsp;');
                _view.visible(sel, this.selectedMediaSourceUI);
            }
        };

        this.showPrintJobMedia = function(_view) {
            var hasPrintable = !this.isCopyJobTicket && _model.hasInboxDocs();
            _view.visible($('.sp-print-job-info'), hasPrintable);
            _view.visible($('.sp-print-job-media-sources-info'), hasPrintable);

            if (hasPrintable) {
                this.showJobsMatchMedia(_view);
            } else {
                _view.visible($('.sp-print-job-media-info'), false);
            }
        };

        /**
         * Displays info about job media and selected 'media' ands sets
         * this.jobsMatchMedia.
         */
        this.showJobsMatchMedia = function(_view) {
            var html = '',
                mediaWlk,
                mapMediaJobs = this.createMediaJobMap(),
                media = this.printSelectedMedia,
                selJobMediaInfo = $('.sp-print-job-media-info');

            // Do we have a job/media match/clash?
            this.jobsMatchMedia = this.MediaMatchEnum.MATCH;

            // Create the html.
            for (mediaWlk in mapMediaJobs) {

                if (mapMediaJobs.hasOwnProperty(mediaWlk)) {

                    if (html.length) {
                        html += ' ';
                    }

                    html += '<span class="';

                    if (media && mediaWlk !== media) {
                        this.jobsMatchMedia = this.MediaMatchEnum.CLASH;
                        if (this.printPageScaling === this.PRINT_SCALING_ENUM.NONE) {
                            html += 'sp-ipp-media-info-none';
                        } else {
                            html += 'sp-ipp-media-info-fit';
                        }
                    } else {
                        html += 'sp-ipp-media-info-match';
                    }
                    html += '">' + mapMediaJobs[mediaWlk].mediaUi + '</span>';
                }
            }

            if (html.length === 0 || !this.myPrinter) {
                _view.visible(selJobMediaInfo, false);
            } else {
                selJobMediaInfo.empty().append(html);
                _view.visible(selJobMediaInfo, this.jobsMatchMedia !== this.MediaMatchEnum.MATCH);
            }
        };

        /**
         * Displays info about job media and available 'media-source' for
         * the selected printer ands sets this.jobsMatchMediaSources.
         */
        this.showJobsMatchMediaSources = function(_view) {
            var html = '',
                selHtml,
                mapMediaJobs = this.createMediaJobMap(),
                mapMediaSources = this.createPrinterMediaSourcesMap(),
                mediaWlk,
                IS_UNIQUE_MEDIASOURCE_REQUIRED = false;

            if (this.isPrintManualFeed) {
                html += '<span class="sp-ipp-media-info-match">M</span>';
            }

            // Do we have a job/source media match/clash?
            this.jobsMatchMediaSources = this.MediaMatchEnum.MATCH;

            // Create the html.
            for (mediaWlk in mapMediaJobs) {

                if (mapMediaJobs.hasOwnProperty(mediaWlk)) {

                    if (html.length) {
                        html += ' ';
                    }

                    html += '<span class="';

                    if (!mapMediaSources[mediaWlk] || (IS_UNIQUE_MEDIASOURCE_REQUIRED && mapMediaSources[mediaWlk].count > 1)) {

                        this.jobsMatchMediaSources = this.MediaMatchEnum.CLASH;

                        if (this.printPageScaling === _this.PRINT_SCALING_ENUM.NONE) {
                            html += 'sp-ipp-media-info-none';
                        } else {
                            html += 'sp-ipp-media-info-fit';
                        }

                    } else {
                        html += 'sp-ipp-media-info-match';
                    }
                    html += '">' + mapMediaJobs[mediaWlk].mediaUi + '</span>';
                }
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

        this.isMediaSourceAuto = function() {
            return this.myPrinterOpt['media-source'] === 'auto';
        };

        /**
         * Does the media of the jobs match the user SELECTED media of the
         * selected printer.
         */
        this.isMediaJobMatch = function() {
            return this.jobsMatchMedia === this.MediaMatchEnum.MATCH;
        };

        // Thumbnails
        this.refreshUniqueImgUrlValueTn = function() {
            this.uniqueImgUrlValue = new Date().getTime().toString();
        };

        this.refreshUniqueImgUrlValues = function() {
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

        this.canSelectAllDocuments = function() {
            return this.myJobsVanilla && _ns.Utils.countProp(this.mySelectPages) === 0;
        };

        this.hasMultipleVanillaJobs = function() {
            return this.canSelectAllDocuments() && this.myJobs.length > 1;
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

            item = _LOC_COUNTRY;
            if (window.localStorage[item] !== null) {
                this.authToken.country = window.localStorage[item];
            }

        };

        this.setLanguage = function(lang) {
            this.authToken.language = lang;
            window.localStorage[_LOC_LANG] = lang;
        };

        this.setCountry = function(country) {
            this.authToken.country = country;
            window.localStorage[_LOC_COUNTRY] = country;
        };

        this.setAuthToken = function(user, token, language, country) {
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
            if (country) {
                this.setCountry(country);
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
            this.myTotPages = 0;
            this.myJobPages = [];
            this.myCutPages = {};
            this.mySelectPages = {};
            this.myPrinter = undefined;
            this.propPdf = this.propPdfDefault;
            this.refreshUniqueImgUrlValues();
            this.prevMsgTime = null;

            this.printDelegation = {};
            this.printDelegationCopies = 0;
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
            var i = 0,
                nPage = 1;
            for (i = 0; i < iPage; i++) {
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
            var i = 0,
                nPageTot = 0,
                rotate,
                jobPage;

            for (i = 0; i < this.myJobPages.length; i++) {
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

            _this.printPageScaling = _this.PRINT_SCALING_ENUM.NONE;
            _this.printMediaClashCurrent = undefined;

            _this.myPrinterOpt = {};

            /*
             * Flatten the options to one (1) array
             */
            $.each(_this.myPrinter.groups, function(key, group) {

                $.each(group.options, function(k2, option) {
                    _this.myPrinterOpt[option.keyword] = option.defchoiceOverride || option.defchoice;
                });

            });
        };

        /** */
        this.addJobPages = function(pages) {
            var i;
            for (i = 0; i < pages.length; i++) {
                this.myJobPages.push(pages[i]);
            }
        };

        /** */
        this.clearJobs = function() {
            this.myJobs = [];
            this.myTotPages = 0;
            this.myJobPages = [];
        };

        /**
         * Return the selected letterhead object.
         *
         * Get the zero-based index of the selected letterhead in an option
         * list, compensate for the 'none' option (-1), and use
         * the index to read into the _model.letterheads[] .
         *
         * @param sel
         *            The jQuery selector of the select option list.
         */
        this.getSelLetterheadObj = function(sel) {
            var idx = $(sel + ' option').index($(sel + ' :selected')) - 1;
            return this.letterheads[idx];
        };

    }

    /**
     *
     */
    function Controller(_i18n, _model, _view, _api, _cometd, _userEvent, _deviceEvent, _proxyprintEvent) {
        var _util = _ns.Utils,
            _this = this,
            i18nRefresh,
            _getLetterheads,
            _tbIndUser,
            _initUser,
            _adaptLetterheadPage,
            _refreshLetterheadList,
            _saveSelectedletterhead,
            _savePdfProps,
            _userLazyEcoPrint,
            _handleSafePageEvent,
            /*
             * The current icon (name of the icon from the standard JQuery Mobile
             * icon set). This should match the initial value in html.
             */
            _iconCur = 'info',
            _changeIcon,
            _prepareReaderForPrinter,
            _cometdMaxNetworkDelay,
            _timeoutAuthPrint,
            _countdownAuthPrint,
            _clearTimeoutAuthPrint,

            _saveRemoveGraphics = function(sel) {
                _model.removeGraphics = _view.isCbChecked($(sel));
            },

            _saveEcoprint = function(sel) {
                _model.ecoprint = _view.isCbChecked($(sel));
            },

            _savePdfGrayscale = function(sel) {
                _model.pdfGrayscale = _view.isCbChecked($(sel));
            },

            _savePdfRasterize = function(sel) {
                _model.pdfRasterize = _view.isCbChecked($(sel));
            },

            _checkVanillaJobs = function() {

                var res = _api.call({
                    request: 'inbox-is-vanilla'
                }),
                    isOk = res.result.code === '0';

                if (isOk) {
                    _model.myJobsVanilla = res.vanilla;
                } else {
                    _view.showApiMsg(res);
                }
                return isOk;
            },

            _refreshPrinterInd = function() {
                var trgColor = $('.sp-button-mini-print-color'),
                    trgMono = $('.sp-button-mini-print-mono'),
                    trgDuplex = $('.sp-button-mini-print-duplex'),
                    trgNup = $('.sp-button-mini-print-n-up'),
                    trgDelegated = $('.sp-button-mini-print-delegation'),
                    ippAttrVal,
                    isColor;

                if (trgDelegated) {
                    _view.enableFlipswitch($('#print-as-delegate'), _model.printDelegationCopies > 0);
                    trgDelegated.html(_model.printDelegationCopies || '-');
                }

                _model.myPrintTitle = $('#print-title').val();
                _saveSelectedletterhead('#print-letterhead-list');
                _saveRemoveGraphics('#print-remove-graphics');
                _saveEcoprint('#print-ecoprint');

                if (!_model.myShowPrinterInd) {
                    _view.visible(trgColor, false);
                    _view.visible(trgMono, false);
                    _view.visible(trgNup, false);
                    _view.visible(trgDuplex, false);
                    return;
                }

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
            };

        //
        i18nRefresh = function(i18nNew) {
            if (i18nNew && i18nNew.i18n) {
                _i18n.refresh(i18nNew.i18n);
                $.mobile.pageLoadErrorMessage = _i18n.string('msg-page-load-error');
            } else {
                _view.message('i18n initialization failed.');
            }
        };

        _tbIndUser = function() {
            //$('#mini-user-name').html(_model.user.id);
            $.noop();
        };

        _initUser = function(loginRes) {
            var res;

            _model.startSession();

            _model.prevMsgTime = loginRes.systime;

            _model.user.key_id = loginRes.key_id;
            _model.user.id = loginRes.id;
            _model.user.id_internal = loginRes.id_internal;
            _model.user.uuid = loginRes.uuid;
            _model.user.number = loginRes.number;

            _model.user.doclog.key_id = loginRes.doclog_key_id;
            _model.user.doclog.id = loginRes.doclog_id;
            _model.user.doclog.id_internal = loginRes.doclog_id_internal;

            _model.user.fullname = loginRes.fullname;

            _model.language = loginRes.language;
            _model.country = loginRes.country;

            _model.setAuthToken(loginRes.id, loginRes.authtoken, _model.language, _model.country);

            /*
             * This is the token used for CometD authentication. See: Java
             * org.savapage.server.cometd.BayeuxAuthenticator
             */
            _model.user.cometdToken = loginRes.cometdToken;

            _model.user.stats = loginRes.stats;

            _model.user.admin = loginRes.admin;
            _model.user.internal = loginRes.internal;
            _model.user.mail = loginRes.mail;
            _model.user.mailDefault = loginRes.mail;
            _model.letterheads = null;
            _model.propPdfDefault.desc.author = _model.user.fullname;

            res = _api.call({
                'request': 'pdf-get-properties'
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

        /**
         * Restores CometD UserEvent connection automatically.
         * Mantis #717.
         */
        this.onWakeUpAutoRestore = function(deferAppWakeUp) {

            if (_ns.logger.isDebugEnabled()) {
                _ns.logger.debug('onWakeUpAutoRestore');
            }

            _ns.stopAppWatchdog();

            /*
             * By pausing we tell the server to interrupt the long-poll: as a
             * result of this call the connection to the server might be lost
             * or the session is expired).
             */
            _ns.userEvent.pause();

            //
            _userEvent.removeListener();
            _proxyprintEvent.removeListener();

            /*
             * Are we still logged in (may be connection to server is lost or
             * session is expired)?
             */
            if (!_model.user.loggedIn) {
                return false;
            }

            // Start with defer setting.
            _ns.startAppWatchdog(deferAppWakeUp);

            _userEvent.setLongPollLost();

            // IMPORTANT: perform next steps async !!
            window.setTimeout(function() {
                _userEvent.addListener();
                _proxyprintEvent.addListener();
                _userEvent.resume();
            }, 10);

        };

        /**
         * Restores CometD UserEvent connection after user acknowledges
         * "Welcome Back" message.
         */
        this.onWakeUp = function(deltaMsec) {

            var buttonGoOn = $('#sp-popup-wakeup-refresh');

            if (_ns.logger.isDebugEnabled()) {
                _ns.logger.debug('onWakeUp');
            }

            /*
             * At this point we want user interaction: so, stop the timer!
             */
            _ns.stopAppWatchdog();

            /*
             * By pausing we tell the server to interrupt the long-poll: as a
             * result of this call the connection to the server might be lost
             * or the session is expired).
             */
            _ns.userEvent.pause();

            /*
             * #320
             */
            _userEvent.removeListener();
            _proxyprintEvent.removeListener();

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
             * IMPORTANT: we use a popup since we NEED user action to
             * continue.
             * Reason: some devices give some CPU cycles every 10-20 seconds
             * to the Web App when it is in the background.
             */
            $('#sp-popup-wakeup').popup('open', {
                positionTo: 'window'
            });

            buttonGoOn.focus();

            buttonGoOn.click(function(e) {

                _ns.startAppWatchdog(false);

                $('#sp-popup-wakeup').popup('close');

                _userEvent.setLongPollLost();

                _userEvent.addListener();
                _proxyprintEvent.addListener();

                _userEvent.resume();

                return false;
            });
        };

        /*
         *
         */
        this.init = function() {
            var res,
                language,
                country,
                authModeRequest = _util.getUrlParam(_ns.URL_PARM.LOGIN);

            _model.initAuth();

            res = _api.call({
                request: 'constants',
                webAppType: _ns.WEBAPP_TYPE,
                authMode: authModeRequest
            });

            /*
             * FIX for Opera to prevent endless reloads when server is down:
             * check the return code. Display a basic message (English is all
             * we
             * have), no fancy stuff (cause jQuery might not be working).
             */
            if (!res || res.result.code !== '0') {
                _view.message(res.result.txt || 'connection to server is lost');
                return;
            }
            _ns.configAppWatchdog(_this.onWakeUp, res.watchdogHeartbeatSecs, res.watchdogTimeoutSecs, _this.onWakeUpAutoRestore);

            _model.prevMsgTime = res.systime;

            _view.pages.login.setAuthMode(res.authName, res.authEmail, res.authId,
                res.authYubiKey, res.authCardLocal, res.authCardIp, res.authModeDefault,
                res.authCardPinReq, res.authCardSelfAssoc, res.yubikeyMaxMsecs,
                res.cardLocalMaxMsecs, res.cardAssocMaxSecs);

            // ProxyPrint
            _model.PRINT_SCALING_MATCH_DFLT = res.printScalingMatch;
            _model.PRINT_SCALING_CLASH_DFLT = res.printScalingClash;

            // WebPrint
            _model.webPrintEnabled = res.webPrintEnabled;
            _model.webPrintDropZoneEnabled = res.webPrintDropZoneEnabled;
            _model.webPrintMaxBytes = res.webPrintMaxBytes;
            _model.webPrintFileExt = res.webPrintFileExt;
            _model.webPrintUploadUrl = res.webPrintUploadUrl;
            _model.webPrintUploadFileParm = res.webPrintUploadFileParm;
            _model.webPrintUploadFontParm = res.webPrintUploadFontParm;

            // Configures CometD without starting it.
            _cometdMaxNetworkDelay = res.cometdMaxNetworkDelay;
            _cometd.configure(_cometdMaxNetworkDelay);

            _model.authCardIp = res.authCardIp;
            _model.cometdDeviceToken = res.cometdToken;
            _model.maxIdleSeconds = res.maxIdleSeconds;

            // OnOffEnum
            _model.showNavButtonTxt = res.showNavButtonTxt;

            _model.MY_THUMBNAIL_WIDTH = res['thumbnail-width'];
            _model.propPdfDefault = res['pdf-prop-default'];

            //
            _model.PROXY_PRINT_CLEAR_PRINTER = res.proxyPrintClearPrinter;
            _model.PROXY_PRINT_CLEAR_DELEGATE = res.proxyPrintClearDelegate;

            _model.JOBTICKET_COPIER_ENABLE = res.jobticketCopierEnable;

            _model.JOBTICKET_DOMAINS_RETAIN = res.jobticketDomainsRetain;

            _model.JOBTICKET_DELIVERY_DAYS = res.jobticketDeliveryDays;
            _model.JOBTICKET_DELIVERY_DATE_PRESET = _model.JOBTICKET_DELIVERY_DAYS > 0;
            _model.JOBTICKET_DELIVERY_DAYS_MIN = res.jobticketDeliveryDaysMin;
            _model.JOBTICKET_DELIVERY_DAYS_OF_WEEK = res.jobticketDeliveryDaysOfweek;
            _model.JOBTICKET_DELIVERY_HOUR = res.jobticketDeliveryHour;
            _model.JOBTICKET_DELIVERY_MINUTE = res.jobticketDeliveryMinute;

            _model.DELEGATE_ACCOUNT_SHARED_GROUP = res.delegateAccountSharedGroup;

            if (res.delegatorGroupDetail) {
                _model.DELEGATOR_GROUP_DETAIL = res.delegatorGroupDetail;
            }
            if (res.delegatorUserDetail) {
                _model.DELEGATOR_USER_DETAIL = res.delegatorUserDetail;
            }

            _model.PRINTERS_QUICK_SEARCH_MAX = res.quickSearchMaxPrinters;

            //
            _view.userChartColors = [res.colors.printIn, res.colors.printOut, res.colors.pdfOut];
            _view.imgBase64 = res.img_base64;

            language = _util.getUrlParam(_ns.URL_PARM.LANGUAGE);
            if (!language) {
                language = _model.authToken.language || '';
            }
            country = _util.getUrlParam(_ns.URL_PARM.COUNTRY);
            if (!country) {
                country = _model.authToken.country || '';
            }

            res = _api.call({
                request: 'language',
                language: language,
                country: country
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

            $('body').on('click', '.sp-btn-gdpr-show-dialog', null, function() {
                _view.showUserPageAsync('#page-gdpr-export', 'GdprExport');
                return false;
            });

            $('body').on('click', '.sp-btn-show-librejs', null, function() {
                _view.changePage($('#page-librejs'));
                return false;
            });

            $(document).on('click', '.sp-collapse', null, function() {
                $(this).closest('[data-role=collapsible]').collapsible('collapse');
                return false;
            });

        };

        /*
         * authMode === _view.AUTH_MODE*
         */
        this.login = function(authMode, authId, authPw, authToken, assocCardNumber) {

            _model.user.loggedIn = false;

            _api.callAsync({
                request: 'login',
                dto: JSON.stringify({
                    webAppType: _ns.WEBAPP_TYPE,
                    authMode: authMode,
                    authId: authId,
                    authPw: authPw,
                    authToken: authToken,
                    assocCardNumber: assocCardNumber
                })
            }, function(data) {

                _model.user.loggedIn = (data.result.code === '0');

                if (_model.user.loggedIn) {

                    if (_model.authCardIp) {
                        _deviceEvent.removeListener();
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

                    _ns.startAppWatchdog(false);

                    if (!authMode && authPw) {
                        _ns.Utils.asyncFoo(function() {
                            _view.pages.main.showUserTOTP();
                        });
                    }

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

                        } else if (data.authTOTPRequired) {
                            _view.pages.login.notifyTOTPRequired();
                        } else {
                            _view.pages.login.notifyLoginFailed((assocCardNumber ? null : authMode), data.result.txt);

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
                        }, {
                            webAppType: _ns.WEBAPP_TYPE
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

            var res,
                pub,
                html = '',
                lh;

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
                    request: 'letterhead-get',
                    id: $('#letterhead-list').val(),
                    pub: pub,
                    base64: _view.imgBase64
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
                            ,
                            imgHeightA4 = imgWidth * 1.4
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
                'request': 'letterhead-list'
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
         *            The selector, e.g. '#print-job-list'
         */
        this.setJobScopeMenu = function(sel, isPrintDialog) {
            var keyAll = '-1',
                keySelect =
                    keyAll,
                options = '';

            if (_model.canSelectAllDocuments() && _model.myJobs.length === 1) {
                keySelect = '0';
            } else {
                options = '<option value="' + keyAll + '">' + _i18n.format('scope-all-documents', null) + '</option>';
            }

            if (_model.canSelectAllDocuments()) {
                $.each(_model.myJobs, function(key, value) {
                    options += '<option value="' + key + '">' + value.title + '</option>';
                });
            }

            $(sel).empty().append(options);
            $(sel).val(keySelect).selectmenu('refresh');

            return keySelect;
        };

        /**
         * Sets (refreshed) a letterhead optionmenu from the model.
         *
         * @param sel
         *            The selector, e.g. '#letterhead-list'
         */
        this.setLetterheadMenu = function(sel) {
            var options,
                jSel = $(sel);

            if (jSel.length === 0) {
                return;
            }

            _getLetterheads();

            options = '<option value="none"';

            if (!_model.letterheadDefault) {
                options += ' selected';
            }
            options += '>&ndash;</option>';

            $.each(_model.letterheads, function(key, value) {
                options += '<option value="' + value.id + '"';
                if (_model.myLetterheadIdx === value.id) {
                    options += ' selected';
                }
                options += '>' + value.name + '</option>';
            });

            $(jSel).empty().append(options).selectmenu('refresh');
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
        _handleSafePageEvent = function(res, nPageInView) {

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
                } else {
                    _ns.userEvent.onPollInvitation();
                }

                _view.pages.main.setThumbnails();

                _view.pages.pagebrowser.setImages(nPageInView);

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
        _ns.PanelCommon.api = _api;
        _ns.PanelCommon.userId = _model.user.id;

        _ns.PanelCommon.onDisconnected = function() {
            window.location.reload();
        };

        _ns.PanelCommon.refreshPanelCommon = function(wClass, skipBeforeLoad, thePanel) {
            var jqId = thePanel.jqId,
                data = thePanel.getInput(),
                jsonData = JSON.stringify(data);

            $.mobile.loading("show");
            $.ajax({
                type: "POST",
                async: true,
                url: '/pages/' + wClass + _ns.WebAppTypeUrlParm(),
                data: {
                    user: _ns.PanelCommon.userId,
                    data: jsonData
                }
            }).done(function(html) {
                $(jqId).html(html).enhanceWithin();

                // Hide the top divider with the title
                $(jqId + ' > ul > .ui-li-divider').hide();

                thePanel.onOutput(undefined);
                thePanel.afterload();
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
            window.location.reload();
        });
        // -----------------------------
        // Call-back: polling
        // -----------------------------
        _changeIcon = function(icon, title) {
            if (icon !== _iconCur) {
                $("#button-cometd-status").attr('title', title || '');
                $("#button-cometd-status").buttonMarkup({
                    icon: icon
                });
                _iconCur = icon;
            }
        };

        //--------------------------------------------------
        _userEvent.onWaitingForEvent = function() {
            _changeIcon("check", 'Watching events');
        };

        _cometd.onConnecting = function() {
            _changeIcon("recycle", 'Connecting');
        };

        _cometd.onHandshakeSuccess = function() {
            if (_model.user.loggedIn) {
                _userEvent.addListener();
            } else if (_model.authCardIp) {
                _deviceEvent.addListener();
            }
        };
        _cometd.onHandshakeFailure = function() {
            _changeIcon("alert", 'Handshake failure');
        };
        _cometd.onReconnect = function() {
            if (_userEvent.isLongPollPending()) {
                _changeIcon("check", 'Watching events');
            } else {
                _changeIcon("plus", 'Start event watch');
                _userEvent.onPollInvitation();
            }
        };

        _cometd.onConnectionBroken = function() {
            _changeIcon("delete", 'Connection is broken');
            if (_model.user.loggedIn) {
                if (_ns.isAppWakeUpDeferred()) {
                    _this.onWakeUpAutoRestore(true);
                } else {
                    _this.onWakeUp();
                }
            }
        };
        _cometd.onConnectionClosed = function() {
            /*
             * IMPORTANT: This is end-state in Google Chrome.
             */
            _changeIcon("minus", 'Connection is closed');
        };

        _cometd.onDisconnecting = function() {
            _changeIcon("clock", 'Disconnecting');
        };

        /*
         * As reported by listener on /meta/unsuccessful channel.
         */
        _cometd.onUnsuccessful = function(message) {
            /*
             * #327: We handle this message as redundant: no action needed.
             */
            $.noop();
        };

        //--------------------------------------------------
        _deviceEvent.onCardSwipe = function(cardNumber) {
            _deviceEvent.pause();
            _view.pages.login.notifyCardIp(cardNumber);
        };
        _deviceEvent.onEventError = function(msg) {
            _view.message(msg);
        };
        _deviceEvent.onPollInvitation = function() {
            _deviceEvent.poll(_model.language, _model.country);
        };

        _deviceEvent.onException = function(msg) {
            _ns.logger.warn('DeviceEvent exception: ' + msg);
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
                if (_model.PROXY_PRINT_CLEAR_PRINTER && _model.hasMultiplePrinters) {
                    _view.pages.print.onClearPrinter();
                }
                // Do NOT use $('#button-printer-back').click();
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

            $('#auth-popup-user-msg-title').text(_view.apiResMsgTitle(res.result));
            $('#auth-popup-user-msg-title').attr('class', _view.apiResMsgCssClass(res.result));
            $('#auth-popup-user-msg').attr('class', _view.apiResMsgCssClass(res.result));
            $('#auth-popup-user-msg').text(res.result.txt);
        };

        _proxyprintEvent.onException = function(msg) {
            _ns.logger.warn('ProxyPrintEvent exception: ' + msg);
            _view.message(msg);
        };

        //--------------------------------------------------
        _userEvent.onSysMaintenance = function(msg) {
            _view.pages.main.onLogout();
        };

        _userEvent.onEventError = function(msg) {
            _view.msgDialogBox(msg, 'sp-msg-popup-error');
        };

        _userEvent.onException = function(msg) {
            $.noop();
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

        _userEvent.onMsgEvent = function(data, dialogBox) {
            var msg = '',
                i = 0;

            _model.prevMsgTime = data.msgTime;

            $.each(data.messages, function(key, value) {
                var err = value.level > 0;
                msg += (i > 0 ? '<br>' : '');
                msg += (err ? '<span class="sp-txt-warn">' : '');
                msg += value.text;
                msg += (err ? '</span>' : '');
                i = i + 1;
            });
            if (dialogBox) {
                _view.msgDialogBox(msg, 'sp-msg-popup-warn');
            } else {
                _view.message(msg);
            }
        };

        _userEvent.onPollInvitation = function() {
            _userEvent.poll(_model.user.id, _model.user.doclog.id,
                _model.getPageCount(),
                _model.uniqueImgUrlValue, _model.prevMsgTime,
                _model.language, _model.country, _view.imgBase64,
                _model.user.id_internal, _model.user.doclog.id_internal);
        };

        /*
         *
         */
        _view.onDisconnected(function() {
            window.location.reload();
        });

        /**
         * Callbacks: page language
         */
        _view.pages.language.onSelectLocale(function(lang, country) {
            /*
             * This call sets the locale for the current session and returns
             * strings needed for off-line mode.
             */
            var res = _api.call({
                request: 'language',
                language: lang,
                country: country
            });

            if (res.result.code === "0") {

                _model.setLanguage(lang);
                _model.setCountry(country);

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
                request: 'page-delete',
                ranges: ranges
            }),
                isOk = res.result.code === "0";

            if (isOk) {
                _model.clearJobs();
                _view.pages.main.onRefreshPages();
            }
            _view.showApiMsg(res);
            return isOk;
        };

        _view.pages.pagebrowser.onClear = function(nPage) {
            var res = _api.call({
                request: 'page-delete',
                ranges: nPage
            }),
                isOk = res.result.code === "0";
            if (isOk) {
                _model.refreshUniqueImgUrlValue4Browser();
                _view.pages.main.onExpandPage(nPage - 1);
            }
            _view.showApiMsg(res);
            return isOk;
        };

        _view.pages.pagebrowser.onOverlayOnOff = function() {
            _view.pages.main.refreshPagesOnShow = true;
        };

        /**
         * Callbacks: page redeem voucher
         */
        _view.pages.voucherRedeem.onRedeemVoucher = function(cardNumber) {
            var res = _api.call({
                request: "account-voucher-redeem",
                cardNumber: cardNumber
            });
            if (res.result.code === "0") {
                $("#button-voucher-redeem-back").click();
            }
            _view.showApiMsg(res);
        };

        /**
         * Callbacks: Print Delegation Dialog
         */
        _view.pages.printDelegation.onBeforeHide = function() {
            _refreshPrinterInd();
        };

        _view.pages.printDelegation.onButtonBack = function() {
            $('#button-printer-back').click();
        };

        _view.pages.printDelegation.onButtonNext = function(previousPageId) {
            if (previousPageId !== '#' + _view.pages.main.id) {
                $('#button-print-settings-next').click();
            } else {
                _view.pages.printDelegation.onButtonBack();
            }
        };

        /**
         * Callbacks: File Upload Dialog
         */
        _view.pages.fileUpload.onClear = function() {
            var res = _api.call({
                request: 'inbox-clear'
            }),
                isOk = res.result.code === "0";

            if (isOk) {
                _view.pages.main.onRefreshPages();
            }
            _view.showApiMsg(res);
            return isOk;
        };

        /**
         * Callbacks: page credit transfer
         */
        _view.pages.creditTransfer.onTransferCredit = function(userTo, amountMain, amountCents, comment) {
            // UserCreditTransferDto.java
            var res = _api.call({
                request: "user-credit-transfer",
                dto: JSON.stringify({
                    userFrom: _model.user.id,
                    userTo: userTo,
                    amountMain: amountMain,
                    amountCents: amountCents,
                    comment: comment
                })
            }),
                isOk = res.result.code === "0";

            _view.showApiMsg(res);
            return isOk;
        };

        /**
         * Callbacks: page outbox
         */
        _view.pages.outbox.onOutboxClear = function() {
            var res = _api.call({
                request: 'outbox-clear'
            });
            if (res.result.code === "0") {
                _model.user.stats = res.stats;
                _model.myShowUserStats = true;
            }
            _view.showApiMsg(res);
        };

        _view.pages.outbox.onOutboxExtend = function() {
            var res = _api.call({
                request: 'outbox-extend'
            });
            if (res.result.code === "0") {
                _model.user.stats = res.stats;
                _model.myShowUserStats = true;
            }
            _view.showApiMsg(res);
        };

        _view.pages.outbox.onOutboxDeleteJob = function(jobFileName, isJobTicket) {
            var res = _api.call({
                request: 'outbox-delete-job',
                dto: JSON.stringify({
                    jobFileName: jobFileName,
                    jobTicket: isJobTicket
                })
            });

            if (res.result.code === "0") {
                _model.user.stats = res.stats;
                _model.myShowUserStats = true;
            }
            _view.showApiMsg(res);
        };

        /**
         * Callbacks: money transfer
         */
        _view.pages.moneyTransfer.onMoneyTransfer = function(gatewayId, method, main, cents) {
            // MoneyTransferDto.java
            var res = _api.call({
                request: "user-money-transfer-request",
                dto: JSON.stringify({
                    userId: _model.user.id,
                    gatewayId: gatewayId,
                    method: method,
                    amountMain: main,
                    amountCents: cents,
                    senderUrl: window.location.protocol + "//" + window.location.host + window.location.pathname
                })
            });

            if (res.result.code === '0') {
                window.location.assign(res.paymentUrl);
            } else {
                _view.showApiMsg(res);
            }
        };

        /**
         * Callbacks: page send
         */
        _view.pages.send.onSend = function(mailto, ranges, removeGraphics, ecoprint, grayscale, rasterize) {
            var res;

            if (_util.isEmailValid(mailto)) {
                res = _api.call({
                    request: 'send',
                    mailto: mailto,
                    jobIndex: _model.pdfJobIndex,
                    ranges: ranges,
                    removeGraphics: removeGraphics,
                    ecoprint: ecoprint,
                    grayscale: grayscale,
                    rasterize: rasterize
                });
                if (res.result.code === "0") {
                    _model.user.stats = res.stats;
                    _model.myShowUserStats = true;
                }
                _view.showApiMsg(res);
            } else {
                // Use a "quoted string" since mailto can be empty.
                _view.message(_i18n.format('msg-email-invalid', ['"' + mailto + '"']));
            }
        };

        /**
         * Callbacks: page Pdf properties
         */

        _view.pages.pdfprop.onShow = function() {

            _this.setLetterheadMenu('#pdf-letterhead-list');

            _model.pdfJobIndex = _this.setJobScopeMenu('#pdf-job-list', false);

            $('#pdf-title').val(_model.myInboxTitle);

            _view.checkCb('#pdf-remove-graphics', _model.removeGraphics);
            _view.checkCb('#pdf-ecoprint', _model.ecoprint);
        };

        /**
         * @return true if PDF props saved ok, false is an error occurred.
         */
        _view.pages.pdfprop.onHide = function() {
            if (_model.pdfJobIndex === '-1') {
                _model.myInboxTitle = $('#pdf-title').val();
            }

            _saveSelectedletterhead('#pdf-letterhead-list');
            _saveRemoveGraphics('#pdf-remove-graphics');
            _saveEcoprint('#pdf-ecoprint');
            _savePdfGrayscale('#pdf-grayscale');
            _savePdfRasterize('#pdf-rasterize');

            _model.pdfPageRanges = $('#pdf-page-ranges').val();

            if (!_savePdfProps()) {
                return false;
            }
            return true;
        };

        /**
         * @return true if pre-conditions are OK, false is an error occurred.
         */
        _view.pages.pdfprop.onDownload = function() {
            var pageRanges = $('#pdf-page-ranges').val(),
                filters;

            _saveRemoveGraphics('#pdf-remove-graphics');
            _saveEcoprint('#pdf-ecoprint');
            _savePdfGrayscale('#pdf-grayscale');
            _savePdfRasterize('#pdf-rasterize');

            filters = (_model.removeGraphics ? 1 : 0) + (_model.ecoprint ? 1 : 0);

            if (filters > 1) {
                _view.message(_i18n.format('msg-select-single-pdf-filter', null));
                return false;
            }

            if (!_saveSelectedletterhead('#pdf-letterhead-list', true)) {
                return false;
            }
            if (!_savePdfProps(_model.pdfJobIndex, pageRanges)) {
                return false;
            }
            if (_model.ecoprint && !_userLazyEcoPrint(_model.pdfJobIndex, pageRanges)) {
                return false;
            }

            // Mantis #725
            _ns.deferAppWakeUp(true);

            //
            window.location.assign(_api.getUrl4Pdf(pageRanges, _model.removeGraphics,
                _model.ecoprint, _model.pdfGrayscale, _model.pdfRasterize, _model.pdfJobIndex));
            _model.myShowUserStatsGet = true;
            return true;
        };

        /**
         *
         */
        _view.pages.print.onFastProxyPrintRenew = function(showMsg) {
            var res = _api.call({
                request: 'print-fast-renew'
            }),
                resOk = (res.result.code === '0');

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
                request: 'print-auth-cancel',
                idUser: _model.user.key_id,
                printer: _model.myPrinter.name
            });
            _view.message(res.result.txt);
        };

        /**
         *
         */
        _view.pages.print.onChangeJobTicketType = function(isCopyJob) {
            $.noop();
        };

        /**
         * For now, validate Job Tickets only.
         */
        _view.pages.printSettings.onPrinterOptValidate = function(printerOptions) {
            var res,
                valid = true;

            res = _api.call({
                request: 'printer-opt-validate',
                dto: JSON.stringify({
                    printer: _model.myPrinter.name,
                    options: printerOptions,
                    jobTicketType: _view.getRadioValue('sp-print-jobticket-type')
                })
            });
            valid = res.result.code === '0';
            if (!valid) {
                _view.showApiMsg(res);
            }
            return valid;
        };

        /**
         * Callbacks: page print
         */
        _view.pages.print.onPrint = function(clearScope, isClose, removeGraphics,
            ecoprint, collate, archive, isDelegation, separateDocs, isJobticket,
            jobTicketType, landscapeView, calcCostMode) {

            var res,
                sel,
                cost,
                visible,
                date,
                jobTicketDate,
                jobTicketDomain,
                jobTicketUse,
                jobTicketTag,
                accountId,
                isJobTicketDateTime = $('#sp-jobticket-date').length > 0,
                copies = isDelegation ? "1" : (isJobticket ? $('#number-print-copies').val() : $('#slider-print-copies').val());

            if (_saveSelectedletterhead('#print-letterhead-list')) {
                return;
            }

            if (_model.isMediaSourceAuto() && !_model.isMediaSourceMatch()) {
                _view.msgDialogBox(_i18n.format('msg-select-media-source'), 'sp-msg-popup-warn');
                return;
            }

            if (_model.myPrinter.jobTicket) {
                if (isJobTicketDateTime) {
                    sel = $('#sp-jobticket-date');
                    date = _view.mobipickGetDate(sel);
                    jobTicketDate = sel.val().length > 0 ? date.getTime() : null;
                } else {
                    jobTicketDate = null;
                }
            }

            if (!isDelegation) {
                accountId = $('#sp-print-shared-account-list').val();
            }

            sel = $('#sp-jobticket-domain-list');
            jobTicketDomain = _model.myPrinter.jobTicketLabelsEnabled && sel.length > 0 ? sel.val() : null;

            sel = $('#sp-jobticket-use-list');
            jobTicketUse = _model.myPrinter.jobTicketLabelsEnabled && sel.length > 0 ? sel.val() : null;

            sel = $('#sp-jobticket-tag-list');
            jobTicketTag = _model.myPrinter.jobTicketLabelsEnabled && sel.length > 0 ? sel.val() : null;

            _model.myPrintTitle = $('#print-title').val();

            res = _api.call({
                request: 'printer-print',
                dto: JSON.stringify({
                    calcCostMode: calcCostMode,
                    user: _model.user.id,
                    accountId: accountId,
                    printer: _model.myPrinter.name,
                    readerName: _model.myPrinterReaderName,
                    jobName: _model.myPrintTitle,
                    jobIndex: _model.printJobIndex,
                    landscapeView: landscapeView,
                    pageScaling: _model.printPageScaling,
                    copies: parseInt(copies, 10),
                    ranges: $('#print-page-ranges').val(),
                    collate: collate,
                    removeGraphics: removeGraphics,
                    ecoprint: ecoprint,
                    clearScope: clearScope,
                    separateDocs: separateDocs,
                    archive: archive,
                    options: _model.myPrinterOpt,
                    delegation: isDelegation ? _model.printDelegation : null,
                    jobTicketDomain: jobTicketDomain,
                    jobTicketUse: jobTicketUse,
                    jobTicketTag: jobTicketTag,
                    jobTicket: isJobticket,
                    jobTicketCopyPages: isJobticket ? $('#sp-jobticket-copy-pages').val() : null,
                    jobTicketType: jobTicketType,
                    jobTicketDate: jobTicketDate,
                    jobTicketHrs: isJobticket && isJobTicketDateTime ? $('#sp-jobticket-hrs').val() : null,
                    jobTicketMin: isJobticket && isJobTicketDateTime ? $('#sp-jobticket-min').val() : null,
                    jobTicketRemark: isJobticket ? $('#sp-jobticket-remark').val() : null
                })
            });
            if (calcCostMode) {
                return res;
            }
            if (res.requestStatus === 'NEEDS_AUTH') {

                _view.pages.print.clearInput();
                _model.closePrintDlg = isClose;

                _proxyprintEvent.poll(_model.user.key_id, _model.myPrinter.name,
                    _model.myPrinterReaderName, _model.language, _model.country);

                _view.visible($('#auth-popup-content-wait'), true);
                _view.visible($('#auth-popup-content-msg'), false);

                // Financial.
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

                // Countdown start.
                $('#sp-print-auth-countdown').text(res.printAuthExpirySecs);

                $('#sp-popup-print-auth').popup('open', {});

                // Countdown timer.
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

                if (res.clearDelegate) {
                    _view.pages.printDelegation.clear();
                }

                if (isClose) {
                    $('#button-printer-back').click();
                }
                if (clearScope !== null) {
                    _view.pages.main.onRefreshPages();
                }

                _model.user.stats = res.stats;
                _model.myShowUserStats = true;
            }

            if (isJobticket && res.result.code === '0') {
                //Perform next step when this event is done.
                window.setTimeout(function() {
                    _view.msgDialogBox(res.result.txt, 'sp-msg-popup-info');
                }, 100);

            } else {
                _view.showApiMsg(res);
            }

        };

        /**
         *
         */
        _view.pages.print.onPrinter = function(printerName) {
            var res,
                retValue = true;

            if (!_model.myPrinter || _model.myPrinter.name !== printerName) {

                res = _api.call({
                    'request': 'printer-detail',
                    'printer': printerName
                });

                if (res.result.code === '0') {
                    _model.myPrinter = res.printer;
                    _model.setPrinterDefaults();
                    _model.myFirstPageShowPrintSettings = true;

                    _view.visible($('#button-print-settings'), _model.myPrinter.groups.length > 0);

                    /* This method might be called to implicitly select a
                     * single printer, so printer settings are shown
                     * immediately (just before the page-print is shown).
                     * The asyncFoo wrapper is used, to satisfy iOS,
                     * otherwise the settings will not show.
                     */
                    _ns.Utils.asyncFoo(function() {
                        _view.showUserPageAsync('#page-printer-settings', 'PrinterSettings');
                    });

                } else {
                    retValue = false;
                    _view.showApiMsg(res);
                }

            }

            _prepareReaderForPrinter();

            return retValue;
        };

        /**
         * TODO: looks very much like onPrinter() function above
         */
        _view.pages.print.onSettings = function(printerName) {
            var res;

            _model.preservePrintJobSettings = true;

            if (_model.myPrinter.name !== printerName) {

                res = _api.call({
                    'request': 'printer-detail',
                    'printer': printerName
                });

                if (res.result.code === '0') {
                    _model.myPrinter = res.printer;
                    _model.setPrinterDefaults();
                    _model.myFirstPageShowPrintSettings = true;
                }
            }
            _view.showUserPageAsync('#page-printer-settings', 'PrinterSettings');
        };

        _view.pages.print.onShowPrintDelegation = function() {
            _view.showPageAsync('#page-print-delegation', 'PagePrintDelegation');
        };

        _view.pages.print.onClearPrinter = function() {
            _model.myPrinter = undefined;
            _model.isPrintManualFeed = false;
            _model.printPageScaling = _model.PRINT_SCALING_MATCH_DFLT.value;
            _model.printMediaClashCurrent = undefined;

            _model.showJobsMatchMediaSources(_view);
            _model.showJobsMatchMedia(_view);
            _model.showCopyJobMedia(_view);

            _model.myPrinterOpt = {};
            _refreshPrinterInd();
        };

        _view.pages.print.onClearPrinterInd = function() {
            _refreshPrinterInd();
        };

        _view.pages.print.onShow = function() {

            if (_model.preservePrintJobSettings) {
                _model.preservePrintJobSettings = false;
                _refreshPrinterInd();
                return;
            }

            if (_model.myFirstPageShowPrint) {
                _model.showJobsMatchMediaSources(_view);
                _model.showCopyJobMedia(_view);

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
            _view.checkCb('#print-ecoprint', _model.ecoprint);

            _this.setLetterheadMenu('#print-letterhead-list');

            _model.printJobIndex = _this.setJobScopeMenu('#print-job-list', true);
            $('#print-title').val(_model.myInboxTitle);

            // Refreshes display of possible changed inbox media.
            _model.showJobsMatchMedia(_view);

            // When opened from SafePage Sort mode, selected page ranges are
            // filled.
            $('#print-page-ranges').val(_model.getSelectPageRanges());

            _model.myShowPrinterInd = true;

            _refreshPrinterInd();
        };

        _view.pages.print.onHide = function() {

            if (_model.printJobIndex === '-1') {
                _model.myInboxTitle = $('#print-title').val();
            }

            _refreshPrinterInd();
        };

        /**
         * Callbacks: page letterhead
         */

        _view.pages.letterhead.onApply = function(id, name, fg, pub, pubNew) {
            var res = _api.call({
                request: 'letterhead-set',
                id: id,
                data: JSON.stringify({
                    name: name,
                    foreground: fg,
                    pub: pub,
                    'pub-new': pubNew
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
         *
         */
        _userLazyEcoPrint = function(jobIndex, ranges) {
            var res = _api.call({
                request: 'user-lazy-ecoprint',
                jobIndex: jobIndex,
                ranges: ranges
            });
            if (res.result.code === "0") {
                return true;
            }
            _view.showApiMsg(res);
            return false;
        };

        /**
         * Saves the PDF properties.
         *
         * @return true if saved ok, false is an error occurred
         */
        _savePdfProps = function(iJob, ranges) {
            var res = _api.call({
                request: 'pdf-set-properties',
                props: JSON.stringify(_model.propPdf),
                jobIndex: iJob,
                ranges: ranges
            });
            _view.showApiMsg(res);
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
            var pub,
                res = null,
                ret = false,
                lh,
                letterheadIdx = $(sel + ' :selected').val() || 'none';

            if (!force && (letterheadIdx === _model.myLetterheadIdx)) {
                return;
            }

            if (letterheadIdx === 'none') {
                res = _api.call({
                    'request': 'letterhead-detach'
                });
            } else {
                lh = _model.getSelLetterheadObj(sel);
                if (lh) {
                    pub = lh.pub;
                    res = _api.call({
                        'request': 'letterhead-attach',
                        'id': letterheadIdx,
                        pub: pub
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
            var pub = _view.pages.letterhead.getSelected().pub,
                res = _api.call({
                    request: 'letterhead-delete',
                    id: id,
                    pub: pub
                });
            _view.showApiMsg(res);
            if (res.result.code === '0') {
                _refreshLetterheadList();
            }
        };

        // ----------------
        _view.pages.letterhead.onCreate = function() {
            var res = _api.call({
                'request': 'letterhead-new'
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
            var res = _api.call({
                request: 'logout',
                dto: JSON.stringify({
                    authToken: _model.authToken.token
                })
            });

            _view.pages.login.notifyLogout();

            if (res.result.code !== '0') {
                /*
                 * NOTE: when we are disconnected the onDisconnected()
                 * callback is called, which displays the login window. The
                 * BACK button WILL work in this case. See Mantis #108 how to
                 * prevent this.
                 *
                 * Is there a way to unload the whole application !??
                 * window.location.reload(true) does NOT work (why?)
                 */

                /*
                 * Do NOT use _view.showApiMsg(res), because the login dialog
                 * will not show.
                 */
                _view.message(res.result.txt);
                _model.setAuthToken(null, null, null);
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

            // first statement
            _ns.deferAppWakeUp(false);

            if (this.refreshPagesOnShow) {
                this.refreshPagesOnShow = false;
                _model.refreshUniqueImgUrlValueTn();
                this.onExpandPage(0);
            }

            _view.pages.main.alignThumbnails();

            _userEvent.resume();

            if (_model.myShowUserStats || _model.myShowUserStatsGet) {
                _model.myShowUserStats = false;
                if (_model.myShowUserStatsGet) {
                    _model.myShowUserStatsGet = false;
                    res = _api.call({
                        'request': 'user-get-stats'
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
                'request': 'page-move',
                'ranges': ranges,
                'position': position
            });
            if (data.result.code === "0") {
                $('#main-arr-undo').click();
                _view.pages.main.onRefreshPages();
            }
            _view.showApiMsg(data);
        };

        _view.pages.main.onPageDelete = function(ranges) {
            var data = _api.call({
                'request': 'page-delete',
                'ranges': ranges
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
                request: 'inbox-job-pages',
                'first-detail-page': nPage,
                'unique-url-value': _model.uniqueImgUrlValue,
                base64: _view.imgBase64
            });
            if (data.result.code === '0') {
                _handleSafePageEvent(data, nPage);
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
                request: 'exit-event-monitor'
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
                'request': 'inbox-job-edit',
                ijob: _model.iPopUpJob,
                data: JSON.stringify({
                    rotate: $('#sp-popup-job-rotate').is(':checked'),
                    undelete: $('#sp-popup-job-undelete').is(':checked')
                })
            });
            if (data.result.code === '0') {
                $('#sp-popup-job-info').popup('close');
                _view.pages.main.onRefreshPages();
            }
            _view.showApiMsg(data);
        };

        /**
         *
         */
        _view.pages.main.onPopupJobDelete = function() {
            var data = _api.call({
                'request': 'inbox-job-delete',
                ijob: _model.iPopUpJob
            });
            if (data.result.code === '0') {
                $('#sp-popup-job-info').popup('close');
                _view.pages.main.onRefreshPages();
            }
            _view.showApiMsg(data);
        };

        /**
         *
         */
        _view.pages.main.onShowPrintDialog = function() {
            if (_checkVanillaJobs()) {
                _model.isPrintDialogFromMain = true;
                _view.showUserPageAsync('#page-print', 'Print');
            }
        };

        /**
         *
         */
        _view.pages.main.onShowPdfDialog = function() {
            if (_checkVanillaJobs()) {
                _view.showUserPageAsync('#page-pdf-properties', 'PdfProperties');
            }
        };

        /**
         * Callbacks: page User Password Reset
         */
        _view.pages.userPwReset.onSelectReset(function(password) {
            var res = _api.call({
                request: 'reset-user-pw',
                iuser: _model.user.id,
                password: password
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
                request: 'reset-user-pin',
                user: _model.user.id,
                pin: pin
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
        var _i18n = new _ns.I18n(),
            _model = new Model(),
            _api = new _ns.Api(_i18n, _model.user),
            _view = new _ns.View(_i18n, _api),
            _cometd,
            _userEvent,
            _deviceEvent,
            _proxyprintEvent,
            _ctrl,
            _nativeLogin;

        _ns.commonWebAppInit();

        _view.pages = {
            language: new _ns.PageLanguage(_i18n, _view, _model),
            login: new _ns.PageLogin(_i18n, _view, _api),
            letterhead: new PageLetterhead(_i18n, _view, _model),
            clear: new PageClear(_i18n, _view, _model),
            accountTrx: new PageAccountTrx(_i18n, _view, _model, _api),
            doclog: new PageDocLog(_i18n, _view, _model, _api),
            outbox: new PageOutbox(_i18n, _view, _model, _api),
            send: new PageSend(_i18n, _view, _model),
            pagebrowser: new PageBrowser(_i18n, _view, _model, _api),
            pageDashboard: new PageDashboard(_i18n, _view, _model, _api),
            voucherRedeem: new PageVoucherRedeem(_i18n, _view, _model),
            creditTransfer: new PageCreditTransfer(_i18n, _view, _model),
            moneyTransfer: new PageMoneyTransfer(_i18n, _view, _model),
            pdfprop: new PagePdfProp(_i18n, _view, _model),
            main: new PageMain(_i18n, _view, _model, _api),
            print: new PagePrint(_i18n, _view, _model, _api),
            printDelegation: new _ns.PagePrintDelegation(_i18n, _view, _model, _api),
            printSettings: new PagePrintSettings(_i18n, _view, _model),
            fileUpload: new PageFileUpload(_i18n, _view, _model),
            userPinReset: new PageUserPinReset(_i18n, _view, _model),
            userInternetPrinter: new PageUserInternetPrinter(_i18n, _view, _model, _api),
            totpUser: new PageTOTPUser(_i18n, _view, _model, _api),
            userTelegram: new PageUserTelegram(_i18n, _view, _model, _api),
            userPwReset: new _ns.PageUserPasswordReset(_i18n, _view, _model),
            gdprExport: new PageGdprExport(_i18n, _view, _model, _api)
        };

        _cometd = new _ns.Cometd();
        _userEvent = new UserEvent(_cometd, _api);

        // Mantis #320
        _ns.cometd = _cometd;
        _ns.userEvent = _userEvent;
        _ns.model = _model;
        //

        _ns.view = _view;
        _ns.api = _api;

        //
        _deviceEvent = new DeviceEvent(_cometd);
        _proxyprintEvent = new ProxyPrintEvent(_cometd);

        _ctrl = new Controller(_i18n, _model, _view, _api, _cometd, _userEvent, _deviceEvent, _proxyprintEvent);

        _nativeLogin = function(user, authMode) {
            if (user) {
                _ctrl.login(_view.AUTH_MODE_NAME, user, null, _model.authToken.token);
            } else if (_model.authToken.user && _model.authToken.token) {
                _ctrl.login(_view.AUTH_MODE_NAME, _model.authToken.user, null, _model.authToken.token);
            } else {
                _ctrl.initLoginUserInput();
                _view.pages.login.loadShow(_ns.WEBAPP_TYPE);
            }
        };

        this.init = function() {
            var user = _ns.Utils.getUrlParam(_ns.URL_PARM.USER),
                authMode = _ns.Utils.getUrlParam(_ns.URL_PARM.LOGIN),
                // Note: getUrlParam() does not work properly for LOGIN_OAUTH
                isLoginOAuth = _ns.Utils.hasUrlParam(_ns.URL_PARM.LOGIN_OAUTH);

            // Java: WebAppTypeEnum
            if (window.location.pathname === '/mailtickets') {
                _model.setMailTicketsLocalStorageParms();
                _ns.initWebApp('MAILTICKETS');
            } else if (window.location.pathname === '/payment') {
                _model.setPaymentLocalStorageParms();
                _ns.initWebApp('PAYMENT');
            } else {
                _ns.initWebApp('USER');
            }

            _ctrl.init();

            if (isLoginOAuth) {
                _ctrl.login(_view.AUTH_MODE_OAUTH);
            } else {
                _nativeLogin(user, authMode);
            }

        };

        $(window).on('beforeunload', function() {
            // By NOT returning anything the unload dialog will not show.
            $.noop();
        }).on('unload', function() {
            _api.unloadWebApp();
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
        try {
            $.savapage.init();
        } catch (e) {
            _ns.onLoadException();
        }
    });

}(jQuery, this, this.document, JSON, this.org.savapage));

// @license-end
