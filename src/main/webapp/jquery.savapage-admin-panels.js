/*! SavaPage jQuery Mobile Admin Panels | (c) 2011-2017 Datraverse B.V. | GNU
 * Affero General Public License */

/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */

/*
 * NOTE: the *! comment blocks are part of compressed version.
 */

/*
 * SavaPage jQuery Mobile Admin Panels
 */
( function($, window, document, JSON, _ns) {
        "use strict";

        /*jslint browser: true*/
        /*global $, jQuery, alert*/

        /**
         *
         */
        _ns.PanelAccountVoucherBase = {

            applyDefaults : function(my) {
                my.input.page = 1;
                my.input.maxResults = 10;

                my.input.select.batch = null;
                my.input.select.number = null;
                my.input.select.userId = null;

                my.input.select.dateFrom = null;
                my.input.select.dateTo = null;

                // boolean
                my.input.select.used = null;
                // boolean
                my.input.select.expired = null;

                // NUMBER, VALUE, USER, DATE_USED, DATE_EXPIRED
                my.input.sort.field = 'NUMBER';
                my.input.sort.ascending = true;

            },

            onVoucherSelectBatch : function(batch) {
                var sel = $(".sp-voucher-batch-selected ");
                if (batch !== "0") {
                    sel.show();
                } else {
                    sel.hide();
                }
            },

            beforeload : function(my) {
                my.applyDefaults(my);
            },

            afterload : function(my) {
                var _view = _ns.PanelCommon.view;
                _view.mobipick($("#sp-voucher-date-from"));
                _view.mobipick($("#sp-voucher-date-to"));
                my.m2v(my);
                my.page(my, my.input.page);
            },

            getBatch : function() {
                var sel = $('#sp-voucher-batch')
                //
                ,
                    present = (sel.val() !== "0")
                //
                ;
                return ( present ? sel && sel.val() : null);
            },

            getCardDesign : function() {
                return $("#sp-voucher-card-print-format").val();
            },

            m2v : function(my) {
                var id,
                    val,
                    _view = _ns.PanelCommon.view;

                val = my.input.select.batch;
                val = (val === null ? "0" : val);
                $('#sp-voucher-batch').val(val).selectmenu('refresh');
                my.onVoucherSelectBatch(val);

                $('#sp-voucher-number').val(my.input.select.number);
                $('#sp-voucher-user').val(my.input.select.userId);

                _view.mobipickSetDate($('#sp-voucher-date-from'), my.input.select.dateFrom);
                _view.mobipickSetDate($('#sp-voucher-date-to'), my.input.select.dateTo);

                //
                val = my.input.select.used;
                _view.checkRadioValue('sp-voucher-select-used', val === null ? "" : ( val ? "1" : "0"));

                //
                val = my.input.select.expired;
                _view.checkRadioValue('sp-voucher-select-expired', val === null ? "" : ( val ? "1" : "0"));

                //
                id = 'sp-voucher-sort-by';
                _view.checkRadioValue(id, my.input.sort.field);

                id = 'sp-voucher-sort-dir';
                _view.checkRadio(id, my.input.sort.ascending ? id + '-asc' : id + '-desc');

            },

            v2m : function(my) {
                var _view = _ns.PanelCommon.view
                //
                ,
                    val
                //
                ,
                    sel = $('#sp-voucher-date-from')
                //
                ,
                    date = _view.mobipickGetDate(sel)
                //
                ,
                    present = (sel.val().length > 0);

                my.input.select.dateFrom = ( present ? date.getTime() : null);

                sel = $('#sp-voucher-date-to');
                date = _view.mobipickGetDate(sel);
                present = (sel.val().length > 0);
                my.input.select.dateTo = ( present ? date.getTime() : null);

                //
                my.input.select.batch = my.getBatch();

                //
                sel = $('#sp-voucher-number');
                present = (sel.val().length > 0);
                my.input.select.number = ( present ? sel.val() : null);

                //
                sel = $('#sp-voucher-user');
                present = (sel.val().length > 0);
                my.input.select.userId = ( present ? sel.val() : null);

                //
                val = _view.getRadioValue('sp-voucher-select-used');
                my.input.select.used = (val === "" ? null : val === "1");

                //
                val = _view.getRadioValue('sp-voucher-select-expired');
                my.input.select.expired = (val === "" ? null : val === "1");

                //
                my.input.sort.field = _view.getRadioValue('sp-voucher-sort-by');
                my.input.sort.ascending = _view.isRadioIdSelected('sp-voucher-sort-dir', 'sp-voucher-sort-dir-asc');
            },

            // JSON input
            input : {
                page : 1,
                maxResults : 10,
                select : {
                    batch : null,
                    number : null,
                    userId : null,
                    dateFrom : null,
                    dateTo : null,
                    // boolean
                    used : null,
                    // boolean
                    expired : null
                },
                sort : {
                    // NUMBER, VALUE, USER, DATE_USED, DATE_EXPIRED
                    field : 'NUMBER',
                    ascending : true
                }
            },

            // JSON output
            output : {
                lastPage : null,
                nextPage : null,
                prevPage : null
            },

            refresh : function(my, skipBeforeLoad) {
                _ns.PanelCommon.refreshPanelAdmin('AccountVoucherBase', skipBeforeLoad);
            },

            // show page
            page : function(my, nPage) {
                _ns.PanelCommon.onValidPage(function() {
                    my.input.page = nPage;
                    my.v2m(my);
                    _ns.PanelCommon.loadListPageAdmin(my, 'AccountVoucherPage', '#sp-voucher-list-page');
                });
            },

            getInput : function(my) {
                return my.input;
            },

            onOutput : function(my, output) {

                my.output = output;
                /*
                 * NOTICE the $().one() construct. Since the page get reloaded
                 * all
                 * the time, we want a single-shot binding.
                 */
                $(".sp-voucher-page").one('click', null, function() {
                    my.page(my, parseInt($(this).text(), 10));
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-voucher-page-next").one('click', null, function() {
                    my.page(my, my.output.nextPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-voucher-page-prev").one('click', null, function() {
                    my.page(my, my.output.prevPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

            }
        };

        /**
         *
         */
        _ns.PanelAppLogBase = {

            applyDefaults : function(my) {
                my.input.page = 1;
                my.input.maxResults = 10;

                my.input.select.text = null;

                my.input.select.level = null;
                // INFO | WARN | ERROR
                my.input.select.date_from = null;
                my.input.select.date_to = null;

                my.input.sort.field = 'DATE';
                // LEVEl | DATE
                my.input.sort.ascending = false;

            },

            beforeload : function(my) {
                my.applyDefaults(my);
            },

            afterload : function(my) {
                var _view = _ns.PanelCommon.view;
                _view.mobipick($("#sp-applog-date-from"));
                _view.mobipick($("#sp-applog-date-to"));
                my.m2v(my);
                my.page(my, my.input.page);
            },

            m2v : function(my) {
                var id,
                    _view = _ns.PanelCommon.view;

                $('#sp-applog-containing-text').val(my.input.select.text);

                _view.mobipickSetDate($('#sp-applog-date-from'), my.input.select.date_from);
                _view.mobipickSetDate($('#sp-applog-date-to'), my.input.select.date_to);

                if (my.input.select.level) {
                    _view.checkRadioValue('sp-applog-select-level', my.input.select.level);
                } else {
                    _view.checkRadio('sp-applog-select-level', 'sp-applog-select-level-all');
                }

                id = 'sp-applog-sort-by';
                _view.checkRadioValue(id, my.input.sort.field);

                id = 'sp-applog-sort-dir';
                _view.checkRadio(id, my.input.sort.ascending ? id + '-asc' : id + '-desc');

            },

            v2m : function(my) {
                var _view = _ns.PanelCommon.view
                //
                ,
                    level = _view.getRadioValue('sp-applog-select-level')
                //
                ,
                    sel = $('#sp-applog-date-from')
                //
                ,
                    date = _view.mobipickGetDate(sel)
                //
                ,
                    present = (sel.val().length > 0);

                my.input.select.date_from = ( present ? date.getTime() : null);

                sel = $('#sp-applog-date-to');
                date = _view.mobipickGetDate(sel);
                present = (sel.val().length > 0);
                my.input.select.date_to = ( present ? date.getTime() : null);

                sel = $('#sp-applog-containing-text');
                present = (sel.val().length > 0);
                my.input.select.text = ( present ? sel.val() : null);

                my.input.select.level = (level === "" ? null : level);

                my.input.sort.field = _view.getRadioValue('sp-applog-sort-by');

                my.input.sort.ascending = _view.isRadioIdSelected('sp-applog-sort-dir', 'sp-applog-sort-dir-asc');
            },

            // JSON input
            input : {
                page : 1,
                maxResults : 10,
                select : {
                    // null (all) | 0 (info) | 1 (warn) | 2 (error)
                    // See Java:
                    // org.savapage.core.domain.AppLog.Level
                    level : null,
                    date_from : null,
                    date_to : null,
                    text : null
                },
                sort : {
                    field : 'DATE', // DATE | LEVEL
                    ascending : false
                }
            },

            // JSON output
            output : {
                lastPage : null,
                nextPage : null,
                prevPage : null
            },

            refresh : function(my, skipBeforeLoad) {
                _ns.PanelCommon.refreshPanelAdmin('AppLogBase', skipBeforeLoad);
            },

            // show page
            page : function(my, nPage) {
                _ns.PanelCommon.onValidPage(function() {
                    my.input.page = nPage;
                    my.v2m(my);
                    _ns.PanelCommon.loadListPageAdmin(my, 'AppLogPage', '#sp-applog-list-page');
                });
            },

            getInput : function(my) {
                return my.input;
            },

            onOutput : function(my, output) {

                my.output = output;
                /*
                 * NOTICE the $().one() construct. Since the page get
                 * reloaded all the time, we want a single-shot binding.
                 */
                $(".sp-applog-page").one('click', null, function() {
                    my.page(my, parseInt($(this).text(), 10));
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-applog-page-next").one('click', null, function() {
                    my.page(my, my.output.nextPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-applog-page-prev").one('click', null, function() {
                    my.page(my, my.output.prevPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

            }
        };

        /**
         *
         */
        _ns.PanelConfigPropBase = {

            applyDefaults : function(my) {
                my.input.page = 1;
                my.input.maxResults = 10;
                my.input.sort.ascending = true;
                my.input.select.text = null;
            },

            beforeload : function(my) {
                $.noop();
            },

            afterload : function(my) {
                my.m2v(my);
                my.page(my, my.input.page);
            },

            m2v : function(my) {
                var val = my.input.select.text
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;
                $('#sp-config-containing-text').val(val || '');

                _view.checkRadio('sp-config-sort-dir', 'sp-config-sort-dir-asc');
            },

            v2m : function(my) {
                var sel = $('#sp-config-containing-text'),
                    present = (sel.val().length > 0)
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;
                my.input.select.text = ( present ? sel.val() : null);

                my.input.sort.ascending = _view.isRadioIdSelected('sp-config-sort-dir', 'sp-config-sort-dir-asc');
            },

            // JSON input
            input : {
                page : 1,
                maxResults : 10,
                select : {},
                sort : {
                    ascending : true
                }
            },

            // JSON output
            output : {
                lastPage : null,
                nextPage : null,
                prevPage : null
            },

            refresh : function(my, skipBeforeLoad) {
                _ns.PanelCommon.refreshPanelAdmin('ConfigPropBase', skipBeforeLoad);
            },

            // show page
            page : function(my, nPage) {
                _ns.PanelCommon.onValidPage(function() {
                    my.input.page = nPage;
                    my.v2m(my);
                    _ns.PanelCommon.loadListPageAdmin(my, 'ConfigPropPage', '#sp-config-list-page');
                });
            },

            getInput : function(my) {
                return my.input;
            },

            onOutput : function(my, output) {

                my.output = output;
                /*
                 * NOTICE the $().one() construct. Since the page get
                 * reloaded all the time, we want a single-shot binding.
                 */
                $(".sp-config-page").one('click', null, function() {
                    my.page(my, parseInt($(this).text(), 10));
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-config-page-next").one('click', null, function() {
                    my.page(my, my.output.nextPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-config-page-prev").one('click', null, function() {
                    my.page(my, my.output.prevPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

            }
        };

        /**
         *
         */
        _ns.PanelDashboard = {

            // Parameter to be filled
            model : null,

            // Refresh every minute.
            REFRESH_MSEC : 60000,

            // The saved scroll position.
            scrollTop : null,

            piechart : null,
            xychart : null,

            timeout : null,

            destroyPlots : function(my) {
                if (my.piechart) {
                    my.piechart.destroy();
                    my.piechart = null;
                }
                if (my.xychart) {
                    my.xychart.destroy();
                    my.xychart = null;
                }
            },

            /**
             * Retrieves plot data and shows them.
             * @return true when success, false when connection errors.
             */
            showPlots : function(my) {
                var _view = _ns.PanelCommon.view,
                    xydata,
                    piedata;

                xydata = _view.jqPlotData('dashboard-xychart', true);
                if (!xydata) {
                    return false;
                }
                piedata = _view.jqPlotData('dashboard-piechart', true);
                if (!piedata) {
                    return false;
                }

                my.xychart = _view.showXyChart('dashboard-xychart', xydata);
                my.piechart = _view.showPieChart('dashboard-piechart', piedata);

                return true;
            },

            /*
             * A refresh of the WHOLE panel.
             */
            refresh : function(my, skipBeforeLoad) {
                /*
                 * As an extra check we use #live-messages as indicator
                 * that the Dashboard is in view.
                 */
                if ($('#live-messages').length > 0) {
                    my.scrollTop = $(window).scrollTop();
                    _ns.PanelCommon.refreshPanelAdmin('Dashboard', skipBeforeLoad);
                }
            },

            /*
             * A refresh of the PART of the panel.
             */
            refreshSysStatus : function() {

                var scrollTop,
                    heightXyChart,
                    heightPieChart,
                    html
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ,
                    my = _ns.PanelDashboard;

                /*
                 * As an extra check we use #live-messages as indicator
                 * that the Dashboard is in view.
                 */
                if ($('#live-messages').length > 0) {

                    /*
                     * Save scrolling position.
                     */
                    scrollTop = $(window).scrollTop();

                    /*
                     * Save the height of current plots.
                     */
                    heightXyChart = $('#dashboard-xychart').height();
                    heightPieChart = $('#dashboard-piechart').height();

                    /*
                     * IMPORTANT: Release all resources occupied by the jqPlots.
                     * NOT releasing introduces a HUGE memory leak, each time
                     * the plots are refreshed.
                     */
                    my.destroyPlots(my);

                    /*
                     * Load the html.
                     */
                    html = _view.getAdminPageHtml('SystemStatusAddin');

                    if (!html) {
                        // Failure (and disconnected): stop the timer!
                        my.onUnload(my);
                        return;
                    }

                    $('#dashboard-system-status-addin').html(html).enhanceWithin();

                    /*
                     * Be sure height stays the same.
                     */
                    $('#dashboard-xychart').height(heightXyChart);
                    $('#dashboard-piechart').height(heightPieChart);

                    /*
                     * Restore scrolling position.
                     */
                    $(window).scrollTop(scrollTop);

                    /*
                     * Show the plots.
                     */
                    if (!my.showPlots(my)) {
                        // Failure (and disconnected): stop the timer!
                        my.onUnload(my);
                    }
                }
            },

            beforeload : function(my) {
                /*
                 * IMPORTANT: Release all resources occupied by the
                 * jqPlots.
                 * NOT releasing introduces a HUGE memory leak, each time
                 * the plots are refreshed.
                 */
                my.destroyPlots(my);
            },

            afterload : function(my) {

                var i,
                    j,
                    _model = my.model;

                for ( i = 0; i < _model.pubMsgStack.length; i++) {
                    $('#live-messages').append(_model.pubMsgAsHtml(_model.pubMsgStack[i]));
                }
                // Fill it up...
                for ( j = i; j < _model.MAX_PUB_MSG; j++) {
                    $('#live-messages').append('<div>&nbsp;</div>');
                }

                if (my.showPlots(my)) {
                    /*
                     * Interval timer: refresh of part.
                     */
                    my.timeout = window.setInterval(my.refreshSysStatus, my.REFRESH_MSEC);
                }
            },

            onResize : function(my) {
                var selXyChart = $('#dashboard-xychart')
                //
                ,
                    selPieChart = $('#dashboard-piechart')
                //
                ;

                if (selXyChart && selPieChart) {

                    selXyChart.width(selXyChart.parent().width());
                    my.xychart.replot({
                        resetAxes : true
                    });

                    selPieChart.width(selPieChart.parent().width());
                    my.piechart.replot({});

                }

            },

            onUnload : function(my) {
                /*
                 * Clear the timer asap.
                 */
                window.clearTimeout(my.timeout);
            }
        };

        /**
         *
         */
        _ns.PanelDevicesBase = {

            applyDefaults : function(my) {
                my.input.page = 1;
                my.input.maxResults = 10;
                my.input.sort.ascending = true;
                my.input.select.text = null;
                // Boolean
                my.input.select.disabled = null;
                // Boolean
                my.input.select.reader = null;
            },

            beforeload : function(my) {
                $.noop();
            },

            afterload : function(my) {
                my.m2v(my);
                my.page(my, my.input.page);
            },

            m2v : function(my) {
                var id,
                    val
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;
                $('#sp-device-containing-text').val(my.input.select.text);

                val = my.input.select.disabled;
                _view.checkRadioValue("sp-device-select-status", val === null ? "" : ( val ? "0" : "1"));

                val = my.input.select.reader;
                _view.checkRadioValue("sp-device-select-type", val === null ? "" : ( val ? "1" : "0"));

                id = 'sp-device-sort-dir';
                _view.checkRadio(id, my.input.sort.ascending ? id + '-asc' : id + '-desc');

            },

            v2m : function(my) {
                var val,
                    sel = $('#sp-device-containing-text')
                //
                ,
                    present = (sel.val().length > 0)
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;

                my.input.select.text = ( present ? sel.val() : null);

                val = _view.getRadioValue("sp-device-select-status");
                my.input.select.disabled = (val === "" ? null : (val === "0"));

                val = _view.getRadioValue("sp-device-select-type");
                my.input.select.reader = (val === "" ? null : (val === "1"));

                my.input.sort.ascending = _view.isRadioIdSelected('sp-device-sort-dir', 'sp-device-sort-dir-asc');
            },

            // JSON input
            input : {
                page : 1,
                maxResults : 10,
                select : {
                    disabled : null,
                    reader : null
                },
                sort : {
                    ascending : true
                }
            },

            // JSON output
            output : {
                lastPage : null,
                nextPage : null,
                prevPage : null
            },

            refresh : function(my, skipBeforeLoad) {
                _ns.PanelCommon.refreshPanelAdmin('DevicesBase', skipBeforeLoad);
            },

            // show page
            page : function(my, nPage) {
                _ns.PanelCommon.onValidPage(function() {
                    my.input.page = nPage;
                    my.v2m(my);
                    _ns.PanelCommon.loadListPageAdmin(my, 'DevicesPage', '#sp-device-list-page');
                });
            },

            getInput : function(my) {
                return my.input;
            },

            onOutput : function(my, output) {
                my.output = output;
                /*
                 * NOTICE the $().one() construct. Since the page gets
                 * reloaded all the time, we want a single-shot binding.
                 */
                $(".sp-devices-page").one('click', null, function() {
                    my.page(my, parseInt($(this).text(), 10));
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-devices-page-next").one('click', null, function() {
                    my.page(my, my.output.nextPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-devices-page-prev").one('click', null, function() {
                    my.page(my, my.output.prevPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

            }
        };

        /**
         *
         */
        _ns.PanelOptions = {

            onAuthMethodSelect : function(method) {
                //
                var _view = _ns.PanelCommon.view;

                _view.visible($('.ldap-parms'), method === 'ldap');

                if (method === 'none') {
                    $('.user-source-group-s').hide();
                } else {
                    $('.user-source-group-s').show();
                    $('.user-source-group-display').show();
                    $('.user-source-group-edit').hide();
                }
            },

            onInternalUsersEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('#internal-users-parms'), enabled);
            },

            onAuthLdapUseSsl : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('#ldap-use-ssl-parms'), enabled);
            },

            onAuthModeLocalEnabled : function() {
                var
                //
                _view = _ns.PanelCommon.view
                //
                ,
                    userPw = _view.isCbChecked($("#auth-mode\\.name"))
                //
                ,
                    idNumber = _view.isCbChecked($("#auth-mode\\.id"))
                //
                ,
                    cardNumber = _view.isCbChecked($("#auth-mode\\.card-local"))
                //
                ,
                    yubikey = _view.isCbChecked($("#auth-mode\\.yubikey"))
                //
                ,
                    nMode = 0
                //
                ;

                $('#auth-mode-default-user').checkboxradio( userPw ? 'enable' : 'disable');
                $('#auth-mode-default-number').checkboxradio( idNumber ? 'enable' : 'disable');
                $('#auth-mode-default-card').checkboxradio( cardNumber ? 'enable' : 'disable');
                $('#auth-mode-default-yubikey').checkboxradio( yubikey ? 'enable' : 'disable');

                _view.visible($('#group-user-auth-mode-name-pw'), userPw);
                if (userPw) {
                    nMode++;
                }

                _view.visible($('#group-user-auth-mode-id-number'), idNumber);
                if (idNumber) {
                    nMode++;
                }

                _view.visible($('#group-user-auth-mode-card-local'), cardNumber);
                if (cardNumber) {
                    nMode++;
                }

                _view.visible($('#group-user-auth-mode-yubikey'), yubikey);
                if (yubikey) {
                    nMode++;
                }

                _view.visible($('#group-user-auth-mode-default'), nMode > 1);
            },

            onPrintImapEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.imap-enabled'), enabled);
            },

            onProxyPrintClearInboxEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.proxyprint-clear-inbox-scope-enabled'), enabled);
            },

            onProxyPrintEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.proxyprint-enabled'), enabled);
            },

            onWebPrintEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.webprint-enabled'), enabled);
            },

            onEcoPrintEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.ecoprint-enabled'), enabled);
            },

            onSmartSchoolEnabled : function(enabled1, enabled2) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.smartschool-1-print-enabled'), enabled1);
                _view.visible($('.smartschool-2-print-enabled'), enabled2);
                _view.visible($('.smartschool-print-enabled'), enabled1 || enabled2);
            },

            onSmartSchoolNodeEnabled : function(enabled1, enabled2) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.smartschool-1-print-node-enabled'), enabled1);
                _view.visible($('.smartschool-2-print-node-enabled'), enabled2);
            },

            onSmartSchoolNodeProxyEnabled : function(enabled1, enabled2) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.smartschool-1-print-node-proxy-enabled'), !enabled1);
                _view.visible($('.smartschool-2-print-node-proxy-enabled'), !enabled2);
            },

            onSmartSchoolPaperCutEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.smartschool-papercut-enabled'), enabled);
            },

            onProxyPrintDelegateEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.sp-proxyprint-delegate-enable-enabled'), enabled);
                this.onProxyPrintDelegatePaperCutEnabled( enabled ? _view.isCbChecked($("#proxy-print\\.delegate\\.papercut\\.enable")) : false);
            },

            onProxyPrintDelegatePaperCutEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.sp-download-papercut-delegator-cost-enabled'), enabled);
            },

            onPaperCutEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.papercut-enabled'), enabled);
            },

            onFinancialUserTransfersEnabled : function(enabled) {
                var _view = _ns.PanelCommon.view;
                _view.visible($('.financial-user-transfers-enabled'), enabled);
            },

            onGcpRefresh : function(my) {
                var _view = _ns.PanelCommon.view
                //
                ,
                    _model = my.model
                //
                ,
                    enabled = _view.isCbChecked($('#gcp\\.enable'))
                //
                ,
                    state = _model.gcp.state || $('#gcp-printer-state').text()
                //
                ,
                    isPresent = (state === 'ON_LINE' || state === 'OFF_LINE')
                //
                ,
                    notPresent = !isPresent;

                $('#gcp-printer-state-display').attr('class', (state === 'ON_LINE') ? 'sp-txt-valid' : 'sp-txt-warn');

                isPresent = (isPresent && enabled);
                notPresent = (notPresent && enabled);

                _view.visible($('#gcp-notification-section'), enabled);

                _view.visible($('#gcp-mail-after-cancel-detail'), _view.isCbChecked($('#gcp-mail-after-cancel-enable')));

                _view.visible($('#gcp-register-section'), notPresent);

                _view.visible($('#register-gcp'), notPresent);

                _view.visible($('#gcp-set-online'), enabled && (state === 'OFF_LINE'));
                _view.visible($('#gcp-set-offline'), enabled && (state === 'ON_LINE'));
            },

            onOutput : function(my, output) {
                var _view = _ns.PanelCommon.view;

                my.onAuthMethodSelect($("input:radio[name='auth.method']:checked").val());
                my.onAuthModeLocalEnabled();
                my.onPrintImapEnabled(_view.isCbChecked($('#print\\.imap\\.enable')));
                my.onProxyPrintEnabled(_view.isCbChecked($('#proxy-print\\.non-secure')));
                my.onProxyPrintClearInboxEnabled(_view.isCbChecked($('#webapp\\.user\\.proxy-print\\.clear-inbox\\.enable')));
                my.onWebPrintEnabled(_view.isCbChecked($('#web-print\\.enable')));
                my.onEcoPrintEnabled(_view.isCbChecked($('#eco-print\\.enable')));
                my.onAuthLdapUseSsl(_view.isCbChecked($('#auth\\.ldap\\.use-ssl')));
                my.onInternalUsersEnabled(_view.isCbChecked($('#internal-users\\.enable')));

                my.onSmartSchoolEnabled(_view.isCbChecked($('#smartschool\\.1\\.enable')), _view.isCbChecked($('#smartschool\\.2\\.enable')));
                my.onSmartSchoolNodeEnabled(_view.isCbChecked($('#smartschool\\.1\\.soap\\.print\\.node\\.enable')), _view.isCbChecked($('#smartschool\\.2\\.soap\\.print\\.node\\.enable')));
                my.onSmartSchoolNodeProxyEnabled(_view.isCbChecked($('#smartschool\\.1\\.soap\\.print\\.node\\.proxy\\.enable')), _view.isCbChecked($('#smartschool\\.2\\.soap\\.print\\.proxy\\.node\\.enable')));
                my.onSmartSchoolPaperCutEnabled(_view.isCbChecked($('#smartschool\\.papercut\\.enable')));
                my.onPaperCutEnabled(_view.isCbChecked($('#papercut\\.enable')));

                my.onGcpRefresh(my);

                my.onFinancialUserTransfersEnabled(_view.isCbChecked($('#financial\\.user\\.transfers\\.enable')));

                $('.user-source-group-display').show();
                $('.user-source-group-edit').hide();

                _view.mobipick($("#sp-smartschool-papercut-student-cost-date-from"));
                _view.mobipick($("#sp-smartschool-papercut-student-cost-date-to"));

                _view.mobipick($("#sp-papercut-delegator-cost-date-from"));
                _view.mobipick($("#sp-papercut-delegator-cost-date-to"));

                my.onProxyPrintDelegatePaperCutEnabled(_view.isCbChecked($('#proxy-print\\.delegate\\.papercut\\.enable')));
                my.onProxyPrintDelegateEnabled(_view.isCbChecked($('#proxy-print\\.delegate\\.enable')));
            }
        };

        /**
         *
         */
        _ns.PanelPrintersBase = {

            applyDefaults : function(my) {
                my.input.page = 1;
                my.input.maxResults = 10;
                my.input.sort.ascending = true;
                my.input.select.text = null;
                my.input.select.disabled = null;
                my.input.select.deleted = false;
            },

            beforeload : function(my) {
                $.noop();
            },

            afterload : function(my) {
                my.m2v(my);
                my.page(my, my.input.page);
            },

            m2v : function(my) {
                var id,
                    val,
                    _view = _ns.PanelCommon.view;

                $('#sp-printer-containing-text').val(my.input.select.text);

                val = my.input.select.disabled;
                _view.checkRadioValue('sp-printer-select-status', val === null ? "" : ( val ? "0" : "1"));

                val = my.input.select.deleted;
                _view.checkRadioValue('sp-printer-select-deleted', val === null ? "" : ( val ? "1" : "0"));

                //
                id = 'sp-printer-sort-dir';
                _view.checkRadio(id, my.input.sort.ascending ? id + '-asc' : id + '-desc');
            },

            v2m : function(my) {
                var val
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ,
                    sel = $('#sp-printer-containing-text')
                //
                ,
                    present = (sel.val().length > 0)
                //
                ;

                my.input.select.text = ( present ? sel.val() : null);

                val = _view.getRadioValue('sp-printer-select-status');
                my.input.select.disabled = (val === "" ? null : (val === "0"));

                val = _view.getRadioValue('sp-printer-select-deleted');
                my.input.select.deleted = (val === "" ? null : (val === "1"));

                my.input.sort.ascending = _view.isRadioIdSelected('sp-printer-sort-dir', 'sp-printer-sort-dir-asc');
            },

            // JSON input
            input : {
                page : 1,
                maxResults : 10,
                select : {
                    disabled : null,
                    deleted : false
                },
                sort : {
                    ascending : true
                }
            },

            // JSON output
            output : {
                lastPage : null,
                nextPage : null,
                prevPage : null
            },

            refresh : function(my, skipBeforeLoad) {
                _ns.PanelCommon.refreshPanelAdmin('PrintersBase', skipBeforeLoad);
            },

            // show page
            page : function(my, nPage) {
                _ns.PanelCommon.onValidPage(function() {
                    my.input.page = nPage;
                    my.v2m(my);
                    _ns.PanelCommon.loadListPageAdmin(my, 'PrintersPage', '#sp-printer-list-page');
                });
            },

            getInput : function(my) {
                return my.input;
            },

            onOutput : function(my, output) {
                var _view = _ns.PanelCommon.view;

                my.output = output;
                /*
                 * NOTICE the $().one() construct. Since the page gets
                 * reloaded all the time, we want a single-shot binding.
                 */
                $(".sp-printers-page").one('click', null, function() {
                    my.page(my, parseInt($(this).text(), 10));
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-printers-page-next").one('click', null, function() {
                    my.page(my, my.output.nextPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

                $(".sp-printers-page-prev").one('click', null, function() {
                    my.page(my, my.output.prevPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

                $('.sp-sparkline-printer').sparkline('html', {
                    enableTagOptions : true
                });

            }
        };

        /**
         *
         */
        _ns.PanelQueuesBase = {

            applyDefaults : function(my) {
                my.input.page = 1;
                my.input.maxResults = 10;
                my.input.sort.ascending = true;
                my.input.select.text = null;
                my.input.select.trusted = null;
                // Boolean
                my.input.select.disabled = null;
                // Boolean
                my.input.select.deleted = false;
                // Boolean
            },

            beforeload : function(my) {
                $.noop();
            },

            afterload : function(my) {
                my.m2v(my);
                my.page(my, my.input.page);
            },

            m2v : function(my) {
                var id,
                    val = my.input.select.text
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;
                $('#sp-queue-containing-text').val(val || '');

                //
                val = my.input.select.trusted;
                _view.checkRadioValue('sp-queue-select-trust', val === null ? "" : ( val ? "1" : "0"));

                //
                val = my.input.select.disabled;
                _view.checkRadioValue('sp-queue-select-status', val === null ? "" : ( val ? "0" : "1"));

                //
                val = my.input.select.deleted;
                _view.checkRadioValue('sp-queue-select-deleted', val === null ? "" : ( val ? "1" : "0"));

                //
                id = 'sp-queue-sort-dir';
                _view.checkRadio(id, my.input.sort.ascending ? id + '-asc' : id + '-desc');
            },

            v2m : function(my) {
                var val
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ,
                    sel = $('#sp-queue-containing-text'),
                    present = (sel.val().length > 0)
                //
                ;

                my.input.select.text = ( present ? sel.val() : null);

                //
                val = _view.getRadioValue('sp-queue-select-trust');
                my.input.select.trusted = (val === "" ? null : (val === "1"));

                //
                val = _view.getRadioValue('sp-queue-select-status');
                my.input.select.disabled = (val === "" ? null : (val === "0"));

                //
                val = _view.getRadioValue('sp-queue-select-deleted');
                my.input.select.deleted = (val === "" ? null : (val === "1"));

                //
                my.input.sort.ascending = _view.isRadioIdSelected('sp-queue-sort-dir', 'sp-queue-sort-dir-asc');
            },

            // JSON input
            input : {
                page : 1,
                maxResults : 10,
                select : {
                    trusted : null,
                    disabled : null,
                    deleted : false
                },
                sort : {
                    ascending : true
                }
            },

            // JSON output
            output : {
                lastPage : null,
                nextPage : null,
                prevPage : null
            },

            refresh : function(my, skipBeforeLoad) {
                _ns.PanelCommon.refreshPanelAdmin('QueuesBase', skipBeforeLoad);
            },

            // show page
            page : function(my, nPage) {
                _ns.PanelCommon.onValidPage(function() {
                    my.input.page = nPage;
                    my.v2m(my);
                    _ns.PanelCommon.loadListPageAdmin(my, 'QueuesPage', '#sp-queue-list-page');
                });
            },

            getInput : function(my) {
                return my.input;
            },

            onOutput : function(my, output) {

                var _view = _ns.PanelCommon.view;

                my.output = output;

                /*
                 * NOTICE the $().one() construct. Since the page get
                 * reloaded all the time, we want a single-shot binding.
                 */
                $(".sp-queues-page").one('click', null, function() {
                    my.page(my, parseInt($(this).text(), 10));
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-queues-page-next").one('click', null, function() {
                    my.page(my, my.output.nextPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-queues-page-prev").one('click', null, function() {
                    my.page(my, my.output.prevPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

                $('.sp-sparkline-queue').sparkline('html', {
                    enableTagOptions : true
                });

            }
        };

        /**
         *
         */
        _ns.PanelUsersBase = {

            applyDefaults : function(my) {

                my.input.page = 1;
                my.input.maxResults = 10;

                my.input.select.id_text = null;
                my.input.select.email_text = null;

                my.input.select.usergroup_id = null;

                // Boolean
                my.input.select.admin = null;
                // Boolean
                my.input.select.person = null;
                // Boolean
                my.input.select.disabled = null;
                // Boolean
                my.input.select.deleted = false;
                // Boolean

                my.input.sort.field = 'id';
                my.input.sort.ascending = true;
            },

            beforeload : function(my) {
                $.noop();
            },

            afterload : function(my) {
                my.m2v(my);
                my.page(my, my.input.page);
            },

            m2v : function(my) {
                var val,
                    id
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;

                $('#sp-user-id-containing-text').val(my.input.select.id_text);
                $('#sp-user-email-containing-text').val(my.input.select.email_text);

                $('#sp-user-select-group').val(my.input.select.usergroup_id).selectmenu('refresh');

                val = my.input.select.person;
                _view.checkRadioValue('sp-user-select-type', val === null ? "" : ( val ? "1" : "0"));

                val = my.input.select.admin;
                _view.checkRadioValue('sp-user-select-role', val === null ? "" : ( val ? "1" : "0"));

                val = my.input.select.disabled;
                _view.checkRadioValue('sp-user-select-status', val === null ? "" : ( val ? "0" : "1"));

                val = my.input.select.deleted;
                _view.checkRadioValue('sp-user-select-deleted', val === null ? "" : ( val ? "1" : "0"));

                //
                id = (my.input.sort.field === 'id' ? 'sp-user-sort-by-id' : 'sp-user-sort-by-email');
                _view.checkRadio('sp-user-sort-by', id);

                //
                id = 'sp-user-sort-dir';
                _view.checkRadio(id, my.input.sort.ascending ? id + '-asc' : id + '-desc');

            },

            v2m : function(my) {

                var val,
                    sel,
                    present
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;

                val = _view.getRadioValue('sp-user-select-type');
                my.input.select.person = (val === "" ? null : (val === "1"));

                val = _view.getRadioValue('sp-user-select-role');
                my.input.select.admin = (val === "" ? null : (val === "1"));

                val = _view.getRadioValue('sp-user-select-status');
                my.input.select.disabled = (val === "" ? null : (val === "0"));

                val = _view.getRadioValue('sp-user-select-deleted');
                my.input.select.deleted = (val === "" ? null : (val === "1"));

                sel = $('#sp-user-id-containing-text');
                present = (sel.val().length > 0);
                my.input.select.id_text = ( present ? sel.val() : null);

                sel = $('#sp-user-email-containing-text');
                present = (sel.val().length > 0);
                my.input.select.email_text = ( present ? sel.val() : null);

                sel = $('#sp-user-select-group');
                present = (sel.val() !== "0");
                my.input.select.usergroup_id = present ? sel.val() : null;

                my.input.sort.field = _view.getRadioValue('sp-user-sort-by');
                my.input.sort.ascending = _view.isRadioIdSelected('sp-user-sort-dir', 'sp-user-sort-dir-asc');
            },

            // JSON input
            input : {

                page : 1,
                maxResults : 10,

                select : {
                    id_text : null,
                    email_text : null,
                    admin : null,
                    person : null,
                    disabled : null,
                    deleted : false
                },
                sort : {
                    field : 'id', // id | email
                    ascending : true
                }
            },

            // JSON output
            output : {
                lastPage : null,
                nextPage : null,
                prevPage : null
            },

            refresh : function(my, skipBeforeLoad) {
                _ns.PanelCommon.refreshPanelAdmin('UsersBase', skipBeforeLoad);
            },

            // show page
            page : function(my, nPage) {
                _ns.PanelCommon.onValidPage(function() {
                    my.input.page = nPage;
                    my.v2m(my);
                    _ns.PanelCommon.loadListPageAdmin(my, 'UsersPage', '#sp-user-list-page');
                });
            },

            getInput : function(my) {
                return my.input;
            },

            onOutput : function(my, output) {

                var _view = _ns.PanelCommon.view;

                my.output = output;
                /*
                 * NOTICE the $().one() construct. Since the page get
                 * reloaded all the time, we want a single-shot binding.
                 */
                $(".sp-users-page").one('click', null, function() {
                    my.page(my, parseInt($(this).text(), 10));
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-users-page-next").one('click', null, function() {
                    my.page(my, my.output.nextPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-users-page-prev").one('click', null, function() {
                    my.page(my, my.output.prevPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

                $('.sp-sparkline-user').sparkline('html', {
                    enableTagOptions : true
                });
            }
        };

        /**
         *
         */
        _ns.PanelAccountsBase = {

            applyDefaults : function(my) {

                my.input.page = 1;
                my.input.maxResults = 10;

                my.input.select.name_text = null;
                // AccountTypeEnum
                my.input.select.accountType = undefined;
                // Boolean
                my.input.select.deleted = false;
                // Boolean
                my.input.sort.ascending = true;
            },

            beforeload : function(my) {
                $.noop();
            },

            afterload : function(my) {
                my.m2v(my);
                my.page(my, my.input.page);
            },

            m2v : function(my) {
                var val,
                    id
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;

                $('#sp-shared-account-name-containing-text').val(my.input.select.name_text);

                val = my.input.select.deleted;
                _view.checkRadioValue('sp-shared-account-select-deleted', val === null ? "" : ( val ? "1" : "0"));

                val = my.input.select.accountType;
                _view.checkRadioValue('sp-shared-account-select-type', val === undefined ? "" : val);

                //
                id = 'sp-shared-account-sort-dir';
                _view.checkRadio(id, my.input.sort.ascending ? id + '-asc' : id + '-desc');

            },

            v2m : function(my) {

                var val,
                    sel,
                    present
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;

                val = _view.getRadioValue('sp-shared-account-select-deleted');
                my.input.select.deleted = (val === "" ? null : (val === "1"));

                val = _view.getRadioValue('sp-shared-account-select-type');
                my.input.select.accountType = (val === "" ? undefined : val);

                sel = $('#sp-shared-account-name-containing-text');
                present = (sel.val().length > 0);
                my.input.select.name_text = ( present ? sel.val() : null);

                my.input.sort.field = _view.getRadioValue('sp-shared-account-sort-by');
                my.input.sort.ascending = _view.isRadioIdSelected('sp-shared-account-sort-dir', 'sp-shared-account-sort-dir-asc');
            },

            // JSON input
            input : {

                page : 1,
                maxResults : 10,

                select : {
                    name_text : null,
                    deleted : false
                },
                sort : {
                    field : 'name', // name
                    ascending : true
                }
            },

            // JSON output
            output : {
                lastPage : null,
                nextPage : null,
                prevPage : null
            },

            refresh : function(my, skipBeforeLoad) {
                _ns.PanelCommon.refreshPanelAdmin('AccountsBase', skipBeforeLoad);
            },

            // show page
            page : function(my, nPage) {
                _ns.PanelCommon.onValidPage(function() {
                    my.input.page = nPage;
                    my.v2m(my);
                    _ns.PanelCommon.loadListPageAdmin(my, 'AccountsPage', '#sp-shared-account-list-page');
                });
            },

            getInput : function(my) {
                return my.input;
            },

            onOutput : function(my, output) {

                my.output = output;
                /*
                 * NOTICE the $().one() construct. Since the page get
                 * reloaded all the time, we want a single-shot binding.
                 */
                $(".sp-shared-accounts-page").one('click', null, function() {
                    my.page(my, parseInt($(this).text(), 10));
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-shared-accounts-page-next").one('click', null, function() {
                    my.page(my, my.output.nextPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-shared-accounts-page-prev").one('click', null, function() {
                    my.page(my, my.output.prevPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

            }
        };

        /**
         *
         */
        _ns.PanelUserGroupsBase = {

            applyDefaults : function(my) {

                my.input.page = 1;
                my.input.maxResults = 10;

                my.input.select.name_text = null;
                // Boolean
                my.input.sort.ascending = true;
            },

            beforeload : function(my) {
                $.noop();
            },

            afterload : function(my) {
                my.m2v(my);
                my.page(my, my.input.page);
            },

            m2v : function(my) {
                var id
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;

                $('#sp-user-group-name-containing-text').val(my.input.select.name_text);

                //
                id = 'sp-user-group-sort-dir';
                _view.checkRadio(id, my.input.sort.ascending ? id + '-asc' : id + '-desc');

            },

            v2m : function(my) {

                var sel,
                    present
                //
                ,
                    _view = _ns.PanelCommon.view
                //
                ;

                sel = $('#sp-user-group-name-containing-text');
                present = (sel.val().length > 0);
                my.input.select.name_text = ( present ? sel.val() : null);

                my.input.sort.ascending = _view.isRadioIdSelected('sp-user-group-sort-dir', 'sp-user-group-sort-dir-asc');
            },

            // JSON input
            input : {

                page : 1,
                maxResults : 10,

                select : {
                    name_text : null
                },
                sort : {
                    ascending : true
                }
            },

            // JSON output
            output : {
                lastPage : null,
                nextPage : null,
                prevPage : null
            },

            refresh : function(my, skipBeforeLoad) {
                _ns.PanelCommon.refreshPanelAdmin('UserGroupsBase', skipBeforeLoad);
            },

            // show page
            page : function(my, nPage) {
                _ns.PanelCommon.onValidPage(function() {
                    my.input.page = nPage;
                    my.v2m(my);
                    _ns.PanelCommon.loadListPageAdmin(my, 'UserGroupsPage', '#sp-user-group-list-page');
                });
            },

            getInput : function(my) {
                return my.input;
            },

            onOutput : function(my, output) {

                my.output = output;

                /*
                 * NOTICE the $().one() construct. Since the page get
                 * reloaded all the time, we want a single-shot binding.
                 */
                $(".sp-user-groups-page").one('click', null, function() {
                    my.page(my, parseInt($(this).text(), 10));
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-user-groups-page-next").one('click', null, function() {
                    my.page(my, my.output.nextPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });
                $(".sp-user-groups-page-prev").one('click', null, function() {
                    my.page(my, my.output.prevPage);
                    /*
                     * return false so URL is not followed.
                     */
                    return false;
                });

            }
        };

    }(jQuery, this, this.document, JSON, this.org.savapage));
