/*! SavaPage jQuery Mobile Job Tickets Page | (c) 2011-2016 Datraverse B.V. | GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

/*jslint browser: true*/
/*global $, jQuery, alert*/

( function($, window, document, JSON, _ns) {"use strict";

		// =========================================================================
		/**
		 * Constructor
		 */
		_ns.PageJobTickets = function(_i18n, _view, _model, _api) {

			//_self.onBack();

			/*
			* SavaPage call-backs from pagecontainershow / pagecontainerhide.
			*/
			//_self.onPageShow()
			//_self.onPageHide()

			var _page = new _ns.Page(_i18n, _view, "#page-jobtickets", "PageJobTickets")
			//
			, _self = _ns.derive(_page);

			/**
			 *
			 */
			$(_self.id()).on('pagecreate', function(event) {
			}).on("pageshow", function(event, ui) {
			});

			return _self;
		};

	}(jQuery, this, this.document, JSON, this.org.savapage));
