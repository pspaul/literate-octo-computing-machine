/*! SavaPage jQuery Mobile CanvasEditor | (c) 2020 Datraverse B.V. | GNU Affero
* General Public License */

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

//--------------------------------------------------------------
//
//--------------------------------------------------------------
( function(window, document, navigator, _ns) {
        "use strict";

        _ns.HtmlCanvasEditor = function(_canvasId) {
            var _this = this,
                _canvas = new fabric.Canvas(_canvasId, {
                isDrawingMode : true
            });

            _canvas.skipOffscreen = true;

            this.setFreeDrawingBrush = function(name, color, width) {
                _canvas.freeDrawingBrush = new fabric[name + 'Brush'](_canvas);
                _this.setBrushColor(color);
                _canvas.freeDrawingBrush.width = width;
            };

            this.toSVG = function() {
                return _canvas.toSVG();
            };

            this.clear = function() {
                _canvas.clear();
            };

            this.deactivateAll = function() {
                _canvas.discardActiveObject().renderAll();
            };

            this.clearSelected = function() {
                var objs = _canvas.getActiveObjects();
                _canvas.discardActiveObject();
                if (objs.length) {
                    _canvas.remove.apply(_canvas, objs);
                }
            };

            this.enableDrawingMode = function(enable) {
                _canvas.isDrawingMode = enable;
                if (enable) {
                    _this.deactivateAll();
                }
            };

            this.setBrushColor = function(color) {
                _canvas.freeDrawingBrush.color = color;
            };

            this.setBrushWidth = function(width) {
                _canvas.freeDrawingBrush.width = width;
            };

            this.setWidth = function(val) {
                _canvas.setWidth(val);
            };

            this.setHeight = function(val) {
                _canvas.setHeight(val);
            };

            this.setBackgroundImage = function(url) {
                _canvas.setBackgroundImage(url, _canvas.renderAll.bind(_canvas), {
                    width : _canvas.width,
                    height : _canvas.height,
                    // Needed to position backgroundImage at 0/0
                    originX : 'left',
                    originY : 'top',
                    excludeFromExport : true
                });
            };

        };

    }(this, this.document, this.navigator, this.org.savapage));

