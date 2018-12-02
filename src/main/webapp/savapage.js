/*! SavaPage Common | (c) 2011-2017 Datraverse B.V. | GNU Affero General Public License */

/*
* This file is part of the SavaPage project <https://www.savapage.org>.
* Copyright (c) 2011-2017 Datraverse B.V.
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
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
String.prototype.format = function() {
    "use strict";
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function(match, number) {
        //return typeof args[number] !== 'undefined' ? args[number] : '{' +
        // number + '}';
        return args[number] || '{' + number + '}';
    });
};
/**
 * My (RRA) own redirection for format(), i.e. converting argument of type array
 * to the arguments "array" of the format() function.
 *
 * @example "{0} is dead, but {1} is alive! {0} {2}".format(["ASP", "ASP.NET"])
 */
String.prototype.vformat = function() {
    "use strict";
    var args = arguments;
    if (args[0] === undefined) {// This check is needed for IE 8
        return String.prototype.format.apply(this, []);
    }
    return String.prototype.format.apply(this, args[0]);
};

/**
 *
 */
String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

/**
 *
 */
( function(window, document, navigator, _ns) {
        "use strict";

        /*
         *  WebApp URL parameters.
         */
        _ns.URL_PARM = {
            WEBAPP_TYPE : 'sp-app',
            LOG_LEVEL : 'sp-log',
            USER : 'sp-user',
            LANGUAGE : 'sp-lang',
            COUNTRY : 'sp-ctry',
            LOGIN : 'sp-login',
            LOGIN_LOCAL : 'sp-login-local',
            LOGIN_OAUTH : 'sp-login-oauth',
            SHOW : 'sp-show'
        };

        _ns.URL_PARM_SHOW_PDF = 'pdf';
        _ns.URL_PARM_SHOW_PRINT = 'print';

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
            return (pfx || '?') + _ns.URL_PARM.WEBAPP_TYPE + '=' + _ns.WEBAPP_TYPE;
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

        // Execute foo async with max 5 parameters.
        _ns.Utils.asyncFoo = function(foo, p1, p2, p3, p4, p5) {
            window.setTimeout(function() {
                foo(p1, p2, p3, p4, p5);
            }, 10);
        };

        _ns.Utils.isMobileOrTablet = function() {
            var check = false;
            (function(a) {
                if (/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino|android|ipad|playbook|silk/i.test(a) || /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0, 4))) {
                    check = true;
                }
            })(navigator.userAgent || navigator.vendor || window.opera);
            return check;
        };

        /**
         * Counts the number of properties in an object.
         *
         * Since IE8 does NOT support Object.keys(_model.myCutPages).length
         * we use a workaround.
         */
        _ns.Utils.countProp = function(obj) {
            var n = 0,
                p;
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
            var p,
                first = null;
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

        // Turns URI query string like hello=1&another=2 into object {hello: 1,
        // another: 2}
        _ns.Utils.parseUriQuery = function(qstr) {
            var query = {},
                a = (qstr[0] === '?' ? qstr.substr(1) : qstr).split('&'),
                i,
                b;
            for ( i = 0; i < a.length; i++) {
                b = a[i].split('=');
                query[decodeURIComponent(b[0])] = decodeURIComponent(b[1] || '');
            }
            return query;
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

        _ns.Utils.hasUrlParam = function(name) {
            return document.location.search.indexOf(name) !== -1;
        };

        _ns.Utils.isFunction = function(value) {
            if (value === undefined || value === null) {
                return false;
            }
            return typeof value === 'function';
        };

        /**
         *
         */
        _ns.Utils.zeroPad = function(value, length) {
            var result = '';
            while (--length > 0) {
                if (value >= Math.pow(10, length)) {
                    break;
                }
                result += '0';
            }
            result += value;
            return result;
        };

        /**
         *
         */
        _ns.Utils.formatDateTime = function(date) {
            return date.getFullYear() + '-' + _ns.Utils.zeroPad(date.getMonth() + 1, 2) + '-' + _ns.Utils.zeroPad(date.getDate(), 2) + ' ' + _ns.Utils.zeroPad(date.getHours(), 2) + ':' + _ns.Utils.zeroPad(date.getMinutes(), 2) + ':' + _ns.Utils.zeroPad(date.getSeconds(), 2);
        };

        /**
         * A simple replacement of array.find(), which is not supported in old
         * IE (11) version(s).
         * 
         * @param array The array.
         * @param value The value to find.
         * @return value when found, otherwise undefined.
         */
        _ns.Utils.findInArray = function(array, value) {
            var len = array.length,
                i;
            for ( i = 0; i < len; i++) {
                if (array[i] === value) {
                    return array[i];
                }
            }
            return undefined;
        };

        /**
         * Constructor
         */
        _ns.User = function() {
            this.alias = null;
            this.id = null;
            this.admin = null;
            this.mail = null;
            this.mailDefault = null;
            this.loggedOn = false;
        };
        /**
         * Constructor
         */
        _ns.I18n = function() {

            var _i18n = {},
                _values = {};

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

            var _log = function(level, args) {
                if (window.console) {
                    var logger = window.console[level],
                        now;
                    if (_ns.Utils.isFunction(logger)) {
                        now = new Date();
                        [].splice.call(args, 0, 0, _ns.Utils.zeroPad(now.getHours(), 2) + ':' + _ns.Utils.zeroPad(now.getMinutes(), 2) + ':' + _ns.Utils.zeroPad(now.getSeconds(), 2) + '.' + _ns.Utils.zeroPad(now.getMilliseconds(), 3));
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

    }(this, this.document, this.navigator, this.org.savapage));
