

define([
    'flight/lib/component'
], 
function(defineComponent) {
    'use strict';

    return defineComponent(MouseOverlay);

    function MouseOverlay() {

        this.after('initialize', function() {
            this.overlayNode = //$('<div id="mouseoverlay" class="tooltip"><div class="tooltip-arrow"/></div>')
                               $('<div class="tooltip fade right in"><div class="tooltip-arrow"></div><div class="tooltip-inner"></div></div>').
                css('transform', '').
                hide().
                appendTo(document.body);

            this.transformProperty = $.cssProps.transform;
            this.textNode = this.overlayNode.find('.tooltip-inner');

            this.on('displayInformation', this.onDisplayInformation);
            this.on('mousemove.displayInformation', this.onMouseMove);
            this.on(document, 'click', this.onClick);
        });

        this.onClick = function() {
            clearTimeout(this.timeout);
            this.overlayNode.hide();
            this.tracking = false;
        };

        /**
         * @options data.dismiss [click, auto]
         * @options data.dismissDuration default 2000
         */
        this.onDisplayInformation = function(event, data) {
            var self = this;

            clearTimeout(this.timeout);
            if (!this.position) return;

            this.tracking = true;
            this.textNode.text(data.message);
            this.overlayNode.css('transform', 'translate(' + this.position[0] + 'px,' + this.position[1] + 'px)').show();

            this.timeout = _.delay(function() {
                self.overlayNode.hide();
                self.tracking = false;
            }, data.dismissDuration || 2000);

            requestAnimationFrame(function align() {
                if (self.tracking) {
                    if (self.mouseMoved) {
                        self.overlayNode[0].style[self.transformProperty] = 'translate(' + self.position[0] + 'px,' + self.position[1] + 'px)';
                        self.mouseMoved = false;
                    }
                    requestAnimationFrame(align);
                }
            });
        };

        this.onMouseMove = function(event) {
            this.mouseMoved = true;
            this.position = [event.pageX+10,event.pageY-10];
        };
    }
});