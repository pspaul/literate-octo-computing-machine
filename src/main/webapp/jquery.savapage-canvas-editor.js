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

        _ns.HtmlCanvasEditor = function(_canvasId, _onAfterRender, _onSelectionCreated, _onSelectionCleared, _onSelectionUpdated) {
            const ADD_L_INIT = 50,
                ADD_T_INIT = 50,
                ADD_INC = 20,
                ADD_VISIBLE = 50,
            /*
            * How to keep fixed strokeWidth afer resize/scaling?
            */
            // _strokeUniform works (eventually, after loadJSON workaround), but
            // SVG to PDF utility (rsvg-convert) produces scaled strokeWidth
            // anyway...
                _strokeUniform = false,
            // ... so, we try to scale to fixed strokeWidth here in Javascript.
                SCALE_TO_FIXED_STROKEWIDTH = true && !_strokeUniform;

            var _canvas,
                _coordAddObj = {
                left : ADD_L_INIT,
                top : ADD_T_INIT
            },
                _fill = 'rgb(255,255,255)',
                _stroke = 'rgb(0,0,0)',
                _strokeWidth = 0,
                _fillTextbox = 'rgb(0,0,0)',
                _opacity = 1.0,
            //
                __nActiveObjectsWlk = 0,
            //
                _getLastAddedObject = function() {
                var count = _canvas.getObjects().length;
                return count > 0 ? _canvas.item(count - 1) : null;
            },
            //
                _addActiveObject = function(obj) {
                obj.left = _coordAddObj.left;
                obj.top = _coordAddObj.top;
                _canvas.add(obj).setActiveObject(obj);
                _coordAddObj.left += ADD_INC;
                _coordAddObj.top += ADD_INC;
                if (_coordAddObj.left + ADD_VISIBLE > _canvas.width || _coordAddObj.top + ADD_VISIBLE > _canvas.height) {
                    _coordAddObj.left = ADD_L_INIT;
                    _coordAddObj.top = ADD_T_INIT;
                }
            },
            //
                _applyCommonShapeProps = function(o) {
                o.fill = _fill;
                o.opacity = _opacity;
                o.stroke = _stroke;
                o.strokeWidth = _strokeWidth;
                if (_strokeUniform) {
                    o.strokeUniform = _strokeUniform;
                }
                return o;
            },
            //
                _setActiveObjectsProp = function(key, value) {
                var objs = _canvas.getActiveObjects();
                if (objs.length) {
                    objs.forEach(function(o) {
                        o.set(key, value);
                    });
                    _canvas.renderAll();
                }
            },
            //
                _resizeRect = function(o, strokeWidth) {
                o.height = o.height * o.scaleY;
                o.width = o.width * o.scaleX;

                if (strokeWidth || strokeWidth === 0) {
                    o.strokeWidth = strokeWidth;
                    o.strokeWidthUnscaled = strokeWidth;
                } else {
                    o.strokeWidth = o.strokeWidthUnscaled;
                }

                o.scaleX = 1;
                o.scaleY = 1;
                o.zoomX = 1;
                o.zoomY = 1;
            },
            //
                _onLoadObj = function(o) {
                if (( o instanceof fabric.Line) || ( o instanceof fabric.Textbox)) {
                    o.setControlsVisibility({
                        mt : false,
                        mb : false,
                    });
                } else if ( o instanceof fabric.Rect) {
                    if (SCALE_TO_FIXED_STROKEWIDTH) {
                        // Ensure correct resizing if part of active group.
                        o.objectCaching = false;
                    }
                }
            },
            //
                _onObjectScaling = function(o) {
                var scaleFactor;
                /*
                 * Note: A saved Triangle is loaded as Polygon.
                 */
                if (( o instanceof fabric.Polygon) || ( o instanceof fabric.Textbox)) {
                    return;
                }

                if (!o.strokeWidthUnscaled && o.strokeWidth) {
                    o.strokeWidthUnscaled = o.strokeWidth;
                }

                scaleFactor = (o.scaleX + o.scaleY) / 2;

                if (o.strokeWidthUnscaled) {
                    o.strokeWidth = o.strokeWidthUnscaled / scaleFactor;
                }
            },
            //
                _onObjectScaled = function(o) {
                if ( o instanceof fabric.Rect) {
                    _resizeRect(o);
                }
                // set 'dirty' to re-render object's cache !!
                o.dirty = true;
            },
            /**
             *  Constructs this instance.
             */
                _initialize = function() {

                _canvas = new fabric.Canvas(_canvasId, {
                    isDrawingMode : false
                });
                _canvas.skipOffscreen = true;

                /**
                 * Drawing operation ended and the path is added.
                 */
                _canvas.on('path:created', function(data) {
                    var path = _getLastAddedObject();
                    if (path) {
                        /*
                         * Since opacity is not a brush property, we set the
                         * opacity of the path after it is created.
                         */
                        path.set('opacity', _opacity);

                        // Toggle drawing mode to render.
                        _canvas.isDrawingMode = false;
                        _canvas.renderAll();
                        _canvas.isDrawingMode = true;
                    }
                }).on('selection:created', function(data) {
                    __nActiveObjectsWlk = _canvas.getActiveObjects().length;
                    if (__nActiveObjectsWlk > 1) {
                        data.target.setControlsVisibility({
                            mt : false,
                            mb : false,
                            mr : false,
                            ml : false,
                        });
                    }
                    _onSelectionCreated && _onSelectionCreated(__nActiveObjectsWlk);
                }).on('selection:cleared', function(data) {
                    __nActiveObjectsWlk = 0;
                    _onSelectionCleared && _onSelectionCleared();
                }).on('selection:updated', function(data) {
                    __nActiveObjectsWlk = _canvas.getActiveObjects().length;
                    _onSelectionUpdated && _onSelectionUpdated(__nActiveObjectsWlk);
                }).on('after:render', function(data) {
                    _onAfterRender && _onAfterRender(_canvas.getObjects().length);
                });

                if (SCALE_TO_FIXED_STROKEWIDTH) {
                    _canvas.on('object:scaling', function(e) {
                        var t = e.target;
                        if (__nActiveObjectsWlk === 1) {
                            _onObjectScaling(t);
                        }
                    }).on('object:scaled', function(e) {
                        var t = e.target;
                        if (__nActiveObjectsWlk === 1) {
                            _onObjectScaled(t);
                            t.dirty = true;
                        }
                    });
                }
            };

            //
            _initialize();

            /* Public methods */

            this.hasTextObjects = function() {
                var i,
                    j,
                    obj,
                    objs = _canvas.getObjects();

                for ( i = 0,
                j = objs.length; i < j; i++) {
                    obj = objs[i];
                    if ( obj instanceof fabric.Textbox) {
                        return true;
                    }
                };
                return false;
            };

            this.addLine = function() {
                var o = new fabric.Line([50, 100, 200, 100], {
                    opacity : _opacity,
                    stroke : _stroke,
                    strokeWidth : _strokeWidth
                });
                o.setControlsVisibility({
                    mt : false,
                    mb : false,
                });
                _addActiveObject(o);
            };

            this.addCircle = function() {
                _addActiveObject(_applyCommonShapeProps(new fabric.Circle({
                    radius : 25
                })));
            };

            this.addRect = function() {
                var o = new fabric.Rect({
                    width : 50,
                    height : 50
                });
                if (SCALE_TO_FIXED_STROKEWIDTH) {
                    // Ensure correct resizing if part of active group.
                    o.objectCaching = false;
                }
                _addActiveObject(_applyCommonShapeProps(o));
            };

            this.addTriangle = function() {
                _addActiveObject(_applyCommonShapeProps(new fabric.Triangle({
                    width : 50,
                    height : 50
                })));
            };

            this.addTextbox = function() {
                var o = new fabric.Textbox('Line 1\r\nLine 2', {
                    fontSize : 20,
                    fontFamily : 'helvetica',
                    angle : 0,
                    fill : _fillTextbox,
                    stroke : _stroke,
                    fontWeight : '',
                    originX : 'left',
                    width : 200,
                    hasRotatingPoint : true,
                    centerTransform : true,
                    opacity : _opacity
                });
                o.setControlsVisibility({
                    mt : false,
                    mb : false,
                });
                _addActiveObject(o);
            };

            this.setFreeDrawingBrush = function(name, color, width) {
                _canvas.freeDrawingBrush = new fabric[name + 'Brush'](_canvas);
                this.setBrushColor(color);
                _canvas.freeDrawingBrush.width = width;
            };

            this.setOpacityPerc = function(perc) {
                _opacity = perc / 100;
            };

            this.setOpacitySelected = function() {
                _setActiveObjectsProp('opacity', _opacity);
            };

            this.setStroke = function(color) {
                _stroke = color;
                _fillTextbox = color;
            };

            this.setStrokeSelected = function() {
                _setActiveObjectsProp('stroke', _stroke);
            };

            this.setStrokeWidth = function(width) {
                _strokeWidth = width;
            };

            this.setStrokeWidthSelected = function() {
                var objs;
                if (SCALE_TO_FIXED_STROKEWIDTH) {
                    objs = _canvas.getActiveObjects();
                    if (objs.length) {
                        objs.forEach(function(o) {
                            if ( o instanceof fabric.Rect) {
                                _resizeRect(o, _strokeWidth);
                            } else {
                                // Scale strokeWidth: width will differ around
                                // shape though.
                                o.strokeWidthUnscaled = _strokeWidth;
                                o.strokeWidth = _strokeWidth / ((o.scaleX + o.scaleY) / 2);
                            }
                            // set 'dirty' to re-render object's cache !!
                            o.dirty = true;
                        });
                        _canvas.renderAll();
                    }
                } else {
                    _setActiveObjectsProp('strokeWidth', _strokeWidth);
                }
            };

            this.setFill = function(color) {
                _fill = color;
            };

            this.setFillSelected = function() {
                _setActiveObjectsProp('fill', _fill);
            };

            this.setFillSelectedExt = function(value) {
                _setActiveObjectsProp('fill', value);
            };

            this.toSVG = function() {
                return _canvas.toSVG();
            };

            this.toJSON = function() {
                return JSON.stringify(_canvas.toJSON());
            };

            this.loadSVG = function(svg) {
                fabric.loadSVGFromString(svg, function(objs, options) {
                    _canvas.add.apply(_canvas, objs);
                    objs.forEach(function(o) {
                        _onLoadObj(o);
                    });
                    _canvas.renderAll();
                });
            };

            this.loadJSON = function(json) {
                var objs;
                _canvas.loadFromJSON(json, _canvas.renderAll.bind(_canvas));
                objs = _canvas.getObjects();
                if (objs.length) {
                    if (_strokeUniform) {
                        /*
                         * Workaround to restore borderWidth (strokeUniform) and
                         * trigger object position. FabricJS bug?.
                         */
                        objs.forEach(function(obj) {
                            obj.set({
                                strokeUniform : _strokeUniform,
                                top : obj.top + 1
                            });
                            _onLoadObj(obj);
                        });
                        _canvas.renderAll();
                        objs.forEach(function(obj) {
                            obj.set({
                                top : obj.top - 1
                            });
                        });

                    } else {
                        _canvas.getObjects().forEach(function(o) {
                            _onLoadObj(o);
                        });
                        _canvas.renderAll();
                    }
                }
            };

            this.clear = function() {
                _canvas.clear();
                _coordAddObj.left = ADD_L_INIT;
                _coordAddObj.top = ADD_T_INIT;
            };

            this.selectAll = function() {
                var selection;
                this.deactivateAll();
                selection = new fabric.ActiveSelection(_canvas.getObjects(), {
                    canvas : _canvas
                });
                _canvas.setActiveObject(selection).renderAll();
                return;
            };

            this.countObjects = function() {
                return _canvas.getObjects().length;
            };

            this.deactivateAll = function() {
                _canvas.discardActiveObject().renderAll();
            };

            this.debugSelected = function() {
                console.log(_canvas.getActiveObjects()[0]);
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
                    this.deactivateAll();
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

