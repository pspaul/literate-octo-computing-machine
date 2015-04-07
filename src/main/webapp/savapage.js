/*! SavaPage Common | (c) 2011-2015 Datraverse B.V. | GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
(function(window, document, _ns) {"use strict";

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
     * Constructor
     */
    _ns.Base = function() {

        var _config = {
            logLevel : 'info'
        },
        //
        _isFunction = function(value) {
            if (value === undefined || value === null) {
                return false;
            }
            return typeof value === 'function';
        },
        //
        _log = function(level, args) {
            if (window.console) {
                var logger = window.console[level];
                if (_isFunction(logger)) {
                    logger.apply(window.console, args);
                }
            }
        };

        this._warn = function() {
            _log('warn', arguments);
        };

        this._info = function() {
            if (_config.logLevel !== 'warn') {
                _log('info', arguments);
            }
        };

        this._debug = function() {
            if (_config.logLevel === 'debug') {
                _log('debug', arguments);
            }
        };
    };
}(this, this.document, this.org.savapage));
