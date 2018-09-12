/*! SavaPage jQuery Mobile Print Site Web App | (c) 2011-2018 Datraverse B.V. 
 * | GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

        /** */
        _ns.PanelUserOutbox = {

            // Parameter to be filled
            userKey : null,

            /** */
            refresh : function(my, skipBeforeLoad) {
                var html = _ns.PanelCommon.view.getUserPageHtml('OutboxAddin', {
                    jobTickets : false,
                    expiryAsc : false,
                    userKey : my.userKey
                });

                if (html) {
                    html = '<ul data-role="listview" data-inset="true"><li data-role="list-divider">&nbsp;</li><li>' + html;
                    html += '</li></ul>';
                    $('#sp-a-content').html(html).enhanceWithin();
                    $('.sp-sparkline-printout').sparkline('html', {
                        enableTagOptions : true
                    });

                }
            },

            /** */
            beforeload : function(my) {
                $.noop();
            },

            /** */
            afterload : function(my) {
                $.noop();
            }
        };

    }(jQuery, this, this.document, JSON, this.org.savapage));
