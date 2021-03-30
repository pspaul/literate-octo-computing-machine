// @license http://www.gnu.org/licenses/agpl-3.0.html AGPL-3.0

/*! SavaPage jQuery Mobile Admin Panels | (c) 2020 Datraverse B.V. | GNU
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
 * NOTE: the *! comment blocks are part of compressed version.
 */

/*
 * SavaPage jQuery Mobile Admin Panels
 */
(function($, window, document, JSON, _ns) {
    "use strict";

    /*jslint browser: true*/
    /*global $, jQuery, alert*/

    /**
     *
     */
    _ns.PanelAccountVoucherBase = {

        applyDefaults: function() {
            this.input.page = 1;
            this.input.maxResults = 10;

            this.input.select.batch = null;
            this.input.select.number = null;
            this.input.select.userId = null;

            this.input.select.dateFrom = null;
            this.input.select.dateTo = null;

            // boolean
            this.input.select.used = null;
            // boolean
            this.input.select.expired = null;

            // NUMBER, VALUE, USER, DATE_USED, DATE_EXPIRED
            this.input.sort.field = 'NUMBER';
            this.input.sort.ascending = true;

        },

        onVoucherSelectBatch: function(batch) {
            var sel = $(".sp-voucher-batch-selected ");
            if (batch !== "0") {
                sel.show();
            } else {
                sel.hide();
            }
        },

        beforeload: function() {
            this.applyDefaults();
        },

        afterload: function() {
            var _view = _ns.PanelCommon.view;
            _view.mobipick($("#sp-voucher-date-from"));
            _view.mobipick($("#sp-voucher-date-to"));
            this.m2v();
            this.page(this.input.page);
        },

        getBatch: function() {
            var sel = $('#sp-voucher-batch'),
                present = (sel.val() !== "0");
            return (present ? sel && sel.val() : null);
        },

        getCardDesign: function() {
            return $("#sp-voucher-card-print-format").val();
        },

        m2v: function() {
            var id,
                val,
                _view = _ns.PanelCommon.view;

            val = this.input.select.batch;
            val = (val === null ? "0" : val);
            $('#sp-voucher-batch').val(val).selectmenu('refresh');
            this.onVoucherSelectBatch(val);

            $('#sp-voucher-number').val(this.input.select.number);
            $('#sp-voucher-user').val(this.input.select.userId);

            _view.mobipickSetDate($('#sp-voucher-date-from'), this.input.select.dateFrom);
            _view.mobipickSetDate($('#sp-voucher-date-to'), this.input.select.dateTo);

            //
            val = this.input.select.used;
            _view.checkRadioValue('sp-voucher-select-used', val === null ? "" : (val ? "1" : "0"));

            //
            val = this.input.select.expired;
            _view.checkRadioValue('sp-voucher-select-expired', val === null ? "" : (val ? "1" : "0"));

            //
            id = 'sp-voucher-sort-by';
            _view.checkRadioValue(id, this.input.sort.field);

            id = 'sp-voucher-sort-dir';
            _view.checkRadio(id, this.input.sort.ascending ? id + '-asc' : id + '-desc');

        },

        v2m: function() {
            var _view = _ns.PanelCommon.view,
                val,
                sel = $('#sp-voucher-date-from'),
                date = _view.mobipickGetDate(sel),
                present = (sel.val().length > 0);

            this.input.select.dateFrom = (present ? date.getTime() : null);

            sel = $('#sp-voucher-date-to');
            date = _view.mobipickGetDate(sel);
            present = (sel.val().length > 0);
            this.input.select.dateTo = (present ? date.getTime() : null);

            //
            this.input.select.batch = this.getBatch();

            //
            sel = $('#sp-voucher-number');
            present = (sel.val().length > 0);
            this.input.select.number = (present ? sel.val() : null);

            //
            sel = $('#sp-voucher-user');
            present = (sel.val().length > 0);
            this.input.select.userId = (present ? sel.val() : null);

            //
            val = _view.getRadioValue('sp-voucher-select-used');
            this.input.select.used = (val === "" ? null : val === "1");

            //
            val = _view.getRadioValue('sp-voucher-select-expired');
            this.input.select.expired = (val === "" ? null : val === "1");

            //
            this.input.sort.field = _view.getRadioValue('sp-voucher-sort-by');
            this.input.sort.ascending = _view.isRadioIdSelected('sp-voucher-sort-dir', 'sp-voucher-sort-dir-asc');
        },

        // JSON input
        input: {
            page: 1,
            maxResults: 10,
            select: {
                batch: null,
                number: null,
                userId: null,
                dateFrom: null,
                dateTo: null,
                // boolean
                used: null,
                // boolean
                expired: null
            },
            sort: {
                // NUMBER, VALUE, USER, DATE_USED, DATE_EXPIRED
                field: 'NUMBER',
                ascending: true
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelAdmin('AccountVoucherBase', skipBeforeLoad);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageAdmin(_this, 'AccountVoucherPage', '#sp-voucher-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page get reloaded
             * all
             * the time, we want a single-shot binding.
             */
            $(".sp-voucher-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-voucher-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-voucher-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
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

        applyDefaults: function() {
            this.input.page = 1;
            this.input.maxResults = 10;

            this.input.select.text = null;

            this.input.select.level = null;
            // INFO | WARN | ERROR
            this.input.select.date_from = null;
            this.input.select.date_to = null;

            this.input.sort.field = 'DATE';
            // LEVEl | DATE
            this.input.sort.ascending = false;

        },

        beforeload: function() {
            this.applyDefaults();
        },

        afterload: function() {
            var _view = _ns.PanelCommon.view;
            _view.mobipick($("#sp-applog-date-from"));
            _view.mobipick($("#sp-applog-date-to"));
            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var id,
                _view = _ns.PanelCommon.view;

            $('#sp-applog-containing-text').val(this.input.select.text);

            _view.mobipickSetDate($('#sp-applog-date-from'), this.input.select.date_from);
            _view.mobipickSetDate($('#sp-applog-date-to'), this.input.select.date_to);

            if (this.input.select.level) {
                _view.checkRadioValue('sp-applog-select-level', this.input.select.level);
            } else {
                _view.checkRadio('sp-applog-select-level', 'sp-applog-select-level-all');
            }

            id = 'sp-applog-sort-by';
            _view.checkRadioValue(id, this.input.sort.field);

            id = 'sp-applog-sort-dir';
            _view.checkRadio(id, this.input.sort.ascending ? id + '-asc' : id + '-desc');

        },

        v2m: function() {
            var _view = _ns.PanelCommon.view,
                level = _view.getRadioValue('sp-applog-select-level'),
                sel = $('#sp-applog-date-from'),
                date = _view.mobipickGetDate(sel),
                present = (sel.val().length > 0);

            this.input.select.date_from = (present ? date.getTime() : null);

            sel = $('#sp-applog-date-to');
            date = _view.mobipickGetDate(sel);
            present = (sel.val().length > 0);
            this.input.select.date_to = (present ? date.getTime() : null);

            sel = $('#sp-applog-containing-text');
            present = (sel.val().length > 0);
            this.input.select.text = (present ? sel.val() : null);

            this.input.select.level = (level === "" ? null : level);

            this.input.sort.field = _view.getRadioValue('sp-applog-sort-by');

            this.input.sort.ascending = _view.isRadioIdSelected('sp-applog-sort-dir', 'sp-applog-sort-dir-asc');
        },

        // JSON input
        input: {
            page: 1,
            maxResults: 10,
            select: {
                // null (all) | 0 (info) | 1 (warn) | 2 (error)
                // See Java:
                // org.savapage.core.domain.AppLog.Level
                level: null,
                date_from: null,
                date_to: null,
                text: null
            },
            sort: {
                field: 'DATE', // DATE | LEVEL
                ascending: false
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelAdmin('AppLogBase', skipBeforeLoad);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageAdmin(_this, 'AppLogPage', '#sp-applog-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;

            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page get
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-applog-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-applog-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-applog-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
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

        applyDefaults: function() {
            this.input.page = 1;
            this.input.maxResults = 10;
            this.input.sort.ascending = true;
            this.input.select.text = null;
        },

        beforeload: function() {
            $.noop();
        },

        afterload: function() {
            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var val = this.input.select.text,
                _view = _ns.PanelCommon.view;
            $('#sp-config-containing-text').val(val || '');

            _view.checkRadio('sp-config-sort-dir', 'sp-config-sort-dir-asc');
        },

        v2m: function() {
            var sel = $('#sp-config-containing-text'),
                present = (sel.val().length > 0),
                _view = _ns.PanelCommon.view;
            this.input.select.text = (present ? sel.val() : null);

            this.input.sort.ascending = _view.isRadioIdSelected('sp-config-sort-dir', 'sp-config-sort-dir-asc');
        },

        // JSON input
        input: {
            page: 1,
            maxResults: 10,
            select: {},
            sort: {
                ascending: true
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelAdmin('ConfigPropBase', skipBeforeLoad);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageAdmin(_this, 'ConfigPropPage', '#sp-config-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page get
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-config-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-config-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-config-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
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
        model: null,

        // Refresh every minute.
        REFRESH_MSEC: 60000,

        // The saved scroll position.
        scrollTop: null,

        piechart: null,
        xychart: null,

        timeout: null,

        destroyPlots: function() {
            if (this.piechart) {
                this.piechart.destroy();
                this.piechart = null;
            }
            if (this.xychart) {
                this.xychart.destroy();
                this.xychart = null;
            }
        },

        /**
         * Retrieves plot data and shows them.
         * @return true when success, false when connection errors.
         */
        showPlots: function() {
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

            this.xychart = _view.showXyChart('dashboard-xychart', xydata);
            this.piechart = _view.showPieChart('dashboard-piechart', piedata);

            return true;
        },

        /*
         * A refresh of the WHOLE panel.
         */
        refresh: function(skipBeforeLoad) {
            /*
             * As an extra check we use #live-messages as indicator
             * that the Dashboard is in view.
             */
            if ($('#live-messages').length > 0) {
                this.scrollTop = $(window).scrollTop();
                _ns.PanelCommon.refreshPanelAdmin('Dashboard', skipBeforeLoad);
            }
        },

        /*
         * A refresh of the PART of the panel.
         */
        refreshSysStatus: function() {

            var scrollTop,
                heightXyChart,
                heightPieChart,
                html,
                _view = _ns.PanelCommon.view,
                // NOTE: 'this' does not work, because this method is
                // called from window.setInterval().
                _this = _ns.PanelDashboard;

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
                _this.destroyPlots();

                /*
                 * Load the html.
                 */
                html = _view.getAdminPageHtml('SystemStatusAddin');

                if (!html) {
                    // Failure (and disconnected): stop the timer!
                    _this.onUnload();
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
                if (!_this.showPlots()) {
                    // Failure (and disconnected): stop the timer!
                    _this.onUnload();
                }
            }
        },

        beforeload: function() {
            /*
             * IMPORTANT: Release all resources occupied by the
             * jqPlots.
             * NOT releasing introduces a HUGE memory leak, each time
             * the plots are refreshed.
             */
            this.destroyPlots();
        },

        afterload: function() {

            var i,
                j,
                _model = this.model;

            for (i = 0; i < _model.pubMsgStack.length; i++) {
                $('#live-messages').append(_model.pubMsgAsHtml(_model.pubMsgStack[i]));
            }
            // Fill it up...
            for (j = i; j < _model.MAX_PUB_MSG; j++) {
                $('#live-messages').append('<div>&nbsp;</div>');
            }

            if (this.showPlots()) {
                /*
                 * Interval timer: refresh of part.
                 */
                this.timeout = window.setInterval(this.refreshSysStatus, this.REFRESH_MSEC);
            }
        },

        onResize: function() {
            var selXyChart = $('#dashboard-xychart'),
                selPieChart = $('#dashboard-piechart');

            if (selXyChart && selPieChart) {

                selXyChart.width(selXyChart.parent().width());
                this.xychart.replot({
                    resetAxes: true
                });

                selPieChart.width(selPieChart.parent().width());
                this.piechart.replot({});

            }

        },

        onUnload: function() {
            /*
             * Clear the timer asap.
             */
            window.clearTimeout(this.timeout);
        }
    };

    /**
     *
     */
    _ns.PanelDevicesBase = {

        applyDefaults: function() {
            this.input.page = 1;
            this.input.maxResults = 10;
            this.input.sort.ascending = true;
            this.input.select.text = null;
            // Boolean
            this.input.select.disabled = null;
            // Boolean
            this.input.select.reader = null;
        },

        beforeload: function() {
            $.noop();
        },

        afterload: function() {
            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var id,
                val,
                _view = _ns.PanelCommon.view;
            $('#sp-device-containing-text').val(this.input.select.text);

            val = this.input.select.disabled;
            _view.checkRadioValue("sp-device-select-status", val === null ? "" : (val ? "0" : "1"));

            val = this.input.select.reader;
            _view.checkRadioValue("sp-device-select-type", val === null ? "" : (val ? "1" : "0"));

            id = 'sp-device-sort-dir';
            _view.checkRadio(id, this.input.sort.ascending ? id + '-asc' : id + '-desc');

        },

        v2m: function() {
            var val,
                sel = $('#sp-device-containing-text'),
                present = (sel.val().length > 0),
                _view = _ns.PanelCommon.view;

            this.input.select.text = (present ? sel.val() : null);

            val = _view.getRadioValue("sp-device-select-status");
            this.input.select.disabled = (val === "" ? null : (val === "0"));

            val = _view.getRadioValue("sp-device-select-type");
            this.input.select.reader = (val === "" ? null : (val === "1"));

            this.input.sort.ascending = _view.isRadioIdSelected('sp-device-sort-dir', 'sp-device-sort-dir-asc');
        },

        // JSON input
        input: {
            page: 1,
            maxResults: 10,
            select: {
                disabled: null,
                reader: null
            },
            sort: {
                ascending: true
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelAdmin('DevicesBase', skipBeforeLoad);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageAdmin(_this, 'DevicesPage', '#sp-device-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page gets
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-devices-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-devices-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-devices-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
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
    _ns.PanelReports = {

        refresh: function(skipBeforeLoad) {
            this.scrollTop = $(window).scrollTop();
            _ns.PanelCommon.refreshPanelAdmin('Reports', skipBeforeLoad);
        },

        v2m: function() {
            var _view = _ns.PanelCommon.view,
                groups = $('.sp-reports-user-printout-tot ._sp-groups').val(),
                sel;

            sel = $('.sp-reports-user-printout-tot ._sp-date-from');
            this.input.timeFrom = sel.val().length > 0 ? _view.mobipickGetDate(sel).getTime() : null;

            sel = $('.sp-reports-user-printout-tot ._sp-date-to');
            this.input.timeTo = sel.val().length > 0 ? _view.mobipickGetDate(sel).getTime() : null;

            if (groups.length > 0) {
                this.input.userGroups = groups.split(" ");
            } else {
                this.input.userGroups = null;
            }

            this.input.aspect = _view.getRadioValue('sp-reports-user-printout-tot-aspect');
            this.input.pages = _view.getRadioValue('sp-reports-user-printout-tot-aspect-pages');
        },

        // JSON input: UserPrintOutTotalsReq
        input: {
            timeFrom: null,
            timeTo: null,
            userGroups: null,
            aspect: 'PAGES',
            pages: 'SENT'
        },

        onOutput: function(output) {
            var _view = _ns.PanelCommon.view;
            _view.mobipick($('.sp-reports-user-printout-tot ._sp-date-from'));
            _view.mobipick($('.sp-reports-user-printout-tot ._sp-date-to'));
        }
    };

    /**
     *
     */
    _ns.PanelOptions = {

        onAuthMethodSelect: function(method) {
            var _view = _ns.PanelCommon.view,
                isLdapMethod = method === 'ldap';

            _view.visible($('.ldap-parms'), isLdapMethod);

            if (method === 'none') {
                $('.user-source-group-s').hide();
            } else {
                $('.user-source-group-s').show();
                $('.user-source-group-display').show();
                $('.user-source-group-edit').hide();
                if (isLdapMethod) {
                    this.onLdapSchemaTypeSelect($('#sp-ldap-schema-type').val());
                }
            }
        },

        onLdapSchemaTypeSelect: function(schemaType) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.ldap-parms-extra-fields'), schemaType !== 'GOOGLE_CLOUD');
            _view.visible($('.ldap-parms-standard'), schemaType !== 'GOOGLE_CLOUD');
            _view.visible($('.ldap-parms-google-cloud'), schemaType === 'GOOGLE_CLOUD');
        },

        onInternalUsersEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('#internal-users-parms'), enabled);
        },

        onAuthLdapUseSsl: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('#ldap-use-ssl-parms'), enabled);
        },

        onAuthModeLocalEnabled: function() {
            var _view = _ns.PanelCommon.view,
                userPw = _view.isCbChecked($("#auth-mode\\.name")),
                idNumber = _view.isCbChecked($("#auth-mode\\.id")),
                cardNumber = _view.isCbChecked($("#auth-mode\\.card-local")),
                yubikey = _view.isCbChecked($("#auth-mode\\.yubikey")),
                nMode = 0;

            $('#auth-mode-default-user').checkboxradio(userPw ? 'enable' : 'disable');
            $('#auth-mode-default-number').checkboxradio(idNumber ? 'enable' : 'disable');
            $('#auth-mode-default-card').checkboxradio(cardNumber ? 'enable' : 'disable');
            $('#auth-mode-default-yubikey').checkboxradio(yubikey ? 'enable' : 'disable');

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

        onPrintImapEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.imap-enabled'), enabled);
        },

        onProxyPrintClearInboxEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.proxyprint-clear-inbox-scope-enabled'), enabled);
        },

        onProxyPrintEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.proxyprint-enabled'), enabled);
        },

        onWebPrintEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.webprint-enabled'), enabled);
        },

        onEcoPrintEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.ecoprint-enabled'), enabled);
        },

        onProxyPrintDelegateEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.sp-proxyprint-delegate-enable-enabled'), enabled);
            this.onProxyPrintDelegatePaperCutEnabled(enabled ? _view.isCbChecked($("#proxy-print\\.delegate\\.papercut\\.enable")) : false);
            _view.visibleCheckboxRadio($('#proxy-print\\.personal\\.papercut\\.enable'), !enabled);
        },

        onProxyPrintDelegatePaperCutEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.sp-download-papercut-delegator-cost-enabled'), enabled);
        },

        onPaperCutEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.papercut-enabled'), enabled);
        },

        onFinancialUserTransfersEnabled: function(enabled) {
            var _view = _ns.PanelCommon.view;
            _view.visible($('.financial-user-transfers-enabled'), enabled);
        },

        onOutput: function(output) {
            var _view = _ns.PanelCommon.view;

            this.onAuthMethodSelect($("input:radio[name='auth.method']:checked").val());

            this.onAuthModeLocalEnabled();
            this.onPrintImapEnabled(_view.isCbChecked($('#print\\.imap\\.enable')));
            this.onProxyPrintEnabled(_view.isCbChecked($('#proxy-print\\.non-secure')));
            this.onProxyPrintClearInboxEnabled(_view.isCbChecked($('#webapp\\.user\\.proxy-print\\.clear-inbox\\.enable')));
            this.onWebPrintEnabled(_view.isCbChecked($('#web-print\\.enable')));
            this.onEcoPrintEnabled(_view.isCbChecked($('#eco-print\\.enable')));
            this.onAuthLdapUseSsl(_view.isCbChecked($('#auth\\.ldap\\.use-ssl')));
            this.onInternalUsersEnabled(_view.isCbChecked($('#internal-users\\.enable')));

            this.onPaperCutEnabled(_view.isCbChecked($('#papercut\\.enable')));

            this.onFinancialUserTransfersEnabled(_view.isCbChecked($('#financial\\.user\\.transfers\\.enable')));

            $('.user-source-group-display').show();
            $('.user-source-group-edit').hide();

            _view.mobipick($("#sp-papercut-delegator-cost-date-from"));
            _view.mobipick($("#sp-papercut-delegator-cost-date-to"));

            this.onProxyPrintDelegatePaperCutEnabled(_view.isCbChecked($('#proxy-print\\.delegate\\.papercut\\.enable')));
            this.onProxyPrintDelegateEnabled(_view.isCbChecked($('#proxy-print\\.delegate\\.enable')));
        },

        /** One-shot function */
        scrollToAnchor: null,

        onPanelShow: function() {
            if (this.scrollToAnchor) {
                this.scrollToAnchor();
                this.scrollToAnchor = null;
                return true;
            }
            return false;
        }
    };

    /**
     *
     */
    _ns.PanelPrintersBase = {

        groupSearchObj: null,

        applyDefaults: function() {
            this.input.page = 1;
            this.input.maxResults = 10;
            this.input.sort.ascending = true;
            this.input.select.text = null;
            this.input.select.group = null;
            this.input.select.disabled = null;
            this.input.select.ticket = null;
            this.input.select.internal = null;
            this.input.select.deleted = false;
        },

        beforeload: function() {
            $.noop();
        },

        afterload: function() {
            if (!this.groupSearchObj) {
                this.groupSearchObj = new _ns.QuickObjectSearch(_ns.PanelCommon.view, _ns.PanelCommon.api);
            }
            this.groupSearchObj.onCreate($("#sp-printer-printer-group-contain"), 'sp-printer-printer-group-filter', 'printergroup-quick-search', null,
                //
                function(item) {
                    return item.text;
                }, //
                function(item) {
                    $("#sp-printer-printer-group").val(item.text);
                });

            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var id,
                val,
                _view = _ns.PanelCommon.view;

            $('#sp-printer-containing-text').val(this.input.select.text);
            $('#sp-printer-printer-group').val(this.input.select.group);

            val = this.input.select.disabled;
            _view.checkRadioValue('sp-printer-select-status', val === null ? "" : (val ? "0" : "1"));

            val = this.input.select.ticket;
            _view.checkRadioValue('sp-printer-select-jobticket', val === null ? "" : (val ? "1" : "0"));

            val = this.input.select.internal;
            _view.checkRadioValue('sp-printer-select-internal', val === null ? "" : (val ? "1" : "0"));

            val = this.input.select.deleted;
            _view.checkRadioValue('sp-printer-select-deleted', val === null ? "" : (val ? "1" : "0"));

            //
            id = 'sp-printer-sort-dir';
            _view.checkRadio(id, this.input.sort.ascending ? id + '-asc' : id + '-desc');
        },

        v2m: function() {
            var val,
                _view = _ns.PanelCommon.view,
                sel = $('#sp-printer-containing-text'),
                present = (sel.val().length > 0);

            this.input.select.text = (present ? sel.val() : null);

            sel = $('#sp-printer-printer-group'),
                present = (sel.val().length > 0);
            this.input.select.group = (present ? sel.val() : null);

            val = _view.getRadioValue('sp-printer-select-status');
            this.input.select.disabled = (val === "" ? null : (val === "0"));

            val = _view.getRadioValue('sp-printer-select-jobticket');
            this.input.select.ticket = (val === "" ? null : (val === "1"));

            val = _view.getRadioValue('sp-printer-select-internal');
            this.input.select.internal = (val === "" ? null : (val === "1"));

            val = _view.getRadioValue('sp-printer-select-deleted');
            this.input.select.deleted = (val === "" ? null : (val === "1"));

            this.input.sort.ascending = _view.isRadioIdSelected('sp-printer-sort-dir', 'sp-printer-sort-dir-asc');
        },

        // JSON input
        input: {
            page: 1,
            maxResults: 10,
            select: {
                disabled: null,
                ticket: null,
                internal: null,
                deleted: false
            },
            sort: {
                ascending: true
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelAdmin('PrintersBase', skipBeforeLoad);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m(_this);
                _ns.PanelCommon.loadListPageAdmin(_this, 'PrintersPage', '#sp-printer-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page gets
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-printers-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                // return false so URL is not followed.
                return false;
            });
            $(".sp-printers-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                // return false so URL is not followed.
                return false;
            });

            $(".sp-printers-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
                // return false so URL is not followed.
                return false;
            });

            $('.sp-sparkline-printer').sparkline('html', {
                enableTagOptions: true
            });

        }
    };

    /**
     *
     */
    _ns.PanelQueuesBase = {

        applyDefaults: function() {
            this.input.page = 1;
            this.input.maxResults = 10;
            this.input.sort.ascending = true;
            this.input.select.text = null;
            this.input.select.trusted = null;
            // Boolean
            this.input.select.disabled = null;
            // Boolean
            this.input.select.deleted = false;
            // Boolean
        },

        beforeload: function() {
            $.noop();
        },

        afterload: function() {
            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var id,
                val = this.input.select.text,
                _view = _ns.PanelCommon.view;
            $('#sp-queue-containing-text').val(val || '');

            //
            val = this.input.select.trusted;
            _view.checkRadioValue('sp-queue-select-trust', val === null ? "" : (val ? "1" : "0"));

            //
            val = this.input.select.disabled;
            _view.checkRadioValue('sp-queue-select-status', val === null ? "" : (val ? "0" : "1"));

            //
            val = this.input.select.deleted;
            _view.checkRadioValue('sp-queue-select-deleted', val === null ? "" : (val ? "1" : "0"));

            //
            id = 'sp-queue-sort-dir';
            _view.checkRadio(id, this.input.sort.ascending ? id + '-asc' : id + '-desc');
        },

        v2m: function() {
            var val,
                _view = _ns.PanelCommon.view,
                sel = $('#sp-queue-containing-text'),
                present = (sel.val().length > 0);

            this.input.select.text = (present ? sel.val() : null);

            //
            val = _view.getRadioValue('sp-queue-select-trust');
            this.input.select.trusted = (val === "" ? null : (val === "1"));

            //
            val = _view.getRadioValue('sp-queue-select-status');
            this.input.select.disabled = (val === "" ? null : (val === "0"));

            //
            val = _view.getRadioValue('sp-queue-select-deleted');
            this.input.select.deleted = (val === "" ? null : (val === "1"));

            //
            this.input.sort.ascending = _view.isRadioIdSelected('sp-queue-sort-dir', 'sp-queue-sort-dir-asc');
        },

        // JSON input
        input: {
            page: 1,
            maxResults: 10,
            select: {
                trusted: null,
                disabled: null,
                deleted: false
            },
            sort: {
                ascending: true
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelAdmin('QueuesBase', skipBeforeLoad);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageAdmin(_this, 'QueuesPage', '#sp-queue-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;

            /*
             * NOTICE the $().one() construct. Since the page get
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-queues-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-queues-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-queues-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });

            $('.sp-sparkline-queue').sparkline('html', {
                enableTagOptions: true
            });

        }
    };

    /**
     *
     */
    _ns.PanelUsersBase = {

        applyDefaults: function() {

            this.input.page = 1;
            this.input.maxResults = 10;

            this.input.select.name_id_text = null;
            this.input.select.email_text = null;

            this.input.select.usergroup_id = null;

            // Boolean
            this.input.select.admin = null;
            // Boolean
            this.input.select.person = null;
            // Boolean
            this.input.select.disabled = null;
            // Boolean
            this.input.select.deleted = false;
            // Boolean

            this.input.sort.field = 'id';
            this.input.sort.ascending = true;
        },

        beforeload: function() {
            $.noop();
        },

        afterload: function() {
            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var val,
                id,
                _view = _ns.PanelCommon.view;

            $('#sp-user-id-containing-text').val(this.input.select.name_id_text);
            $('#sp-user-email-containing-text').val(this.input.select.email_text);

            $('#sp-user-select-group').val(this.input.select.usergroup_id).selectmenu('refresh');

            val = this.input.select.person;
            _view.checkRadioValue('sp-user-select-type', val === null ? "" : (val ? "1" : "0"));

            val = this.input.select.admin;
            _view.checkRadioValue('sp-user-select-role', val === null ? "" : (val ? "1" : "0"));

            val = this.input.select.disabled;
            _view.checkRadioValue('sp-user-select-status', val === null ? "" : (val ? "0" : "1"));

            val = this.input.select.deleted;
            _view.checkRadioValue('sp-user-select-deleted', val === null ? "" : (val ? "1" : "0"));

            //
            id = (this.input.sort.field === 'id' ? 'sp-user-sort-by-id' : 'sp-user-sort-by-email');
            _view.checkRadio('sp-user-sort-by', id);

            //
            id = 'sp-user-sort-dir';
            _view.checkRadio(id, this.input.sort.ascending ? id + '-asc' : id + '-desc');

        },

        v2m: function() {
            var val,
                sel,
                present,
                _view = _ns.PanelCommon.view;

            val = _view.getRadioValue('sp-user-select-type');
            this.input.select.person = (val === "" ? null : (val === "1"));

            val = _view.getRadioValue('sp-user-select-role');
            this.input.select.admin = (val === "" ? null : (val === "1"));

            val = _view.getRadioValue('sp-user-select-status');
            this.input.select.disabled = (val === "" ? null : (val === "0"));

            val = _view.getRadioValue('sp-user-select-deleted');
            this.input.select.deleted = (val === "" ? null : (val === "1"));

            sel = $('#sp-user-id-containing-text');
            present = (sel.val().length > 0);
            this.input.select.name_id_text = (present ? sel.val() : null);

            sel = $('#sp-user-email-containing-text');
            present = (sel.val().length > 0);
            this.input.select.email_text = (present ? sel.val() : null);

            sel = $('#sp-user-select-group');
            present = (sel.val() !== "0");
            this.input.select.usergroup_id = present ? sel.val() : null;

            this.input.sort.field = _view.getRadioValue('sp-user-sort-by');
            this.input.sort.ascending = _view.isRadioIdSelected('sp-user-sort-dir', 'sp-user-sort-dir-asc');
        },

        // JSON input
        input: {

            page: 1,
            maxResults: 10,

            select: {
                name_id_text: null,
                email_text: null,
                admin: null,
                person: null,
                disabled: null,
                deleted: false
            },
            sort: {
                field: 'id', // id | email
                ascending: true
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelAdmin('UsersBase', skipBeforeLoad);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageAdmin(_this, 'UsersPage', '#sp-user-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page get
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-users-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-users-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-users-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });

            $('.sp-sparkline-user').sparkline('html', {
                enableTagOptions: true
            });
        }
    };

    /**
     *
     */
    _ns.PanelAccountsBase = {

        applyDefaults: function() {

            this.input.page = 1;
            this.input.maxResults = 10;

            this.input.select.name_text = null;
            // AccountTypeEnum
            this.input.select.accountType = undefined;
            // Boolean
            this.input.select.deleted = false;
            // Boolean
            this.input.sort.ascending = true;
        },

        beforeload: function() {
            $.noop();
        },

        afterload: function() {
            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var val,
                id,
                _view = _ns.PanelCommon.view;

            $('#sp-shared-account-name-containing-text').val(this.input.select.name_text);

            val = this.input.select.deleted;
            _view.checkRadioValue('sp-shared-account-select-deleted', val === null ? "" : (val ? "1" : "0"));

            val = this.input.select.accountType;
            _view.checkRadioValue('sp-shared-account-select-type', val === undefined ? "" : val);

            //
            id = 'sp-shared-account-sort-dir';
            _view.checkRadio(id, this.input.sort.ascending ? id + '-asc' : id + '-desc');
        },

        v2m: function() {

            var val,
                sel,
                present,
                _view = _ns.PanelCommon.view;

            val = _view.getRadioValue('sp-shared-account-select-deleted');
            this.input.select.deleted = (val === "" ? null : (val === "1"));

            val = _view.getRadioValue('sp-shared-account-select-type');
            this.input.select.accountType = (val === "" ? undefined : val);

            sel = $('#sp-shared-account-name-containing-text');
            present = (sel.val().length > 0);
            this.input.select.name_text = (present ? sel.val() : null);

            this.input.sort.field = _view.getRadioValue('sp-shared-account-sort-by');
            this.input.sort.ascending = _view.isRadioIdSelected('sp-shared-account-sort-dir', 'sp-shared-account-sort-dir-asc');
        },

        // JSON input
        input: {

            page: 1,
            maxResults: 10,

            select: {
                name_text: null,
                deleted: false
            },
            sort: {
                field: 'name', // name
                ascending: true
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelAdmin('AccountsBase', skipBeforeLoad);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageAdmin(_this, 'AccountsPage', '#sp-shared-account-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page get
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-shared-accounts-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-shared-accounts-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-shared-accounts-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
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

        applyDefaults: function() {

            this.input.page = 1;
            this.input.maxResults = 10;

            this.input.select.name_text = null;
            // Boolean
            this.input.sort.ascending = true;
        },

        beforeload: function() {
            $.noop();
        },

        afterload: function() {
            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var id,
                _view = _ns.PanelCommon.view;

            $('#sp-user-group-name-containing-text').val(this.input.select.name_text);

            id = 'sp-user-group-sort-dir';
            _view.checkRadio(id, this.input.sort.ascending ? id + '-asc' : id + '-desc');

        },

        v2m: function() {
            var sel,
                present,
                _view = _ns.PanelCommon.view;

            sel = $('#sp-user-group-name-containing-text');
            present = (sel.val().length > 0);
            this.input.select.name_text = (present ? sel.val() : null);

            this.input.sort.ascending = _view.isRadioIdSelected('sp-user-group-sort-dir', 'sp-user-group-sort-dir-asc');
        },

        // JSON input
        input: {

            page: 1,
            maxResults: 10,

            select: {
                name_text: null
            },
            sort: {
                ascending: true
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelAdmin('UserGroupsBase', skipBeforeLoad);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageAdmin(_this, 'UserGroupsPage', '#sp-user-group-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page get
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-user-groups-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-user-groups-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-user-groups-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });

        }
    };

}(jQuery, this, this.document, JSON, this.org.savapage));

// @license-end
