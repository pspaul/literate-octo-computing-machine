/*! SavaPage Common | (c) 2011-2016 Datraverse B.V. | GNU Affero General Public License */

/*
* This file is part of the SavaPage project <http://savapage.org>.
* Copyright (c) 2011-2016 Datraverse B.V.
* Authors: Rijk Ravestein.
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

// Namespace
this.org = this.org || {};
this.org.savapage = {};

// ---------------------------------------------------------------
// http://stackoverflow.com/questions/610406/javascript-equivalent-to-printf-string-format
// ---------------------------------------------------------------
/*
 * Example: "{0} is dead, but {1} is alive! {0} {2}".format ("ASP", "ASP.NET")
 */
String.prototype.format = function() {"use strict";
	var args = arguments;
	return this.replace(/\{(\d+)\}/g, function(match, number) {
		//return typeof args[number] !== 'undefined' ? args[number] : '{' + number + '}';
		return args[number] || '{' + number + '}';
	});
};
/**
 * My (RRA) own redirection for format(), i.e. converting argument of type array
 * to the arguments "array" of the format() function.
 *
 * @example "{0} is dead, but {1} is alive! {0} {2}".format(["ASP", "ASP.NET"])
 */
String.prototype.vformat = function() {"use strict";
	var args = arguments;
	if (args[0] === undefined) {// This check is needed for IE 8
		return String.prototype.format.apply(this, []);
	}
	return String.prototype.format.apply(this, args[0]);
};

/**
 *
 */
( function(window, document, _ns) {"use strict";

		/*
		 *  WebApp URL parameters.
		 */
		_ns.URL_PARM = {
			LOG_LEVEL : 'sp-log',
			USER : 'sp-user',
			LANGUAGE : 'sp-lang',
			COUNTRY: 'sp-ctry',
			LOGIN : 'sp-login'
		};

		/**
		 *
		 */
		_ns.initWebApp = function(webAppType) {
			_ns.WEBAPP_TYPE = webAppType;
			_ns.logger.setLogLevel(_ns.Utils.getUrlParam(_ns.URL_PARM.LOG_LEVEL));
		};

		// WebAppTypeEnum
		_ns.WEBAPP_TYPE = 'UNDEFINED';

		// URL parameter for HTML pages (/page).
		_ns.WebAppTypeUrlParm = function(pfx) {
			return (pfx || '?') + 'sp-app=' + _ns.WEBAPP_TYPE;
		};

		/**
		 *
		 */
		_ns.derive = function(baseObject) {
			function F() {
			}


			F.prototype = baseObject;
			return new F();
		};

		/**
		 *
		 */
		_ns.Utils = {};

		/**
		 * Counts the number of properties in an object.
		 *
		 * Since IE8 does NOT support Object.keys(_model.myCutPages).length
		 * we use a workaround.
		 */
		_ns.Utils.countProp = function(obj) {
			var n = 0, p;
			for (p in obj) {
				if (obj.hasOwnProperty(p)) {
					n++;
				}
			}
			return n;
		};

		/**
		 * @return first object property or null when not found.
		 */
		_ns.Utils.getFirstProp = function(obj) {
			var p, first = null;
			for (p in obj) {
				if (obj.hasOwnProperty(p)) {
					first = p;
					break;
				}
			}
			return first;
		};

		/**
		 * http://codesnippets.joyent.com/posts/show/1917
		 *
		 * @param val
		 *            The email address
		 * @return
		 */
		_ns.Utils.isEmailValid = function(val) {
			var p = /^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/;
			return p.test(val);
		};
		/**
		 * Gets the value of parameter of URL of current document.
		 *
		 * Thanks to:
		 * http://snipplr.com/view/26662/get-url-parameters-with-jquery--improved/
		 *
		 * @param name
		 *            The name of the URL parameter.
		 * @return The value of the URL parameter.
		 */
		_ns.Utils.getUrlParam = function(name) {
			var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(document.location.href);
			if (!results) {
				return null;
			}
			return results[1] || 0;
		};

		_ns.Utils.isFunction = function(value) {
			if (value === undefined || value === null) {
				return false;
			}
			return typeof value === 'function';
		};

		/**
		 * Constructor
		 */
		_ns.User = function() {
			this.alias = null;
			this.id = null;
			this.admin = null;
			this.role = null;
			this.mail = null;
			this.mailDefault = null;
			this.loggedOn = false;
		};
		/**
		 * Constructor
		 */
		_ns.I18n = function() {

			var _i18n = {}, _values = {};

			/**
			 *
			 */
			this.refresh = function(i18n) {
				if (i18n) {
					_i18n = i18n;
				}
			};
			/**
			 *
			 */
			this.format = function(key, values) {
				if (_i18n !== null) {
					if (_i18n[key]) {
						if (values) {
							_values[key] = values;
						}
						return _i18n[key].vformat(_values[key]);
					}
				}
				return key;
			};
			/**
			 *
			 */
			this.setValues = function(key, values) {
				_values[key] = values;
			};
			/**
			 *
			 */
			this.initValues = function(values) {
				_values = values;
			};
			/**
			 *
			 */
			this.string = function(key) {
				return _i18n[key];
			};
		};

		/**
		 * Logging to window.console.
		 */
		_ns.Logger = function() {

			var _zeroPad = function(value, length) {
				var result = '';
				while (--length > 0) {
					if (value >= Math.pow(10, length)) {
						break;
					}
					result += '0';
				}
				result += value;
				return result;
			}
			//
			, _log = function(level, args) {
				if (window.console) {
					var logger = window.console[level];
					if (_ns.Utils.isFunction(logger)) {
						var now = new Date();
						[].splice.call(args, 0, 0, _zeroPad(now.getHours(), 2) + ':' + _zeroPad(now.getMinutes(), 2) + ':' + _zeroPad(now.getSeconds(), 2) + '.' + _zeroPad(now.getMilliseconds(), 3));
						logger.apply(window.console, args);
					}
				}
			};

			this.LEVEL_INFO = 'info';
			this.LEVEL_WARN = 'warn';
			this.LEVEL_DEBUG = 'debug';

			this.logLevel = this.LEVEL_WARN;

			this.setLogLevel = function(level) {

				if (!level || level === this.logLevel) {
					return;
				}

				if (level !== this.LEVEL_INFO && level !== this.LEVEL_WARN && level !== this.LEVEL_DEBUG) {
					this.warn('Log level \"' + level + '\" invalid: keeping level \"' + this.logLevel + '\".');
					return;
				}
				this.info('Log level set to \"' + level + '\".');
				this.logLevel = level;
			};

			this.isInfoEnabled = function() {
				return this.logLevel !== this.LEVEL_WARN;
			};

			this.isDebugEnabled = function() {
				return this.logLevel === this.LEVEL_DEBUG;
			};

			this.warn = function() {
				_log(this.LEVEL_WARN, arguments);
			};

			this.info = function() {
				if (this.logLevel !== this.LEVEL_WARN) {
					_log(this.LEVEL_INFO, arguments);
				}
			};

			this.debug = function() {
				if (this.logLevel === this.LEVEL_DEBUG) {
					_log(this.LEVEL_DEBUG, arguments);
				}
			};
		};

		_ns.logger = new _ns.Logger();

		/**
		 * Constructor
		 */
		_ns.Base = function() {
		};

	}(this, this.document, this.org.savapage));
